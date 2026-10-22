package editor.dev.recorder;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.combatpipeline.projectile.ProjectileInstance;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileRenderSystem;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.EntityRenderSystem;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.runtime.world.WorldSystem;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCameraTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLayerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingProjectileTrackStruct;
import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.assets.camera.CameraInstance;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.quaternions.Quaternion;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

public class DevRecorderSystem extends SystemPackage {

    /*
     * Carries out the recorder's session in this Dev window, inside its crash
     * boundary. When a session starts here it notes where the window stood —
     * the player, the camera, free flight and the clock — and puts it all
     * back when the session ends. Every frame it plays each channel the
     * session plays from the topmost layer covering the playhead: the camera
     * flies the window's player along as an unseen anchor, so the world
     * streams and draws around the recorded view; the player and the
     * residents are puppets — entities drawn only, posed sample by sample and
     * dressed in the look they wore — and thrown items are drawn where they
     * flew; the clock is held to the recording, and a time command used while
     * it plays shifts it from there on. A take records each live channel into
     * its layer after the frame has moved: the camera, the player, the clock,
     * and every resident and thrown item near the window, dropping a marker
     * wherever a resident falls. Starting a take hands each live channel over
     * from what was playing, so a performance carries on from where the
     * recording stood.
     */

    // Internal
    private RecorderManager recorderManager;
    private PlayerManager playerManager;
    private ClockManager clockManager;
    private NpcManager npcManager;
    private ProjectileManager projectileManager;
    private EntityManager entityManager;
    private GenderManager genderManager;
    private AnimationManager animationManager;
    private ItemDefinitionManager itemDefinitionManager;
    private SaveManager saveManager;
    private EntityRenderSystem entityRenderSystem;
    private ProjectileRenderSystem projectileRenderSystem;
    private WorldSystem worldSystem;

    // Session
    private boolean sessionActive;
    private int seenTakeCount;
    private float lastPlayhead;

    // Snapshot — where the window stood when the session began
    private WorldPositionStruct snapshotPlace;
    private float snapshotBodyYaw;
    private Vector3 snapshotCameraPosition;
    private Vector3 snapshotCameraDirection;
    private float snapshotFov;
    private boolean snapshotFreeCamera;
    private long snapshotElapsedMillis;

    // Clock
    private boolean clockHeld;
    private long heldElapsedMillis;
    private long heldAtTime;
    private long liveClockOffsetMillis;

    // Puppets
    private Reference2ObjectOpenHashMap<RecordingBodyTrackStruct, EntityInstance> track2Puppet;
    private Reference2ObjectOpenHashMap<RecordingBodyTrackStruct, ObjectArrayList<AnimationClipHandle>> track2Clips;
    private Reference2IntOpenHashMap<RecordingBodyTrackStruct> track2AppliedLook;
    private ReferenceOpenHashSet<Object> refusedTracks;
    private ObjectArrayList<EntityInstance> shownPuppets;

    // Thrown Items
    private Reference2ObjectOpenHashMap<RecordingProjectileTrackStruct, ItemDefinitionHandle> track2Item;
    private ObjectArrayList<ItemDefinitionHandle> shownItems;
    private ObjectArrayList<WorldPositionStruct> shownItemPlaces;
    private ObjectArrayList<Quaternion> shownItemOrientations;

    // Capture
    private Reference2ObjectOpenHashMap<EntityInstance, RecordingBodyTrackStruct> resident2Track;
    private Reference2ObjectOpenHashMap<ProjectileInstance, RecordingProjectileTrackStruct> projectile2Track;
    private float lastSampleTime;
    private float lastWorldSampleTime;

    // Scratch
    private WorldPositionStruct placeScratch;
    private Vector3 eyeScratch;
    private Vector3 directionScratch;

    // Base \\

    @Override
    protected void create() {

        // Snapshot
        this.snapshotPlace = new WorldPositionStruct();
        this.snapshotCameraPosition = new Vector3();
        this.snapshotCameraDirection = new Vector3();

        // Puppets
        this.track2Puppet = new Reference2ObjectOpenHashMap<>();
        this.track2Clips = new Reference2ObjectOpenHashMap<>();
        this.track2AppliedLook = new Reference2IntOpenHashMap<>();
        this.track2AppliedLook.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.refusedTracks = new ReferenceOpenHashSet<>();
        this.shownPuppets = new ObjectArrayList<>();

        // Thrown Items
        this.track2Item = new Reference2ObjectOpenHashMap<>();
        this.shownItems = new ObjectArrayList<>();
        this.shownItemPlaces = new ObjectArrayList<>();
        this.shownItemOrientations = new ObjectArrayList<>();

        // Capture
        this.resident2Track = new Reference2ObjectOpenHashMap<>();
        this.projectile2Track = new Reference2ObjectOpenHashMap<>();

        // Scratch
        this.placeScratch = new WorldPositionStruct();
        this.eyeScratch = new Vector3();
        this.directionScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.recorderManager = get(RecorderManager.class);
        this.playerManager = get(PlayerManager.class);
        this.clockManager = get(ClockManager.class);
        this.npcManager = get(NpcManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.entityManager = get(EntityManager.class);
        this.genderManager = get(GenderManager.class);
        this.animationManager = get(AnimationManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.saveManager = get(SaveManager.class);
        this.entityRenderSystem = get(EntityRenderSystem.class);
        this.projectileRenderSystem = get(ProjectileRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // The window is going, its player with it, so only the shared clock is put back
    @Override
    protected void dispose() {

        if (sessionActive) {
            clockManager.holdClock(snapshotElapsedMillis);
            releasePuppets();
            this.sessionActive = false;
        }

        recorderManager.releaseDevContext((DevContext) context);
    }

    // Update \\

    @Override
    protected void update() {

        boolean session = recorderManager.isSessionFor((DevContext) context);

        if (session && !sessionActive)
            beginSession();

        if (!session && sessionActive)
            endSession();

        if (!sessionActive)
            return;

        if (recorderManager.getState() == RecorderState.RECORDING && recorderManager.getTakeCount() != seenTakeCount)
            beginTake();

        holdAnchor();
        play();
    }

    @Override
    protected void lateUpdate() {
        if (sessionActive && recorderManager.getState() == RecorderState.RECORDING)
            capture();
    }

    // Session \\

    private void beginSession() {

        int windowID = context.getWindow().getWindowID();
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        snapshotPlace.setPosition(player.getWorldPositionStruct().getPosition());
        snapshotPlace.setChunkCoordinate(player.getWorldPositionStruct().getChunkCoordinate());
        this.snapshotBodyYaw = player.getEntityStateHandle().getBodyYaw();
        snapshotCameraPosition.set(camera.getPosition());
        snapshotCameraDirection.set(camera.getDirection());
        this.snapshotFov = camera.getFOV();
        this.snapshotFreeCamera = playerManager.isFreeCameraForWindow(windowID);
        this.snapshotElapsedMillis = clockManager.getElapsedMillis();

        this.sessionActive = true;
        this.lastPlayhead = recorderManager.getPlayhead();
        this.clockHeld = false;
        this.liveClockOffsetMillis = 0L;
    }

    // Puts the window back just as it stood when the session began
    private void endSession() {

        int windowID = context.getWindow().getWindowID();
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        EntityStateHandle state = player.getEntityStateHandle();

        player.setLocation(snapshotPlace.getPosition(), snapshotPlace.getChunkCoordinate());
        state.getGravityVelocity().set(0f);
        state.getHorizontalVelocity().set(0f);
        state.setBodyYaw(snapshotBodyYaw, 0f);
        camera.setPosition(snapshotCameraPosition);
        camera.setDirection(snapshotCameraDirection);
        camera.setFOV(snapshotFov);
        camera.setShake(0f, 0f, 0f);

        playerManager.setHeldForWindow(windowID, false);
        playerManager.setFreeCameraForWindow(windowID, snapshotFreeCamera);
        playerManager.pushPlayerPositionForWindow(windowID);
        clockManager.holdClock(snapshotElapsedMillis);

        releasePuppets();
        resident2Track.clear();
        projectile2Track.clear();
        this.sessionActive = false;
    }

    private void releasePuppets() {
        track2Puppet.clear();
        track2Clips.clear();
        track2AppliedLook.clear();
        track2Item.clear();
        refusedTracks.clear();
        shownPuppets.clear();
        shownItems.clear();
    }

    // The window's player is the camera's anchor: held still while the recording flies the camera, flying free
    // while a take records the camera alone, and played as ever while a take records the player
    private void holdAnchor() {

        int windowID = context.getWindow().getWindowID();
        int live = recorderManager.getLiveChannels();
        boolean cameraPlayed = RecorderChannel.CAMERA.isIn(recorderManager.getPlayedChannels());

        playerManager.setHeldForWindow(windowID, cameraPlayed);
        playerManager.setFreeCameraForWindow(windowID, cameraPlayed || !RecorderChannel.PLAYER.isIn(live));
    }

    // Take \\

    // Each live channel picks up from where the recording stood at the playhead
    private void beginTake() {

        this.seenTakeCount = recorderManager.getTakeCount();
        this.lastSampleTime = EditorSetting.RECORDER_TIME_UNSET;
        this.lastWorldSampleTime = EditorSetting.RECORDER_TIME_UNSET;
        resident2Track.clear();
        projectile2Track.clear();

        RecordingInstance recording = recorderManager.getRecording();
        int live = recorderManager.getLiveChannels();
        float time = recorderManager.getPlayhead();

        if (RecorderChannel.PLAYER.isIn(live))
            handOverPlayer(recording.findPlayingLayer(RecorderChannel.PLAYER, time, null), time);

        if (RecorderChannel.TIME.isIn(live))
            handOverClock(recording.findPlayingLayer(RecorderChannel.TIME, time, null), time);

        this.clockHeld = false;
    }

    private void handOverPlayer(RecordingLayerStruct layer, float time) {

        if (layer == null || !layer.hasPlayerTrack())
            return;

        RecordingBodyTrackStruct track = layer.getPlayerTrack();
        int sample = Math.max(0, track.findSample(time));
        EntityInstance player = playerManager.getPlayerForWindow(context.getWindow().getWindowID());

        RecordingSampleUtility.resolvePlace(resolveWorld(), track, sample, 0f, placeScratch);
        player.setLocation(placeScratch.getPosition(), placeScratch.getChunkCoordinate());
        player.getEntityStateHandle().setBodyYaw(track.getBodyYaws().getFloat(sample), 0f);
    }

    private void handOverClock(RecordingLayerStruct layer, float time) {
        if (layer != null)
            clockManager.holdClock(layer.getClockTrack().evaluate(time, layer.getTimeRampSeconds()));
    }

    // Play \\

    private void play() {

        RecordingInstance recording = recorderManager.getRecording();
        int played = recorderManager.getPlayedChannels();
        float time = recorderManager.getPlayhead();
        float animationDelta = Math.max(0f, time - lastPlayhead);
        RecordingLayerStruct excluded = recorderManager.getTakeLayer();

        this.lastPlayhead = time;
        shownPuppets.clear();
        shownItems.clear();

        if (RecorderChannel.CAMERA.isIn(played))
            playCamera(recording.findPlayingLayer(RecorderChannel.CAMERA, time, excluded), time);

        if (RecorderChannel.PLAYER.isIn(played))
            playPlayer(recording.findPlayingLayer(RecorderChannel.PLAYER, time, excluded), time, animationDelta);

        if (RecorderChannel.WORLD.isIn(played))
            playWorld(recording.findPlayingLayer(RecorderChannel.WORLD, time, excluded), time, animationDelta);

        if (RecorderChannel.TIME.isIn(played))
            playClock(recording.findPlayingLayer(RecorderChannel.TIME, time, excluded), time);
        else
            this.clockHeld = false;

        playerManager.pushPlayerPositionForWindow(context.getWindow().getWindowID());
    }

    // The anchor stands so its eye is on the camera, and the camera is placed in the frame of the anchor's chunk
    private void playCamera(RecordingLayerStruct layer, float time) {

        if (layer == null || layer.getCameraTrack().isEmpty())
            return;

        RecordingCameraTrackStruct track = layer.getCameraTrack();
        int sample = Math.max(0, track.findSample(time));
        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, time);
        int windowID = context.getWindow().getWindowID();
        EntityInstance anchor = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        WorldHandle worldHandle = resolveWorld();
        WorldPositionStruct anchorPlace = anchor.getWorldPositionStruct();

        RecordingSampleUtility.resolvePlace(worldHandle, track, sample, blend, placeScratch);
        anchor.getEyePosition(eyeScratch).subtract(anchorPlace.getPosition());

        long cameraChunk = placeScratch.getChunkCoordinate();
        Vector3 cameraPosition = placeScratch.getPosition();

        anchorPlace.getPosition().set(
                cameraPosition.x - eyeScratch.x,
                cameraPosition.y - eyeScratch.y,
                cameraPosition.z - eyeScratch.z);
        anchorPlace.setChunkCoordinate(cameraChunk);
        WorldPositionUtility.settleChunk(worldHandle, anchorPlace);

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                worldHandle, anchorPlace.getChunkCoordinate(), cameraChunk);

        cameraPosition.add(
                Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE,
                0f,
                Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE);
        camera.setPosition(cameraPosition);
        camera.setDirection(directionScratch.set(
                RecordingSampleUtility.blend(track.getDirectionsX(), sample, blend),
                RecordingSampleUtility.blend(track.getDirectionsY(), sample, blend),
                RecordingSampleUtility.blend(track.getDirectionsZ(), sample, blend)));
        camera.setShake(
                RecordingSampleUtility.blend(track.getShakeYaws(), sample, blend),
                RecordingSampleUtility.blend(track.getShakePitches(), sample, blend),
                RecordingSampleUtility.blend(track.getShakeRolls(), sample, blend));
        camera.setFOV(RecordingSampleUtility.blend(track.getFovs(), sample, blend));
    }

    private void playPlayer(RecordingLayerStruct layer, float time, float animationDelta) {
        if (layer != null && layer.hasPlayerTrack())
            posePuppet(layer.getPlayerTrack(), time, animationDelta);
    }

    private void playWorld(RecordingLayerStruct layer, float time, float animationDelta) {

        if (layer == null)
            return;

        ObjectArrayList<RecordingBodyTrackStruct> residentTracks = layer.getResidentTracks();

        for (int i = 0; i < residentTracks.size(); i++)
            posePuppet(residentTracks.get(i), time, animationDelta);

        ObjectArrayList<RecordingProjectileTrackStruct> projectileTracks = layer.getProjectileTracks();

        for (int i = 0; i < projectileTracks.size(); i++)
            placeItem(projectileTracks.get(i), time);
    }

    // The clock follows the recording; a jump nothing here made — a time command — shifts it from then on
    private void playClock(RecordingLayerStruct layer, float time) {

        if (layer == null) {
            this.clockHeld = false;
            return;
        }

        if (clockHeld) {

            long expected = heldElapsedMillis + (internal.getTime() - heldAtTime);
            long moved = clockManager.getElapsedMillis() - expected;

            if (Math.abs(moved) >= EditorSetting.RECORDER_CLOCK_JUMP_MILLIS)
                liveClockOffsetMillis += moved;
        }

        long elapsed = layer.getClockTrack().evaluate(time, layer.getTimeRampSeconds()) + liveClockOffsetMillis;

        clockManager.holdClock(elapsed);
        this.heldElapsedMillis = elapsed;
        this.heldAtTime = internal.getTime();
        this.clockHeld = true;
    }

    // Puppets \\

    private void posePuppet(RecordingBodyTrackStruct track, float time, float animationDelta) {

        int sample = track.findSample(time);

        if (!RecordingSampleUtility.isPresent(track.getTimes(), sample, time))
            return;

        EntityInstance puppet = resolvePuppet(track);

        if (puppet == null)
            return;

        dressPuppet(track, puppet, time);

        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, time);
        EntityStateHandle state = puppet.getEntityStateHandle();

        RecordingSampleUtility.resolvePlace(resolveWorld(), track, sample, blend, placeScratch);
        puppet.setLocation(placeScratch.getPosition(), placeScratch.getChunkCoordinate());
        state.setMovementState(EntityState.VALUES[track.getStates().getInt(sample)]);
        state.setSpeed(
                RecordingSampleUtility.blend(track.getHorizontalSpeeds(), sample, blend),
                RecordingSampleUtility.blend(track.getVerticalSpeeds(), sample, blend));
        state.setBodyYaw(
                RecordingSampleUtility.blendDegrees(track.getBodyYaws(), sample, blend),
                RecordingSampleUtility.blend(track.getBodyYawRates(), sample, blend));
        state.setLook(
                RecordingSampleUtility.blend(track.getLookPitches(), sample, blend),
                RecordingSampleUtility.blendDegrees(track.getLookYaws(), sample, blend));
        state.setGroundOffset(RecordingSampleUtility.blend(track.getGroundOffsets(), sample, blend));

        int clip = track.getClips().getInt(sample);
        boolean sameAction = sample + 1 < track.size()
                && track.getSequences().getInt(sample + 1) == track.getSequences().getInt(sample);

        puppet.updateAnimation(
                EntityAction.VALUES[track.getActions().getInt(sample)],
                clip == EditorSetting.RECORDER_NO_CLIP ? null : track2Clips.get(track).get(clip),
                sameAction
                        ? RecordingSampleUtility.blend(track.getPhases(), sample, blend)
                        : track.getPhases().getFloat(sample),
                track.getSequences().getInt(sample),
                animationDelta);

        shownPuppets.add(puppet);
    }

    // The puppet a track plays through, made the first time it is seen; a template that no longer exists is
    // reported once and the track left unseen
    private EntityInstance resolvePuppet(RecordingBodyTrackStruct track) {

        EntityInstance puppet = track2Puppet.get(track);

        if (puppet != null || refusedTracks.contains(track))
            return puppet;

        try {
            puppet = entityManager.createPuppet(
                    entityManager.getEntityHandleFromTemplateName(track.getTemplateName()),
                    resolveWorld(),
                    playerManager.getPlayerForWindow(context.getWindow().getWindowID())
                            .getWorldPositionStruct().getChunkCoordinate(),
                    eyeScratch.set(0f, 0f, 0f));
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_NO_TEMPLATE + track.getTemplateName());
            refusedTracks.add(track);
            return null;
        }

        if (genderManager.hasGender(track.getGenderName())) {

            short genderID = genderManager.getGenderIDFromGenderName(track.getGenderName());

            if (puppet.getEntityData().allowsGender(genderID))
                puppet.setGenderID(genderID);
        }

        track2Clips.put(track, resolveClips(track));
        track2Puppet.put(track, puppet);
        return puppet;
    }

    // Every clip the track names, null where one no longer exists so its action plays without it
    private ObjectArrayList<AnimationClipHandle> resolveClips(RecordingBodyTrackStruct track) {

        ObjectArrayList<AnimationClipHandle> clips = new ObjectArrayList<>(track.getClipNames().size());

        for (int i = 0; i < track.getClipNames().size(); i++) {

            try {
                clips.add(animationManager.getClipHandleFromClipName(track.getClipNames().get(i)));
            } catch (InternalException e) {
                clips.add(null);
            }
        }

        return clips;
    }

    // The look the track wore at the time, applied only when it changes
    private void dressPuppet(RecordingBodyTrackStruct track, EntityInstance puppet, float time) {

        int look = track.findLook(time);

        if (look == EngineSetting.INDEX_NOT_FOUND || look == track2AppliedLook.getInt(track))
            return;

        track2AppliedLook.put(track, look);

        if (!saveManager.applyLook(track.getLooks().get(look), puppet))
            errorLog(EditorSetting.RECORDER_MESSAGE_LOOK_REFUSED + track.getDisplayName());
    }

    // Thrown Items \\

    private void placeItem(RecordingProjectileTrackStruct track, float time) {

        int sample = track.findSample(time);

        if (!RecordingSampleUtility.isPresent(track.getTimes(), sample, time))
            return;

        ItemDefinitionHandle item = resolveItem(track);

        if (item == null)
            return;

        int slot = shownItems.size();

        if (slot == shownItemPlaces.size()) {
            shownItemPlaces.add(new WorldPositionStruct());
            shownItemOrientations.add(new Quaternion());
        }

        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, time);

        RecordingSampleUtility.resolvePlace(resolveWorld(), track, sample, blend, shownItemPlaces.get(slot));
        shownItemOrientations.get(slot).set(
                RecordingSampleUtility.blend(track.getOrientationsW(), sample, blend),
                RecordingSampleUtility.blend(track.getOrientationsX(), sample, blend),
                RecordingSampleUtility.blend(track.getOrientationsY(), sample, blend),
                RecordingSampleUtility.blend(track.getOrientationsZ(), sample, blend)).normalize();
        shownItems.add(item);
    }

    private ItemDefinitionHandle resolveItem(RecordingProjectileTrackStruct track) {

        ItemDefinitionHandle item = track2Item.get(track);

        if (item != null || refusedTracks.contains(track))
            return item;

        if (!itemDefinitionManager.hasItem(track.getItemName())) {
            errorLog(EditorSetting.RECORDER_MESSAGE_NO_ITEM + track.getItemName());
            refusedTracks.add(track);
            return null;
        }

        item = itemDefinitionManager.getItemHandleFromItemName(track.getItemName());
        track2Item.put(track, item);
        return item;
    }

    // Render \\

    @Override
    protected void render() {

        if (!sessionActive)
            return;

        EntityInstance anchor = playerManager.getPlayerForWindow(context.getWindow().getWindowID());
        long viewerChunk = anchor.getWorldPositionStruct().getChunkCoordinate();
        FBOInstance worldFbo = worldSystem.getWorldFbo();

        for (int i = 0; i < shownPuppets.size(); i++)
            entityRenderSystem.pushCharacter(shownPuppets.get(i), false, viewerChunk, worldFbo, context.getWindow());

        for (int i = 0; i < shownItems.size(); i++)
            projectileRenderSystem.pushItem(
                    shownItems.get(i),
                    anchor.getWorldHandle(),
                    viewerChunk,
                    shownItemPlaces.get(i).getChunkCoordinate(),
                    shownItemPlaces.get(i).getPosition(),
                    shownItemOrientations.get(i),
                    worldFbo,
                    context.getWindow());
    }

    // Capture \\

    private void capture() {

        RecordingLayerStruct take = recorderManager.getTakeLayer();
        float time = recorderManager.getPlayhead();

        if (isDue(time, lastSampleTime, EditorSetting.RECORDER_SAMPLE_INTERVAL)) {
            this.lastSampleTime = time;
            captureCamera(take, time);
            capturePlayer(take, time);
            captureClock(take, time);
        }

        if (take.holds(RecorderChannel.WORLD)
                && isDue(time, lastWorldSampleTime, EditorSetting.RECORDER_WORLD_SAMPLE_INTERVAL)) {
            this.lastWorldSampleTime = time;
            captureResidents(take, time);
            captureProjectiles(take, time);
        }
    }

    private boolean isDue(float time, float lastTime, float interval) {
        return lastTime < 0f || time - lastTime >= interval - EditorSetting.RECORDER_SAMPLE_TOLERANCE;
    }

    // The camera as it stands in the frame of the chunk the window's player stands in
    private void captureCamera(RecordingLayerStruct take, float time) {

        if (!take.holds(RecorderChannel.CAMERA))
            return;

        int windowID = context.getWindow().getWindowID();

        take.getCameraTrack().addSample(
                time,
                playerManager.getPlayerForWindow(windowID).getWorldPositionStruct().getChunkCoordinate(),
                playerManager.getCameraForWindow(windowID));
    }

    // The character played, never the camera's anchor while it flies free
    private void capturePlayer(RecordingLayerStruct take, float time) {

        int windowID = context.getWindow().getWindowID();

        if (!take.holds(RecorderChannel.PLAYER) || playerManager.isFreeCameraForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        if (!take.hasPlayerTrack())
            take.setPlayerTrack(createBodyTrack(player, EditorSetting.RECORDER_PLAYER_DISPLAY_NAME));

        if (take.getPlayerTrack() != null)
            captureBody(take.getPlayerTrack(), player, time);
    }

    private void captureClock(RecordingLayerStruct take, float time) {
        if (take.holds(RecorderChannel.TIME))
            take.getClockTrack().addSample(time, clockManager.getElapsedMillis());
    }

    // Every resident near the window, each its own track from the first time it is seen; one that falls drops a marker
    private void captureResidents(RecordingLayerStruct take, float time) {

        ObjectArrayList<NpcInstance> residents = npcManager.getResidents();
        EntityInstance viewer = playerManager.getPlayerForWindow(context.getWindow().getWindowID());

        for (int i = 0; i < residents.size(); i++) {

            NpcInstance resident = residents.get(i);
            EntityInstance entity = resident.getEntityInstance();

            if (!isNear(entity.getWorldHandle(), entity.getWorldPositionStruct().getChunkCoordinate(), viewer,
                    EngineSetting.NPC_RENDER_CHUNK_RADIUS))
                continue;

            RecordingBodyTrackStruct track = resident2Track.get(entity);

            if (track == null) {

                track = createBodyTrack(entity, resident.getNpcHandle().getNpcName());

                if (track == null)
                    continue;

                resident2Track.put(entity, track);
                take.getResidentTracks().add(track);
            }

            captureBody(track, entity, time);

            if (resident.isFallen() && !track.isFallen()) {
                track.setFallen(true);
                recorderManager.addMarker(time, EditorSetting.RECORDER_KILL_MARKER_PREFIX + track.getDisplayName());
            }
        }
    }

    private void captureProjectiles(RecordingLayerStruct take, float time) {

        ObjectArrayList<ProjectileInstance> projectiles = projectileManager.getProjectiles();
        EntityInstance viewer = playerManager.getPlayerForWindow(context.getWindow().getWindowID());

        for (int i = 0; i < projectiles.size(); i++) {

            ProjectileInstance projectile = projectiles.get(i);
            WorldPositionStruct place = projectile.getWorldPositionStruct();

            if (!isNear(projectile.getWorldHandle(), place.getChunkCoordinate(), viewer,
                    EngineSetting.PROJECTILE_RENDER_CHUNK_RADIUS))
                continue;

            RecordingProjectileTrackStruct track = projectile2Track.get(projectile);

            if (track == null) {
                track = new RecordingProjectileTrackStruct(
                        projectile.getItemInstance().getItemDefinitionHandle().getItemName());
                projectile2Track.put(projectile, track);
                take.getProjectileTracks().add(track);
            }

            track.addSample(time, place.getChunkCoordinate(), place.getPosition(), projectile.getOrientation());
        }
    }

    // A track for the being, null for one built from a template that was never registered
    private RecordingBodyTrackStruct createBodyTrack(EntityInstance entity, String displayName) {

        String templateName = entityManager.getTemplateName(entity.getEntityData());

        if (templateName == null)
            return null;

        return new RecordingBodyTrackStruct(
                templateName,
                genderManager.getGenderHandleFromGenderID(entity.getGenderID()).getGenderName(),
                displayName);
    }

    // The being's pose, and its look whenever that has changed since it was last taken
    private void captureBody(RecordingBodyTrackStruct track, EntityInstance entity, float time) {

        int lookSignature = resolveLookSignature(entity);

        if (lookSignature != track.getLookSignature()) {
            track.setLookSignature(lookSignature);
            track.addLook(time, saveManager.captureLook(entity));
        }

        track.addSample(time, entity);
    }

    // Changes whenever anything the look holds visibly changes — weight, skin, hair, or any worn or hidden item
    private int resolveLookSignature(EntityInstance entity) {

        InventoryHandle inventory = entity.getInventoryHandle();
        int signature = Float.floatToIntBits(entity.getWeight());

        if (entity.hasAppearance()) {
            signature = mixColor(signature, entity.getAppearanceHandle().getSkinColor());
            signature = mixColor(signature, entity.getAppearanceHandle().getHairColor());
        }

        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {

            signature = signature * EditorSetting.RECORDER_LOOK_HASH_PRIME + (inventory.hasItem(equipmentSlot)
                    ? inventory.getItem(equipmentSlot).getItemDefinitionHandle().getItemName().hashCode()
                    : 0);
            signature = signature * EditorSetting.RECORDER_LOOK_HASH_PRIME
                    + (inventory.isHidden(equipmentSlot) ? 1 : 0);
        }

        return signature;
    }

    private int mixColor(int signature, Color color) {
        signature = signature * EditorSetting.RECORDER_LOOK_HASH_PRIME + Float.floatToIntBits(color.r);
        signature = signature * EditorSetting.RECORDER_LOOK_HASH_PRIME + Float.floatToIntBits(color.g);
        return signature * EditorSetting.RECORDER_LOOK_HASH_PRIME + Float.floatToIntBits(color.b);
    }

    // Utility \\

    private boolean isNear(WorldHandle worldHandle, long chunkCoordinate, EntityInstance viewer, int radius) {

        if (worldHandle != viewer.getWorldHandle())
            return false;

        long delta = WorldWrapUtility.unwrapToGridCoordinate(
                worldHandle, viewer.getWorldPositionStruct().getChunkCoordinate(), chunkCoordinate);

        return Math.abs(Coordinate2Long.unpackX(delta)) <= radius && Math.abs(Coordinate2Long.unpackY(delta)) <= radius;
    }

    private WorldHandle resolveWorld() {
        return playerManager.getPlayerForWindow(context.getWindow().getWindowID()).getWorldHandle();
    }
}
