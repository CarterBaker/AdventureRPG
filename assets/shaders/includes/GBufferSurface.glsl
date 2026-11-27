#ifndef GBUFFER_SURFACE_GLSL
#define GBUFFER_SURFACE_GLSL

// The world target's fourth layer, gSurface: metalness in r and emissive in g, b unused, and alpha forced to 1.0 as
// every target's is while the pass blends. Every shader drawing into the world target writes it, GBUFFER_SURFACE_NONE
// where the surface is neither metal nor glowing, so the lighting pass never reads what an earlier frame left there.

const vec4 GBUFFER_SURFACE_NONE = vec4(0.0, 0.0, 0.0, 1.0);

vec4 encodeGBufferSurface(float metallic, float emissive) {
    return vec4(metallic, emissive, 0.0, 1.0);
}

#endif
