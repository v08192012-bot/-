#version 150

in vec2 vTexCoord;

out vec4 fragColor;

layout(std140) uniform WetData {
    mat4 uViewMatrix;
    mat4 uProjectionMatrix;
    mat4 uInverseProjectionMatrix;
    mat4 uInverseViewMatrix;
    vec4 uResolutionTime;
    vec4 uWetParams;
    vec4 uReflectionParams;
    vec4 uSkyColor;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float SKY_DEPTH = 0.999999;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(cell);
    float b = hash12(cell + vec2(1.0, 0.0));
    float c = hash12(cell + vec2(0.0, 1.0));
    float d = hash12(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float puddleNoise(vec2 world) {
    return valueNoise(world * 0.28) * 0.65 + valueNoise(world * 0.85) * 0.35;
}

vec3 viewPosOf(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = uInverseProjectionMatrix * clip;
    return view.xyz / max(view.w, 1.0e-4);
}

vec2 projectToUv(vec3 viewPos) {
    vec4 clip = uProjectionMatrix * vec4(viewPos, 1.0);
    if (clip.w <= 1.0e-4) return vec2(-1.0);
    return clip.xy / clip.w * 0.5 + 0.5;
}

void main() {
    vec2 uv = vTexCoord;
    vec3 scene = texture(Sampler0, uv).rgb;
    float depth = texture(Sampler1, uv).r;

    if (depth >= SKY_DEPTH) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    vec3 viewPos = viewPosOf(uv, depth);
    vec3 worldPos = (uInverseViewMatrix * vec4(viewPos, 1.0)).xyz;

    vec3 viewNormal = normalize(cross(dFdx(viewPos), dFdy(viewPos)));
    vec3 worldNormal = normalize(mat3(uInverseViewMatrix) * viewNormal);

    float wetness = clamp(uWetParams.x, 0.0, 2.0);
    float faceUp = smoothstep(0.45, 0.85, worldNormal.y);
    float distFade = 1.0 - smoothstep(uReflectionParams.x * 0.6, uReflectionParams.x, length(viewPos));
    float damp = clamp(wetness * faceUp * distFade * uReflectionParams.y * 0.9, 0.0, 1.0);

    if (damp <= 0.004) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    float threshold = mix(0.74, 0.18, clamp(uWetParams.y, 0.0, 1.0));
    float puddle = damp * smoothstep(threshold, threshold + 0.13, puddleNoise(worldPos.xz));

    vec3 wet = scene * mix(1.0, 0.68, damp);
    wet = mix(vec3(dot(wet, LUMA)), wet, 1.0 + 0.35 * damp);

    if (puddle > 0.01) {
        vec3 viewDir = normalize(viewPos);
        vec3 surfNormal = viewNormal;

        vec3 reflDir = reflect(viewDir, surfNormal);

        int steps = int(clamp(uReflectionParams.z, 4.0, 10.0));
        float maxTravel = clamp(uReflectionParams.x * 0.5, 6.0, 48.0);
        float stepLen = 0.45;
        float travelled = 0.0;
        vec3 rayPos = viewPos + surfNormal * 0.05;
        vec2 hitUv = vec2(-1.0);

        for (int i = 0; i < 10; i++) {
            if (i >= steps) break;

            rayPos += reflDir * stepLen;
            travelled += stepLen;

            vec2 sampleUv = projectToUv(rayPos);
            if (sampleUv.x < 0.0 || sampleUv.x > 1.0 || sampleUv.y < 0.0 || sampleUv.y > 1.0) break;

            float sampleDepth = texture(Sampler1, sampleUv).r;
            if (sampleDepth < SKY_DEPTH) {
                float sceneZ = viewPosOf(sampleUv, sampleDepth).z;
                if (rayPos.z < sceneZ - 0.02 && rayPos.z > sceneZ - stepLen * 2.0 - 0.3) {
                    hitUv = sampleUv;
                    break;
                }
            }

            if (travelled > maxTravel) break;
            stepLen *= 1.45;
        }

        vec3 reflected;
        if (hitUv.x >= 0.0) {
            reflected = texture(Sampler0, hitUv).rgb;
        } else {
            vec2 farUv = projectToUv(viewPos + reflDir * 4096.0);
            vec2 outside = max(vec2(0.0) - farUv, farUv - vec2(1.0));
            float onScreen = 1.0 - smoothstep(0.0, 0.12, max(outside.x, outside.y));
            vec3 projected = texture(Sampler0, clamp(farUv, vec2(0.002), vec2(0.998))).rgb;
            reflected = mix(uSkyColor.rgb, projected, onScreen);
        }

        float fresnel = pow(1.0 - clamp(dot(-viewDir, surfNormal), 0.0, 1.0), 5.0);
        fresnel = mix(0.22, 1.0, fresnel);
        float reflectMix = clamp(fresnel * clamp(uWetParams.z, 0.0, 2.5) * puddle, 0.0, 0.92);

        wet = mix(wet, reflected, reflectMix);
        wet += reflected * smoothstep(0.45, 1.0, dot(reflected, LUMA)) * puddle * fresnel * 0.30;
    }

    fragColor = vec4(wet, 1.0);
}
