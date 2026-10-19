#version 410 core

in vec2 vUV;
in vec2 vSize;
flat in vec4 vRadii;
flat in vec4 vC0;
flat in vec4 vC1;
flat in vec4 vC2;
flat in vec4 vC3;

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

float dither(vec2 uv) {
    return (fract(sin(dot(uv.xy, vec2(12.9898, 78.233))) * 43758.5453123) - 0.5) * (1.0 / 255.0);
}

void main() {
    vec2 pixelPos = vUV * vSize;
    vec2 center = vSize * 0.5;

    float dist = sdRoundedBox(pixelPos - center, center, vRadii);
    float edge = fwidth(dist);
    float alpha = 1.0 - smoothstep(-edge, edge, dist);

    if (alpha <= 0.0) discard;

    vec4 top = mix(vC0, vC1, vUV.x);
    vec4 bot = mix(vC3, vC2, vUV.x);
    vec4 color = mix(top, bot, vUV.y);

    color.rgb += dither(pixelPos);

    fragColor = vec4(color.rgb, color.a * alpha);
}

