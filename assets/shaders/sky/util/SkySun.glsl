#ifndef SKY_SUN_GLSL
#define SKY_SUN_GLSL

#include "includes/TimeData.glsl"
#include "includes/CameraData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/CelestialData.glsl"
#include "sky/util/SkyBodies.glsl"

/*
 * The sun: a round body ringed by tongues of flame. The ring is drawn twice,
 * a deeper back ring turning one way behind the bright front ring turning
 * the other, and every tongue sways and flickers on its own beat, more at
 * its tip than at its root, so the fire around the edge never sits still.
 * The flames only wake when the view turns toward the sun, fading in and
 * stretching from their roots the closer the gaze comes to its centre.
 * The body breathes very slightly. Its size follows the distance of the
 * calendar's star, it warms toward orange as it sinks, fades under
 * overcast, and sets behind the horizon line.
 */

uniform sampler2D u_sunTexture;
uniform sampler2D u_sunFlameTexture;

const float SKY_SUN_RADIUS          = 0.075;
const float SKY_SUN_PICTURE_FILL    = 0.92;
const float SKY_SUN_FLAME_EXTENT    = 1.70;
const float SKY_SUN_FLAME_ROOT      = 0.50;
const float SKY_SUN_FLAME_SHORT     = 0.15;
const float SKY_SUN_GAZE_NEAR       = 0.02;
const float SKY_SUN_GAZE_FAR        = 0.30;
const float SKY_SUN_SWAY_ANGLE      = 0.075;
const float SKY_SUN_SWAY_SPEED      = 1.6;
const float SKY_SUN_SWAY_LOBES      = 8.0;
const float SKY_SUN_SWAY_RIPPLE     = 9.0;
const float SKY_SUN_FLICKER_DEPTH   = 0.07;
const float SKY_SUN_FLICKER_SPEED   = 3.1;
const float SKY_SUN_FLICKER_LOBES   = 5.0;
const float SKY_SUN_FLICKER_DRIFT   = 1.7;
const float SKY_SUN_SPIN_SPEED      = 0.035;
const float SKY_SUN_BACK_SCALE      = 1.10;
const float SKY_SUN_BACK_OFFSET     = 0.19635;
const float SKY_SUN_BACK_BEAT       = 2.4;
const vec3  SKY_SUN_BACK_TINT       = vec3(1.0, 0.62, 0.32);
const float SKY_SUN_BREATH_DEPTH    = 0.015;
const float SKY_SUN_BREATH_SPEED    = 0.9;
const vec3  SKY_SUN_LOW_TINT        = vec3(1.0, 0.68, 0.46);
const float SKY_SUN_LOW_ELEVATION   = 0.35;
const float SKY_SUN_OVERCAST_FADE   = 0.75;

// ── Flames ─────────────────────────────────────────────────────────────────

// One ring of flame at a point on the flame picture's plane, measured in the
// picture's half size. Each tongue is bent sideways and stretched on its own
// beat before the picture is read, the bend growing from root to tip. The
// tongues are stretched out from their roots by stretch, a fraction of full.
vec4 sampleSunFlameRing(vec2 point, float spin, float beat, float stretch, float pixelSpan) {
    float radius = SKY_SUN_FLAME_ROOT + (length(point) - SKY_SUN_FLAME_ROOT) / stretch;
    float angle  = atan(point.y, point.x);
    float reach  = smoothstep(SKY_SUN_FLAME_ROOT, 1.0, radius);

    float sway    = sin(u_time * SKY_SUN_SWAY_SPEED + beat
                  + angle * SKY_SUN_SWAY_LOBES + radius * SKY_SUN_SWAY_RIPPLE);
    float flicker = 0.5 + 0.5 * sin(u_time * SKY_SUN_FLICKER_SPEED + beat * SKY_SUN_FLICKER_DRIFT
                  + angle * SKY_SUN_FLICKER_LOBES);

    angle  += spin + SKY_SUN_SWAY_ANGLE * reach * sway;
    radius *= 1.0 + SKY_SUN_FLICKER_DEPTH * reach * flicker;

    vec2 uv = 0.5 + 0.5 * radius * vec2(cos(angle), sin(angle));

    return sampleSkyBodyPicture(u_sunFlameTexture, uv, pixelSpan / stretch);
}

// How directly the view looks at the sun: one when the centre of the screen
// sits on it, falling to zero as the gaze turns away.
float resolveSunGaze(vec3 sunDir) {
    vec4 centerPoint = u_inverseProjection * vec4(0.0, 0.0, 1.0, 1.0);
    vec3 viewForward = normalize(mat3(u_inverseView) * (centerPoint.xyz / centerPoint.w));
    float gazeAngle  = acos(clamp(dot(viewForward, sunDir), -1.0, 1.0));

    return 1.0 - smoothstep(SKY_SUN_GAZE_NEAR, SKY_SUN_GAZE_FAR, gazeAngle);
}

// ── Sun ────────────────────────────────────────────────────────────────────

vec3 drawSun(vec3 color, vec3 dir, vec3 sunDir, float pixelAngle) {
    float bodyExtent  = SKY_SUN_RADIUS * u_sunScale / SKY_SUN_PICTURE_FILL;
    float flameExtent = bodyExtent * SKY_SUN_FLAME_EXTENT;
    vec2  plane       = resolveSkyBodyPlane(dir, sunDir);

    if (max(abs(plane.x), abs(plane.y)) > flameExtent * SKY_SUN_BACK_SCALE)
        return color;

    float visibility = resolveSkyBodyHorizon(dir) * (1.0 - u_skyBlend.w * SKY_SUN_OVERCAST_FADE);
    float low        = 1.0 - smoothstep(0.0, SKY_SUN_LOW_ELEVATION, sunDir.y);
    vec3  tint       = mix(vec3(1.0), SKY_SUN_LOW_TINT, low);

    float gaze = resolveSunGaze(sunDir);

    if (gaze > 0.0) {
        vec2  flamePoint  = plane / flameExtent;
        float flameSpan   = pixelAngle / (2.0 * flameExtent);
        float flameLength = mix(SKY_SUN_FLAME_SHORT, 1.0, gaze);
        float spin        = u_time * SKY_SUN_SPIN_SPEED;

        vec4 backRing  = sampleSunFlameRing(
            flamePoint / SKY_SUN_BACK_SCALE, SKY_SUN_BACK_OFFSET - spin, SKY_SUN_BACK_BEAT, flameLength, flameSpan);
        vec4 frontRing = sampleSunFlameRing(flamePoint, spin, 0.0, flameLength, flameSpan);

        color = layerSkyBodyPicture(color, backRing, tint * SKY_SUN_BACK_TINT, visibility * gaze);
        color = layerSkyBodyPicture(color, frontRing, tint, visibility * gaze);
    }

    float breath = 1.0 + SKY_SUN_BREATH_DEPTH * sin(u_time * SKY_SUN_BREATH_SPEED);
    float extent = bodyExtent * breath;
    vec4  body   = sampleSkyBodyPicture(u_sunTexture, 0.5 + 0.5 * plane / extent, pixelAngle / (2.0 * extent));

    return layerSkyBodyPicture(color, body, tint, visibility);
}

#endif
