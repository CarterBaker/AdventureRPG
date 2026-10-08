package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeVeinStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TerrainCarveUtility extends EngineUtility {

    /*
     * Stateless volume fields that hollow and thread the ground: winding
     * tunnels where two noise surfaces cross, caverns where a broad field
     * swells, sea caves running in from the shore through the tide band, the
     * notch the waves cut beneath a sea cliff between low and high tide, and mineral veins as thin seams
     * gated by a broad field. Noise is sampled on a lattice anchored to the
     * world grid every CAVE_LATTICE_STEP_BLOCKS and read back trilinearly, so
     * any caller filling the lattice over any range reads the same value at
     * the same block, and a chunk pays for a few hundred samples rather than
     * one per block. Thresholds are pure functions of the sampled values and
     * the column's controls.
     */

    public static final int LATTICE_STEP = EngineSetting.CAVE_LATTICE_STEP_BLOCKS;
    public static final int LATTICE_SIDE = EngineSetting.CHUNK_SIZE / LATTICE_STEP + 1;

    public static final int CHANNEL_TUNNEL_A = 0;
    public static final int CHANNEL_TUNNEL_B = 1;
    public static final int CHANNEL_CAVERN = 2;
    public static final int CHANNEL_SEA = 3;
    public static final int CAVE_CHANNELS = 4;

    public static final int CHANNEL_VEIN_RIBBON = 0;
    public static final int CHANNEL_VEIN_GATE = 1;
    public static final int VEIN_CHANNELS_PER_VEIN = 2;

    // Lattice \\

    // Number of lattice rows covering every block from fromY to toY, starting at the row at or below fromY
    public static int computeLatticeRows(int fromY, int toY) {
        return (alignDown(toY) - alignDown(fromY)) / LATTICE_STEP + 2;
    }

    public static int alignDown(int worldY) {
        return Math.floorDiv(worldY, LATTICE_STEP) * LATTICE_STEP;
    }

    // The sea cave channel is only sampled where a chunk can hold sea caves at all
    public static void fillCaveLattice(
            long seed,
            double worldWidthBlocks, double worldHeightBlocks,
            long originX, int originY, long originZ,
            int rows,
            boolean seaChannel,
            float[] outLattice) {

        for (int row = 0; row < rows; row++) {
            for (int z = 0; z < LATTICE_SIDE; z++) {
                for (int x = 0; x < LATTICE_SIDE; x++) {

                    double worldX = originX + (long) x * LATTICE_STEP;
                    double worldY = originY + row * LATTICE_STEP;
                    double worldZ = originZ + (long) z * LATTICE_STEP;
                    int base = latticeIndex(x, row, z) * CAVE_CHANNELS;

                    outLattice[base + CHANNEL_TUNNEL_A] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_TUNNEL_SEED_SALT_A,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_TUNNEL_WAVELENGTH_BLOCKS, EngineSetting.CAVE_TUNNEL_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_TUNNEL_B] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_TUNNEL_SEED_SALT_B,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_TUNNEL_WAVELENGTH_BLOCKS, EngineSetting.CAVE_TUNNEL_VERTICAL_SCALE);

                    outLattice[base + CHANNEL_CAVERN] = TerrainNoiseUtility.sampleVolume(
                            seed ^ EngineSetting.CAVE_CAVERN_SEED_SALT,
                            worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                            EngineSetting.CAVE_CAVERN_WAVELENGTH_BLOCKS, EngineSetting.CAVE_CAVERN_VERTICAL_SCALE);

                    if (seaChannel)
                        outLattice[base + CHANNEL_SEA] = TerrainNoiseUtility.sampleVolume(
                                seed ^ EngineSetting.CAVE_SEA_SEED_SALT,
                                worldX, worldY, worldZ, worldWidthBlocks, worldHeightBlocks,
                                EngineSetting.CAVE_SEA_WAVELENGTH_BLOCKS, EngineSetting.CAVE_SEA_VERTICAL_SCALE);
                }
            }
        }
    }

    public static void fillVeinLattice(
            long seed,
            double worldWidthBlocks, double worldHeightBlocks,
            long originX, int originY, long originZ,
            int rows,
            long[] veinSeeds,
            int veinCount,
            float[] outLattice) {

        int channels = veinCount * VEIN_CHANNELS_PER_VEIN;

        for (int row = 0; row < rows; row++) {
            for (int z = 0; z < LATTICE_SIDE; z++) {
                for (int x = 0; x < LATTICE_SIDE; x++) {

                    double worldX = originX + (long) x * LATTICE_STEP;
                    double worldY = originY + row * LATTICE_STEP;
                    double worldZ = originZ + (long) z * LATTICE_STEP;
                    int base = latticeIndex(x, row, z) * channels;

                    for (int vein = 0; vein < veinCount; vein++) {

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

    // localY is measured from the lattice's first row
    public static float sampleLattice(
            float[] lattice,
            int channels,
            int channel,
            int localX,
            int localY,
            int localZ) {

        int cellX = Math.min(localX / LATTICE_STEP, LATTICE_SIDE - 2);
        int cellY = localY / LATTICE_STEP;
        int cellZ = Math.min(localZ / LATTICE_STEP, LATTICE_SIDE - 2);

        float tx = (localX - cellX * LATTICE_STEP) / (float) LATTICE_STEP;
        float ty = (localY - cellY * LATTICE_STEP) / (float) LATTICE_STEP;
        float tz = (localZ - cellZ * LATTICE_STEP) / (float) LATTICE_STEP;

        float v000 = lattice[latticeIndex(cellX, cellY, cellZ) * channels + channel];
        float v100 = lattice[latticeIndex(cellX + 1, cellY, cellZ) * channels + channel];
        float v010 = lattice[latticeIndex(cellX, cellY + 1, cellZ) * channels + channel];
        float v110 = lattice[latticeIndex(cellX + 1, cellY + 1, cellZ) * channels + channel];
        float v001 = lattice[latticeIndex(cellX, cellY, cellZ + 1) * channels + channel];
        float v101 = lattice[latticeIndex(cellX + 1, cellY, cellZ + 1) * channels + channel];
        float v011 = lattice[latticeIndex(cellX, cellY + 1, cellZ + 1) * channels + channel];
        float v111 = lattice[latticeIndex(cellX + 1, cellY + 1, cellZ + 1) * channels + channel];

        float near = lerp(lerp(v000, v100, tx), lerp(v010, v110, tx), ty);
        float far = lerp(lerp(v001, v101, tx), lerp(v011, v111, tx), ty);

        return lerp(near, far, tz);
    }

    private static int latticeIndex(int x, int row, int z) {
        return (row * LATTICE_SIDE + z) * LATTICE_SIDE + x;
    }

    // Caves \\

    // Tunnels and caverns narrow toward the floor of their band, so they close off rather than end on a flat cut
    public static float computeFloorFade(int worldY, int floorY) {
        return Math.min(1f, (worldY - floorY + 1) / (float) EngineSetting.CAVE_FLOOR_FADE_BLOCKS);
    }

    // Any tunnels at all run wide enough to walk, widening toward the maximum as tunnels rises
    public static boolean isTunnel(float tunnelA, float tunnelB, float tunnels, float fade) {

        if (tunnels <= 0f)
            return false;

        float radius = (EngineSetting.CAVE_TUNNEL_MIN_RADIUS
                + (EngineSetting.CAVE_TUNNEL_MAX_RADIUS - EngineSetting.CAVE_TUNNEL_MIN_RADIUS) * Math.min(tunnels, 1f))
                * fade;

        return tunnelA * tunnelA + tunnelB * tunnelB < radius * radius;
    }

    public static boolean isCavern(float cavern, float caverns, float fade) {

        if (caverns <= 0f || fade <= 0f)
            return false;

        float threshold = EngineSetting.CAVE_CAVERN_THRESHOLD_MAX
                - (EngineSetting.CAVE_CAVERN_THRESHOLD_MAX - EngineSetting.CAVE_CAVERN_THRESHOLD_MIN)
                        * Math.min(caverns, 1f);

        return cavern > threshold + (1f - threshold) * (1f - fade);
    }

    // Sea \\

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

    // Math \\

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
