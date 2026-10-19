#version 150

layout(std140) uniform BlurData {
    mat4 uProjection;
    vec4 uScreen;
    vec4 uZ_Padding;
    vec4 uRects[128];
};

out vec2 texCoord;
out vec2 pixelCoord;
out vec2 rectSize;
out vec4 cornerRadii;

void main() {
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);

    int rectIndex = gl_VertexID / 6;
    int vertexIndex = gl_VertexID % 6;
    vec2 vertex = vertices[indices[vertexIndex]];

    vec4 rect = uRects[rectIndex * 2];
    vec4 radii = uRects[rectIndex * 2 + 1];

    pixelCoord = vertex * rect.zw;
    rectSize = rect.zw;
    cornerRadii = radii;

    vec2 pos = rect.xy + vertex * rect.zw;
    gl_Position = uProjection * vec4(pos, uZ_Padding.x, 1.0);

    texCoord = vec2(pos.x / uScreen.x, 1.0 - pos.y / uScreen.y);
}
