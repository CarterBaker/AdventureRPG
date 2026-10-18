#ifndef POST_FOCUS_GLSL
#define POST_FOCUS_GLSL

#include "includes/CameraData.glsl"
#include "includes/GBufferNormal.glsl"
#include "includes/PostProcessData.glsl"
#include "postprocessing/includes/InverseDistance.glsl"
#include "weather/includes/CloudComposite.glsl"

/*
 * The depth of field's circle of confusion. AutoFocusSystem focuses on
 * whatever the view rests on, and a band around the focus stays sharp — a
 * small fixed depth plus a share of the focus distance — so a subject keeps
 * its depth in focus while everything off it fades fast. Beyond the band,
 * blur grows with the difference between a surface's inverse distance and
 * the band edge's, as through a real lens: focused near, the background
 * melts away; focused far, only what stands close to the eye softens. Each
 * side of the focus blurs through the share of the aperture AutoFocusSystem
 * gives the view. Behind the player the camera's subject stays sharp
 * wherever the focus lies: a fragment of character skin, flagged smooth in
 * the G-buffer, within the subject's depth is never blurred, so the player
 * stays crisp while what it looks past fades around it. The sky has no surface to
 * focus on, so it always stays sharp and its clouds, stars, sun and moon
 * are never blurred; clouds standing in front of a surface, and the fog of a
 * cloud the camera stands in, keep that surface sharp as far as they hide
 * it, so a cloud over a blurred ridge stays as crisp as one against the sky.
 * A surface's distance is the nearer of the world and the water surface
 * over it. The circle of confusion is signed: negative in front of the
 * focus, positive behind it, so the sign orders any two samples by depth.
 */

uniform sampler2D u_sceneDepth;
uniform sampler2D u_sceneNormal;
uniform sampler2D u_waterDepth;
uniform sampler2D u_cloudColor;
uniform sampler2D u_cloudDistance;

// The focus in inverse distance, and the share of the full aperture each side of it blurs with.
uniform float u_focusInverse;
uniform float u_focusNearAperture;
uniform float u_focusFarAperture;

// The subject's depth in inverse distance, nearest last; an empty range when the view has no subject.
uniform float u_subjectFarInverse;
uniform float u_subjectNearInverse;

const float DOF_APERTURE_SCALE    = 16.0;
const float DOF_FOCUS_BAND_DEPTH  = 0.35;
const float DOF_FOCUS_BAND_SHARE  = 0.15;
const float DOF_FOCUS_BAND_CLOSED = 0.000001;
const float DOF_SKY_DEPTH         = 1.0;
const float DOF_INVERSE_EPSILON   = 0.000001;

ivec2 resolveDepthTexel(vec2 uv, ivec2 offset) {
    ivec2 size = textureSize(u_sceneDepth, 0);
    return clamp(ivec2(uv * vec2(size) - 0.5) + offset, ivec2(0), size - 1);
}

// The raw depth of the nearer of the world and the water surface over it; the nearest of several is the least.
float resolveSurfaceDepth(ivec2 texel) {
    float worldDepth = texelFetch(u_sceneDepth, texel, 0).r;
    float waterDepth = texelFetch(u_waterDepth, texel, 0).r;
    return min(worldDepth, waterDepth);
}

// Signed blur from 0 in focus to 1 at the widest the depth of field reaches. The sharp band's edges are
// worked in inverse distance, so a focus at infinity needs no special case; a band reaching the eye leaves
// nothing in front of the focus to blur.
float resolveCoc(float inverseDistance) {
    float band        = DOF_FOCUS_BAND_DEPTH * u_focusInverse + DOF_FOCUS_BAND_SHARE;
    float farInverse  = u_focusInverse / (1.0 + band);
    float nearOpening = 1.0 - band;
    float scale       = u_dofStrength * DOF_APERTURE_SCALE;

    if (inverseDistance < farInverse)
    return clamp((farInverse - inverseDistance) * scale * u_focusFarAperture, 0.0, 1.0);

    if (nearOpening <= DOF_FOCUS_BAND_CLOSED)
    return 0.0;

    float nearInverse = u_focusInverse / nearOpening;

    if (inverseDistance <= nearInverse)
    return 0.0;

    return -clamp((inverseDistance - nearInverse) * scale * u_focusNearAperture, 0.0, 1.0);
}

// True for a fragment of the subject: a character's skin, unhidden by water, within the subject's depth.
bool isSubject(ivec2 texel, float inverseDistance) {
    if (inverseDistance < u_subjectFarInverse || inverseDistance > u_subjectNearInverse)
    return false;

    if (texelFetch(u_waterDepth, texel, 0).r < texelFetch(u_sceneDepth, texel, 0).r)
    return false;

    return isSmoothNormal(texelFetch(u_sceneNormal, texel, 0).rgb);
}

// The distance along this pixel's view ray to a surface at the given inverse view depth, as the weather pass
// measures its clouds.
float resolveRayDistance(vec2 uv, float inverseDistance) {
    vec2 slope = (uv * 2.0 - 1.0) / vec2(u_projection[0][0], u_projection[1][1]);
    return length(vec3(slope, 1.0)) / max(inverseDistance, DOF_INVERSE_EPSILON);
}

// The circle of confusion of the surface at a depth texel seen at uv; the sky, where nothing was drawn, stays
// sharp, and so do the subject and whatever the clouds hide.
float resolveTexelCoc(ivec2 texel, vec2 uv) {
    float surfaceDepth = resolveSurfaceDepth(texel);

    if (surfaceDepth >= DOF_SKY_DEPTH)
    return 0.0;

    float inverseDistance = resolveInverseDistance(surfaceDepth);

    if (isSubject(texel, inverseDistance))
    return 0.0;

    float coc = resolveCoc(inverseDistance);

    if (coc == 0.0)
    return 0.0;

    float cover = resolveCloudCover(u_cloudColor, u_cloudDistance, uv, resolveRayDistance(uv, inverseDistance));
    return coc * (1.0 - cover);
}

float resolveCocAt(vec2 uv) {
    return resolveTexelCoc(resolveDepthTexel(uv, ivec2(0)), uv);
}

#endif
