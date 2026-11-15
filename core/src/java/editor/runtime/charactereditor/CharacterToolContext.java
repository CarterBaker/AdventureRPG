package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.OutlinerProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.bone.CharacterEditorBoneSystem;
import editor.runtime.charactereditor.menu.CharacterEditorMenuSystem;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.charactereditor.mode.CharacterModeProvider;
import editor.runtime.charactereditor.overlay.CharacterEditorOverlaySystem;
import editor.runtime.charactereditor.paint.CharacterEditorPaintSystem;
import editor.runtime.charactereditor.preview.CharacterEditorPreviewSystem;
import editor.runtime.charactereditor.region.CharacterRegionFrameSystem;
import editor.runtime.charactereditor.region.CharacterRegionIsolationSystem;
import editor.runtime.charactereditor.rig.CharacterEditorRigSystem;
import editor.runtime.charactereditor.sculpt.CharacterEditorSculptSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.characterview.pose.CharacterViewPoseSystem;
import editor.runtime.characterview.render.CharacterViewRenderSystem;
import editor.runtime.faceeditor.gesture.FaceEditorGestureSystem;
import editor.runtime.faceeditor.preview.FaceEditorPreviewSystem;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public abstract class CharacterToolContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, ViewCameraProvider, PropertiesProvider,
        OutlinerProvider, CharacterViewRowSource, FileMenuProvider, LayersProvider,
        CharacterModeProvider {

    /*
     * The base of every character tool tab, each working the character in the
     * one mode it names, so the owner lays the tools out as they wish:
     * Appearance, Build and Equipment shape how it looks on the live preview;
     * Mesh, Sculpt, Weights and Rig model the meshes it wears and the rig it
     * is built on in the region picked in the Layers tab, shared by all four,
     * framed and shown alone, every pick, box, stroke and mirror held to it;
     * Paint paints the maps of the part open, its paint layers listed in the
     * Layers tab; Face shapes the face's controls on the Head. Each tool tab
     * is a tool canvas worked with the Tools and Brushes tabs, its actions
     * listed in the Tools tab and its values and those of what is picked in
     * the Properties tab while the tab is worked in; the File and Edit menus
     * act on the character, or on its face in the Face tab. All state lives
     * in CharacterEditorManager and FaceEditorManager, the region, the bone
     * and the vertices selected shared by every tab; a tab keeps its mode,
     * reads input, orbits the camera, poses and draws the preview, and shows
     * the status bar.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private FaceEditorManager faceEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterViewPoseSystem characterViewPoseSystem;
    private ViewCameraSystem viewCameraSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private CharacterEditorPreviewSystem characterEditorPreviewSystem;
    private FaceEditorPreviewSystem faceEditorPreviewSystem;
    private CharacterRegionFrameSystem characterRegionFrameSystem;
    private CharacterRegionIsolationSystem characterRegionIsolationSystem;
    private CharacterEditorMeshViewSystem characterEditorMeshViewSystem;
    private CharacterEditorSculptSystem characterEditorSculptSystem;
    private CharacterEditorRigSystem characterEditorRigSystem;
    private CharacterEditorBoneSystem characterEditorBoneSystem;
    private CharacterEditorPaintSystem characterEditorPaintSystem;
    private CharacterEditorOverlaySystem characterEditorOverlaySystem;
    private FaceEditorGestureSystem faceEditorGestureSystem;
    private CharacterViewRenderSystem characterViewRenderSystem;
    private CharacterEditorMenuSystem characterEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private LayersSourceSystem layersSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterViewPoseSystem = create(CharacterViewPoseSystem.class);
        this.viewCameraSystem = create(ViewCameraSystem.class);
        this.characterEditorModeSystem = create(CharacterEditorModeSystem.class);
        this.characterEditorPreviewSystem = create(CharacterEditorPreviewSystem.class);
        this.faceEditorPreviewSystem = create(FaceEditorPreviewSystem.class);
        this.characterRegionFrameSystem = create(CharacterRegionFrameSystem.class);
        this.characterRegionIsolationSystem = create(CharacterRegionIsolationSystem.class);
        this.characterEditorMeshViewSystem = create(CharacterEditorMeshViewSystem.class);
        this.characterEditorSculptSystem = create(CharacterEditorSculptSystem.class);
        this.characterEditorRigSystem = create(CharacterEditorRigSystem.class);
        this.characterEditorBoneSystem = create(CharacterEditorBoneSystem.class);
        this.characterEditorPaintSystem = create(CharacterEditorPaintSystem.class);
        this.characterEditorOverlaySystem = create(CharacterEditorOverlaySystem.class);
        this.faceEditorGestureSystem = create(FaceEditorGestureSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);

        if (getMode().listsLayers())
            this.layersSourceSystem = create(LayersSourceSystem.class);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.faceEditorManager = get(FaceEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tool \\

    // The one mode this tab works the character in
    @Override
    public abstract CharacterEditorMode getMode();

    // The tab's title, naming the tool at the Layers tab's head
    protected abstract String getTabTitle();

    // Layers \\

    @Override
    public int getLayerRevision() {
        return characterEditorModeSystem.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return getTabTitle();
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        characterEditorModeSystem.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        characterEditorModeSystem.select(layerKey);
    }

    @Override
    public void toggleLayerVisible(String layerKey) {
        characterEditorModeSystem.toggleVisible(layerKey);
    }

    // True for the Face tab, whose tools, rows and file actions are the face editor's
    private boolean isFace() {
        return getMode().shapesFace();
    }

    // Tools \\

    // The tools of the tab's mode, the face's only while the character has a face to shape
    @Override
    public ToolCanvas getToolCanvas() {
        return isFace() ? faceEditorManager.getToolCanvas() : characterEditorModeSystem.getMode().getToolCanvas();
    }

    // The tab's actions run exactly as their keys and panel rows run them, in this window's region and mode
    @Override
    public void runToolAction(String actionKey) {

        characterEditorModeSystem.adopt();

        if (isFace())
            faceEditorManager.act(actionKey);
        else
            characterEditorManager.act(actionKey);
    }

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    // The face editor's revision moves with every change of either editor, so the rows follow both
    @Override
    public int getRowRevision() {
        return faceEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return isFace()
                ? EditorSetting.FACE_PROPERTIES_TITLE
                : EditorSetting.CHARACTER_PROPERTIES_TITLE + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                        + characterEditorManager.getMode().getLabel();
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (isFace())
            faceEditorManager.buildPropertyRows(rows);
        else
            characterEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return isFace() ? EditorSetting.FACE_EDITOR_ROW_TEMPLATES : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {

        if (isFace())
            faceEditorManager.step(key, steps);
        else
            characterEditorManager.step(key, steps);
    }

    // Outliner \\

    @Override
    public int getOutlinerRevision() {
        return faceEditorManager.getRevision();
    }

    @Override
    public String getOutlinerTitle() {
        return isFace()
                ? EditorSetting.FACE_OUTLINER_TITLE
                : EditorSetting.CHARACTER_OUTLINER_TITLE + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                        + characterEditorManager.getMode().getLabel();
    }

    @Override
    public void buildOutlinerRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (isFace())
            faceEditorManager.buildListRows(rows);
        else
            characterEditorManager.buildListRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getOutlinerTemplates() {
        return getRowTemplates();
    }

    @Override
    public void stepOutlinerRow(String key, int steps) {
        stepRow(key, steps);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return characterRegionFrameSystem;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return isFace() ? EditorSetting.FILE_MENU_FACE : EditorSetting.FILE_MENU_CHARACTER;
    }
}
