#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Color;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;

vec3 phobosFract(vec2 fragCoord) {
    vec2 p = (2.0 * fragCoord - u_Resolution.xy) / u_Resolution.y;
    vec4 s1 = vec4(1.0, 1.0, 7.0, 2.0);
    vec4 s2 = vec4(-2.0, 1.0, 4.0, 4.6);

    float t = 0.5 * sin(u_Time) + 0.5;
    float a = mix(s1.x, s2.x, t);
    float b = mix(s1.y, s2.y, t);
    float n = mix(s1.z, s2.z, t);
    float m = mix(s1.w, s2.w, t);
    float amp = a * sin(PI * n * p.x) * sin(PI * m * p.y) + b * sin(PI * m * p.x) * sin(PI * n * p.y);
    float line = step(abs(amp), 0.1);

    vec3 glow = u_Color.rgb * line;
    glow += u_Color.rgb * 0.18 / max(abs(amp), 0.08);
    return clamp(glow, 0.0, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(phobosFract(texCoord * u_Resolution.xy), mask.a * u_Opacity);
}
