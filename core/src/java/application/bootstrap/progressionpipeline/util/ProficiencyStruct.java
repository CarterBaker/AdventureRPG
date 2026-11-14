package application.bootstrap.progressionpipeline.util;

import engine.root.StructPackage;

public class ProficiencyStruct extends StructPackage {

    /*
     * One ranged value a being grows and loses: a trade, a skill, or its
     * morality. Holds the value, the highest it has ever reached, when it was
     * last used, and the moment its decay has been settled up to, both in game
     * seconds, so decay over any stretch of time — including time spent away
     * while the world clock ran on — is settled exactly in one step. Every
     * change goes through ProficiencyUtility.
     */

    // Value
    private float value;
    private float peak;

    // Time
    private double lastUsedTime;
    private double settledTime;

    // Constructor \\

    public ProficiencyStruct() {
    }

    // Management \\

    public void reset(float value, double time) {
        this.value = value;
        this.peak = Math.max(0f, value);
        this.lastUsedTime = time;
        this.settledTime = time;
    }

    // Accessible \\

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public float getPeak() {
        return peak;
    }

    public void setPeak(float peak) {
        this.peak = peak;
    }

    public double getLastUsedTime() {
        return lastUsedTime;
    }

    public void setLastUsedTime(double lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }

    public double getSettledTime() {
        return settledTime;
    }

    public void setSettledTime(double settledTime) {
        this.settledTime = settledTime;
    }
}
