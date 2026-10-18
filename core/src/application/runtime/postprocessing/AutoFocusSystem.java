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
     * Finds where this window's camera is looking for the depth of field, and
     * how hard each side of the focus blurs. Each frame a ray is cast from the
     * camera along its view to the first solid block — past the player's own
     * body, which never takes the focus behind it — and the focus lands
     * there, so whatever the view rests on stays sharp and, up close,
     * everything else fades away. From the eye the background blurs through
     * the full aperture and the foreground through half. Behind the player
     * the view runs its own depth of field, switched and scaled by its own
     * settings, and the player is its subject: the subject's depth is handed
     * to the shader, which keeps the character's skin within it sharp
     * wherever the focus lies. Posed for a preview the player alone is in
     * focus, through the full aperture, as a portrait. The focus eases toward
     * its target in inverse distance, the way a lens's focus ring turns, so a
     * pull from a far ridge to a nearby wall reads as one smooth rack of
     * focus, and the apertures ease with it. Nothing within reach focuses at
     * infinity. No ray is cast while depth of field is off.
     */

    // Internal
    private RaycastManager raycastManager;
    private PlayerManager playerManager;
    private WorldSystem worldSystem;

    // Focus
    private final Vector3 lookDirection = new Vector3();
    private final BlockCastStruct castStruct = new BlockCastStruct();
    private float focusInverse;
    private float nearAperture;
    private float farAperture;

    // Subject — the player's depth in inverse distance while its body is in view
    private float subjectNearInverse;
    private float subjectFarInverse;

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

        int windowID = context.getWindow().getWindowID();
        CameraInstance camera = context.getWindow().getActiveCamera();
        EntityInstance subject = resolveSubject(windowID);
        boolean portrait = subject != null && playerManager.isCharacterPreview(windowID);
        float ease = 1f - (float) Math.exp(-RuntimeSetting.FOCUS_RATE * internal.getDeltaTime());

        resolveSubjectRange(subject, camera);

        focusInverse += (resolveTargetInverse(subject, portrait, camera) - focusInverse) * ease;
        nearAperture += (resolveNearAperture(subject, portrait) - nearAperture) * ease;
        farAperture += (resolveFarAperture(subject, portrait) - farAperture) * ease;
    }

    // Focus \\

    private float resolveTargetInverse(EntityInstance subject, boolean portrait, CameraInstance camera) {

        GridInstance grid = worldSystem.getGridInstance();

        if (grid == null || camera == null)
            return RuntimeSetting.FOCUS_INFINITY_INVERSE;

        float distance = portrait
                ? resolveSubjectDistance(subject, camera)
                : resolveSightDistance(grid, camera);

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

    // Apertures \\

    private float resolveNearAperture(EntityInstance subject, boolean portrait) {

        if (portrait)
            return RuntimeSetting.FOCUS_PORTRAIT_NEAR_APERTURE;

        if (subject != null)
            return resolveThirdPersonAperture() * RuntimeSetting.FOCUS_THIRD_PERSON_NEAR_SHARE;

        return RuntimeSetting.FOCUS_FIRST_PERSON_NEAR_APERTURE;
    }

    private float resolveFarAperture(EntityInstance subject, boolean portrait) {

        if (portrait)
            return RuntimeSetting.FOCUS_PORTRAIT_FAR_APERTURE;

        if (subject != null)
            return resolveThirdPersonAperture();

        return RuntimeSetting.FOCUS_FIRST_PERSON_FAR_APERTURE;
    }

    private float resolveThirdPersonAperture() {
        return settings.depthOfFieldThirdPerson ? settings.depthOfFieldThirdPersonStrength : 0f;
    }

    // Subject \\

    // The window's player while its body is in view — behind it or posed for a preview — or null when the camera
    // sees from its eye or flies free
    private EntityInstance resolveSubject(int windowID) {

        if (!playerManager.hasPlayerForWindow(windowID)
                || playerManager.isFreeCameraForWindow(windowID)
                || playerManager.isFirstPerson(windowID))
            return null;

        return playerManager.getPlayerForWindow(windowID);
    }

    // The subject's depth: a sphere about the middle of its body, reaching past its corners by a margin
    private void resolveSubjectRange(EntityInstance subject, CameraInstance camera) {

        if (subject == null || camera == null) {
            subjectNearInverse = RuntimeSetting.FOCUS_SUBJECT_NONE_INVERSE;
            subjectFarInverse = RuntimeSetting.FOCUS_SUBJECT_NONE_INVERSE;
            return;
        }

        float distance = resolveSubjectDistance(subject, camera);
        float radius = subject.getSize().length() * RuntimeSetting.FOCUS_SUBJECT_RADIUS_SHARE
                + RuntimeSetting.FOCUS_SUBJECT_MARGIN;

        subjectNearInverse = 1f / Math.max(distance - radius, RuntimeSetting.FOCUS_MIN_DISTANCE);
        subjectFarInverse = 1f / (distance + radius);
    }

    // The distance from the camera to the middle of the subject's body
    private float resolveSubjectDistance(EntityInstance subject, CameraInstance camera) {

        Vector3 position = subject.getWorldPositionStruct().getPosition();
        Vector3 size = subject.getSize();
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

    public float getNearAperture() {
        return nearAperture;
    }

    public float getFarAperture() {
        return farAperture;
    }

    public float getSubjectNearInverse() {
        return subjectNearInverse;
    }

    public float getSubjectFarInverse() {
        return subjectFarInverse;
    }
}
