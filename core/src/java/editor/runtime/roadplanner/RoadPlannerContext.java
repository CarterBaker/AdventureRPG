package editor.runtime.roadplanner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.roadplannerpipeline.roadplannermanager.RoadPlannerManager;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.roadplanner.menu.RoadPlannerMenuSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RoadPlannerContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, PaletteRowSource, PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for making roads, the way the Biome Planner is for biomes
     * and on the same grid: the Biome Planner's cells, painted by the same
     * Brush with the biome the Palette tab picks, so the rivers, mountains
     * and lakes they raise are the ones the road meets, and the road drawn
     * through them with Place and Erase, cell by cell in order, as the road
     * type the Palette tab picks above the biomes. The road is planned as
     * generation plans every road, its bends, heights, bridges and tunnels,
     * and laid into the biome planner's world, so the Biome Viewer, opened
     * from the toolbar, shows it as the game builds it. The Properties tab
     * holds the road type's whole entry, how the road is made, rebuilt live
     * as it is edited; the Tools tab's Road actions clear it, and the File
     * menu makes, saves and reverts road types. All state lives in
     * RoadPlannerManager and the grid's in BiomePlannerManager.
     */

    // Internal
    private RoadPlannerManager roadPlannerManager;
    private RoadPlannerMenuSystem roadPlannerMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.roadPlannerMenuSystem = create(RoadPlannerMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.roadPlannerManager = get(RoadPlannerManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.ROAD;
    }

    @Override
    public void runToolAction(String actionKey) {
        roadPlannerManager.act(actionKey);
    }

    // Palette \\

    // Every road type, then every biome the grid may be painted with
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return roadPlannerManager.getRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        roadPlannerManager.buildPaletteRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return RoadPlannerSetting.ROW_TEMPLATES;
    }

    // The palette holds no numbers to step
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // Properties \\

    // The road type's whole entry, selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_ROADS;
    }
}
