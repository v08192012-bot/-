#version 410 core

in vec2 vUV;
in vec2 vSize;
flat in vec4 vRadii;
flat in float vThickness;
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

void main() {
    vec2 pixelPos = vUV * vSize;
    vec2 center = vSize * 0.5;

    float dist = sdRoundedBox(pixelPos - center, center, vRadii);
    float edge = fwidth(dist);

    float d2 = abs(dist + vThickness * 0.5) - vThickness * 0.5;
    float alpha = 1.0 - smoothstep(-edge, edge, d2);

    if (alpha <= 0.0) discard;

    vec4 top = mix(vC0, vC1, vUV.x);
    vec4 bot = mix(vC3, vC2, vUV.x);
    vec4 color = mix(top, bot, vUV.y);

    fragColor = vec4(color.rgb, color.a * alpha);
}

