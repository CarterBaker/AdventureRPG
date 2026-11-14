package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.physicspipeline.util.SubBlockSampleUtility;
import application.bootstrap.soundpipeline.soundcue.SoundSurfaceStruct;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class MovementSoundBranch extends BranchPackage {

    /*
     * Sounds an entity's movement once it has moved, from what it did against
     * what was last heard of it: a step each time it walks a stride — longer
     * the faster it goes, louder too — on the covering under its feet, or the
     * block when bare, the deck of a ship, the shallows it wades or the water
     * it swims; a jump as it leaves the ground, a landing by how fast it fell,
     * and a splash by how fast it met the water. Every entity moves through
     * here, players and NPCs alike, so all of them are heard the same way.
     */

    // Internal
    private SoundCueManager soundCueManager;
    private WorldStreamManager worldStreamManager;
    private BlockManager blockManager;
    private CoveringManager coveringManager;

    // Cues
    private int jumpCueID;
    private int landCueID;
    private int landHardCueID;
    private int wadeCueID;
    private int swimCueID;
    private int splashCueID;
    private int splashBigCueID;
    private int climbCueID;
    private SoundSurfaceStruct deckSurface;

    // Internal \\

    @Override
    protected void get() {
        this.soundCueManager = get(SoundCueManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.jumpCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_JUMP);
        this.landCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_LAND);
        this.landHardCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_LAND_HARD);
        this.wadeCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_WADE);
        this.swimCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SWIM);
        this.splashCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SPLASH);
        this.splashBigCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SPLASH_BIG);
        this.climbCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_CLIMB);
        this.deckSurface = soundCueManager.getSurfaceStruct(EngineSetting.SOUND_SURFACE_DECK);
    }

    // Update \\

    void update(EntityInstance entity, boolean inWater, boolean swimming, boolean climbing, boolean onDeck) {

        EntityStateHandle state = entity.getEntityStateHandle();
        boolean airborne = !state.isGrounded() && !swimming && !climbing;

        hearWater(entity, state, inWater);
        hearGround(entity, state, airborne, inWater, onDeck);
        hearStride(entity, state, airborne, inWater, swimming, climbing, onDeck);

        state.setHeard(
                airborne,
                airborne ? Math.max(state.getHeardFallSpeed(), -state.getVerticalSpeed()) : 0f,
                inWater);
    }

    // The water met, louder and fuller the faster it was met
    private void hearWater(EntityInstance entity, EntityStateHandle state, boolean inWater) {

        if (!inWater || state.isHeardInLiquid())
            return;

        float speed = Math.max(state.getHeardFallSpeed(), state.getHorizontalSpeed());

        play(speed >= EngineSetting.SOUND_SPLASH_SPEED_BIG ? splashBigCueID : splashCueID,
                resolveLoudness(speed), entity);
    }

    // Leaving the ground, and meeting it again by how fast it fell
    private void hearGround(
            EntityInstance entity,
            EntityStateHandle state,
            boolean airborne,
            boolean inWater,
            boolean onDeck) {

        if (airborne && !state.isHeardAirborne() && state.isJumping()) {
            play(jumpCueID, 1f, entity);
            return;
        }

        float speed = state.getHeardFallSpeed();

        if (airborne || !state.isHeardAirborne() || inWater || speed < EngineSetting.SOUND_LAND_SPEED_MIN)
            return;

        play(speed >= EngineSetting.SOUND_LAND_SPEED_HARD ? landHardCueID : landCueID, resolveLoudness(speed), entity);
        playSurface(resolveSurface(entity, onDeck), resolveLoudness(speed), entity);
    }

    // A step each stride walked, a stroke each stride swum
    private void hearStride(
            EntityInstance entity,
            EntityStateHandle state,
            boolean airborne,
            boolean inWater,
            boolean swimming,
            boolean climbing,
            boolean onDeck) {

        float speed = climbing ? Math.abs(state.getVerticalSpeed()) : state.getHorizontalSpeed();
        float stride = swimming
                ? EngineSetting.SOUND_SWIM_STRIDE
                : EngineSetting.SOUND_STEP_STRIDE + speed * EngineSetting.SOUND_STEP_STRIDE_PER_SPEED;

        if (airborne || speed < EngineSetting.SOUND_STEP_SPEED_MIN) {
            state.setStepStride(stride * EngineSetting.SOUND_STEP_FIRST_SHARE);
            return;
        }

        float walked = state.getStepStride() + speed * internal.getDeltaTime();

        if (walked < stride) {
            state.setStepStride(walked);
            return;
        }

        state.setStepStride(walked - stride);

        float loudness = resolveLoudness(speed);

        if (swimming)
            play(swimCueID, loudness, entity);
        else if (climbing)
            play(climbCueID, loudness, entity);
        else if (inWater)
            play(wadeCueID, loudness, entity);
        else
            playSurface(resolveSurface(entity, onDeck), loudness, entity);
    }

    // Surface \\

    // What the feet stand on: a ship's deck, else the covering over the block below, else that block
    private SoundSurfaceStruct resolveSurface(EntityInstance entity, boolean onDeck) {

        if (onDeck)
            return deckSurface;

        WorldPositionStruct place = entity.getWorldPositionStruct();
        Vector3 position = place.getPosition();
        Vector3 size = entity.getSize();
        long chunkCoordinate = place.getChunkCoordinate();
        int subX = SubBlockSampleUtility.toSub(position.x + size.x * 0.5f);
        int subY = SubBlockSampleUtility.toSub(position.y - SubBlockUtility.SIZE * 0.5f);
        int subZ = SubBlockSampleUtility.toSub(position.z + size.z * 0.5f);

        CoveringHandle covering = coveringManager.getCoveringHandleFromCoverage(
                SubBlockSampleUtility.getCoverageAt(worldStreamManager, chunkCoordinate, subX, subY, subZ));

        if (covering != null && covering.hasSoundSurface())
            return covering.getSoundSurface();

        BlockHandle below = SubBlockSampleUtility.getSubBlockAt(
                worldStreamManager, blockManager, chunkCoordinate, subX, subY, subZ);

        return below != null ? below.getSoundSurface() : null;
    }

    // Utility \\

    private void playSurface(SoundSurfaceStruct surface, float loudness, EntityInstance entity) {
        if (surface != null)
            play(surface.getStepCueID(), loudness, entity);
    }

    private void play(int cueID, float loudness, EntityInstance entity) {
        soundCueManager.playCueAt(cueID, loudness, entity.getWorldHandle(), entity.getWorldPositionStruct());
    }

    // From the quietest step at a stroll to full at a sprint
    private float resolveLoudness(float speed) {

        float share = Math.min(1f, speed / EngineSetting.SOUND_STEP_SPEED_LOUD);

        return EngineSetting.SOUND_STEP_VOLUME_MIN + (1f - EngineSetting.SOUND_STEP_VOLUME_MIN) * share;
    }
}
