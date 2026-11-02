#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Color;

in vec2 texCoord;
out vec4 fragColor;

#define NUM_LAYERS 7.0

float rand(vec2 p) {
    return fract(sin(p.x * 317.0 + p.y * 137.0) * 132.0);
}

float star(vec2 uv, float rays) {
    float d = max(length(uv), 0.001);
    float m = 0.1 / d;
    m += 1.0 / exp(100.0 * abs(uv.x * uv.y)) * rays;
    m *= smoothstep(1.0, 0.2, d);
    return m;
}

vec3 starLayer(vec2 uv) {
    float t = u_Time * 1.8;
    vec2 gv = fract(uv) - 0.5;
    vec2 id = floor(uv);
    vec3 col = vec3(0.0);

    for (float y = -1.0; y <= 1.0; y += 1.0) {
        for (float x = -1.0; x <= 1.0; x += 1.0) {
            vec2 offs = vec2(x, y);
            float r = rand(id + offs);
            float size = max(0.3, fract(r * 127.0));
            float rays = exp((size - 1.0) * 50.0) * 2.0;
            float blink = sin(t * r) * 0.5 + 0.5;
            vec3 c = 2.0 * normalize(vec3(0.8, 0.4, 0.9)) * fract(vec3(r * 123.0, r * 987.0, r * 567.0));
            col += blink * c * size * star(gv - offs - vec2(r, fract(r * 275.0)) + 0.5, rays);
        }
    }

    return col;
}

mat2 rot(float a) {
    float s = sin(a);
    float c = cos(a);
    return mat2(c, -s, s, c);
}

vec3 phobosColorful(vec2 fragCoord) {
    vec2 uv = (fragCoord - u_Resolution.xy * 0.5) / u_Resolution.y;
    vec3 col = vec3(0.0);
    float t = u_Time * 0.08;
    uv *= rot(t);

    for (float i = 0.0; i < 1.0; i += 1.0 / NUM_LAYERS) {
        float depth = fract(i + t);
        float scale = mix(20.0, 2.0, depth);
        float fade = depth * smoothstep(1.0, 0.9, depth);
        col += fade * starLayer(uv * scale + 323.0 * i);
    }

    return col * mix(vec3(1.0), u_Color.rgb, 0.35);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(phobosColorful(texCoord * u_Resolution.xy), mask.a * u_Opacity);
}
