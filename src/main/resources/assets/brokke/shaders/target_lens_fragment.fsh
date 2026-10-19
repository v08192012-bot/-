#version 150

layout(std140) uniform TargetLensUniforms {
    float Count;
    float Aspect;
    float Strength;
    float _pad;
    vec4 Lens[18];
};

in vec2 vTexCoord;
out vec4 OutColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

const int MAX_LENSES = 18;
const float PULL = 0.85;
const float CHROMA = 0.35;

void main() {
    vec2 uv = vTexCoord;
    float sceneDepth = texture(Sampler1, uv).r;

    int count = int(Count + 0.5);
    vec2 offset = vec2(0.0);

    for (int i = 0; i < MAX_LENSES; i++) {
        if (i >= count) break;
        vec4 c = Lens[i];
        if (c.w <= 0.0) continue;

        vec2 d = (uv - c.xy) * vec2(Aspect, 1.0);
        float len = length(d);
        float t = len / c.w;
        if (t >= 1.0 || len < 1e-5) continue;

        if (c.z <= 0.0 || c.z >= 1.0) continue;
        if (sceneDepth < c.z - 0.0005) continue;

        float p = t * (1.0 - t) * (1.0 - t) * 6.75;

        vec2 dir = d / len;
        offset -= (dir / vec2(Aspect, 1.0)) * (p * c.w * PULL * Strength);
    }

    if (dot(offset, offset) < 1e-12) {
        discard;
    }

    vec2 uvR = clamp(uv + offset * (1.0 - CHROMA), vec2(0.001), vec2(0.999));
    vec2 uvG = clamp(uv + offset, vec2(0.001), vec2(0.999));
    vec2 uvB = clamp(uv + offset * (1.0 + CHROMA), vec2(0.001), vec2(0.999));

    OutColor = vec4(texture(Sampler0, uvR).r, texture(Sampler0, uvG).g, texture(Sampler0, uvB).b, 1.0);
}
