#version 410 core

layout(location = 0) in vec3 Position;

layout(std140) uniform Uniforms {
    mat4 ProjMat;
    vec4 RectPosSize;
    vec4 WheelParams;
    vec4 ExtraParams;
    vec4 SlotColors[8];
};

out vec2 v_TexCoord;

const vec2 CORNERS[6] = vec2[6](
    vec2(0.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0),
    vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 0.0)
);

void main() {
    vec2 rawPos = CORNERS[gl_VertexID];
    v_TexCoord = rawPos;
    vec2 finalPos = RectPosSize.xy + (rawPos * RectPosSize.zw);
    gl_Position = ProjMat * vec4(finalPos, ExtraParams.z, 1.0);
}
