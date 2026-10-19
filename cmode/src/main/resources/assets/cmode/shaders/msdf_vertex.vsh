#version 150

struct Glyph {
    mat4 projection;
    vec4 rect;
    vec4 texRect;
    vec4 color;
    vec4 params;
};

layout(std140) uniform GlyphData {
    Glyph glyphs[128];
};

out vec2 vTexCoord;
out vec4 vColor;
out float vPxRange;

void main() {
    int glyphIndex = gl_VertexID / 6;
    int vertexIndex = gl_VertexID % 6;
    
    vec2 vertices[4] = vec2[](
        vec2(0.0, 0.0),
        vec2(1.0, 0.0),
        vec2(1.0, 1.0),
        vec2(0.0, 1.0)
    );
    int indices[6] = int[](0, 1, 2, 2, 3, 0);
    vec2 vertex = vertices[indices[vertexIndex]];
    
    Glyph glyph = glyphs[glyphIndex];
    vec4 rect = glyph.rect;
    vec4 texRect = glyph.texRect;
    vec4 color = glyph.color;
    
    float angle = glyph.params.y;
    vec2 localPos = vertex * rect.zw;
    
    if (angle != 0.0) {
        vec2 center = rect.zw * 0.5;
        vec2 centered = localPos - center;
        float s = sin(angle);
        float c = cos(angle);
        vec2 rotated = vec2(centered.x * c - centered.y * s, centered.x * s + centered.y * c);
        localPos = rotated + center;
    }
    
    vec2 pos = rect.xy + localPos;
    gl_Position = glyph.projection * vec4(pos, glyph.params.z, 1.0);
    
    vTexCoord = texRect.xy + vertex * texRect.zw;
    vColor = color;
    vPxRange = glyph.params.x;
}
