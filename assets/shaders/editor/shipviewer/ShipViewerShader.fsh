#version 330 core

flat in int vTile;
in vec3 vModelPosition;
in vec3 vViewPosition;
flat in int vFace;

#include "items/includes/BlockAlbedo.glsl"
#include "editor/treeviewer/TreeViewerLight.glsl"


// The model height everything above is cut away at, in model blocks
uniform float u_cutawayHeight;

out vec4 FragColor;

/*
 * A vehicle in the Ship Viewer: its texture repeated once per block of the model
 * grid and its glass and rope screened through the same ordered dither as the
 * game's vehicle pass, then lit here by the viewer's one fixed light, as an
 * editor viewport has no deferred lighting pass. Everything above the cutaway is
 * left out, so the deck shown and those under it can be seen into.
 */
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
    if (vModelPosition.y > u_cutawayHeight)
    discard;

    vec4 albedo = sampleBlockAlbedo(vTile, vModelPosition, vFace);

    if (albedo.a < resolveDitherThreshold())
    discard;

    vec3 normalView = normalize(cross(dFdx(vViewPosition), dFdy(vViewPosition)));

    FragColor = vec4(albedo.rgb * resolveTreeViewerLight(normalView, 1.0), 1.0);
}
