#ifndef SSAO_OCCLUSION_GLSL
#define SSAO_OCCLUSION_GLSL

// Requires: CameraData (u_projection, u_inverseProjection, u_viewport)
//           GBufferData (u_gDepth)
//           SSAOData
//           ViewPosReconstruct

float computeOcclusion(vec3 fragPos, vec3 normal, vec2 texCoord) {
    vec2 noiseScale = u_viewport / 4.0;
    vec3 randVec    = normalize(texture(u_texNoise, texCoord * noiseScale).xyz);
    vec3 tangent    = normalize(randVec - normal * dot(randVec, normal));
    vec3 bitangent  = cross(normal, tangent);
    mat3 TBN        = mat3(tangent, bitangent, normal);

    float occlusion = 0.0;

    for (int i = 0; i < u_kernelSize; ++i) {
        vec3 samplePos = fragPos + (TBN * u_samples[i].xyz) * u_radius;

        vec4 offset = u_projection * vec4(samplePos, 1.0);
        offset.xyz /= offset.w;
        offset.xyz  = offset.xyz * 0.5 + 0.5;

        float rawDepth    = texture(u_gDepth, offset.xy).r;
        vec4  sampleClip  = vec4(offset.xy * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
        vec4  sampleView  = u_inverseProjection * sampleClip;
        float sampleViewZ = sampleView.z / sampleView.w;

        float rangeCheck = smoothstep(0.0, 1.0, u_radius / max(abs(fragPos.z - sampleViewZ), 0.0001));
        occlusion += (sampleViewZ >= samplePos.z + u_bias ? 1.0 : 0.0) * rangeCheck;
    }

    return 1.0 - (occlusion / float(u_kernelSize));
}

#endif