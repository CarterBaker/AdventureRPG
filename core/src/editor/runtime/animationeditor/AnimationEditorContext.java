package editor.runtime.animationeditor;

import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.animationeditor.menu.AnimationEditorMenuSystem;
import editor.runtime.animationeditor.pose.AnimationEditorPoseSystem;
import editor.runtime.animationeditor.preview.AnimationEditorPreviewSystem;
import editor.runtime.characterview.camera.CharacterViewCameraSystem;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class AnimationEditorContext extends ContextPackage implements ToolCanvasProvider {

    /*
     * Editor tab for keyframing animation clips on a character: the
     * character posed at the playhead, its joints picked and its bones
     * turned, moved and scaled straight in the viewport by the pose tools,
     * each drag keyed at the playhead, with its undo. The clip's bones, the
     * selected bone's key and the timeline are each a window of their own.
     * All state lives in AnimationEditorManager and every edit in
     * InfoManager; this tab reads input, orbits the camera, poses and draws
     * the character at the playhead, takes the pose tools, and shows the
     * toolbar and status. While a clip plays on a character the tab is a
     * tool canvas, the pose.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private CharacterViewCameraSystem characterViewCameraSystem;
    private AnimationEditorManager animationEditorManager;
    private AnimationEditorPreviewSystem animationEditorPreviewSystem;
    private AnimationEditorPoseSystem animationEditorPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private AnimationEditorMenuSystem animationEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.characterViewCameraSystem = create(CharacterViewCameraSystem.class);
        this.animationEditorPreviewSystem = create(AnimationEditorPreviewSystem.class);
        this.animationEditorPoseSystem = create(AnimationEditorPoseSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.animationEditorMenuSystem = create(AnimationEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return animationEditorManager.getToolCanvas();
    }
}
