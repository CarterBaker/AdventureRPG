package editor.runtime.animationtreeeditor;

import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.animationtreeeditor.menu.AnimationTreeEditorMenuSystem;
import editor.runtime.animationtreeeditor.preview.AnimationTreeEditorPreviewSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewStepTarget;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;

public class AnimationTreeEditorContext extends ContextPackage
        implements ViewCameraProvider, CharacterViewStepTarget, PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for building animation trees: layers and their blend and
     * bone masks, the nodes and blend spaces they play, the node each
     * movement state and action enters, and the cross-fades between them —
     * all played live on a character the tab simulates in any state, at any
     * parameter, through any action. All state lives in
     * AnimationTreeEditorManager and every edit in InfoManager; this tab
     * reads input, orbits the camera, plays and draws the character, and
     * shows the toolbar and panels, whose number sliders step the tree's
     * values through the manager.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private AnimationTreeEditorPreviewSystem animationTreeEditorPreviewSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private AnimationTreeEditorMenuSystem animationTreeEditorMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.animationTreeEditorPreviewSystem = create(AnimationTreeEditorPreviewSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.animationTreeEditorMenuSystem = create(AnimationTreeEditorMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public void stepRow(String key, int steps) {
        animationTreeEditorManager.step(key, steps);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterViewPoseSystem;
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ANIMATION_TREE;
    }
}
