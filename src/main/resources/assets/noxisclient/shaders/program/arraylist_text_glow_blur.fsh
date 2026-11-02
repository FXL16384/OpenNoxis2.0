#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 sampleStep;

uniform float Radius;
uniform float Sigma;
uniform float Strength;
uniform float FinalPass;

out vec4 fragColor;

const int MAX_RADIUS = 10;

void main() {
    float radius = clamp(round(Radius), 1.0, float(MAX_RADIUS));
    float sigma = max(Sigma, 0.01);
    vec3 weightedRgb = vec3(0.0);
    float weightedAlpha = 0.0;
    float weightSum = 0.0;

    for (int i = -MAX_RADIUS; i <= MAX_RADIUS; i++) {
        float offset = float(i);
        if (abs(offset) > radius) {
            continue;
        }

        float weight = exp(-(offset * offset) / (2.0 * sigma * sigma));
        vec4 sampleColor = texture(DiffuseSampler, texCoord + sampleStep * offset);
        weightedRgb += sampleColor.rgb * sampleColor.a * weight;
        weightedAlpha += sampleColor.a * weight;
        weightSum += weight;
    }

    float alpha = weightSum > 0.0 ? weightedAlpha / weightSum : 0.0;
    vec3 rgb = weightedAlpha > 0.0 ? weightedRgb / weightedAlpha : vec3(0.0);

    if (FinalPass > 0.5) {
        alpha *= Strength;
    }

    fragColor = vec4(rgb, alpha);
}
