package editor.runtime.imageeditor;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.MapsProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.imageeditor.render.ImageEditorRenderSystem;
import editor.runtime.imageeditor.tool.ImageEditorToolSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, LayersProvider, PropertiesProvider, MapsProvider,
        CharacterViewRowSource, FileMenuProvider {

    /*
     * The Image Editor, under Textures, worked the way a painting tool such
     * as Krita is: File, New names an image and sets its size and
     * background; its layers are listed in the Layers tab, added and deleted
     * by the tab's icons or the Tools tab's actions, the one picked painted
     * with the Tools tab's tools, the Brushes tab's brush and the Color
     * Wheel's colour; its maps are listed in the Maps tab, each a special
     * layer the Layers tab never shows, made by Generate from the map's own
     * settings and erased by Clear, the map picked shown and painted here in
     * place of the layers; the Properties tab steps through the images and
     * names and fades the layer picked; the File and Edit menus save, copy,
     * revert, delete, undo and redo. The view pans and zooms as the UV
     * Editor's does. All state lives in ImageEditorManager; this tab reads
     * input, keeps its view and draws.
     */

    // Internal
    private ImageEditorManager imageEditorManager;
    private EditorInputSystem editorInputSystem;
    private UVEditorViewSystem uvEditorViewSystem;
    private ImageEditorToolSystem imageEditorToolSystem;
    private ImageEditorRenderSystem imageEditorRenderSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private LayersSourceSystem layersSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.uvEditorViewSystem = create(UVEditorViewSystem.class);
        this.imageEditorToolSystem = create(ImageEditorToolSystem.class);
        this.imageEditorRenderSystem = create(ImageEditorRenderSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
    }

    @Override
    protected void get() {
        this.imageEditorManager = get(ImageEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.IMAGE_EDITOR;
    }

    // The tab's actions run exactly as their keys and the Layers tab's icons run them
    @Override
    public void runToolAction(String actionKey) {
        imageEditorManager.act(actionKey);
    }

    // Layers \\

    @Override
    public int getLayerRevision() {
        return imageEditorManager.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.TAB_TITLE_IMAGE_EDITOR;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        imageEditorManager.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        imageEditorManager.selectLayer(layerKey);
    }

    @Override
    public void toggleLayerVisible(String layerKey) {
        imageEditorManager.toggleLayerVisible(layerKey);
    }

    @Override
    public String getAddLayerKey() {
        return EditorSetting.IMAGE_EDITOR_KEY_LAYER_ADD;
    }

    @Override
    public String getRemoveLayerKey() {
        return EditorSetting.IMAGE_EDITOR_KEY_LAYER_REMOVE;
    }

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return imageEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.TAB_TITLE_IMAGE_EDITOR;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        imageEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.IMAGE_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        imageEditorManager.step(key, steps);
    }

    // Maps \\

    @Override
    public int getMapsRevision() {
        return imageEditorManager.getRevision();
    }

    @Override
    public void buildMapsRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        imageEditorManager.buildMapRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getMapsTemplates() {
        return EditorSetting.IMAGE_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepMapsRow(String key, int steps) {
        imageEditorManager.step(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_IMAGE_EDITOR;
    }
}
