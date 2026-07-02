#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

/*
* Puff-blob cloud system — v4.
 *
 * Puff shape (unchanged in concept)
 * ----------------------------------
 * back circle  (radius PUFF_OUTER_RADIUS) — cloud colour: white with a bit
 *              of horizon sky colour mixed in.
 * front circle (radius PUFF_INNER_RADIUS) — smaller, darker, still fully
 *              sky-colour derived (built from horizon/zenith directly, NOT
 *              from darkening the already-whitened back colour — doing
 *              that produced literal grey, since a near-white colour has
 *              almost no hue left to darken).
 * Fixed radii and rim width — every puff, everywhere, is identically sized.
 *
 * Blobs, not a band
 * ------------------
 * Clouds are literal rounded 3-D regions ("blobs") on a coarse grid. Each
 * blob independently rolls its own vertical centre anywhere within a wide
 * allowed dir.y range, so clouds vary in height across the sky instead of
 * all sitting on one shared line. A blob's silhouette is an ellipsoid with
 * a per-cell randomised boundary offset, so its edge is never a hard
 * cutoff or a perfect geometric outline — combined with the puffs
 * themselves (real circle arcs), the visible edge is organic.
 *
 * Layered compositing (fixes the flat-dark-interior/outline look)
 * ------------------------------------------------------------------
 * Puffs are painted one on top of another with a real src-over-dst blend,
 * bottom-to-top, back circle then front circle per puff — NOT merged with
 * max(). That's what lets individual puff lobes stay visible as rounded
 * bumps instead of collapsing into one flat fill with only the outer edge
 * showing colour.
 *
 * Bottom-blend
 * ------------
 * Each puff's own alpha (both circles, as one unit) fades from opaque at
 * its own top to soft/near-transparent at its own bottom. The STRENGTH of
 * that fade scales with how low the puff sits within the blob it belongs
 * to — puffs near the blob's base melt hard into whatever's beneath them,
 * puffs near the blob's top stay crisp almost all the way down.
 *
 * Single layer for now, per earlier agreement — get this right first.
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

// ── Bottom-blend behaviour ───────────────────────────────────────────────────
const float PUFF_BLEND_MIN = 0.15;  // fade strength for puffs at the TOP of their blob
const float PUFF_BLEND_MAX = 0.95;  // fade strength for puffs at the BOTTOM of their blob

const float PUFF_BASE_ALPHA = 0.92;
const float PUFF_CORE_ALPHA = 1.0;

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

// Finds the nearest cloud "blob" to sample point p. Blobs sit on a coarse
// horizontal grid; each has its own randomised vertical centre (so clouds
// vary in height instead of sharing one band) and a fixed elliptical
// radius. Returns the closest blob's centre and un-clamped ellipsoidal
// distance (< 1 = inside).
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

    // Nothing anywhere near a cloud here — bail before touching the puff grid.
    if (!blobFound || blobEllDist > 1.5) return vec4(0.0);

    // Domain warp on puff placement only, so the blob's overall shape stays
    // coherent while individual puffs don't read as a mechanical lattice.
    float warpX = fbmNoise2D(p.xz * WARP_FREQ + dailySeed) - 0.5;
    float warpZ = fbmNoise2D(p.zx * WARP_FREQ + dailySeed + 7.3) - 0.5;
    vec3 pWarped = p + vec3(warpX, 0.0, warpZ) * WARP_AMT;

    vec3 ip = floor(pWarped);

    vec4 result = vec4(0.0);

    // y outermost so lower puffs are drawn first (background) and higher
    // puffs are drawn last (on top) — this ordering is what makes cloud
    // bases read as sitting underneath their tops.
    for (int y = -1; y <= 1; y++)
    for (int x = -1; x <= 1; x++)
    for (int z = -1; z <= 1; z++) {
        vec3 cell = ip + vec3(x, y, z);

        // Whole-cell admission against the blob we found — never a partial
        // slice. A bit of per-cell noise on the threshold keeps the
        // boundary from reading as a perfect, unnatural ellipse.
        vec3  cellPos    = cell + 0.5;
        vec3  cd         = (cellPos - blobCenter) / vec3(BLOB_RADIUS_XZ, BLOB_RADIUS_Y, BLOB_RADIUS_XZ);
        float cellEll    = length(cd);
        float edgeJitter = (_puffHash3(cell).x - 0.5) * BLOB_EDGE_NOISE;
        if (cellEll > 1.0 + edgeJitter) continue;

        // How low this cell sits within ITS OWN blob: 0 = blob base, 1 = blob top.
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

            // Vertical fade local to THIS puff: 0 at its own bottom edge,
            // 1 at its own top edge.
            float localV = smoothstep(-PUFF_OUTER_RADIUS, PUFF_OUTER_RADIUS, pWarped.y - ctr.y);

            // How strongly that fade applies scales with how low the puff
            // sits in the cloud (blob) it belongs to.
            float blendStrength = mix(PUFF_BLEND_MIN, PUFF_BLEND_MAX, 1.0 - puffHeightInBlob);
            float fade = mix(1.0, localV, blendStrength);

            // Both colours always derive from sky colour (SkyColorData UBO).
            vec3 backColor  = mix(u_skyHorizonColor, vec3(1.0), 0.60);   // white, sky-kissed
            vec3 frontColor = mix(u_skyHorizonColor, u_skyZenithColor, 0.45); // darker, still sky-hued

            vec4 srcBack  = vec4(backColor,  outerMask * fade * PUFF_BASE_ALPHA);
            result = _cloudOver(result, srcBack);

            vec4 srcFront = vec4(frontColor, coreMask * fade * PUFF_CORE_ALPHA);
            result = _cloudOver(result, srcFront);
        }
    }

    return result;
}
#endif