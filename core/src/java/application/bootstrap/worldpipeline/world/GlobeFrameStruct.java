package application.bootstrap.worldpipeline.world;

import engine.root.StructPackage;

public class GlobeFrameStruct extends StructPackage {

    /*
     * Where one cell of a grid's chart stands on a globe's block cube: the
     * canonical coordinate of the face cell, its face packed into x, and the
     * quarter turns carrying a vector along the chart's axes into that face's
     * own axes, each turn taking +X to +Z. A torus world's frame is its own
     * coordinate, unturned. A fold also leaves the face it ended on and the
     * folded point's place on it, doubled about the face centre.
     */

    // Frame
    private long canonicalCoordinate;
    private int turns;

    // Fold
    private int face;
    private double foldA;
    private double foldB;

    // Accessible \\

    public void set(long canonicalCoordinate, int turns) {
        this.canonicalCoordinate = canonicalCoordinate;
        this.turns = turns;
    }

    public void setFold(int face, double foldA, double foldB, int turns) {
        this.face = face;
        this.foldA = foldA;
        this.foldB = foldB;
        this.turns = turns;
    }

    public long getCanonicalCoordinate() {
        return canonicalCoordinate;
    }

    public int getTurns() {
        return turns;
    }

    public int getFace() {
        return face;
    }

    public double getFoldA() {
        return foldA;
    }

    public double getFoldB() {
        return foldB;
    }

    public boolean matches(long canonicalCoordinate, int turns) {
        return this.canonicalCoordinate == canonicalCoordinate && this.turns == turns;
    }
}
