#version 150

in vec2 in_uv;
out vec4 out_color;

uniform sampler2D texture_in;
uniform sampler2D depth_texture_in;

layout(std140) uniform JumpData {
    mat4 inv_mvp;
    mat4 mvp;
    vec4 wave_params;
    vec4 first_color;
    vec4 second_color;
    vec4 gradient_dir;
    vec4 wave_pos[8];
    vec4 wave_fade[8];
};

void main() {
    float rawDepth = texture(depth_texture_in, in_uv).r;

    if (rawDepth >= 0.99999) {
        out_color = texture(texture_in, in_uv);
        return;
    }

    vec4 clip = vec4(in_uv * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    vec4 wp = inv_mvp * clip;
    if (wp.w == 0.0) {
        out_color = texture(texture_in, in_uv);
        return;
    }
    vec3 worldRel = wp.xyz / wp.w;

    float thickness = max(wave_params.x, 0.38);
    float amplitude = wave_params.y;
    float aberration = wave_params.w;

    float bestStrength = 0.0;
    float bestDisplace = 0.0;
    vec2 bestDir = vec2(0.0, 1.0);

    for (int i = 0; i < 8; i++) {
        float fade = wave_fade[i].x;
        if (fade <= 0.001) continue;

        vec3 center = wave_pos[i].xyz;
        float currentRadius = wave_pos[i].w;
        if (currentRadius <= 0.05) continue;

        vec3 diff = worldRel - center;
        float distXZ = length(diff.xz);
        float dy = diff.y;

        float delta = abs(distXZ - currentRadius);

        if (delta < thickness) {
            float ring = 1.0 - (delta / thickness);
            ring = smoothstep(0.0, 1.0, ring);

            float vFade = exp(-abs(dy) * 0.16);
            float strength = ring * fade * vFade;

            if (strength > bestStrength) {
                bestStrength = strength;
                bestDir = diff.xz / max(distXZ, 0.001);

                float phase = (distXZ - currentRadius) / thickness;
                bestDisplace = -sin(phase * 3.14159265) * strength;
            }
        }
    }

    if (bestStrength <= 0.001) {
        out_color = texture(texture_in, in_uv);
        return;
    }

    vec2 screenOffset = bestDir * bestDisplace * (amplitude * 0.015);

    vec2 caShift = bestDir * (aberration * 0.00025) * bestStrength;
    vec2 sampleUV = in_uv - screenOffset;

    float r = texture(texture_in, sampleUV + caShift).r;
    float g = texture(texture_in, sampleUV).g;
    float b = texture(texture_in, sampleUV - caShift).b;
    vec3 sceneColor = vec3(r, g, b);

    float grad = dot(bestDir, gradient_dir.xy) * 0.5 + 0.5;
    vec3 waveColor = mix(first_color.rgb, second_color.rgb, grad);

    float glow = pow(bestStrength, 1.8);
    float core = pow(bestStrength, 5.0) * 1.6;

    vec3 finalColor = sceneColor;
    finalColor = mix(finalColor, waveColor, glow * 0.45);
    finalColor += waveColor * glow * 0.7;
    finalColor += vec3(1.0) * core;

    out_color = vec4(finalColor, 1.0);
}
