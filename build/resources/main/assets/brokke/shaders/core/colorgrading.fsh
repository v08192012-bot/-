#version 150

uniform sampler2D u_texture;

layout(std140) uniform uResolution {
    vec2 resolution;
};
layout(std140) uniform uSaturation {
    float saturation;
};
layout(std140) uniform uWarmth {
    float warmth;
};

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / resolution.xy;
    vec4 color = texture(u_texture, uv);

    // Saturation: lerp between grayscale and original
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(gray), color.rgb, saturation);

    // Warmth: reduce blue channel, boost red/green
    color.r = min(1.0, color.r * (1.0 + warmth * 0.20));
    color.g = min(1.0, color.g * (1.0 + warmth * 0.10));
    color.b = max(0.0, color.b * max(0.05, 1.0 - warmth * 0.45));

    fragColor = color;
}
