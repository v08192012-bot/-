#version 150

in vec2 vTexCoord;

out vec4 fragColor;

layout(std140) uniform FogData {
    mat4 uInverseProjection;
    vec4 uResolution;
    vec4 uFogColor;
    vec4 uParams;
    vec4 uFlags;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

const float SKY_DEPTH = 0.999999;
const float GOLDEN_ANGLE = 2.39996323;
const int BLUR_TAPS = 24;

float viewDistance(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = uInverseProjection * clip;
    float w = abs(view.w) < 1.0e-6 ? 1.0e-6 : view.w;
    return length(view.xyz / w);
}

vec3 blurScene(vec2 uv, float radiusPixels) {
    vec3 sum = texture(Sampler0, uv).rgb;
    if (radiusPixels < 0.75) return sum;

    vec2 texel = 1.0 / max(uResolution.xy, vec2(1.0));
    float total = 1.0;

    for (int i = 1; i <= BLUR_TAPS; i++) {
        float t = float(i) / float(BLUR_TAPS);
        float angle = float(i) * GOLDEN_ANGLE;
        vec2 offset = vec2(cos(angle), sin(angle)) * sqrt(t) * radiusPixels * texel;
        sum += texture(Sampler0, clamp(uv + offset, vec2(0.0005), vec2(0.9995))).rgb;
        total += 1.0;
    }

    return sum / total;
}

void main() {
    vec2 uv = vTexCoord;
    vec3 scene = texture(Sampler0, uv).rgb;
    float depth = texture(Sampler1, uv).r;

    float strength = clamp(uParams.z, 0.0, 1.0);
    bool sky = depth >= SKY_DEPTH;

    float fog;
    if (sky) {
        fog = (uFlags.x > 0.5 && uFlags.y > 0.5) ? strength : 0.0;
    } else {
        float spread = max(uParams.y, 0.01);
        float dist = viewDistance(uv, depth);
        float t = clamp((dist - uParams.x) / spread, 0.0, 1.0);
        fog = t * t * (3.0 - 2.0 * t) * strength;
    }

    if (fog <= 0.002) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    vec3 result = uFlags.x > 0.5
        ? mix(scene, uFogColor.rgb, fog)
        : blurScene(uv, uParams.w * fog);

    fragColor = vec4(result, 1.0);
}
