#version 330 core

layout (location = 0) in vec3  aPos;
layout (location = 1) in vec3  aNormal;
layout (location = 2) in vec2  aUV;

#include "includes/CameraData.glsl"
#include "includes/CloudSettingsData.glsl"

uniform float u_cloudScale;
uniform float u_cloudVerticalThickness;

uniform vec3  u_cloudInstancePosition;    // fully resolved render-space position — CPU-computed every frame, see CloudRenderSystem.updateInstance(). x/z already relative to the player's current chunk; y is world altitude.
uniform float u_cloudInstanceRandomSeed;  // stable per-cloud shape/warp seed
uniform float u_cloudInstanceDomainRotation; // stable per-cloud stretch-axis angle (radians) — see VolumetricCloudUtility.glsl's "Shape diversity fix"
uniform float u_cloudInstanceFadeAlpha;   // streaming fade — see OverheadCellStruct.fadeAlpha
uniform float u_cloudInstanceIntensity;   // live weather strength — see OverheadCellStruct.getIntensity()

out vec3  vWorldPos;
out vec3  vNormal;
out float vRandomSeed;
out float vDomainRotation;
out float vFadeAlpha;
out float vIntensity;

flat out vec3 vBoxMin;
flat out vec3 vBoxMax;

/*
* Player-chunk-relative instance position is resolved ENTIRELY on the CPU,
 * once per instance per frame — see CloudRenderSystem.updateInstance().
 * This shader does no chunk-index math of its own and no longer reads
 * PlayerPositionData at all: u_cloudInstancePosition arrives already
 * expressed in the same moving-world render frame every other vertex here
 * (and the camera itself) is already in — exactly mirroring how the
 * terrain's own u_gridPosition offset works (see
 * StandardSurfaceShader.vsh), just computed once in Java per cloud
 * instance instead of read back out of a shared UBO. This keeps the whole
 * cloud pipeline CPU-driven end to end, and removes any chance of the
 * cloud layer's own reference frame drifting out of sync with whatever
 * PlayerPositionData happens to hold on a given frame.
 *
 * CAMERA POSITION FIX: this shader must never read u_cameraPosition
 * directly for a manual world-space distance calculation against
 * instancePos. Whether u_cameraPosition happens to already sit in this
 * same recentered frame is not something to assume — LightingShader.fsh
 * avoids exactly this trap by reconstructing every view-relative vector
 * from the view/projection matrices instead of ever differencing
 * u_cameraPosition against a world position. surface/includes/Height.glsl
 * documents the correct reconstruction, used here too:
 * `(u_inverseView * vec4(0,0,0,1)).xyz` recovers the camera's own position
 * in WHATEVER frame the view matrix operates in, guaranteed to match
 * instancePos with no assumption required.
 */
void main() {
    vec3 cameraRenderPos = (u_inverseView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    vec3 instancePos = u_cloudInstancePosition;

    float distFromCamera = length(instancePos.xz - cameraRenderPos.xz);
    float distanceT = clamp(distFromCamera / u_cloudHorizonDistance, 0.0, 1.0);
    float sizeScale = mix(u_cloudMaxScale, u_cloudMinScale, distanceT);

    float transitionSpan = max(u_cloudHorizonDistance - u_cloudTransitionStart, 0.001);
    float fadeT = clamp((distFromCamera - u_cloudTransitionStart) / transitionSpan, 0.0, 1.0);
    float horizonFade = 1.0 - smoothstep(0.0, 1.0, fadeT);

    float finalScaleXZ = u_cloudScale * sizeScale;
    float finalScaleY = u_cloudVerticalThickness * sizeScale;

    vec3 scaledLocal = vec3(aPos.x * finalScaleXZ, aPos.y * finalScaleY, aPos.z * finalScaleXZ);
    vec3 worldPos = instancePos + scaledLocal;

    vBoxMin = vec3(instancePos.x - finalScaleXZ * 0.5, instancePos.y, instancePos.z - finalScaleXZ * 0.5);
    vBoxMax = vec3(instancePos.x + finalScaleXZ * 0.5, instancePos.y + finalScaleY, instancePos.z + finalScaleXZ * 0.5);

    vWorldPos = worldPos;
    vNormal = aNormal;
    vRandomSeed = u_cloudInstanceRandomSeed;
    vDomainRotation = u_cloudInstanceDomainRotation;
    vFadeAlpha = u_cloudInstanceFadeAlpha * horizonFade;
    vIntensity = u_cloudInstanceIntensity;

    gl_Position = u_viewProjection * vec4(worldPos, 1.0);
}