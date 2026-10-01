#version 330 core

layout(location = 0)  in vec3 a_position;
layout(location = 1)  in vec2 a_uv;
layout(location = 2)  in vec4 a_boneIndices;
layout(location = 3)  in vec4 a_boneWeights;
layout(location = 4)  in mat4 a_instanceModel;
layout(location = 8)  in vec4 a_instanceTint;        // rgb = part tint, a = hidden bone (-1 = none)
layout(location = 9)  in vec4 a_instanceDetailTint;  // rgb = brow tint
layout(location = 10) in vec4 a_instanceFaceRegion;  // atlas rect of the head's face tile
layout(location = 11) in vec4 a_instanceEyesRegion;
layout(location = 12) in vec4 a_instanceBrowsRegion;
layout(location = 13) in vec4 a_instanceMouthRegion;

#include "includes/CameraData.glsl"

uniform sampler2D u_bonePalette;

out vec2 v_uv;
out vec3 v_viewPosition;
flat out vec3 v_tint;
flat out vec3 v_detailTint;
flat out vec4 v_faceRegion;
flat out vec4 v_eyesRegion;
flat out vec4 v_browsRegion;
flat out vec4 v_mouthRegion;

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

bool isHiddenVertex() {
    if (a_instanceTint.a < 0.0)
    return false;

    int hidden = int(a_instanceTint.a + 0.5);

    if (a_boneWeights.x > 0.5 && int(a_boneIndices.x) == hidden) return true;
    if (a_boneWeights.y > 0.5 && int(a_boneIndices.y) == hidden) return true;
    if (a_boneWeights.z > 0.5 && int(a_boneIndices.z) == hidden) return true;
    if (a_boneWeights.w > 0.5 && int(a_boneIndices.w) == hidden) return true;

    return false;
}

void main() {
    mat4 skin =
    a_boneWeights.x * fetchBoneMatrix(int(a_boneIndices.x)) +
    a_boneWeights.y * fetchBoneMatrix(int(a_boneIndices.y)) +
    a_boneWeights.z * fetchBoneMatrix(int(a_boneIndices.z)) +
    a_boneWeights.w * fetchBoneMatrix(int(a_boneIndices.w));

    vec4 skinnedPosition = skin * vec4(a_position, 1.0);
    vec4 worldPosition   = a_instanceModel * skinnedPosition;

    v_uv           = a_uv;
    v_viewPosition = (u_view * worldPosition).xyz;
    v_tint         = a_instanceTint.rgb;
    v_detailTint   = a_instanceDetailTint.rgb;
    v_faceRegion   = a_instanceFaceRegion;
    v_eyesRegion   = a_instanceEyesRegion;
    v_browsRegion  = a_instanceBrowsRegion;
    v_mouthRegion  = a_instanceMouthRegion;

    if (isHiddenVertex())
    gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
    else
    gl_Position = u_viewProjection * worldPosition;
}
