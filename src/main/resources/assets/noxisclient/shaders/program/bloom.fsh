#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform int Width;
uniform int FastLines;
uniform float GlowMultiplier;
uniform int GlowQuality;
uniform vec4 FillColor;
uniform vec4 OutlineColor;

in vec2 texCoord;
out vec4 fragColor;

float occupiedAt(vec2 pixelOffset) {
    vec2 sampleUv = texCoord + pixelOffset / max(InSize, vec2(1.0));
    return texture(DiffuseSampler, sampleUv).a > 0.0 ? 1.0 : 0.0;
}

float calculateBloom() {
    int width = clamp(Width, 0, 10);
    if (width == 0) {
        return 0.0;
    }

    int quality = clamp(GlowQuality, 1, 6);
    int extent = width * quality;
    float occupied = 0.0;

    occupied += occupiedAt(vec2( extent, 0));
    occupied += occupiedAt(vec2(-extent, 0));
    occupied += occupiedAt(vec2(0,  extent));
    occupied += occupiedAt(vec2(0, -extent));
    occupied += occupiedAt(vec2( extent,  extent));
    occupied += occupiedAt(vec2( extent, -extent));
    occupied += occupiedAt(vec2(-extent,  extent));
    occupied += occupiedAt(vec2(-extent, -extent));

    if (FastLines != 0 && width > 2 && occupied == 0.0) {
        return 0.0;
    }

    for (int x = -extent; x <= extent; x += quality) {
        for (int y = -extent; y <= extent; y += quality) {
            if (x == 0 && y == 0) {
                continue;
            }

            bool skipSample = (sign(x) == extent && sign(y) == extent)
                    || (sign(x) == extent && y == 0)
                    || (sign(y) == 0 && x == 0);
            if (skipSample) {
                continue;
            }

            occupied += occupiedAt(vec2(x, y));
        }
    }

    float sampleCount = float((width * width + width) * 4);
    return clamp(occupied / max(sampleCount, 1.0), 0.0, 1.0) * max(GlowMultiplier, 0.0);
}

bool touchesMask() {
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            if (x == 0 && y == 0) {
                continue;
            }
            if (occupiedAt(vec2(x, y)) > 0.0) {
                return true;
            }
        }
    }
    return false;
}

void main() {
    vec4 center = texture(DiffuseSampler, texCoord);
    int width = clamp(Width, 0, 10);

    if (center.a > 0.0) {
        vec4 result = FillColor;
        if (width > 0) {
            result = mix(result, OutlineColor, max(GlowMultiplier, 0.0) - calculateBloom());
        }
        fragColor = result;
        return;
    }

    if (width == 0) {
        fragColor = vec4(0.0);
        return;
    }

    float bloom = calculateBloom();
    if (bloom <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 result = OutlineColor;
    result.a *= touchesMask() ? 1.0 : bloom;
    fragColor = result;
}
