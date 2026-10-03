#ifndef POST_FOCUS_GLSL
#define POST_FOCUS_GLSL

#include "includes/CameraData.glsl"
#include "includes/PostProcessData.glsl"
#include "postprocessing/includes/InverseDistance.glsl"

/*
 * The depth of field's circle of confusion. A band around the focus stays
 * sharp — a fixed depth plus a share of the focus distance — so a subject
 * keeps its whole body in focus however close it stands. Beyond the band,
 * blur grows with the difference between a surface's inverse distance and
 * the band edge's, as through a real lens: focused near, the background
 * melts away quickly; focused far, only what stands close to the eye
 * softens; focused at infinity, the focus's inverse distance is zero and the
 * sky stays sharp. A surface's distance is the nearer of the world and the
 * water surface over it. The circle of confusion is signed: negative in
 * front of the focus, positive behind it, so the sign orders any two samples
 * by depth.
 */

uniform sampler2D u_sceneDepth;
uniform sampler2D u_waterDepth;
uniform float     u_focusInverse;

const float DOF_APERTURE_SCALE    = 16.0;
const float DOF_FOCUS_BAND_DEPTH  = 0.75;
const float DOF_FOCUS_BAND_SHARE  = 0.15;
const float DOF_FOCUS_BAND_CLOSED = 0.000001;

ivec2 resolveDepthTexel(vec2 uv, ivec2 offset) {
    ivec2 size = textureSize(u_sceneDepth, 0);
    return clamp(ivec2(uv * vec2(size) - 0.5) + offset, ivec2(0), size - 1);
}

float resolveSurfaceInverseDistance(ivec2 texel) {
    float worldDepth = texelFetch(u_sceneDepth, texel, 0).r;
    float waterDepth = texelFetch(u_waterDepth, texel, 0).r;
    return resolveInverseDistance(min(worldDepth, waterDepth));
}

// Signed blur from 0 in focus to 1 at the widest the depth of field reaches. The sharp band's edges are
// worked in inverse distance, so a focus at infinity needs no special case; a band reaching the eye
// leaves nothing in front of the focus to blur.
float resolveCoc(float inverseDistance) {
    float band        = DOF_FOCUS_BAND_DEPTH * u_focusInverse + DOF_FOCUS_BAND_SHARE;
    float farInverse  = u_focusInverse / (1.0 + band);
    float nearOpening = 1.0 - band;
    float diopters    = 0.0;

    if (inverseDistance < farInverse)
    diopters = inverseDistance - farInverse;

    else if (nearOpening > DOF_FOCUS_BAND_CLOSED && inverseDistance > u_focusInverse / nearOpening)
    diopters = inverseDistance - u_focusInverse / nearOpening;

    float blur = clamp(abs(diopters) * u_dofStrength * DOF_APERTURE_SCALE, 0.0, 1.0);
    return diopters > 0.0 ? -blur : blur;
}

float resolveCocAt(vec2 uv) {
    return resolveCoc(resolveSurfaceInverseDistance(resolveDepthTexel(uv, ivec2(0))));
}

#endif
