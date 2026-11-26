package editor.runtime.animationtreeeditor;

import editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager.AnimationTreeEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.animationtreeeditor.menu.AnimationTreeEditorMenuSystem;
import editor.runtime.animationtreeeditor.preview.AnimationTreeEditorPreviewSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationTreeEditorContext extends ContextPackage
        implements ViewCameraProvider, PropertiesProvider, CharacterViewRowSource, PaletteRowSource, FileMenuProvider {

    /*
     * Editor tab for building animation trees: layers and their blend and
     * bone masks, the nodes and blend spaces they play, the node each
     * movement state and action enters, and the cross-fades between them —
     * all played live on a character the tab simulates in any state, at any
     * parameter, through any action. All state lives in
     * AnimationTreeEditorManager and every edit in InfoManager; this tab
     * reads input, orbits the camera, plays and draws the character, and
     * shows the toolbar; the tree's layers and nodes are picked in the
     * Palette tab and the values of what is picked, with how the character
     * is simulated, set in the Properties tab, whose number sliders step them
     * through the manager.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private AnimationTreeEditorPreviewSystem animationTreeEditorPreviewSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
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
        this.animationTreeEditorMenuSystem = create(AnimationTreeEditorMenuSystem.class);
        create(ViewGizmoSystem.class);
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
    public int getRowRevision() {
        return animationTreeEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return AnimationTreeEditorSetting.PROPERTIES_TITLE;
    }

    // How the character is simulated, then the values of the layer or node picked
    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        animationTreeEditorManager.buildSimulationRows(rows);
        animationTreeEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return AnimationTreeEditorSetting.ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        animationTreeEditorManager.step(key, steps);
    }

    // Palette \\

    // Every layer and node of the tree, to pick the one worked on
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return animationTreeEditorManager.getRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        animationTreeEditorManager.buildPickRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return AnimationTreeEditorSetting.ROW_TEMPLATES;
    }

    @Override
    public void stepPaletteRow(String key, int steps) {
        animationTreeEditorManager.step(key, steps);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterViewPoseSystem;
    }

    // Properties \\

    // The values of the layer or node picked in the Palette tab
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ANIMATION_TREE;
    }
}
