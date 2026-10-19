#version 150

in vec2 texCoord;
in vec2 vOffset;
in float vPassType;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    if (vPassType < 0.5) {
        vec2 o = vOffset;
        vec3 sum = texture(Sampler0, texCoord).rgb * 4.0;
        sum += texture(Sampler0, texCoord + vec2(-o.x, -o.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2( o.x, -o.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2(-o.x,  o.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2( o.x,  o.y)).rgb;
        fragColor = vec4(sum * 0.125, 1.0);
    } else {
        vec2 d = vOffset;
        vec2 d2 = vOffset * 2.0;

        vec3 sum = texture(Sampler0, texCoord).rgb * 4.0;
        sum += texture(Sampler0, texCoord + vec2(-d2.x, 0.0)).rgb * 2.0;
        sum += texture(Sampler0, texCoord + vec2( d2.x, 0.0)).rgb * 2.0;
        sum += texture(Sampler0, texCoord + vec2(0.0, -d2.y)).rgb * 2.0;
        sum += texture(Sampler0, texCoord + vec2(0.0,  d2.y)).rgb * 2.0;

        sum += texture(Sampler0, texCoord + vec2(-d.x, -d.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2( d.x, -d.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2(-d.x,  d.y)).rgb;
        sum += texture(Sampler0, texCoord + vec2( d.x,  d.y)).rgb;

        fragColor = vec4(sum * 0.0625, 1.0);
    }
}
