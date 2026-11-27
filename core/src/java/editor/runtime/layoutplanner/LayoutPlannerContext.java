package editor.runtime.layoutplanner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.layoutplannerpipeline.layoutplannermanager.LayoutPlannerManager;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.layoutplanner.menu.LayoutPlannerMenuSystem;
import editor.runtime.layoutplanner.render.LayoutPlannerRenderSystem;
import editor.runtime.layoutplanner.view.LayoutPlannerViewSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.viewcamera.ViewCameraProvider;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.viewcamera.ViewGizmoSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutPlannerContext extends ContextPackage
        implements ViewCameraProvider, ToolCanvasProvider, ToolActionProvider, LayersProvider, PropertiesProvider,
        CharacterViewRowSource, PaletteRowSource, FileMenuProvider {

    /*
     * Editor tab for designing how a settlement type lays itself out, the
     * way the Tree Planner is for trees: its layout as nodes in the Layers
     * tab, Settlement the root and its stages under it (Ground, Streets,
     * Lots, Overpasses, Wall, Outposts), each labelled with what its fields
     * say; a node picked opens its numbers and ranges in the Properties tab,
     * each with a slider and a typed value, and the eyes of Streets, Lots and
     * Wall hide what they lay in the preview. The viewport shows the
     * settlement as generation plans it, at a place the world names, in the
     * architecture the Palette tab picks below every settlement type; the
     * Tools tab's Preview actions plan the next layout or site, and the File
     * menu makes, saves and reverts types. Roles and the names a type builds
     * with stay the Info Panel's. All state lives in LayoutPlannerManager;
     * this tab only shows it.
     */

    // Internal
    private LayoutPlannerManager layoutPlannerManager;
    private EditorInputSystem editorInputSystem;
    private LayoutPlannerViewSystem layoutPlannerViewSystem;
    private LayoutPlannerRenderSystem layoutPlannerRenderSystem;
    private LayoutPlannerMenuSystem layoutPlannerMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private LayersSourceSystem layersSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.layoutPlannerViewSystem = create(LayoutPlannerViewSystem.class);
        create(ViewCameraSystem.class);
        this.layoutPlannerRenderSystem = create(LayoutPlannerRenderSystem.class);
        this.layoutPlannerMenuSystem = create(LayoutPlannerMenuSystem.class);
        create(ViewGizmoSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.layoutPlannerManager = get(LayoutPlannerManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Camera \\

    @Override
    public ViewCameraSubject getViewCameraSubject() {
        return layoutPlannerViewSystem;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.LAYOUT;
    }

    @Override
    public void runToolAction(String actionKey) {
        layoutPlannerManager.act(actionKey);
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return layoutPlannerManager.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return LayoutPlannerSetting.LAYER_TITLE;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        layoutPlannerManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        layoutPlannerManager.selectLayer(layerKey);
    }

    @Override
    public void toggleLayerVisible(String layerKey) {
        layoutPlannerManager.toggleLayerShown(layerKey);
    }

    // A layout's stages are fixed, so the stack takes no new layers
    @Override
    public String getAddLayerKey() {
        return "";
    }

    @Override
    public String getRemoveLayerKey() {
        return "";
    }

    // Properties \\

    // The fields of the node picked in the Layers tab
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return layoutPlannerManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return LayoutPlannerSetting.PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        layoutPlannerManager.buildFieldRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return LayoutPlannerSetting.ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        layoutPlannerManager.stepField(key, steps);
    }

    // Palette \\

    // Every settlement type, then every architecture its preview may be built in
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return layoutPlannerManager.getRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        layoutPlannerManager.buildPaletteRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return LayoutPlannerSetting.ROW_TEMPLATES;
    }

    // The palette holds no numbers to step
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_LAYOUTS;
    }
}
