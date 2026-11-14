package application.bootstrap.entitypipeline.player;

import engine.root.StructPackage;

public class PlayerCameraStruct extends StructPackage {

    /*
     * Per-window state of the shake CameraShakeBranch lays over a player's
     * view: the trauma its jolts have built, 0 calm to 1 the hardest shake,
     * and the clock the shake's noise runs on; and what the player was last
     * frame — in the air and how fast it fell, its health, the action it last
     * began — so a landing, a hit or a stagger reads as the change it is.
     * Nothing is compared until the first frame has been watched. Owned by
     * PlayerManager for as long as its window's player lives.
     */

    // Shake
    private float trauma;
    private float shakeTime;

    // Watch
    private boolean watched;
    private boolean airborne;
    private float fallSpeed;
    private float health;
    private int actionSequence;

    // Management \\

    public void addTrauma(float amount) {
        trauma = Math.min(1f, trauma + amount);
    }

    public void advance(float deltaTime, float decay, float period) {
        trauma = Math.max(0f, trauma - decay * deltaTime);
        shakeTime = (shakeTime + deltaTime) % period;
    }

    public void watch(boolean airborne, float fallSpeed, float health, int actionSequence) {
        this.watched = true;
        this.airborne = airborne;
        this.fallSpeed = fallSpeed;
        this.health = health;
        this.actionSequence = actionSequence;
    }

    public void reset() {
        trauma = 0f;
        watched = false;
    }

    // Accessible \\

    public float getTrauma() {
        return trauma;
    }

    public float getShakeTime() {
        return shakeTime;
    }

    public boolean isWatched() {
        return watched;
    }

    public boolean isAirborne() {
        return airborne;
    }

    public float getFallSpeed() {
        return fallSpeed;
    }

    public float getHealth() {
        return health;
    }

    public int getActionSequence() {
        return actionSequence;
    }
}
