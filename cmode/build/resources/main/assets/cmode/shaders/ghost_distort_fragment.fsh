#version 150
in vec2 vTexCoord;
out vec4 fragColor;

layout(std140) uniform DistortData {
    vec4 uResolutionTime; // x=width, y=height, z=time, w=count
    vec4 uParams;         // x=strength, y=frequency, z=speed, w=unused
    vec4 uCenters[8];     // xy=screen uv [0,1], z=radius (uv units), w=intensity [0,1]
};

uniform sampler2D Sampler0;

void main() {
    vec2 uv = vTexCoord;
    float aspect = uResolutionTime.x / max(uResolutionTime.y, 1.0);
    float time = uResolutionTime.z;
    int count = int(uResolutionTime.w + 0.5);

    vec2 offset = vec2(0.0);

    for (int i = 0; i < 8; i++) {
        if (i >= count) break;
        vec4 center = uCenters[i];
        if (center.w <= 0.001 || center.z <= 0.0001) continue;

        vec2 delta = vec2((uv.x - center.x) * aspect, uv.y - center.y);
        float dist = length(delta);
        if (dist >= center.z) continue;

        float k = dist / center.z;
        float falloff = 1.0 - k;
        falloff *= falloff;
        float wave = sin(k * 14.0 * uParams.y - time * uParams.z);
        vec2 dir = dist > 1.0e-5 ? delta / dist : vec2(0.0);
        offset += vec2(dir.x / aspect, dir.y) * wave * falloff * center.w * uParams.x;
    }

    if (length(offset) <= 1.0e-5) {
        fragColor = vec4(texture(Sampler0, uv).rgb, 1.0);
        return;
    }

    vec2 lo = vec2(0.001);
    vec2 hi = vec2(0.999);

    vec3 color;
    color.r = texture(Sampler0, clamp(uv + offset * 1.08, lo, hi)).r;
    color.g = texture(Sampler0, clamp(uv + offset, lo, hi)).g;
    color.b = texture(Sampler0, clamp(uv + offset * 0.92, lo, hi)).b;

    fragColor = vec4(color, 1.0);
}
