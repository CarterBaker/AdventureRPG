package application.bootstrap.entitypipeline.player;

import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import engine.root.StructPackage;

public class MenuViewStruct extends StructPackage {

    /*
     * Per-window state of the view a main menu looks out from: the camera
     * chosen for it as the menu opened, and the block column the window's
     * player was last carried to so the world streams around the view,
     * none until it has first been carried. Owned by PlayerManager for as
     * long as its window's menu looks out.
     */

    // Camera
    private final WorldPlacementStruct camera;

    // Anchor
    private boolean anchored;
    private long anchorX;
    private long anchorZ;

    // Constructor \\

    public MenuViewStruct(WorldPlacementStruct camera) {
        this.camera = camera;
    }

    // Management \\

    public void anchor(long anchorX, long anchorZ) {
        this.anchored = true;
        this.anchorX = anchorX;
        this.anchorZ = anchorZ;
    }

    // Accessible \\

    public WorldPlacementStruct getCamera() {
        return camera;
    }

    // True once the player has been carried to this block column
    public boolean isAnchoredAt(long columnX, long columnZ) {
        return anchored && anchorX == columnX && anchorZ == columnZ;
    }
}
