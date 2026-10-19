#version 410 core
precision highp float;

in vec2 v_TexCoord;

layout(std140) uniform Uniforms {
    mat4 ProjMat;
    vec4 RectPosSize;
    vec4 Params;
    vec4 Color;
    vec4 BgColor;
};

out vec4 fragColor;

void main() {
    vec2 uv = (v_TexCoord - 0.5) * 2.0;
    float dist = length(uv);
    float thicknessNorm = (Params.x * 2.0) / max(RectPosSize.z, 0.001);
    float innerR = clamp(1.0 - thicknessNorm, 0.0, 1.0);

    float aa = max(fwidth(dist), 0.001);
    float inRing = smoothstep(innerR - aa, innerR + aa, dist) * (1.0 - smoothstep(1.0 - aa, 1.0 + aa, dist));
    if (inRing <= 0.001) discard;

    float angle = atan(uv.y, uv.x);
    float normalizedAngle = mod(angle + 1.57079632679 + 6.28318530718, 6.28318530718) / 6.28318530718;

    float progress = clamp(Params.y, 0.0, 1.0);
    float progressMask;
    if (progress >= 0.99) {
        progressMask = 1.0;
    } else if (progress <= 0.005) {
        progressMask = 0.0;
    } else {
        float angleAA = 1.0 / max(dist * RectPosSize.z * 3.14159265359, 1.0);
        float startMask = smoothstep(0.0, angleAA * 2.0, normalizedAngle);
        float endMask = 1.0 - smoothstep(progress - angleAA * 2.0, progress, normalizedAngle);
        progressMask = clamp(startMask * endMask, 0.0, 1.0);
    }

    vec4 finalColor = mix(BgColor, Color, progressMask);
    float alpha = inRing * finalColor.a * Params.z;
    if (alpha < (1.0 / 255.0)) discard;

    fragColor = vec4(finalColor.rgb, alpha);
}
