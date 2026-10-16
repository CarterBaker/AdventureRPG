package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.StructPackage;

public class ShipSurveyStruct extends StructPackage {

    /*
     * What the drafting table reports about a compiled ship, so a design can
     * be judged before it is built: the triangles it draws once greedily
     * meshed; the speed it makes running before a fair breeze and before a
     * gale under every sail, in blocks per second; what it weighs and the
     * ballast it carries in tonnes; its length, beam, the draft it floats at
     * and the freeboard left before the sea comes aboard, in blocks; how much
     * more than its weight its hull could float before it sank, as a share of
     * its weight; how far it can heel and still right itself, in degrees, 0
     * for a ship that capsizes as it is launched; how many sails it sets; and
     * the angle its bow opens at below the waterline in degrees. Refilled in
     * place once the ship has rested after every change.
     */

    // Mesh
    private int triangles;

    // Speed — blocks per second
    private float breezeSpeed;
    private float galeSpeed;

    // Weight — tonnes
    private float mass;
    private float ballast;

    // Dimensions — blocks
    private float length;
    private float beam;
    private float draft;
    private float freeboard;

    // Buoyancy
    private float reserve;

    // Stability — degrees
    private float stableDegrees;

    // Rig
    private int sailCount;

    // Bow — degrees
    private float entryDegrees;

    // Management \\

    public void set(
            int triangles,
            float breezeSpeed,
            float galeSpeed,
            float mass,
            float ballast,
            float length,
            float beam,
            float draft,
            float freeboard,
            float reserve,
            float stableDegrees,
            int sailCount,
            float entryDegrees) {

        // Mesh
        this.triangles = triangles;

        // Speed
        this.breezeSpeed = breezeSpeed;
        this.galeSpeed = galeSpeed;

        // Weight
        this.mass = mass;
        this.ballast = ballast;

        // Dimensions
        this.length = length;
        this.beam = beam;
        this.draft = draft;
        this.freeboard = freeboard;

        // Buoyancy
        this.reserve = reserve;

        // Stability
        this.stableDegrees = stableDegrees;

        // Rig
        this.sailCount = sailCount;

        // Bow
        this.entryDegrees = entryDegrees;
    }

    // Accessible \\

    public int getTriangles() {
        return triangles;
    }

    public float getBreezeSpeed() {
        return breezeSpeed;
    }

    public float getGaleSpeed() {
        return galeSpeed;
    }

    public float getMass() {
        return mass;
    }

    public float getBallast() {
        return ballast;
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

    public float getFreeboard() {
        return freeboard;
    }

    // How much more than its own weight the hull could float, as a share of its weight
    public float getReserve() {
        return reserve;
    }

    public float getStableDegrees() {
        return stableDegrees;
    }

    public int getSailCount() {
        return sailCount;
    }

    public float getEntryDegrees() {
        return entryDegrees;
    }
}
