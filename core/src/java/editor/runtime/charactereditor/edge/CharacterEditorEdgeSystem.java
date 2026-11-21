package editor.runtime.charactereditor.edge;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.CharacterEditorSetting;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class CharacterEditorEdgeSystem extends SystemPackage {

    /*
     * Turns this tab's pointer into edge work on the Topology layer while the
     * tool in use is Flip Edge, Loop Cut or Dissolve Loop: the edge of the
     * face under the pointer that passes nearest it is the edge hovered,
     * found again only when the pointer or the mesh moves and drawn by the
     * overlay as a guide, and a click on it applies the tool there, Ctrl
     * turning a flip the other way. A click off the mesh is left to the view
     * camera.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorManager characterEditorManager;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private ShortcutManager shortcutManager;

    // Shortcuts
    private int invertShortcutID;

    // Hover — the face and corner the hovered edge starts at, and the pointer and mesh it was found for
    private int hoveredFace;
    private int hoveredCorner;
    private float hoveredMouseX;
    private float hoveredMouseY;
    private int hoveredRevision;

    // Base \\

    @Override
    protected void create() {

        // Hover
        this.hoveredFace = EngineSetting.INDEX_NOT_FOUND;
        this.hoveredCorner = EngineSetting.INDEX_NOT_FOUND;
        this.hoveredRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterEditorMeshViewSystem = get(CharacterEditorMeshViewSystem.class);
        this.shortcutManager = get(ShortcutManager.class);
    }

    @Override
    protected void start() {
        this.invertShortcutID = shortcutManager.getShortcutIDFromShortcutName(
                CharacterEditorSetting.SHORTCUT_INVERT_STROKE);
    }

    // Update \\

    @Override
    protected void update() {

        if (!characterEditorManager.isEdgeTool() || !characterEditorMeshViewSystem.hasActiveDocument()
                || !editorInputSystem.isPointerOver() || viewCameraSystem.isDragging()) {
            this.hoveredFace = EngineSetting.INDEX_NOT_FOUND;
            return;
        }

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        syncHover(rawInput.getMouseX(), rawInput.getMouseY());

        if (hoveredFace == EngineSetting.INDEX_NOT_FOUND || !editorInputSystem.isPointerActive()
                || !editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT))
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        characterEditorManager.applyEdgeTool(
                hoveredFace,
                hoveredCorner,
                shortcutManager.isHeld(invertShortcutID, context.getWindow()));
        this.hoveredFace = EngineSetting.INDEX_NOT_FOUND;
        this.hoveredRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // The edge under the pointer, found again only when the pointer or the active mesh moved since
    private void syncHover(float mouseX, float mouseY) {

        int revision = characterEditorMeshViewSystem.getActiveDocument().getRevision();

        if (mouseX == hoveredMouseX && mouseY == hoveredMouseY && revision == hoveredRevision)
            return;

        this.hoveredMouseX = mouseX;
        this.hoveredMouseY = mouseY;
        this.hoveredRevision = revision;
        this.hoveredFace = characterEditorMeshViewSystem.pickEdge(mouseX, mouseY);
        this.hoveredCorner = characterEditorMeshViewSystem.getPickedCorner();
    }

    // Accessible \\

    public boolean hasHoveredEdge() {
        return hoveredFace != EngineSetting.INDEX_NOT_FOUND && characterEditorManager.isEdgeTool();
    }

    // The vertex the hovered edge runs from, or to with end set
    public int getHoveredVertex(boolean end) {

        RiggedMeshStruct mesh = characterEditorMeshViewSystem.getActiveDocument().getMesh();
        int corner = end ? mesh.getNextCorner(hoveredFace, hoveredCorner) : hoveredCorner;

        return mesh.getFaceVertex(hoveredFace, corner);
    }
}
