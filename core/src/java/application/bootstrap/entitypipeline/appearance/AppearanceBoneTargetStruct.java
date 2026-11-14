package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class AppearanceBoneTargetStruct extends StructPackage {

    /*
     * One bone a shape slider drives, by how much per unit of the slider's
     * value: a move of its joint in blocks under its parent, a turn in
     * degrees on its bind rotation, and a scale of its own skin as a
     * fraction, so a value of one moves the joint by the whole move and
     * grows the bone by the whole scale, and a negative value the other way.
     */

    // Identity
    private final int boneIndex;

    // Amounts — per unit of the slider's value
    private final Vector3 move;
    private final Vector3 turn;
    private final Vector3 scale;

    // Constructor \\

    public AppearanceBoneTargetStruct(int boneIndex, Vector3 move, Vector3 turn, Vector3 scale) {

        // Identity
        this.boneIndex = boneIndex;

        // Amounts
        this.move = move;
        this.turn = turn;
        this.scale = scale;
    }

    // Accessible \\

    public int getBoneIndex() {
        return boneIndex;
    }

    public Vector3 getMove() {
        return move;
    }

    public Vector3 getTurn() {
        return turn;
    }

    public Vector3 getScale() {
        return scale;
    }
}
