#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Color;

in vec2 texCoord;
out vec4 fragColor;

vec3 phobosMarble(vec2 fragCoord) {
    vec2 uv = (2.0 * fragCoord - u_Resolution.xy) / min(u_Resolution.x, u_Resolution.y);

    for (float i = 1.0; i < 10.0; i += 1.0) {
        uv.x += 0.6 / i * cos(i * 2.5 * uv.y + u_Time);
        uv.y += 0.6 / i * cos(i * 1.5 * uv.x + u_Time);
    }

    vec3 baseColor = vec3(0.1) / max(abs(sin(u_Time - uv.y - uv.x)), 0.05);
    return clamp(baseColor * u_Color.rgb, 0.0, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(phobosMarble(texCoord * u_Resolution.xy), mask.a * u_Opacity);
}
