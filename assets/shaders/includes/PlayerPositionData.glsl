#ifndef PLAYER_POSITION_DATA_GLSL
#define PLAYER_POSITION_DATA_GLSL
layout(std140) uniform PlayerPositionData {
    vec3  u_playerPosition;
    int   u_playerChunkX;
    int   u_playerChunkZ;
};
#endif