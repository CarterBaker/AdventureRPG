package editor.runtime.worldeditor;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.layerssource.LayersSourceSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.worldeditor.layer.WorldEditorLayerSystem;
import editor.runtime.worldeditor.menu.WorldEditorMenuSystem;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorContext extends ContextPackage
        implements ToolCanvasProvider, WorldEditorLayerProvider, LayersProvider, PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for the world's images, one image pixel per biome cell of
     * the world, worked as a stack of layers in the Layers tab: Biomes, the
     * biome canvas, which also places settlements, structures and cameras,
     * and Heights over it, drawn as its black and white heightmap, where the
     * Raise and Lower tools build the ground up and wear it down, Smooth
     * evens it, Flatten levels it and Erase returns it to the ground as
     * generated. Reads input, pans and zooms the view, turns the pointer into
     * image pixels for the tool chosen for the layer picked, draws every
     * layer shown with the brush or the design's footprint and an icon for
     * every hand placement, and shows the toolbar and status line, with every
     * player's marker when shown. A design dragged from the Settlement
     * Planner is stood where it is dropped. The tools, brushes and design are
     * chosen in their own tabs, the biome painted in the Biome Outliner and
     * its fields shown in the Properties tab; the images, their history and
     * every live update live in the editor and engine. The tab is a tool
     * canvas for as long as it is open, working on the layer picked.
     */

    // Internal
    private WorldEditorManager worldEditorManager;
    private EditorInputSystem editorInputSystem;
    private WorldEditorLayerSystem worldEditorLayerSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private WorldEditorMenuSystem worldEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;
    private LayersSourceSystem layersSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorLayerSystem = create(WorldEditorLayerSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
        this.worldEditorRenderSystem = create(WorldEditorRenderSystem.class);
        this.worldEditorMenuSystem = create(WorldEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
        this.layersSourceSystem = create(LayersSourceSystem.class);
    }

    @Override
    protected void get() {
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Management \\

    public void togglePlayers() {
        worldEditorRenderSystem.togglePlayers();
    }

    public void dropDesign() {
        worldEditorToolSystem.drop();
    }

    // Tools \\

    // The tools of the layer picked, the placing mode's while the biomes place
    @Override
    public ToolCanvas getToolCanvas() {
        return worldEditorManager.getToolCanvas(worldEditorLayerSystem.getLayer());
    }

    // Layers \\

    @Override
    public WorldEditorLayer getWorldEditorLayer() {
        return worldEditorLayerSystem.getLayer();
    }

    @Override
    public int getLayerRevision() {
        return worldEditorLayerSystem.getRevision();
    }

    @Override
    public String getLayerTitle() {
        return EditorSetting.TAB_TITLE_WORLD_EDITOR;
    }

    @Override
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {
        worldEditorLayerSystem.buildLayers(roots);
    }

    @Override
    public void selectLayer(String layerKey) {
        worldEditorLayerSystem.select(layerKey);
    }

    @Override
    public void toggleLayerVisible(String layerKey) {
        worldEditorLayerSystem.toggleShown(layerKey);
    }

    // Properties \\

    // The biome chosen to paint while the World Editor paints, the placement chosen to place while it places
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_WORLD;
    }
}
