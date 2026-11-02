#version 150

// Heartfelt - by Martijn Steinrucken aka BigWings - 2017
// License: Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported.
// Adapted for Noxis' Minecraft post shader pipeline.

uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Mouse;

in vec2 texCoord;
out vec4 fragColor;

float NoxisSmooth(float a, float b, float t) {
    float x = clamp((t - a) / (b - a), 0., 1.);
    return x * x * (3. - 2. * x);
}

#define S(a, b, t) NoxisSmooth(a, b, t)
#define HAS_HEART
#define USE_POST_PROCESSING

vec3 N13(float p) {
    vec3 p3 = fract(vec3(p) * vec3(.1031, .11369, .13787));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract(vec3((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y, (p3.y + p3.z) * p3.x));
}

float Saw(float b, float t) {
    return S(0., b, t) * S(1., b, t);
}

vec2 DropLayer2(vec2 uv, float t) {
    vec2 UV = uv;

    uv.y += t * .75;
    vec2 a = vec2(6., 1.);
    vec2 grid = a * 2.;
    vec2 id = floor(uv * grid);

    float colShift = fract(sin(id.x * 12345.564) * 7658.76);
    uv.y += colShift;

    id = floor(uv * grid);
    vec3 n = N13(id.x * 35.2 + id.y * 2376.1);
    vec2 st = fract(uv * grid) - vec2(.5, 0);

    float x = n.x - .5;

    float y = UV.y * 20.;
    float wiggle = sin(y + sin(y));
    x += wiggle * (.5 - abs(x)) * (n.z - .5);
    x *= .7;
    float ti = fract(t + n.z);
    y = (Saw(.85, ti) - .5) * .9 + .5;
    vec2 p = vec2(x, y);

    float d = length((st - p) * a.yx);
    float mainDrop = S(.4, .0, d);

    float r = sqrt(S(1., y, st.y));
    float cd = abs(st.x - x);
    float trail = S(.23 * r, .15 * r * r, cd);
    float trailFront = S(-.02, .02, st.y - y);
    trail *= trailFront * r * r;

    y = UV.y;
    float trail2 = S(.2 * r, .0, cd);
    float droplets = max(0., (sin(y * (1. - y) * 120.) - st.y)) * trail2 * trailFront * n.z;
    y = fract(y * 10.) + (st.y - .5);
    float dd = length(st - vec2(x, y));
    droplets = S(.3, 0., dd);
    float m = mainDrop + droplets * r * trailFront;

    return vec2(m, trail);
}

float StaticDrops(vec2 uv, float t) {
    uv *= 40.;

    vec2 id = floor(uv);
    uv = fract(uv) - .5;
    vec3 n = N13(id.x * 107.45 + id.y * 3543.654);
    vec2 p = (n.xy - .5) * .7;
    float d = length(uv - p);

    float fade = Saw(.025, fract(t + n.z));
    return S(.3, 0., d) * fract(n.z * 10.) * fade;
}

vec2 Drops(vec2 uv, float t, float l0, float l1, float l2) {
    float s = StaticDrops(uv, t) * l0;
    vec2 m1 = DropLayer2(uv, t) * l1;
    vec2 m2 = DropLayer2(uv * 1.85, t) * l2;

    float c = s + m1.x + m2.x;
    c = S(.3, 1., c);

    return vec2(c, max(m1.y * l0, m2.y * l1));
}

vec3 HeartfeltBackground(vec2 uv, float t, float focus) {
    vec2 p = uv - .5;
    float y = clamp(uv.y, 0., 1.);

    vec3 col = mix(vec3(.015, .018, .035), vec3(.115, .045, .095), y);
    float cloud = sin(p.x * 8. + p.y * 5. + t * .13) * .5 + .5;
    cloud *= sin(p.x * 19. - p.y * 9. - t * .08) * .5 + .5;
    col += cloud * vec3(.03, .024, .04);

    float horizon = exp(-18. * abs(p.y + .18));
    col += horizon * vec3(.13, .035, .085);

    float glow = exp(-7.5 * length(p - vec2(.12, -.05)));
    col += glow * vec3(.22, .045, .12);

    float defocus = clamp(focus / 8., 0., 1.);
    float gray = dot(col, vec3(.299, .587, .114));
    col = mix(col, vec3(gray), defocus * .28);

    return col;
}

void mainImage(out vec4 outColor, in vec2 fragCoord) {
    vec3 iResolution = vec3(max(u_Resolution, vec2(1.)), 1.);
    vec4 iMouse = u_Mouse;
    float iTime = u_Time;

    vec2 uv = (fragCoord.xy - .5 * iResolution.xy) / iResolution.y;
    vec2 UV = fragCoord.xy / iResolution.xy;
    vec3 M = iMouse.xyz / iResolution.xyz;
    float T = iTime + M.x * 2.;

#ifdef HAS_HEART
    T = mod(iTime, 102.);
    T = mix(T, M.x * 102., M.z > 0. ? 1. : 0.);
#endif

    float t = T * .2;
    float rainAmount = iMouse.z > 0. ? M.y : sin(T * .05) * .3 + .7;

    float maxBlur = mix(3., 6., rainAmount);
    float minBlur = 2.;
    float story = 0.;
    float heart = 0.;

#ifdef HAS_HEART
    story = S(0., 70., T);

    t = min(1., T / 70.);
    t = 1. - t;
    t = (1. - t * t) * 70.;

    float zoom = mix(.3, 1.2, story);
    uv *= zoom;
    minBlur = 4. + S(.5, 1., story) * 3.;
    maxBlur = 6. + S(.5, 1., story) * 1.5;

    vec2 hv = uv - vec2(.0, -.1);
    hv.x *= .5;
    float heartScale = S(110., 70., T);
    hv.y -= sqrt(abs(hv.x)) * .5 * heartScale;
    heart = length(hv);
    heart = S(.4 * heartScale, .2 * heartScale, heart) * heartScale;
    rainAmount = heart;

    maxBlur -= heart;
    uv *= 1.5;
    t *= .25;
#else
    float zoom = -cos(T * .2);
    uv *= .7 + zoom * .3;
#endif

    UV = (UV - .5) * (.9 + zoom * .1) + .5;

    float staticDrops = S(-.5, 1., rainAmount) * 2.;
    float layer1 = S(.25, .75, rainAmount);
    float layer2 = S(.0, .5, rainAmount);

    vec2 c = Drops(uv, t, staticDrops, layer1, layer2);
    vec2 e = vec2(.001, 0.);
    float cx = Drops(uv + e, t, staticDrops, layer1, layer2).x;
    float cy = Drops(uv + e.yx, t, staticDrops, layer1, layer2).x;
    vec2 n = vec2(cx - c.x, cy - c.x);

#ifdef HAS_HEART
    n *= 1. - S(60., 85., T);
    c.y *= 1. - S(80., 100., T) * .8;
#endif

    float focus = mix(maxBlur - c.y, minBlur, S(.1, .2, c.x));
    vec3 col = HeartfeltBackground(UV + n, T, focus);

#ifdef USE_POST_PROCESSING
    t = (T + 3.) * .5;
    float colFade = sin(t * .2) * .5 + .5 + story;
    col *= mix(vec3(1.), vec3(.8, .9, 1.3), colFade);
    float fade = S(0., 10., T);
    float lightning = sin(t * sin(t * 10.));
    lightning *= pow(max(0., sin(t + sin(t))), 10.);
    col *= 1. + lightning * fade * mix(1., .1, story * story);
    UV -= .5;
    col *= 1. - dot(UV, UV);

#ifdef HAS_HEART
    col = mix(pow(col, vec3(1.2)), col, heart);
    fade *= S(102., 97., T);
#endif

    col *= fade;
#endif

    outColor = vec4(col, 1.);
}

void main() {
    vec2 resolution = max(u_Resolution, vec2(1.));
    mainImage(fragColor, texCoord * resolution);
    fragColor.a *= clamp(u_Opacity, 0., 1.);
}
