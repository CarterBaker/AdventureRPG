package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class OverheadCellStruct extends StructPackage {

    /*
     * One renderable cloud-volume placement — what CloudRenderSystem
     * actually turns into a single ModelInstance. As of the Stage 1
     * weather-pattern retrofit (see OverheadManager's own doc comment),
     * this is no longer an independent patch of weather with its own
     * resolved identity — it is a thin, live read-through view of exactly
     * one "lobe" (see WeatherPatternLobeStruct) belonging to exactly one
     * WeatherPatternStruct. Every getter below simply delegates to the
     * owning pattern and/or lobe, so a cell's drifted position, fade
     * alpha, and intensity automatically reflect whatever the pattern
     * most recently resolved to on a given frame, with no per-frame sync
     * step of its own required.
     *
     * Deliberately kept as its own public type, with this exact same
     * public API, so CloudRenderSystem — a different package, and the
     * only outside consumer of this class — needed no changes at all for
     * the Stage 1 retrofit; only how OverheadManager constructs and
     * manages these changed underneath it.
     *
     * A pattern's lobes are fixed for the pattern's entire lifetime, each
     * with its own stable random seed, size variance, domain rotation,
     * and (independently, per lobe) cloud archetype choice — inherited
     * essentially unchanged from the original per-cell design this
     * replaces:
     *
     * randomSeed is a stable per-lobe float handed to the render system
     * so each lobe's cloud instance can vary shape/warp without ever
     * re-rolling and without every lobe in the world looking identical.
     *
     * sizeVariance multiplies both an instance's baked scale and
     * verticalThickness together (see
     * CloudRenderSystem.bakeArchetypeUniforms()) so proportions stay true
     * to the archetype (a Stratus stays flat and wide, a Nimbus stays
     * tall and dense) while the actual size of any two same-type clouds
     * still genuinely differs.
     *
     * domainRotation (see VolumetricCloudUtility.glsl's "Shape diversity
     * fix") picks the compass direction THIS lobe's raymarch stretches
     * toward as it climbs, so its elongation reads as a real, visible
     * streak in a random direction rather than a symmetric bulge every
     * lobe shares.
     *
     * fadeAlpha and intensity are no longer this class's own state — they
     * belong to the owning WeatherPatternStruct and are shared identically
     * by every one of its lobes, since a pattern's lobes fade in, fade
     * out, and strengthen/weaken together as one weather system, never
     * independently.
     */

    // Internal
    private final long cellKey;
    private final WeatherPatternStruct pattern;
    private final WeatherPatternLobeStruct lobe;

    // Constructor \\

    OverheadCellStruct(long cellKey, WeatherPatternStruct pattern, WeatherPatternLobeStruct lobe) {

        // Internal
        this.cellKey = cellKey;
        this.pattern = pattern;
        this.lobe = lobe;
    }

    // Accessible \\

    public long getCellKey() {
        return cellKey;
    }

    public WeatherHandle getWeatherHandle() {
        return pattern.getWeatherHandle();
    }

    public CloudHandle getCloudHandle() {
        return lobe.getCloudHandle();
    }

    public boolean hasCloud() {
        return lobe.hasCloud();
    }

    public float getEffectiveAltitude() {
        return lobe.getEffectiveAltitude();
    }

    public float getRandomSeed() {
        return lobe.getRandomSeed();
    }

    public float getSizeVariance() {
        return lobe.getSizeVariance();
    }

    public float getDomainRotation() {
        return lobe.getDomainRotation();
    }

    public double getCurrentChunkX() {
        return pattern.getCurrentChunkX() + lobe.getOffsetChunkX();
    }

    public double getCurrentChunkZ() {
        return pattern.getCurrentChunkZ() + lobe.getOffsetChunkZ();
    }

    public float getFadeAlpha() {
        return pattern.getFadeAlpha();
    }

    public boolean isRetiring() {
        return pattern.isRetiring();
    }

    public float getIntensity() {
        return pattern.getIntensity();
    }
}