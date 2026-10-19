#version 330

layout(std140) uniform Uniforms {
    mat4 uInvProjection;
    mat4 uProjection;
    mat4 uViewRot;
    vec4 uCameraPos;
    vec4 uScreen;
    vec4 uParams;
    vec4 uQualityParams;
    vec4 uFogParams;
    vec4 uFogColor;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 vUV;
out vec4 fragColor;

float getDither(vec2 coord) {
    return fract(52.9829189 * fract(dot(coord, vec2(0.06711056, 0.00583715))));
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    mat2 rot = mat2(0.8, 0.6, -0.6, 0.8);
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = rot * p * 2.02 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

vec3 getViewPosFromRawDepth(vec2 uv, float d) {
    vec2 ndc = uv * 2.0 - 1.0;
    vec4 clip = vec4(ndc, d * 2.0 - 1.0, 1.0);
    vec4 v = uInvProjection * clip;
    return v.xyz / max(abs(v.w), 0.00001);
}

vec3 getViewPosFromDepth(vec2 uv) {
    float d = texture(Sampler1, clamp(uv, vec2(0.0001), vec2(0.9999))).r;
    return getViewPosFromRawDepth(uv, d);
}

float getViewZFromDepth(float d) {
    float clipZ = d * 2.0 - 1.0;
    float vz = uInvProjection[2][2] * clipZ + uInvProjection[3][2];
    float vw = uInvProjection[2][3] * clipZ + uInvProjection[3][3];
    return vz / max(abs(vw), 0.00001);
}

vec3 getNormalFromDepth(vec2 uv, vec3 viewPos) {
    vec2 texel = 1.0 / max(uScreen.xy, vec2(1.0));

    float dL = texture(Sampler1, clamp(uv - vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dR = texture(Sampler1, clamp(uv + vec2(texel.x, 0.0), vec2(0.0001), vec2(0.9999))).r;
    float dD = texture(Sampler1, clamp(uv - vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;
    float dU = texture(Sampler1, clamp(uv + vec2(0.0, texel.y), vec2(0.0001), vec2(0.9999))).r;

    vec3 pL = getViewPosFromRawDepth(uv - vec2(texel.x, 0.0), dL);
    vec3 pR = getViewPosFromRawDepth(uv + vec2(texel.x, 0.0), dR);
    vec3 pD = getViewPosFromRawDepth(uv - vec2(0.0, texel.y), dD);
    vec3 pU = getViewPosFromRawDepth(uv + vec2(0.0, texel.y), dU);

    vec3 vL = viewPos - pL;
    vec3 vR = pR - viewPos;
    vec3 vD = viewPos - pD;
    vec3 vU = pU - viewPos;

    vec3 dX = (abs(vL.z) < abs(vR.z)) ? vL : vR;
    vec3 dY = (abs(vD.z) < abs(vU.z)) ? vD : vU;

    vec3 N = cross(dX, dY);
    float nLen = length(N);
    if (nLen > 1e-6) {
        N /= nLen;
    } else {
        N = normalize(-viewPos);
    }

    if (dot(N, viewPos) > 0.0) {
        N = -N;
    }
    return N;
}

vec3 getNormalFromDepth(vec2 uv) {
    vec3 viewPos = getViewPosFromDepth(uv);
    return getNormalFromDepth(uv, viewPos);
}

vec2 getWaterWaves(vec2 p, float time, float speed, float quality) {
    float t = time * speed;
    vec2 dN = vec2(0.0);

    float w1 = sin(p.x * 2.5 + p.y * 1.8 + t * 3.2);
    float w2 = cos(p.x * -2.2 + p.y * 3.1 - t * 2.6);
    dN += vec2(w1 + w2, -w1 + w2) * 0.025;

    vec2 grid = fract(p * 2.5) - 0.5;
    vec2 id = floor(p * 2.5);
    float dropRand = hash21(id);
    float dropTime = fract(t * 1.6 + dropRand);
    float dropDist = length(grid);
    float ring = sin(dropDist * 32.0 - dropTime * 16.0) * exp(-dropDist * 4.5) * (1.0 - dropTime);
    dN += (grid / max(dropDist, 0.001)) * ring * 0.030;

    if (quality >= 2.0) {
        float w3 = sin(p.x * 6.5 - p.y * 4.8 + t * 4.2);
        float w4 = cos(p.x * 5.8 + p.y * 7.2 - t * 3.5);
        dN += vec2(w3, w4) * 0.012;
    }
    return dN;
}

void main() {
    vec4 originalColor = texture(Sampler0, vUV);
    float depthVal = texture(Sampler1, vUV).r;

    if (depthVal >= 0.99999) {
        fragColor = originalColor;
        return;
    }

    vec3 viewPos = getViewPosFromDepth(vUV);
    vec3 V_dir = normalize(viewPos);

    vec3 worldOffset = (uViewRot * vec4(viewPos, 0.0)).xyz;
    vec3 worldPos = uCameraPos.xyz + worldOffset;

    vec3 N_view = getNormalFromDepth(vUV, viewPos);
    vec3 N_world = normalize((uViewRot * vec4(N_view, 0.0)).xyz);

    float reflectionStrength = uParams.x;
    float darkeningFactor = uParams.y;
    float time = uParams.z;

    float qualityIdx = uQualityParams.x;
    float ripplesEnabled = uQualityParams.y;
    float rippleSpeed = uQualityParams.z;

    float upFacing = clamp(N_world.y, 0.0, 1.0);
    float isGround = smoothstep(0.35, 0.85, upFacing);
    float wetness = 0.45 + 0.55 * isGround;

    vec3 perturbedN_view = N_view;
    if (ripplesEnabled > 0.5 && isGround > 0.05) {
        vec2 dN = getWaterWaves(worldPos.xz, time, rippleSpeed, qualityIdx);
        vec3 perturbedN_world = normalize(vec3(-dN.x * isGround * 0.45, 1.0, -dN.y * isGround * 0.45));
        perturbedN_view = normalize((transpose(uViewRot) * vec4(perturbedN_world, 0.0)).xyz);
        if (dot(perturbedN_view, viewPos) > 0.0) {
            perturbedN_view = -perturbedN_view;
        }
    }

    vec3 R_view = reflect(V_dir, perturbedN_view);

    int maxSteps = 48;
    int binarySteps = 4;
    float maxDist = 36.0;
    float stepSize = 0.20;

    if (qualityIdx < 0.5) {
        maxSteps = 24;
        binarySteps = 3;
        maxDist = 24.0;
        stepSize = 0.32;
    } else if (qualityIdx < 1.5) {
        maxSteps = 40;
        binarySteps = 4;
        maxDist = 32.0;
        stepSize = 0.22;
    } else if (qualityIdx < 2.5) {
        maxSteps = 64;
        binarySteps = 6;
        maxDist = 48.0;
        stepSize = 0.16;
    } else {
        maxSteps = 96;
        binarySteps = 8;
        maxDist = 64.0;
        stepSize = 0.12;
    }

    float dither = getDither(gl_FragCoord.xy);

    vec3 hitColor = vec3(0.0);
    float hitAlpha = 0.0;
    
    vec3 rayOrigin = viewPos + perturbedN_view * 0.03;

    if (R_view.z < 0.05) {
        float hitT = 0.0;
        vec2 finalHitUV = vec2(-1.0);
        bool foundHit = false;

        for (int i = 0; i < maxSteps; i++) {
            float t = (float(i) + dither) * stepSize + (stepSize * 0.5);
            if (t > maxDist) break;

            vec3 P_ray = rayOrigin + R_view * t;
            vec4 pClip = uProjection * vec4(P_ray, 1.0);
            if (pClip.w <= 0.001) break;

            vec3 pNdc = pClip.xyz / pClip.w;
            vec2 sUV = pNdc.xy * 0.5 + 0.5;

            if (sUV.x < 0.001 || sUV.x > 0.999 || sUV.y < 0.001 || sUV.y > 0.999 || pNdc.z < -1.0 || pNdc.z > 1.0) {
                break;
            }

            float dSamp = texture(Sampler1, sUV).r;
            if (dSamp >= 0.99999) {
                continue;
            }

            float sceneZ = getViewZFromDepth(dSamp);
            float depthDelta = sceneZ - P_ray.z;
            float dynamicThickness = max(stepSize * 1.5, 0.06 + 0.015 * t);

            if (depthDelta >= 0.0 && depthDelta < dynamicThickness) {
                float tMin = max(0.0, t - stepSize);
                float tMax = t;
                vec2 refinedUV = sUV;
                float lastSamp = dSamp;

                for (int b = 0; b < binarySteps; b++) {
                    float tMid = (tMin + tMax) * 0.5;
                    vec3 pMid = rayOrigin + R_view * tMid;
                    vec4 mClip = uProjection * vec4(pMid, 1.0);
                    if (mClip.w <= 0.001) break;

                    vec2 mUV = (mClip.xyz / mClip.w).xy * 0.5 + 0.5;
                    if (mUV.x < 0.001 || mUV.x > 0.999 || mUV.y < 0.001 || mUV.y > 0.999) break;

                    float mSamp = texture(Sampler1, mUV).r;
                    if (mSamp >= 0.99999) {
                        tMin = tMid;
                        continue;
                    }

                    float mSceneZ = getViewZFromDepth(mSamp);
                    float mDelta = mSceneZ - pMid.z;

                    if (mDelta >= 0.0) {
                        tMax = tMid;
                        refinedUV = mUV;
                        lastSamp = mSamp;
                    } else {
                        tMin = tMid;
                    }
                }

                vec3 hitViewPos = getViewPosFromRawDepth(refinedUV, lastSamp);
                vec3 hitRayPos = rayOrigin + R_view * tMax;
                float finalDelta = hitViewPos.z - hitRayPos.z;

                if (finalDelta >= -0.03 && finalDelta < dynamicThickness) {
                    vec2 uvDiff = abs(refinedUV - vUV) * uScreen.xy;
                    if (length(uvDiff) < 3.5 && tMax < stepSize * 2.5) {
                        continue;
                    }

                    vec3 hitNormal = getNormalFromDepth(refinedUV, hitViewPos);
                    if (dot(hitNormal, R_view) >= -0.05) {
                        continue;
                    }

                    finalHitUV = refinedUV;
                    hitT = tMax;
                    foundHit = true;
                    break;
                }
            }
        }

        if (foundHit && finalHitUV.x >= 0.0) {
            hitColor = texture(Sampler0, finalHitUV).rgb;

            float screenFade = smoothstep(0.0, 0.10, finalHitUV.x) * (1.0 - smoothstep(0.90, 1.0, finalHitUV.x)) *
                               smoothstep(0.0, 0.10, finalHitUV.y) * (1.0 - smoothstep(0.90, 1.0, finalHitUV.y));
            float rayDistFade = 1.0 - smoothstep(maxDist * 0.65, maxDist, hitT);
            float facingFade = 1.0 - smoothstep(-0.25, 0.05, R_view.z);

            hitAlpha = screenFade * rayDistFade * facingFade;
        }
    }

    vec3 R_world = normalize((uViewRot * vec4(R_view, 0.0)).xyz);
    float skyPitch = clamp(R_world.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 horizonColor = vec3(0.22, 0.30, 0.42);
    vec3 zenithColor = vec3(0.08, 0.15, 0.28);
    vec3 skyRefl = mix(horizonColor, zenithColor, pow(skyPitch, 0.75));

    float cloudNoise = fbm(R_world.xz * 1.5 / max(abs(R_world.y) + 0.18, 0.08));
    skyRefl = mix(skyRefl, vec3(0.38, 0.45, 0.55), cloudNoise * 0.28 * smoothstep(-0.1, 0.5, R_world.y));

    vec3 lightDir = normalize(vec3(0.35, 0.75, 0.25));
    float sunSpec = pow(max(dot(R_world, lightDir), 0.0), 32.0);
    skyRefl += vec3(0.85, 0.88, 0.95) * clamp(sunSpec, 0.0, 1.0) * 0.50 * isGround;

    vec3 totalReflection = mix(skyRefl, hitColor, hitAlpha);

    float nearFade = smoothstep(0.35, 0.70, abs(viewPos.z));

    float fogStart = uFogParams.x;
    float fogEnd = uFogParams.y;
    float fogActive = uFogParams.z;

    float dist = length(viewPos);
    float fogFactor = clamp((dist - fogStart) / max(fogEnd - fogStart, 0.001), 0.0, 1.0);
    fogFactor = fogFactor * fogFactor * (3.0 - 2.0 * fogFactor);

    vec3 darkenedBase = mix(originalColor.rgb * (1.0 - darkeningFactor * wetness), originalColor.rgb, fogFactor);

    float NdotV = clamp(dot(-V_dir, perturbedN_view), 0.0, 1.0);
    float F0 = mix(0.04, 0.16, isGround);
    float fresnel = F0 + (1.0 - F0) * pow(1.0 - NdotV, 3.5);
    fresnel = clamp(fresnel, 0.03, 0.95);

    float effectiveStrength = reflectionStrength * wetness * nearFade * (1.0 - fogFactor);
    vec3 finalColor = mix(darkenedBase, totalReflection, fresnel * effectiveStrength);

    if (fogActive > 0.5 && uFogColor.a > 0.001) {
        finalColor = mix(finalColor, uFogColor.rgb, fogFactor * uFogColor.a);
    }

    fragColor = vec4(finalColor, 1.0);
}
