#version 330 core

layout(location = 0) in vec3 a_position;
layout(location = 1) in vec2 a_uv;
layout(location = 2) in vec4 a_boneIndices;
layout(location = 3) in vec4 a_boneWeights;
layout(location = 4) in vec3 a_normal;          // smooth normal MeshBuilder bakes, on either side of the face
layout(location = 5) in vec2 a_morph;           // first morph entry of the vertex, entry count
layout(location = 6) in vec3 a_detail;          // zone, raw corner uv
layout(location = 7) in vec4 a_masks;           // freckles, blush, lips, liner
layout(location = 8) in mat4 a_instanceModel;

#include "includes/CameraData.glsl"
#include "characters/includes/CharacterAppearance.glsl"
#include "characters/includes/CharacterHair.glsl"

uniform sampler2D u_morphTargets;
uniform int u_morphBase;

out vec4 v_uv;                                  // xy = atlas uv, zw = raw corner uv
out vec3 v_viewPosition;
out vec3 v_viewNormal;
out vec3 v_surfacePosition;                     // bind-pose position after morphs, where skin detail is fixed
out vec4 v_masks;
out float v_maskWeight;
flat out int v_zone;
flat out int v_instance;

mat4 fetchBoneMatrix(int boneIndex) {
    int baseX = boneIndex * 3;

    vec4 row0 = texelFetch(u_bonePalette, ivec2(baseX,     gl_InstanceID), 0);
    vec4 row1 = texelFetch(u_bonePalette, ivec2(baseX + 1, gl_InstanceID), 0);
    vec4 row2 = texelFetch(u_bonePalette, ivec2(baseX + 2, gl_InstanceID), 0);

    return mat4(
        row0.x, row1.x, row2.x, 0.0,
        row0.y, row1.y, row2.y, 0.0,
        row0.z, row1.z, row2.z, 0.0,
        row0.w, row1.w, row2.w, 1.0);
}

// The vertex's morph entries, each a delta scaled by its morph's weight in this instance's palette row
vec3 applyMorphs(vec3 position) {
    int start = int(a_morph.x + 0.5);
    int end   = start + int(a_morph.y + 0.5);

    if (end <= start)
    return position;

    int width = textureSize(u_morphTargets, 0).x;

    for (int i = start; i < end; i++) {
        vec4 entry   = texelFetch(u_morphTargets, ivec2(i % width, i / width), 0);
        int  morph   = int(entry.w + 0.5);
        vec4 weights = texelFetch(u_bonePalette, ivec2(u_morphBase + morph / 4, gl_InstanceID), 0);

        position += entry.xyz * weights[morph % 4];
    }

    return position;
}

// How much of this vertex the masked bones move, 0 when the draw masks nothing (CharacterHidden.glsl).
float resolveMaskWeight() {
    if (fetchMaskMode(gl_InstanceID) == CHARACTER_MASK_NONE)
    return 0.0;

    float weight = 0.0;

    if (isMaskedBone(gl_InstanceID, int(a_boneIndices.x + 0.5))) weight += a_boneWeights.x;
    if (isMaskedBone(gl_InstanceID, int(a_boneIndices.y + 0.5))) weight += a_boneWeights.y;
    if (isMaskedBone(gl_InstanceID, int(a_boneIndices.z + 0.5))) weight += a_boneWeights.z;
    if (isMaskedBone(gl_InstanceID, int(a_boneIndices.w + 0.5))) weight += a_boneWeights.w;

    return weight;
}

void main() {
    mat4 skin =
    a_boneWeights.x * fetchBoneMatrix(int(a_boneIndices.x)) +
    a_boneWeights.y * fetchBoneMatrix(int(a_boneIndices.y)) +
    a_boneWeights.z * fetchBoneMatrix(int(a_boneIndices.z)) +
    a_boneWeights.w * fetchBoneMatrix(int(a_boneIndices.w));

    vec3 morphedPosition = applyMorphs(a_position);
    vec4 worldPosition   = a_instanceModel * (skin * vec4(morphedPosition, 1.0));
    int  zone            = int(a_detail.x + 0.5);

    worldPosition.xyz += resolveHairSway(zone, a_detail.z, morphedPosition);

    v_uv              = vec4(a_uv, a_detail.yz);
    v_viewPosition    = (u_view * worldPosition).xyz;
    v_viewNormal      = mat3(u_view) * mat3(a_instanceModel) * mat3(skin) * a_normal;
    v_surfacePosition = morphedPosition;
    v_masks           = a_masks;
    v_maskWeight      = resolveMaskWeight();
    v_zone            = zone;
    v_instance        = gl_InstanceID;

    gl_Position = u_viewProjection * worldPosition;
}
