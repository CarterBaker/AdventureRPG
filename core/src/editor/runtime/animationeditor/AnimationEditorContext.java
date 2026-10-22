package editor.runtime.animationeditor;

import editor.runtime.animationeditor.menu.AnimationEditorMenuSystem;
import editor.runtime.animationeditor.preview.AnimationEditorPreviewSystem;
import editor.runtime.characterview.camera.CharacterViewCameraSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import engine.root.ContextPackage;

public class AnimationEditorContext extends ContextPackage {

    /*
     * Editor tab for keyframing animation clips on a character: a timeline
     * to scrub and play, a key per bone for its rotation, position and scale,
     * and whole-pose tools to key, copy, paste and mirror. All state lives in
     * AnimationEditorManager and every edit in InfoManager; this tab reads
     * input, orbits the camera, poses and draws the character at the
     * playhead, and shows the toolbar, panels and timeline.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewCameraSystem characterViewCameraSystem;
    private AnimationEditorPreviewSystem animationEditorPreviewSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private AnimationEditorMenuSystem animationEditorMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.characterViewCameraSystem = create(CharacterViewCameraSystem.class);
        this.animationEditorPreviewSystem = create(AnimationEditorPreviewSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.animationEditorMenuSystem = create(AnimationEditorMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
