package com.AdventureRPG.WorldSystem;

import com.AdventureRPG.SaveSystem.UserData;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.Biomes.Biome;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Chunks.Chunk;
import com.AdventureRPG.WorldSystem.Chunks.ChunkSystem;
import com.AdventureRPG.Util.OpenSimplex2;

public class WorldGenerator {

    // Region System
    public final Settings settings;
    public final UserData userData;
    public final WorldSystem worldSystem;
    public final ChunkSystem chunkSystem;
    public final BiomeSystem biomeSystem;

    // Data
    private long Seed;

    private final Block VACUUM_BLOCK;
    private final Block LAVA_BLOCK;

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

    private final int CAVE_NOISE_OFFSET;
    private final int SURFACE_BREAK__OFFSET;

    private final int BIOME_BLEND_OFFSET;

    // Temp
    private Biome biome;
    private int elevation;

    // Base \\

    public WorldGenerator(WorldSystem worldSystem) {

        // Region System
        this.settings = worldSystem.settings;
        this.userData = worldSystem.saveSystem.userData;
        this.worldSystem = worldSystem;
        this.chunkSystem = worldSystem.chunkSystem;
        this.biomeSystem = worldSystem.biomeSystem;

        // Data
        this.Seed = userData.Seed();

        this.VACUUM_BLOCK = worldSystem.GetBlockByName("vacuum");
        this.LAVA_BLOCK = worldSystem.GetBlockByName("lava");

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

        this.CAVE_NOISE_OFFSET = settings.CAVE_NOISE_OFFSET;
        this.SURFACE_BREAK__OFFSET = settings.SURFACE_BREAK__OFFSET;

        this.BIOME_BLEND_OFFSET = settings.BIOME_BLEND_OFFSET;

        // Temp
        this.biome = new Biome();
    }

    // Data \\

    public long GetSeed() {
        return Seed;
    }

    public void SetSeed(long Seed) {
        this.Seed = Seed;
    }

    // Generator \\

    // Chunk \\

    public Chunk GenerateChunk(Vector3Int coordinate, Vector3Int position) {

        WorldRegion WorldRegion = worldSystem.worldReader.WorldRegionFromPosition(coordinate);
        Chunk chunk = new Chunk(coordinate, position, worldSystem);

        Block[][][] blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {

                    Vector3Int blockPos = new Vector3Int(x, y, z).add(coordinate);

                    Block block = GenerateBlock(WorldRegion, blockPos);

                    blocks[x][y][z] = block;
                }
            }
        }

        chunk.Generate(blocks);

        return chunk;
    }

    // Block \\

    private Block GenerateBlock(WorldRegion region, Vector3Int position) {

        int x = position.x;
        int y = position.y;
        int z = position.z;

        if (y < MIN_WORLD_ELEVATION)
            return LAVA_BLOCK;
        if (y > MAX_WORLD_ELEVATION)
            return VACUUM_BLOCK;

        biome = GenerateBiome(region, position);
        Block airBlock = worldSystem.GetBlockByID(biome.airBlock);
        Block waterBlock = worldSystem.GetBlockByID(biome.waterBlock);

        // Only fill water if above terrain and below sea level
        if (biome.ocean && y > elevation && y <= BASE_OCEAN_LEVEL)
            return waterBlock;

        // Fill air blocks above terrain
        else if (y > elevation)
            return airBlock;

        // Water noise for variation
        float waterNoiseScaleX = biome.waterNoiseScaleX * x;
        float waterNoiseScaleY = biome.waterNoiseScaleY * z;
        float waterThreshold = biome.waterThreshold;
        float waterNoise = OpenSimplex2.noise2(Seed + WATER_NOISE_OFFSET, waterNoiseScaleX, waterNoiseScaleY);

        // Water heightmap decoupled from sea level
        float waterHeightScaleX = biome.waterHeightScaleX * x;
        float waterHeightScaleY = biome.waterHeightScaleY * z;
        float waterHeightNoise = OpenSimplex2.noise2(Seed + WATER_HEIGHT_OFFSET, waterHeightScaleX, waterHeightScaleY);
        float waterHeighValue = (waterHeightNoise + 1f) / 2f;
        int localWaterHeight = (int) (MIN_WORLD_ELEVATION
                + waterHeighValue * (MAX_WORLD_ELEVATION - MIN_WORLD_ELEVATION));

        if (!biome.ocean && biome.aquatic && y <= localWaterHeight && waterNoise > waterThreshold)
            return waterBlock;

        // Caves - only apply if under cave elevation and under terrain height
        if (y > MIN_CAVE_ELEVATION && y < elevation && biome.allowCaves) {
            float caveNoise = OpenSimplex2.noise3_ImproveXZ(
                    Seed + CAVE_NOISE_OFFSET,
                    x * biome.caveNoiseScaleX,
                    y * biome.caveNoiseScaleY,
                    z * biome.caveNoiseScaleZ);

            if (biome.IsCave(caveNoise)) {
                return biome.aquatic ? waterBlock : airBlock;
            }
        }

        // Surface cave entrance holes
        float breakNoiseScaleX = biome.breakNoiseScaleX * x;
        float breakNoiseScaleY = biome.breakNoiseScaleY * z;
        float breakThreshold = biome.breakThreshold;
        float surfaceBreakNoise = OpenSimplex2.noise2(Seed + SURFACE_BREAK__OFFSET, breakNoiseScaleX, breakNoiseScaleY);

        if (biome.allowSurfaceBreak && y == elevation && surfaceBreakNoise > breakThreshold) {
            return airBlock;
        }

        // Terrain
        return worldSystem.GetBlockByID(biome.GetBlockForElevation(y, MIN_WORLD_ELEVATION, elevation));
    }

    // Biome \\

    private Biome GenerateBiome(WorldRegion region, Vector3Int position) {

        int x = position.x;
        int y = position.y;
        int z = position.z;

        Biome baseBiome = biomeSystem.GetBiomeByID(region.regionID);

        elevation = BlendedElevation(region, baseBiome, position);
        boolean isUnderground = y < elevation;

        float biomeBlendScaleX = baseBiome.biomeBlendScaleX * x;
        float biomeBlendScaleY = baseBiome.biomeBlendScaleY * z;
        float blendNoise = OpenSimplex2.noise2(Seed + BIOME_BLEND_OFFSET, biomeBlendScaleX, biomeBlendScaleY);
        float blendValue = (blendNoise + 1f) / 2f;

        int[] related = isUnderground
                ? biomeSystem.GetRelatedSubTerrainianBiomes(baseBiome.ID)
                : biomeSystem.GetRelatedSurfaceBiomes(baseBiome.ID);

        if (related == null || related.length == 0)
            return baseBiome;

        int index = (int) (blendValue * related.length);
        index = Math.min(index, related.length - 1);

        Biome generatedBiome = biomeSystem.GetBiomeByID(related[index]);
        elevation = BlendedElevation(region, generatedBiome, position);

        return generatedBiome;
    }

    // Elevation \\

    private int BlendedElevation(WorldRegion region, Biome biome, Vector3Int position) {
        int imageElevation = region.elevation; // from PNG
        int biomeElevation = biome.elevation; // biome bias
        int biomeBlend = biome.elevationBlending;

        // Base blended elevation from PNG + biome
        int blendedElevation = imageElevation * biomeElevation / (biomeBlend * BASE_ELEVATION_BLENDING);

        // === Noise variation ===
        // Low frequency for big hills/mountains
        float noiseScale = 0.0025f; // smooth variation across world
        float noiseValue = OpenSimplex2.noise2(
                Seed,
                position.x * noiseScale,
                position.z * noiseScale);

        // Map noise (-1..1) to vertical offset based on total world height range
        int noiseOffset = (int) (noiseValue * (MAX_WORLD_ELEVATION - MIN_WORLD_ELEVATION) * 0.05f);

        return BASE_WORLD_ELEVATION + blendedElevation + noiseOffset;
    }

}
