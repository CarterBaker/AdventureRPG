#ifndef TILE_TEXTURES_GLSL
#define TILE_TEXTURES_GLSL

#include "includes/SettingsData.glsl"

// Every texture tile, streamed into fixed pools. Each tier (128, 256 and 512 texels a side) has one array per map:
// albedo (alpha is the cutout), normal (tangent-space x and y, z rebuilt here) and material (ambient occlusion,
// specular, metalness, height). The fallback pool holds every tile at 16 texels, the extra set holds growth,
// roughness, tint and emissive for only the tiles that carry them (its layer 0 holds their defaults), and the lookup
// turns a tile ID into its layers. A tile's UVs run 0..1 across it whatever its size and repeat beyond. A read
// takes the tier its footprint calls for from the highest one resident, blends into the next tier across the band
// where they meet so a tier never pops, and reads the fallback while the tile has no tier resident. Gradients are
// always explicit, so every read is safe inside any branch. The lookup layout must match the Texture Streaming
// group in EngineSetting and TexturePoolBranch.

uniform usampler2D     u_tileSlots;
uniform sampler2DArray u_tileAlbedo0;
uniform sampler2DArray u_tileAlbedo1;
uniform sampler2DArray u_tileAlbedo2;
uniform sampler2DArray u_tileAlbedoFallback;
uniform sampler2DArray u_tileNormal0;
uniform sampler2DArray u_tileNormal1;
uniform sampler2DArray u_tileNormal2;
uniform sampler2DArray u_tileNormalFallback;
uniform sampler2DArray u_tileMaterial0;
uniform sampler2DArray u_tileMaterial1;
uniform sampler2DArray u_tileMaterial2;
uniform sampler2DArray u_tileMaterialFallback;
uniform sampler2DArray u_tileExtra;

const int   TILE_LOOKUP_WIDTH_BITS   = 8;
const int   TILE_LOOKUP_WIDTH_MASK   = 255;
const int   TILE_WIDE_BITS           = 16;
const int   TILE_NARROW_BITS         = 8;
const uint  TILE_WIDE_MASK           = 65535u;
const uint  TILE_NARROW_MASK         = 255u;
const int   TILE_TIER_FALLBACK       = -1;
const int   TILE_TIER_TOP            = 2;
const float TILE_BASE_SIZE           = 128.0;
const float TILE_FALLBACK_LOD_OFFSET = 3.0;
const float TILE_TIER_LEAD           = 0.5;
const float TILE_TIER_BLEND_RATE     = 2.0;
const float TILE_MIN_FOOTPRINT       = 1e-6;

struct TileRead {
    int   lowTier;
    float lowLayer;
    int   highTier;
    float highLayer;
    float blend;
    float extraLayer;
};

// The tile's two lookup words: the base and middle tier slots, then the top tier slot, extra layer and fallback layer
uvec2 fetchTileSlots(int tileID) {
    return texelFetch(u_tileSlots, ivec2(tileID & TILE_LOOKUP_WIDTH_MASK, tileID >> TILE_LOOKUP_WIDTH_BITS), 0).rg;
}

// Base-tier texels a pixel spans, the way anisotropic filtering measures it: the minor axis, or the major one
// spread over every sample the filter may take
float resolveTileFootprint(vec2 dx, vec2 dy) {
    float lengthX = length(dx);
    float lengthY = length(dy);
    float major   = max(lengthX, lengthY);
    float minor   = min(lengthX, lengthY);

    return max(major / max(u_textureAnisotropy, 1.0), minor) * TILE_BASE_SIZE;
}

// The tiers a read of the given footprint takes: the one it calls for from those resident, and the next above it
// with its blend weight where the two meet
TileRead resolveTileRead(int tileID, float footprint) {
    uvec2 words = fetchTileSlots(tileID);
    float layers[3] = float[3](
        float(words.x & TILE_WIDE_MASK),
        float(words.x >> TILE_WIDE_BITS),
        float(words.y & TILE_NARROW_MASK));
    int top = layers[2] > 0.0 ? 2 : (layers[1] > 0.0 ? 1 : (layers[0] > 0.0 ? 0 : TILE_TIER_FALLBACK));

    TileRead read;
    read.extraLayer = float((words.y >> TILE_NARROW_BITS) & TILE_NARROW_MASK);

    if (top == TILE_TIER_FALLBACK) {
        read.lowTier   = TILE_TIER_FALLBACK;
        read.lowLayer  = float(words.y >> TILE_WIDE_BITS);
        read.highTier  = TILE_TIER_FALLBACK;
        read.highLayer = read.lowLayer;
        read.blend     = 0.0;
        return read;
    }

    float wanted     = -log2(max(footprint, TILE_MIN_FOOTPRINT));
    float coordinate = clamp(wanted + TILE_TIER_LEAD, 0.0, float(top));
    int   low        = min(int(coordinate), top);
    int   high       = min(low + 1, top);

    read.lowTier   = low;
    read.lowLayer  = layers[low];
    read.highTier  = high;
    read.highLayer = layers[high];
    read.blend     = high > low ? clamp((coordinate - float(low)) * TILE_TIER_BLEND_RATE, 0.0, 1.0) : 0.0;
    return read;
}

TileRead resolveTileReadGrad(int tileID, vec2 dx, vec2 dy) {
    return resolveTileRead(tileID, resolveTileFootprint(dx, dy));
}

vec3 decodeTileNormal(vec2 encoded) {
    vec2 xy = encoded * 2.0 - 1.0;
    return vec3(xy, sqrt(max(1.0 - dot(xy, xy), 0.0)));
}

// Albedo

vec4 readTileAlbedo(int tier, float layer, vec2 uv, vec2 dx, vec2 dy) {
    vec3 coordinate = vec3(uv, layer);

    if (tier == 2) return textureGrad(u_tileAlbedo2, coordinate, dx, dy);
    if (tier == 1) return textureGrad(u_tileAlbedo1, coordinate, dx, dy);
    if (tier == 0) return textureGrad(u_tileAlbedo0, coordinate, dx, dy);
    return textureGrad(u_tileAlbedoFallback, coordinate, dx, dy);
}

vec4 sampleTileAlbedo(TileRead read, vec2 uv, vec2 dx, vec2 dy) {
    vec4 albedo = readTileAlbedo(read.lowTier, read.lowLayer, uv, dx, dy);

    if (read.blend > 0.0)
        albedo = mix(albedo, readTileAlbedo(read.highTier, read.highLayer, uv, dx, dy), read.blend);

    return albedo;
}

// Normal

vec2 readTileNormal(int tier, float layer, vec2 uv, vec2 dx, vec2 dy) {
    vec3 coordinate = vec3(uv, layer);

    if (tier == 2) return textureGrad(u_tileNormal2, coordinate, dx, dy).rg;
    if (tier == 1) return textureGrad(u_tileNormal1, coordinate, dx, dy).rg;
    if (tier == 0) return textureGrad(u_tileNormal0, coordinate, dx, dy).rg;
    return textureGrad(u_tileNormalFallback, coordinate, dx, dy).rg;
}

// The tangent-space normal, unit length
vec3 sampleTileNormal(TileRead read, vec2 uv, vec2 dx, vec2 dy) {
    vec2 encoded = readTileNormal(read.lowTier, read.lowLayer, uv, dx, dy);

    if (read.blend > 0.0)
        encoded = mix(encoded, readTileNormal(read.highTier, read.highLayer, uv, dx, dy), read.blend);

    return decodeTileNormal(encoded);
}

// Material

vec4 readTileMaterial(int tier, float layer, vec2 uv, vec2 dx, vec2 dy) {
    vec3 coordinate = vec3(uv, layer);

    if (tier == 2) return textureGrad(u_tileMaterial2, coordinate, dx, dy);
    if (tier == 1) return textureGrad(u_tileMaterial1, coordinate, dx, dy);
    if (tier == 0) return textureGrad(u_tileMaterial0, coordinate, dx, dy);
    return textureGrad(u_tileMaterialFallback, coordinate, dx, dy);
}

// Ambient occlusion, specular, metalness and height in r, g, b and a
vec4 sampleTileMaterial(TileRead read, vec2 uv, vec2 dx, vec2 dy) {
    vec4 material = readTileMaterial(read.lowTier, read.lowLayer, uv, dx, dy);

    if (read.blend > 0.0)
        material = mix(material, readTileMaterial(read.highTier, read.highLayer, uv, dx, dy), read.blend);

    return material;
}

// The material at a level of detail measured in base-tier mips, for stages with no derivatives
vec4 readTileMaterialLod(int tier, float layer, vec2 uv, float baseLod) {
    vec3 coordinate = vec3(uv, layer);

    if (tier == 2) return textureLod(u_tileMaterial2, coordinate, baseLod + 2.0);
    if (tier == 1) return textureLod(u_tileMaterial1, coordinate, baseLod + 1.0);
    if (tier == 0) return textureLod(u_tileMaterial0, coordinate, baseLod);
    return textureLod(u_tileMaterialFallback, coordinate, baseLod - TILE_FALLBACK_LOD_OFFSET);
}

vec4 sampleTileMaterialLod(TileRead read, vec2 uv, float baseLod) {
    vec4 material = readTileMaterialLod(read.lowTier, read.lowLayer, uv, baseLod);

    if (read.blend > 0.0)
        material = mix(material, readTileMaterialLod(read.highTier, read.highLayer, uv, baseLod), read.blend);

    return material;
}

// Extra

// Growth, roughness, tint and emissive in r, g, b and a; a tile with none reads their defaults
vec4 sampleTileExtra(TileRead read, vec2 uv, vec2 dx, vec2 dy) {
    return textureGrad(u_tileExtra, vec3(uv, read.extraLayer), dx, dy);
}

#endif
