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
     * Persistent biome record, including every response curve
     * WorldGenerationManager needs to shape this biome's own terrain — a
     * continentalness-to-height spline, an erosion-to-amplitude spline, a
     * peaks-valleys ridge-contribution spline, a small-scale detail
     * amplitude/wavelength pair, an overall height-scale multiplier, and
     * oceanWater — whether this biome is permitted to flood its
     * below-sea-level terrain with water at all — and beachBiomeName, the
     * buffer biome BiomeManager inserts between this biome and any ocean it
     * borders. Each resolved once at load time in BiomeBuilder and defaulting
     * to TerrainShapeUtility's global curves (or false, or no beach) when a
     * biome's JSON omits them.
     */

    public static final int MAP_COLOR_UNDEFINED = EngineSetting.BIOME_MAP_COLOR_UNDEFINED;

    private final String biomeName;
    private final short biomeID;

    private final Color biomeColor;

    private final Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames;
    private final Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances;
    private final ObjectArrayList<String> seasonNames;

    private final int mapColor;
    private final ObjectArrayList<String> probableBiomeNames;
    private final FloatArrayList probableBiomeChances;

    private final String surfaceBlockName;
    private final String subsurfaceBlockName;
    private final String underwaterBlockName;

    private final LinearSpline continentalnessSpline;
    private final LinearSpline erosionSpline;
    private final LinearSpline peaksValleysSpline;
    private final float detailAmplitudeBlocks;
    private final float detailWavelengthBlocks;
    private final float terrainHeightScale;

    private final boolean oceanWater;
    private final String beachBiomeName;

    public BiomeData(
            String biomeName,
            short biomeID,
            Color biomeColor,
            Object2ObjectOpenHashMap<String, ObjectArrayList<String>> seasonWeatherNames,
            Object2ObjectOpenHashMap<String, FloatArrayList> seasonWeatherChances,
            ObjectArrayList<String> seasonNames,
            int mapColor,
            ObjectArrayList<String> probableBiomeNames,
            FloatArrayList probableBiomeChances,
            String surfaceBlockName,
            String subsurfaceBlockName,
            String underwaterBlockName,
            LinearSpline continentalnessSpline,
            LinearSpline erosionSpline,
            LinearSpline peaksValleysSpline,
            float detailAmplitudeBlocks,
            float detailWavelengthBlocks,
            float terrainHeightScale,
            boolean oceanWater,
            String beachBiomeName) {

        this.biomeName = biomeName;
        this.biomeID = biomeID;

        this.biomeColor = biomeColor;

        this.seasonWeatherNames = seasonWeatherNames;
        this.seasonWeatherChances = seasonWeatherChances;
        this.seasonNames = seasonNames;

        this.mapColor = mapColor;
        this.probableBiomeNames = probableBiomeNames;
        this.probableBiomeChances = probableBiomeChances;

        this.surfaceBlockName = surfaceBlockName;
        this.subsurfaceBlockName = subsurfaceBlockName;
        this.underwaterBlockName = underwaterBlockName;

        this.continentalnessSpline = continentalnessSpline;
        this.erosionSpline = erosionSpline;
        this.peaksValleysSpline = peaksValleysSpline;
        this.detailAmplitudeBlocks = detailAmplitudeBlocks;
        this.detailWavelengthBlocks = detailWavelengthBlocks;
        this.terrainHeightScale = terrainHeightScale;

        this.oceanWater = oceanWater;
        this.beachBiomeName = beachBiomeName;
    }

    public String getBiomeName() {
        return biomeName;
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

    public ObjectArrayList<String> getProbableBiomeNames() {
        return probableBiomeNames;
    }

    public FloatArrayList getProbableBiomeChances() {
        return probableBiomeChances;
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

    public boolean hasOceanWater() {
        return oceanWater;
    }

    public String getBeachBiomeName() {
        return beachBiomeName;
    }

    public boolean hasBeachBiome() {
        return beachBiomeName != null;
    }
}