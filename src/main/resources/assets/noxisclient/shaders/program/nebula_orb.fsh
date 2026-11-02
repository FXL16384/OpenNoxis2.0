#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;

in vec2 texCoord;
out vec4 fragColor;

#define T (u_Time)

float orb(vec3 p) {
    float t = T * 4.0;
    return length(p - vec3(
            sin(sin(t * 0.2) + t * 0.4) * 6.0,
            1.0 + sin(sin(t * 0.5) + t * 0.2) * 4.0,
            12.0 + T + cos(t * 0.3) * 8.0));
}

vec4 nebulaOrb(vec2 fragCoord) {
    float d = 0.0;
    float a = 0.0;
    float e = 0.0;
    float s = 0.0;
    float t = T;
    vec3 p;
    vec2 u = fragCoord;
    vec4 o = vec4(0.0);

    u = (u + u - u_Resolution.xy) / u_Resolution.y;
    u += vec2(cos(t * 0.1) * 0.3, cos(t * 0.3) * 0.1);

    for (float i = 0.0; i < 128.0; i += 1.0) {
        p = vec3(u * d, d + t);
        e = orb(p) - 0.1;
        p.xy = p.xy * mat2(cos(0.1 * t + p.z / 8.0 + vec4(0.0, 33.0, 11.0, 0.0)));
        s = 4.0 - abs(p.y);

        for (a = 0.8; a < 32.0; a += a) {
            p += cos(0.7 * t + p.yzx) * 0.2;
            s -= abs(dot(sin(0.1 * t + p * a), vec3(0.6))) / a;
        }

        e = max(0.5 * e, 0.01);
        s = min(0.03 + 0.2 * abs(s), e);
        d += s;
        o += vec4(1.0) / (s + e * 3.0);
    }

    return tanh(o / 10.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 color = nebulaOrb(texCoord * u_Resolution.xy);
    fragColor = vec4(color.rgb, mask.a * u_Opacity);
}
