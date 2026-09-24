#ifndef PRECIPITATION_DATA_GLSL
#define PRECIPITATION_DATA_GLSL

// Must match EngineSetting.PRECIPITATION_MAP_SIZE and the packing in
// PrecipitationInstance: one column top per ring slot, two 16-bit heights
// per int, four ints per ivec4.
#define PRECIPITATION_MAP_SIZE 64
#define PRECIPITATION_COLUMN_VECTORS 512

layout(std140) uniform PrecipitationData {
    ivec4 u_precipitationColumns[PRECIPITATION_COLUMN_VECTORS];
    ivec4 u_precipitationWindow;  // xy window corner, zw camera frame origin — absolute block columns
    vec4  u_precipitationState;   // x intensity, y snow share, zw wind drift in blocks per second
};

#endif
