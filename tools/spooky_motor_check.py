#!/usr/bin/env python3
"""Независимая проверка motor.bin: парсит формат ZENMOTOR и гоняет golden-тики
через forward-pass ровно в том порядке, что и SpookyModel.java (SiLU, GRU 2 слоя).
Смысл — убедиться, что бинарник целый и семантика порта совпадает с эталоном."""
import math
import struct
import sys
from array import array

path = sys.argv[1] if len(sys.argv) > 1 else "motor.bin"
raw = open(path, "rb").read()
off = 0


def take(n):
    global off
    b = raw[off:off + n]
    off += n
    return b


def i32():
    return struct.unpack("<i", take(4))[0]


def f64():
    return struct.unpack("<d", take(8))[0]


def doubles(n):
    a = array("d")
    a.frombytes(take(8 * n))
    if sys.byteorder != "little":
        a.byteswap()
    return a


assert take(8) == b"ZENMOTOR", "signature"
ver = i32()
assert ver == 1, ver
n_features, mlp, gru_h, gru_l, mix, latency = (i32() for _ in range(6))
asinh_s, act_yaw, act_pitch, inside_sharp, ls_min, ls_max, max_yaw, max_pitch, peak_decay, zero_eps = (
    f64() for _ in range(10)
)
quant_round = take(1)[0] != 0
residual = take(1)[0] != 0
has_env = take(1)[0] != 0
env = None
if has_env:
    env = (doubles(i32()), doubles(i32()), doubles(i32()))
mean = doubles(n_features)
var = doubles(n_features)
inp = []
for i in range(i32()):
    rows, cols = i32(), i32()
    inp.append((doubles(rows * cols), doubles(rows), rows, cols))
gru = []
for i in range(i32()):
    h, in_dim = i32(), i32()
    gru.append((doubles(3 * h * in_dim), doubles(3 * h), doubles(3 * h * h), doubles(3 * h), h, in_dim))
heads = []
for _ in range(5):
    rows, cols = i32(), i32()
    heads.append((doubles(rows * cols), doubles(rows), rows, cols))
ngold = i32()
golden = []
for _ in range(ngold):
    g = {
        "f": doubles(n_features),
        "pi": doubles(mix),
        "mu": doubles(2 * mix),
        "ls": doubles(2 * mix),
        "rho": doubles(mix),
        "pause": f64(),
        "hidden": doubles(gru_l * gru_h),
    }
    golden.append(g)
assert off == len(raw), (off, len(raw))


def sigmoid(z):
    return 1.0 / (1.0 + math.exp(-z))


def silu(z):
    return z / (1.0 + math.exp(-z))


def linear(w, b, x):
    n = len(x)
    out = [0.0] * len(b)
    for i in range(len(b)):
        acc = b[i]
        o = i * n
        for j in range(n):
            acc += w[o + j] * x[j]
        out[i] = acc
    return out


hidden = [[0.0] * gru_h for _ in range(gru_l)]


def forward(feat):
    x = [(feat[i] - mean[i]) / math.sqrt(var[i] + 1e-4) for i in range(n_features)]
    for w, b, rows, cols in inp:
        x = [silu(v) for v in linear(w, b, x)]
    for layer in range(gru_l):
        wih, bih, whh, bhh, h, in_dim = gru[layer]
        n = linear(wih, bih, x)
        g = linear(whh, bhh, hidden[layer])
        nxt = [0.0] * h
        for j in range(h):
            r = sigmoid(n[j] + g[j])
            z = sigmoid(n[h + j] + g[h + j])
            c = math.tanh(n[2 * h + j] + r * g[2 * h + j])
            nxt[j] = (1.0 - z) * c + z * hidden[layer][j]
        hidden[layer] = nxt
        x = nxt
    pi = linear(heads[0][0], heads[0][1], x)
    mu = linear(heads[1][0], heads[1][1], x)
    ls = linear(heads[2][0], heads[2][1], x)
    rho = linear(heads[3][0], heads[3][1], x)
    pause = linear(heads[4][0], heads[4][1], x)[0]
    ls = [min(max(v, ls_min), ls_max) for v in ls]
    rho = [math.tanh(v) * 0.95 for v in rho]
    hidden_flat = [v for layer in hidden for v in layer]
    return pi, mu, ls, rho, pause, hidden_flat


# эталонные тики идут подряд: состояние GRU между ними НЕ сбрасывается
worst = 0.0
for g in golden:
    pi, mu, ls, rho, pause, hflat = forward(g["f"])
    for got, want in ((pi, g["pi"]), (mu, g["mu"]), (ls, g["ls"]), (rho, g["rho"]), (hflat, g["hidden"])):
        worst = max(worst, max(abs(a - b) for a, b in zip(got, want)))
    worst = max(worst, abs(pause - g["pause"]))

print(f"n_features={n_features} mlp={mlp} gru={gru_l}x{gru_h} mix={mix} latency={latency}")
print(f"envelope={'on' if has_env else 'off'} quant={'round' if quant_round else 'trunc'} residual={residual}")
print(f"golden ticks={len(golden)}  max delta={worst:.3e}")
sys.exit(0 if worst <= 1e-9 else 1)
