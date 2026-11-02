#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 u_Resolution;
uniform float u_Time;
uniform float u_Opacity;

in vec2 texCoord;
out vec4 fragColor;

vec4 ismFractal(vec2 fragCoord) {
    vec3 p;
    vec3 dr = vec3(1.0);
    vec3 origin = dr * 0.7;
    vec3 color = vec3(0.0);
    vec3 transient = dr * 0.0002;

    vec2 uv = fragCoord / u_Resolution.y - 0.5;
    vec2 flight = vec2(0.18 + 0.035 * sin(u_Time * 0.08), -0.08 + 0.03 * cos(u_Time * 0.06));

    float shift = 0.57;
    float stepSize = 0.1;
    float zoom = 0.4;
    float tile = 0.85;
    float speed = 0.05;
    float brightness = 0.0007;
    float dust = 0.5;
    float fade = 0.8;
    float saturation = 0.9;
    float intensity = 0.01;
    float darkNebula = 1.8;
    float travel = 0.1;
    float flightLen = max(length(flight), 0.001);
    float x = u_Time * speed * flightLen + 0.25;
    float amp = 1.0;

    mat2 flightRot = mat2(flight.x, flight.y, -flight.y, flight.x) * (1.0 / flightLen);
    dr.xy = uv * zoom * flightRot;
    origin += vec3(2.0 * x, x, -2.0);

    for (float ray = 0.0; ray < 23.0; ray += 1.0) {
        p = tile - mod(origin + travel * dr * 0.5, tile * 2.0) + transient * u_Time;

        float accum = 0.0;
        float previous = 0.0;
        for (float j = 0.0; j < 19.0; j += 1.0) {
            p = abs(p) / max(dot(p, p), 0.0001) - shift;
            float current = length(p);
            accum += abs(previous - current);
            previous = current;
        }

        float accum2 = accum * accum;
        float dustMask = max(0.0, dust - 0.001 * accum2);
        float starTint = 0.5 + 0.5 * tanh(2.0 * cos(100000000.0 * travel));
        if (ray > 6.0) {
            amp *= 1.0 - darkNebula - dustMask;
        }

        color += vec3(amp);
        color += travel * vec3(starTint, min(0.4, starTint), 1.0 - starTint) * accum * accum2 * brightness * amp;

        amp *= fade;
        travel += stepSize;
    }

    vec3 grayscale = vec3(length(color));
    vec3 finalColor = mix(grayscale, color, saturation) * intensity;
    finalColor = max(finalColor, vec3(0.0));
    return vec4(finalColor, 1.0);
}

void main() {
    vec4 mask = texture(DiffuseSampler, texCoord);
    if (mask.a <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 color = ismFractal(texCoord * u_Resolution.xy);
    fragColor = vec4(color.rgb, mask.a * u_Opacity);
}
