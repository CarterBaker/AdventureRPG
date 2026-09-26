#ifndef BEVEL_GLSL
#define BEVEL_GLSL

#include "surface/includes/SurfaceTessellationTier.glsl"

// Rounded-solid solver for natural terrain. Every displacement is a pure function of a position and the eight
// sub-blocks around its nearest sub-block lattice vertex, never of the patch evaluating it: a patch rebuilds that
// window from its own edge columns (see SubCellSampleBranch.classifyColumn), which carry every sub-block's solidity
// and naturalness exactly, and any other patch touching the same vertex rebuilds the identical window from its own,
// so two patches can only ever produce the same position for a shared vertex, whatever their sizes, resolutions,
// orientations or T-junctions. The bevel radius is half a sub-block, so everything within reach of a position lies
// inside its window. Outer edges and corners fold out and inner ones fold in exactly as a morphological opening and
// closing would round them: the nearest point of the eroded solid, and of the eroded air, is found among the
// window's lattice elements — sub-block centers and the edges, faces and cube they span wherever every sub-block
// spanning them is solid (or air) — and the position is reprojected onto the sphere of the bevel radius around it.
// A straight edge lands on O - R(1 - 1/sqrt(2))(N1 + N2) and an outer triple corner on O - R(1 - 1/sqrt(3))(N1 + N2
// + N3). Where a fold out and a fold in reach the same position each damps the other by its own activation, both
// measured from the shared window, so a mixed corner closes from every face meeting at it. Artificial blocks never
// round, never take detail and fade anything natural to flat within the bevel radius of them; natural detail also
// fades to nothing on any fold and against any other natural block's surface, so neighbouring surfaces always meet
// on the undisplaced lattice. Column codes must match SubCellSampleBranch.

const float BEVEL_RADIUS           = 0.25;
const float WINDOW_HALF_CELL       = SUB_BLOCK_SIZE * 0.5;
const float BEVEL_EDGE_ACTIVATION  = BEVEL_RADIUS * (sqrt(2.0) - 1.0);
const int   WINDOW_CELL_MASK       = 255;
const int   EDGE_BITS_PER_ENTRY    = 4;
const int   EDGE_ENTRIES_PER_WORD  = 6;
const int   EDGE_ENTRY_MASK        = 15;

// A column code is the state of the sub-block beside the edge in its low two bits and the state of the sub-block
// in front of that one in its high two.
const int COLUMN_BEHIND_EMPTY      = 0;
const int COLUMN_BEHIND_SAME       = 1;
const int COLUMN_BEHIND_NATURAL    = 2;
const int COLUMN_BEHIND_ARTIFICIAL = 3;
const int COLUMN_FRONT_EMPTY       = 0;
const int COLUMN_FRONT_NATURAL     = 1;
const int COLUMN_FRONT_ARTIFICIAL  = 2;
const int COLUMN_FRONT_SHIFT       = 2;
const int COLUMN_STATE_MASK        = 3;
const int COLUMN_EXPOSED_SAME      = COLUMN_BEHIND_SAME;

// Eight sub-blocks around a lattice vertex, one bit each, indexed by world axis: bit (z << 2) | (y << 1) | x
// where 1 is the positive side of the vertex.
struct SurfaceWindow {
    vec3 vertex;
    int  solid;
    int  behind;
    int  artificial;
    int  seam;
};

// Edge Columns \\

// Maps a sub-block index along an edge run (0 = low padding) to its entry, where an entry covers cellsPerEntry
// sub-blocks; with two per entry both padding sub-blocks land on their own padding entry.
int toEdgeEntry(int subIndex, int cellsPerEntry) {
    return (cellsPerEntry == 2) ? ((subIndex + 1) >> 1) : subIndex;
}

int readColumnCode(float lowWord, float highWord, int entry) {
    if (entry < EDGE_ENTRIES_PER_WORD)
    return (int(lowWord) >> (entry * EDGE_BITS_PER_ENTRY)) & EDGE_ENTRY_MASK;
    return (int(highWord) >> ((entry - EDGE_ENTRIES_PER_WORD) * EDGE_BITS_PER_ENTRY)) & EDGE_ENTRY_MASK;
}

// The code of the column at sub-block (columnA, columnB) of a patch; a column outside the patch is read from the
// edge it lies beyond, and a corner column from the padding of the A edge on its side.
int resolveColumnCode(
    int columnA, int columnB,
    int sizeASub, int sizeBSub, int cellsPerEntry,
    vec4 edgeLow, vec4 edgeHigh) {
    bool insideA = columnA >= 0 && columnA < sizeASub;
    bool insideB = columnB >= 0 && columnB < sizeBSub;

    if (insideA && insideB)
    return COLUMN_EXPOSED_SAME;

    if (!insideA) {
        bool lowSide = columnA < 0;
        return readColumnCode(
            lowSide ? edgeLow.x : edgeLow.y,
            lowSide ? edgeHigh.x : edgeHigh.y,
            toEdgeEntry(columnB + 1, cellsPerEntry));
    }

    bool lowSide = columnB < 0;
    return readColumnCode(
        lowSide ? edgeLow.z : edgeLow.w,
        lowSide ? edgeHigh.z : edgeHigh.w,
        toEdgeEntry(columnA + 1, cellsPerEntry));
}

// Window \\

int toWindowBit(vec3 offset) {
    return (offset.x > 0.0 ? 1 : 0) | (offset.y > 0.0 ? 2 : 0) | (offset.z > 0.0 ? 4 : 0);
}

// Builds the window around the lattice vertex nearest a position on a natural patch. The vertex is resolved from
// the world position itself, so every patch picks the same one for the same point.
SurfaceWindow buildSurfaceWindow(
    vec3 position, vec3 origin,
    vec3 tangentA, vec3 tangentB, vec3 normal,
    int sizeASub, int sizeBSub, int cellsPerEntry,
    vec4 edgeLow, vec4 edgeHigh) {
    SurfaceWindow window;
    window.vertex     = round(position / SUB_BLOCK_SIZE) * SUB_BLOCK_SIZE;
    window.solid      = 0;
    window.behind     = 0;
    window.artificial = 0;
    window.seam       = 0;

    vec3 fromOrigin = window.vertex - origin;
    int  vertexA    = int(round(dot(fromOrigin, tangentA) / SUB_BLOCK_SIZE));
    int  vertexB    = int(round(dot(fromOrigin, tangentB) / SUB_BLOCK_SIZE));

    for (int columnB = 0; columnB < 2; columnB++) {
        for (int columnA = 0; columnA < 2; columnA++) {

            int code = resolveColumnCode(
                vertexA - 1 + columnA, vertexB - 1 + columnB,
                sizeASub, sizeBSub, cellsPerEntry, edgeLow, edgeHigh);

            vec3 columnCenter = tangentA * (columnA == 0 ? -WINDOW_HALF_CELL : WINDOW_HALF_CELL)
            + tangentB * (columnB == 0 ? -WINDOW_HALF_CELL : WINDOW_HALF_CELL);

            int behindBit = 1 << toWindowBit(columnCenter - normal * WINDOW_HALF_CELL);
            int frontBit  = 1 << toWindowBit(columnCenter + normal * WINDOW_HALF_CELL);

            int behindState = code & COLUMN_STATE_MASK;
            int frontState  = (code >> COLUMN_FRONT_SHIFT) & COLUMN_STATE_MASK;

            window.behind |= behindBit;

            if (behindState != COLUMN_BEHIND_EMPTY)
            window.solid |= behindBit;

            if (behindState == COLUMN_BEHIND_ARTIFICIAL)
            window.artificial |= behindBit;

            if (behindState == COLUMN_BEHIND_NATURAL && frontState == COLUMN_FRONT_EMPTY)
            window.seam |= behindBit;

            if (frontState != COLUMN_FRONT_EMPTY)
            window.solid |= frontBit;

            if (frontState == COLUMN_FRONT_ARTIFICIAL)
            window.artificial |= frontBit;
        }
    }

    return window;
}

// Rounding \\

float elementAxis(int element, float value) {
    if (element == 0)
    return -WINDOW_HALF_CELL;
    if (element == 1)
    return WINDOW_HALF_CELL;
    return clamp(value, -WINDOW_HALF_CELL, WINDOW_HALF_CELL);
}

// Sub-blocks a lattice element spans, where per axis 0 is the negative sub-block center, 1 the positive one and 2
// the segment between them.
int elementSpan(int elementX, int elementY, int elementZ) {
    int span = 0;

    for (int bit = 0; bit < 8; bit++) {
        int bitX = bit & 1;
        int bitY = (bit >> 1) & 1;
        int bitZ = (bit >> 2) & 1;

        if ((elementX == 2 || elementX == bitX) &&
        (elementY == 2 || elementY == bitY) &&
        (elementZ == 2 || elementZ == bitZ))
        span |= 1 << bit;
    }

    return span;
}

// Nearest point to a window-relative position among the lattice elements every one of whose sub-blocks is in the
// mask — the eroded set, restricted to the window. Elements are visited in one fixed world-axis order, so ties
// resolve identically from every patch.
bool findErodedCore(int mask, vec3 relative, out vec3 core) {
    float bestDistanceSq = 1e9;
    bool  found          = false;
    core = vec3(0.0);

    for (int elementZ = 0; elementZ < 3; elementZ++) {
        for (int elementY = 0; elementY < 3; elementY++) {
            for (int elementX = 0; elementX < 3; elementX++) {

                if ((elementSpan(elementX, elementY, elementZ) & ~mask) != 0)
                continue;

                vec3 candidate = vec3(
                    elementAxis(elementX, relative.x),
                    elementAxis(elementY, relative.y),
                    elementAxis(elementZ, relative.z));

                vec3  toCandidate = relative - candidate;
                float distanceSq  = dot(toCandidate, toCandidate);

                if (distanceSq < bestDistanceSq) {
                    bestDistanceSq = distanceSq;
                    core           = candidate;
                    found          = true;
                }
            }
        }
    }

    return found;
}

// Fades to zero on the sub-blocks in the mask and to one a bevel radius away from all of them.
float fadeFromCells(int mask, vec3 relative) {
    float fade = 1.0;

    for (int bit = 0; bit < 8; bit++) {

        if ((mask & (1 << bit)) == 0)
        continue;

        vec3 low  = vec3(
            ((bit & 1) != 0) ? 0.0 : -SUB_BLOCK_SIZE,
            ((bit & 2) != 0) ? 0.0 : -SUB_BLOCK_SIZE,
            ((bit & 4) != 0) ? 0.0 : -SUB_BLOCK_SIZE);
        vec3 high = low + vec3(SUB_BLOCK_SIZE);

        float distanceToCell = length(relative - clamp(relative, low, high));
        fade = min(fade, smoothstep(0.0, BEVEL_RADIUS, distanceToCell));
    }

    return fade;
}

// Bevel offset and detail weight for a position, both pure functions of the position and its window.
void resolveSurfaceShape(SurfaceWindow window, vec3 position, out vec3 bevelOffset, out float detailWeight) {
    bevelOffset  = vec3(0.0);
    detailWeight = 1.0;

    if (window.solid == window.behind && window.artificial == 0 && window.seam == 0)
    return;

    vec3 relative = position - window.vertex;

    vec3 solidCore;
    vec3 airCore;
    bool hasSolid = findErodedCore(window.solid, relative, solidCore);
    bool hasAir   = findErodedCore(~window.solid & WINDOW_CELL_MASK, relative, airCore);

    vec3  toSolid     = relative - solidCore;
    vec3  toAir       = relative - airCore;
    float solidLength = length(toSolid);
    float airLength   = length(toAir);

    float solidActivation = hasSolid ? max(solidLength - BEVEL_RADIUS, 0.0) : 0.0;
    float airActivation   = hasAir   ? max(airLength   - BEVEL_RADIUS, 0.0) : 0.0;

    vec3 solidOffset = (solidActivation > 0.0) ? toSolid * (BEVEL_RADIUS / solidLength - 1.0) : vec3(0.0);
    vec3 airOffset   = (airActivation   > 0.0) ? toAir   * (BEVEL_RADIUS / airLength   - 1.0) : vec3(0.0);

    float solidDamp = 1.0 - clamp(airActivation   / BEVEL_EDGE_ACTIVATION, 0.0, 1.0);
    float airDamp   = 1.0 - clamp(solidActivation / BEVEL_EDGE_ACTIVATION, 0.0, 1.0);

    float naturalFade = fadeFromCells(window.artificial, relative);
    float seamFade    = fadeFromCells(window.seam, relative);
    float featureFade = 1.0 - smoothstep(0.0, BEVEL_EDGE_ACTIVATION, max(solidActivation, airActivation));

    bevelOffset  = (solidOffset * solidDamp + airOffset * airDamp) * naturalFade;
    detailWeight = naturalFade * seamFade * featureFade;
}

#endif
