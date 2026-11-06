package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.runtime.charactereditor.menu.CharacterEditorMenuSystem;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.charactereditor.preview.CharacterEditorPreviewSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class CharacterEditorContext extends ContextPackage
        implements ViewCameraProvider, CharacterEditorWindowProvider {

    /*
     * Editor tab for how a character looks: its default features, skin and
     * hair colors and palettes, its build curve and its equipment anchors,
     * on a live preview wearing every slider, tone and build as it stands,
     * walks or runs through the template's own animation tree. Its modes are
     * Appearance, Build and Equipment, and every value is set in the
     * Character Properties window on a slider or typed. The meshes it wears
     * and its rig are modelled in the Character Mesh Editor beside it. All
     * state lives in CharacterEditorManager and template edits in
     * InfoManager; this tab reads input, orbits the camera, poses and draws
     * the preview with the markers of its mode, and shows the toolbar and
     * status. Its panels are the Character Outliner and Character Properties
     * windows.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterEditorPreviewSystem characterEditorPreviewSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterEditorMenuSystem characterEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.characterEditorModeSystem = create(CharacterEditorModeSystem.class);
        this.characterEditorPreviewSystem = create(CharacterEditorPreviewSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Window \\

    @Override
    public CharacterEditorMode getStartingMode() {
        return CharacterEditorMode.APPEARANCE;
    }

    @Override
    public String getToolbarMenu() {
        return CharacterEditorSetting.MENU_TOOLBAR;
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterViewPoseSystem;
    }
}
