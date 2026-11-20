#ifndef CHARACTER_CLOTH_GLSL
#define CHARACTER_CLOTH_GLSL

#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "characters/includes/CharacterZone.glsl"

// Cloth hung from spring bones (a cape, a cloak's skirts) flutters in the wind on top of the swing the bones give it:
// each vertex moves as far as the spring bones carry it, so the cloth sewn to the body stays put and the free hem
// ripples most. Ripples run down the cloth from where it hangs, a phase read smoothly off where each vertex lies in
// the bind pose, so the bones swinging it never quicken them; most of the travel is across the wind and toward the
// body's back, with a little downwind, swelling in slow gusts, and still air keeps a faint stir. Hair flutters by its
// own strands instead (CharacterHair.glsl).

const float CHARACTER_CLOTH_FLUTTER_BLOCKS = 0.022;
const float CHARACTER_CLOTH_FLUTTER_WIND   = 0.12;
const float CHARACTER_CLOTH_FLUTTER_CALM   = 0.08;
const float CHARACTER_CLOTH_RIPPLE_SPEED   = 5.1;
const float CHARACTER_CLOTH_RIPPLE_LENGTH  = 9.0;
const float CHARACTER_CLOTH_RIPPLE_SPREAD  = 6.5;
const float CHARACTER_CLOTH_DETAIL_SPEED   = 8.3;
const float CHARACTER_CLOTH_DETAIL_SHARE   = 0.35;
const float CHARACTER_CLOTH_DOWNWIND_SHARE = 0.3;
const float CHARACTER_CLOTH_GUST_SPEED     = 0.37;
const float CHARACTER_CLOTH_GUST_SWELL     = 0.91;
const float CHARACTER_CLOTH_GUST_FLOOR     = 0.55;

// How far the wind flutters a vertex this moment, carried by spring bones as much as given; nothing for hair
vec3 resolveClothFlutter(int zone, float springWeight, vec3 bindPosition) {
    if (zone == CHARACTER_ZONE_HAIR || springWeight <= 0.0)
    return vec3(0.0);

    float strength = clamp(u_windSpeed * CHARACTER_CLOTH_FLUTTER_WIND, CHARACTER_CLOTH_FLUTTER_CALM, 1.0);
    vec3  heading  = vec3(u_windDirection.x, 0.0, u_windDirection.z);

    if (dot(heading, heading) < 0.0001)
    heading = vec3(1.0, 0.0, 0.0);

    heading = normalize(heading);

    vec3  across = vec3(-heading.z, 0.0, heading.x);
    float phase  = -bindPosition.y * CHARACTER_CLOTH_RIPPLE_LENGTH + bindPosition.x * CHARACTER_CLOTH_RIPPLE_SPREAD;
    float gust   = mix(CHARACTER_CLOTH_GUST_FLOOR, 1.0,
                       0.5 + 0.5 * sin(u_time * CHARACTER_CLOTH_GUST_SPEED) * sin(u_time * CHARACTER_CLOTH_GUST_SWELL + 1.3));
    float ripple = sin(u_time * CHARACTER_CLOTH_RIPPLE_SPEED + phase)
                 + sin(u_time * CHARACTER_CLOTH_DETAIL_SPEED + phase * 1.7) * CHARACTER_CLOTH_DETAIL_SHARE;

    vec3 flutter = across * ripple + heading * (CHARACTER_CLOTH_DOWNWIND_SHARE * (1.0 + ripple));

    return flutter * CHARACTER_CLOTH_FLUTTER_BLOCKS * strength * gust * springWeight;
}

#endif
