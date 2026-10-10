package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.biome.BiomeBlendStruct;
import application.bootstrap.worldpipeline.biome.BiomeCaveStruct;
import application.bootstrap.worldpipeline.biome.BiomeCliffStruct;
import application.bootstrap.worldpipeline.biome.BiomeCoastStruct;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.BiomeRidgeStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.LinearSpline;

public final class TerrainShapeUtility extends EngineUtility {

    /*
     * Combines the continentalness, erosion and peaks-valleys noise fields with
     * the biome field to produce ground height. The noise is identical
     * everywhere; only each biome's curves differ, weighted by its share at the
     * position, plus blended small-scale detail and ridged layers. The ground
     * is then shaped in a fixed order: cliff terraces break its smooth macro
     * slopes into ledges and sheer risers before ridges are laid over them,
     * sea cliffs lift sheer from that broad shoreline wherever the sea is near
     * and plunge into deep water at their toe, fine detail roughens it all,
     * and still water floods the ground below its level within its reach and
     * holds a rim around it. Coverage masks decide where cliffs and sea cliffs
     * stand, so they come and go along a range or a coastline.
     */

    public static final LinearSpline DEFAULT_CONTINENTALNESS_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_X,
            EngineSetting.TERRAIN_CONTINENTALNESS_SPLINE_HEIGHT_BLOCKS);

    public static final LinearSpline DEFAULT_EROSION_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_EROSION_SPLINE_X,
            EngineSetting.TERRAIN_EROSION_SPLINE_AMPLITUDE_BLOCKS);

    public static final LinearSpline DEFAULT_PEAKS_VALLEYS_SPLINE = new LinearSpline(
            EngineSetting.TERRAIN_PV_SPLINE_X,
            EngineSetting.TERRAIN_PV_SPLINE_CONTRIBUTION);

    // Macro Shape \\

    public static float computeMacroShapeBlocks(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            BiomeBlendStruct blend) {

        if (blend.isEmpty())
            return EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);

        float continentalness = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_CONTINENTALNESS_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_CONTINENTALNESS_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_CONTINENTALNESS_OCTAVES,
                EngineSetting.TERRAIN_CONTINENTALNESS_PERSISTENCE,
                EngineSetting.TERRAIN_CONTINENTALNESS_LACUNARITY);

        float erosion = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_EROSION_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_EROSION_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_EROSION_OCTAVES,
                EngineSetting.TERRAIN_EROSION_PERSISTENCE,
                EngineSetting.TERRAIN_EROSION_LACUNARITY);

        float peaksValleysRaw = TerrainNoiseUtility.sampleFractal(
                seed ^ EngineSetting.TERRAIN_PV_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_PV_WAVELENGTH_BLOCKS,
                EngineSetting.TERRAIN_PV_OCTAVES,
                EngineSetting.TERRAIN_PV_PERSISTENCE,
                EngineSetting.TERRAIN_PV_LACUNARITY);

        float ridge = 1f - Math.abs(peaksValleysRaw);

        float blendedHeight = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            float baseHeight = biome.getContinentalnessSpline().evaluate(continentalness);
            float erosionAmplitude = biome.getErosionSpline().evaluate(erosion);
            float peaksValleysContribution = biome.getPeaksValleysSpline().evaluate(ridge)
                    * erosionAmplitude;

            float heightAboveSeaLevel = (baseHeight - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS)
                    + peaksValleysContribution;

            float biomeHeight = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS
                    + heightAboveSeaLevel * biome.getTerrainHeightScale();

            blendedHeight += biomeHeight * blend.getWeight(i);
        }

        return blendedHeight;
    }

    // Features \\

    public static void resolveFeatures(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            BiomeBlendStruct blend,
            TerrainFeatureStruct outFeatures) {

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);

        outFeatures.coastalWeight = blend.isEmpty() ? 0f : blend.getCoastalWeight();
        outFeatures.oceanWeight = blend.isEmpty() ? 0f : blend.getOceanWeight();
        outFeatures.clearLayers();

        resolveDetail(blend, outFeatures);
        resolveRidges(blend, outFeatures);
        resolveCliffs(seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks, blend, outFeatures);
        resolveCoast(seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks, blend, outFeatures);
        resolveCaves(blend, outFeatures);
        resolveLake(blend, outFeatures);
    }

    private static void resolveDetail(BiomeBlendStruct blend, TerrainFeatureStruct outFeatures) {

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            outFeatures.addDetailLayer(
                    biome.getDetailWavelengthBlocks(),
                    biome.getDetailAmplitudeBlocks() * biome.getTerrainHeightScale() * blend.getWeight(i));
        }
    }

    private static void resolveRidges(BiomeBlendStruct blend, TerrainFeatureStruct outFeatures) {

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);
            BiomeRidgeStruct ridges = biome.getRidges();

            outFeatures.addRidgeLayer(
                    ridges.getWavelengthBlocks(),
                    ridges.getAmplitudeBlocks() * biome.getTerrainHeightScale() * blend.getWeight(i));
        }
    }

    private static void resolveCliffs(
            long seed,
            double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            BiomeBlendStruct blend,
            TerrainFeatureStruct outFeatures) {

        float strength = 0f;
        float coverage = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeCliffStruct cliffs = blend.getBiome(i).getCliffs();

            if (!cliffs.hasCliffs())
                continue;

            float share = cliffs.getStrength() * blend.getWeight(i);

            strength += share;
            coverage += cliffs.getCoverage() * share;
            outFeatures.addCliffLayer(cliffs.getStepBlocks(), share);
        }

        if (strength <= 0f)
            return;

        float mask = computeCoverageMask(
                seed ^ EngineSetting.TERRAIN_CLIFF_MASK_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_CLIFF_MASK_WAVELENGTH_BLOCKS,
                coverage / strength,
                EngineSetting.TERRAIN_CLIFF_MASK_EDGE);

        for (int i = 0; i < outFeatures.cliffLayerCount; i++)
            outFeatures.cliffStrength[i] *= mask;
    }

    // Only land shapes its shore, measured by the weight it holds itself rather than its shore buffer's
    private static void resolveCoast(
            long seed,
            double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            BiomeBlendStruct blend,
            TerrainFeatureStruct outFeatures) {

        float landWeight = 0f;
        float height = 0f;
        float coverage = 0f;
        float overhang = 0f;
        float seaCaves = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            if (biome.hasOceanWater())
                continue;

            BiomeCoastStruct coast = biome.getCoast();
            float weight = blend.getNaturalWeight(i);

            landWeight += weight;
            height += coast.getCliffHeightBlocks() * weight;
            coverage += coast.getCoverage() * weight;
            overhang += coast.getOverhangBlocks() * weight;
            seaCaves += coast.getSeaCaves() * weight;
        }

        if (landWeight <= 0f || height <= 0f) {
            outFeatures.coastCliffHeightBlocks = 0f;
            outFeatures.coastOverhangBlocks = 0f;
            outFeatures.seaCaves = 0f;
            return;
        }

        float mask = computeCoverageMask(
                seed ^ EngineSetting.TERRAIN_COAST_MASK_SEED_SALT,
                cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                EngineSetting.TERRAIN_COAST_MASK_WAVELENGTH_BLOCKS,
                coverage / landWeight,
                EngineSetting.TERRAIN_COAST_MASK_EDGE);

        outFeatures.coastCliffHeightBlocks = height / landWeight * mask;
        outFeatures.coastOverhangBlocks = overhang / landWeight * mask;
        outFeatures.seaCaves = seaCaves / landWeight * mask;
    }

    private static void resolveCaves(BiomeBlendStruct blend, TerrainFeatureStruct outFeatures) {

        float tunnels = 0f;
        float caverns = 0f;
        float noodles = 0f;
        float lakes = 0f;
        float minHeight = 0f;
        float maxDepth = 0f;
        float entrances = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeCaveStruct caves = blend.getBiome(i).getCaves();
            float weight = blend.getWeight(i);

            tunnels += caves.getTunnels() * weight;
            caverns += caves.getCaverns() * weight;
            noodles += caves.getNoodles() * weight;
            lakes += caves.getLakes() * weight;
            minHeight += caves.getMinHeightBlocks() * weight;
            maxDepth += caves.getMaxDepthBlocks() * weight;
            entrances += caves.hasEntrances() ? weight : 0f;
        }

        outFeatures.caveTunnels = tunnels;
        outFeatures.caveCaverns = caverns;
        outFeatures.caveNoodles = noodles;
        outFeatures.caveLakes = lakes;
        outFeatures.caveMinHeightBlocks = minHeight;
        outFeatures.caveMaxDepthBlocks = maxDepth;
        outFeatures.caveEntrances = entrances;
    }

    // Biomes sharing a level are one body of water, so the strongest level wins with the weight of every biome on it
    private static void resolveLake(BiomeBlendStruct blend, TerrainFeatureStruct outFeatures) {

        int bestLevel = TerrainFeatureStruct.LAKE_LEVEL_UNDEFINED;
        float bestWeight = 0f;

        for (int i = 0; i < blend.getCount(); i++) {

            BiomeHandle biome = blend.getBiome(i);

            if (!biome.hasLakeWater())
                continue;

            int level = biome.getWaterLevelBlocks();
            float levelWeight = 0f;

            for (int j = 0; j < blend.getCount(); j++)
                if (blend.getBiome(j).getWaterLevelBlocks() == level)
                    levelWeight += blend.getWeight(j);

            if (levelWeight > bestWeight) {
                bestWeight = levelWeight;
                bestLevel = level;
            }
        }

        outFeatures.lakeLevelBlocks = bestLevel;
        outFeatures.lakeWeight = bestWeight;
    }

    // 1 where the mask stands, 0 where it does not, with the share of the area it covers following coverage
    private static float computeCoverageMask(
            long seed,
            double cosAngle, double sinAngle, double worldZ,
            double worldWidthBlocks, double worldHeightBlocks,
            double wavelengthBlocks,
            float coverage,
            float edge) {

        if (coverage <= 0f)
            return 0f;

        if (coverage >= 1f)
            return 1f;

        float noise = TerrainNoiseUtility.sampleFractal(
                seed, cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                wavelengthBlocks,
                EngineSetting.TERRAIN_FEATURE_MASK_OCTAVES,
                EngineSetting.TERRAIN_FEATURE_MASK_PERSISTENCE,
                EngineSetting.TERRAIN_FEATURE_MASK_LACUNARITY);

        float threshold = (1f - 2f * coverage) * EngineSetting.TERRAIN_FEATURE_MASK_SPAN;

        return smoothstep((noise - threshold) / edge + 0.5f);
    }

    // Detail \\

    public static float computeDetailBlocks(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            TerrainFeatureStruct features) {

        if (features.detailLayerCount == 0)
            return 0f;

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);
        float detail = 0f;

        for (int i = 0; i < features.detailLayerCount; i++)
            detail += TerrainNoiseUtility.sampleFractal(
                    seed ^ EngineSetting.TERRAIN_DETAIL_SEED_SALT,
                    cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                    features.detailWavelengthBlocks[i],
                    EngineSetting.TERRAIN_DETAIL_OCTAVES,
                    EngineSetting.TERRAIN_DETAIL_PERSISTENCE,
                    EngineSetting.TERRAIN_DETAIL_LACUNARITY)
                    * features.detailAmplitudeBlocks[i];

        return detail;
    }

    // Centered on the ridged layer's typical value, so ridges carve gullies as much as they raise spines
    public static float computeRidgeBlocks(
            long seed, double worldX, double worldZ, double worldWidthBlocks, double worldHeightBlocks,
            TerrainFeatureStruct features) {

        if (features.ridgeLayerCount == 0)
            return 0f;

        double spatialAngle = (worldX / worldWidthBlocks) * (Math.PI * 2.0);
        double cosAngle = Math.cos(spatialAngle);
        double sinAngle = Math.sin(spatialAngle);
        float ridges = 0f;

        for (int i = 0; i < features.ridgeLayerCount; i++)
            ridges += (TerrainNoiseUtility.sampleRidged(
                    seed ^ EngineSetting.TERRAIN_RIDGE_SEED_SALT,
                    cosAngle, sinAngle, worldZ, worldWidthBlocks, worldHeightBlocks,
                    features.ridgeWavelengthBlocks[i],
                    EngineSetting.TERRAIN_RIDGE_OCTAVES,
                    EngineSetting.TERRAIN_RIDGE_GAIN,
                    EngineSetting.TERRAIN_RIDGE_LACUNARITY)
                    - EngineSetting.TERRAIN_RIDGE_BIAS) * features.ridgeAmplitudeBlocks[i];

        return ridges;
    }

    // Shape \\

    // Cliffs terrace the smooth macro shape alone and sea cliffs rise from it with its ridges, so the fine detail
    // laid over them afterwards never jumps a whole step or flickers a cliff on and off along the shore
    public static float shapeGroundHeightBlocks(
            float macroShapeBlocks,
            float ridgeBlocks,
            float detailBlocks,
            TerrainFeatureStruct features) {

        float height = applyCliffs(macroShapeBlocks, features) + ridgeBlocks;
        height = applyCoast(height, features) + detailBlocks;
        height = applyLake(height, features);

        return Math.max(
                EngineSetting.TERRAIN_MIN_HEIGHT_BLOCKS,
                Math.min(EngineSetting.TERRAIN_MAX_HEIGHT_BLOCKS, height));
    }

    public static int finalizeGroundHeightBlocks(float shapedHeightBlocks) {
        return Math.round(shapedHeightBlocks);
    }

    // Each layer pulls the ground toward its own terraces, so layers of different steps blend their ledges
    private static float applyCliffs(float heightBlocks, TerrainFeatureStruct features) {

        float height = heightBlocks;

        for (int i = 0; i < features.cliffLayerCount; i++)
            height += (terrace(heightBlocks, features.cliffStepBlocks[i]) - heightBlocks)
                    * Math.min(features.cliffStrength[i], 1f);

        return height;
    }

    // Each step holds a flat ledge, then rises over a narrow riser into the next
    private static float terrace(float heightBlocks, float stepBlocks) {

        float steps = heightBlocks / stepBlocks;
        float floor = (float) Math.floor(steps);
        float riserFraction = EngineSetting.TERRAIN_CLIFF_RISER_FRACTION;
        float riser = smoothstep((steps - floor - (1f - riserFraction)) / riserFraction);

        return (floor + riser) * stepBlocks;
    }

    // Near the sea, ground rising out of the water lifts sheer to the cliff top and ground just below it drops to a
    // toe that stays under water even at low tide, so cliffs meet the sea at every tide, follow the natural
    // shoreline and fade inland as the sea's share falls away
    private static float applyCoast(float heightBlocks, TerrainFeatureStruct features) {

        float cliffHeight = features.coastCliffHeightBlocks;

        if (cliffHeight < EngineSetting.TERRAIN_COAST_CLIFF_MIN_HEIGHT_BLOCKS)
            return heightBlocks;

        float zone = smoothstep(features.oceanWeight / EngineSetting.TERRAIN_COAST_ZONE_OCEAN_WEIGHT);

        if (zone <= 0f)
            return heightBlocks;

        float seaLevel = EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS;

        if (heightBlocks >= seaLevel) {

            float aboveSea = heightBlocks - seaLevel;
            float lift = cliffHeight * zone * smoothstep(aboveSea / EngineSetting.TERRAIN_COAST_CLIFF_RISE_BLOCKS);

            return Math.max(heightBlocks, seaLevel + lift + aboveSea * EngineSetting.TERRAIN_COAST_PLATEAU_RELIEF);
        }

        if (!isOceanReached(features))
            return heightBlocks;

        float drop = smoothstep((seaLevel - heightBlocks) / EngineSetting.TERRAIN_COAST_TOE_DROP_BLOCKS);
        float toeFloor = TideUtility.getTopWaterY(TideUtility.MIN_SURFACE_LEVELS)
                - EngineSetting.TERRAIN_COAST_TOE_CLEARANCE_BLOCKS;

        return Math.min(heightBlocks, seaLevel - 1f - (seaLevel - 1f - toeFloor) * zone * drop);
    }

    // Within the water's reach, ground below its level becomes its bed and sinks toward its heart, while ground
    // above it stays shore; around that reach the ground is held up into a rim that contains it
    private static float applyLake(float heightBlocks, TerrainFeatureStruct features) {

        if (!features.hasLake() || isOceanReached(features))
            return heightBlocks;

        int level = features.lakeLevelBlocks;
        float weight = features.lakeWeight;

        if (weight >= EngineSetting.LAKE_SHORE_WEIGHT) {

            if (heightBlocks >= level)
                return heightBlocks;

            return Math.min(heightBlocks, level - 1f
                    - (weight - EngineSetting.LAKE_SHORE_WEIGHT) * EngineSetting.LAKE_BED_DEPTH_PER_WEIGHT);
        }

        float rim = level + EngineSetting.LAKE_RIM_BLOCKS;

        if (heightBlocks >= rim)
            return heightBlocks;

        return heightBlocks + (rim - heightBlocks) * smoothstep(weight / EngineSetting.LAKE_RIM_FULL_WEIGHT);
    }

    // Water \\

    public static boolean isOceanReached(TerrainFeatureStruct features) {
        return features.coastalWeight > EngineSetting.OCEAN_REACH_THRESHOLD;
    }

    public static boolean isLakeCovered(TerrainFeatureStruct features, int groundHeightBlocks) {
        return features.hasLake()
                && features.lakeWeight >= EngineSetting.LAKE_SHORE_WEIGHT
                && groundHeightBlocks < features.lakeLevelBlocks
                && !isOceanReached(features);
    }

    // Blocks inland from the natural shoreline, read from the unshaped ground and its slope, negative out to sea
    public static float computeShoreDistanceBlocks(float rawHeightBlocks, float rawSlope) {
        return (rawHeightBlocks - EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS)
                / Math.max(rawSlope, EngineSetting.TERRAIN_COAST_MIN_SLOPE);
    }

    // Blocks inland from where a sea cliff has risen to its full height, negative on its face
    public static float computeCliffFaceDistanceBlocks(float rawHeightBlocks, float rawSlope) {
        return computeShoreDistanceBlocks(rawHeightBlocks - EngineSetting.TERRAIN_COAST_CLIFF_RISE_BLOCKS, rawSlope);
    }

    // Math \\

    private static float smoothstep(float t) {

        if (t <= 0f)
            return 0f;

        if (t >= 1f)
            return 1f;

        return t * t * (3f - 2f * t);
    }
}
