// AtmosphericFog.glsl
#ifndef ATMOSPHERIC_FOG_GLSL
#define ATMOSPHERIC_FOG_GLSL

#include "includes/CameraData.glsl"
#include "includes/PlayerPositionData.glsl"
#include "includes/SettingsData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/SunLightData.glsl"
#include "sky/util/SkyColor.glsl"

/*
 * Distance fog measured against the edge of the visible world. The world is
 * flat, so its edge is the macro terrain's reach, a fixed horizontal distance
 * from the player in every direction; a fragment's fog fraction is its true
 * horizontal distance over that reach. Fog rises along that fraction from
 * clear at the player to total at the rim, so the last macro tiles always
 * dissolve before they end.
 *
 * Chunks, megas and macros meet where the chunk grid stops drawing, near its
 * render radius. A seam haze rises over the band of chunks leading up to that
 * radius and holds beyond it, so the change from voxel ground to coarse macro
 * ground is already veiled on both sides, and the seam blend also eases the
 * lighting's fog re-weighting to neutral there, so a voxel face and a macro
 * facet at the same distance always take exactly the same fog. Fog depends
 * only on horizontal distance, never on which renderer drew the fragment.
 *
 * Fog color follows the fog fraction from the weather pipeline's fog tint
 * (u_skyFogColor) near the player to the exact sky color along the view
 * direction at the rim, glow on the sun side, anti-solar belt opposite, so
 * the world's edge dissolves seamlessly into the sky above it.
 */

// Must match EngineSetting.MACRO_RENDER_DISTANCE_BLOCKS.
const float FOG_WORLD_EDGE_BLOCKS = 8192.0;

const float FOG_EPSILON           = 0.0001;
const float FOG_DISTANCE_EXPONENT = 1.5;
const float FOG_EDGE_START        = 0.75;
const float FOG_SEAM_HAZE         = 0.4;
const float FOG_SEAM_BAND_CHUNKS  = 12.0;
const float FOG_SEAM_INSET_CHUNKS = 1.0;

// True horizontal distance from the player, the origin the chunk grid and the macro ring are both laid around.
float computeFogDistance(vec3 worldPos) {
    return length(worldPos.xz - u_playerPosition.xz);
}

// Distance over the reach of the visible world, 0 to 1.
float computeFogFraction(float fogDistance) {
    return clamp(fogDistance / FOG_WORLD_EDGE_BLOCKS, 0.0, 1.0);
}

// 0 well inside the chunk grid, rising to 1 where its last drawn ring meets the macro terrain.
float computeFogSeamBlend(float fogDistance) {
    float seamBlocks = (u_renderDistance * 0.5 - FOG_SEAM_INSET_CHUNKS) * u_chunkSize;
    float bandBlocks = FOG_SEAM_BAND_CHUNKS * u_chunkSize;

    return smoothstep(seamBlocks - bandBlocks, seamBlocks, fogDistance);
}

float computeFogAmount(float fogFraction, float seamBlend) {
    return max(pow(fogFraction, FOG_DISTANCE_EXPONENT), FOG_SEAM_HAZE * seamBlend);
}

// Eases a light-dependent fog weight to neutral across the seam, so both sides of it fog identically.
float resolveFogWeight(float lightWeight, float seamBlend) {
    return mix(lightWeight, 1.0, seamBlend);
}

// Pulls an already light-weighted fog blend to total fog at the rim of the visible world.
float resolveFogEdge(float fogBlend, float fogFraction) {
    return mix(clamp(fogBlend, 0.0, 1.0), 1.0, smoothstep(FOG_EDGE_START, 1.0, fogFraction));
}

vec3 resolveFogColor(vec3 viewDir, float fogFraction) {
    vec3 skyDir = normalize(vec3(viewDir.x, max(viewDir.y, 0.0), viewDir.z) + vec3(0.0, FOG_EPSILON, 0.0));
    vec3 sky    = resolveSkyColor(skyDir, normalize(u_sunDirection), 0.0);

    return mix(u_skyFogColor, sky, fogFraction);
}

#endif
