#version 150

struct TextureData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uColor;
    vec4 uRadius_Padding;
    vec4 uZ_Padding;
    vec4 uUV;
};

layout(std140) uniform Uniforms {
    TextureData textures[64];
};

out vec2 vUV;
out vec2 vPos;
flat out vec2 vSize;
flat out vec4 vColor;
flat out float vRadius;

void main() {
    TextureData data = textures[gl_VertexID / 6];
    vSize = data.uRect.zw;
    vColor = data.uColor;
    vRadius = data.uRadius_Padding.x;
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[gl_VertexID % 6]];

    float ang = data.uRadius_Padding.y;
    vec2 local = vertex * data.uRect.zw - data.uRect.zw * 0.5;
    if (ang != 0.0) {
        float ca = cos(ang);
        float sa = sin(ang);
        local = vec2(local.x * ca - local.y * sa, local.x * sa + local.y * ca);
    }

    vPos = vertex * data.uRect.zw;
    vec2 pos = data.uRect.xy + data.uRect.zw * 0.5 + local;
    gl_Position = data.uProjection * vec4(pos, data.uZ_Padding.x, 1.0);

    vUV = data.uUV.xy + vertex * data.uUV.zw;
}
