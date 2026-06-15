#version 330 core

in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/CameraData.glsl"
#include "includes/GBufferData.glsl"
#include "postprocessing/ssao/SSAOData.glsl"
#include "postprocessing/includes/ViewPosReconstruct.glsl"
#include "postprocessing/ssao/SSAOOcclusion.glsl"

void main() {
    float depth = texture(u_gDepth, v_texCoord).r;

    if (depth >= 1.0) {
        fragColor = vec4(1.0, 1.0, 1.0, 1.0);
        return;
    }

    vec3  fragPos = reconstructViewPos(v_texCoord);
    vec3  normal  = normalize(texture(u_gNormal, v_texCoord).rgb);
    float ao      = computeOcclusion(fragPos, normal, v_texCoord);

    fragColor = vec4(ao, ao, ao, 1.0);
}