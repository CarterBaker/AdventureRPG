package editor.runtime.structureeditor.tool;

import engine.root.StructPackage;
import engine.util.mathematics.extras.Direction3Vector;

public class StructureEditorHitStruct extends StructPackage {

    /*
     * Where the pointer's ray meets the structure: the cell struck and the
     * face it entered through, or, when it strikes no cell, the cell just
     * under the ground plane it crosses with its top face, so the Brush builds
     * on the ground. Reused every frame.
     */

    // Hit
    private boolean hit;
    private int cellX;
    private int cellY;
    private int cellZ;
    private Direction3Vector face;

    // Management \\

    public void clear() {
        this.hit = false;
    }

    public void set(int x, int y, int z, Direction3Vector enteredFace) {

        this.hit = true;
        this.cellX = x;
        this.cellY = y;
        this.cellZ = z;
        this.face = enteredFace;
    }

    // Accessible \\

    public boolean hasHit() {
        return hit;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public int getCellZ() {
        return cellZ;
    }

    public Direction3Vector getFace() {
        return face;
    }
}
