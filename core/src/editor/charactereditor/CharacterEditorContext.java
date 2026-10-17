package editor.charactereditor;

import editor.charactereditor.menu.CharacterEditorMenuSystem;
import editor.charactereditor.preview.CharacterEditorPreviewSystem;
import editor.characterview.camera.CharacterViewCameraSystem;
import editor.characterview.panel.CharacterViewPanelSystem;
import editor.characterview.pose.CharacterViewPoseSystem;
import editor.characterview.render.CharacterViewRenderSystem;
import editor.runtime.EditorInputSystem;
import engine.root.ContextPackage;

public class CharacterEditorContext extends ContextPackage {

    /*
     * Editor tab for shaping how a character looks: its default features,
     * skin and hair colors and palettes, its build curve and its equipment
     * anchors, on a live preview that stands, walks or runs through the
     * template's own animation tree. All state lives in CharacterEditorManager
     * and every edit in InfoManager; this tab reads input, orbits the camera,
     * poses and draws the preview, and shows the toolbar and panels.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewCameraSystem characterViewCameraSystem;
    private CharacterEditorPreviewSystem characterEditorPreviewSystem;
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
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
