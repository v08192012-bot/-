#version 150

in vec4 vColor;

out vec4 fragColor;

void main() {
    if (vColor.a <= 0.001) discard;
    fragColor = vColor;
}
