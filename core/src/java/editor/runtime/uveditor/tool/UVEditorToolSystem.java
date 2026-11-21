package editor.runtime.uveditor.tool;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSelection;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.uveditor.UVEditorSetting;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class UVEditorToolSystem extends SystemPackage {

    /*
     * Turns the UV Editor's pointer into UV edits through the character
     * editor. With Select, a click picks the UV nearest the pointer within
     * reach (every corner of its vertex at that place), replacing the
     * selection, Shift adding and Ctrl taking away, a click on nothing
     * clearing it; a drag boxes. With Move, Rotate or Scale a press takes
     * the selected UVs and the drag carries them about their centre, letting
     * go to confirm and a right click to put them back. Every press the
     * tool acts on is claimed from the view's pan. The tab's shortcuts and
     * Tools tab actions run here only with nothing under way.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private UVEditorViewSystem uvEditorViewSystem;
    private CharacterEditorManager characterEditorManager;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int addSelectionShortcutID;
    private int subtractSelectionShortcutID;

    // Press — where a Select press went down, until it becomes a click or a box
    private boolean pressed;
    private float pressX;
    private float pressY;

    // Box — its corners in UV while dragged; the revision moves with it so the overlay redraws it
    private boolean boxSelecting;
    private float boxMinU;
    private float boxMinV;
    private float boxMaxU;
    private float boxMaxV;
    private int boxRevision;

    // Transform
    private boolean transforming;

    // Scratch
    private IntArrayList faces;

    // Base \\

    @Override
    protected void create() {
        this.faces = new IntArrayList();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.uvEditorViewSystem = get(UVEditorViewSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {

        // Shortcuts
        this.addSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                UVEditorSetting.SHORTCUT_ADD_SELECTION);
        this.subtractSelectionShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                UVEditorSetting.SHORTCUT_SUBTRACT_SELECTION);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (characterEditorManager.getActiveMeshDocument() == null) {
            abandon();
            return;
        }

        if (transforming) {
            updateTransform(rawInput, mouseX, mouseY);
            return;
        }

        if (pressed) {
            updatePress(rawInput, mouseX, mouseY);
            return;
        }

        if (uvEditorViewSystem.isPanning() || !editorInputSystem.isClicked(UVEditorSetting.BUTTON_SELECT))
            return;

        editorInputSystem.claim(UVEditorSetting.BUTTON_SELECT);

        EditorTool tool = characterEditorManager.getUVTool();

        if (tool == EditorTool.SELECT) {
            this.pressed = true;
            this.pressX = mouseX;
            this.pressY = mouseY;
            return;
        }

        this.transforming = characterEditorManager.beginUVTransform(toU(mouseX), toV(mouseY));
    }

    // The drag carried on, confirmed when the button lets go, put back on a right click
    private void updateTransform(RawInputHandle rawInput, float mouseX, float mouseY) {

        if (editorInputSystem.takeClick(UVEditorSetting.BUTTON_CANCEL)) {
            characterEditorManager.cancelUVTransform();
            this.transforming = false;
            return;
        }

        characterEditorManager.updateUVTransform(toU(mouseX), toV(mouseY));

        if (rawInput.isButtonHeld(UVEditorSetting.BUTTON_SELECT))
            return;

        characterEditorManager.endUVTransform();
        this.transforming = false;
    }

    // A Select press becomes a box once it travels, a click once it lets go where it went down
    private void updatePress(RawInputHandle rawInput, float mouseX, float mouseY) {

        if (!boxSelecting && Math.hypot(mouseX - pressX, mouseY - pressY) > UVEditorSetting.BOX_THRESHOLD_PIXELS)
            this.boxSelecting = true;

        if (boxSelecting)
            updateBox(mouseX, mouseY);

        if (rawInput.isButtonHeld(UVEditorSetting.BUTTON_SELECT))
            return;

        CharacterEditorSelection mode = resolveSelection();

        if (boxSelecting)
            characterEditorManager.boxSelectUV(boxMinU, boxMinV, boxMaxU, boxMaxV, mode);
        else
            pick(mouseX, mouseY, mode);

        this.pressed = false;
        this.boxSelecting = false;
        boxRevision++;
    }

    private void updateBox(float mouseX, float mouseY) {

        float pressU = toU(pressX);
        float pressV = toV(pressY);
        float mouseU = toU(mouseX);
        float mouseV = toV(mouseY);

        this.boxMinU = Math.min(pressU, mouseU);
        this.boxMinV = Math.min(pressV, mouseV);
        this.boxMaxU = Math.max(pressU, mouseU);
        this.boxMaxV = Math.max(pressV, mouseV);
        boxRevision++;
    }

    // The corner nearest the pointer within reach among the faces shown, or none, combined by mode
    private void pick(float mouseX, float mouseY, CharacterEditorSelection mode) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();
        RiggedMeshStruct mesh = document.getMesh();
        float reach = UVEditorSetting.PICK_RADIUS_PIXELS * UVEditorSetting.PICK_RADIUS_PIXELS;
        int pickedFace = EngineSetting.INDEX_NOT_FOUND;
        int pickedCorner = 0;

        characterEditorManager.collectUVFaces(faces);

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < mesh.getFaceCornerCount(faceIndex); c++) {

                float dx = uvEditorViewSystem.uToScreen(mesh.getFaceU(faceIndex, c)) - mouseX;
                float dy = uvEditorViewSystem.vToScreen(mesh.getFaceV(faceIndex, c)) - mouseY;
                float distance = dx * dx + dy * dy;

                if (distance > reach)
                    continue;

                reach = distance;
                pickedFace = faceIndex;
                pickedCorner = c;
            }
        }

        if (pickedFace != EngineSetting.INDEX_NOT_FOUND || mode == CharacterEditorSelection.REPLACE)
            characterEditorManager.pickUV(pickedFace, pickedCorner, mode);
    }

    private CharacterEditorSelection resolveSelection() {

        if (shortcutManager.isHeld(addSelectionShortcutID, context.getWindow()))
            return CharacterEditorSelection.ADD;

        if (shortcutManager.isHeld(subtractSelectionShortcutID, context.getWindow()))
            return CharacterEditorSelection.SUBTRACT;

        return CharacterEditorSelection.REPLACE;
    }

    // Drops whatever the pointer held once the part open is gone
    private void abandon() {

        if (transforming)
            characterEditorManager.cancelUVTransform();

        this.transforming = false;
        this.pressed = false;

        if (boxSelecting) {
            this.boxSelecting = false;
            boxRevision++;
        }
    }

    // Shortcuts \\

    // One of the UV Editor's actions by its key, its shortcut and its Tools tab row alike, only with nothing under way
    public void act(String key) {
        if (!transforming && !pressed)
            characterEditorManager.act(key);
    }

    // Utility \\

    private float toU(float screenX) {
        return (float) uvEditorViewSystem.screenToU(screenX);
    }

    private float toV(float screenY) {
        return (float) uvEditorViewSystem.screenToV(screenY);
    }

    // Accessible \\

    public boolean isBoxSelecting() {
        return boxSelecting;
    }

    public float getBoxMinU() {
        return boxMinU;
    }

    public float getBoxMinV() {
        return boxMinV;
    }

    public float getBoxMaxU() {
        return boxMaxU;
    }

    public float getBoxMaxV() {
        return boxMaxV;
    }

    public int getBoxRevision() {
        return boxRevision;
    }
}
