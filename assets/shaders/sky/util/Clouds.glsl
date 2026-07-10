#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL
#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"
#include "includes/SkyColorData.glsl"
#include "includes/WeatherData.glsl"
#include "includes/WeatherRegionData.glsl"

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
 * The ring's ALPHA never changes — it is always fully opaque (before the
 * whole-blob presence fade described below is applied). This is
 * intentional: alpha-fading the ring on its own reads as the cloud
 * dissolving into the sky, which is wrong for a toon look.
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
 *
 * Weather integration
 * ---------------------
 * Blob spawn density and puff color are no longer fixed constants — see
 * "Weather-driven coverage & color" below. Coverage and color come from
 * WeatherData (the player's own resolved weather) blended with
 * WeatherRegionData's 8 compass-direction samples by the view ray's
 * heading, so an approaching storm is visible building on the horizon in
 * its own direction, in the same colour and density RegionSampleBranch
 * already resolved for it on the CPU, before it ever reaches the player.
 *
 * Smooth existence & smooth sector blending
 * -------------------------------------------
 * Two changes here specifically target the "clouds flicker / cut on and
 * off / colour changes too fast" failure mode:
 *
 * 1. Every value WeatherData/WeatherRegionData carries has already been
 *    passed through RegionSampleBranch's own temporal smoothing (see
 *    EngineSetting.WEATHER_SAMPLE_SMOOTHING_TIME_SECONDS) before it ever
 *    reaches these uniforms, so u_cloudCoverage and every
 *    u_cloudCoverage<Direction> value can only glide, never snap.
 * 2. Individual blob EXISTENCE is no longer a hard per-cell boolean
 *    (previously: `if (hA.x > spawnChance) continue;`). With dozens of
 *    blob cells each carrying their own fixed random threshold, a
 *    smoothly rising or falling coverage value would still cross many of
 *    those thresholds in quick succession, reading as clouds flickering
 *    on and off across the sky ("windshield wipers") even once the
 *    coverage value itself was smooth. _findNearestBlob now returns a
 *    continuous blobPresence in [0,1] — a narrow smoothstep band around
 *    each cell's own threshold — and calculateClouds scales the whole
 *    blob's final alpha by it, so a blob now visibly thins into
 *    translucency and back rather than popping.
 * 3. _sampleHorizonWeather's blend across the 8 compass sectors now uses
 *    smoothstep instead of a linear mix — still only 8 samples, but the
 *    RATE of change is now continuous at each sector boundary too (not
 *    just the value), which removes the visible "crease" a linear blend
 *    leaves at every 45-degree seam.
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
// Spawn chance used to be a fixed constant here — now resolved per-fragment
// from live weather coverage, see "Weather-driven coverage & color" below.
const float BLOB_RADIUS_XZ    = 3.0;   // horizontal radius
const float BLOB_RADIUS_Y     = 1.4;   // vertical radius (flatter than wide)
const float BLOB_EDGE_NOISE   = 0.22;  // per-cell randomised boundary roughness

// How gradually an individual blob fades in/out of existence as the
// resolved spawnChance drifts past that blob's own fixed per-cell
// threshold — see "Smooth existence & smooth sector blending" above. A
// wider range fades more gradually (softer, but a coverage change takes
// longer to visibly add/remove blobs); narrower snaps back closer to the
// old hard-cutoff look. 0.12 reads as a soft thinning-in/out over a
// believable few seconds of coverage drift, not an instant pop.
const float BLOB_PRESENCE_FADE_RANGE = 0.12;

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
const float PUFF_BACK_WHITE_MIX  = 0.75; // ring: mostly weather-tinted, lightly sky-tinted
const float PUFF_FRONT_SKY_MIX   = 0.45; // core's underlying horizon/zenith blend
const float PUFF_FRONT_WHITE_MIX = 0.55; // core: weather-tinted colour mixed over that tint, still darker than the ring

const float CLOUD_WIND_SPEED = 0.0035;

// ── Weather-driven coverage & color ─────────────────────────────────────────
// Ties this file's blob spawn density and puff color to the live weather
// simulation instead of the fixed constants used previously. WeatherData
// carries the resolved weather at the player's own position (already
// blending local wind-drifted noise with GlobalNoiseBranch's planetary
// rotation noise — see WeatherManager/RegionSampleBranch); WeatherRegionData
// carries that same resolution 8 compass directions out
// (WEATHER_REGION_SAMPLE_DISTANCE chunks away), refreshed every frame by
// WeatherBufferBranch. Blending across those 8 directions by the view ray's
// horizontal heading, then fading toward the player's own local sample as
// the ray tilts up toward the zenith, is what makes an approaching storm
// visible on the horizon in its own direction before it ever reaches the
// player — the CPU-side sampling this reads already existed; this is the
// GPU-side consumer that was missing.
//
// Compass convention matches Direction2Vector / RegionSampleBranch's own
// sampling offsets: north = -Z, east = +X, south = +Z, west = -X, the same
// axes chunk coordinates already sample against. If clouds appear to build
// from the wrong horizon direction in testing, this is the mapping to fix.
const float WEATHER_COVERAGE_SPAWN_MIN = 0.08; // clearest sky still shows a few stray puffs
const float WEATHER_COVERAGE_SPAWN_MAX = 0.85; // fully overcast — blobs fill nearly every cell

float _resolveSpawnChance(float coverage) {
    return mix(WEATHER_COVERAGE_SPAWN_MIN, WEATHER_COVERAGE_SPAWN_MAX, clamp(coverage, 0.0, 1.0));
}

struct CompassSample {
    float coverage;
    vec3  color;
};

CompassSample _compassSample(int index) {
    if (index == 0) return CompassSample(u_cloudCoverageNorth,     u_cloudColorNorth);
    if (index == 1) return CompassSample(u_cloudCoverageNortheast, u_cloudColorNortheast);
    if (index == 2) return CompassSample(u_cloudCoverageEast,      u_cloudColorEast);
    if (index == 3) return CompassSample(u_cloudCoverageSoutheast, u_cloudColorSoutheast);
    if (index == 4) return CompassSample(u_cloudCoverageSouth,     u_cloudColorSouth);
    if (index == 5) return CompassSample(u_cloudCoverageSouthwest, u_cloudColorSouthwest);
    if (index == 6) return CompassSample(u_cloudCoverageWest,      u_cloudColorWest);
    return CompassSample(u_cloudCoverageNorthwest, u_cloudColorNorthwest);
}

// Blends the two nearest of the 8 compass samples by the view ray's
// horizontal heading (see convention note above). Uses a smoothstep-eased
// blend factor rather than a raw linear one — a linear mix is continuous
// in VALUE at each 45-degree sector boundary but not in its RATE of
// change, which shows up as a visible "crease" sweeping across the sky as
// the underlying compass values update. Easing removes that crease.
void _sampleHorizonWeather(vec3 dir, out float coverage, out vec3 color) {
    float headingDeg = degrees(atan(dir.x, -dir.z));
    headingDeg = mod(headingDeg + 360.0, 360.0);

    float sector = headingDeg / 45.0;
    int i0 = int(floor(sector)) % 8;
    int i1 = (i0 + 1) % 8;
    float t = smoothstep(0.0, 1.0, fract(sector));

    CompassSample a = _compassSample(i0);
    CompassSample b = _compassSample(i1);

    coverage = mix(a.coverage, b.coverage, t);
    color    = mix(a.color, b.color, t);
}

// Public: resolves the coverage and color the sky's puff system should use
// for a given view ray. Fades from the horizon blend above toward the
// player's own local WeatherData sample as dir.y rises — dir.y is already
// clamped to [0, CLOUD_DIR_Y_HARD_LIMIT] by calculateClouds() before this
// is ever called, so the fade completes within the cloud layer's own
// visible vertical band rather than across the full hemisphere.
void sampleWeatherForSky(vec3 dir, out float coverage, out vec3 color) {
    float horizonCoverage;
    vec3  horizonColor;
    _sampleHorizonWeather(dir, horizonCoverage, horizonColor);

    float zenithT = clamp(dir.y / CLOUD_DIR_Y_HARD_LIMIT, 0.0, 1.0);

    coverage = mix(horizonCoverage, u_cloudCoverage, zenithT);
    color    = mix(horizonColor, u_cloudColor, zenithT);
}

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

// Finds the nearest cloud "blob" to sample point p. spawnChance comes from
// live weather coverage (see _resolveSpawnChance above) rather than a fixed
// constant, so overcast directions/positions fill in with blobs and clear
// ones thin out. blobPresence is a continuous [0,1] existence factor for
// whichever blob is nearest — see "Smooth existence & smooth sector
// blending" in the file header for why this replaced a hard boolean gate.
void _findNearestBlob(vec3 p, float dailySeed, float spawnChance,
    out vec3 blobCenter, out float blobEllDist, out bool blobFound, out float blobPresence) {
    vec2 cellCoord = floor(p.xz / BLOB_CELL_XZ);

    blobFound    = false;
    blobEllDist  = 1.0e6;
    blobCenter   = vec3(0.0);
    blobPresence = 0.0;

    for (int x = -1; x <= 1; x++)
    for (int z = -1; z <= 1; z++) {
        vec2 cell = cellCoord + vec2(x, z);

        vec3 hA = _puffHash3(vec3(cell.x, 3.17 + dailySeed, cell.y));
        float presence = smoothstep(0.0, BLOB_PRESENCE_FADE_RANGE, spawnChance - hA.x);
        if (presence <= 0.001) continue; // fully faded out — skip, still an optimization

        vec2 centerXZ = (cell + 0.5) * BLOB_CELL_XZ + (hA.yz - 0.5) * BLOB_JITTER_XZ;

        vec3  hB         = _puffHash3(vec3(cell.x, 58.91 + dailySeed, cell.y));
        float centerYDir = mix(CLOUD_DIR_Y_MIN, CLOUD_DIR_Y_MAX, hB.x);
        vec3  center     = vec3(centerXZ.x, centerYDir * PUFF_SCALE, centerXZ.y);

        vec3  d       = (p - center) / vec3(BLOB_RADIUS_XZ, BLOB_RADIUS_Y, BLOB_RADIUS_XZ);
        float ellDist = length(d);

        if (ellDist < blobEllDist) {
            blobEllDist  = ellDist;
            blobCenter   = center;
            blobFound    = true;
            blobPresence = presence;
        }
    }
}

// ── Public API ────────────────────────────────────────────────────────────────

vec4 calculateClouds(vec3 dir, float dailySeed) {
    if (dir.y < 0.0 || dir.y > CLOUD_DIR_Y_HARD_LIMIT) return vec4(0.0);

    float weatherCoverage;
    vec3  weatherColor;
    sampleWeatherForSky(dir, weatherCoverage, weatherColor);
    float spawnChance = _resolveSpawnChance(weatherCoverage);

    vec3 wind = vec3(u_time * CLOUD_WIND_SPEED + dailySeed, 0.0,
        u_time * CLOUD_WIND_SPEED * 0.7 + dailySeed * 1.3);
    vec3 p = dir * PUFF_SCALE + wind;

    vec3  blobCenter;
    float blobEllDist;
    bool  blobFound;
    float blobPresence;
    _findNearestBlob(p, dailySeed, spawnChance, blobCenter, blobEllDist, blobFound, blobPresence);

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
                vec3 backColor    = mix(u_skyHorizonColor, weatherColor, PUFF_BACK_WHITE_MIX);
                vec3 frontColor   = mix(frontSkyTint,       weatherColor, PUFF_FRONT_WHITE_MIX);

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

    // Whole-blob presence fade — see "Smooth existence & smooth sector
    // blending" in the file header. Scales only alpha, never rgb, so a
    // half-formed blob reads as thin/translucent rather than discolored.
    return vec4(result.rgb, result.a * blobPresence);
}
#endif