package editor.runtime.worldeditor;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.placementicon.PlacementIconSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.worldeditor.menu.WorldEditorMenuSystem;
import editor.runtime.worldeditor.render.WorldEditorRenderSystem;
import editor.runtime.worldeditor.tool.WorldEditorToolSystem;
import editor.runtime.worldeditor.view.WorldEditorViewSystem;
import engine.root.ContextPackage;

public class WorldEditorContext extends ContextPackage
        implements ToolCanvasProvider, WorldEditorLayerProvider, PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for the world's biome canvas, one image pixel per biome cell
     * of the world. Reads input, pans and zooms the view, turns the pointer
     * into image pixels for the tool chosen for what it works on, painting
     * the biome image or placing settlements, structures and cameras, draws
     * the image with the brush or the design's footprint and an icon for
     * every hand placement, and shows the toolbar and status line, with every
     * player's marker when shown. A design dragged from the Settlement
     * Planner is stood where it is dropped. The tools, brushes and design are
     * chosen in their own tabs, the biome painted in the Biome Outliner, its
     * fields shown in the Properties tab, and the heights are raised and
     * lowered in the Height Editor; the image, its history and every live
     * update live in the editor and engine. The tab is a tool canvas for as
     * long as it is open, working on whatever the World Editor works on.
     */

    // Internal
    private WorldEditorManager worldEditorManager;
    private EditorInputSystem editorInputSystem;
    private WorldEditorViewSystem worldEditorViewSystem;
    private WorldEditorToolSystem worldEditorToolSystem;
    private PlacementIconSystem placementIconSystem;
    private WorldEditorRenderSystem worldEditorRenderSystem;
    private WorldEditorMenuSystem worldEditorMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.worldEditorViewSystem = create(WorldEditorViewSystem.class);
        this.worldEditorToolSystem = create(WorldEditorToolSystem.class);
        this.placementIconSystem = create(PlacementIconSystem.class);
        this.worldEditorRenderSystem = create(WorldEditorRenderSystem.class);
        this.worldEditorMenuSystem = create(WorldEditorMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
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

    @Override
    public ToolCanvas getToolCanvas() {
        return worldEditorManager.getToolCanvas(WorldEditorLayer.BIOMES);
    }

    // Layer \\

    @Override
    public WorldEditorLayer getWorldEditorLayer() {
        return WorldEditorLayer.BIOMES;
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
