#version 150

in vec2 vUV;
in vec4 vColor;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec4 tex = texture(Sampler0, vUV);
    vec4 col = tex * vColor;
    if (col.a <= 0.001) discard;
    fragColor = col;
}
