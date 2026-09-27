#ifndef SSAO_OCCLUSION_GLSL
#define SSAO_OCCLUSION_GLSL

// Requires: CameraData (u_projection, u_viewport)
//           GBufferData (u_gDepth)
//           SSAOData
//           ViewPosReconstruct

// A fragment whose kernel projects smaller than SSAO_MIN_PIXEL_RADIUS pixels
// samples only its own texel, so it cannot occlude itself and is skipped;
// occlusion fades in up to SSAO_FULL_PIXEL_RADIUS so the cut never shows as
// a ring. Each sample's view depth is linearised straight from the depth
// buffer through the projection's two depth terms, the exact inverse of the
// perspective divide, instead of a full inverse projection per tap.
const float SSAO_MIN_PIXEL_RADIUS  = 1.0;
const float SSAO_FULL_PIXEL_RADIUS = 2.0;
const float SSAO_EPSILON           = 0.0001;

float linearizeViewDepth(float rawDepth) {
    return -u_projection[3][2] / ((rawDepth * 2.0 - 1.0) + u_projection[2][2]);
}

float computeOcclusion(vec3 fragPos, vec3 normal, vec2 texCoord) {
    float pixelRadius = u_radius * u_projection[1][1] * u_viewport.y * 0.5 / max(-fragPos.z, SSAO_EPSILON);

    if (pixelRadius < SSAO_MIN_PIXEL_RADIUS)
    return 1.0;

    vec2 noiseScale = u_viewport / 4.0;
    vec3 randVec    = normalize(texture(u_texNoise, texCoord * noiseScale).xyz);
    vec3 tangent    = normalize(randVec - normal * dot(randVec, normal));
    vec3 bitangent  = cross(normal, tangent);
    mat3 TBN        = mat3(tangent, bitangent, normal);

    float occlusion = 0.0;

    for (int i = 0; i < u_kernelSize; ++i) {
        vec3 samplePos = fragPos + (TBN * u_samples[i].xyz) * u_radius;

        vec4 offset = u_projection * vec4(samplePos, 1.0);
        offset.xy   = offset.xy / offset.w * 0.5 + 0.5;

        float sampleViewZ = linearizeViewDepth(texture(u_gDepth, offset.xy).r);

        float rangeCheck = smoothstep(0.0, 1.0, u_radius / max(abs(fragPos.z - sampleViewZ), SSAO_EPSILON));
        occlusion += (sampleViewZ >= samplePos.z + u_bias ? 1.0 : 0.0) * rangeCheck;
    }

    float ao = 1.0 - (occlusion / float(u_kernelSize));

    return mix(1.0, ao, smoothstep(SSAO_MIN_PIXEL_RADIUS, SSAO_FULL_PIXEL_RADIUS, pixelRadius));
}

#endif
