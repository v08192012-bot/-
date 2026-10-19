#version 150

in vec2 vTexCoord;
out vec4 fragColor;

layout(std140) uniform CompositeUniforms {
    vec4 uResolutionTime;
    vec4 uParams;
    vec4 uColor1;
    vec4 uColor2;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

void main() {
    vec2 uv = vTexCoord;
    vec4 center = texture(Sampler0, uv);

    float fillOpacity = uParams.w;

    if (center.a > 0.05) {
        if (fillOpacity <= 0.001) {
            discard;
        }
    }

    vec2 res = uResolutionTime.xy;
    float time = uResolutionTime.z;
    float mode = uResolutionTime.w;
    float outlineRadius = uParams.x;
    float outlineStrength = uParams.y;
    float glowIntensity = uParams.z;

    vec2 px = res * outlineRadius;
    float maxA = 0.0;
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x, 0.0)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x, 0.0)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(0.0,  px.y)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(0.0, -px.y)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x * 0.7071,  px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x * 0.7071,  px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2( px.x * 0.7071, -px.y * 0.7071)).a);
    maxA = max(maxA, texture(Sampler0, uv + vec2(-px.x * 0.7071, -px.y * 0.7071)).a);

    float outline = maxA * outlineStrength;

    vec2 gpx = res * 2.5;
    vec4 g0 = texture(Sampler1, uv + vec2( gpx.x,  gpx.y));
    vec4 g1 = texture(Sampler1, uv + vec2(-gpx.x,  gpx.y));
    vec4 g2 = texture(Sampler1, uv + vec2( gpx.x, -gpx.y));
    vec4 g3 = texture(Sampler1, uv + vec2(-gpx.x, -gpx.y));
    vec4 blur = (g0 + g1 + g2 + g3) * 0.25;

    float glow = blur.a * glowIntensity;

    if (center.a > 0.05) {
        vec4 bodyColor = uColor1;
        if (mode > 0.5) {
            float grad = clamp((1.0 - uv.y) + sin(time * 2.0 + uv.y * 3.0) * 0.15, 0.0, 1.0);
            bodyColor = mix(uColor2, uColor1, grad);
        }
        fragColor = vec4(bodyColor.rgb, fillOpacity * bodyColor.a);
        return;
    }

    float totalAlpha = outline + glow;
    if (totalAlpha < 0.005) {
        discard;
    }

    vec4 tint = uColor1;
    if (mode > 0.5) {
        float grad = clamp((1.0 - uv.y) + sin(time * 2.0 + uv.y * 3.0) * 0.15, 0.0, 1.0);
        tint = mix(uColor2, uColor1, grad);
    }

    vec3 resultColor = mix(tint.rgb, vec3(1.0), clamp(outline * 0.65, 0.0, 1.0));

    fragColor = vec4(resultColor, clamp(totalAlpha, 0.0, 1.0) * tint.a);
}
