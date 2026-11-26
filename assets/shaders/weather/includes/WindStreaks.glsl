#ifndef WIND_STREAKS_GLSL
#define WIND_STREAKS_GLSL

#include "includes/CameraData.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "includes/NoiseUtility.glsl"

/*
 * Cartoon wind, drawn over the world by the precipitation pass. Gusts bring white wind lines, each a short stroke
 * blown downwind that waves as it goes and curls into a swirl at its head, and leaves torn from the trees nearby
 * that tumble downwind and drift down, a few falling now and then even in calm air. Both live in cells of grids laid
 * in the world and carried along by the wind, sampled where the view ray crosses a few bands of distance, each twice
 * as far as the last with cells twice as wide, so they keep their place as the camera turns and slide past it with
 * parallax as it moves. Nothing is drawn behind the scene. Lines grow in with the wind's speed past a breeze; leaves
 * show only as far as the WindData block's u_windFoliage says trees stand near.
 */

const int   WIND_BANDS               = 3;
const float WIND_BAND_NEAR           = 4.0;
const float WIND_LINE_CELL           = 6.0;
const float WIND_LINE_SHARE          = 0.18;
const float WIND_LINE_LENGTH         = 0.42;
const float WIND_LINE_THICKNESS      = 0.012;
const float WIND_LINE_WAVE           = 0.06;
const float WIND_LINE_WAVE_RATE      = 9.0;
const float WIND_SWIRL_RADIUS        = 0.07;
const float WIND_LINE_SPEED_SHARE    = 1.2;
const float WIND_GUST_START          = 3.0;
const float WIND_GUST_FULL           = 9.0;
const float WIND_LINE_OPACITY        = 0.55;
const vec3  WIND_LINE_COLOR          = vec3(1.0);
const float WIND_LEAF_CELL           = 3.0;
const float WIND_LEAF_SHARE_CALM     = 0.06;
const float WIND_LEAF_SHARE_GUST     = 0.4;
const float WIND_LEAF_FALL           = 0.6;
const float WIND_LEAF_SIZE           = 0.05;
const float WIND_LEAF_SPIN           = 2.5;
const vec3  WIND_LEAF_GREEN          = vec3(0.38, 0.62, 0.22);
const vec3  WIND_LEAF_AMBER          = vec3(0.86, 0.55, 0.18);
const float WIND_EDGE_SLOPE          = 0.002;

float resolveWindGust() {
    return smoothstep(WIND_GUST_START, WIND_GUST_FULL, u_windSpeed);
}

vec3 resolveWindHeading() {
    vec3 heading = vec3(u_windDirection.x, 0.0, u_windDirection.z);
    float length2 = dot(heading, heading);
    return length2 > 0.0001 ? heading * inversesqrt(length2) : vec3(1.0, 0.0, 0.0);
}

// One cell's wind line at a point of the cell's own frame, its head downwind: a waving stroke curling into a swirl
float resolveWindLine(vec3 local, vec3 heading, float phase, float edge) {
    vec3  side   = vec3(-heading.z, 0.0, heading.x);
    float along  = dot(local, heading);
    float across = local.y - sin(along * WIND_LINE_WAVE_RATE + phase * 6.0) * WIND_LINE_WAVE;
    float depth  = abs(dot(local, side));
    float body   = (1.0 - smoothstep(WIND_LINE_THICKNESS - edge, WIND_LINE_THICKNESS + edge, abs(across)))
                 * (1.0 - smoothstep(WIND_LINE_THICKNESS * 4.0, WIND_LINE_THICKNESS * 8.0, depth))
                 * step(abs(along), WIND_LINE_LENGTH * 0.5)
                 * smoothstep(-WIND_LINE_LENGTH * 0.5, 0.0, along);
    vec2  head   = vec2(along - WIND_LINE_LENGTH * 0.5, local.y - WIND_SWIRL_RADIUS);
    float swirl  = 1.0 - smoothstep(WIND_LINE_THICKNESS - edge, WIND_LINE_THICKNESS + edge,
        abs(length(head) - WIND_SWIRL_RADIUS));
    swirl *= step(0.0, head.x + WIND_SWIRL_RADIUS) * (1.0 - smoothstep(WIND_LINE_THICKNESS * 4.0,
        WIND_LINE_THICKNESS * 8.0, depth));
    return max(body, swirl);
}

// One cell's leaf at a point of the cell's own frame: a pointed oval turning as it tumbles
float resolveWindLeaf(vec3 local, float phase, float time, float edge) {
    float angle = time * WIND_LEAF_SPIN + phase * 6.2831853;
    vec2  face  = mat2(cos(angle), -sin(angle), sin(angle), cos(angle)) * local.xy;
    float leaf  = length(vec2(face.x * 0.55, face.y)) + abs(face.x) * 0.25;
    return (1.0 - smoothstep(WIND_LEAF_SIZE - edge, WIND_LEAF_SIZE + edge, leaf)) * step(abs(local.z), WIND_LEAF_SIZE);
}

// The wind over one pixel, premultiplied: its lines and leaves where the view ray crosses each band before the scene
vec4 resolveWind(vec3 dir, float sceneDistance, float pixelAngle) {

    float gust    = resolveWindGust();
    float foliage = clamp(u_windFoliage, 0.0, 1.0);

    if (gust <= 0.0 && foliage <= 0.0)
    return vec4(0.0);

    float time    = u_time;
    vec3  heading = resolveWindHeading();
    float drift   = max(u_windSpeed, 1.0) * WIND_LINE_SPEED_SHARE;
    vec4  wind    = vec4(0.0);

    for (int band = 0; band < WIND_BANDS; band++) {

        float scale    = float(1 << band);
        float distance = WIND_BAND_NEAR * scale;

        if (distance > sceneDistance)
        break;

        vec3  point = u_cameraPosition + dir * distance;
        float edge  = max(pixelAngle * distance, WIND_EDGE_SLOPE * distance) / (WIND_LINE_CELL * scale);

        if (gust > 0.0) {
            vec3  carried = (point - heading * (time * drift)) / (WIND_LINE_CELL * scale);
            vec3  cell    = floor(carried);
            float roll    = hash31(cell + scale);

            if (roll < WIND_LINE_SHARE * gust) {
                float line = resolveWindLine(fract(carried) - 0.5, heading, roll, edge);
                float a    = line * WIND_LINE_OPACITY * gust;
                wind = vec4(wind.rgb + WIND_LINE_COLOR * a * (1.0 - wind.a), wind.a + a * (1.0 - wind.a));
            }
        }

        if (foliage > 0.0) {
            vec3  carried = (point - heading * (time * drift * gust) + vec3(0.0, time * WIND_LEAF_FALL, 0.0))
                          / (WIND_LEAF_CELL * scale);
            vec3  cell    = floor(carried);
            float roll    = hash31(cell + scale + 17.0);
            float share   = mix(WIND_LEAF_SHARE_CALM, WIND_LEAF_SHARE_GUST, gust) * foliage;

            if (roll < share) {
                float edgeLeaf = max(pixelAngle * distance, WIND_EDGE_SLOPE * distance) / (WIND_LEAF_CELL * scale);
                float leaf     = resolveWindLeaf(fract(carried) - 0.5, roll, time, edgeLeaf);
                vec3  color    = mix(WIND_LEAF_GREEN, WIND_LEAF_AMBER, hash31(cell + 31.0));
                wind = vec4(wind.rgb + color * leaf * (1.0 - wind.a), wind.a + leaf * (1.0 - wind.a));
            }
        }
    }

    return wind;
}

#endif
