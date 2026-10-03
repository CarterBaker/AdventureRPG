#ifndef SKY_MOON_GLSL
#define SKY_MOON_GLSL

#include "includes/SkyColorData.glsl"
#include "includes/CelestialData.glsl"
#include "sky/util/SkyBodies.glsl"

/*
 * The moon: its picture is wrapped onto a ball and lit by the true sun, so
 * every phase — the thin crescent beside the sun, the half moon a quarter of
 * the sky away, the full moon opposite — and the tilt of its lit edge come
 * straight from where the two stand. The unlit side shows the sky behind the
 * air in front of it, faintly lit at night by light thrown back from the
 * world, so the moon hides the stars it passes. A full moon that slips into
 * the world's shadow darkens and reddens. Its size follows its orbit, and it
 * pales by day.
 */

uniform sampler2D u_moonTexture;

const float SKY_MOON_RADIUS          = 0.060;
const float SKY_MOON_PICTURE_FILL    = 0.92;
const float SKY_MOON_TERMINATOR      = 0.06;
const vec3  SKY_MOON_EARTHSHINE      = vec3(0.05, 0.06, 0.09);
const float SKY_MOON_DAY_PALENESS    = 0.55;
const float SKY_MOON_SHADOW_OUTER    = 0.022;
const float SKY_MOON_SHADOW_INNER    = 0.006;
const vec3  SKY_MOON_ECLIPSE_TINT    = vec3(0.80, 0.32, 0.18);
const float SKY_MOON_ECLIPSE_DIMMING = 0.70;
const float SKY_MOON_OVERCAST_FADE   = 0.80;

// ── Shadow ─────────────────────────────────────────────────────────────────

// How deep the moon sits in the world's shadow, which lies exactly opposite
// the sun.
float resolveMoonEclipse(vec3 moonDir, vec3 sunDir) {
    float fromAntiSun = acos(clamp(dot(moonDir, -sunDir), -1.0, 1.0));
    return 1.0 - smoothstep(SKY_MOON_SHADOW_INNER, SKY_MOON_SHADOW_OUTER, fromAntiSun);
}

// ── Moon ───────────────────────────────────────────────────────────────────

vec3 drawMoon(vec3 color, vec3 sky, vec3 dir, vec3 moonDir, vec3 sunDir, float pixelAngle) {
    float radius = SKY_MOON_RADIUS * u_moonScale;

    if (radius <= 0.0)
        return color;

    float extent = radius / SKY_MOON_PICTURE_FILL;
    vec2  plane  = resolveSkyBodyPlane(dir, moonDir);

    if (max(abs(plane.x), abs(plane.y)) > extent)
        return color;

    vec3 right;
    vec3 up;
    resolveSkyBodyBasis(moonDir, right, up);

    vec4  picture = sampleSkyBodyPicture(u_moonTexture, 0.5 + 0.5 * plane / extent, pixelAngle / (2.0 * extent));
    vec2  ball    = clamp(plane / radius, -1.0, 1.0);
    float bulge   = sqrt(max(1.0 - dot(ball, ball), 0.0));
    vec3  normal  = normalize(right * ball.x + up * ball.y - moonDir * bulge);
    float lit     = smoothstep(-SKY_MOON_TERMINATOR, SKY_MOON_TERMINATOR, dot(normal, sunDir));

    float eclipse  = resolveMoonEclipse(moonDir, sunDir);
    float daylight = clamp(u_skyBlend.z, 0.0, 1.0);

    vec3 litSide  = picture.rgb * mix(vec3(1.0), SKY_MOON_ECLIPSE_TINT, eclipse)
                  * (1.0 - eclipse * SKY_MOON_ECLIPSE_DIMMING);
    litSide       = mix(litSide, mix(sky, litSide, SKY_MOON_DAY_PALENESS), daylight);
    vec3 darkSide = sky + SKY_MOON_EARTHSHINE * picture.rgb * (1.0 - daylight);

    float visibility = resolveSkyBodyHorizon(dir) * (1.0 - u_skyBlend.w * SKY_MOON_OVERCAST_FADE);
    float cover      = clamp(picture.a * visibility, 0.0, 1.0);

    return mix(color, mix(darkSide, litSide, lit), cover);
}

#endif
