package editor.runtime.dev.recorder;

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
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleRenderSystem;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.runtime.world.WorldSystem;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveProperty;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCameraTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingProjectileTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingVehicleTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingWeatherTrackStruct;
import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import editor.runtime.dev.DevContext;
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
     * the player, the camera, free flight, the clock and the wind's gusts —
     * and puts it all back when the session ends. Every frame it plays each
     * channel the session plays from the clip on the timeline's highest lane
     * covering the playhead, read at that clip's own moment of its take: the
     * camera flies the window's player along as an unseen anchor, so the
     * world streams and draws around the recorded view; the player and the
     * residents are puppets — entities drawn only, posed sample by sample and
     * dressed in the look they wore — and thrown items and vehicles are drawn
     * where they went; the weather is held where it had drifted; the clock is
     * held to the recording, and a time command used while it plays shifts
     * it from there on. Every value a track has a curve for is read from that
     * curve instead of its samples. The interface plays through
     * DevRecorderOverlaySystem.
     * A take records each live channel after the frame has moved, at the
     * take's own time: the camera, the player, the clock, the weather, and
     * every resident, thrown item and vehicle near the window, dropping a
     * marker wherever a resident falls. Starting a take hands each live
     * channel over from what was playing, so a performance carries on from
     * where the timeline stood. While the session is suspended — the game
     * paused — nothing is recorded, and what plays holds still.
     */

    // Internal
    private RecorderManager recorderManager;
    private PlayerManager playerManager;
    private ClockManager clockManager;
    private NpcManager npcManager;
    private ProjectileManager projectileManager;
    private VehicleManager vehicleManager;
    private WeatherManager weatherManager;
    private WindManager windManager;
    private EntityManager entityManager;
    private GenderManager genderManager;
    private AnimationManager animationManager;
    private ItemDefinitionManager itemDefinitionManager;
    private SaveManager saveManager;
    private EntityRenderSystem entityRenderSystem;
    private ProjectileRenderSystem projectileRenderSystem;
    private VehicleRenderSystem vehicleRenderSystem;
    private WorldSystem worldSystem;
    private DevRecorderOverlaySystem devRecorderOverlaySystem;

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
    private float snapshotGustTime;

    // Clock
    private boolean clockHeld;
    private long heldElapsedMillis;
    private long heldAtTime;
    private long liveClockOffsetMillis;

    // Weather
    private boolean weatherHeld;

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

    // Vehicles
    private Reference2ObjectOpenHashMap<RecordingVehicleTrackStruct, VehicleInstance> track2Vehicle;
    private ObjectArrayList<VehicleInstance> shownVehicles;

    // Capture
    private Reference2ObjectOpenHashMap<EntityInstance, RecordingBodyTrackStruct> resident2Track;
    private Reference2ObjectOpenHashMap<ProjectileInstance, RecordingProjectileTrackStruct> projectile2Track;
    private Reference2ObjectOpenHashMap<VehicleInstance, RecordingVehicleTrackStruct> vehicle2Track;
    private float lastSampleTime;
    private float lastWorldSampleTime;
    private float lastWeatherSampleTime;

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

        // Vehicles
        this.track2Vehicle = new Reference2ObjectOpenHashMap<>();
        this.shownVehicles = new ObjectArrayList<>();

        // Capture
        this.resident2Track = new Reference2ObjectOpenHashMap<>();
        this.projectile2Track = new Reference2ObjectOpenHashMap<>();
        this.vehicle2Track = new Reference2ObjectOpenHashMap<>();

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
        this.vehicleManager = get(VehicleManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.windManager = get(WindManager.class);
        this.entityManager = get(EntityManager.class);
        this.genderManager = get(GenderManager.class);
        this.animationManager = get(AnimationManager.class);
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.saveManager = get(SaveManager.class);
        this.entityRenderSystem = get(EntityRenderSystem.class);
        this.projectileRenderSystem = get(ProjectileRenderSystem.class);
        this.vehicleRenderSystem = get(VehicleRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
        this.devRecorderOverlaySystem = get(DevRecorderOverlaySystem.class);
    }

    // The window is going, its player with it, so only the shared clock and weather are put back
    @Override
    protected void dispose() {

        if (sessionActive) {
            clockManager.holdClock(snapshotElapsedMillis);
            releaseWeather();
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
        if (sessionActive && recorderManager.getState() == RecorderState.RECORDING && !recorderManager.isSuspended())
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
        this.snapshotGustTime = windManager.getGustTime();

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
        releaseWeather();

        releasePuppets();
        resident2Track.clear();
        projectile2Track.clear();
        vehicle2Track.clear();
        this.sessionActive = false;
    }

    private void releasePuppets() {
        track2Puppet.clear();
        track2Clips.clear();
        track2AppliedLook.clear();
        track2Item.clear();
        track2Vehicle.clear();
        refusedTracks.clear();
        shownPuppets.clear();
        shownItems.clear();
        shownVehicles.clear();
    }

    // The weather lets go of the recording and its gusts carry on from where the session found them
    private void releaseWeather() {

        if (!weatherHeld)
            return;

        weatherManager.releaseFlow();
        windManager.holdGustTime(snapshotGustTime);
        windManager.releaseGustTime();
        this.weatherHeld = false;
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

    // Each live channel picks up from where the timeline stood at the playhead
    private void beginTake() {

        this.seenTakeCount = recorderManager.getTakeCount();
        this.lastSampleTime = EditorSetting.RECORDER_TIME_UNSET;
        this.lastWorldSampleTime = EditorSetting.RECORDER_TIME_UNSET;
        this.lastWeatherSampleTime = EditorSetting.RECORDER_TIME_UNSET;
        resident2Track.clear();
        projectile2Track.clear();
        vehicle2Track.clear();

        RecordingInstance recording = recorderManager.getRecording();
        int live = recorderManager.getLiveChannels();
        float time = recorderManager.getPlayhead();

        if (RecorderChannel.PLAYER.isIn(live))
            handOverPlayer(recording.findPlayingClip(RecorderChannel.PLAYER, time), time);

        if (RecorderChannel.TIME.isIn(live))
            handOverClock(recording.findPlayingClip(RecorderChannel.TIME, time), time);

        if (RecorderChannel.WEATHER.isIn(live))
            releaseWeather();

        devRecorderOverlaySystem.beginTake(recorderManager.getTake());
        this.clockHeld = false;
    }

    private void handOverPlayer(RecordingClipStruct clip, float time) {

        if (clip == null || !clip.getTake().hasPlayerTrack())
            return;

        RecordingBodyTrackStruct track = clip.getTake().getPlayerTrack();
        float takeTime = clip.toTakeTime(time);
        int sample = Math.max(0, track.findSample(takeTime));
        EntityInstance player = playerManager.getPlayerForWindow(context.getWindow().getWindowID());

        RecordingCurveUtility.resolvePlace(resolveWorld(), track, takeTime, sample, 0f, placeScratch);
        player.setLocation(placeScratch.getPosition(), placeScratch.getChunkCoordinate());
        player.getEntityStateHandle().setBodyYaw(RecordingCurveUtility.resolveFloat(
                track, RecorderCurveProperty.FACING, takeTime, track.getBodyYaws(), sample, 0f), 0f);
    }

    private void handOverClock(RecordingClipStruct clip, float time) {
        if (clip != null)
            clockManager.holdClock(clip.getTake().getClockTrack().evaluate(
                    clip.toTakeTime(time), clip.getTimeRampSeconds()));
    }

    // Play \\

    private void play() {

        RecordingInstance recording = recorderManager.getRecording();
        int played = recorderManager.getPlayedChannels();
        float time = recorderManager.getPlayhead();
        float animationDelta = Math.max(0f, time - lastPlayhead);

        this.lastPlayhead = time;
        shownPuppets.clear();
        shownItems.clear();
        shownVehicles.clear();

        if (RecorderChannel.CAMERA.isIn(played))
            playCamera(recording.findPlayingClip(RecorderChannel.CAMERA, time), time);

        if (RecorderChannel.PLAYER.isIn(played))
            playPlayer(recording.findPlayingClip(RecorderChannel.PLAYER, time), time, animationDelta);

        if (RecorderChannel.CHARACTERS.isIn(played))
            playCharacters(recording.findPlayingClip(RecorderChannel.CHARACTERS, time), time, animationDelta);

        if (RecorderChannel.ITEMS.isIn(played))
            playItems(recording.findPlayingClip(RecorderChannel.ITEMS, time), time);

        if (RecorderChannel.VEHICLES.isIn(played))
            playVehicles(recording.findPlayingClip(RecorderChannel.VEHICLES, time), time);

        if (RecorderChannel.WEATHER.isIn(played))
            playWeather(recording.findPlayingClip(RecorderChannel.WEATHER, time), time);

        if (RecorderChannel.TIME.isIn(played))
            playClock(recording.findPlayingClip(RecorderChannel.TIME, time), time);
        else
            this.clockHeld = false;

        playerManager.pushPlayerPositionForWindow(context.getWindow().getWindowID());
    }

    // The anchor stands so its eye is on the camera, and the camera is placed in the frame of the anchor's chunk
    private void playCamera(RecordingClipStruct clip, float time) {

        if (clip == null || clip.getTake().getCameraTrack().isEmpty())
            return;

        RecordingCameraTrackStruct track = clip.getTake().getCameraTrack();
        float takeTime = clip.toTakeTime(time);
        int sample = Math.max(0, track.findSample(takeTime));
        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, takeTime);
        int windowID = context.getWindow().getWindowID();
        EntityInstance anchor = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        WorldHandle worldHandle = resolveWorld();
        WorldPositionStruct anchorPlace = anchor.getWorldPositionStruct();

        RecordingCurveUtility.resolvePlace(worldHandle, track, takeTime, sample, blend, placeScratch);
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
        camera.setDirection(RecordingCurveUtility.resolveDirection(track, takeTime, sample, blend, directionScratch));
        camera.setShake(
                RecordingCurveUtility.resolveFloat(
                        track, RecorderCurveProperty.SHAKE_YAW, takeTime, track.getShakeYaws(), sample, blend),
                RecordingCurveUtility.resolveFloat(
                        track, RecorderCurveProperty.SHAKE_PITCH, takeTime, track.getShakePitches(), sample, blend),
                RecordingCurveUtility.resolveFloat(
                        track, RecorderCurveProperty.SHAKE_ROLL, takeTime, track.getShakeRolls(), sample, blend));
        camera.setFOV(RecordingCurveUtility.resolveFloat(
                track, RecorderCurveProperty.FIELD_OF_VIEW, takeTime, track.getFovs(), sample, blend));
    }

    private void playPlayer(RecordingClipStruct clip, float time, float animationDelta) {
        if (clip != null && clip.getTake().hasPlayerTrack())
            posePuppet(clip.getTake().getPlayerTrack(), clip.toTakeTime(time), animationDelta);
    }

    private void playCharacters(RecordingClipStruct clip, float time, float animationDelta) {

        if (clip == null)
            return;

        ObjectArrayList<RecordingBodyTrackStruct> residentTracks = clip.getTake().getResidentTracks();
        float takeTime = clip.toTakeTime(time);

        for (int i = 0; i < residentTracks.size(); i++)
            posePuppet(residentTracks.get(i), takeTime, animationDelta);
    }

    private void playItems(RecordingClipStruct clip, float time) {

        if (clip == null)
            return;

        ObjectArrayList<RecordingProjectileTrackStruct> projectileTracks = clip.getTake().getProjectileTracks();
        float takeTime = clip.toTakeTime(time);

        for (int i = 0; i < projectileTracks.size(); i++)
            placeItem(projectileTracks.get(i), takeTime);
    }

    private void playVehicles(RecordingClipStruct clip, float time) {

        if (clip == null)
            return;

        ObjectArrayList<RecordingVehicleTrackStruct> vehicleTracks = clip.getTake().getVehicleTracks();
        float takeTime = clip.toTakeTime(time);

        for (int i = 0; i < vehicleTracks.size(); i++)
            poseVehicle(vehicleTracks.get(i), takeTime);
    }

    // The weather and the gusts held where the take had them; with no clip to play them they go on live
    private void playWeather(RecordingClipStruct clip, float time) {

        if (clip == null || clip.getTake().getWeatherTrack().isEmpty()) {
            releaseWeather();
            return;
        }

        RecordingWeatherTrackStruct track = clip.getTake().getWeatherTrack();
        float takeTime = clip.toTakeTime(time);
        int sample = track.findSample(takeTime);
        float blend = track.resolveBlend(sample, takeTime);

        weatherManager.holdFlow(
                RecordingCurveUtility.resolveDouble(
                        track, RecorderCurveProperty.DRIFT_X, takeTime, track.getFlowsX(), sample, blend),
                RecordingCurveUtility.resolveDouble(
                        track, RecorderCurveProperty.DRIFT_Z, takeTime, track.getFlowsZ(), sample, blend));
        windManager.holdGustTime(RecordingCurveUtility.resolveFloat(
                track, RecorderCurveProperty.GUST, takeTime, track.getGustTimes(), sample, blend));
        this.weatherHeld = true;
    }

    // The clock follows the recording; a jump nothing here made — a time command — shifts it from then on
    private void playClock(RecordingClipStruct clip, float time) {

        if (clip == null) {
            this.clockHeld = false;
            return;
        }

        if (clockHeld && !recorderManager.isSuspended()) {

            long expected = heldElapsedMillis + (internal.getTime() - heldAtTime);
            long moved = clockManager.getElapsedMillis() - expected;

            if (Math.abs(moved) >= EditorSetting.RECORDER_CLOCK_JUMP_MILLIS)
                liveClockOffsetMillis += moved;
        }

        long elapsed = clip.getTake().getClockTrack().evaluate(clip.toTakeTime(time), clip.getTimeRampSeconds())
                + liveClockOffsetMillis;

        clockManager.holdClock(elapsed);
        this.heldElapsedMillis = elapsed;
        this.heldAtTime = internal.getTime();
        this.clockHeld = true;
    }

    // Puppets \\

    private void posePuppet(RecordingBodyTrackStruct track, float takeTime, float animationDelta) {

        int sample = track.findSample(takeTime);

        if (!RecordingSampleUtility.isPresent(track.getTimes(), sample, takeTime))
            return;

        EntityInstance puppet = resolvePuppet(track);

        if (puppet == null)
            return;

        dressPuppet(track, puppet, takeTime);

        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, takeTime);
        EntityStateHandle state = puppet.getEntityStateHandle();

        RecordingCurveUtility.resolvePlace(resolveWorld(), track, takeTime, sample, blend, placeScratch);
        puppet.setLocation(placeScratch.getPosition(), placeScratch.getChunkCoordinate());
        state.setMovementState(EntityState.VALUES[track.getStates().getInt(sample)]);
        state.setSpeed(
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.GROUND_SPEED, takeTime,
                        track.getHorizontalSpeeds(), sample, blend),
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.RISE_SPEED, takeTime,
                        track.getVerticalSpeeds(), sample, blend));
        state.setBodyYaw(
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.FACING, takeTime,
                        track.getBodyYaws(), sample, blend),
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.TURN_RATE, takeTime,
                        track.getBodyYawRates(), sample, blend));
        state.setLook(
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.LOOK_PITCH, takeTime,
                        track.getLookPitches(), sample, blend),
                RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.LOOK_YAW, takeTime,
                        track.getLookYaws(), sample, blend));
        state.setGroundOffset(RecordingCurveUtility.resolveFloat(track, RecorderCurveProperty.GROUND_OFFSET, takeTime,
                track.getGroundOffsets(), sample, blend));

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
    private void dressPuppet(RecordingBodyTrackStruct track, EntityInstance puppet, float takeTime) {

        int look = track.findLook(takeTime);

        if (look == EngineSetting.INDEX_NOT_FOUND || look == track2AppliedLook.getInt(track))
            return;

        track2AppliedLook.put(track, look);

        if (!saveManager.applyLook(track.getLooks().get(look), puppet))
            errorLog(EditorSetting.RECORDER_MESSAGE_LOOK_REFUSED + track.getDisplayName());
    }

    // Thrown Items \\

    private void placeItem(RecordingProjectileTrackStruct track, float takeTime) {

        int sample = track.findSample(takeTime);

        if (!RecordingSampleUtility.isPresent(track.getTimes(), sample, takeTime))
            return;

        ItemDefinitionHandle item = resolveItem(track);

        if (item == null)
            return;

        int slot = shownItems.size();

        if (slot == shownItemPlaces.size()) {
            shownItemPlaces.add(new WorldPositionStruct());
            shownItemOrientations.add(new Quaternion());
        }

        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, takeTime);

        RecordingCurveUtility.resolvePlace(resolveWorld(), track, takeTime, sample, blend, shownItemPlaces.get(slot));
        RecordingCurveUtility.resolveOrientation(
                track,
                takeTime,
                track.getOrientationsW(),
                track.getOrientationsX(),
                track.getOrientationsY(),
                track.getOrientationsZ(),
                sample,
                blend,
                shownItemOrientations.get(slot));
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

    // Vehicles \\

    // The vehicle's puppet laid where the take had it, its rig standing as it stood
    private void poseVehicle(RecordingVehicleTrackStruct track, float takeTime) {

        int sample = track.findSample(takeTime);

        if (!RecordingSampleUtility.isPresent(track.getTimes(), sample, takeTime))
            return;

        VehicleInstance puppet = resolveVehicle(track);

        if (puppet == null)
            return;

        float blend = RecordingSampleUtility.resolveBlend(track.getTimes(), sample, takeTime);
        VehicleHandle vehicleHandle = puppet.getVehicleHandle();

        RecordingCurveUtility.resolvePlace(
                resolveWorld(), track, takeTime, sample, blend, puppet.getWorldPositionStruct());
        RecordingCurveUtility.resolveOrientation(
                track,
                takeTime,
                track.getOrientationsW(),
                track.getOrientationsX(),
                track.getOrientationsY(),
                track.getOrientationsZ(),
                sample,
                blend,
                puppet.getOrientation());
        puppet.refreshRotation();
        puppet.setRudderAngle(RecordingCurveUtility.resolveFloat(
                track, RecorderCurveProperty.RUDDER, takeTime, track.getRudderAngles(), sample, blend));
        puppet.setAnchored(track.getAnchored().getInt(sample) != 0);

        for (int i = 0; i < Math.min(track.getMastCount(), vehicleHandle.getMastCount()); i++)
            puppet.setBraceAngle(i, RecordingCurveUtility.resolvePart(track, RecorderCurveProperty.BRACE, i,
                    takeTime, track.getBraceAngles(), track.getMastCount(), sample, blend));

        for (int i = 0; i < Math.min(track.getSailCount(), vehicleHandle.getSailCount()); i++)
            puppet.setSailHoist(i, RecordingCurveUtility.resolvePart(track, RecorderCurveProperty.HOIST, i,
                    takeTime, track.getSailHoists(), track.getSailCount(), sample, blend));

        for (int i = 0; i < Math.min(track.getDoorCount(), vehicleHandle.getDoorCount()); i++)
            puppet.setDoorOpening(i, RecordingCurveUtility.resolvePart(track, RecorderCurveProperty.DOOR, i,
                    takeTime, track.getDoorOpenings(), track.getDoorCount(), sample, blend));

        shownVehicles.add(puppet);
    }

    // The puppet a vehicle track plays through, made the first time it is seen; a type that no longer exists is
    // reported once and the track left unseen
    private VehicleInstance resolveVehicle(RecordingVehicleTrackStruct track) {

        VehicleInstance puppet = track2Vehicle.get(track);

        if (puppet != null || refusedTracks.contains(track))
            return puppet;

        try {
            puppet = vehicleManager.createPuppet(
                    vehicleManager.getVehicleHandleFromVehicleName(track.getVehicleName()),
                    resolveWorld(),
                    track.getChunkCoordinate(0),
                    eyeScratch.set(track.getPositionX(0), track.getPositionY(0), track.getPositionZ(0)),
                    new Quaternion(
                            track.getOrientationsW().getFloat(0),
                            track.getOrientationsX().getFloat(0),
                            track.getOrientationsY().getFloat(0),
                            track.getOrientationsZ().getFloat(0)));
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_NO_VEHICLE + track.getVehicleName());
            refusedTracks.add(track);
            return null;
        }

        track2Vehicle.put(track, puppet);
        return puppet;
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

        for (int i = 0; i < shownVehicles.size(); i++)
            vehicleRenderSystem.pushVehicle(
                    shownVehicles.get(i), anchor.getWorldHandle(), viewerChunk, worldFbo, context.getWindow());
    }

    // Capture \\

    private void capture() {

        RecordingTakeStruct take = recorderManager.getTake();
        float time = recorderManager.getTakeTime();

        if (isDue(time, lastSampleTime, EditorSetting.RECORDER_SAMPLE_INTERVAL)) {
            this.lastSampleTime = time;
            captureCamera(take, time);
            capturePlayer(take, time);
            captureClock(take, time);
        }

        if (isDue(time, lastWorldSampleTime, EditorSetting.RECORDER_WORLD_SAMPLE_INTERVAL)) {
            this.lastWorldSampleTime = time;
            captureResidents(take, time);
            captureProjectiles(take, time);
            captureVehicles(take, time);
        }

        if (isDue(time, lastWeatherSampleTime, EditorSetting.RECORDER_WEATHER_SAMPLE_INTERVAL)) {
            this.lastWeatherSampleTime = time;
            captureWeather(take, time);
        }
    }

    private boolean isDue(float time, float lastTime, float interval) {
        return lastTime < 0f || time - lastTime >= interval - EditorSetting.RECORDER_SAMPLE_TOLERANCE;
    }

    // The camera as it stands in the frame of the chunk the window's player stands in
    private void captureCamera(RecordingTakeStruct take, float time) {

        if (!take.holds(RecorderChannel.CAMERA))
            return;

        int windowID = context.getWindow().getWindowID();

        take.getCameraTrack().addSample(
                time,
                playerManager.getPlayerForWindow(windowID).getWorldPositionStruct().getChunkCoordinate(),
                playerManager.getCameraForWindow(windowID));
    }

    // The character played, never the camera's anchor while it flies free
    private void capturePlayer(RecordingTakeStruct take, float time) {

        int windowID = context.getWindow().getWindowID();

        if (!take.holds(RecorderChannel.PLAYER) || playerManager.isFreeCameraForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        if (!take.hasPlayerTrack())
            take.setPlayerTrack(createBodyTrack(player, EditorSetting.RECORDER_PLAYER_DISPLAY_NAME));

        if (take.getPlayerTrack() != null)
            captureBody(take.getPlayerTrack(), player, time);
    }

    private void captureClock(RecordingTakeStruct take, float time) {
        if (take.holds(RecorderChannel.TIME))
            take.getClockTrack().addSample(time, clockManager.getElapsedMillis());
    }

    private void captureWeather(RecordingTakeStruct take, float time) {
        if (take.holds(RecorderChannel.WEATHER))
            take.getWeatherTrack().addSample(
                    time,
                    weatherManager.getFlowOffsetXBlocks(),
                    weatherManager.getFlowOffsetZBlocks(),
                    windManager.getGustTime());
    }

    // Every resident near the window, each its own track from the first time it is seen; one that falls drops a
    // marker on the timeline where it fell
    private void captureResidents(RecordingTakeStruct take, float time) {

        if (!take.holds(RecorderChannel.CHARACTERS))
            return;

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
                recorderManager.addMarker(
                        recorderManager.getPlayhead(),
                        EditorSetting.RECORDER_KILL_MARKER_PREFIX + track.getDisplayName());
            }
        }
    }

    private void captureProjectiles(RecordingTakeStruct take, float time) {

        if (!take.holds(RecorderChannel.ITEMS))
            return;

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

    // Every vehicle near the window, each its own track from the first time it is seen
    private void captureVehicles(RecordingTakeStruct take, float time) {

        if (!take.holds(RecorderChannel.VEHICLES))
            return;

        ObjectArrayList<VehicleInstance> vehicles = vehicleManager.getVehicles();
        EntityInstance viewer = playerManager.getPlayerForWindow(context.getWindow().getWindowID());

        for (int i = 0; i < vehicles.size(); i++) {

            VehicleInstance vehicle = vehicles.get(i);

            if (!isNear(vehicle.getWorldHandle(), vehicle.getWorldPositionStruct().getChunkCoordinate(), viewer,
                    EngineSetting.VEHICLE_RENDER_CHUNK_RADIUS))
                continue;

            RecordingVehicleTrackStruct track = vehicle2Track.get(vehicle);

            if (track == null) {

                VehicleHandle vehicleHandle = vehicle.getVehicleHandle();

                track = new RecordingVehicleTrackStruct(
                        vehicleHandle.getVehicleName(),
                        vehicleHandle.getMastCount(),
                        vehicleHandle.getSailCount(),
                        vehicleHandle.getDoorCount());
                vehicle2Track.put(vehicle, track);
                take.getVehicleTracks().add(track);
            }

            track.addSample(time, vehicle);
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
