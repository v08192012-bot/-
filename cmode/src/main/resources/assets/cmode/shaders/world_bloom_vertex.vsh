#version 150

in vec3 Position;
in vec2 UV;
in vec4 Color;

layout(std140) uniform Uniforms {
    mat4 uProjView;
};

out vec2 vUV;
out vec4 vColor;

void main() {
    vUV = UV;
    vColor = Color;
    gl_Position = uProjView * vec4(Position, 1.0);
}
