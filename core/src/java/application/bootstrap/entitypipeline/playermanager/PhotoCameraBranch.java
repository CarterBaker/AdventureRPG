package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.player.PlayerPhotoStruct;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import engine.assets.camera.CameraInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class PhotoCameraBranch extends BranchPackage {

    /*
     * Photo mode's free camera, as a modern photo mode floats one: it flies
     * on the player's own movement keys the way the free camera does, looks
     * where the mouse turns it, and is held within PHOTO_CAMERA_REACH_BLOCKS
     * of the player's eyes, so a shot can be framed from any side without
     * leaving the world the player stands in. The player never moves while it
     * flies.
     */

    // Internal
    private MovementManager movementManager;

    // Cached Vectors
    private Vector3 flight;

    // Internal \\

    @Override
    protected void create() {

        // Cached Vectors
        this.flight = new Vector3();
    }

    @Override
    protected void get() {
        this.movementManager = get(MovementManager.class);
    }

    // Camera \\

    // The camera carried by the player's flight input, then held within reach of their eyes
    void fly(EntityInstance player, PlayerPhotoStruct photo, Vector3 eyePosition, CameraInstance camera) {

        Vector3 position = photo.getCameraPosition();

        movementManager.resolveFlight(player, flight);
        position.add(flight);

        float offsetX = position.x - eyePosition.x;
        float offsetY = position.y - eyePosition.y;
        float offsetZ = position.z - eyePosition.z;
        float distance = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);

        if (distance > EngineSetting.PHOTO_CAMERA_REACH_BLOCKS) {
            float scale = EngineSetting.PHOTO_CAMERA_REACH_BLOCKS / distance;
            position.set(
                    eyePosition.x + offsetX * scale,
                    eyePosition.y + offsetY * scale,
                    eyePosition.z + offsetZ * scale);
        }

        camera.setPosition(position);
        player.getEntityInputHandle().clear();
    }
}
