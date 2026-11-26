#ifndef TREE_IMPOSTOR_GLSL
#define TREE_IMPOSTOR_GLSL

// A species' impostor is its shared tree seen from TREE_IMPOSTOR_FRAMES across and down directions over the upper
// hemisphere, laid out hemi-octahedrally: a direction's x and z over the sum of its three magnitudes, turned a
// quarter onto the square's diagonals. Each frame was framed on the sphere about the trunk from its own direction,
// its right the world's up crossed with that direction and its up that direction crossed with its right, exactly as
// a quad facing that direction is laid here, so a frame's texels land on the quad's. The steps match EngineSetting's
// Tree Impostor group and TreeImpostorBakeBranch.

const float TREE_IMPOSTOR_FRAMES     = 8.0;
const float TREE_IMPOSTOR_CUTOFF     = 0.5;
const float TREE_IMPOSTOR_SPECULAR   = 0.04;
const float TREE_IMPOSTOR_MIN_ACROSS = 0.0001;

uniform sampler2DArray u_impostorAlbedo;
uniform sampler2DArray u_impostorNormal;

// A direction turned about the vertical by an angle
vec3 turnTreeImpostor(vec3 direction, float turn) {
    float c = cos(turn);
    float s = sin(turn);

    return vec3(c * direction.x + s * direction.z, direction.y, -s * direction.x + c * direction.z);
}

// The frame a direction over the upper hemisphere falls in, counted across and down
vec2 resolveTreeImpostorFrame(vec3 direction) {
    vec3  upper  = normalize(vec3(direction.x, max(direction.y, 0.0), direction.z));
    vec2  folded = upper.xz / (abs(upper.x) + abs(upper.y) + abs(upper.z));
    vec2  cell   = vec2(folded.x + folded.y, folded.x - folded.y) * 0.5 + 0.5;

    return clamp(floor(cell * TREE_IMPOSTOR_FRAMES), 0.0, TREE_IMPOSTOR_FRAMES - 1.0);
}

// A quad facing a direction: its right, then its up
vec3 resolveTreeImpostorRight(vec3 facing) {
    vec3 right = cross(vec3(0.0, 1.0, 0.0), facing);

    return length(right) < TREE_IMPOSTOR_MIN_ACROSS ? vec3(1.0, 0.0, 0.0) : normalize(right);
}

#endif
