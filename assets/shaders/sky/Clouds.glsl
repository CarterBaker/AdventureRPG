#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

/*
* Puff-blob cloud system — v6 (toon shading, overlap-gated).
 *
 * Puff shape (unchanged in concept)
 * ----------------------------------
 * back circle  (radius PUFF_OUTER_RADIUS) — the "ring". Mostly white with a
 *              bit of horizon sky colour mixed in — lighter than the core.
 * front circle (radius PUFF_INNER_RADIUS) — the "core". Smaller, darker.
 *              Still white blended WITH sky colour, just a smaller white
 *              mix than the ring. Never pure sky colour on its own. NEVER
 *              shaded, NEVER faded, NEVER gated — always identical
 *              regardless of position or what's behind it.
 * Fixed radii and rim width — every puff, everywhere, is identically sized.
 *
 * Blobs, not a band
 * ------------------
 * Clouds are literal rounded 3-D regions ("blobs") on a coarse grid. Each
 * blob independently rolls its own vertical centre anywhere within a wide
 * allowed dir.y range. A blob's silhouette is an ellipsoid with a per-cell
 * randomised boundary offset, so its edge is never a hard cutoff or a
 * perfect geometric outline.
 *
 * Layered compositing
 * ---------------------
 * Puffs are painted one on top of another with a real src-over-dst blend,
 * bottom-to-top, back circle then front circle per puff — NOT merged with
 * max().
 *
 * Ring shading — overlap-gated, colour-based, NOT alpha-based
 * --------------------------------------------------------------
 * The ring's ALPHA never changes — it is always fully opaque. This is
 * intentional: alpha-fading the ring reads as the cloud dissolving into
 * the sky, which is wrong for a toon look.
 *
 * Instead, only the ring's COLOUR can shift, from the bright ring colour
 * toward the core colour — a flat, opaque "shadow band", not a see-
 * through fade. And that colour shift is GATED: it is only allowed to
 * happen where this ring pixel already has another puff painted behind it
 * (tested via the accumulated alpha in `result` at the moment this ring
 * is drawn). If nothing is behind a given ring pixel yet, that pixel IS
 * part of the outer silhouette of the whole cloud at that point, and it
 * must stay the plain, unshaded ring colour — full stop, no exceptions.
 *
 * Where the gate IS open (something behind it), the shift strength still
 * follows the same two rules as always: more at THIS puff's own bottom
 * than its own top, and more the lower this puff sits within its blob.
 */

// ── Puff shape — identical for every puff, always ────────────────────────────
const float PUFF_SCALE        = 26.0;
const float PUFF_OUTER_RADIUS = 0.85;
const float PUFF_RIM_WIDTH    = 0.24;
const float PUFF_INNER_RADIUS = PUFF_OUTER_RADIUS - PUFF_RIM_WIDTH;
const float PUFF_JITTER       = 0.55;
const float PUFF_EDGE_AA      = 0.06;
const int   PUFFS_PER_CELL    = 3;

// ── Domain warp — keeps puff placement from reading as a lattice ───────────
const float WARP_FREQ = 0.09;
const float WARP_AMT  = 1.1;

// ── Blob layer — defines the cloud regions themselves ───────────────────────
const float BLOB_CELL_XZ      = 5.0;   // coarse grid spacing (horizontal only)
const float BLOB_JITTER_XZ    = 2.0;   // how far a blob drifts off its cell centre
const float BLOB_SPAWN_CHANCE = 0.60;  // probability a given cell has a cloud at all
const float BLOB_RADIUS_XZ    = 3.0;   // horizontal radius
const float BLOB_RADIUS_Y     = 1.4;   // vertical radius (flatter than wide)
const float BLOB_EDGE_NOISE   = 0.22;  // per-cell randomised boundary roughness

// Vertical placement range for blob CENTRES — this is what lets clouds
// spawn higher or lower instead of locking to one band.
const float CLOUD_DIR_Y_MIN        = 0.02;
const float CLOUD_DIR_Y_MAX        = 0.46;
const float CLOUD_DIR_Y_HARD_LIMIT = 0.62; // absolute safety bound, backstop only

// ── Ring shading behaviour (toon, overlap-gated) ────────────────────────────
const float PUFF_SHADE_MIN = 0.15;  // shade strength for puffs at the TOP of their blob
const float PUFF_SHADE_MAX = 0.95;  // shade strength for puffs at the BOTTOM of their blob

const float PUFF_BASE_ALPHA = 0.92;
const float PUFF_CORE_ALPHA = 1.0;

// Threshold on accumulated alpha to decide "is there a puff behind me".
const float PUFF_OVERLAP_EPS = 0.001;

// ── Puff colour mixing ──────────────────────────────────────────────────────
const float PUFF_BACK_WHITE_MIX  = 0.75; // ring: mostly white, lightly sky-tinted
const float PUFF_FRONT_SKY_MIX   = 0.45; // core's underlying horizon/zenith blend
const float PUFF_FRONT_WHITE_MIX = 0.55; // core: white mixed over that tint, still darker than the ring

const float CLOUD_WIND_SPEED = 0.0035;

// ── Hash ──────────────────────────────────────────────────────────────────────
vec3 _puffHash3(vec3 p) {
    p  = fract(p * vec3(127.1, 311.7, 74.7));
    p += dot(p, p.yxz + 19.19);
    return fract(vec3((p.x + p.y) * p.z,
            (p.y + p.z) * p.x,
            (p.z + p.x) * p.y));
}

// Standard src-over-dst compositing.
vec4 _cloudOver(vec4 dst, vec4 src) {
    float a  = src.a + dst.a * (1.0 - src.a);
    vec3 rgb = (src.rgb * src.a + dst.rgb * dst.a * (1.0 - src.a)) / max(a, 1.0e-5);
    return vec4(rgb, a);
}

// Finds the nearest cloud "blob" to sample point p.
void _findNearestBlob(vec3 p, float dailySeed,
    out vec3 blobCenter, out float blobEllDist, out bool blobFound) {
    vec2 cellCoord = floor(p.xz / BLOB_CELL_XZ);

    blobFound   = false;
    blobEllDist = 1.0e6;
    blobCenter  = vec3(0.0);

    for (int x = -1; x <= 1; x++)
    for (int z = -1; z <= 1; z++) {
        vec2 cell = cellCoord + vec2(x, z);

        vec3 hA = _puffHash3(vec3(cell.x, 3.17 + dailySeed, cell.y));
        if (hA.x > BLOB_SPAWN_CHANCE) continue; // no blob in this cell

        vec2 centerXZ = (cell + 0.5) * BLOB_CELL_XZ + (hA.yz - 0.5) * BLOB_JITTER_XZ;

        vec3  hB         = _puffHash3(vec3(cell.x, 58.91 + dailySeed, cell.y));
        float centerYDir = mix(CLOUD_DIR_Y_MIN, CLOUD_DIR_Y_MAX, hB.x);
        vec3  center     = vec3(centerXZ.x, centerYDir * PUFF_SCALE, centerXZ.y);

        vec3  d       = (p - center) / vec3(BLOB_RADIUS_XZ, BLOB_RADIUS_Y, BLOB_RADIUS_XZ);
        float ellDist = length(d);

        if (ellDist < blobEllDist) {
            blobEllDist = ellDist;
            blobCenter  = center;
            blobFound   = true;
        }
    }
}

// ── Public API ────────────────────────────────────────────────────────────────

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < 0.0 || dir.y > CLOUD_DIR_Y_HARD_LIMIT) return vec4(0.0);

    vec3 wind = vec3(u_time * CLOUD_WIND_SPEED + dailySeed, 0.0,
        u_time * CLOUD_WIND_SPEED * 0.7 + dailySeed * 1.3);
    vec3 p = dir * PUFF_SCALE + wind;

    vec3  blobCenter;
    float blobEllDist;
    bool  blobFound;
    _findNearestBlob(p, dailySeed, blobCenter, blobEllDist, blobFound);

    if (!blobFound || blobEllDist > 1.5) return vec4(0.0);

    float warpX = fbmNoise2D(p.xz * WARP_FREQ + dailySeed) - 0.5;
    float warpZ = fbmNoise2D(p.zx * WARP_FREQ + dailySeed + 7.3) - 0.5;
    vec3 pWarped = p + vec3(warpX, 0.0, warpZ) * WARP_AMT;

    vec3 ip = floor(pWarped);

    vec4 result = vec4(0.0);

    // y outermost so lower puffs are drawn first (background) and higher
    // puffs are drawn last (on top).
    for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++)
    for (int z = -1; z <= 1; z++) {
        vec3 cell = ip + vec3(x, y, z);

        vec3  cellPos    = cell + 0.5;
        vec3  cd         = (cellPos - blobCenter) / vec3(BLOB_RADIUS_XZ, BLOB_RADIUS_Y, BLOB_RADIUS_XZ);
        float cellEll    = length(cd);
        float edgeJitter = (_puffHash3(cell).x - 0.5) * BLOB_EDGE_NOISE;
        if (cellEll > 1.0 + edgeJitter) continue;

        float puffHeightInBlob = clamp(
            (cellPos.y - (blobCenter.y - BLOB_RADIUS_Y)) / (2.0 * BLOB_RADIUS_Y), 0.0, 1.0);

        for (int k = 0; k < PUFFS_PER_CELL; k++) {
            vec3  seed   = cell + vec3(0.0, 0.0, float(k) * 17.13 + 4.7);
            vec3  jitter = _puffHash3(seed);
            vec3  ctr    = cell + 0.5 + (jitter - 0.5) * PUFF_JITTER;
            float dist   = length(pWarped - ctr);

            float outerMask = 1.0 - smoothstep(PUFF_OUTER_RADIUS - PUFF_EDGE_AA,
                PUFF_OUTER_RADIUS + PUFF_EDGE_AA, dist);
            if (outerMask <= 0.001) continue;

            float coreMask = 1.0 - smoothstep(PUFF_INNER_RADIUS - PUFF_EDGE_AA,
                PUFF_INNER_RADIUS + PUFF_EDGE_AA, dist);

            // 0 at this puff's own bottom edge, 1 at its own top edge.
            float localV = smoothstep(-PUFF_OUTER_RADIUS, PUFF_OUTER_RADIUS, pWarped.y - ctr.y);

            vec3 frontSkyTint = mix(u_skyHorizonColor, u_skyZenithColor, PUFF_FRONT_SKY_MIX);
            vec3 backColor    = mix(u_skyHorizonColor, vec3(1.0), PUFF_BACK_WHITE_MIX);
            vec3 frontColor   = mix(frontSkyTint,       vec3(1.0), PUFF_FRONT_WHITE_MIX);

            // GATE: does this ring pixel already have a puff behind it?
            // This is checked BEFORE we draw anything for this puff.
            bool hasPuffBehind = result.a > PUFF_OVERLAP_EPS;

            float shadeStrength = mix(PUFF_SHADE_MIN, PUFF_SHADE_MAX, 1.0 - puffHeightInBlob);
            float shadeAmount   = hasPuffBehind ? (1.0 - localV) * shadeStrength : 0.0;

            // Ring (back circle): fully opaque always. Colour only shifts
            // toward the core colour when gated open (something behind
            // it). Otherwise it's the plain, unshaded ring colour — this
            // is what keeps the true outer cloud silhouette crisp and
            // un-shaded, exactly as it should be.
            vec3 ringColor = mix(backColor, frontColor, shadeAmount);
            vec4 srcBack   = vec4(ringColor, outerMask * PUFF_BASE_ALPHA);
            result = _cloudOver(result, srcBack);

            // Core (front circle): never changes. Not shaded, not gated,
            // not faded — always the same solid colour and alpha.
            vec4 srcFront = vec4(frontColor, coreMask * PUFF_CORE_ALPHA);
            result = _cloudOver(result, srcFront);
        }
    }

    return result;
}
#endif