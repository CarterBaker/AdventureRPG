package editor.runtime.charactermesheditor;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.charactereditor.CharacterEditorWindowProvider;
import editor.runtime.charactereditor.menu.CharacterEditorMenuSystem;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.charactereditor.overlay.CharacterEditorOverlaySystem;
import editor.runtime.charactereditor.preview.CharacterEditorPreviewSystem;
import editor.runtime.charactereditor.rig.CharacterEditorRigSystem;
import editor.runtime.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class CharacterMeshEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, ViewCameraProvider, CharacterEditorWindowProvider {

    /*
     * Editor tab for modelling a character: the meshes it wears — their
     * vertices, faces, new parts, sculpted surface and bone weights — and the
     * rig it is built on, shown in the exact bind pose. Its modes are Mesh,
     * Sculpt, Weights and Rig, each a tool canvas worked with the Tools and
     * Brushes tabs: Move, Rotate and Scale drag the selection, the sculpt and
     * weight tools stroke the brush chosen in the Brushes tab, soft or hard,
     * and the rig tools pick, drag, size, add and remove bones; the mesh's
     * transform, geometry, weight and selection actions are listed in the
     * Tools tab under its tools. How the character looks is shaped in the
     * Character Editor beside it. All state lives in CharacterEditorManager;
     * this tab reads input, orbits the camera, picks and transforms
     * vertices, strokes the brushes, picks and drags joints, draws the
     * preview with its overlay, and shows its toolbar and status.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterEditorPreviewSystem characterEditorPreviewSystem;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterEditorRigSystem characterEditorRigSystem;
    private CharacterEditorOverlaySystem characterEditorOverlaySystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterEditorMenuSystem characterEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.characterEditorModeSystem = create(CharacterEditorModeSystem.class);
        this.characterEditorPreviewSystem = create(CharacterEditorPreviewSystem.class);
        this.characterEditorMeshViewSystem = create(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = create(CharacterEditorSculptSystem.class);
        this.characterEditorRigSystem = create(CharacterEditorRigSystem.class);
        this.characterEditorOverlaySystem = create(CharacterEditorOverlaySystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Window \\

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.MESH;
    }

    @Override
    public String getToolbarMenu() {
        return CharacterMeshEditorSetting.MENU_TOOLBAR;
    }

    // Tools \\

    // The tools of the mode this tab works in, whichever tab was worked in last
    @Override
    public ToolCanvas getToolCanvas() {
        return characterEditorModeSystem.getMode().getToolCanvas();
    }

    // The mesh's actions run exactly as their keys and panel rows run them, in this tab's mode
    @Override
    public void runToolAction(String actionKey) {
        characterEditorModeSystem.adopt();
        characterEditorManager.act(actionKey);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterViewPoseSystem;
    }
}
