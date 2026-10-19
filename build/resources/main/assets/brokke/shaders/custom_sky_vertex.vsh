#version 150

layout(std140) uniform SkyUniforms {
    mat4 uInverseViewProj;
    vec4 uParams;
    vec4 uExtra;
};

out vec2 vUV;
out vec3 vWorldDir;

void main() {
    vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );

    vec2 pos = positions[gl_VertexID];
    gl_Position = vec4(pos, 1.0, 1.0);
    vUV = pos * 0.5 + 0.5;

    vec4 worldRay = uInverseViewProj * vec4(pos, 1.0, 1.0);
    vWorldDir = worldRay.xyz / worldRay.w;
}
