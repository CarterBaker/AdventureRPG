#ifndef CHARACTER_FACE_GLSL
#define CHARACTER_FACE_GLSL

// A character's albedo is a near-white tile multiplied by its part's tint. Fragments landing inside the head's face
// tile are re-mapped to face-local 0..1 and every chosen feature overlay (eyes, brows, mouth) is alpha-blended on
// top, each sampled from its own tile rect in the same array. A zero-width region means that slot is empty.

bool hasRegion(vec4 region) {
    return region.z > region.x;
}

vec4 sampleRegion(sampler2DArray textureArray, int layer, vec4 region, vec2 localUV) {
    vec2 uv = mix(region.xy, region.zw, clamp(localUV, 0.0, 1.0));
    return texture(textureArray, vec3(uv, float(layer)));
}

vec3 applyOverlay(sampler2DArray textureArray, int layer, vec3 albedo, vec4 region, vec2 faceUV, vec3 tint) {
    if (!hasRegion(region))
    return albedo;

    vec4 overlay = sampleRegion(textureArray, layer, region, faceUV);
    return mix(albedo, overlay.rgb * tint, overlay.a);
}

vec3 resolveCharacterAlbedo(
    sampler2DArray textureArray,
    int layer,
    vec2 uv,
    vec3 baseColor,
    vec3 tint,
    vec3 detailTint,
    vec4 faceRegion,
    vec4 eyesRegion,
    vec4 browsRegion,
    vec4 mouthRegion) {

    vec3 albedo = baseColor * tint;

    if (!hasRegion(faceRegion)
        || any(lessThan(uv, faceRegion.xy))
        || any(greaterThan(uv, faceRegion.zw)))
    return albedo;

    vec2 faceUV = (uv - faceRegion.xy) / (faceRegion.zw - faceRegion.xy);

    albedo = applyOverlay(textureArray, layer, albedo, eyesRegion,  faceUV, vec3(1.0));
    albedo = applyOverlay(textureArray, layer, albedo, browsRegion, faceUV, detailTint);
    albedo = applyOverlay(textureArray, layer, albedo, mouthRegion, faceUV, vec3(1.0));

    return albedo;
}

#endif
