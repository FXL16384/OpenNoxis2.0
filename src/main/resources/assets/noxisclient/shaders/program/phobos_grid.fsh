#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Color;

in vec2 texCoord;
out vec4 fragColor;

vec3 permute(vec3 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

float snoise(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439,
    -0.577350269189626, 0.024390243902439);
    vec2 i = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    i = mod(i, 289.0);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    vec3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    return 130.0 * dot(m, g);
}

vec3 phobosGrid(vec2 fragCoord) {
    vec2 uv = (fragCoord - 0.5 * u_Resolution.xy) / min(u_Resolution.y, u_Resolution.x);
    float w = 30.0 / u_Resolution.y;
    uv *= 30.0;
    vec2 grid = floor(uv);
    vec2 subuv = fract(uv);

    vec3 col = vec3(0.0);
    for (int x = -1; x < 2; x++) {
        for (int y = -1; y < 2; y++) {
            vec2 os = vec2(float(x), float(y));
            vec2 g = grid + os;
            vec2 local = subuv - os;
            float r = snoise(g * 0.1 + u_Time * 0.5) * 0.2 + 0.3;
            col += vec3(smoothstep(r + w, r - w, length(local - 0.5)));
        }
    }

    return clamp(col * u_Color.rgb, 0.0, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(phobosGrid(texCoord * u_Resolution.xy), mask.a * u_Opacity);
}
