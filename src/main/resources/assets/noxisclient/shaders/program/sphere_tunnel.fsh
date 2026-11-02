#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;

in vec2 texCoord;
out vec4 fragColor;

const int MAX_STEPS = 200;
const int NUM_SPHERES = 12;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float sphere(vec3 pos, float radius, vec3 smpl) {
    return length(pos - smpl) - radius;
}

float plane(vec3 dir, float offset, vec3 smpl) {
    return dot(dir, smpl) + offset;
}

float dfDist(vec3 smpl) {
    float T1 = 10.0;
    float T2 = 2.0 * T1;

    float result = 10000.0;

    smpl.y += sin(smpl.z * 0.2 + u_Time) * sin(u_Time * 1.33)
              + sin(smpl.x * 0.3 + u_Time) * sin(u_Time * 3.22)
              + sin(smpl.x * 0.5 + smpl.z * 0.22 + u_Time) * sin(u_Time * 2.22 + smpl.z * 0.1);
    float o = floor((smpl.z + T1) / T2);
    smpl.x += o * 7.0;
    smpl.xz = mod(smpl.xz + T1, T2) - T1;

    for (int i = 0; i < NUM_SPHERES; i++) {
        float t = float(i) / float(NUM_SPHERES);
        float n = t + u_Time * 0.25 + o * 0.5;
        vec3 pos = vec3(sin(n * 5.0) * 5.0, cos(n * 3.0) * 9.0, cos(n * 2.0) * 3.0 + 5.0);
        float radius = hash11(t * t + o * 0.173) * 2.0 + 1.4;
        result = min(result, sphere(pos, radius, smpl));
    }

    result = min(result, plane(vec3(0.0, -1.0, 0.0), 10.0, smpl));
    result = min(result, plane(vec3(0.0, 1.0, 0.0), 10.0, smpl));

    return result;
}

vec3 dfNormal(vec3 smpl) {
    const float E = 0.04;

    float d0 = dfDist(smpl);
    float dX = dfDist(smpl + vec3(E, 0.0, 0.0));
    float dY = dfDist(smpl + vec3(0.0, E, 0.0));
    float dZ = dfDist(smpl + vec3(0.0, 0.0, E));

    return normalize(vec3(dX - d0, dY - d0, dZ - d0));
}

float dfOcclusion(vec3 smpl, vec3 normal) {
    float N = 1.0;
    return clamp(dfDist(smpl + normal * N) / N, 0.0, 1.0);
}

float trace(inout vec3 pos, vec3 dir, out vec3 normal) {
    int steps = 0;
    for (int i = 0; i < MAX_STEPS; i++) {
        steps++;
        float d = dfDist(pos);
        pos += d * dir;

        if (d < 0.001) {
            break;
        }
    }

    normal = dfNormal(pos);
    return float(steps) / float(MAX_STEPS);
}

vec4 sphereTunnel(vec2 fragCoord) {
    vec3 opos = vec3(4.5, sin(u_Time * 0.4) * 3.0 + 2.0, -7.0 + u_Time * 3.0);
    vec3 pos = opos;
    vec3 dir = normalize(vec3((fragCoord.x - u_Resolution.x * 0.5) / u_Resolution.y, fragCoord.y / u_Resolution.y - 0.5, 1.0));
    vec3 normal;

    float steps = trace(pos, dir, normal);
    float occ = dfOcclusion(pos, normal);
    float fogAmt = 1.0 - exp(-distance(opos, pos) * 0.01);
    vec3 fogCol = vec3(0.2, 0.14, 0.18);

    vec3 diffuse = vec3(0.4, 0.5, 0.6) * dot(normal, normalize(vec3(1.0, 0.3, -1.0)));
    vec3 ambient = vec3(0.4, 0.2, 0.1);
    vec3 color = (ambient + diffuse) * vec3(1.0 - steps) + pow(1.0 - occ, 1.5) * vec3(1.0, 0.9, 0.8) * 0.8;

    color = mix(color, fogCol, fogAmt);
    color = (1.0 - exp(-color * 1.5)) * 1.3;
    return vec4(color, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 color = sphereTunnel(texCoord * u_Resolution.xy);
    fragColor = vec4(color.rgb, mask.a * u_Opacity);
}
