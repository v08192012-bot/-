#version 150
in vec2 vTexCoord;
out vec4 fragColor;

layout(std140) uniform Uniforms {
    vec4 uResolutionTexel;
    vec4 uParams;
    vec4 uColor;
    vec4 uHandCenters;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

float isHand(vec2 p) {
    float now = texture(Sampler1, p).r;
    float was = texture(Sampler2, p).r;
    if ((was - now) <= 0.0001) {
        return 0.0;
    }

    int activeArm = int(uParams.z + 0.5);
    if (activeArm < 0) {
        return 1.0;
    }

    float aspect = uParams.w;
    vec2 pAspect = vec2(p.x * aspect, p.y);
    vec2 rAspect = vec2(uHandCenters.x * aspect, uHandCenters.y);
    vec2 lAspect = vec2(uHandCenters.z * aspect, uHandCenters.w);

    float dRight = length(pAspect - rAspect);
    float dLeft = length(pAspect - lAspect);

    if (activeArm == 1) {
        if (dRight <= dLeft && p.x > (uHandCenters.z + 0.04)) {
            return 1.0;
        }
    } else if (activeArm == 0) {
        if (dLeft <= dRight && p.x < (uHandCenters.x - 0.04)) {
            return 1.0;
        }
    }
    return 0.0;
}

void main() {
    vec2 uv = vTexCoord;
    vec2 texel = uResolutionTexel.zw;
    float thickness = max(uParams.y, 1.5);
    float center = isHand(uv);

    vec2 offsets[8] = vec2[](
        vec2(-1.0, 0.0), vec2(1.0, 0.0), vec2(0.0, -1.0), vec2(0.0, 1.0),
        vec2(-0.707, -0.707), vec2(0.707, -0.707), vec2(-0.707, 0.707), vec2(0.707, 0.707)
    );

    float edge = 0.0;
    for (int i = 0; i < 8; i++) {
        vec2 nUv = uv + offsets[i] * texel * thickness;
        edge += abs(center - isHand(nUv));
    }
    edge = clamp(edge / 3.0, 0.0, 1.0);

    float glow = 0.0;
    for (int i = 0; i < 8; i++) {
        vec2 gUv = uv + offsets[i] * texel * (thickness * 2.5);
        glow += abs(center - isHand(gUv));
    }
    glow = clamp(glow / 5.0, 0.0, 1.0);

    float holdAnim = uParams.x;
    float outlineAlpha = (edge * 0.95 + glow * 0.45) * holdAnim * uColor.a;

    if (outlineAlpha <= 0.005) {
        discard;
    }

    fragColor = vec4(uColor.rgb, outlineAlpha);
}
