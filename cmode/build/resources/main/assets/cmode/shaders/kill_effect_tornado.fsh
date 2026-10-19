#version 330

layout(std140) uniform BeamData {
    float uTime;
    float uProgress;
    vec4  uColor;
    float uIntensity;
    float uSize;
};

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(443.8975, 397.2973));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float smoothNoise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0,0.0)), f.x),
        mix(hash21(i + vec2(0.0,1.0)), hash21(i + vec2(1.0,1.0)), f.x),
        f.y
    );
}

float fbm2D(vec2 p, int octaves) {
    float v = 0.0, a = 0.5, freq = 1.0;
    for (int k = 0; k < octaves; k++) {
        v += a * smoothNoise2D(p * freq);
        freq *= 2.03;
        a *= 0.48;
    }
    return v;
}

void main() {
    if (texCoord.y < 0.0) discard;

    float x = texCoord.x * 2.0 - 1.0;
    float y = texCoord.y;
    float t = uTime;
    float prog = clamp(uProgress, 0.0, 1.0);

    float baseFade = smoothstep(0.0, 0.05, y);
    float topFade = 1.0 - smoothstep(0.55, 0.98, y);

    float rise = 0.10 + prog * 1.0;
    float growing = 1.0 - smoothstep(rise - 0.12, rise, y);
    float body = growing * topFade * baseFade;
    if (body <= 0.001) discard;

    float radius = mix(0.10, 0.95, pow(y, 1.4));
    float r = abs(x) / max(radius, 1.0e-4);

    float wall = 1.0 - smoothstep(0.0, 1.0, abs(r - 0.72) * 2.2);

    vec2 sw = vec2(x * 3.0, y * 6.0 - t * 2.5);
    float twirl = fbm2D(sw + vec2(t * 0.3, 0.0), 4);
    float bands = smoothstep(0.30, 0.80, twirl + 0.2 * sin(x * 8.0 + y * 12.0 - t * 4.0));

    float core = exp(-(x * x) / (2.0 * 0.11 * 0.11)) * smoothstep(0.0, 0.10, y);

    float particles = 0.0;
    for (int i = 0; i < 20; i++) {
        float seed = float(i) * 1.37 + 3.1;
        float hgt = fract(t * 0.35 + hash21(vec2(seed, 0.0)) * 0.8);
        if (hgt > rise - 0.1) continue;
        float angular = (hgt * 3.0 + t) * 3.0;
        float px = sin(angular + seed) * 0.55;
        float sz = 0.03 + 0.05 * hash21(vec2(seed, 1.0));
        vec2 dc = vec2((x - px) * 1.2, (y - hgt) * 2.0);
        particles += exp(-dot(dc, dc) / (2.0 * sz * sz)) * 0.7;
    }

    float glow = exp(-abs(x) * 2.6) * 0.4 * (0.6 + 0.4 * sin(t * 2.0));

    vec3 base = uColor.rgb;
    vec3 spiralColor = base;
    vec3 coreColor = mix(base, vec3(1.0), 0.75);
    vec3 particleColor = mix(base, vec3(1.0), 0.45);
    vec3 glowColor = mix(base, vec3(1.0), 0.25);

    vec3 finalColor = spiralColor * bands * wall * 1.30
                    + coreColor * core * 1.60
                    + particleColor * particles * 1.00
                    + glowColor * glow * 1.10;

    float fin = (wall * bands * 0.80 + core + particles * 0.50 + glow * 0.30) * body;
    float alpha = clamp(fin * 1.20, 0.0, 1.0) * vertexColor.a;

    float fadeIn = smoothstep(0.0, 0.08, prog);
    float fadeOut = 1.0 - smoothstep(0.60, 1.0, prog);
    alpha *= fadeIn * fadeOut * uIntensity;

    if (alpha < 0.01) discard;
    fragColor = vec4(clamp(finalColor * uIntensity, 0.0, 1.0), alpha);
}
