package editor.runtime.faceeditor;

import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.faceeditor.gesture.FaceEditorGestureSystem;
import editor.runtime.faceeditor.menu.FaceEditorMenuSystem;
import editor.runtime.faceeditor.preview.FaceEditorPreviewSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class FaceEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, ViewCameraProvider {

    /*
     * Editor tab for shaping a character's face the way a face rig is set up:
     * the character the Character Editor has open, framed on its head and
     * held still at the Character Editor's preview gender and sliders, its
     * face bones marked. A click picks a face bone with every tool; Move,
     * Rotate and Scale then drag it, shaping the picked face control's target
     * on that bone at the control's preview value, mirrored across the face
     * when asked, each drag one undo step. The face's controls and bones are
     * the Face Outliner window, the picked control the Face Properties window,
     * and its tools and control actions the Tools window's. All state lives
     * in FaceEditorManager and every edit in InfoManager; this tab reads
     * input, orbits the camera, poses and draws the character, takes the face
     * tools, and shows the toolbar and status.
     */

    // Internal
    private FaceEditorManager faceEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private FaceEditorPreviewSystem faceEditorPreviewSystem;
    private FaceEditorGestureSystem faceEditorGestureSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private FaceEditorMenuSystem faceEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.faceEditorPreviewSystem = create(FaceEditorPreviewSystem.class);
        this.faceEditorGestureSystem = create(FaceEditorGestureSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.faceEditorMenuSystem = create(FaceEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void get() {
        this.faceEditorManager = get(FaceEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return faceEditorManager.getToolCanvas();
    }

    // The face's actions run exactly as their panel rows and keys run them
    @Override
    public void runToolAction(String actionKey) {
        faceEditorManager.act(actionKey);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return faceEditorPreviewSystem;
    }
}
