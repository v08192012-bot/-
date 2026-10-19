#version 410 core

struct OutlineData {
    mat4 projection;
    vec4 depth;
    vec4 rect;
    vec4 radii;
    vec4 params;
    vec4 c0;
    vec4 c1;
    vec4 c2;
    vec4 c3;
};

layout(std140) uniform Uniforms {
    OutlineData uOutlines[64];
};

out vec2 vUV;
out vec2 vSize;
flat out vec4 vRadii;
flat out float vThickness;
flat out vec4 vC0;
flat out vec4 vC1;
flat out vec4 vC2;
flat out vec4 vC3;

void main() {
    int outlineIdx = gl_VertexID / 6;
    int cornerIdx = gl_VertexID % 6;

    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[cornerIdx]];

    OutlineData data = uOutlines[outlineIdx];
    float thickness = data.params.x;
    float pad = max(thickness, 1.0) + 1.5;

    vec2 pos = data.rect.xy - vec2(pad) + vertex * (data.rect.zw + vec2(pad * 2.0));
    vUV = (pos - data.rect.xy) / data.rect.zw;
    vSize = data.rect.zw;
    vRadii = data.radii;
    vThickness = thickness;
    vC0 = data.c0;
    vC1 = data.c1;
    vC2 = data.c2;
    vC3 = data.c3;

    gl_Position = data.projection * vec4(pos, data.depth.x, 1.0);
}

