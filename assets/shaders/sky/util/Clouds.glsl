#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

/*
* Placeholder for the sky dome's distant-weather cloud pass. The previous
 * per-lobe raymarch has been removed to make way for a single weather map
 * shared with the overhead renderer. Returns fully transparent until that
 * lands.
 */

vec4 calculateClouds(vec3 dir) {
    return vec4(0.0);
}

#endif