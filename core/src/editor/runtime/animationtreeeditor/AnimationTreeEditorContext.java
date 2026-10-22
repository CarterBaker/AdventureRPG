package editor.runtime.animationtreeeditor;

import editor.runtime.animationtreeeditor.menu.AnimationTreeEditorMenuSystem;
import editor.runtime.animationtreeeditor.preview.AnimationTreeEditorPreviewSystem;
import editor.runtime.characterview.camera.CharacterViewCameraSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import engine.root.ContextPackage;

public class AnimationTreeEditorContext extends ContextPackage {

    /*
     * Editor tab for building animation trees: layers and their blend and
     * bone masks, the nodes and blend spaces they play, the node each
     * movement state and action enters, and the cross-fades between them —
     * all played live on a character the tab simulates in any state, at any
     * parameter, through any action. All state lives in
     * AnimationTreeEditorManager and every edit in InfoManager; this tab
     * reads input, orbits the camera, plays and draws the character, and
     * shows the toolbar and panels.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewCameraSystem characterViewCameraSystem;
    private AnimationTreeEditorPreviewSystem animationTreeEditorPreviewSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private AnimationTreeEditorMenuSystem animationTreeEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.characterViewCameraSystem = create(CharacterViewCameraSystem.class);
        this.animationTreeEditorPreviewSystem = create(AnimationTreeEditorPreviewSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.animationTreeEditorMenuSystem = create(AnimationTreeEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
