#ifndef TOON_LIGHTING_GLSL
#define TOON_LIGHTING_GLSL

// Characters are lit the anime way. The smooth flag on their stored normal (GBufferNormal.glsl) marks them, and the
// world's own lighting is left as it is. Sun and moon fall in two clean tones, lit and shadowed, with a narrow soft
// seam between; the shadowed side keeps a warm share of the sun rather than dropping to the sky alone; a highlight
// is cut to a small sharp shape; and a rim of light runs round the silhouette, full on the lit side and faint on the
// shadowed one.

const float TOON_EDGE_LOW        = 0.02;
const float TOON_EDGE_HIGH       = 0.10;
const vec3  TOON_SHADOW_TINT     = vec3(0.86, 0.72, 0.80);
const float TOON_SHADOW_FILL     = 0.30;
const float TOON_SPECULAR_CUT    = 0.45;
const float TOON_SPECULAR_SHARE  = 0.35;
const float TOON_RIM_LOW         = 0.60;
const float TOON_RIM_HIGH        = 0.76;
const float TOON_RIM_STRENGTH    = 0.45;
const float TOON_RIM_SHADOWED    = 0.25;
const float TOON_RIM_WHITENESS   = 0.5;

// Two tones from a diffuse term, the seam between them soft over a narrow range
float resolveToonBand(float diffuse) {
    return smoothstep(TOON_EDGE_LOW, TOON_EDGE_HIGH, diffuse);
}

vec3 resolveToonLight(
    vec3 albedo,
    vec3 normalView,
    vec3 viewDir,
    float sunDiff,
    float sunVisibility,
    float sunSpec,
    float moonDiff,
    float skyAmbient,
    vec3 sunLight,
    vec3 moonLight) {

    float lit     = resolveToonBand(sunDiff) * sunVisibility;
    float moonLit = resolveToonBand(moonDiff);

    vec3 direct  = sunLight * albedo * lit + moonLight * albedo * moonLit;
    vec3 shadow  = sunLight * albedo * TOON_SHADOW_TINT * TOON_SHADOW_FILL * (1.0 - lit);
    vec3 shine   = sunLight * step(TOON_SPECULAR_CUT, sunSpec) * TOON_SPECULAR_SHARE * lit;

    float rim    = smoothstep(TOON_RIM_LOW, TOON_RIM_HIGH, 1.0 - max(dot(normalView, viewDir), 0.0));
    vec3  rimTone = mix(albedo, vec3(1.0), TOON_RIM_WHITENESS);
    vec3  rimLight = (sunLight + moonLight) * rimTone * rim * mix(TOON_RIM_SHADOWED, 1.0, lit) * TOON_RIM_STRENGTH;

    return albedo * skyAmbient + direct + shadow + shine + rimLight;
}

#endif
