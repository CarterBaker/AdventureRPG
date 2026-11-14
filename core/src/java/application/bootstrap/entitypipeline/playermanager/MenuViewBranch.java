package application.bootstrap.entitypipeline.playermanager;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.player.MenuViewStruct;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.assets.camera.CameraInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MenuViewBranch extends BranchPackage {

    /*
     * Seats a window's camera while its main menu looks out over the world.
     * The view is one of the world's hand-placed cameras, chosen at random as
     * the menu opens, or in a world without one the default view over the
     * column its player stands on. A camera the editor pins is shown by every
     * menu instead, followed live as it is edited. The camera is raised its
     * height above the player, whom PlayerManager carries to the camera's
     * column and stands on its ground, and turned by its yaw and pitch.
     */

    // Internal
    private WorldManager worldManager;

    // Pin
    private WorldPlacementStruct pinnedCamera;

    // Scratch
    private ObjectArrayList<WorldPlacementStruct> cameras;
    private Vector3 viewPosition;
    private Vector3 viewDirection;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.cameras = new ObjectArrayList<>();
        this.viewPosition = new Vector3();
        this.viewDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    // Choose \\

    // One of the player's world's cameras at random, or the default view over the column the player stands on
    WorldPlacementStruct chooseCamera(EntityInstance player) {

        WorldPlacementStruct[] placements = worldManager.getPlacements(player.getWorldHandle());

        cameras.clear();

        for (int i = 0; i < placements.length; i++)
            if (placements[i].getKind() == WorldPlacementKind.CAMERA)
                cameras.add(placements[i]);

        if (!cameras.isEmpty())
            return cameras.get(ThreadLocalRandom.current().nextInt(cameras.size()));

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        long chunkCoordinate = worldPositionStruct.getChunkCoordinate();
        Vector3 position = worldPositionStruct.getPosition();

        return new WorldPlacementStruct(
                WorldPlacementKind.CAMERA,
                "",
                "",
                EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL,
                (long) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE
                        + (long) Math.floor(position.x),
                (long) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE
                        + (long) Math.floor(position.z),
                0,
                EngineSetting.MENU_CAMERA_DEFAULT_HEIGHT,
                EngineSetting.MENU_CAMERA_DEFAULT_YAW,
                EngineSetting.MENU_CAMERA_DEFAULT_PITCH);
    }

    // Pin \\

    // Every menu view shows this camera instead of its own, until it is let go with null
    void pin(WorldPlacementStruct camera) {
        this.pinnedCamera = camera;
    }

    // The camera a menu view shows: the pinned one over the one chosen for it
    WorldPlacementStruct resolveCamera(MenuViewStruct menuView) {
        return pinnedCamera != null ? pinnedCamera : menuView.getCamera();
    }

    // Seat \\

    // The camera raised its height above the player's feet, looking along its yaw and pitch
    void seat(EntityInstance player, CameraInstance camera, WorldPlacementStruct shown) {

        double yaw = Math.toRadians(shown.getYaw());
        double pitch = Math.toRadians(shown.getPitch());
        float level = (float) Math.cos(pitch);

        viewPosition.set(player.getWorldPositionStruct().getPosition()).add(0f, shown.getHeight(), 0f);
        viewDirection.set(
                (float) Math.sin(yaw) * level,
                (float) Math.sin(pitch),
                (float) Math.cos(yaw) * level);

        camera.setPosition(viewPosition);
        camera.setDirection(viewDirection);
    }
}
