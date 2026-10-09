package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.LinearSpline;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeData extends DataPackage {

    /*
     * Persistent biome record: registry and display names and every curve world
     * generation shapes this biome with — continentalness, erosion and
     * peaks-valleys splines, detail amplitude and wavelength, height scale —
     * its cliffs, ridges, coast, caves and veins, the rock its steep faces
     * bare and the slope they bare it from, plus its ocean flag, the level of
     * its own still water, its beach biome and the probable biomes chained
     * into it, the trees it grows and how it spreads them, and the
     * architectures settlements on it may be built in, none letting no
     * settlement grow there on its own. Omitted curves default to
     * TerrainShapeUtility's.
     */

    public static final int MAP_COLOR_UNDEFINED = EngineSetting.BIOME_MAP_COLOR_UNDEFINED;
    public static final int WATER_LEVEL_UNDEFINED = EngineSetting.LAKE_LEVEL_UNDEFINED;

    private final String biomeName;
    private final String displayName;
    private final short biomeID;

    private final Color biomeColor;

    private final Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames;
    private final Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances;
    private final ObjectArrayList<String> seasonNames;

    private final int mapColor;
    private final ObjectArrayList<ProbableBiomeStruct> probableBiomes;

    private final String surfaceBlockName;
    private final String subsurfaceBlockName;
    private final String underwaterBlockName;
    private final String rockBlockName;
    private final float rockSlope;

    private final LinearSpline continentalnessSpline;
    private final LinearSpline erosionSpline;
    private final LinearSpline peaksValleysSpline;
    private final float detailAmplitudeBlocks;
    private final float detailWavelengthBlocks;
    private final float terrainHeightScale;

    private final BiomeCliffStruct cliffs;
    private final BiomeRidgeStruct ridges;
    private final BiomeCoastStruct coast;
    private final BiomeCaveStruct caves;
    private final ObjectArrayList<BiomeVeinStruct> veins;
    private final ObjectArrayList<BiomeTreeStruct> trees;
    private final ObjectArrayList<String> architectureNames;

    private final boolean oceanWater;
    private final int waterLevelBlocks;
    private final String beachBiomeName;

    public BiomeData(
            String biomeName,
            String displayName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances,
            ObjectArrayList<String> seasonNames,
            int mapColor,
            ObjectArrayList<ProbableBiomeStruct> probableBiomes,
            String surfaceBlockName,
            String subsurfaceBlockName,
            String underwaterBlockName,
            String rockBlockName,
            float rockSlope,
            LinearSpline continentalnessSpline,
            LinearSpline erosionSpline,
            LinearSpline peaksValleysSpline,
            float detailAmplitudeBlocks,
            float detailWavelengthBlocks,
            float terrainHeightScale,
            BiomeCliffStruct cliffs,
            BiomeRidgeStruct ridges,
            BiomeCoastStruct coast,
            BiomeCaveStruct caves,
            ObjectArrayList<BiomeVeinStruct> veins,
            ObjectArrayList<BiomeTreeStruct> trees,
            ObjectArrayList<String> architectureNames,
            boolean oceanWater,
            int waterLevelBlocks,
            String beachBiomeName) {

        this.biomeName = biomeName;
        this.displayName = displayName;
        this.biomeID = biomeID;

        this.biomeColor = biomeColor;

        this.seasonWeatherNames = seasonWeatherNames;
        this.seasonWeatherChances = seasonWeatherChances;
        this.seasonNames = seasonNames;

        this.mapColor = mapColor;
        this.probableBiomes = probableBiomes;

        this.surfaceBlockName = surfaceBlockName;
        this.subsurfaceBlockName = subsurfaceBlockName;
        this.underwaterBlockName = underwaterBlockName;
        this.rockBlockName = rockBlockName;
        this.rockSlope = rockSlope;

        this.continentalnessSpline = continentalnessSpline;
        this.erosionSpline = erosionSpline;
        this.peaksValleysSpline = peaksValleysSpline;
        this.detailAmplitudeBlocks = detailAmplitudeBlocks;
        this.detailWavelengthBlocks = detailWavelengthBlocks;
        this.terrainHeightScale = terrainHeightScale;

        this.cliffs = cliffs;
        this.ridges = ridges;
        this.coast = coast;
        this.caves = caves;
        this.veins = veins;
        this.trees = trees;
        this.architectureNames = architectureNames;

        this.oceanWater = oceanWater;
        this.waterLevelBlocks = waterLevelBlocks;
        this.beachBiomeName = beachBiomeName;
    }

    public String getBiomeName() {
        return biomeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public short getBiomeID() {
        return biomeID;
    }

    public Color getBiomeColor() {
        return biomeColor;
    }

    public ObjectArrayList<String> getWeatherNamesForSeason(String seasonName) {
        return seasonWeatherNames.get(seasonName);
    }

    public FloatArrayList getWeatherChancesForSeason(String seasonName) {
        return seasonWeatherChances.get(seasonName);
    }

    public ObjectArrayList<String> getSeasonNames() {
        return seasonNames;
    }

    public int getMapColor() {
        return mapColor;
    }

    public boolean hasMapColor() {
        return mapColor != MAP_COLOR_UNDEFINED;
    }

    public ObjectArrayList<ProbableBiomeStruct> getProbableBiomes() {
        return probableBiomes;
    }

    public String getSurfaceBlockName() {
        return surfaceBlockName;
    }

    public String getSubsurfaceBlockName() {
        return subsurfaceBlockName;
    }

    public String getUnderwaterBlockName() {
        return underwaterBlockName;
    }

    public String getRockBlockName() {
        return rockBlockName;
    }

    public float getRockSlope() {
        return rockSlope;
    }

    public LinearSpline getContinentalnessSpline() {
        return continentalnessSpline;
    }

    public LinearSpline getErosionSpline() {
        return erosionSpline;
    }

    public LinearSpline getPeaksValleysSpline() {
        return peaksValleysSpline;
    }

    public float getDetailAmplitudeBlocks() {
        return detailAmplitudeBlocks;
    }

    public float getDetailWavelengthBlocks() {
        return detailWavelengthBlocks;
    }

    public float getTerrainHeightScale() {
        return terrainHeightScale;
    }

    public BiomeCliffStruct getCliffs() {
        return cliffs;
    }

    public BiomeRidgeStruct getRidges() {
        return ridges;
    }

    public BiomeCoastStruct getCoast() {
        return coast;
    }

    public BiomeCaveStruct getCaves() {
        return caves;
    }

    public ObjectArrayList<BiomeVeinStruct> getVeins() {
        return veins;
    }

    public ObjectArrayList<BiomeTreeStruct> getTrees() {
        return trees;
    }

    public ObjectArrayList<String> getArchitectureNames() {
        return architectureNames;
    }

    public boolean hasOceanWater() {
        return oceanWater;
    }

    public int getWaterLevelBlocks() {
        return waterLevelBlocks;
    }

    public boolean hasLakeWater() {
        return waterLevelBlocks != WATER_LEVEL_UNDEFINED;
    }

    public String getBeachBiomeName() {
        return beachBiomeName;
    }

    public boolean hasBeachBiome() {
        return beachBiomeName != null;
    }
}