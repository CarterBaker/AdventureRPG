// WeatherMapData.glsl
#ifndef WEATHER_MAP_DATA_GLSL
#define WEATHER_MAP_DATA_GLSL

// Must match EngineSetting.WEATHER_MAP_RESOLUTION, WEATHER_MAP_MAX_LAYERS,
// WEATHER_MAP_LAYERS_PER_COMPONENT, WEATHER_MAP_CHANNEL_MAX, and
// WEATHER_MAP_DENSITY_SCALE_MAX, and the counts in ubos/WeatherMapData.json —
// GLSL has no visibility into the Java constants.
#define WEATHER_MAP_RESOLUTION 24
#define WEATHER_MAP_MAX_LAYERS 8
#define WEATHER_MAP_LAYERS_PER_COMPONENT 4
#define WEATHER_MAP_CHANNEL_MAX 255.0
#define WEATHER_MAP_DENSITY_SCALE_MAX 2.5

// Source: WeatherMapBufferSystem, one instance per grid. Positions are in
// blocks relative to the grid's reference chunk corner, the same space
// u_cameraPosition and the surface shader's vLocalPos use.
//
// weatherCells:        the grid's window of the scrolling weather image,
//                      row-major (z * WEATHER_MAP_RESOLUTION + x). One byte
//                      per layer, layer n in bits (n % 4) * 8:
//                      x = coverage of layers 0-3, y = coverage of layers 4-7,
//                      z = density scale of layers 0-3, w = density scale of
//                      layers 4-7 (byte / 255 * WEATHER_MAP_DENSITY_SCALE_MAX).
// weatherLayerColor:   xyz = archetype color, w = saturation
// weatherLayerShape:   x = base altitude, y = vertical thickness,
//                      z = density, w = fullness (0 = sheet, 1 = puffy)
// weatherLayerNoise:   x = shape lattice cells across the shape period on X
//                      (elongated along the flow), y = the same on Z,
//                      z = detail lattice multiplier, w = domain warp strength
// weatherLayerSurface: xy = shape noise origin in blocks, already carried by
//                      the flow and wrapped into the shape period,
//                      z = coverage bias, w = silhouette softness
// weatherMapOrigin:    xy = offset from a position to its place on the window
//                      in blocks, z = cell size in blocks, w = dome range in
//                      blocks (where each layer's dome meets the horizon)
// weatherShapePeriod:  blocks after which every layer's shape noise repeats;
//                      divides both world axes so the sky never seams
// weatherLayerCount:   live layers, ordered by base altitude
layout(std140) uniform WeatherMapData {
    ivec4 u_weatherCells[WEATHER_MAP_RESOLUTION * WEATHER_MAP_RESOLUTION];
    vec4  u_weatherLayerColor[WEATHER_MAP_MAX_LAYERS];
    vec4  u_weatherLayerShape[WEATHER_MAP_MAX_LAYERS];
    vec4  u_weatherLayerNoise[WEATHER_MAP_MAX_LAYERS];
    vec4  u_weatherLayerSurface[WEATHER_MAP_MAX_LAYERS];
    vec4  u_weatherMapOrigin;
    float u_weatherShapePeriod;
    int   u_weatherLayerCount;
};

#endif
