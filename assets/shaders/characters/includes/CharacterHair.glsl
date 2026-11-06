#ifndef CHARACTER_HAIR_GLSL
#define CHARACTER_HAIR_GLSL

#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "characters/includes/CharacterZone.glsl"

// Hair is cards of strands, each running from its root at the scalp (raw v = 0) to its tip (raw v = 1), and the wind
// moves it from the root out: the root never moves and the tip travels furthest. The wave's phase runs along the
// card and across the head, so a lock ripples and its neighbours drift apart rather than swing as one sheet; most of
// the travel is downwind, a little flutters across it, and still air keeps a slow breath of movement.

const float CHARACTER_HAIR_SWAY_BLOCKS   = 0.03;
const float CHARACTER_HAIR_SWAY_SPEED    = 2.2;
const float CHARACTER_HAIR_SWAY_WIND     = 0.12;
const float CHARACTER_HAIR_SWAY_CALM     = 0.12;
const float CHARACTER_HAIR_SWAY_SPREAD   = 7.0;
const float CHARACTER_HAIR_SWAY_DOWNWIND = 0.65;
const float CHARACTER_HAIR_FLUTTER_SPEED = 5.1;
const float CHARACTER_HAIR_FLUTTER_SHARE = 0.3;
const float CHARACTER_HAIR_ROOT_POWER    = 1.7;

// How far the wind carries a hair vertex this moment, nothing for any other zone
vec3 resolveHairSway(int zone, float strandV, vec3 worldPosition) {
    if (zone != CHARACTER_ZONE_HAIR)
    return vec3(0.0);

    float reach    = pow(clamp(strandV, 0.0, 1.0), CHARACTER_HAIR_ROOT_POWER);
    float strength = clamp(u_windSpeed * CHARACTER_HAIR_SWAY_WIND, CHARACTER_HAIR_SWAY_CALM, 1.0);
    vec3  heading  = vec3(u_windDirection.x, 0.0, u_windDirection.z);

    if (dot(heading, heading) < 0.0001)
    heading = vec3(1.0, 0.0, 0.0);

    heading = normalize(heading);

    vec3  across  = vec3(-heading.z, 0.0, heading.x);
    float spread  = dot(worldPosition, vec3(CHARACTER_HAIR_SWAY_SPREAD));
    float gust    = CHARACTER_HAIR_SWAY_DOWNWIND
                  + (1.0 - CHARACTER_HAIR_SWAY_DOWNWIND) * sin(u_time * CHARACTER_HAIR_SWAY_SPEED + spread);
    float flutter = sin(u_time * CHARACTER_HAIR_FLUTTER_SPEED + spread * 1.7);

    return (heading * gust + across * flutter * CHARACTER_HAIR_FLUTTER_SHARE)
         * CHARACTER_HAIR_SWAY_BLOCKS * strength * reach;
}

#endif
