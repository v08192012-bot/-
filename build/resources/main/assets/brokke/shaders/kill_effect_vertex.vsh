#version 330

in vec3 Position;
in vec2 UV;
in vec4 Color;

layout(std140) uniform MatrixData {
    mat4 uProjView;
};

out vec2 texCoord;
out vec4 vertexColor;
out vec4 clipPos;

void main() {
    texCoord = UV;
    vertexColor = Color;
    clipPos = uProjView * vec4(Position, 1.0);
    gl_Position = clipPos;
}