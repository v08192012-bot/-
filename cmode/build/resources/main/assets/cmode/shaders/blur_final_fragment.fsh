#version 150

in vec2 texCoord;
in vec2 pixelCoord;
in vec2 rectSize;
in vec4 cornerRadii;

out vec4 fragColor;

uniform sampler2D Sampler0;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.yz : r.xw;
    r.x = (p.y > 0.0) ? r.y : r.x;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec3 blurred = texture(Sampler0, texCoord).rgb;

    float dither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);
    blurred += (dither - 0.5) * (0.35 / 255.0);

    vec2 halfSize = rectSize * 0.5;
    vec2 center = pixelCoord - halfSize;
    float maxRadius = min(halfSize.x, halfSize.y);
    vec4 rRadii = min(cornerRadii, vec4(maxRadius));
    float dist = roundedBoxSDF(center, halfSize, rRadii);

    float smoothing = max(fwidth(dist), 0.7);
    float alpha = 1.0 - smoothstep(-smoothing, smoothing, dist);

    if (alpha <= 0.001) discard;

    fragColor = vec4(blurred, alpha);
}
