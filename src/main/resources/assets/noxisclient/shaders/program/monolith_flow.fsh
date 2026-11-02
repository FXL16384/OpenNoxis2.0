#version 150

uniform sampler2D DiffuseSampler;
uniform vec4 u_Color;
uniform vec4 u_Color2;
uniform vec2 u_Resolution;
uniform vec2 u_Mouse;
uniform float u_Scale;
uniform float u_Time;
uniform float u_Intensity;
uniform float u_Opacity;

in vec2 texCoord;
out vec4 fragColor;

#define TAU 6.28318530718
#define MAX_ITER 5

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 uv = (gl_FragCoord.xy / u_Resolution.xy + u_Mouse) * u_Scale;
    vec2 p = uv * TAU - 250.0;

    vec2 iterPos = p;
    float value = 1.0;
    float timeOffset = u_Time * 0.5 + 23.0;

    for (int n = 0; n < MAX_ITER; n++) {
        float iterTime = timeOffset * (1.0 - (3.5 / float(n + 1)));

        iterPos = p + vec2(
            cos(iterTime - iterPos.x) + sin(iterTime + iterPos.y),
            sin(iterTime - iterPos.y) + cos(iterTime + iterPos.x)
        );

        float sinX = sin(iterPos.x + iterTime);
        float cosY = cos(iterPos.y + iterTime);

        float compX = p.x * u_Intensity / max(abs(sinX), 0.001);
        float compY = p.y * u_Intensity / max(abs(cosY), 0.001);
        float distance = sqrt(compX * compX + compY * compY);

        value += 1.0 / distance;
    }

    value = 1.17 - pow(value / float(MAX_ITER), 1.4);

    float pwr = value * value;
    pwr = pwr * pwr;
    pwr = pwr * pwr;

    vec3 baseColor = vec3(abs(pwr));
    vec3 enhancedColor = clamp(baseColor + vec3(0.0, 0.35, 0.5), 0.0, 1.0);
    float luminance = dot(enhancedColor, vec3(0.299, 0.587, 0.114));

    vec3 gradientColor = mix(u_Color.rgb, u_Color2.rgb, smoothstep(0.0, 1.0, luminance));
    vec3 rgb = vec3(luminance) * gradientColor * 1.4;
    fragColor = vec4(rgb, u_Color.a * u_Opacity * mask.a);
}
