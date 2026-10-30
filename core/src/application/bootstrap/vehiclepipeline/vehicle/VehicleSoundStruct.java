package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.soundpipeline.soundcue.SoundLoopInstance;
import engine.root.StructPackage;

public class VehicleSoundStruct extends StructPackage {

    /*
     * What one vehicle is heard by: the loops of its hull, its wake, its sails
     * and its rigging, held by VehicleSoundBranch every frame it floats, and
     * how long until its hull may sound against the ground again.
     */

    // Loops
    private final SoundLoopInstance hullLoop;
    private final SoundLoopInstance wakeLoop;
    private final SoundLoopInstance sailLoop;
    private final SoundLoopInstance riggingLoop;

    // Ground
    private float groundCooldown;

    // Constructor \\

    public VehicleSoundStruct(
            SoundLoopInstance hullLoop,
            SoundLoopInstance wakeLoop,
            SoundLoopInstance sailLoop,
            SoundLoopInstance riggingLoop) {

        // Loops
        this.hullLoop = hullLoop;
        this.wakeLoop = wakeLoop;
        this.sailLoop = sailLoop;
        this.riggingLoop = riggingLoop;

        // Ground
        this.groundCooldown = 0f;
    }

    // Accessible \\

    public SoundLoopInstance getHullLoop() {
        return hullLoop;
    }

    public SoundLoopInstance getWakeLoop() {
        return wakeLoop;
    }

    public SoundLoopInstance getSailLoop() {
        return sailLoop;
    }

    public SoundLoopInstance getRiggingLoop() {
        return riggingLoop;
    }

    public float getGroundCooldown() {
        return groundCooldown;
    }

    public void setGroundCooldown(float groundCooldown) {
        this.groundCooldown = groundCooldown;
    }
}
