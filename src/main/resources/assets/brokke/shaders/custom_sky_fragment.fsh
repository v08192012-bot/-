#version 150

in vec2 vUV;
in vec3 vWorldDir;

out vec4 fragColor;

layout(std140) uniform SkyUniforms {
    mat4 uInverseViewProj;
    vec4 uParams;
    vec4 uExtra;
};

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float hash3(vec3 p) {
    p = fract(p * vec3(443.897, 441.423, 437.195));
    p += dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm2(vec2 p) {
    return vnoise(p) * 0.65 + vnoise(p * 2.0) * 0.35;
}

float vnoise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = hash3(i);
    float n100 = hash3(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash3(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash3(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash3(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash3(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash3(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash3(i + vec3(1.0, 1.0, 1.0));
    return mix(
        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
        f.z
    );
}

float fbm3(vec3 p) {
    return vnoise3(p) * 0.6 + vnoise3(p * 2.0) * 0.28 + vnoise3(p * 4.0) * 0.12;
}

float fbm3_fast(vec3 p) {
    return vnoise3(p) * 0.65 + vnoise3(p * 2.0) * 0.35;
}

float starField(vec3 dir, float time) {
    vec3 p = dir * 150.0;
    vec3 id = floor(p);
    vec3 f = fract(p) - 0.5;
    float rnd = hash3(id);
    if (rnd < 0.94) return 0.0;
    float size = (rnd - 0.94) * 16.666;
    float d = length(f);
    float star = smoothstep(0.35 * size, 0.0, d);
    float twinkle = sin(time * 2.5 + rnd * 62.83) * 0.35 + 0.65;
    return star * twinkle * 1.8;
}

vec3 renderNebula(vec3 dir, float time, bool starsEnabled) {
    vec3 p = dir * 2.8;
    float t = time * 0.03;
    vec3 q = vec3(
        fbm3_fast(p + vec3(t, 0.0, 0.0)),
        fbm3_fast(p + vec3(0.0, t * 0.7, 1.2)),
        fbm3_fast(p + vec3(-t * 0.5, 0.0, -0.8))
    );
    vec3 r = vec3(
        fbm3_fast(p + q * 2.0 + vec3(t * 0.5, 1.7, 0.5)),
        fbm3_fast(p + q * 2.0 + vec3(-t * 0.3, -2.1, 0.9)),
        fbm3_fast(p + q * 2.0 + vec3(0.8, -t * 0.4, -1.5))
    );
    float f = fbm3(p + r);
    vec3 baseColor = vec3(0.012, 0.006, 0.025);
    vec3 col1 = vec3(0.65, 0.12, 0.75);
    vec3 col2 = vec3(0.08, 0.65, 0.85);
    vec3 col3 = vec3(0.95, 0.45, 0.15);
    vec3 nebula = mix(baseColor, col1, clamp(f * 1.6, 0.0, 1.0));
    nebula += col2 * pow(clamp(q.x * q.y * 2.5, 0.0, 1.0), 2.0) * 1.2;
    nebula += col3 * pow(clamp(f * r.x * 2.0, 0.0, 1.0), 3.0) * 1.8;
    if (starsEnabled) {
        float s = starField(dir, time);
        nebula += vec3(s);
    }
    return nebula;
}

vec3 renderSunset(vec3 dir, float time, bool starsEnabled) {
    float h = clamp(dir.y, 0.0, 1.0);
    vec3 sunPos = normalize(vec3(0.8, 0.06, 0.2));
    float sunAngle = max(dot(dir, sunPos), 0.0);
    vec3 cHorizon = vec3(0.98, 0.38, 0.08);
    vec3 cMid = vec3(0.65, 0.18, 0.35);
    vec3 cUpper = vec3(0.25, 0.12, 0.45);
    vec3 cZenith = vec3(0.06, 0.08, 0.22);
    vec3 sky = mix(cHorizon, cMid, pow(h, 0.6));
    sky = mix(sky, cUpper, pow(h, 1.4));
    sky = mix(sky, cZenith, pow(h, 2.5));
    sky += vec3(1.0, 0.6, 0.2) * pow(sunAngle, 8.0) * 0.8;
    sky += vec3(1.0, 0.9, 0.7) * pow(sunAngle, 64.0) * 1.5;
    if (sunAngle > 0.9992) {
        sky += vec3(1.5, 1.2, 0.9);
    }
    if (starsEnabled && dir.y > 0.25) {
        float s = starField(dir, time * 0.3);
        sky += vec3(s) * (1.0 - pow(sunAngle, 2.0)) * smoothstep(0.25, 0.6, dir.y);
    }
    return sky;
}

vec3 renderGalaxy(vec3 dir, float time, bool starsEnabled) {
    vec3 sky = vec3(0.004, 0.006, 0.014);
    vec3 galNormal = normalize(vec3(0.6, 0.7, -0.3));
    float dist = abs(dot(dir, galNormal));
    float band = exp(-dist * 5.5);
    vec2 galUV = vec2(dot(dir, cross(galNormal, vec3(0.0, 1.0, 0.0))), dist);
    float dust = fbm2(galUV * 6.0 + vec2(time * 0.01));
    float gas = fbm2(galUV * 3.5 - vec2(time * 0.008));
    vec3 galBlue = vec3(0.15, 0.45, 0.85);
    vec3 galViolet = vec3(0.65, 0.2, 0.75);
    vec3 galCore = vec3(0.95, 0.8, 0.6);
    vec3 bandColor = mix(galBlue, galViolet, gas);
    bandColor = mix(bandColor, galCore, pow(band, 3.0) * 0.8);
    bandColor *= smoothstep(0.15, 0.75, dust);
    sky += bandColor * band * 1.5;
    if (starsEnabled) {
        float s1 = starField(dir, time * 0.2);
        float s2 = starField(dir * 1.7, time * 0.15) * band * 2.0;
        sky += vec3(s1 + s2);
    }
    return sky;
}

vec3 renderAurora(vec3 dir, float time, bool starsEnabled) {
    vec3 sky = vec3(0.003, 0.006, 0.015);
    vec3 p = dir * 2.2;
    float t = time * 0.4;
    float wave1 = sin(p.x * 2.0 + t + sin(p.z * 1.5 + p.y * 1.2 + t * 0.5) * 1.5);
    float wave2 = sin(p.z * 3.0 - t * 0.7 + cos(p.x * 1.8 + p.y * 1.4 - t * 0.3) * 1.2);
    float curtain = exp(-abs(wave1 * 0.6 + wave2 * 0.4) * 3.0);
    float wave3 = sin(p.x * 1.5 + p.z * 1.2 - t * 0.3 + p.y * 0.8);
    float curtain2 = exp(-abs(wave3) * 3.2);
    vec3 colGreen = vec3(0.05, 0.95, 0.45);
    vec3 colCyan = vec3(0.05, 0.75, 0.95);
    vec3 colPurple = vec3(0.75, 0.15, 0.85);
    vec3 aurora = mix(colGreen, colCyan, sin(p.x + p.z + t) * 0.5 + 0.5);
    aurora = mix(aurora, colPurple, pow(clamp(wave2, 0.0, 1.0), 2.0));
    sky += aurora * curtain * 2.4;
    sky += colGreen * curtain2 * 0.9;
    if (starsEnabled) {
        float s = starField(dir, time * 0.2);
        sky += vec3(s);
    }
    return sky;
}

vec3 renderBloodMoon(vec3 dir, float time, bool starsEnabled) {
    vec3 sky = vec3(0.02, 0.003, 0.005);
    vec3 moonPos = normalize(vec3(0.3, 0.45, 0.8));
    float moonDist = length(dir - moonPos);
    float moonDisc = smoothstep(0.095, 0.09, moonDist);
    float moonGlow = exp(-moonDist * 4.0) * 1.6 + exp(-moonDist * 1.2) * 0.4;
    float crat = fbm2(dir.xy * 25.0 + vec2(time * 0.02));
    vec3 moonCol = vec3(0.95, 0.15, 0.08) * (0.8 + crat * 0.35);
    vec3 glowCol = vec3(0.85, 0.08, 0.04);
    float cloud = fbm3_fast(dir * 3.0 + vec3(time * 0.03, 0.0, 0.0));
    vec3 bloodCloud = vec3(0.4, 0.03, 0.06) * pow(cloud, 2.0) * 2.5;
    sky += bloodCloud;
    sky += glowCol * moonGlow;
    sky += moonCol * moonDisc * 2.0;
    if (starsEnabled) {
        float s = starField(dir, time * 0.15);
        sky += vec3(s * 1.2, s * 0.2, s * 0.2) * (1.0 - moonDisc);
    }
    return sky;
}

vec3 renderDeepSpace(vec3 dir, float time, bool starsEnabled) {
    vec3 sky = vec3(0.002, 0.003, 0.006);
    vec3 p = dir * 2.2;
    float dust1 = fbm3_fast(p + vec3(time * 0.01, 0.0, 0.0));
    float dust2 = fbm3_fast(p * 1.8 - vec3(0.0, time * 0.008, 0.5));
    vec3 dustCol1 = vec3(0.08, 0.18, 0.45) * pow(dust1, 2.0) * 2.2;
    vec3 dustCol2 = vec3(0.35, 0.10, 0.42) * pow(dust2, 2.2) * 2.0;
    sky += dustCol1 + dustCol2;
    if (starsEnabled) {
        float s1 = starField(dir, time * 0.1);
        float s2 = starField(dir * 2.3 + vec3(12.3, 45.6, 78.9), time * 0.15) * 0.8;
        float s3 = starField(dir * 4.1 - vec3(98.7, 65.4, 32.1), time * 0.08) * 0.5;
        sky += vec3(s1 * 0.9, s1 * 0.95, s1 * 1.2);
        sky += vec3(s2 * 1.1, s2 * 1.0, s2 * 0.85);
        sky += vec3(s3);
    }
    return sky;
}

vec3 renderCyberpunk(vec3 dir, float time, bool starsEnabled) {
    float h = clamp(dir.y, -0.2, 1.0);
    vec3 cDeep = vec3(0.05, 0.01, 0.15);
    vec3 cMagenta = vec3(0.65, 0.05, 0.45);
    vec3 cCyan = vec3(0.0, 0.7, 0.9);
    vec3 cOrange = vec3(0.95, 0.35, 0.05);
    vec3 sky = mix(cOrange, cMagenta, smoothstep(-0.1, 0.3, h));
    sky = mix(sky, cDeep, smoothstep(0.3, 0.8, h));
    vec3 sunDir = normalize(vec3(0.0, 0.12, 1.0));
    float sunDist = length(dir - sunDir);
    float sunMask = smoothstep(0.28, 0.27, sunDist);
    if (sunMask > 0.0) {
        float scanline = sin((dir.y - 0.12) * 90.0 + time * 0.8);
        float cutoff = smoothstep(-0.2, 0.5, scanline);
        float sunGrad = (dir.y - (0.12 - 0.28)) / 0.56;
        vec3 sunColor = mix(vec3(1.0, 0.9, 0.1), vec3(1.0, 0.05, 0.5), clamp(sunGrad, 0.0, 1.0));
        sky = mix(sky, sunColor, sunMask * cutoff);
    }
    float sunGlow = exp(-sunDist * 3.5) * 1.2;
    sky += vec3(1.0, 0.1, 0.6) * sunGlow;
    if (dir.y > 0.05 && starsEnabled) {
        float s = starField(dir, time * 0.25);
        sky += vec3(s * 0.5, s * 0.9, s * 1.2) * (1.0 - exp(-sunDist * 2.0));
    }
    return sky;
}

void main() {
    vec3 dir = normalize(vWorldDir);
    float animTime = uParams.x * uParams.z;
    int mode = int(uParams.y + 0.5);
    bool stars = uExtra.x > 0.5;

    vec3 col = vec3(0.0);
    if (mode == 0) {
        col = renderNebula(dir, animTime, stars);
    } else if (mode == 1) {
        col = renderSunset(dir, animTime, stars);
    } else if (mode == 2) {
        col = renderGalaxy(dir, animTime, stars);
    } else if (mode == 3) {
        col = renderAurora(dir, animTime, stars);
    } else if (mode == 4) {
        col = renderBloodMoon(dir, animTime, stars);
    } else if (mode == 5) {
        col = renderDeepSpace(dir, animTime, stars);
    } else if (mode == 6) {
        col = renderCyberpunk(dir, animTime, stars);
    }

    if (uExtra.z > 0.5 && (mode == 1 || mode == 6)) {
        float horizonMask = smoothstep(-0.25, 0.05, dir.y);
        vec3 bottomColor = col * 0.25;
        col = mix(bottomColor, col, horizonMask);
    }

    col *= mix(0.35, 1.0, uExtra.y);
    col *= uParams.w;

    fragColor = vec4(col, 1.0);
}
