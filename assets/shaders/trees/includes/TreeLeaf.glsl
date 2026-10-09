#ifndef TREE_LEAF_GLSL
#define TREE_LEAF_GLSL

#include "includes/CameraData.glsl"
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/WindData.glsl"
#include "trees/includes/TreeShading.glsl"

// A leaf cluster is drawn as the box around its ellipsoid, never turned toward the camera: each fragment of the box
// casts the view ray into the clump and draws the clump's own surface, writing its true depth, so clusters sink into
// each other and into the wood exactly where they meet. The surface is lumped by a noise of the direction from the
// centre, rolled from the cluster's seed, and where the leaf texture leaves a gap the ray passes into a smaller,
// shaded shell beneath, so the clump reads as layered foliage rather than a ball. A camera inside a clump sees
// through it, the way one walks through leaves. The whole cluster sways with the wind as one piece, so a box never
// tears. Both sides of the box trace the same clump, so the box needs no facing. The vertex layout must match
// the one TreeMeshUtility writes: the low edge slot carries the way from the vertex to the centre and the
// horizontal radius, the high edge slot the vertical radius and the seed.

const float TREE_LEAF_LUMP_MIN       = 0.72;
const float TREE_LEAF_LUMP_FREQUENCY = 2.6;
const float TREE_LEAF_SEED_SPREAD    = 37.0;
const float TREE_LEAF_INNER_SHELL    = 0.82;
const float TREE_LEAF_INNER_SHADE    = 0.55;
const float TREE_LEAF_GAP_ALPHA      = 0.5;
const float TREE_LEAF_UNDERSIDE_AO   = 0.55;
const float TREE_LEAF_SPECULAR       = 0.04;
const float TREE_LEAF_NORMAL_NOISE   = 0.35;
const float TREE_LEAF_SWAY_BLOCKS    = 0.05;
const float TREE_LEAF_SWAY_SPEED     = 1.7;
const float TREE_LEAF_SWAY_WIND      = 0.15;
const float TREE_LEAF_SWAY_CALM      = 0.2;
const float TREE_LEAF_SWAY_SPREAD    = 0.3;
const float TREE_LEAF_GRAIN_FREQUENCY = 4.0;
const vec3  TREE_LEAF_GRAIN_OFFSET   = vec3(0.0, 17.0, 31.0);
const float TREE_LEAF_TWO_PI         = 6.28318530718;

// How far the wind carries a cluster this moment, the same for every vertex of it
vec3 resolveTreeLeafSway(vec3 center, float seed) {
    float strength = clamp(u_windSpeed * TREE_LEAF_SWAY_WIND, TREE_LEAF_SWAY_CALM, 1.0);
    float phase    = u_time * TREE_LEAF_SWAY_SPEED + seed * TREE_LEAF_TWO_PI
                   + (center.x + center.z) * TREE_LEAF_SWAY_SPREAD;
    vec3  heading  = vec3(u_windDirection.x, 0.0, u_windDirection.z);

    if (dot(heading, heading) < 0.0001)
    heading = vec3(1.0, 0.0, 0.0);

    return normalize(heading) * sin(phase) * TREE_LEAF_SWAY_BLOCKS * strength;
}

// The nearer crossing of a ray with a sphere of a radius about the origin, false when it misses or starts inside
bool intersectTreeLeafShell(vec3 origin, vec3 direction, float radius, out float distance) {
    float a = dot(direction, direction);
    float b = dot(origin, direction);
    float c = dot(origin, origin) - radius * radius;
    float discriminant = b * b - a * c;

    distance = 0.0;

    if (c <= 0.0 || discriminant < 0.0)
    return false;

    distance = (-b - sqrt(discriminant)) / a;

    return distance > 0.0;
}

float resolveTreeLeafLump(vec3 direction, float seed) {
    float noise = smoothNoise3D(direction * TREE_LEAF_LUMP_FREQUENCY + vec3(seed * TREE_LEAF_SEED_SPREAD));
    return mix(TREE_LEAF_LUMP_MIN, 1.0, noise);
}

// Every G-buffer target and the depth for one fragment of a cluster's box — false when the ray misses the clump;
// rotation turns the cluster's own axes into the world's, and modelCenter is its centre in its own frame
bool shadeTreeLeaf(
    vec3 boxPos, vec3 center, vec3 modelCenter, vec2 radii, float seed, vec2 uvOrigin, vec3 color, mat3 rotation,
    out vec4 albedoOut, out vec4 normalOut, out vec4 materialOut, out float depthOut) {
    vec3  camera    = u_inverseView[3].xyz;
    vec3  scale     = vec3(radii.x, radii.y, radii.x);
    vec3  origin    = (camera - center) / scale;
    vec3  direction = (boxPos - camera) / scale;
    float distance;

    albedoOut   = vec4(0.0);
    normalOut   = vec4(0.0);
    materialOut = vec4(0.0);
    depthOut    = 1.0;

    if (!intersectTreeLeafShell(origin, direction, 1.0, distance))
    return false;

    float lump = resolveTreeLeafLump(normalize(origin + direction * distance), seed);

    if (!intersectTreeLeafShell(origin, direction, lump, distance))
    return false;

    vec3  local    = origin + direction * distance;
    vec3  hit      = center + local * scale;
    vec3  modelHit = modelCenter + transpose(rotation) * (hit - center);
    vec3  normal   = normalize(local / scale);
    int   face     = resolveTreeFace(transpose(rotation) * normal);
    vec4  albedo   = texture(u_textureArray, vec3(resolveRepeatedUV(uvOrigin, modelHit, face, u_uvPerBlock),
        float(u_layer_albedo)));
    float shade    = 1.0;

    if (albedo.a < TREE_LEAF_GAP_ALPHA) {

        if (!intersectTreeLeafShell(origin, direction, lump * TREE_LEAF_INNER_SHELL, distance))
        return false;

        local    = origin + direction * distance;
        hit      = center + local * scale;
        modelHit = modelCenter + transpose(rotation) * (hit - center);
        normal   = normalize(local / scale);
        face     = resolveTreeFace(transpose(rotation) * normal);
        albedo   = texture(u_textureArray, vec3(resolveRepeatedUV(uvOrigin, modelHit, face, u_uvPerBlock),
            float(u_layer_albedo)));
        shade    = TREE_LEAF_INNER_SHADE;
    }

    vec3 grain = vec3(
        smoothNoise3D(modelHit * TREE_LEAF_GRAIN_FREQUENCY + TREE_LEAF_GRAIN_OFFSET.x),
        smoothNoise3D(modelHit * TREE_LEAF_GRAIN_FREQUENCY + TREE_LEAF_GRAIN_OFFSET.y),
        smoothNoise3D(modelHit * TREE_LEAF_GRAIN_FREQUENCY + TREE_LEAF_GRAIN_OFFSET.z)) - 0.5;

    normal = normalize(normal + grain * TREE_LEAF_NORMAL_NOISE);

    float ao  = shade * mix(TREE_LEAF_UNDERSIDE_AO, 1.0, normal.y * 0.5 + 0.5);
    vec4 clip = u_viewProjection * vec4(hit, 1.0);

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    albedoOut   = vec4(albedo.rgb * color, 1.0);
    normalOut   = vec4(normalize(mat3(u_view) * normal), 1.0);
    materialOut = vec4(resolveTreeSunVisibility(hit), TREE_LEAF_SPECULAR, ao, 1.0);
    depthOut    = clip.z / clip.w * 0.5 + 0.5;

    return true;
}

#endif
