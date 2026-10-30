#version 330 core

in vec2 vUVOrigin;
in vec3 vModelPosition;
in vec3 vViewPosition;
flat in int vFace;

#include "items/includes/ItemsStandard.glsl"
#include "items/includes/BlockRepeat.glsl"

uniform sampler2DArray u_textureArray;

layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMaterial;

/*
 * A vehicle's faces are merged across block boundaries, so a face carries only
 * the corner of its part's texture and the texture repeats once per block of
 * the model grid, read from the model position the same way a single-block
 * item lays its texels out on each face. Writes the same G-buffer as worn
 * gear, so the deferred lighting pass lights both identically. A texel's alpha
 * is the share of its pixels kept, screened through an ordered dither, so a
 * pane of glass lets the world behind it show through in the same pass and
 * rope's cut-outs stay open.
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
    vec2 uv     = resolveRepeatedUV(vUVOrigin, vModelPosition, vFace, u_uvPerBlock);
    vec4 albedo = texture(u_textureArray, vec3(uv, float(u_layer_albedo)));

    if (albedo.a < resolveDitherThreshold())
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    gAlbedo   = vec4(albedo.rgb, 1.0);
    gNormal   = vec4(normalView, 1.0);
    gMaterial = vec4(1.0, VEHICLE_SPECULAR, 1.0, 1.0);
}
