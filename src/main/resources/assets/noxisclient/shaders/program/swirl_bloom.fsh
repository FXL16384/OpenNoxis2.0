#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform float u_Scale;

in vec2 texCoord;
out vec4 fragColor;

vec4 swirlBloom(vec2 fragCoord) {
    vec2 R = u_Resolution.xy;
    float scale = max(u_Scale, 0.1);
    vec2 uv = ((5.3 * fragCoord - R) / R.y) / scale;
    vec4 O = vec4(0.0);
    float T = u_Time;

    for (float i = -1.5; i < 1.0; i += 0.32) {
        vec2 p = (uv + vec2(sin(T * 0.2 + i), cos(T * 0.15 + i)) * 0.3) / (0.25 + 0.1 * sin(T * 0.4 + i));

        float angle = T * 0.5 - i * 1.2 - length(p) * 0.6;
        float c = cos(angle);
        float s = sin(angle);
        p = p * mat2(c, -s, s, c);

        p.x += sin(p.y * 2.0 + T) * 0.2;
        p.y += cos(p.x * 2.0 - T) * 0.2;

        vec4 num = cos(i * 2.5 + vec4(0.0, 1.5, 3.0, 0.0) + T) + 1.0;
        float den = length(p * sin(p * 1.5)) * (0.7 + 0.4 * sin(T + i)) + 0.02;

        O += (num / den) * (0.5 + 0.4 * sin(i * 3.2 + T));
    }

    O = tanh(O * O / 62.5);
    O = pow(O, vec4(0.5050));
    return O;
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 color = swirlBloom(texCoord * u_Resolution.xy);
    fragColor = vec4(color.rgb, mask.a * u_Opacity);
}
