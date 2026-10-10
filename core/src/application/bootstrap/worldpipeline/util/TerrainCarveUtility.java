package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TerrainCarveUtility extends EngineUtility {

    /*
     * Stateless volume fields that hollow and thread the ground. Caves are a
     * signed density measured in blocks from the nearest wall, positive inside
     * the hollow: long winding tunnels where two noise surfaces cross, thin
     * passages where two finer ones cross behind a gate, and caverns where a
     * broad field roughened by the finer ones swells, swelling wider the
     * deeper they lie. Each closes
     * smoothly toward the floor and roof of its band and below the tide where
     * the sea is walled off, so nothing ends on a flat cut and a wall can be
     * placed at sub-block precision. Sea caves run in from the shore through
     * the tide band, a notch is cut beneath sea cliffs, and mineral veins run
     * as thin seams gated by a broad field. Noise is sampled on a lattice
     * anchored to the world grid every CAVE_LATTICE_STEP_BLOCKS and read back
     * trilinearly, so every caller filling the lattice over any range reads
     * the same value at the same point, and a lattice cell's corners bound
     * every value inside it, which lets solid rock be skipped a cell at a time.
     */

    public static final int LATTICE_STEP = EngineSetting.CAVE_LATTICE_STEP_BLOCKS;
    public static final int LATTICE_SIDE = EngineSetting.CHUNK_SIZE / LATTICE_STEP + 1;
    public static final int COLUMN_LATTICE_SIDE = 2;

    public static final int CHANNEL_SPAGHETTI_A = 0;
    public static final int CHANNEL_SPAGHETTI_B = 1;
    public static final int CHANNEL_SPAGHETTI_RADIUS = 2;
    public static final int CHANNEL_CHEESE = 3;
    public static final int CHANNEL_NOODLE_A = 4;
    public static final int CHANNEL_NOODLE_B = 5;
    public static final int CHANNEL_NOODLE_GATE = 6;
    public static final int CHANNEL_SEA = 7;
    public static final int CAVE_CHANNELS = 8;

    public static final int CHANNEL_VEIN_RIBBON = 0;
    public static final int CHANNEL_VEIN_GATE = 1;
    public static final int VEIN_CHANNELS_PER_VEIN = 2;

    private static final float GRADIENT = EngineSetting.CAVE_NOISE_GRADIENT;
    private static final float SPAGHETTI_SCALE = (float) EngineSetting.CAVE_SPAGHETTI_WAVELENGTH_BLOCKS / GRADIENT;
    private static final float NOODLE_SCALE = (float) EngineSetting.CAVE_NOODLE_WAVELENGTH_BLOCKS / GRADIENT;
    private static final float CHEESE_SCALE = (float) EngineSetting.CAVE_CHEESE_WAVELENGTH_BLOCKS / GRADIENT;

    // Lattice \\

    // Number of lattice rows covering every point from fromY to toY, starting at the row at or below fromY
    public static int computeLatticeRows(int fromY, int toY) {
        return (alignDown(toY) - alignDown(fromY)) / LATTICE_STEP + 2;
    }

    public static int alignDown(int worldY) {
        return Math.floorDiv(worldY, LATTICE_STEP) * LATTICE_STEP;
    }

    public static long alignDown(long worldXZ) {
        return Math.floorDiv(worldXZ, (long) LATTICE_STEP) * LATTICE_STEP;
    }

    // side points per horizontal axis from the origin; the sea channel is only sampled where it can matter
    public static void fillCaveLattice(
            long seed,
            double worldWidthBlocks, double worldHeightBlocks,
            long originX, int originY, long originZ,
            int side,
            int rows,
            boolean seaChannel,
            float[] outLattice) {

        for (int row = 0; row < rows; row++) {
            for (int z = 0; z < side; z++) {
                for (int x = 0; x < side; x++) {

                    double worldX = originX + (long) x * LATTICE_STEP;
                    double worldY = originY + row * LATTICE_STEP;
                    double worldZ = originZ + (long) z * LATTICE_STEP;
                    int base = latticeIndex(side, x, row, z) * CAVE_CHANNELS;

                    outLattice[base + CHANNEL_SPAGHETTI_A] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_SPAGHETTI_SEED_SALT_A,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_SPAGHETTI_WAVELENGTH_BLOCKS,
                            EngineSetting.CAVE_SPAGHETTI_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_SPAGHETTI_B] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_SPAGHETTI_SEED_SALT_B,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_SPAGHETTI_WAVELENGTH_BLOCKS,
                            EngineSetting.CAVE_SPAGHETTI_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_SPAGHETTI_RADIUS] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_SPAGHETTI_RADIUS_SEED_SALT,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_SPAGHETTI_RADIUS_WAVELENGTH_BLOCKS, 1f);

                    outLattice[base + CHANNEL_CHEESE] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_CHEESE_SEED_SALT,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_CHEESE_WAVELENGTH_BLOCKS, EngineSetting.CAVE_CHEESE_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_NOODLE_A] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_NOODLE_SEED_SALT_A,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_NOODLE_WAVELENGTH_BLOCKS, EngineSetting.CAVE_NOODLE_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_NOODLE_B] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_NOODLE_SEED_SALT_B,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_NOODLE_WAVELENGTH_BLOCKS, EngineSetting.CAVE_NOODLE_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_NOODLE_GATE] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_NOODLE_GATE_SEED_SALT,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_NOODLE_GATE_WAVELENGTH_BLOCKS, 1f);

                    outLattice[base + CHANNEL_CHEESE] += outLattice[base + CHANNEL_SPAGHETTI_RADIUS]
                            * EngineSetting.CAVE_CHEESE_SWELL_DETAIL
                            + outLattice[base + CHANNEL_NOODLE_A] * EngineSetting.CAVE_CHEESE_FINE_DETAIL;

                    outLattice[base + CHANNEL_SEA] = seaChannel
                            ? TerrainNoiseUtility.sampleVolume(
                                    seed ^ EngineSetting.CAVE_SEA_SEED_SALT,
                                    worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                                    EngineSetting.CAVE_SEA_WAVELENGTH_BLOCKS, EngineSetting.CAVE_SEA_VERTICAL_SCALE)
                            : 0f;
                }
            }
        }
    }

    // Only the slots marked active are sampled; the rest keep stale values no caller reads
    public static void fillVeinLattice(
            long seed,
            double worldWidthBlocks, double worldHeightBlocks,
            long originX, int originY, long originZ,
            int rows,
            long[] veinSeeds,
            boolean[] activeVeins,
            int veinCount,
            float[] outLattice) {

        int channels = veinCount * VEIN_CHANNELS_PER_VEIN;

        for (int row = 0; row < rows; row++) {
            for (int z = 0; z < LATTICE_SIDE; z++) {
                for (int x = 0; x < LATTICE_SIDE; x++) {

                    double worldX = originX + (long) x * LATTICE_STEP;
                    double worldY = originY + row * LATTICE_STEP;
                    double worldZ = originZ + (long) z * LATTICE_STEP;
                    int base = latticeIndex(LATTICE_SIDE, x, row, z) * channels;

                    for (int vein = 0; vein < veinCount; vein++) {

                        if (!activeVeins[vein])
                            continue;

                        long veinSeed = seed ^ EngineSetting.TERRAIN_VEIN_SEED_SALT ^ veinSeeds[vein];
                        int veinBase = base + vein * VEIN_CHANNELS_PER_VEIN;

                        outLattice[veinBase + CHANNEL_VEIN_RIBBON] = TerrainNoiseUtility.sampleVolume(
                                veinSeed, worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_VEIN_WAVELENGTH_BLOCKS, 1f);

                        outLattice[veinBase + CHANNEL_VEIN_GATE] = TerrainNoiseUtility.sampleVolume(
                                veinSeed ^ EngineSetting.TERRAIN_VEIN_GATE_SEED_SALT,
                                worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.TERRAIN_VEIN_GATE_WAVELENGTH_BLOCKS, 1f);
                    }
                }
            }
        }
    }

    // Local coordinates are measured from the lattice's first point, and points past its last are held to its edge
    public static float sampleLattice(
            float[] lattice,
            int side,
            int rows,
            int channels,
            int channel,
            float localX,
            float localY,
            float localZ) {

        int cellX = Math.min((int) (localX / LATTICE_STEP), side - 2);
        int cellY = Math.min((int) (localY / LATTICE_STEP), rows - 2);
        int cellZ = Math.min((int) (localZ / LATTICE_STEP), side - 2);

        float tx = (localX - cellX * LATTICE_STEP) / LATTICE_STEP;
        float ty = (localY - cellY * LATTICE_STEP) / LATTICE_STEP;
        float tz = (localZ - cellZ * LATTICE_STEP) / LATTICE_STEP;

        float v000 = lattice[latticeIndex(side, cellX, cellY, cellZ) * channels + channel];
        float v100 = lattice[latticeIndex(side, cellX + 1, cellY, cellZ) * channels + channel];
        float v010 = lattice[latticeIndex(side, cellX, cellY + 1, cellZ) * channels + channel];
        float v110 = lattice[latticeIndex(side, cellX + 1, cellY + 1, cellZ) * channels + channel];
        float v001 = lattice[latticeIndex(side, cellX, cellY, cellZ + 1) * channels + channel];
        float v101 = lattice[latticeIndex(side, cellX + 1, cellY, cellZ + 1) * channels + channel];
        float v011 = lattice[latticeIndex(side, cellX, cellY + 1, cellZ + 1) * channels + channel];
        float v111 = lattice[latticeIndex(side, cellX + 1, cellY + 1, cellZ + 1) * channels + channel];

        float near = lerp(lerp(v000, v100, tx), lerp(v010, v110, tx), ty);
        float far = lerp(lerp(v001, v101, tx), lerp(v011, v111, tx), ty);

        return lerp(near, far, tz);
    }

    // The value one channel holds at one lattice point
    public static float readLattice(float[] lattice, int side, int channels, int channel, int x, int row, int z) {
        return lattice[latticeIndex(side, x, row, z) * channels + channel];
    }

    private static int latticeIndex(int side, int x, int row, int z) {
        return (row * side + z) * side + x;
    }

    // Tunnels \\

    // Long winding tunnels where two surfaces cross, their radius swelling, pinching and closing with a third field
    public static float computeSpaghetti(float spaghettiA, float spaghettiB, float radiusField, float tunnels) {

        if (tunnels <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float distance = (float) Math.sqrt(spaghettiA * spaghettiA + spaghettiB * spaghettiB) * SPAGHETTI_SCALE;

        return computeSpaghettiRadius(radiusField, tunnels) - distance;
    }

    private static float computeSpaghettiRadius(float radiusField, float tunnels) {

        float swell = clamp01(radiusField * 0.5f + 0.5f);
        float radius = EngineSetting.CAVE_SPAGHETTI_MIN_RADIUS_BLOCKS
                + (EngineSetting.CAVE_SPAGHETTI_MAX_RADIUS_BLOCKS - EngineSetting.CAVE_SPAGHETTI_MIN_RADIUS_BLOCKS)
                        * swell;

        return radius * computeAmountScale(tunnels)
                * smoothstep(swell / EngineSetting.CAVE_SPAGHETTI_CLOSE_SHARE);
    }

    // Thin passages where two finer surfaces cross, only where their gate opens, narrowing to nothing at its edge
    public static float computeNoodle(float noodleA, float noodleB, float gate, float radiusField, float noodles) {

        float open = computeNoodleOpening(gate, noodles);

        if (open <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float distance = (float) Math.sqrt(noodleA * noodleA + noodleB * noodleB) * NOODLE_SCALE;
        float swell = clamp01(radiusField * 0.5f + 0.5f);
        float radius = EngineSetting.CAVE_NOODLE_MIN_RADIUS_BLOCKS
                + (EngineSetting.CAVE_NOODLE_MAX_RADIUS_BLOCKS - EngineSetting.CAVE_NOODLE_MIN_RADIUS_BLOCKS) * swell;

        return radius * open - distance;
    }

    private static float computeNoodleOpening(float gate, float noodles) {

        if (noodles <= 0f)
            return 0f;

        float threshold = EngineSetting.CAVE_NOODLE_GATE_THRESHOLD - noodles * EngineSetting.CAVE_NOODLE_GATE_RANGE;

        return smoothstep((gate - threshold) / EngineSetting.CAVE_NOODLE_GATE_EDGE);
    }

    // Caverns where a broad field swells past its threshold, the threshold falling as the ground above deepens
    public static float computeCheese(float cheese, float caverns, float depthBelowGroundBlocks) {

        if (caverns <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float deep = clamp01((depthBelowGroundBlocks - EngineSetting.CAVE_CHEESE_MIN_DEPTH_BLOCKS)
                / EngineSetting.CAVE_CHEESE_DEPTH_RAMP_BLOCKS);
        float share = EngineSetting.CAVE_CHEESE_SHALLOW_SHARE + (1f - EngineSetting.CAVE_CHEESE_SHALLOW_SHARE) * deep;

        return (cheese - computeCheeseThreshold(caverns * share)) * CHEESE_SCALE;
    }

    private static float computeCheeseThreshold(float amount) {
        return EngineSetting.CAVE_CHEESE_THRESHOLD_MAX
                - (EngineSetting.CAVE_CHEESE_THRESHOLD_MAX - EngineSetting.CAVE_CHEESE_THRESHOLD_MIN)
                        * Math.min(amount, 1f);
    }

    private static float computeAmountScale(float amount) {
        return EngineSetting.CAVE_SPAGHETTI_AMOUNT_FLOOR
                + (1f - EngineSetting.CAVE_SPAGHETTI_AMOUNT_FLOOR) * Math.min(amount, 1f);
    }

    // Bounds \\

    // The most any tunnel can open anywhere inside a lattice cell, from the extremes its corners hold
    public static float boundSpaghetti(float minA, float maxA, float minB, float maxB, float tunnels) {

        if (tunnels <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float nearA = computeNearestToZero(minA, maxA);
        float nearB = computeNearestToZero(minB, maxB);
        float distance = (float) Math.sqrt(nearA * nearA + nearB * nearB) * SPAGHETTI_SCALE;

        return EngineSetting.CAVE_SPAGHETTI_MAX_RADIUS_BLOCKS * computeAmountScale(tunnels) - distance;
    }

    public static float boundNoodle(float minA, float maxA, float minB, float maxB, float maxGate, float noodles) {

        float open = computeNoodleOpening(maxGate, noodles);

        if (open <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        float nearA = computeNearestToZero(minA, maxA);
        float nearB = computeNearestToZero(minB, maxB);
        float distance = (float) Math.sqrt(nearA * nearA + nearB * nearB) * NOODLE_SCALE;

        return EngineSetting.CAVE_NOODLE_MAX_RADIUS_BLOCKS * open - distance;
    }

    public static float boundCheese(float maxCheese, float caverns) {

        if (caverns <= 0f)
            return EngineSetting.CAVE_DENSITY_SOLID;

        return (maxCheese - computeCheeseThreshold(caverns)) * CHEESE_SCALE;
    }

    private static float computeNearestToZero(float min, float max) {

        if (min <= 0f && max >= 0f)
            return 0f;

        return Math.min(Math.abs(min), Math.abs(max));
    }

    // Band \\

    // Closes every cave toward the lowest block of its band, so it ends on a rounded floor
    public static float computeFloorPenalty(float worldY, int floorY) {
        return Math.max(0f, floorY + EngineSetting.CAVE_FLOOR_FADE_BLOCKS - worldY)
                * EngineSetting.CAVE_FLOOR_FADE_STRENGTH;
    }

    // Closes every cave toward the highest block of its band, so it ends on a domed roof
    public static float computeRoofPenalty(float worldY, int ceilingY) {
        return Math.max(0f, worldY - (ceilingY - EngineSetting.CAVE_ROOF_TAPER_BLOCKS))
                * EngineSetting.CAVE_ROOF_TAPER_STRENGTH;
    }

    // Rock left above a cave thickens with the slope, so a steep face keeps as much rock sideways as flat ground above
    public static int computeRoofBlocks(float slope) {
        return Math.min(
                EngineSetting.CAVE_ROOF_MAX_BLOCKS,
                EngineSetting.CAVE_ROOF_BLOCKS + Math.round(slope * EngineSetting.CAVE_ROOF_SLOPE_BLOCKS));
    }

    // Sea \\

    // 1 across the walled band between a sea that floods caves and ground kept dry, easing to 0 either side of it
    public static float computeBarrierWeight(float coastalWeight) {

        float edge = EngineSetting.CAVE_SEA_BARRIER_EDGE;
        float rise = smoothstep((coastalWeight - (EngineSetting.CAVE_SEA_BARRIER_WEIGHT - edge)) / edge);
        float fall = 1f - smoothstep((coastalWeight - EngineSetting.CAVE_SEA_FLOOD_WEIGHT) / edge);

        return Math.min(rise, fall);
    }

    // Caves close below high tide wherever the sea is walled off, easing open again above it
    public static float computeBarrierPenalty(float worldY, float barrierWeight) {

        if (barrierWeight <= 0f)
            return 0f;

        float above = (worldY - TideUtility.BAND_MAX_Y) / EngineSetting.CAVE_SEA_BARRIER_RISE_BLOCKS;

        return barrierWeight * EngineSetting.CAVE_SEA_BARRIER_PENALTY_BLOCKS * (1f - clamp01(above));
    }

    // Runs in from the shore through the whole tide band with headroom above high tide, tallest at the band's middle
    // and narrowing as it reaches inland
    public static boolean isSeaCave(float sea, float seaCaves, int worldY, float shoreDistanceBlocks) {

        if (seaCaves <= 0f || shoreDistanceBlocks < 0f || shoreDistanceBlocks >= EngineSetting.CAVE_SEA_REACH_BLOCKS)
            return false;

        float center = (TideUtility.BAND_MIN_Y + TideUtility.BAND_MAX_Y) * 0.5f;
        float halfHeight = (TideUtility.BAND_MAX_Y - TideUtility.BAND_MIN_Y) * 0.5f
                + EngineSetting.CAVE_SEA_HEADROOM_BLOCKS;
        float offset = (worldY - center) / halfHeight;
        float profile = 1f - offset * offset;

        if (profile <= 0f)
            return false;

        float inland = 1f - shoreDistanceBlocks / EngineSetting.CAVE_SEA_REACH_BLOCKS;
        float width = Math.min(seaCaves, 1f) * EngineSetting.CAVE_SEA_MAX_WIDTH * profile * inland;

        return Math.abs(sea) < width;
    }

    // Undercuts a sea cliff from its face inland across the tide band, deepest from low tide to mean sea level and
    // closing toward the splash line just above high tide
    public static boolean isNotch(int worldY, float faceDistanceBlocks, float overhangBlocks) {

        if (overhangBlocks <= 0f || worldY < TideUtility.BAND_MIN_Y || worldY > getNotchTopY())
            return false;

        int seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;
        float closing = 1f - Math.max(0, worldY - seaLevel) / (float) (getNotchTopY() - seaLevel + 1);

        return faceDistanceBlocks < overhangBlocks * closing;
    }

    private static int getNotchTopY() {
        return TideUtility.BAND_MAX_Y + EngineSetting.TERRAIN_COAST_NOTCH_SPLASH_BLOCKS;
    }

    // The lowest and highest blocks a notch or sea cave can hollow
    public static int getSeaFeatureFloorY() {
        return TideUtility.BAND_MIN_Y - EngineSetting.CAVE_SEA_HEADROOM_BLOCKS;
    }

    public static int getSeaFeatureCeilingY() {
        return Math.max(getNotchTopY(), TideUtility.BAND_MAX_Y + EngineSetting.CAVE_SEA_HEADROOM_BLOCKS);
    }

    // Entrances \\

    // The rare patches where caves may break through the ground, from a broad field over the world's surface
    public static boolean isEntrance(
            long seed,
            double worldX, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks) {

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);

        float field = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.CAVE_ENTRANCE_SEED_SALT,
                Math.cos(spatialAngle), Math.sin(spatialAngle), worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.CAVE_ENTRANCE_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_FEATURE_MASK_OCTAVES,
                EngineSetting.TERRAIN_FEATURE_MASK_PERSISTENCE,
                EngineSetting.TERRAIN_FEATURE_MASK_LACUNARITY);

        return field > EngineSetting.CAVE_ENTRANCE_THRESHOLD;
    }

    // Veins \\

    // A broad field opens the rock to a vein only where its abundance reaches
    public static boolean isVeinGateOpen(float gate, BiomeVeinStruct vein) {
        return vein.getAbundance() > 0f && gate > EngineSetting.TERRAIN_VEIN_GATE_THRESHOLD
                - vein.getAbundance() * EngineSetting.TERRAIN_VEIN_GATE_RANGE;
    }

    // A thin seam where the ribbon field crosses zero, as thick as the vein declares
    public static boolean isVeinSeam(float ribbon, BiomeVeinStruct vein) {
        return Math.abs(ribbon) < vein.getThicknessBlocks() * EngineSetting.TERRAIN_VEIN_SHEET_GRADIENT
                / (float) EngineSetting.TERRAIN_VEIN_WAVELENGTH_BLOCKS;
    }

    // Hashing \\

    public static long hashCell(long seed, int cellX, int cellY, int cellZ) {
        return BiomeFieldUtility.hashCell(seed ^ (cellY * EngineSetting.CAVE_HASH_Y_MULTIPLIER), cellX, cellZ);
    }

    public static float rollCell(long seed, long salt, int cellX, int cellY, int cellZ) {
        return BiomeFieldUtility.hash01(hashCell(seed ^ salt, cellX, cellY, cellZ));
    }

    // Math \\

    public static float smoothstep(float t) {

        if (t <= 0f)
            return 0f;

        if (t >= 1f)
            return 1f;

        return t * t * (3f - 2f * t);
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
