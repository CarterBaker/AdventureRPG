package editor.runtime.charactereditor.stamp;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.stamppipeline.stampmanager.StampManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.assets.camera.CameraInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CharacterEditorStampSystem extends SystemPackage {

    /*
     * Turns the Sculpt Brush into a placing tool while a part is chosen in
     * the Stamps tab: the part follows the pointer over the body in the
     * bind pose, snapping to an open loop whose centre lies near the pointer,
     * green where it can join and red where it cannot, its twin across the
     * body shown with it while mirroring. A click where it is green sets it
     * down; moving the pointer then turns it about the point it joins at,
     * left and right about the view's up and up and down about its side, a
     * second click joins it to the body, and a right click or Escape lifts
     * it again. A root part stands at the rig's root and joins at the first
     * click; fur follows the pointer anywhere on the body, never snapping,
     * and is laid at the first click. On a blank model, with no body to aim
     * at, a click starts the body from the root part armed. Once joined the
     * part is put down, and
     * Escape puts it down unplaced, so the Brush strokes again. The placement
     * is worked out again only when the pointer, the view, the part, the
     * tool's options or the body have moved.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ToolManager toolManager;
    private StampManager stampManager;

    // Placement — the part as it would join, its twin across the body, and a count moved whenever either changes
    private CharacterPartPlacementStruct placement;
    private CharacterPartPlacementStruct twin;
    private boolean twinShown;
    private int revision;

    // Aim — the open loop's attach point or the face the pointer is over, and the point and normal it meets there
    private int aimAttachPoint;
    private int aimFace;
    private Vector3 aimPoint;
    private Vector3 aimNormal;

    // Set Down — the pointer where the part was set down, and the turn it has given the part since
    private boolean setDown;
    private float setDownX;
    private float setDownY;
    private Matrix4 tilt;

    // Built — what the placement was last worked out from
    private float builtMouseX;
    private float builtMouseY;
    private int builtOptionRevision;
    private int builtStampRevision;
    private int builtDocumentRevision;
    private boolean builtMirror;
    private Vector3 builtCameraPosition;
    private Vector3 builtCameraDirection;

    // Scratch
    private Vector3 attachScratch;
    private Vector3 sideScratch;
    private Vector2 screenScratch;
    private Matrix4 turnScratch;

    // Base \\

    @Override
    protected void create() {

        // Placement
        this.placement = new CharacterPartPlacementStruct();
        this.twin = new CharacterPartPlacementStruct();

        // Aim
        this.aimAttachPoint = EngineSetting.INDEX_NOT_FOUND;
        this.aimFace = EngineSetting.INDEX_NOT_FOUND;
        this.aimPoint = new Vector3();
        this.aimNormal = new Vector3();

        // Set Down
        this.tilt = new Matrix4(1f);

        // Built
        this.builtCameraPosition = new Vector3();
        this.builtCameraDirection = new Vector3();

        // Scratch
        this.attachScratch = new Vector3();
        this.sideScratch = new Vector3();
        this.screenScratch = new Vector2();
        this.turnScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.characterViewPoseSystem = get(CharacterViewPoseSystem.class);
        this.toolManager = get(ToolManager.class);
        this.stampManager = get(StampManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (characterEditorManager.isBlankModel()) {
            updateBlank();
            return;
        }

        if (!characterEditorManager.armsStamp() || !characterViewPoseSystem.hasSubject()
                || !characterEditorMeshViewSystem.hasActiveDocument()) {
            lift();
            hide();
            return;
        }

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (setDown) {
            updateSetDown(rawInput);
            return;
        }

        if (editorInputSystem.isPointerActive() && rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            hide();
            characterEditorManager.showPlacementMessage(null);
            characterEditorManager.disarmStamp();
            return;
        }

        if (!characterEditorManager.placesStamp()) {
            hide();
            characterEditorManager.showPlacementMessage(EditorSetting.CHARACTER_STAMP_MESSAGE_BODY_ONLY);
            return;
        }

        if (!editorInputSystem.isPointerOver() || viewCameraSystem.isDragging()) {
            hide();
            return;
        }

        refresh(rawInput.getMouseX(), rawInput.getMouseY());

        if (editorInputSystem.isPointerActive()
                && editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            press(rawInput.getMouseX(), rawInput.getMouseY());
    }

    // A blank model has no body to aim at: a click with a part armed starts the body from it at the rig's root
    private void updateBlank() {

        lift();
        hide();

        if (characterEditorManager.armsStamp() && editorInputSystem.isPointerActive()
                && editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
            characterEditorManager.startModelBody();
        }
    }

    // A click where the part is green sets it down, or joins a root part at once; anywhere else is the camera's
    private void press(float mouseX, float mouseY) {

        if (!placement.isValid() || twinShown && !twin.isValid())
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);

        if (placement.joinsAtOnce()) {
            join();
            return;
        }

        this.setDown = true;
        this.setDownX = mouseX;
        this.setDownY = mouseY;
        tilt.set(1f);
    }

    // Moving the pointer turns the part set down, a click joins it, a right click or Escape lifts it again
    private void updateSetDown(RawInputHandle rawInput) {

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)
                || rawInput.isButtonClicked(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_CANCEL);
            lift();
            return;
        }

        if (editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
            join();
            lift();
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (!hasMoved(mouseX, mouseY))
            return;

        resolveTilt(mouseX - setDownX, mouseY - setDownY);
        resolve(mouseX, mouseY);
    }

    // The pointer's travel since the part was set down as a turn about the point it joins at: across the screen
    // about the view's up, up and down about the view's side
    private void resolveTilt(float travelX, float travelY) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        Vector3 up = camera.getUp();
        Vector3 direction = camera.getDirection();
        float perPixel = (float) Math.toRadians(EditorSetting.CHARACTER_STAMP_TURN_DEGREES_PER_PIXEL);

        sideScratch.set(
                direction.y * up.z - direction.z * up.y,
                direction.z * up.x - direction.x * up.z,
                direction.x * up.y - direction.y * up.x);

        if (sideScratch.length() > EditorSetting.CHARACTER_VIEW_RAY_EPSILON)
            sideScratch.normalize();

        attachScratch.set(up).normalize();
        CharacterPoseUtility.toAxisRotation(attachScratch, travelX * perPixel, tilt);
        CharacterPoseUtility.toAxisRotation(sideScratch, -travelY * perPixel, turnScratch);
        tilt.multiply(turnScratch);
    }

    // The part joined where it stands, with its twin while one is shown
    private void join() {
        characterEditorManager.placeStamp(placement, twinShown ? twin : null);
    }

    // The part lifted from where it was set down, aimed by the pointer again
    private void lift() {

        if (!setDown)
            return;

        this.setDown = false;
        tilt.set(1f);
        this.builtMouseX = Float.NaN;
    }

    // Nothing shown while the pointer is away or the camera turns
    private void hide() {

        if (!placement.isShown() && !twinShown)
            return;

        placement.clear();
        this.twinShown = false;
        this.builtMouseX = Float.NaN;
        revision++;
    }

    // Aim \\

    // The aim found again under the pointer, and the placement with it, when anything it is worked out from moved
    private void refresh(float mouseX, float mouseY) {

        if (!hasMoved(mouseX, mouseY))
            return;

        StampHandle stampHandle = characterEditorManager.getActiveStamp();

        this.aimAttachPoint = stampHandle.hasFur()
                ? EngineSetting.INDEX_NOT_FOUND
                : findSnappedLoop(mouseX, mouseY);
        this.aimFace = aimAttachPoint == EngineSetting.INDEX_NOT_FOUND
                ? characterEditorMeshViewSystem.pickSurface(mouseX, mouseY, aimPoint, aimNormal)
                : EngineSetting.INDEX_NOT_FOUND;

        resolve(mouseX, mouseY);
    }

    // The open loop whose centre the pointer lies nearest within the snapping reach, INDEX_NOT_FOUND for none
    private int findSnappedLoop(float mouseX, float mouseY) {

        float reach = EditorSetting.CHARACTER_STAMP_LOOP_SNAP_PIXELS;
        float nearest = reach * reach;
        int snapped = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < characterEditorManager.getAttachPointCount(); i++) {

            if (!characterEditorManager.isLoopAttachPoint(i))
                continue;

            characterEditorManager.getAttachPoint(i, attachScratch);

            if (!characterEditorMeshViewSystem.project(
                    attachScratch.x, attachScratch.y, attachScratch.z, screenScratch))
                continue;

            float dx = screenScratch.x - mouseX;
            float dy = screenScratch.y - mouseY;
            float distance = dx * dx + dy * dy;

            if (distance <= nearest) {
                nearest = distance;
                snapped = i;
            }
        }

        return snapped;
    }

    // The placement and its twin worked out for the aim held, and what the tab says about it told
    private void resolve(float mouseX, float mouseY) {

        StampHandle stampHandle = characterEditorManager.getActiveStamp();

        hold(mouseX, mouseY);
        revision++;

        if ((stampHandle.hasPlug() || stampHandle.hasFur()) && aimAttachPoint == EngineSetting.INDEX_NOT_FOUND
                && aimFace == EngineSetting.INDEX_NOT_FOUND) {
            placement.clear();
            this.twinShown = false;
            return;
        }

        characterEditorManager.resolvePlacement(aimAttachPoint, aimFace, aimPoint, aimNormal, tilt, placement);
        this.twinShown = characterEditorManager.placesTwin(placement);

        if (twinShown)
            characterEditorManager.resolvePlacementTwin(placement, twin);

        if (placement.isRoot())
            characterEditorManager.showPlacementMessage(EditorSetting.CHARACTER_STAMP_MESSAGE_ROOT);
        else if (placement.getRefusal() != null || !twinShown)
            characterEditorManager.showPlacementMessage(placement.getRefusal());
        else
            characterEditorManager.showPlacementMessage(twin.getRefusal());
    }

    // Built \\

    // Whether the pointer, the view, the part, the tool's options, the mirror or the body moved since the placement
    // was last worked out
    private boolean hasMoved(float mouseX, float mouseY) {

        CameraInstance camera = context.getWindow().getActiveCamera();
        CharacterMeshDocumentInstance document = characterEditorMeshViewSystem.getActiveDocument();

        return mouseX != builtMouseX
                || mouseY != builtMouseY
                || toolManager.getOptionRevision() != builtOptionRevision
                || stampManager.getRevision() != builtStampRevision
                || document.getRevision() != builtDocumentRevision
                || characterEditorManager.isMirror() != builtMirror
                || !camera.getPosition().equals(builtCameraPosition)
                || !camera.getDirection().equals(builtCameraDirection);
    }

    private void hold(float mouseX, float mouseY) {

        CameraInstance camera = context.getWindow().getActiveCamera();

        this.builtMouseX = mouseX;
        this.builtMouseY = mouseY;
        this.builtOptionRevision = toolManager.getOptionRevision();
        this.builtStampRevision = stampManager.getRevision();
        this.builtDocumentRevision = characterEditorMeshViewSystem.getActiveDocument().getRevision();
        this.builtMirror = characterEditorManager.isMirror();
        builtCameraPosition.set(camera.getPosition());
        builtCameraDirection.set(camera.getDirection());
    }

    // Accessible \\

    public CharacterPartPlacementStruct getPlacement() {
        return placement;
    }

    // The twin across the body joined with the part, null while none is
    public CharacterPartPlacementStruct getTwin() {
        return twinShown ? twin : null;
    }

    // Moves whenever the placement or its twin changes, so the ghost is drawn again only then
    public int getRevision() {
        return revision;
    }
}
