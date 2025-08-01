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
    public final UserData UserData;
    public final WorldSystem WorldSystem;
    public final BiomeSystem BiomeSystem;
    public final ChunkSystem ChunkSystem;

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

    public WorldGenerator(WorldSystem WorldSystem) {

        // Region System

        this.settings = WorldSystem.settings;
        this.UserData = WorldSystem.SaveSystem.UserData;
        this.WorldSystem = WorldSystem;
        this.BiomeSystem = WorldSystem.BiomeSystem;
        this.ChunkSystem = WorldSystem.ChunkSystem;

        // Data

        this.Seed = UserData.Seed();

        this.VACUUM_BLOCK = WorldSystem.getBlockByName("vacuum");
        this.LAVA_BLOCK = WorldSystem.getBlockByName("lava");

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
        Chunk chunk = new Chunk(position.x, position.y, position.z, ChunkSystem);

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
        Block airBlock = WorldSystem.getBlockByID(biome.airBlock);
        Block waterBlock = WorldSystem.getBlockByID(biome.waterBlock);

        // Ocean biome below ocean level fills with water
        if (biome.ocean && y < BASE_OCEAN_LEVEL) {
            return waterBlock;
        }

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

            if (biome.isCave(caveNoise)) {
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
        return WorldSystem.getBlockByID(biome.getBlockForElevation(y, MIN_WORLD_ELEVATION, elevation));
    }

    // Biome

    private Biome GetBiomeAtPosition(WorldRegion region, Vector3Int position) {

        int x = position.x;
        int y = position.y;
        int z = position.z;

        Biome baseBiome = BiomeSystem.GetBiomeByID(region.regionID);

        elevation = BlendedElevation(region, baseBiome);
        boolean isUnderground = y < elevation;

        float biomeBlendScaleX = biome.biomeBlendScaleX * x;
        float biomeBlendScaleY = biome.biomeBlendScaleY * z;
        float blendNoise = OpenSimplex2.noise2(Seed + BIOME_BLEND_OFFSET, biomeBlendScaleX, biomeBlendScaleY);
        float blendValue = (blendNoise + 1f) / 2f;

        int[] related = isUnderground
                ? BiomeSystem.getRelatedSubTerrainianBiomes(baseBiome.ID)
                : BiomeSystem.getRelatedSurfaceBiomes(baseBiome.ID);

        if (related == null || related.length == 0)
            return baseBiome;

        int index = (int) (blendValue * related.length);
        index = Math.min(index, related.length - 1);

        return BiomeSystem.GetBiomeByID(related[index]);
    }

    // Elevation

    private int BlendedElevation(WorldRegion region, Biome biome) {

        int imageElevation = region.elevation;
        int biomeElevation = biome.elevation;
        int biomeBlend = biome.elevationBlending;

        int blendedElevation = imageElevation * biomeElevation / (biomeBlend * BASE_ELEVATION_BLENDING);

        return (BASE_WORLD_ELEVATION + blendedElevation);
    }

}
