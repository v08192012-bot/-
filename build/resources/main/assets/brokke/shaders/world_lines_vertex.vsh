#version 150

in vec3 Position;
in vec4 Color;

layout(std140) uniform Uniforms {
    mat4 uProjView;
};

out vec4 vColor;

void main() {
    vColor = Color;
    gl_Position = uProjView * vec4(Position, 1.0);
}
