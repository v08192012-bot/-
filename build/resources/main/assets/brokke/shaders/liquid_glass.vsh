#version 150

layout(std140) uniform LiquidData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uRadii;
    vec4 uTime;
    vec4 uParams1;
    vec4 uParams2;
    vec4 uParams3;
    vec4 uFresnelColor;
    vec4 uGlareColor;
};

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;

void main() {
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[gl_VertexID]];

    FragCoord = vertex;
    vec2 pos = uRect.xy + vertex * uRect.zw;
    gl_Position = uProjection * vec4(pos, 0.0, 1.0);

    TexCoord = vec2(pos.x / uScreen.x, 1.0 - pos.y / uScreen.y);
    FragColor = vec4(1.0);
}
