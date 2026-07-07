#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"

/*
* Puff-blob cloud system — v8 (multi-pass layered puffs, toon shading, height-driven ring shading).
 *
 * Puff shape (unchanged in concept)
 * ----------------------------------
 * back circle  (radius outerR, see below) — the "ring". Mostly white with a
 *              bit of horizon sky colour mixed in — lighter than the core.
 * front circle (radius innerR, see below) — the "core". Smaller, darker.
 *              Still white blended WITH sky colour, just a smaller white
 *              mix than the ring. Never pure sky colour on its own. NEVER
 *              shaded, NEVER faded, NEVER gated — colour and alpha
 *              treatment is always identical regardless of position or
 *              what's behind it. Its SIZE, however, now varies (see
 *              "Multi-pass layering" and "Height-based sizing" below).
 * Rim width scales with the puff, so the ring stays proportionally the
 * same thickness relative to the puff at every size.
 *
 * Multi-pass layering
 * ---------------------
 * A cloud is built from NUM_PUFF_PASSES layers, each one a full sweep of
 * the same puff-placement logic over the same fine grid, but with
 * different tuning (PASS_SIZE_SCALE, PASS_REACH, PASS_LIFT):
 *
 *   pass 0: baseline puff size, full reach (spans the whole blob, edge
 *           to edge), no lift. This is the "base coat" that establishes
 *           the cloud's overall silhouette.
 *   pass 1..N-1: each pass is smaller than the one before it, and its
 *           puffs are only allowed to spawn in a region pulled inward
 *           toward the blob's centre of mass and lifted upward within
 *           the blob. Later passes layer on top of earlier ones,
 *           building progressively smaller, more-central, higher-sitting
 *           puff clusters — the piled-up "cauliflower" look of a toon
 *           cumulus cloud.
 *
 * Height-based sizing
 * ---------------------
 * Independent of which pass drew it, every puff is also sized by how high
 * it sits within its OWN blob (bottom of blob = full size, top of blob =
 * PUFF_TOP_SIZE_MUL of full size). This one rule applies identically in
 * every pass — passes only change the baseline size and where puffs are
 * allowed to spawn, not this per-puff height falloff.
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
 * Puffs are painted one on top of another with a real src-over-dst blend
 * — back circle then front circle per puff, NOT merged with max(). Passes
 * are drawn in order (pass 0 first), and within a pass, cells are swept
 * bottom-to-top so lower puffs land first and higher puffs land on top.
 *
 * Ring shading — colour-based, NOT alpha-based, entirely height-driven
 * --------------------------------------------------------------
 * The ring's ALPHA never changes — it is always fully opaque. This is
 * intentional: alpha-fading the ring reads as the cloud dissolving into
 * the sky, which is wrong for a toon look.
 *
 * Instead, only the ring's COLOUR shifts, from the plain ring colour
 * toward the EXACT core colour — a flat, opaque "shadow band", not a
 * see-through fade. The shift amount has exactly one input: how high
 * this puff sits within its own blob (puffHeightInBlob). Nothing else —
 * not overlap with other puffs, not this puff's own local top/bottom —
 * feeds into it.
 *
 *   bottom of blob -> shift = PUFF_SHADE_MAX -> ring colour becomes the
 *                      core colour exactly.
 *   top of blob    -> shift = PUFF_SHADE_MIN -> ring stays mostly (but
 *                      not entirely) its own colour.
 *
 * Every puff at a given height shades the same amount, regardless of
 * pass, regardless of what's behind it.
 */

// ── Puff shape — pass-0 (base layer) baseline; later passes scale this
// down via PASS_SIZE_SCALE ──────────────────────────────────────────────────
const float PUFF_SCALE        = 26.0;
const float PUFF_OUTER_RADIUS = 0.85;
const float PUFF_RIM_WIDTH    = 0.24;
const float PUFF_JITTER       = 0.55;
const float PUFF_EDGE_AA      = 0.06;
const int   PUFFS_PER_CELL    = 3;

// ── Multi-pass layering — see "Multi-pass layering" above ──────────────────
const int NUM_PUFF_PASSES = 4;

// Puff-size multiplier per pass. Pass 0 = 1.0 (full baseline size); each
// later pass is smaller. To add a 5th pass: bump NUM_PUFF_PASSES to 5 and
// append one more (smaller / lower-reach / higher-lift) entry to each of
// the three arrays below, e.g. ..., 0.26) / ..., 0.20) / ..., 0.46).
const float PASS_SIZE_SCALE[NUM_PUFF_PASSES] = float[NUM_PUFF_PASSES](1.00, 0.72, 0.52, 0.37);

// How far each pass's puffs may spawn from the blob's centre, as a
// fraction of the full blob radius. 1.0 = full span (edge to edge),
// smaller = pulled in toward the centre of mass.
const float PASS_REACH[NUM_PUFF_PASSES] = float[NUM_PUFF_PASSES](1.00, 0.70, 0.48, 0.32);

// How far each pass's spawn region is lifted upward within the blob, as a
// fraction of the blob's full vertical diameter. Combined with PASS_REACH,
// this is what pulls later passes toward the upper-centre of the cloud.
const float PASS_LIFT[NUM_PUFF_PASSES] = float[NUM_PUFF_PASSES](0.00, 0.12, 0.24, 0.36);

// Height-based sizing (applies identically in every pass): a puff at the
// very top of its blob is this fraction of the size it would be at the
// very bottom.
const float PUFF_TOP_SIZE_MUL = 0.55;

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

// ── Ring shading behaviour (toon, height-driven) ────────────────────────────
const float PUFF_SHADE_MIN = 0.15;  // shade amount for puffs at the TOP of their blob (ring stays mostly its own colour)
const float PUFF_SHADE_MAX = 1.00;  // shade amount for puffs at the BOTTOM of their blob (ring becomes the core colour exactly)

const float PUFF_BASE_ALPHA = 0.92;
const float PUFF_CORE_ALPHA = 1.0;

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

    // Pass 0 first (base coat, full span), each later pass layered on top,
    // pulled toward the blob's centre and shrunk down.
    for (int passIdx = 0; passIdx < NUM_PUFF_PASSES; passIdx++) {
        float passScale = PASS_SIZE_SCALE[passIdx];
        float reach     = PASS_REACH[passIdx];
        float liftAmt   = PASS_LIFT[passIdx] * BLOB_RADIUS_Y * 2.0;

        vec3 passRadii  = vec3(BLOB_RADIUS_XZ, BLOB_RADIUS_Y, BLOB_RADIUS_XZ) * reach;
        vec3 passCenter = blobCenter + vec3(0.0, liftAmt, 0.0);

        // y outermost so lower puffs are drawn first (background) and
        // higher puffs are drawn last (on top), same as before — now just
        // repeated once per pass.
        for (int y = -1; y <= 1; y++)
        for (int x = -1; x <= 1; x++)
        for (int z = -1; z <= 1; z++) {
            vec3 cell = ip + vec3(x, y, z);

            vec3  cellPos    = cell + 0.5;
            vec3  cd         = (cellPos - passCenter) / passRadii;
            float cellEll    = length(cd);
            float edgeJitter = (_puffHash3(cell + vec3(0.0, float(passIdx) * 91.7, 0.0)).x - 0.5) * BLOB_EDGE_NOISE;
            if (cellEll > 1.0 + edgeJitter) continue;

            // Height within the TRUE blob (always the full, un-pulled-in
            // extent) — this is what drives the bigger-at-bottom /
            // smaller-at-top size falloff AND the ring-shading falloff,
            // identically in every pass.
            float puffHeightInBlob = clamp(
                (cellPos.y - (blobCenter.y - BLOB_RADIUS_Y)) / (2.0 * BLOB_RADIUS_Y), 0.0, 1.0);
            float heightSizeMul = mix(1.0, PUFF_TOP_SIZE_MUL, puffHeightInBlob);
            float sizeMul       = passScale * heightSizeMul;

            float outerR    = PUFF_OUTER_RADIUS * sizeMul;
            float rimW      = PUFF_RIM_WIDTH * sizeMul;
            float innerR    = outerR - rimW;
            float aa        = PUFF_EDGE_AA * sizeMul;
            float jitterAmt = PUFF_JITTER * passScale;

            // Ring -> core colour shift for every puff in this cell: purely
            // a function of height in the blob (see header comment), no
            // other inputs. Same value for every puff drawn from this cell.
            float shadeAmount = mix(PUFF_SHADE_MIN, PUFF_SHADE_MAX, 1.0 - puffHeightInBlob);

            for (int k = 0; k < PUFFS_PER_CELL; k++) {
                vec3  seed   = cell + vec3(0.0, 0.0, float(k) * 17.13 + 4.7 + float(passIdx) * 133.7);
                vec3  jitter = _puffHash3(seed);
                vec3  ctr    = cell + 0.5 + (jitter - 0.5) * jitterAmt;
                float dist   = length(pWarped - ctr);

                float outerMask = 1.0 - smoothstep(outerR - aa, outerR + aa, dist);
                if (outerMask <= 0.001) continue;

                float coreMask = 1.0 - smoothstep(innerR - aa, innerR + aa, dist);

                vec3 frontSkyTint = mix(u_skyHorizonColor, u_skyZenithColor, PUFF_FRONT_SKY_MIX);
                vec3 backColor    = mix(u_skyHorizonColor, vec3(1.0), PUFF_BACK_WHITE_MIX);
                vec3 frontColor   = mix(frontSkyTint,       vec3(1.0), PUFF_FRONT_WHITE_MIX);

                // Ring (back circle): fully opaque always. Colour shifts
                // toward the core colour purely based on shadeAmount (this
                // puff's height in its blob) — same value for every puff in
                // this cell, no other inputs.
                vec3 ringColor = mix(backColor, frontColor, shadeAmount);
                vec4 srcBack   = vec4(ringColor, outerMask * PUFF_BASE_ALPHA);
                result = _cloudOver(result, srcBack);

                // Core (front circle): never shaded, never faded — always
                // the same solid colour and alpha treatment.
                vec4 srcFront = vec4(frontColor, coreMask * PUFF_CORE_ALPHA);
                result = _cloudOver(result, srcFront);
            }
        }
    }

    return result;
}
#endif