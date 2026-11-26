#ifndef TREE_IMPOSTOR_GLSL
#define TREE_IMPOSTOR_GLSL

// A species' impostor is its shared tree seen from TREE_IMPOSTOR_FRAMES across and down directions over the upper
// hemisphere, laid out hemi-octahedrally: a direction's x and z over the sum of its three magnitudes, turned a
// quarter onto the square's diagonals. Each frame was framed on the sphere about the trunk from its own direction,
// its right the world's up crossed with that direction and its up that direction crossed with its right, exactly as
// a quad facing that direction is laid here, so a frame's texels land on the quad's, and the three frames nearest the
// view are blended (its fade is TreeImpostorFade's, for fragments alone). The steps match EngineSetting's Tree
// Impostor group and TreeImpostorBakeBranch.

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

// Where a direction over the upper hemisphere falls on the atlas, in frames across and down, a frame's centre at its
// whole number
vec2 resolveTreeImpostorCell(vec3 direction) {
    vec3 upper  = normalize(vec3(direction.x, max(direction.y, 0.0), direction.z));
    vec2 folded = upper.xz / (abs(upper.x) + abs(upper.y) + abs(upper.z));
    vec2 cell   = vec2(folded.x + folded.y, folded.x - folded.y) * 0.5 + 0.5;

    return cell * TREE_IMPOSTOR_FRAMES - 0.5;
}

// The three frames nearest a direction and how much each is seen: the corners of the triangle of frame centres it
// falls in, weighed by how near it lies to each, so the view turns smoothly from frame to frame
void resolveTreeImpostorFrames(vec3 direction, out vec2 frameA, out vec2 frameB, out vec2 frameC, out vec3 weights) {
    vec2 cell  = resolveTreeImpostorCell(direction);
    vec2 base  = floor(cell);
    vec2 share = cell - base;
    vec2 last  = vec2(TREE_IMPOSTOR_FRAMES - 1.0);

    if (share.x + share.y < 1.0) {
        frameA  = base;
        weights = vec3(1.0 - share.x - share.y, share.x, share.y);
    } else {
        frameA  = base + 1.0;
        weights = vec3(share.x + share.y - 1.0, 1.0 - share.y, 1.0 - share.x);
    }

    frameB = clamp(base + vec2(1.0, 0.0), vec2(0.0), last);
    frameC = clamp(base + vec2(0.0, 1.0), vec2(0.0), last);
    frameA = clamp(frameA, vec2(0.0), last);
}

// One map of the impostor sampled through the three frames at a place on the quad, blended by their weights
vec4 sampleTreeImpostor(
    sampler2DArray atlas, vec2 quad, vec2 frameA, vec2 frameB, vec2 frameC, vec3 weights, float layer) {
    return texture(atlas, vec3((frameA + quad) / TREE_IMPOSTOR_FRAMES, layer)) * weights.x
        + texture(atlas, vec3((frameB + quad) / TREE_IMPOSTOR_FRAMES, layer)) * weights.y
        + texture(atlas, vec3((frameC + quad) / TREE_IMPOSTOR_FRAMES, layer)) * weights.z;
}

// A quad facing a direction: its right, then its up
vec3 resolveTreeImpostorRight(vec3 facing) {
    vec3 right = cross(vec3(0.0, 1.0, 0.0), facing);

    return length(right) < TREE_IMPOSTOR_MIN_ACROSS ? vec3(1.0, 0.0, 0.0) : normalize(right);
}

#endif
