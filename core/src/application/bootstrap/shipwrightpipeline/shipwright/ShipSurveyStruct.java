package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.StructPackage;

public class ShipSurveyStruct extends StructPackage {

    /*
     * What the drafting table reports about a compiled ship: the triangles it
     * draws once greedily meshed against the most a ship may draw, the speed
     * it makes running before the reference wind under every sail in blocks
     * per second, its mass in tonnes, its length, beam and draft in blocks,
     * how many sails it sets, and the shape of its bow below the waterline —
     * the angle its entry opens at in degrees and how full its entrance is.
     * Refilled in place after every compile.
     */

    // Mesh
    private int triangles;

    // Performance
    private float speed;
    private float mass;

    // Dimensions — blocks
    private float length;
    private float beam;
    private float draft;

    // Rig
    private int sailCount;

    // Bow
    private float entryAngleDegrees;
    private float entranceCoefficient;

    // Management \\

    public void set(
            int triangles,
            float speed,
            float mass,
            float length,
            float beam,
            float draft,
            int sailCount,
            float entryAngleDegrees,
            float entranceCoefficient) {

        // Mesh
        this.triangles = triangles;

        // Performance
        this.speed = speed;
        this.mass = mass;

        // Dimensions
        this.length = length;
        this.beam = beam;
        this.draft = draft;

        // Rig
        this.sailCount = sailCount;

        // Bow
        this.entryAngleDegrees = entryAngleDegrees;
        this.entranceCoefficient = entranceCoefficient;
    }

    // Accessible \\

    public int getTriangles() {
        return triangles;
    }

    public float getSpeed() {
        return speed;
    }

    public float getMass() {
        return mass;
    }

    public float getLength() {
        return length;
    }

    public float getBeam() {
        return beam;
    }

    public float getDraft() {
        return draft;
    }

    public int getSailCount() {
        return sailCount;
    }

    public float getEntryAngleDegrees() {
        return entryAngleDegrees;
    }

    public float getEntranceCoefficient() {
        return entranceCoefficient;
    }
}
