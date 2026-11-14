package editor.runtime.charactereditor;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.faceeditorpipeline.faceeditormanager.FaceEditorManager;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.menu.CharacterEditorMenuSystem;
import editor.runtime.charactereditor.meshview.CharacterEditorMeshViewSystem;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
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

public class CharacterEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, ViewCameraProvider, PropertiesProvider,
        CharacterViewRowSource, FileMenuProvider, LayersProvider {

    /*
     * The one editor tab for every part of making a character, worked as a
     * stack of layers in the Layers tab. The Character group shapes how it
     * looks — Appearance, Build and Equipment on the live preview — and the
     * Body and every region within it (the Head with its Hair, Ears, Eyes and
     * Mouth, the Hands and the Feet) each hold Mesh, Sculpt, Weights, Paint
     * and Rig, modelling the meshes it wears and the rig it is built on in
     * the exact bind pose, framed on the region and shown alone, every pick,
     * box, stroke and mirror held to it; the Head also holds Face, the
     * controls shaped through the face's bones. Each layer that takes tools
     * is a tool canvas worked with the Tools and Brushes tabs, its actions
     * listed in the Tools tab and its values and those of what is picked in
     * the Properties tab while the tab is worked in; the File and Edit menus
     * act on the character, or on its face on the Face layer. All state lives
     * in CharacterEditorManager and FaceEditorManager; this tab keeps the
     * layer worked on, reads input, orbits the camera, poses and draws the
     * preview, and shows the status bar.
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
        this.characterEditorPaintSystem = create(CharacterEditorPaintSystem.class);
        this.characterEditorOverlaySystem = create(CharacterEditorOverlaySystem.class);
        this.faceEditorGestureSystem = create(FaceEditorGestureSystem.class);
        this.characterViewRenderSystem = create(CharacterViewRenderSystem.class);
        this.characterEditorMenuSystem = create(CharacterEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
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

    // Layers \\

    @Override
    public int getLayerRevision() {
        return characterEditorModeSystem.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.TAB_TITLE_CHARACTER_EDITOR;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        characterEditorModeSystem.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        characterEditorModeSystem.select(layerKey);
    }

    // Every layer is always drawn, the region worked on standing alone
    @Override
    public void toggleLayerVisible(String layerKey) {
    }

    // True while the Face layer is worked on, whose tools, rows and file actions are the face editor's
    private boolean isFace() {
        return characterEditorModeSystem.getMode().shapesFace();
    }

    // Tools \\

    // The tools of the layer worked on, the face's only while the character has a face to shape
    @Override
    public ToolCanvas getToolCanvas() {
        return isFace() ? faceEditorManager.getToolCanvas() : characterEditorModeSystem.getMode().getToolCanvas();
    }

    // The layer's actions run exactly as their keys and panel rows run them, in this window's region and mode
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
