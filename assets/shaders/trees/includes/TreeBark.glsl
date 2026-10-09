#ifndef TREE_BARK_GLSL
#define TREE_BARK_GLSL

#include "includes/CameraData.glsl"
#include "trees/includes/TreeShading.glsl"

// Wood is square, so its shading rounds it. Each quad knows how each of its four edges meets the wood beyond it
// (see TreeMeshUtility): over an outer edge the normal rolls toward the side it turns to, linearly with the
// distance in, which is exactly how a cylinder's normal turns across it, so a branch no wider than twice the bevel
// reads as a round limb and a wide trunk as a square one with softly rounded corners; along an inner edge, where
// a limb grows out of its parent or wood folds into a notch, the crease darkens like the occluded corner it is;
// and a flat edge, where the surface runs on into the next quad, shades nothing, so merged quads never show a
// seam. The texture repeats once per block over the merged faces exactly as on an item, read from the wood's own
// position so it stays on the wood however a falling tree turns. The vertex layout must match the one
// TreeMeshUtility writes: the face and the four edge kinds share the meta word, the tint is an exact 24-bit RGB
// triple, and the low edge slot holds the vertex's place on its quad and the quad's size, both in sub-voxels.

const int   TREE_EDGE_FLAT      = 0;
const int   TREE_EDGE_CONVEX    = 1;
const int   TREE_EDGE_CONCAVE   = 2;
const int   TREE_EDGE_BITS      = 2;
const int   TREE_EDGE_MASK      = 3;
const float TREE_BEVEL_REACH    = 6.0;
const float TREE_BEVEL_TILT     = 0.9;
const float TREE_CREASE_REACH   = 3.0;
const float TREE_CREASE_SHADE   = 0.5;
const float TREE_MIN_FACING     = 0.05;
const float TREE_BARK_SPECULAR  = 0.08;
const int   TREE_META_FACE_MASK = 7;
const int   TREE_META_EDGE_SHIFT = 3;

int unpackTreeFace(float meta) {
    return int(meta) & TREE_META_FACE_MASK;
}

int unpackTreeEdges(float meta) {
    return int(meta) >> TREE_META_EDGE_SHIFT;
}


float rampTreeEdge(float distance, float reach) {
    return reach > 0.0 ? clamp(1.0 - distance / reach, 0.0, 1.0) : 0.0;
}

float shadeTreeCrease(int kind, float distance) {
    if (kind != TREE_EDGE_CONCAVE)
    return 1.0;

    return mix(TREE_CREASE_SHADE, 1.0, clamp(distance / TREE_CREASE_REACH, 0.0, 1.0));
}

// The rounded normal at a point of a quad, local in sub-voxels from its low corner, and the crease shade there
vec3 bevelTreeNormal(int face, int edges, vec2 local, vec2 size, out float crease) {
    int axis    = resolveTreeFaceAxis(face);
    vec3 normal = TREE_FACE_NORMALS[face];
    vec3 axisU  = resolveTreeAxis((axis + 1) % 3);
    vec3 axisV  = resolveTreeAxis((axis + 2) % 3);

    int lowU  = edges & TREE_EDGE_MASK;
    int highU = (edges >> TREE_EDGE_BITS) & TREE_EDGE_MASK;
    int lowV  = (edges >> (TREE_EDGE_BITS * 2)) & TREE_EDGE_MASK;
    int highV = (edges >> (TREE_EDGE_BITS * 3)) & TREE_EDGE_MASK;

    float reachU = min(TREE_BEVEL_REACH, size.x * 0.5);
    float reachV = min(TREE_BEVEL_REACH, size.y * 0.5);
    float tiltU  = 0.0;
    float tiltV  = 0.0;

    if (lowU == TREE_EDGE_CONVEX)  tiltU -= rampTreeEdge(local.x, reachU);
    if (highU == TREE_EDGE_CONVEX) tiltU += rampTreeEdge(size.x - local.x, reachU);
    if (lowV == TREE_EDGE_CONVEX)  tiltV -= rampTreeEdge(local.y, reachV);
    if (highV == TREE_EDGE_CONVEX) tiltV += rampTreeEdge(size.y - local.y, reachV);

    tiltU *= TREE_BEVEL_TILT;
    tiltV *= TREE_BEVEL_TILT;

    crease = shadeTreeCrease(lowU, local.x)
        * shadeTreeCrease(highU, size.x - local.x)
        * shadeTreeCrease(lowV, local.y)
        * shadeTreeCrease(highV, size.y - local.y);

    float facing = sqrt(max(1.0 - tiltU * tiltU - tiltV * tiltV, TREE_MIN_FACING));

    return normalize(normal * facing + axisU * tiltU + axisV * tiltV);
}

// Every G-buffer target for one bark fragment; rotation turns the wood's own axes into the world's
void shadeTreeBark(
    vec3 worldPos, vec3 modelPos, vec2 local, vec2 size, vec2 uvOrigin, vec3 color, int face, int edges,
    mat3 rotation, out vec4 albedoOut, out vec4 normalOut, out vec4 materialOut) {
    float crease;
    vec3  normal = bevelTreeNormal(face, edges, local, size, crease);
    vec2  uv     = resolveRepeatedUV(uvOrigin, modelPos, face, u_uvPerBlock);
    vec4  albedo = texture(u_textureArray, vec3(uv, float(u_layer_albedo)));

    // Blending is enabled for this pass, so every target writes alpha = 1.0.
    albedoOut   = vec4(albedo.rgb * color, 1.0);
    normalOut   = vec4(normalize(mat3(u_view) * (rotation * normal)), 1.0);
    materialOut = vec4(resolveTreeSunVisibility(worldPos), TREE_BARK_SPECULAR, crease, 1.0);
}

#endif
