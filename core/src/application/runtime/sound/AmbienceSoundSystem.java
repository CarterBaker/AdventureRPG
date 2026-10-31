package application.runtime.sound;

import java.util.SplittableRandom;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.soundpipeline.soundcue.SoundLoopInstance;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;

public class AmbienceSoundSystem extends SystemPackage {

    /*
     * Holds the world around this window's player as one loop per
     * AmbienceLayer, each eased toward what the place, the weather and the
     * hour call for: the wind by how hard it blows, and its gale above that;
     * the trees of a wooded biome stirring calm, windy or in a storm as the
     * wind rises; rain by how hard it falls as rain rather than snow; the open
     * sea, or a shore where the biome has a coast, the nearer the camera is
     * to the water; birds by day and crickets by night on land, hushed by the
     * rain; and nothing but the hush under water while the camera is beneath
     * it. In a hard storm thunder rolls now and then, and in a strong wind it
     * gusts. Only the window that is heard holds them, unless it is pinned
     * quiet; any other lets them fade, and a window with no player hears none. The biome is read again
     * only when the player steps into another chunk.
     */

    // Internal
    private SoundCueManager soundCueManager;
    private PlayerManager playerManager;
    private BiomeManager biomeManager;
    private WorldSystem worldSystem;
    private SoundListenerSystem soundListenerSystem;

    // Layers
    private SoundLoopInstance[] loops;
    private float[] levels;

    // Moments
    private int thunderCueID;
    private int gustCueID;
    private float thunderSeconds;
    private float gustSeconds;
    private SplittableRandom random;

    // Biome — read again only when the player's chunk changes
    private WorldHandle biomeWorld;
    private long biomeChunk;
    private BiomeHandle biome;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.loops = new SoundLoopInstance[AmbienceLayer.VALUES.length];
        this.levels = new float[AmbienceLayer.VALUES.length];

        // Moments
        this.random = new SplittableRandom(RuntimeSetting.SOUND_AMBIENCE_RANDOM_SEED);
    }

    @Override
    protected void get() {
        this.soundCueManager = get(SoundCueManager.class);
        this.playerManager = get(PlayerManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.soundListenerSystem = get(SoundListenerSystem.class);
    }

    @Override
    protected void awake() {

        // Layers
        for (int i = 0; i < loops.length; i++) {
            loops[i] = create(SoundLoopInstance.class);
            loops[i].constructor(soundCueManager.getCueIDFromCueName(AmbienceLayer.VALUES[i].getCueName()));
        }

        // Moments
        this.thunderCueID = soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_THUNDER);
        this.gustCueID = soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_GUST);
        this.thunderSeconds = RuntimeSetting.SOUND_THUNDER_SECONDS_MIN;
        this.gustSeconds = RuntimeSetting.SOUND_GUST_SECONDS_MIN;
    }

    // Update \\

    @Override
    protected void update() {

        int windowID = context.getWindow().getWindowID();
        GridInstance grid = worldSystem.getGridInstance();

        for (int i = 0; i < levels.length; i++)
            levels[i] = 0f;

        if (soundListenerSystem.isAmbientHere() && playerManager.hasPlayerForWindow(windowID) && grid != null)
            listen(playerManager.getPlayerForWindow(windowID), grid,
                    playerManager.getCameraForWindow(windowID).getPosition().y);

        for (int i = 0; i < loops.length; i++)
            loops[i].hold(levels[i]);
    }

    // Every layer's level from the place, the weather and the hour, and the storm's moments
    private void listen(EntityInstance player, GridInstance grid, float cameraY) {

        BiomeHandle here = resolveBiome(player);

        if (grid.getWaveInstance().isCameraSubmerged()) {
            levels[AmbienceLayer.UNDERWATER.ordinal()] = 1f;
            return;
        }

        float wind = Math.min(1f, grid.getWindInstance().getLocalWindSpeed() / RuntimeSetting.SOUND_WIND_SPEED_FULL);
        float gale = resolveAbove(wind, RuntimeSetting.SOUND_WIND_STRONG_START);
        float wooded = here != null && !here.getTrees().isEmpty() ? 1f : 0f;
        Vector4 precipitation = grid.getPrecipitationInstance().getState();
        float rain = precipitation.x * (1f - precipitation.y);
        float downpour = resolveAbove(rain, RuntimeSetting.SOUND_RAIN_HEAVY_START);
        float sea = here != null && here.hasOceanWater()
                ? resolveNear(cameraY - grid.getWaveInstance().getCameraSurfaceHeightBlocks())
                : 0f;
        float coast = here != null && here.getCoast() != null ? 1f : 0f;
        float day = resolveDay(grid);
        float calm = (1f - rain) * (1f - sea);

        levels[AmbienceLayer.WIND.ordinal()] = wind;
        levels[AmbienceLayer.WIND_STRONG.ordinal()] = gale;
        levels[AmbienceLayer.TREES_CALM.ordinal()] = wooded * Math.max(0f, 1f - wind * 2f);
        levels[AmbienceLayer.TREES_WINDY.ordinal()] = wooded * (1f - Math.abs(wind * 2f - 1f));
        levels[AmbienceLayer.TREES_STORMY.ordinal()] = wooded * Math.max(0f, wind * 2f - 1f);
        levels[AmbienceLayer.RAIN.ordinal()] = rain * (1f - downpour);
        levels[AmbienceLayer.RAIN_HEAVY.ordinal()] = rain * downpour;
        levels[AmbienceLayer.OCEAN.ordinal()] = sea * (1f - coast);
        levels[AmbienceLayer.SHORE.ordinal()] = sea * coast;
        levels[AmbienceLayer.BIRDS.ordinal()] = calm * day
                * (wooded + (1f - wooded) * RuntimeSetting.SOUND_BIRDS_OPEN_SHARE);
        levels[AmbienceLayer.CRICKETS.ordinal()] = calm * (1f - day);

        hearStorm(rain * downpour, gale);
    }

    // Thunder now and then in a hard storm, and a gust now and then in a strong wind
    private void hearStorm(float storm, float gale) {

        float deltaTime = internal.getDeltaTime();

        this.thunderSeconds -= deltaTime;
        this.gustSeconds -= deltaTime;

        if (thunderSeconds <= 0f) {
            this.thunderSeconds = resolveWait(
                    RuntimeSetting.SOUND_THUNDER_SECONDS_MIN, RuntimeSetting.SOUND_THUNDER_SECONDS_MAX);

            if (storm >= RuntimeSetting.SOUND_THUNDER_START)
                soundCueManager.playCue(thunderCueID, storm);
        }

        if (gustSeconds <= 0f) {
            this.gustSeconds = resolveWait(
                    RuntimeSetting.SOUND_GUST_SECONDS_MIN, RuntimeSetting.SOUND_GUST_SECONDS_MAX);

            if (gale >= RuntimeSetting.SOUND_GUST_START)
                soundCueManager.playCue(gustCueID, gale);
        }
    }

    // Biome \\

    private BiomeHandle resolveBiome(EntityInstance player) {

        WorldHandle worldHandle = player.getWorldHandle();
        long chunkCoordinate = player.getWorldPositionStruct().getChunkCoordinate();

        if (biome == null || worldHandle != biomeWorld || chunkCoordinate != biomeChunk) {
            this.biomeWorld = worldHandle;
            this.biomeChunk = chunkCoordinate;
            this.biome = biomeManager.getBiome(worldHandle, chunkCoordinate);
        }

        return biome;
    }

    // Utility \\

    // How far a share has risen past where it begins, from 0 there to 1 at full
    private float resolveAbove(float share, float start) {
        return Math.max(0f, (share - start) / (1f - start));
    }

    // Full at the water's surface, silent once the camera stands the heard height above it
    private float resolveNear(float heightAboveWater) {
        return Math.clamp(1f - heightAboveWater / RuntimeSetting.SOUND_OCEAN_HEARD_HEIGHT, 0f, 1f);
    }

    // From 0 by night to 1 by day, easing across the twilight either side of the horizon
    private float resolveDay(GridInstance grid) {

        float elevation = (float) grid.getClockInstance().getSolarElevation();

        return Math.clamp(0.5f + elevation / (2f * RuntimeSetting.SOUND_DAY_TWILIGHT), 0f, 1f);
    }

    private float resolveWait(float minimum, float maximum) {
        return minimum + (float) random.nextDouble() * (maximum - minimum);
    }
}
