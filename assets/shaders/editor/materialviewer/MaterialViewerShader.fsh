#version 330 core

in vec3 vNormalView;
in vec3 vViewPosition;
in vec2 vUV;

#include "characters/includes/CharacterMaterial.glsl"
#include "editor/treeviewer/TreeViewerLight.glsl"

// The tile each map is read from: albedo, normal, ambient occlusion and metallic, then roughness and tint
uniform ivec4 u_mapTiles;
uniform ivec2 u_mapTilesExtra;

// The material's parameters: its base colour, then its tiling, normal strength, roughness offset and metallic offset
uniform vec3 u_baseColor;
uniform vec4 u_surface;

out vec4 FragColor;

const float MATERIAL_VIEWER_CUTOUT           = 0.5;
const float MATERIAL_VIEWER_SHININESS_SMOOTH = 256.0;
const float MATERIAL_VIEWER_SHININESS_ROUGH  = 4.0;
const vec3  MATERIAL_VIEWER_DIELECTRIC       = vec3(0.04);

/*
 * A material on the Material Viewer's shape: each map read from the tile the material takes it from, at the shape's
 * UVs repeated by the tiling and shaped by the parameters as the Paint tab's Material tool lays them, the normal map
 * bent through the cotangent frame the character shaders build, then lit by the viewer's one light over its ambient
 * floor with a highlight roughness widens and metal colours.
 */
void main() {
    vec2 uv = vUV * u_surface.x;
    CharacterFrame frame = resolveCharacterFrame(normalize(vNormalView), vViewPosition, uv);

    vec4  albedo    = sampleTileAlbedo(resolveTileReadGrad(u_mapTiles.x, frame.uvDx, frame.uvDy), uv, frame.uvDx,
                                       frame.uvDy);
    vec3  mapNormal = sampleTileNormal(resolveTileReadGrad(u_mapTiles.y, frame.uvDx, frame.uvDy), uv, frame.uvDx,
                                       frame.uvDy);
    float ao        = sampleTileMaterial(resolveTileReadGrad(u_mapTiles.z, frame.uvDx, frame.uvDy), uv, frame.uvDx,
                                         frame.uvDy).r;
    float metallic  = sampleTileMaterial(resolveTileReadGrad(u_mapTiles.w, frame.uvDx, frame.uvDy), uv, frame.uvDx,
                                         frame.uvDy).b;
    float roughness = sampleTileExtra(resolveTileReadGrad(u_mapTilesExtra.x, frame.uvDx, frame.uvDy), uv, frame.uvDx,
                                      frame.uvDy).g;

    if (albedo.a < MATERIAL_VIEWER_CUTOUT)
    discard;

    vec3 base   = albedo.rgb * u_baseColor;
    vec3 bent   = normalize(vec3(mapNormal.xy * u_surface.y, mapNormal.z));
    vec3 normal = normalize(frame.basis * bent);

    metallic  = clamp(metallic + u_surface.w, 0.0, 1.0);
    roughness = clamp(roughness + u_surface.z, 0.0, 1.0);

    vec3  lightView = resolveTreeViewerLightView();
    vec3  halfway   = normalize(lightView + normalize(-vViewPosition));
    float facing    = max(dot(normal, lightView), 0.0);
    float shininess = mix(MATERIAL_VIEWER_SHININESS_SMOOTH, MATERIAL_VIEWER_SHININESS_ROUGH, roughness);
    float highlight = pow(max(dot(normal, halfway), 0.0), shininess) * (1.0 - roughness) * facing;
    vec3  specular  = mix(MATERIAL_VIEWER_DIELECTRIC, base, metallic) * highlight;
    vec3  diffuse   = base * resolveTreeViewerLight(normal, ao) * mix(1.0, u_ambient, metallic);

    FragColor = vec4(diffuse + specular, 1.0);
}
