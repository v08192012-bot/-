#version 150

layout(std140) uniform NoiseData {
    vec4 TintColor;
    vec4 uParams;
    vec4 uExtra;
    vec4 uPadding;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

in vec2 vTexCoord;
out vec4 OutColor;

float noiseMask(vec2 uv) {
    float now = texture(Sampler1, uv).r;
    float was = texture(Sampler2, uv).r;
    return smoothstep(0.0001, 0.0004, was - now);
}

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float hash21(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}

const mat2 ROT = mat2(0.80, 0.60, -0.60, 0.80);

float fbmRot(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = ROT * p * 2.07;
        a *= 0.5;
    }
    return v;
}

float surfaceSlime(vec2 p, float t, float goo) {
    vec2 q = p * 2.6;
    vec2 warp = vec2(fbmRot(q + vec2(1.7, t * 0.14)), fbmRot(q + vec2(-4.3, t * 0.10)));
    float body = fbmRot(q + warp * 1.7 + vec2(0.0, t * 0.16));

    vec2 r = p + warp * 0.06;
    float runnel = vnoise(vec2(r.x * 13.0, r.y * 2.0 + t * 0.50)) * 0.65
                 + vnoise(vec2(r.x * 29.0, r.y * 4.2 + t * 0.78)) * 0.35;
    runnel = smoothstep(0.30, 0.88, runnel);

    float bead = vnoise(vec2(r.x * 13.0, r.y * 9.0 + t * 0.50));
    runnel *= 0.72 + 0.55 * bead;

    return clamp(body * 0.58 + runnel * 0.42 * goo, 0.0, 1.0);
}

float starfieldWinlend(vec2 p, float scale, float threshold, float t) {
    vec2 g = p * scale;
    vec2 cell = floor(g);
    vec2 f = fract(g);
    float h = hash21(cell);
    vec2 pos = vec2(hash21(cell + 11.3), hash21(cell + 27.7));
    float core = smoothstep(0.14, 0.0, length(f - pos));
    float twinkle = 0.45 + 0.55 * sin(t * 2.6 + h * 47.0);
    return core * twinkle * step(threshold, h);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.02 + vec2(8.4, 5.7);
        a *= 0.5;
    }
    return v;
}

float ridged(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += r * a;
        p = p * 2.18 + vec2(3.1, 9.2);
        a *= 0.52;
    }
    return v;
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec4 renderRadiation(vec2 uv, float t, vec3 baseColor) {
    vec2 flow = uv * 2.5;
    vec2 drift = vec2(t * 0.20, -t * 0.15);

    vec2 warp = vec2(
        fbm(flow * 0.90 + drift * 0.75 + vec2(0.0, 4.1)),
        fbm(flow * 0.78 - drift * 0.48 + vec2(3.7, 1.8))
    );
    vec2 q = flow + (warp - 0.5) * 1.8;

    float mist = fbm(q * 0.72 - drift * 0.24 + vec2(4.2, 8.1));
    float veins = pow(clamp(ridged(q * 1.85 + vec2(mist * 2.5, mist * 1.6) - drift * 0.55), 0.0, 1.0), 2.4);
    float sA = pow(clamp(1.0 - abs(sin((q.x * 1.08 + q.y * 0.42) * 1.7 + t * 0.85 + mist * 4.3)), 0.0, 1.0), 4.8);
    float sB = pow(clamp(1.0 - abs(sin((q.x * -0.58 + q.y * 1.12) * 1.45 - t * 0.65 - mist * 2.9)), 0.0, 1.0), 5.4);

    float energy = clamp(mist * 0.22 + veins * 0.88 + sA * 0.55 + sB * 0.32, 0.0, 1.0);
    float core = smoothstep(0.18, 0.98, energy);
    float accent = pow(clamp(max(veins, sA), 0.0, 1.0), 1.25);

    vec3 col = mix(baseColor, mix(baseColor, vec3(1.0), 0.4), clamp(core * 0.75 + sB * 0.25, 0.0, 1.0));
    float fill = 0.26 + core * 0.82 + accent * 0.28;
    return vec4(col, fill);
}

vec4 renderPlasma(vec2 uv, float t, vec3 baseColor) {
    vec2 p = uv * 4.0;
    float v1 = sin(p.x + t * 1.2);
    float v2 = sin(p.y - t * 0.9);
    float v3 = sin(p.x + p.y + t * 0.7);
    vec2 c = p + vec2(sin(t * 0.4), cos(t * 0.3)) * 1.5;
    float v4 = sin(length(c) * 2.2 - t * 1.8);
    float plasma = (v1 + v2 + v3 + v4) * 0.25;

    float rings = sin(plasma * 6.28318);
    vec3 colA = baseColor;
    vec3 colB = mix(baseColor, vec3(1.0) - baseColor, 0.5);
    vec3 colC = vec3(0.9, 0.95, 1.0);

    vec3 col = mix(colA, colB, plasma * 0.5 + 0.5);
    col = mix(col, colC, pow(clamp(rings * 0.5 + 0.5, 0.0, 1.0), 4.0) * 0.7);

    float fill = 0.45 + 0.55 * (plasma * 0.5 + 0.5);
    return vec4(col, fill);
}

vec4 renderRainbow(vec2 uv, float t) {
    vec2 p = uv - 0.5;
    float dist = length(p);
    float angle = atan(p.y, p.x);

    float ripple = sin(dist * 20.0 - t * 3.5 + angle * 2.0);
    float hue = fract(uv.x * 0.7 + uv.y * 0.5 + t * 0.25 + ripple * 0.1);
    vec3 col = hsv2rgb(vec3(hue, 0.85, 1.0));

    float waveHigh = pow(clamp(ripple * 0.5 + 0.5, 0.0, 1.0), 3.0);
    col = mix(col, vec3(1.0), waveHigh * 0.45);

    float fill = 0.5 + 0.5 * waveHigh;
    return vec4(col, fill);
}

vec4 renderNebula(vec2 uv, float t, vec3 baseColor, float aspect) {
    vec2 p = vec2(uv.x * aspect, uv.y) * 2.2;
    float tt = t * 0.15;
    vec2 q = vec2(fbm(p * 1.5 + vec2(tt, -tt * 0.7)), fbm(p * 1.5 - vec2(tt * 0.7, tt * 0.5)));
    vec2 r = vec2(fbm(p * 2.5 + q * 2.0 + vec2(tt * 0.5, -tt * 0.3)), fbm(p * 2.5 - q * 2.0 - vec2(tt * 0.3, tt * 0.4)));
    float f = fbm(p * 1.8 + r);

    vec3 col1 = baseColor;
    vec3 col2 = mix(baseColor, vec3(0.08, 0.65, 0.85), 0.6);
    vec3 col3 = mix(baseColor, vec3(0.95, 0.45, 0.15), 0.5);

    vec3 neb = mix(vec3(0.02, 0.01, 0.03), col1, clamp(f * 1.6, 0.0, 1.0));
    neb += col2 * pow(clamp(q.x * q.y * 2.5, 0.0, 1.0), 2.0) * 1.2;
    neb += col3 * pow(clamp(f * r.x * 2.0, 0.0, 1.0), 3.0) * 1.8;

    vec2 starP = uv * vec2(aspect, 1.0) * 50.0;
    vec2 sid = floor(starP);
    float sh = hash2(sid);
    float star = 0.0;
    if (sh > 0.93) {
        vec2 sgv = fract(starP) - 0.5;
        float twinkle = 0.5 + 0.5 * sin(t * 4.0 + sh * 62.8);
        star = smoothstep(0.12, 0.01, length(sgv)) * twinkle * 1.5;
    }
    neb += vec3(star);

    float fill = clamp(0.35 + f * 0.5 + star, 0.0, 1.0);
    return vec4(neb, fill);
}

vec4 renderSunset(vec2 uv, float t, vec3 baseColor) {
    float h = clamp(1.0 - uv.y, 0.0, 1.0);
    vec3 cHorizon = vec3(0.98, 0.38, 0.08);
    vec3 cMid = mix(baseColor, vec3(0.65, 0.18, 0.35), 0.5);
    vec3 cUpper = vec3(0.25, 0.12, 0.45);
    vec3 cZenith = vec3(0.06, 0.08, 0.22);

    vec3 sky = mix(cHorizon, cMid, pow(h, 0.6));
    sky = mix(sky, cUpper, pow(h, 1.4));
    sky = mix(sky, cZenith, pow(h, 2.5));

    vec2 sunPos = vec2(0.5 + 0.3 * cos(t * 0.3), 0.3 + 0.1 * sin(t * 0.2));
    float sunDist = length(uv - sunPos);
    float sunFlare = exp(-sunDist * 5.0) * 0.9 + exp(-sunDist * 18.0) * 1.5;
    sky += vec3(1.0, 0.75, 0.35) * sunFlare;

    float cloud = fbm(uv * vec2(3.0, 6.0) + vec2(t * 0.08, 0.0));
    sky = mix(sky, mix(cHorizon, vec3(1.0, 0.9, 0.8), 0.5), cloud * 0.35 * smoothstep(0.2, 0.7, h));

    float fill = clamp(0.45 + sunFlare * 0.5 + cloud * 0.2, 0.0, 1.0);
    return vec4(sky, fill);
}

vec4 renderGalaxy(vec2 uv, float t, vec3 baseColor, float aspect) {
    vec2 p = vec2(uv.x * aspect, uv.y) * 2.0;
    vec2 center = vec2(0.5 * aspect, 0.5) * 2.0;
    vec2 d = p - center;
    float dist = length(d);
    float angle = atan(d.y, d.x);

    float swirl = angle + dist * 1.8 - t * 0.3;
    vec2 swP = vec2(cos(swirl), sin(swirl)) * dist;

    float dust = fbm(swP * 3.5 + vec2(t * 0.04, -t * 0.03));
    float gas = fbm(swP * 2.0 - vec2(t * 0.05, t * 0.02));

    vec3 galBlue = mix(baseColor, vec3(0.15, 0.45, 0.85), 0.6);
    vec3 galViolet = vec3(0.65, 0.2, 0.75);
    vec3 galCore = vec3(0.95, 0.85, 0.65);

    vec3 bandColor = mix(galBlue, galViolet, gas);
    float corePulse = exp(-dist * 2.5);
    bandColor = mix(bandColor, galCore, corePulse * 0.85);
    bandColor *= smoothstep(0.15, 0.75, dust);

    vec2 starP1 = uv * vec2(aspect, 1.0) * 45.0;
    vec2 id1 = floor(starP1);
    float h1 = hash2(id1);
    float star1 = 0.0;
    if (h1 > 0.92) {
        vec2 gv1 = fract(starP1) - 0.5;
        float twinkle = 0.5 + 0.5 * sin(t * 5.0 + h1 * 62.8);
        star1 = smoothstep(0.12, 0.01, length(gv1)) * twinkle;
    }

    vec3 sky = vec3(0.01, 0.015, 0.03) + bandColor * 1.4 + vec3(star1 * 1.2);
    float fill = clamp(0.35 + corePulse * 0.5 + dust * 0.35 + star1, 0.0, 1.0);
    return vec4(sky, fill);
}

vec4 renderLiquidGlass(vec2 uv, float t, vec3 baseColor, vec2 origUv) {
    vec2 p = uv - 0.5;
    float dist = length(p);
    float angle = atan(p.y, p.x);

    float wave = sin(dist * 12.0 - t * 2.5 + angle * 2.0) * 0.03;
    vec2 distortUv = origUv + normalize(p + vec2(1.0e-4)) * wave;
    vec3 sceneCol = texture(Sampler0, clamp(distortUv, 0.001, 0.999)).rgb;

    float fresnel = pow(clamp(dist * 2.0, 0.0, 1.0), 2.2);
    float sheen = pow(clamp(sin((uv.x + uv.y) * 9.0 - t * 2.0) * 0.5 + 0.5, 0.0, 1.0), 6.0);

    vec3 col = mix(sceneCol, baseColor, 0.45);
    col = mix(col, vec3(1.0), fresnel * 0.4 + sheen * 0.6);

    float fill = clamp(0.4 + fresnel * 0.45 + sheen * 0.4, 0.0, 1.0);
    return vec4(col, fill);
}

vec4 renderGlow(vec2 uv, float t, vec3 baseColor, vec2 origUv, vec2 texel) {
    float waveOffsetX = sin(t * 2.5 + uv.y * 8.0) * 0.025;
    float waveOffsetY = cos(t * 1.8 + uv.x * 6.0) * 0.018;

    float trailOffset1 = 0.02 * sin(t * 3.5 + uv.y * 12.0);
    float trailOffset2 = 0.015 * cos(t * 2.5 + uv.x * 9.0);

    float sparkOffsetX = 0.03 * sin(t * 5.0 + uv.y * 20.0);
    float sparkOffsetY = 0.025 * cos(t * 4.0 + uv.x * 14.0);

    vec2 waveUv = uv + vec2(waveOffsetX, waveOffsetY);
    vec2 trailUv = uv + vec2(trailOffset1, trailOffset2);
    vec2 sparkUv = uv + vec2(sparkOffsetX, sparkOffsetY);

    float b1 = fbm(waveUv * 2.8 + vec2(0.0, -t * 0.4));
    float b2 = fbm(trailUv * 4.0 + vec2(t * 0.3, -t * 0.5));
    float b3 = fbm(sparkUv * 5.5 + vec2(-t * 0.4, -t * 0.7));

    float outer = b1 * 0.5 + b2 * 0.3 + b3 * 0.2;

    float wavePulse = 0.75 + 0.25 * sin(t * 2.0 + uv.x * 10.0 + uv.y * 8.0);
    float breathPulse = 0.88 + 0.12 * sin(t * 1.0);
    outer *= wavePulse * breathPulse;

    vec2 e = texel * 4.0;
    float mCenter = noiseMask(origUv);
    float mEdge = min(min(noiseMask(origUv + vec2(e.x, 0.0)), noiseMask(origUv - vec2(e.x, 0.0))),
                      min(noiseMask(origUv + vec2(0.0, e.y)), noiseMask(origUv - vec2(0.0, e.y))));
    float rim = clamp(mCenter - mEdge, 0.0, 1.0);

    vec3 darkCol = baseColor * 0.3;
    vec3 grad = mix(baseColor, darkCol, uv.y);
    vec3 sparkColor = mix(baseColor, vec3(1.0), 0.6) * 1.35;

    vec3 col = mix(grad, sparkColor, clamp(b3 * 1.4 - 0.3, 0.0, 1.0));
    col += baseColor * pow(outer, 2.0) * 0.9;
    col += vec3(1.0) * pow(clamp(outer - 0.25, 0.0, 1.0), 3.0) * 1.4;
    col += sparkColor * pow(rim, 1.5) * 1.5;

    float fill = clamp(0.55 + outer * 0.55 + rim * 0.4, 0.0, 1.0);
    return vec4(col, fill);
}

vec4 renderSlime(vec2 uv, float t, vec3 baseColor, float aspect) {
    float goo = 1.0;
    vec2 p = uv * vec2(aspect, 1.0) * 1.5;

    float d = 0.0035;
    float h = surfaceSlime(p, t, goo);
    vec2 grad = vec2(h - surfaceSlime(p + vec2(d, 0.0), t, goo),
                     h - surfaceSlime(p + vec2(0.0, d), t, goo)) * 48.0;

    vec3 n = normalize(vec3(grad, 1.0));
    vec3 lightDir = normalize(vec3(-0.38, 0.66, 0.65));
    vec3 halfDir = normalize(lightDir + vec3(0.0, 0.0, 1.0));
    float ndh = max(dot(n, halfDir), 0.0);
    float wrap = clamp(dot(n, lightDir) * 0.5 + 0.5, 0.0, 1.0);
    float sss = pow(wrap, 1.5);
    float spec = pow(ndh, 34.0) * 0.75 + pow(ndh, 200.0) * 1.60;
    float fresnel = pow(1.0 - clamp(n.z, 0.0, 1.0), 3.0);

    vec3 hue = baseColor / max(max(baseColor.r, max(baseColor.g, baseColor.b)), 0.001);
    vec3 deep = mix(hue * 0.34, vec3(0.10, 0.11, 0.14), 0.20);
    vec3 body = mix(hue, vec3(1.0), 0.42);
    vec3 cream = mix(hue, vec3(1.0), 0.88);

    float bubbles = smoothstep(0.62, 0.93, fbmRot(p * 11.0 + vec2(0.0, t * 0.22)));

    vec3 col = mix(deep, body, sss);
    col = mix(col, cream, pow(h, 2.0) * 0.65);
    col = mix(col, cream, bubbles * 0.35);
    col += vec3(1.0) * spec;
    col += cream * fresnel * 0.35;

    float fill = clamp(0.65 + h * 0.35 + spec * 0.3, 0.0, 1.0);
    return vec4(col, fill);
}

vec4 renderFire(vec2 uv, float t, vec3 baseColor) {
    vec2 fireUv = uv;
    fireUv.x += sin(t * 1.5 + uv.y * 4.0) * 0.025;
    fireUv.y += cos(t * 1.2 + uv.x * 3.0) * 0.02;

    vec2 fireCoord1 = fireUv * 2.8 + vec2(t * 0.2, t * 0.9);
    float fire1 = fbm(fireCoord1);

    vec2 fireCoord2 = fireUv * 2.0 + vec2(-t * 0.15, t * 0.7);
    float fire2 = fbm(fireCoord2 + vec2(1.7, 3.4));

    vec2 fireCoord3 = fireUv * 3.8 - vec2(t * 0.1, t * 1.0);
    float fire3 = fbm(fireCoord3 + vec2(5.2, 1.1));

    float finalFire = fire1 * 0.5 + fire2 * 0.3 + fire3 * 0.2;

    float sideMovement = sin(t * 0.9 + uv.y * 5.0) * 0.5 + 0.5;
    finalFire = mix(finalFire, finalFire * (0.7 + 0.3 * sideMovement), 0.3);

    float flameHeight = clamp(1.0 - uv.y, 0.0, 1.0);
    flameHeight = pow(flameHeight, 0.5 + 0.3 * sin(t * 0.8 + uv.x * 10.0));
    finalFire *= flameHeight;

    finalFire *= (0.8 + 0.2 * sin(t * 2.0 + uv.x * 20.0 + uv.y * 20.0));

    vec3 cDark = mix(baseColor * 0.35, vec3(0.7, 0.08, 0.02), 0.5);
    vec3 cMid = mix(baseColor, vec3(1.0, 0.45, 0.05), 0.5);
    vec3 cBright = vec3(1.0, 0.88, 0.25);
    vec3 cWhite = vec3(1.0, 0.98, 0.88);

    vec3 fireColor = mix(cDark, cMid, clamp(finalFire * 1.6, 0.0, 1.0));
    fireColor = mix(fireColor, cBright, clamp(finalFire * 2.4 - 0.5, 0.0, 1.0));
    fireColor = mix(fireColor, cWhite, clamp(finalFire * 3.2 - 1.3, 0.0, 1.0));

    vec2 sparkP = uv * vec2(22.0, 14.0) - vec2(0.0, t * 3.8);
    vec2 sparkId = floor(sparkP);
    float sparkH = hash(sparkId);
    float spark = 0.0;
    if (sparkH > 0.87) {
        vec2 gv = fract(sparkP) - 0.5;
        spark = smoothstep(0.14, 0.01, length(gv)) * (0.5 + 0.5 * sin(t * 7.5 + sparkH * 25.0));
    }
    fireColor += vec3(1.0, 0.92, 0.55) * spark * 1.6;

    float fill = clamp(finalFire * 1.7 + spark * 0.5, 0.0, 1.0);
    return vec4(fireColor, fill);
}

vec4 renderTrail(vec2 uv, float t, vec3 baseColor, vec2 origUv) {
    float flow = noise(uv * vec2(6.0, 3.0) + vec2(0.0, -t * 0.7));
    float flowSide = noise(uv * vec2(11.0, 5.0) + vec2(7.3, -t * 1.2));
    vec2 sway = vec2((flowSide - 0.5) * 0.04, flow * 0.06);

    vec2 cell = floor((uv + sway * 0.5) * 140.0);
    float crumb = hash(cell);
    float dissolve = smoothstep(0.18, 0.82, sin(crumb * 6.28 + t * 2.2) * 0.5 + 0.5);

    vec2 driftUv1 = origUv - sway * 0.6;
    vec2 driftUv2 = origUv - sway * 1.2;
    vec3 s0 = texture(Sampler0, clamp(origUv, 0.001, 0.999)).rgb;
    vec3 s1 = texture(Sampler0, clamp(driftUv1, 0.001, 0.999)).rgb;
    vec3 s2 = texture(Sampler0, clamp(driftUv2, 0.001, 0.999)).rgb;

    vec3 ghost = mix(s1, baseColor, 0.52) * 0.6 + mix(s2, baseColor, 0.75) * 0.4;
    vec3 col = mix(s0, ghost, 0.7);
    col += baseColor * dissolve * 0.4;
    col += vec3(1.0) * pow(dissolve, 4.0) * 0.6;

    float fill = clamp(0.5 + dissolve * 0.4 + flow * 0.2, 0.0, 1.0);
    return vec4(col, fill);
}

vec4 renderCosmos(vec2 uv, float t, vec3 baseColor, float aspect) {
    vec2 p = uv * vec2(aspect, 1.0);
    vec2 q = p * 2.6;
    vec2 warp = vec2(
        fbmRot(q + vec2(0.0, t * 0.09)),
        fbmRot(q + vec2(4.7, -t * 0.07))
    );
    float clouds = fbmRot(q + warp * 1.9 + vec2(t * 0.05, -t * 0.03));
    float wisps = fbmRot(q * 2.4 - warp * 1.2 + vec2(-t * 0.08, t * 0.04));

    clouds = pow(clamp(clouds * 1.25, 0.0, 1.0), 1.7);
    wisps = pow(clamp(wisps * 1.15, 0.0, 1.0), 3.0);

    vec3 deep = mix(vec3(0.015, 0.010, 0.045), baseColor * 0.10, 0.65);
    vec3 body = baseColor;
    vec3 alt = baseColor.gbr;
    vec3 hot = mix(baseColor, vec3(1.0), 0.7);

    vec3 col = deep;
    col = mix(col, body, clouds);
    col = mix(col, alt * 1.15, wisps * 0.5);
    col += hot * pow(clouds, 4.0) * 0.85;

    float st = starfieldWinlend(p + vec2(t * 0.010, -t * 0.004), 120.0, 0.978, t)
             + starfieldWinlend(p + vec2(-t * 0.018, t * 0.007), 70.0, 0.986, t * 1.4) * 1.6;
    col += vec3(0.85, 0.90, 1.0) * st * 1.6;

    float fill = clamp(0.42 + clouds * 0.5 + wisps * 0.3 + st * 0.8, 0.0, 1.0);
    return vec4(col, fill);
}

void main() {
    vec2 uv = vTexCoord;
    float mask = noiseMask(uv);

    if (mask < 0.01) discard;

    float t = uParams.x * uParams.w;
    float scale = max(uExtra.x, 0.01);
    vec2 scaledUv = (uv - 0.5) / scale + 0.5;
    float aspect = max(uExtra.z, 0.01);
    vec2 texel = vec2(max(uExtra.w, 0.0001), max(uPadding.x, 0.0001));

    int mode = int(uParams.z + 0.5);
    bool isRainbow = (uExtra.y > 0.5) || (mode == 2);

    vec3 baseColor = TintColor.rgb;
    if (isRainbow && mode != 2) {
        baseColor = hsv2rgb(vec3(fract(t * 0.2 + uv.x * 0.3 + uv.y * 0.2), 0.85, 1.0));
    }

    vec4 res;
    if (mode == 0) {
        res = renderRadiation(scaledUv, t, baseColor);
    } else if (mode == 1) {
        res = renderPlasma(scaledUv, t, baseColor);
    } else if (mode == 2) {
        res = renderRainbow(scaledUv, t);
    } else if (mode == 3) {
        res = renderNebula(scaledUv, t, baseColor, aspect);
    } else if (mode == 4) {
        res = renderSunset(scaledUv, t, baseColor);
    } else if (mode == 5) {
        res = renderGalaxy(scaledUv, t, baseColor, aspect);
    } else if (mode == 6) {
        res = renderLiquidGlass(scaledUv, t, baseColor, uv);
    } else if (mode == 7) {
        res = renderGlow(scaledUv, t, baseColor, uv, texel);
    } else if (mode == 8) {
        res = renderSlime(scaledUv, t, baseColor, aspect);
    } else if (mode == 9) {
        res = renderFire(scaledUv, t, baseColor);
    } else if (mode == 10) {
        res = renderTrail(scaledUv, t, baseColor, uv);
    } else if (mode == 11) {
        res = renderCosmos(scaledUv, t, baseColor, aspect);
    } else {
        res = renderRadiation(scaledUv, t, baseColor);
    }

    float outA = clamp(TintColor.a * res.a * mask, 0.0, 1.0);
    if (outA <= 0.001) discard;

    OutColor = vec4(res.rgb * res.a, outA);
}
