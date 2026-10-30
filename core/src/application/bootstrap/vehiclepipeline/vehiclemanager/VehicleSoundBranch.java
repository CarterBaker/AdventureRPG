package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.soundpipeline.soundcue.SoundLoopInstance;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleSoundStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class VehicleSoundBranch extends BranchPackage {

    /*
     * Sounds every vehicle in the world. Each holds four loops from the moment
     * it is spawned until it leaves: its hull creaking always while it floats
     * and harder as it moves or turns, its wake rushing with its speed, its
     * sails luffing by how far they are set and how hard the wind blows, and
     * its rigging straining in that wind. A vehicle beyond the reach of any
     * of them holds them silent, giving their voices back. Its sails being
     * hoisted or set, its anchor, its helm, its doors and its hull striking
     * the ground each sound once where it floats, the ground no more often
     * than its cooldown allows.
     */

    // Internal
    private SoundManager soundManager;
    private SoundCueManager soundCueManager;
    private VehicleRigBranch vehicleRigBranch;

    // Cues
    private int hullCueID;
    private int wakeCueID;
    private int sailsCueID;
    private int riggingCueID;
    private int sailHoistCueID;
    private int sailSetCueID;
    private int anchorCueID;
    private int helmCueID;
    private int doorCueID;
    private int groundCueID;

    // Vehicles
    private Object2ObjectOpenHashMap<VehicleInstance, VehicleSoundStruct> vehicle2VehicleSound;

    // Internal \\

    @Override
    protected void create() {

        // Vehicles
        this.vehicle2VehicleSound = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.soundCueManager = get(SoundCueManager.class);
        this.vehicleRigBranch = get(VehicleRigBranch.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.hullCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_HULL);
        this.wakeCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_WAKE);
        this.sailsCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_SAILS);
        this.riggingCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_RIGGING);
        this.sailHoistCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SAIL_HOIST);
        this.sailSetCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SAIL_SET);
        this.anchorCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_ANCHOR);
        this.helmCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_HELM);
        this.doorCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_DOOR);
        this.groundCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIP_GROUND);
    }

    // Management \\

    void attach(VehicleInstance vehicle) {
        vehicle2VehicleSound.put(vehicle, new VehicleSoundStruct(
                createLoop(hullCueID),
                createLoop(wakeCueID),
                createLoop(sailsCueID),
                createLoop(riggingCueID)));
    }

    void detach(VehicleInstance vehicle) {

        VehicleSoundStruct sound = vehicle2VehicleSound.remove(vehicle);

        if (sound == null)
            return;

        sound.getHullLoop().stop();
        sound.getWakeLoop().stop();
        sound.getSailLoop().stop();
        sound.getRiggingLoop().stop();
    }

    private SoundLoopInstance createLoop(int cueID) {

        SoundLoopInstance loop = create(SoundLoopInstance.class);
        loop.constructor(cueID);

        return loop;
    }

    // Update \\

    // Every loop held at what the vehicle is doing, or silent while the world stands still or it is out of reach
    void update(VehicleInstance vehicle, boolean moving) {

        VehicleSoundStruct sound = vehicle2VehicleSound.get(vehicle);

        if (sound == null)
            return;

        sound.setGroundCooldown(Math.max(0f, sound.getGroundCooldown() - internal.getDeltaTime()));

        WorldHandle worldHandle = vehicle.getWorldHandle();
        WorldPositionStruct place = vehicle.getWorldPositionStruct();
        boolean heard = moving
                && soundManager.getListenerDistance(worldHandle, place) <= EngineSetting.SOUND_SHIP_HEARING_DISTANCE;
        Vector3 velocity = vehicle.getVelocity();
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wind = Math.min(1f, vehicleRigBranch.resolveWindSpeed(vehicle) / EngineSetting.SOUND_SHIP_WIND_FULL);
        float motion = Math.min(1f, Math.max(
                speed / EngineSetting.SOUND_SHIP_SPEED_FULL,
                vehicle.getAngularVelocity().length() / EngineSetting.SOUND_SHIP_TURN_FULL));

        hold(sound.getHullLoop(), heard,
                EngineSetting.SOUND_SHIP_HULL_REST + (1f - EngineSetting.SOUND_SHIP_HULL_REST) * motion,
                worldHandle, place);
        hold(sound.getWakeLoop(), heard, Math.min(1f, speed / EngineSetting.SOUND_SHIP_SPEED_FULL), worldHandle, place);
        hold(sound.getSailLoop(), heard, resolveSetShare(vehicle) * wind, worldHandle, place);
        hold(sound.getRiggingLoop(), heard, wind, worldHandle, place);
    }

    private void hold(
            SoundLoopInstance loop,
            boolean heard,
            float intensity,
            WorldHandle worldHandle,
            WorldPositionStruct place) {
        loop.holdAt(heard ? intensity : 0f, worldHandle, place);
    }

    // How far its sails are hoisted, all of them together
    private float resolveSetShare(VehicleInstance vehicle) {

        VehicleHandle vehicleHandle = vehicle.getVehicleHandle();
        int sailCount = vehicleHandle.getSailCount();

        if (sailCount == 0)
            return 0f;

        float hoisted = 0f;

        for (int sailIndex = 0; sailIndex < sailCount; sailIndex++)
            hoisted += vehicle.getSailHoist(sailIndex);

        return hoisted / sailCount;
    }

    // Moments \\

    void hoist(VehicleInstance vehicle, boolean set) {

        play(sailHoistCueID, 1f, vehicle);

        if (set)
            play(sailSetCueID, 1f, vehicle);
    }

    void anchor(VehicleInstance vehicle) {
        play(anchorCueID, 1f, vehicle);
    }

    void helm(VehicleInstance vehicle) {
        play(helmCueID, 1f, vehicle);
    }

    void door(VehicleInstance vehicle) {
        play(doorCueID, 1f, vehicle);
    }

    // The hull struck against the ground at the speed it met it, no sooner than the last strike has rung out
    void ground(VehicleInstance vehicle, float impactSpeed) {

        VehicleSoundStruct sound = vehicle2VehicleSound.get(vehicle);

        if (sound == null || sound.getGroundCooldown() > 0f
                || impactSpeed < EngineSetting.SOUND_SHIP_GROUND_SPEED_MIN)
            return;

        sound.setGroundCooldown(EngineSetting.SOUND_SHIP_GROUND_SECONDS);
        play(groundCueID, Math.min(1f, impactSpeed / EngineSetting.SOUND_SHIP_GROUND_SPEED_LOUD), vehicle);
    }

    private void play(int cueID, float volumeScale, VehicleInstance vehicle) {
        soundCueManager.playCueAt(cueID, volumeScale, vehicle.getWorldHandle(), vehicle.getWorldPositionStruct());
    }
}
