package application.bootstrap.entitypipeline.player;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class PlayerPhotoStruct extends StructPackage {

    /*
     * Per-window state of photo mode's camera and subject: where the camera
     * floats, free of the player and held within reach of them, the pose clip
     * the player holds, none to keep the stance they were caught in, and
     * whether the player is left out of the picture. Owned by PlayerManager
     * while the window is in photo mode.
     */

    // Camera
    private final Vector3 cameraPosition;

    // Subject
    private AnimationClipHandle pose;
    private boolean playerHidden;

    // Constructor \\

    public PlayerPhotoStruct(Vector3 cameraPosition) {

        // Camera
        this.cameraPosition = new Vector3(cameraPosition);
    }

    // Accessible \\

    public Vector3 getCameraPosition() {
        return cameraPosition;
    }

    public AnimationClipHandle getPose() {
        return pose;
    }

    public void setPose(AnimationClipHandle pose) {
        this.pose = pose;
    }

    public boolean isPlayerHidden() {
        return playerHidden;
    }

    public void setPlayerHidden(boolean playerHidden) {
        this.playerHidden = playerHidden;
    }
}
