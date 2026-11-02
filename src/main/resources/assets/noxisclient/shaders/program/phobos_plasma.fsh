#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Color;

in vec2 texCoord;
out vec4 fragColor;

float plasma(vec2 uv, float scale, float time) {
    float v = cos(uv.x * uv.y * scale) - cos(uv.x / (0.4 + uv.y) + time);
    float f = floor(v);
    float c = ceil(v);
    float e = min(c - v, v - f);
    return min(pow(e * 1.8, 0.7), 1.0);
}

vec3 phobosPlasma(vec2 fragCoord) {
    float scale = 10.0;
    float deform = 0.6;
    vec2 uv = fragCoord / u_Resolution.xy - 0.5;
    uv.y *= deform;

    float r0 = plasma(uv, scale, u_Time);
    float r1 = plasma(uv, scale * 2.0, u_Time * 1.5 + 0.32);
    float r = r0 * r1;
    vec2 centeruv = fragCoord / u_Resolution.xy * 2.0 - 1.0;
    r *= 1.0 - pow(length(centeruv * centeruv) * 0.8, 6.0);
    return clamp(r * u_Color.rgb, 0.0, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(phobosPlasma(texCoord * u_Resolution.xy), mask.a * u_Opacity);
}
