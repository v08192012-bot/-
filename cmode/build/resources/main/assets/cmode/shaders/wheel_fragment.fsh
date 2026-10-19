#version 410 core
precision highp float;

in vec2 v_TexCoord;

layout(std140) uniform Uniforms {
    mat4 ProjMat;
    vec4 RectPosSize;
    vec4 WheelParams;
    vec4 ExtraParams;
    vec4 SlotColors[8];
};

out vec4 fragColor;

const float PI = 3.14159265358979323846;
const float TWO_PI = 6.28318530717958647692;

void main() {
    vec2 pixelOffset = (v_TexCoord - 0.5) * RectPosSize.zw;
    float r = length(pixelOffset);

    float innerR = WheelParams.x;
    float outerR = WheelParams.y;
    float gap = WheelParams.z;
    float cornerR = WheelParams.w;

    float count = max(ExtraParams.x, 3.0);
    float baseAlpha = ExtraParams.y;

    float pad = max(cornerR, 2.0);
    if (r < (innerR - pad) || r > (outerR + pad)) {
        discard;
    }

    float theta = atan(pixelOffset.y, pixelOffset.x);
    float ang = mod(theta + PI * 0.5 + TWO_PI, TWO_PI);
    float sectorSpan = TWO_PI / count;

    int sectorIdx = int(floor(ang / sectorSpan));
    if (sectorIdx < 0) sectorIdx = 0;
    if (sectorIdx >= int(count)) sectorIdx = int(count) - 1;

    float phi = ang - float(sectorIdx) * sectorSpan;
    float deltaPhi = abs(phi - sectorSpan * 0.5);

    float d_divider = r * sin(sectorSpan * 0.5 - deltaPhi);
    float d_edge = (gap * 0.5) - d_divider;
    float d_ring = max(innerR - r, r - outerR);

    float d;
    if (cornerR > 0.5) {
        vec2 v = vec2(d_ring, d_edge) + cornerR;
        d = length(max(v, 0.0)) + min(max(v.x, v.y), 0.0) - cornerR;
    } else {
        d = max(d_ring, d_edge);
    }

    float edge = max(fwidth(d), 0.7);
    float shapeAlpha = 1.0 - smoothstep(-edge * 0.5, edge * 0.5, d);
    if (shapeAlpha <= 0.001) {
        discard;
    }

    vec4 col = SlotColors[sectorIdx];
    float finalAlpha = shapeAlpha * col.a * baseAlpha;
    if (finalAlpha <= (1.0 / 255.0)) {
        discard;
    }

    fragColor = vec4(col.rgb, finalAlpha);
}
