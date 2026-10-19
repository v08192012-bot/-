#version 150

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform LiquidData {
    mat4 uProjection;
    vec4 uRect;
    vec4 uScreen;
    vec4 uRadii;
    vec4 uTime;
    vec4 uParams1;
    vec4 uParams2;
    vec4 uParams3;
    vec4 uFresnelColor;
    vec4 uGlareColor;
};

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

void main() {
    float globalAlpha = uParams1.x;
    float fresnelPower = uParams1.y;
    float fresnelAlpha = uParams1.z;
    float baseAlpha = uParams1.w;

    bool fresnelInvert = uParams2.x > 0.5;
    float fresnelMix = uParams2.y;
    float distortStrength = uParams2.z;
    float cornerSmoothness = uParams2.w;

    float smoothness = max(uParams3.z, 0.5);

    vec2 size = uRect.zw;
    vec2 center = size * 0.5;
    vec2 box_half_size = center - 1.0;
    vec2 pos = (FragCoord * size) - center;

    float distance = roundedBoxSDF(-pos, box_half_size, uRadii, cornerSmoothness);
    float alpha = 1.0 - smoothstep(1.0 - smoothness, 1.0, distance);

    float distToEdge = abs(roundedBoxSDF(pos, box_half_size, uRadii, cornerSmoothness));
    float max_dist_norm = min(box_half_size.x, box_half_size.y);
    float edge_gradient = 1.0 - clamp(distToEdge / max_dist_norm, 0.0, 1.0);

    float fresnel;
    float base = fresnelInvert ? edge_gradient : (1.0 - edge_gradient);

    if (fresnelPower > 20.0) {
        fresnel = exp(fresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, fresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 dir = normalize(pos + vec2(0.0001));
    vec2 distortedTexCoord = TexCoord + dir * fresnel * distortStrength;

    vec4 texColor = texture(Sampler0, distortedTexCoord) * FragColor;

    vec3 finalColor = mix(texColor.rgb, uFresnelColor.rgb, fresnel * fresnelMix);
    float finalAlpha = mix(baseAlpha, fresnelAlpha, fresnel) * alpha;

    if (finalAlpha < 0.001) {
        discard;
    }

    fragColor = vec4(finalColor, finalAlpha * globalAlpha);
}
