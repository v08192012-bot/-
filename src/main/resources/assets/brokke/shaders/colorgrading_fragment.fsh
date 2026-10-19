#version 150

in vec2 vUV;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform ColorGradingData {
    vec4 uParams;
};

void main() {
    vec4 color = texture(Sampler0, vUV);

    float saturation = uParams.x;
    float warmth = uParams.y;

    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(gray), color.rgb, saturation);

    color.r = min(1.0, color.r * (1.0 + warmth * 0.20));
    color.g = min(1.0, color.g * (1.0 + warmth * 0.10));
    color.b = max(0.0, color.b * max(0.05, 1.0 - warmth * 0.45));

    fragColor = color;
}
