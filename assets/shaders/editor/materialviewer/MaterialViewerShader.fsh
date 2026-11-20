#version 330 core

in vec3 vNormalView;
in vec3 vViewPosition;
in vec2 vUV;

#include "characters/includes/CharacterMaterial.glsl"
#include "editor/treeviewer/TreeViewerLight.glsl"

// The material's maps as the brush reads them, each image's first row at its top
uniform sampler2D u_albedoMap;
uniform sampler2D u_normalMap;
uniform sampler2D u_occlusionMap;
uniform sampler2D u_metallicMap;
uniform sampler2D u_roughnessMap;

// The material's parameters: its base colour, then its tiling, normal strength, roughness offset and metallic offset
uniform vec3 u_baseColor;
uniform vec4 u_surface;

out vec4 FragColor;

const float MATERIAL_VIEWER_CUTOUT           = 0.5;
const float MATERIAL_VIEWER_SHININESS_SMOOTH = 256.0;
const float MATERIAL_VIEWER_SHININESS_ROUGH  = 4.0;
const vec3  MATERIAL_VIEWER_DIELECTRIC       = vec3(0.04);

/*
 * A material on the Material Viewer's shape: each map read from the image the Paint tab's Material tool lays it from,
 * at the shape's UVs repeated by the tiling and shaped by the parameters as that tool shapes them, the normal map
 * bent through the cotangent frame the character shaders build, then lit by the viewer's one light over its ambient
 * floor with a highlight roughness widens and metal colours.
 */
void main() {
    vec2 uv = vUV * u_surface.x;
    vec2 imageUV = vec2(uv.x, 1.0 - uv.y);
    CharacterFrame frame = resolveCharacterFrame(normalize(vNormalView), vViewPosition, uv);

    vec4  albedo    = texture(u_albedoMap, imageUV);
    vec3  mapNormal = texture(u_normalMap, imageUV).rgb * 2.0 - 1.0;
    float ao        = texture(u_occlusionMap, imageUV).r;
    float metallic  = clamp(texture(u_metallicMap, imageUV).r + u_surface.w, 0.0, 1.0);
    float roughness = clamp(texture(u_roughnessMap, imageUV).r + u_surface.z, 0.0, 1.0);

    if (albedo.a < MATERIAL_VIEWER_CUTOUT)
    discard;

    vec3 base   = albedo.rgb * u_baseColor;
    vec3 bent   = normalize(vec3(mapNormal.xy * u_surface.y, mapNormal.z));
    vec3 normal = normalize(frame.basis * bent);

    vec3  lightView = resolveTreeViewerLightView();
    vec3  halfway   = normalize(lightView + normalize(-vViewPosition));
    float facing    = max(dot(normal, lightView), 0.0);
    float shininess = mix(MATERIAL_VIEWER_SHININESS_SMOOTH, MATERIAL_VIEWER_SHININESS_ROUGH, roughness);
    float highlight = pow(max(dot(normal, halfway), 0.0), shininess) * (1.0 - roughness) * facing;
    vec3  specular  = mix(MATERIAL_VIEWER_DIELECTRIC, base, metallic) * highlight;
    vec3  diffuse   = base * resolveTreeViewerLight(normal, ao) * mix(1.0, u_ambient, metallic);

    FragColor = vec4(diffuse + specular, 1.0);
}
