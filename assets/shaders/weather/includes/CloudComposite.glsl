#ifndef CLOUD_COMPOSITE_GLSL
#define CLOUD_COMPOSITE_GLSL

/*
 * Lays the weather pass's clouds over a surface another pass draws. Beside
 * its premultiplied cloud color, the weather pass writes a distance target:
 * x = distance along the view ray to the cloud surface it drew in that pixel,
 * y = density per block of the fog around a camera standing inside a cloud,
 * z and w = where the rain shafts in front of that cloud begin along the ray
 * and their optical depth, which the precipitation pass lays (CloudRain).
 * Clouds are solid, so a surface takes exactly the clouds standing in front
 * of it. The weather target is drawn at reduced resolution, so each of the
 * four texels around a pixel counts only when its own cloud is nearer than
 * the surface, and a cloud's edge against terrain stays as clean as its edge
 * against the sky. Inside a cloud the weather pass draws no surfaces, only
 * the fog's color, and the fog thickens with the surface's distance. How
 * much of a surface the clouds and fog hide is read on its own by passes
 * that treat clouds apart from what lies behind them, as the depth of field
 * keeps clouds sharp over a blurred surface.
 */

// Distance written where the weather pass drew fog but no surface; beyond
// anything a pass draws, and within half-float range.
const float CLOUD_COMPOSITE_NO_SURFACE = 60000.0;

// ── Composite ──────────────────────────────────────────────────────────────

// Premultiplied cloud standing in front of a surface fragmentDistance away
// along this pixel's view ray.
vec4 sampleCloudInFront(sampler2D cloudColor, sampler2D cloudDistance, vec2 uv, float fragmentDistance) {
    ivec2 size     = textureSize(cloudColor, 0);
    vec2  position = uv * vec2(size) - 0.5;
    ivec2 base     = ivec2(floor(position));
    vec2  blend    = position - vec2(base);
    vec4  cloud    = vec4(0.0);

    for (int y = 0; y <= 1; y++) {
        for (int x = 0; x <= 1; x++) {
            ivec2 texel  = clamp(base + ivec2(x, y), ivec2(0), size - 1);
            float weight = (x == 0 ? 1.0 - blend.x : blend.x) * (y == 0 ? 1.0 - blend.y : blend.y);

            if (texelFetch(cloudDistance, texel, 0).x < fragmentDistance)
            cloud += texelFetch(cloudColor, texel, 0) * weight;
        }
    }

    return cloud;
}

// How much of a surface fragmentDistance away the fog of a cloud the camera stands in hides, 0 to 1.
float resolveCloudFog(sampler2D cloudDistance, vec2 uv, float fragmentDistance) {
    float fogDensity = textureLod(cloudDistance, uv, 0.0).y;

    if (fogDensity <= 0.0)
    return 0.0;

    return 1.0 - exp(-fragmentDistance * fogDensity);
}

// A surface fragmentDistance away with the clouds in front of it laid over
// it, then the fog of any cloud the camera stands in.
vec3 compositeCloudScene(sampler2D cloudColor, sampler2D cloudDistance, vec2 uv, vec3 color, float fragmentDistance) {
    vec4 cloud = sampleCloudInFront(cloudColor, cloudDistance, uv, fragmentDistance);

    color = color * (1.0 - cloud.a) + cloud.rgb;

    float fog = resolveCloudFog(cloudDistance, uv, fragmentDistance);

    if (fog <= 0.0)
    return color;

    return mix(color, textureLod(cloudColor, uv, 0.0).rgb, fog);
}

// How much of a surface fragmentDistance away the clouds in front of it and the fog around the camera hide, 0 to 1.
float resolveCloudCover(sampler2D cloudColor, sampler2D cloudDistance, vec2 uv, float fragmentDistance) {
    float cloud = sampleCloudInFront(cloudColor, cloudDistance, uv, fragmentDistance).a;
    float fog   = resolveCloudFog(cloudDistance, uv, fragmentDistance);

    return clamp(1.0 - (1.0 - cloud) * (1.0 - fog), 0.0, 1.0);
}

#endif
