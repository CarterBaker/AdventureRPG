package editor.runtime.animationeditor;

import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.animationeditor.menu.AnimationEditorMenuSystem;
import editor.runtime.animationeditor.pose.AnimationEditorPoseSystem;
import editor.runtime.animationeditor.preview.AnimationEditorPreviewSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationEditorContext extends ContextPackage
        implements ToolCanvasProvider, ViewCameraProvider, PropertiesProvider, CharacterViewRowSource,
        FileMenuProvider {

    /*
     * Editor tab for keyframing animation clips on a character: the
     * character posed at the playhead, its joints picked and its bones
     * turned, moved and scaled straight in the viewport by the pose tools,
     * each drag keyed at the playhead, with its undo. The clip's bones and
     * the timeline are each a window of their own, and the selected bone's
     * key is the Properties tab's while this tab is worked in.
     * All state lives in AnimationEditorManager and every edit in
     * InfoManager; this tab reads input, orbits the camera, poses and draws
     * the character at the playhead, takes the pose tools, and shows the
     * toolbar and status. While a clip plays on a character the tab is a
     * tool canvas, the pose.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private AnimationEditorManager animationEditorManager;
    private AnimationEditorPreviewSystem animationEditorPreviewSystem;
    private AnimationEditorPoseSystem animationEditorPoseSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private AnimationEditorMenuSystem animationEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.animationEditorPreviewSystem = create(AnimationEditorPreviewSystem.class);
        this.animationEditorPoseSystem = create(AnimationEditorPoseSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.animationEditorMenuSystem = create(AnimationEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
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

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return animationEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.ANIMATION_PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        animationEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.ANIMATION_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        animationEditorManager.step(key, steps);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterViewPoseSystem;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ANIMATION;
    }
}
