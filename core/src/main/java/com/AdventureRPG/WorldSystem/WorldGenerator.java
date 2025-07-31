package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Biomes.Biome;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.Util.OpenSimplex2;

public class WorldGenerator {

    // Region System
    public final Settings settings;
    public final UserData UserData;
    public final WorldSystem WorldSystem;
    public final BiomeSystem BiomeSystem;

    // Settings

    private final int CHUNK_SIZE;

    private final int BASE_WORLD_ELEVATION;
    private final int MIN_WORLD_ELEVATION;
    private final int MAX_WORLD_ELEVATION;

    private final int BASE_ELEVATION_BLENDING;

    private final int BASE_OCEAN_LEVEL;

    private final int WATER_NOISE_OFFSET;
    private final int WATER_HEIGHT_OFFSET;

    private final int MIN_CAVE_ELEVATION;

    // Data
    private long Seed;

    private final Block AIR_BLOCK;
    private final Block VACUUM_BLOCK;
    private final Block WATER_BLOCK;
    private final Block LAVA_BLOCK;

    public WorldGenerator(WorldSystem WorldSystem) {

        // Region System
        this.settings = WorldSystem.settings;
        this.UserData = WorldSystem.SaveSystem.UserData;
        this.WorldSystem = WorldSystem;
        this.BiomeSystem = WorldSystem.BiomeSystem;

        // Settings

        this.CHUNK_SIZE = settings.CHUNK_SIZE;

        this.BASE_WORLD_ELEVATION = settings.BASE_WORLD_ELEVATION;
        this.MIN_WORLD_ELEVATION = settings.MIN_WORLD_ELEVATION;
        this.MAX_WORLD_ELEVATION = settings.MAX_WORLD_ELEVATION;

        this.BASE_ELEVATION_BLENDING = settings.BASE_ELEVATION_BLENDING;

        this.BASE_OCEAN_LEVEL = settings.BASE_OCEAN_LEVEL;

        this.WATER_NOISE_OFFSET = settings.WATER_NOISE_OFFSET;
        this.WATER_HEIGHT_OFFSET = settings.WATER_HEIGHT_OFFSET;

        this.MIN_CAVE_ELEVATION = settings.MIN_CAVE_ELEVATION;

        // Data
        this.Seed = UserData.Seed();

        this.AIR_BLOCK = WorldSystem.getBlockByName("air");
        this.VACUUM_BLOCK = WorldSystem.getBlockByName("vacuum");
        this.WATER_BLOCK = WorldSystem.getBlockByName("water");
        this.LAVA_BLOCK = WorldSystem.getBlockByName("lava");
    }

    // Data

    public long GetSeed() {
        return Seed;
    }

    public void SetSeed(long Seed) {
        this.Seed = Seed;
    }

    // Generator

    // Chunk

    public Chunk GenerateChunk(Vector3Int position) {

        WorldRegion WorldRegion = WorldSystem.WorldReader.WorldRegionFromPosition(position);
        Chunk chunk = new Chunk(position.x, position.y, position.z, CHUNK_SIZE);

        Block[][][] blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {

                    Vector3Int blockPos = new Vector3Int(x, y, z);
                    blockPos = blockPos.add(position);

                    Block block = GenerateBlock(WorldRegion, blockPos);
                    blocks[x][y][z] = block;
                }
            }
        }

        chunk.Generate(blocks);

        return chunk;
    }

    // Block

    Biome biome = new Biome();
    private int elevation;

    private Block GenerateBlock(WorldRegion region, Vector3Int position) {

        int x = position.x;
        int y = position.y;
        int z = position.z;

        if (y < MIN_WORLD_ELEVATION)
            return LAVA_BLOCK;
        if (y > MAX_WORLD_ELEVATION)
            return VACUUM_BLOCK;

        biome = GetBiomeAtPosition(region, position);

        // Ocean biome below ocean level fills with water
        if (biome.ocean && y < BASE_OCEAN_LEVEL) {
            return WATER_BLOCK;
        }

        // Water noise for variation
        float waterNoiseScaleX = biome.waterNoiseScaleX * x;
        float waterNoiseScaleY = biome.waterNoiseScaleY * z;
        float waterNoise = OpenSimplex2.noise2(Seed + WATER_NOISE_OFFSET, waterNoiseScaleX, waterNoiseScaleY);
        float waterThreshold = biome.waterThreshold;

        // Water heightmap decoupled from sea level
        float waterHeightNorm = (OpenSimplex2.noise2(Seed + WATER_HEIGHT_OFFSET, x * 0.008f, z * 0.008f) + 1f) / 2f;
        int localWaterHeight = (int) (MIN_WORLD_ELEVATION
                + waterHeightNorm * (MAX_WORLD_ELEVATION - MIN_WORLD_ELEVATION));

        if (!biome.ocean && biome.aquatic && y <= localWaterHeight && waterNoise > waterThreshold) {
            return WATER_BLOCK;
        }

        // Caves - only apply if under cave elevation and under terrain height
        if (y > MIN_CAVE_ELEVATION && y < elevation && biome.allowCaves) {
            float caveNoise = OpenSimplex2.noise3_ImproveXZ(
                    Seed + 9999,
                    x * biome.caveNoiseScaleX,
                    y * biome.caveNoiseScaleY,
                    z * biome.caveNoiseScaleZ);

            if (biome.isCave(caveNoise)) {
                return biome.aquatic ? WATER_BLOCK : AIR_BLOCK;
            }
        }

        // Surface cave entrance holes
        float surfaceBreakNoise = OpenSimplex2.noise2(Seed + 2222, x * 0.015f, z * 0.015f);

        if (biome.allowSurfaceBreak && y == elevation && surfaceBreakNoise > 0.75f) {
            return AIR_BLOCK; // or a special block like CAVE_ENTRANCE_BLOCK
        }

        // Terrain
        return WorldSystem.getBlockByID(biome.getBlockForElevation(y, MIN_WORLD_ELEVATION, elevation));
    }

    // Biome

    private Biome GetBiomeAtPosition(WorldRegion region, Vector3Int position) {
        Biome baseBiome = BiomeSystem.GetBiomeByID(region.regionID);

        elevation = BlendedElevation(region, baseBiome);
        boolean isUnderground = position.y < elevation;

        float blendNoise = OpenSimplex2.noise2(Seed + 4321, position.x * 0.002, position.z * 0.002);
        float blendValue = (blendNoise + 1f) / 2f;

        int[] related = isUnderground
                ? BiomeSystem.getRelatedSubTerrainianBiomes(baseBiome.ID)
                : BiomeSystem.getRelatedSurfaceBiomes(baseBiome.ID);

        if (related == null || related.length == 0)
            return baseBiome;

        int index = (int) (blendValue * related.length);
        index = Math.min(index, related.length - 1); // prevent overflow

        return BiomeSystem.GetBiomeByID(related[index]);
    }

    // Elevation

    private int BlendedElevation(WorldRegion region, Biome biome) {

        int imageElevation = region.elevation; // from 0–255
        int biomeElevation = biome.elevation;
        int biomeBlend = biome.elevationBlending;

        int blendedElevation = imageElevation * biomeElevation / (biomeBlend * BASE_ELEVATION_BLENDING);

        return (BASE_WORLD_ELEVATION + blendedElevation);
    }

}
