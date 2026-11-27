#version 330 core

in vec2 vUV;
flat in int vTile;
in vec3 vViewPosition;

#include "items/includes/ItemSurface.glsl"
#include "includes/GBufferSurface.glsl"


layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;
layout(location = 3) out vec4 gSurface;

/*
 * A painted item on a vehicle writes the same G-buffer as the vehicle's own
 * faces, its texel's alpha screened through the same ordered dither, so glass
 * lets the world behind it show through in the same pass.
 */
const float VEHICLE_SPECULAR = 0.18;

const float VEHICLE_DITHER[16] = float[16](
    0.0, 8.0, 2.0, 10.0,
    12.0, 4.0, 14.0, 6.0,
    3.0, 11.0, 1.0, 9.0,
    15.0, 7.0, 13.0, 5.0);

float resolveDitherThreshold() {
    ivec2 cell = ivec2(gl_FragCoord.xy) & 3;
    return (VEHICLE_DITHER[cell.y * 4 + cell.x] + 0.5) / 16.0;
}

void main() {
    ItemSurface surface = sampleItemSurface(vTile, vUV, VEHICLE_SPECULAR);
    vec4        albedo  = surface.albedo;

    if (albedo.a < resolveDitherThreshold())
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, surface.specular, surface.ao, 1.0);
    gSurface  = encodeGBufferSurface(surface.metallic, surface.emissive);
}
