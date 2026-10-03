package application.runtime.postprocessing;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.physicspipeline.raycastmanager.RaycastManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class AutoFocusSystem extends SystemPackage {

    /*
     * Finds where this window's camera is looking for the depth of field. Each
     * frame a ray is cast from the camera along its view to the first solid
     * block; while the player's own body is in view — behind the player or
     * posed for a preview — the player is the subject, and the focus lands on
     * whichever of the two stands nearer. The focus eases toward its target in
     * inverse distance, the way a lens's focus ring turns, so a pull from a far
     * ridge to a nearby wall reads as one smooth rack of focus. Nothing within
     * reach focuses at infinity. No ray is cast while depth of field is off.
     */

    // Internal
    private RaycastManager raycastManager;
    private PlayerManager playerManager;
    private WorldSystem worldSystem;

    // Focus
    private final Vector3 lookDirection = new Vector3();
    private final BlockCastStruct castStruct = new BlockCastStruct();
    private float focusInverse;

    // Base \\

    @Override
    protected void get() {
        this.raycastManager = get(RaycastManager.class);
        this.playerManager = get(PlayerManager.class);
        this.worldSystem = get(WorldSystem.class);
    }

    @Override
    protected void update() {

        if (!settings.depthOfField)
            return;

        float target = resolveTargetInverse();

        focusInverse += (target - focusInverse)
                * (1f - (float) Math.exp(-RuntimeSetting.FOCUS_RATE * internal.getDeltaTime()));
    }

    // Focus \\

    private float resolveTargetInverse() {

        GridInstance grid = worldSystem.getGridInstance();
        CameraInstance camera = context.getWindow().getActiveCamera();

        if (grid == null || camera == null)
            return RuntimeSetting.FOCUS_INFINITY_INVERSE;

        float distance = Math.min(resolveSightDistance(grid, camera), resolveSubjectDistance(camera));

        if (distance >= RuntimeSetting.FOCUS_REACH)
            return RuntimeSetting.FOCUS_INFINITY_INVERSE;

        return 1f / Math.max(distance, RuntimeSetting.FOCUS_MIN_DISTANCE);
    }

    private float resolveSightDistance(GridInstance grid, CameraInstance camera) {

        lookDirection.set(camera.getDirection()).normalize();

        raycastManager.castSolidBlock(
                grid.getActiveChunkCoordinate(),
                camera.getPosition(),
                lookDirection,
                RuntimeSetting.FOCUS_REACH,
                castStruct);

        return castStruct.isHit() ? castStruct.getDistance() : RuntimeSetting.FOCUS_REACH;
    }

    // The distance to the middle of the player's body while it is in view, or the full reach when it is not
    private float resolveSubjectDistance(CameraInstance camera) {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID)
                || playerManager.isFreeCameraForWindow(windowID)
                || playerManager.isFirstPerson(windowID))
            return RuntimeSetting.FOCUS_REACH;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        Vector3 position = player.getWorldPositionStruct().getPosition();
        Vector3 size = player.getSize();
        Vector3 eye = camera.getPosition();

        float x = position.x + size.x * RuntimeSetting.FOCUS_SUBJECT_CENTER - eye.x;
        float y = position.y + size.y * RuntimeSetting.FOCUS_SUBJECT_CENTER - eye.y;
        float z = position.z + size.z * RuntimeSetting.FOCUS_SUBJECT_CENTER - eye.z;

        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    // Accessible \\

    public float getFocusInverse() {
        return focusInverse;
    }
}
