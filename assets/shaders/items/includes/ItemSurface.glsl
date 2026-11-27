#ifndef ITEM_SURFACE_GLSL
#define ITEM_SURFACE_GLSL

#include "includes/TileTextures.glsl"
#include "includes/MaterialSpecular.glsl"

// An item's surface read from its tile's maps as a character's is: ambient occlusion and metalness from the material
// map, roughness and emissive from the extra set, the highlight shaped by roughness about the item's own specular with
// a metal's taking its place (MaterialSpecular), so steel shines and mirrors the sky in its own colour and a torch's
// flame gives off light. A tile with no extra maps reads their defaults: neutral roughness and no glow.

struct ItemSurface {
    vec4  albedo;
    float ao;
    float specular;
    float metallic;
    float emissive;
};

// A face's surface at its UV. Takes derivatives, so call it from uniform control flow.
ItemSurface sampleItemSurface(int tile, vec2 uv, float baseSpecular) {
    vec2     dx    = dFdx(uv);
    vec2     dy    = dFdy(uv);
    TileRead read  = resolveTileReadGrad(tile, dx, dy);
    vec4     maps  = sampleTileMaterial(read, uv, dx, dy);
    vec4     extra = sampleTileExtra(read, uv, dx, dy);

    ItemSurface surface;
    surface.albedo   = sampleTileAlbedo(read, uv, dx, dy);
    surface.ao       = maps.r;
    surface.specular = resolveMaterialSpecular(baseSpecular, maps.b, extra.g);
    surface.metallic = maps.b;
    surface.emissive = extra.a;

    return surface;
}

#endif
