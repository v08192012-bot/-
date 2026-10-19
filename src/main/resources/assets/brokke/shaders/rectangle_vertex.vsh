#version 410 core

struct RectData {
    mat4 projection;
    vec4 depth;
    vec4 rect;
    vec4 radii;
    vec4 c0;
    vec4 c1;
    vec4 c2;
    vec4 c3;
};

layout(std140) uniform Uniforms {
    RectData uRects[64];
};

out vec2 vUV;
out vec2 vSize;
flat out vec4 vRadii;
flat out vec4 vC0;
flat out vec4 vC1;
flat out vec4 vC2;
flat out vec4 vC3;

void main() {
    int rectIdx = gl_VertexID / 6;
    int cornerIdx = gl_VertexID % 6;

    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[cornerIdx]];

    RectData data = uRects[rectIdx];

    vUV = vertex;
    vSize = data.rect.zw;
    vRadii = data.radii;
    vC0 = data.c0;
    vC1 = data.c1;
    vC2 = data.c2;
    vC3 = data.c3;

    vec2 pos = data.rect.xy + vertex * data.rect.zw;
    gl_Position = data.projection * vec4(pos, data.depth.x, 1.0);
}

