#version 150

in vec2 vTexCoord;
out vec4 fragColor;

layout(std140) uniform BlurUniforms {
    vec4 uParams;
};

uniform sampler2D Sampler0;

const float O1 = 1.3846153846;
const float O2 = 3.2307692308;
const float WC0 = 0.2270270270;
const float WC1 = 0.3162162162;
const float WC2 = 0.0702702703;

void main() {
    vec2 step = vec2(uParams.x * uParams.z, 0.0);

    vec4 c = texture(Sampler0, vTexCoord) * WC0;
    c += texture(Sampler0, vTexCoord + step * O1) * WC1;
    c += texture(Sampler0, vTexCoord - step * O1) * WC1;
    c += texture(Sampler0, vTexCoord + step * O2) * WC2;
    c += texture(Sampler0, vTexCoord - step * O2) * WC2;

    fragColor = c;
}
