#ifndef TILED_SAMPLING_GLSL
#define TILED_SAMPLING_GLSL

vec2 tileUV(vec3 localPos, vec2 uvOrigin, vec3 normal, float encodedFaceF) {

    vec2 planar;
    if      (abs(normal.y) > 0.5) planar = localPos.xz; // UP/DOWN
    else if (abs(normal.x) > 0.5) planar = localPos.zy; // EAST/WEST
    else                          planar = localPos.xy;  // NORTH/SOUTH

    vec2 faceData = u_faceOrientations[int(encodedFaceF)];
    int spin = int(faceData.y);

    vec2 c = fract(planar) - 0.5;

    vec2 rotated;
    if      (spin == 1) rotated = vec2(-c.y,  c.x);
    else if (spin == 2) rotated = vec2(-c.x, -c.y);
    else if (spin == 3) rotated = vec2( c.y, -c.x);
    else                rotated = c;

    return uvOrigin + (rotated + 0.5) * u_uvPerBlock;
}

#endif