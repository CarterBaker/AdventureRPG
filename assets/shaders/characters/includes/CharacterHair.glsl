#ifndef CHARACTER_HAIR_GLSL
#define CHARACTER_HAIR_GLSL

#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "characters/includes/CharacterZone.glsl"

// Hair is cards of strands, each running from its root at the scalp (raw v = 0) to its tip (raw v = 1). Long hair
// also rides spring bones the rig simulates, which swing whole locks as the head and body move; short hair rides the
// head whole. On top of either the wind moves every strand here, from the root out, after skinning and in the world,
// so it adds to whatever the bones do: the root never moves and the tip travels furthest. The wind swells and eases
// in slow gusts; each lock waves with its own phase, read smoothly off where it grows in the bind pose, so neither
// the bones swinging it nor the body walking quickens its rhythm, and neighbours drift apart instead of swinging as
// one sheet; the tip trails its root, so a lock whips rather than tilts; most of the travel is downwind, a fine
// flutter runs across it, a gust lifts the tips a little, and still air keeps a slow breath of movement.

const float CHARACTER_HAIR_SWAY_BLOCKS    = 0.035;
const float CHARACTER_HAIR_SWAY_WIND      = 0.12;
const float CHARACTER_HAIR_SWAY_CALM      = 0.12;
const float CHARACTER_HAIR_ROOT_POWER     = 1.7;
const float CHARACTER_HAIR_GUST_SPEED     = 0.37;
const float CHARACTER_HAIR_GUST_SWELL     = 0.91;
const float CHARACTER_HAIR_GUST_FLOOR     = 0.55;
const float CHARACTER_HAIR_WAVE_SPEED     = 2.2;
const float CHARACTER_HAIR_WAVE_LAG       = 1.6;
const float CHARACTER_HAIR_SWAY_DOWNWIND  = 0.65;
const float CHARACTER_HAIR_FLUTTER_SPEED  = 6.3;
const float CHARACTER_HAIR_FLUTTER_SHARE  = 0.22;
const float CHARACTER_HAIR_LIFT_SHARE     = 0.25;
const vec3  CHARACTER_HAIR_LOCK_SPREAD    = vec3(7.0, 3.1, 5.3);
const vec3  CHARACTER_HAIR_LOCK_DETAIL    = vec3(11.0, -7.0, 13.0);
const float CHARACTER_HAIR_FLUTTER_SPREAD = 1.7;

// How far the wind carries a hair vertex this moment, its lock known by where it grows; nothing for any other zone
vec3 resolveHairSway(int zone, float strandV, vec3 bindPosition) {
    if (zone != CHARACTER_ZONE_HAIR)
    return vec3(0.0);

    float along    = clamp(strandV, 0.0, 1.0);
    float reach    = pow(along, CHARACTER_HAIR_ROOT_POWER);
    float strength = clamp(u_windSpeed * CHARACTER_HAIR_SWAY_WIND, CHARACTER_HAIR_SWAY_CALM, 1.0);
    vec3  heading  = vec3(u_windDirection.x, 0.0, u_windDirection.z);

    if (dot(heading, heading) < 0.0001)
    heading = vec3(1.0, 0.0, 0.0);

    heading = normalize(heading);

    vec3  across  = vec3(-heading.z, 0.0, heading.x);
    float lock    = dot(bindPosition, CHARACTER_HAIR_LOCK_SPREAD)
                  + sin(dot(bindPosition, CHARACTER_HAIR_LOCK_DETAIL));
    float gust    = mix(CHARACTER_HAIR_GUST_FLOOR, 1.0,
                        0.5 + 0.5 * sin(u_time * CHARACTER_HAIR_GUST_SPEED + lock * 0.2)
                        * sin(u_time * CHARACTER_HAIR_GUST_SWELL + 1.3));
    float wave    = sin(u_time * CHARACTER_HAIR_WAVE_SPEED + lock - along * CHARACTER_HAIR_WAVE_LAG);
    float push    = CHARACTER_HAIR_SWAY_DOWNWIND + (1.0 - CHARACTER_HAIR_SWAY_DOWNWIND) * wave;
    float flutter = sin(u_time * CHARACTER_HAIR_FLUTTER_SPEED + lock * CHARACTER_HAIR_FLUTTER_SPREAD
                        - along * CHARACTER_HAIR_WAVE_LAG);

    vec3 sway = heading * push
              + across * flutter * CHARACTER_HAIR_FLUTTER_SHARE
              + vec3(0.0, 1.0, 0.0) * max(push, 0.0) * gust * CHARACTER_HAIR_LIFT_SHARE;

    return sway * CHARACTER_HAIR_SWAY_BLOCKS * strength * gust * reach;
}

#endif
