#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 texelSize;
uniform float radius;
uniform vec2 direction;
uniform vec2 BlurXY;
uniform vec2 BlurCoord;
uniform float BlurRadius;

float roundedRectDistance(vec2 fragCoord, vec2 rectPos, vec2 rectSize, float radiusValue) {
    vec2 size = max(rectSize, vec2(0.0));
    float r = min(radiusValue, min(size.x, size.y) * 0.5);
    vec2 local = fragCoord - rectPos - size * 0.5;
    vec2 halfSize = max(size * 0.5 - vec2(r), vec2(0.0));
    vec2 q = abs(local) - halfSize;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

float roundedRectMask(vec2 fragCoord, vec2 rectPos, vec2 rectSize, float radiusValue) {
    float distance = roundedRectDistance(fragCoord, rectPos, rectSize, radiusValue);
    float aa = max(fwidth(distance), 1.0);
    return 1.0 - smoothstep(-aa, aa, distance);
}

void main() {
    bool blurAll = BlurCoord.x <= 0.0 || BlurCoord.y <= 0.0;
    float blurMask = blurAll ? 1.0 : roundedRectMask(gl_FragCoord.xy, BlurXY, BlurCoord, BlurRadius);
    vec4 baseColor = texture(DiffuseSampler, texCoord);

    if (blurMask <= 0.0 || radius <= 0.0) {
        fragColor = baseColor;
        return;
    }

    vec4 color = vec4(0.0);
    float weightSum = 0.0;
    float sigma = max(radius * 0.5, 1.0);

    for (float i = -radius; i <= radius; i += 1.0) {
        vec2 offset = direction * texelSize * i;
        float weight = exp(-(i * i) / (2.0 * sigma * sigma));
        color += texture(DiffuseSampler, texCoord + offset) * weight;
        weightSum += weight;
    }

    fragColor = mix(baseColor, color / max(weightSum, 0.0001), blurMask);
}
