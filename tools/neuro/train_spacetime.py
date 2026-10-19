#!/usr/bin/env python3
"""Тренер SpaceTime: GRU(17->32) + MDN-голова(19 логитов) на датасете из .neuro.

Датасет: CSV от NeuroRecorder с колонками x0..x16,dyaw,dpitch,dist,yaw,pitch.
  x0..x16      — фичи инференса (сжаты asinh/3), как их подаёт SpaceTimeDriver;
  dyaw, dpitch — реальные дельты мыши за тик (градусы) — метки.

Математика в точности как в SpaceTimeModel/SpaceTimeDriver (Java):
  * squash(x) = asinh(x)/3 (только для фич);
  * метка     = asinh(step) (в asinh-пространстве, БЕЗ /3);
  * mean clamp [-4,4], sigma = exp(clamp(log_sigma, -4, 1.5)), rho = tanh*0.95;
  * порядок MDN: [freeze, (logit, mean_y, mean_p, ls_y, ls_p, rho_raw) x 3];
  * GRU-гейты r,z,n в порядке PyTorch (weight_ih_l0 = wi, weight_hh_l0 = wh).

Экспорт (big-endian, формат spacetime.bin, 22556 байт):
  int hidden=32, int mix=3, int freezeCut, float yawLimit, float pitchLimit
  float[101] errorCurve
  int 96,17 + wi[96][17]; int 96,32 + wh[96][32]; int 96 + bi; int 96 + bh
  int 19,32 + outW[19][32]; int 19 + outB

Примеры:
  python tools/neuro/train_spacetime.py --data "run/brokke/neuro/data/*.csv" --epochs 40
  python tools/neuro/train_spacetime.py --data dataset.csv --dry-run --out check.bin
"""

import argparse
import glob
import math
import os
import struct
import sys

import numpy as np
import torch
import torch.nn as nn
import torch.nn.functional as F

FEAT = 17
HIDDEN = 32
MIX = 3
OUT = 1 + 6 * MIX
MEAN_CLAMP = (-4.0, 4.0)
LOG_SIGMA_CLAMP = (-4.0, 1.5)
RHO_CLAMP = 0.95


class SpaceTimeNet(nn.Module):
    def __init__(self):
        super().__init__()
        self.gru = nn.GRU(FEAT, HIDDEN, batch_first=True)
        self.head = nn.Linear(HIDDEN, OUT)

    def forward(self, x):
        h0 = torch.zeros(1, x.size(0), HIDDEN, device=x.device, dtype=x.dtype)
        y, _ = self.gru(x, h0)
        return self.head(y)


def mdn_nll(o, y1, y2):
    """o: [N, 19]; y1/y2: [N] (asinh-пространство). NLL 2D-Gaussian + корреляция."""
    logp_stack = []
    for k in range(MIX):
        b = 1 + 6 * k
        mu1 = o[:, b + 1].clamp(*MEAN_CLAMP)
        mu2 = o[:, b + 2].clamp(*MEAN_CLAMP)
        s1 = (o[:, b + 3].clamp(*LOG_SIGMA_CLAMP)).exp()
        s2 = (o[:, b + 4].clamp(*LOG_SIGMA_CLAMP)).exp()
        rho = torch.tanh(o[:, b + 5]) * RHO_CLAMP
        z1 = (y1 - mu1) / s1
        z2 = (y2 - mu2) / s2
        one_r2 = (1.0 - rho * rho).clamp(min=1e-6)
        logp = (
            -torch.log(s1)
            - torch.log(s2)
            - 0.5 * torch.log(one_r2)
            - 0.5 * math.log(2.0 * math.pi)
            - (z1 * z1 - 2.0 * rho * z1 * z2 + z2 * z2) / (2.0 * one_r2)
        )
        logp_stack.append(logp)
    logits = torch.stack([o[:, 1 + 6 * k] for k in range(MIX)], dim=1)
    logpi = logits - torch.logsumexp(logits, dim=1, keepdim=True)
    comp = torch.stack(logp_stack, dim=1) + logpi
    return -torch.logsumexp(comp, dim=1).mean()


def read_csv(path, limit_rows=0):
    with open(path, "r", encoding="utf-8") as f:
        header = f.readline().strip().split(",")
    try:
        idx = {name: i for i, name in enumerate(header)}
        feat_idx = [idx["x%d" % i] for i in range(FEAT)]
        dyaw_i = idx["dyaw"]
        dpitch_i = idx["dpitch"]
    except KeyError as e:
        print("skip %s: нет колонки %s" % (path, e))
        return None
    data = np.loadtxt(path, delimiter=",", skiprows=1, ndmin=2)
    if limit_rows > 0:
        data = data[:limit_rows]
    if data.size == 0:
        return None
    return (
        data[:, feat_idx].astype(np.float32),
        data[:, dyaw_i].astype(np.float32),
        data[:, dpitch_i].astype(np.float32),
    )


def cut_idle(dyaw, dpitch, max_idle, eps):
    """Вырезаем длинные стояния на месте (иначе модель учится всегда фризить)."""
    if max_idle <= 0:
        return np.ones(len(dyaw), dtype=bool)
    idle = (np.abs(dyaw) < eps) & (np.abs(dpitch) < eps)
    keep = np.ones(len(dyaw), dtype=bool)
    i = 0
    n = len(idle)
    while i < n:
        if idle[i]:
            j = i
            while j < n and idle[j]:
                j += 1
            if j - i > max_idle:
                keep[i:j] = False
            i = j
        else:
            i += 1
    return keep


def build_error_curve(feats, step_yaw, step_pitch, span=20.0):
    """101 бакет по нормированной ошибке -> median |step|, монотонная кривая.

    Индексируется как в SpaceTimeDriver.errorQuantile:
    q = clamp01(hypot(errYaw/span, errPitch/span)), err = sinh(3*x) (inverse squash).
    """
    err_yaw = np.sinh(3.0 * feats[:, 0].astype(np.float64))
    err_pitch = np.sinh(3.0 * feats[:, 1].astype(np.float64))
    q = np.clip(np.hypot(err_yaw / span, err_pitch / span), 0.0, 1.0)
    bucket = np.minimum((q * 100.0).astype(np.int64), 100)
    step = np.hypot(step_yaw.astype(np.float64), step_pitch.astype(np.float64))
    curve = np.full(101, np.nan)
    for b in range(101):
        m = bucket == b
        if m.any():
            curve[b] = np.median(step[m])
    present = ~np.isnan(curve)
    if not present.any():
        curve[:] = 1.0
    else:
        idx = np.arange(101)
        curve = np.interp(idx, idx[present], curve[present])
    curve = np.maximum.accumulate(curve)
    return np.clip(curve, 0.05, 90.0).astype(np.float32)


def pack_vec(w, arr):
    arr = np.asarray(arr, dtype=">f4")
    w.write(struct.pack(">i", arr.size))
    w.write(arr.tobytes())


def pack_mat(w, arr):
    arr = np.asarray(arr, dtype=">f4")
    rows, cols = arr.shape
    w.write(struct.pack(">ii", rows, cols))
    w.write(arr.tobytes())


def export_bin(path, net, curve, freeze_cut, yaw_limit, pitch_limit):
    g = net.gru
    wi = g.weight_ih_l0.detach().cpu().numpy()
    wh = g.weight_hh_l0.detach().cpu().numpy()
    bi = g.bias_ih_l0.detach().cpu().numpy()
    bh = g.bias_hh_l0.detach().cpu().numpy()
    ow = net.head.weight.detach().cpu().numpy()
    ob = net.head.bias.detach().cpu().numpy()
    assert wi.shape == (3 * HIDDEN, FEAT), wi.shape
    assert wh.shape == (3 * HIDDEN, HIDDEN), wh.shape
    assert ow.shape == (OUT, HIDDEN), ow.shape

    with open(path, "wb") as w:
        w.write(struct.pack(">iii", HIDDEN, MIX, int(freeze_cut)))
        w.write(struct.pack(">ff", float(yaw_limit), float(pitch_limit)))
        pack_vec(w, curve)
        pack_mat(w, wi)
        pack_mat(w, wh)
        pack_vec(w, bi)
        pack_vec(w, bh)
        pack_mat(w, ow)
        pack_vec(w, ob)


def verify_bin(path):
    size = os.path.getsize(path)
    with open(path, "rb") as f:
        hidden, mix, freeze_cut = struct.unpack(">iii", f.read(12))
        yaw_limit, pitch_limit = struct.unpack(">ff", f.read(8))
    ok = size == 22556 and hidden == HIDDEN and mix == MIX
    print(
        "проверка: %d байт (ожидается 22556), hidden=%d mix=%d freezeCut=%d yaw=%.1f pitch=%.1f -> %s"
        % (size, hidden, mix, freeze_cut, yaw_limit, pitch_limit, "OK" if ok else "FAIL")
    )
    return ok


def main():
    ap = argparse.ArgumentParser(description="Тренер SpaceTime (GRU+MDN) -> spacetime.bin")
    ap.add_argument("--data", nargs="+", required=True, help="CSV-файлы или glob-паттерны")
    ap.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "spacetime_trained.bin"))
    ap.add_argument("--epochs", type=int, default=30)
    ap.add_argument("--batch-windows", type=int, default=64)
    ap.add_argument("--window", type=int, default=32)
    ap.add_argument("--lr", type=float, default=1e-3)
    ap.add_argument("--freeze-weight", type=float, default=1.0)
    ap.add_argument("--freeze-cut", type=int, default=42)
    ap.add_argument("--yaw-limit", type=float, default=179.0)
    ap.add_argument("--pitch-limit", type=float, default=90.0)
    ap.add_argument("--freeze-eps", type=float, default=0.05)
    ap.add_argument("--max-idle", type=int, default=40, help="вырезать стояния дольше N тиков (0 = не вырезать)")
    ap.add_argument("--limit-rows", type=int, default=0)
    ap.add_argument("--seed", type=int, default=42)
    ap.add_argument("--dry-run", action="store_true", help="экспорт без обучения (проверка формата)")
    args = ap.parse_args()

    torch.manual_seed(args.seed)
    np.random.seed(args.seed)

    paths = []
    for pat in args.data:
        hits = glob.glob(pat)
        if hits:
            paths.extend(sorted(hits))
        elif os.path.isfile(pat):
            paths.append(pat)
    if not paths:
        print("не найдено ни одного CSV по --data")
        return 1

    files = []
    total_rows = 0
    for p in paths:
        r = read_csv(p, args.limit_rows)
        if r is None:
            continue
        feats, dyaw, dpitch = r
        keep = cut_idle(dyaw, dpitch, args.max_idle, args.freeze_eps)
        feats, dyaw, dpitch = feats[keep], dyaw[keep], dpitch[keep]
        if len(feats) < args.window:
            print("skip %s: %d строк (< window %d)" % (p, len(feats), args.window))
            continue
        freeze = ((np.abs(dyaw) < args.freeze_eps) & (np.abs(dpitch) < args.freeze_eps)).astype(np.float32)
        lab_yaw = np.arcsinh(np.clip(dyaw, -args.yaw_limit, args.yaw_limit)).astype(np.float32)
        lab_pitch = np.arcsinh(np.clip(dpitch, -args.pitch_limit, args.pitch_limit)).astype(np.float32)
        files.append((feats, lab_yaw, lab_pitch, freeze, dyaw, dpitch))
        total_rows += len(feats)
        print("загружен %s: %d строк, фриз-меток: %.1f%%" % (p, len(feats), 100.0 * float(freeze.mean())))

    if not files:
        print("нет пригодных данных")
        return 1

    net = SpaceTimeNet()
    if args.dry_run:
        print("--dry-run: пропускаем обучение")
    else:
        opt = torch.optim.Adam(net.parameters(), lr=args.lr)
        base_pool = []
        for fi, f in enumerate(files):
            n = len(f[0])
            if n >= args.window:
                base_pool.extend((fi, s) for s in range(0, n - args.window + 1, args.window))
        print(
            "строк: %d, окон/эпоху: ~%d (batch=%d, window=%d, эпох=%d)"
            % (total_rows, len(base_pool), args.batch_windows, args.window, args.epochs)
        )
        if not base_pool:
            print("данных меньше одного окна")
            return 1

        steps = max(1, len(base_pool) // args.batch_windows)
        for epoch in range(1, args.epochs + 1):
            net.train()
            rng = np.random.default_rng(args.seed + epoch)
            pool = []
            for fi, f in enumerate(files):
                n = len(f[0])
                if n < args.window:
                    continue
                phase = int(rng.integers(0, args.window))
                pool.extend((fi, s) for s in range(phase, n - args.window + 1, args.window))
            agg_nll = agg_bce = 0.0
            n_batches = 0
            order = rng.permutation(len(pool))
            for b0 in range(0, min(len(order), steps * args.batch_windows), args.batch_windows):
                idx = order[b0 : b0 + args.batch_windows]
                xs, y1s, y2s, fzs = [], [], [], []
                for i in idx:
                    fi, s = pool[int(i)]
                    feats, lab_yaw, lab_pitch, freeze = files[fi][:4]
                    sl = slice(s, s + args.window)
                    xs.append(feats[sl])
                    y1s.append(lab_yaw[sl])
                    y2s.append(lab_pitch[sl])
                    fzs.append(freeze[sl])
                x = torch.from_numpy(np.stack(xs))
                y1 = torch.from_numpy(np.stack(y1s))
                y2 = torch.from_numpy(np.stack(y2s))
                fzt = torch.from_numpy(np.stack(fzs))
                o = net(x)
                nll = mdn_nll(o.reshape(-1, OUT), y1.reshape(-1), y2.reshape(-1))
                bce = F.binary_cross_entropy_with_logits(o[..., 0].reshape(-1), fzt.reshape(-1))
                loss = nll + args.freeze_weight * bce
                opt.zero_grad()
                loss.backward()
                nn.utils.clip_grad_norm_(net.parameters(), 5.0)
                opt.step()
                agg_nll += float(nll.detach())
                agg_bce += float(bce.detach())
                n_batches += 1
            if n_batches:
                print(
                    "эпоха %3d/%d: nll=%.4f bce=%.4f"
                    % (epoch, args.epochs, agg_nll / n_batches, agg_bce / n_batches)
                )

    feats_all = np.concatenate([f[0] for f in files])
    dyaw_all = np.concatenate([f[4] for f in files])
    dpitch_all = np.concatenate([f[5] for f in files])
    curve = build_error_curve(feats_all, dyaw_all, dpitch_all)

    export_bin(args.out, net, curve, args.freeze_cut, args.yaw_limit, args.pitch_limit)
    ok = verify_bin(args.out)
    print("готово: %s" % os.path.abspath(args.out))
    if ok:
        print("для игры: скопируй его в src/main/resources/assets/brokke/rotation/spacetime.bin")
    return 0 if ok else 2


if __name__ == "__main__":
    sys.exit(main())
