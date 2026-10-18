package editor.charactereditor;

import editor.charactereditor.menu.CharacterEditorMenuSystem;
import editor.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.charactereditor.overlay.CharacterEditorOverlaySystem;
import editor.charactereditor.preview.CharacterEditorPreviewSystem;
import editor.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.characterview.camera.CharacterViewCameraSystem;
import editor.characterview.panel.CharacterViewPanelSystem;
import editor.characterview.pose.CharacterViewPoseSystem;
import editor.characterview.render.CharacterViewRenderSystem;
import editor.runtime.EditorInputSystem;
import engine.root.ContextPackage;

public class CharacterEditorContext extends ContextPackage {

    /*
     * Editor tab for shaping a character: its default features, skin and
     * hair colors and palettes, its build curve and its equipment anchors,
     * and the meshes it wears themselves — their vertices, faces, new parts
     * and bone weights — on a live preview that stands, walks or runs through
     * the template's own animation tree. All state lives in
     * CharacterEditorManager, template edits in InfoManager and mesh edits in
     * their files; this tab reads input, orbits the camera, poses the
     * preview, picks and transforms vertices, draws the preview with its mesh
     * overlay, and shows the toolbar and panels.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewCameraSystem characterViewCameraSystem;
    private CharacterEditorPreviewSystem characterEditorPreviewSystem;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterEditorOverlaySystem characterEditorOverlaySystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterEditorMenuSystem characterEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.characterViewCameraSystem = create(CharacterViewCameraSystem.class);
        this.characterEditorPreviewSystem = create(CharacterEditorPreviewSystem.class);
        this.characterEditorMeshViewSystem = create(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = create(CharacterEditorSculptSystem.class);
        this.characterEditorOverlaySystem = create(CharacterEditorOverlaySystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
