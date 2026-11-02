#version 150

uniform sampler2D TitleImage;
uniform vec2 u_Resolution;
uniform vec2 u_ImageSize;
uniform float u_Time;
uniform float u_Opacity;
uniform vec4 u_Mouse;

in vec2 texCoord;
out vec4 fragColor;

vec2 coverUv(vec2 uv, vec2 screenSize, vec2 imageSize) {
    float screenAspect = max(screenSize.x, 1.0) / max(screenSize.y, 1.0);
    float imageAspect = max(imageSize.x, 1.0) / max(imageSize.y, 1.0);
    vec2 crop = vec2(1.0);

    if (screenAspect > imageAspect) {
        crop.y = imageAspect / screenAspect;
    } else {
        crop.x = screenAspect / imageAspect;
    }

    return (uv - 0.5) * crop + 0.5;
}

void main() {
    vec2 uv = texCoord;
    vec2 imageUv = coverUv(uv, u_Resolution, u_ImageSize);
    if (u_Mouse.z > 0.5) {
        vec2 mouse = clamp(u_Mouse.xy / max(u_Resolution, vec2(1.0)), vec2(0.0), vec2(1.0));
        imageUv += (mouse - 0.5) * vec2(-0.045, 0.045);
    }
    imageUv = clamp(imageUv, vec2(0.001), vec2(0.999));
    imageUv.y = 1.0 - imageUv.y;
    vec3 color = texture(TitleImage, imageUv).rgb;

    float aspect = max(u_Resolution.x, 1.0) / max(u_Resolution.y, 1.0);
    vec2 centered = (uv - 0.5) * vec2(aspect, 1.0);
    float vignette = smoothstep(1.2, 0.2, length(centered));
    float breathe = sin(u_Time * 0.7 + uv.x * 5.0 + uv.y * 3.0) * 0.015;

    color *= mix(0.82, 1.02, vignette);
    color = mix(color, color * vec3(1.04, 0.98, 1.0), 0.18 + breathe);

    fragColor = vec4(color, clamp(u_Opacity, 0.0, 1.0));
}
