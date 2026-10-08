package application.bootstrap.worldpipeline.biome;

import engine.graphics.color.Color;
import engine.root.HandlePackage;
import engine.util.mathematics.extras.LinearSpline;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BiomeHandle extends HandlePackage {

    /*
     * Persistent biome record. Wraps BiomeData and delegates all access through it,
     * including the
     * per-biome terrain response curves and detail parameters TerrainShapeUtility
     * evaluates during
     * generation.
     */

    private static final ObjectArrayList<String> EMPTY_NAMES = new ObjectArrayList<>();
    private static final FloatArrayList EMPTY_CHANCES = new FloatArrayList();

    // Internal
    private BiomeData biomeData;

    // Constructor \\

    public void constructor(BiomeData biomeData) {
        this.biomeData = biomeData;
    }

    // Accessible \\

    public BiomeData getBiomeData() {
        return biomeData;
    }

    public String getBiomeName() {
        return biomeData.getBiomeName();
    }

    public String getDisplayName() {
        return biomeData.getDisplayName();
    }

    public boolean hasDisplayName() {
        return biomeData.hasDisplayName();
    }

    public short getBiomeID() {
        return biomeData.getBiomeID();
    }

    public Color getBiomeColor() {
        return biomeData.getBiomeColor();
    }

    public ObjectArrayList<String> getWeatherNamesForSeason(String seasonName) {
        ObjectArrayList<String> names = biomeData.getWeatherNamesForSeason(seasonName);
        return names != null ? names : EMPTY_NAMES;
    }

    public FloatArrayList getWeatherChancesForSeason(String seasonName) {
        FloatArrayList chances = biomeData.getWeatherChancesForSeason(seasonName);
        return chances != null ? chances : EMPTY_CHANCES;
    }

    public boolean hasWeathersForSeason(String seasonName) {
        return biomeData.getWeatherNamesForSeason(seasonName) != null;
    }

    public ObjectArrayList<String> getDefinedSeasonNames() {
        return biomeData.getSeasonNames();
    }

    public boolean hasAnyWeathers() {
        return !biomeData.getSeasonNames().isEmpty();
    }

    public int getMapColor() {
        return biomeData.getMapColor();
    }

    public boolean hasMapColor() {
        return biomeData.hasMapColor();
    }

    public ObjectArrayList<ProbableBiomeStruct> getProbableBiomes() {
        return biomeData.getProbableBiomes();
    }

    public String getSurfaceBlockName() {
        return biomeData.getSurfaceBlockName();
    }

    public String getSubsurfaceBlockName() {
        return biomeData.getSubsurfaceBlockName();
    }

    public String getUnderwaterBlockName() {
        return biomeData.getUnderwaterBlockName();
    }

    public String getRockBlockName() {
        return biomeData.getRockBlockName();
    }

    public float getRockSlope() {
        return biomeData.getRockSlope();
    }

    public LinearSpline getContinentalnessSpline() {
        return biomeData.getContinentalnessSpline();
    }

    public LinearSpline getErosionSpline() {
        return biomeData.getErosionSpline();
    }

    public LinearSpline getPeaksValleysSpline() {
        return biomeData.getPeaksValleysSpline();
    }

    public float getDetailAmplitudeBlocks() {
        return biomeData.getDetailAmplitudeBlocks();
    }

    public float getDetailWavelengthBlocks() {
        return biomeData.getDetailWavelengthBlocks();
    }

    public float getTerrainHeightScale() {
        return biomeData.getTerrainHeightScale();
    }

    public BiomeCliffStruct getCliffs() {
        return biomeData.getCliffs();
    }

    public BiomeRidgeStruct getRidges() {
        return biomeData.getRidges();
    }

    public BiomeCoastStruct getCoast() {
        return biomeData.getCoast();
    }

    public BiomeCaveStruct getCaves() {
        return biomeData.getCaves();
    }

    public ObjectArrayList<BiomeVeinStruct> getVeins() {
        return biomeData.getVeins();
    }

    public boolean hasOceanWater() {
        return biomeData.hasOceanWater();
    }

    public int getWaterLevelBlocks() {
        return biomeData.getWaterLevelBlocks();
    }

    public boolean hasLakeWater() {
        return biomeData.hasLakeWater();
    }

    public String getBeachBiomeName() {
        return biomeData.getBeachBiomeName();
    }

    public boolean hasBeachBiome() {
        return biomeData.hasBeachBiome();
    }
}