package editor.runtime.treeplanner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.treeviewerpipeline.treeviewermanager.TreeViewerManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.treeplanner.menu.TreePlannerMenuSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreePlannerContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, PropertiesProvider, CharacterViewRowSource,
        PaletteRowSource, FileMenuProvider {

    /*
     * Editor tab for building trees, the way the Biome Planner is for biomes:
     * the generator a species grows by, drawn as a graph of nodes from its
     * form through its trunk, limbs and twigs to its leaves, with its wood
     * and its growth beside them. The species is picked in the Palette tab,
     * which lists every tree, or in the Hierarchy; a node clicked opens its
     * fields, LOD seed among the Tree node's, in the Properties tab, each
     * with a slider and a typed value, live in every window. The Tools tab's
     * Preview actions grow the preview from a new seed or to another age, the
     * Tree Viewer shows it at every LOD, and the File menu makes, saves and
     * reverts species. All state lives in TreeViewerManager; this tab only
     * shows it.
     */

    // Internal
    private TreeViewerManager treeViewerManager;
    private TreePlannerMenuSystem treePlannerMenuSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.treePlannerMenuSystem = create(TreePlannerMenuSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.treeViewerManager = get(TreeViewerManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.TREE;
    }

    @Override
    public void runToolAction(String actionKey) {
        treeViewerManager.act(actionKey);
    }

    // Properties \\

    // The fields of the node picked in the graph
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return treeViewerManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return TreePlannerSetting.PROPERTIES_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        treeViewerManager.buildFieldRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return TreePlannerSetting.ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        treeViewerManager.stepField(key, steps);
    }

    // Palette \\

    // Every tree species, to pick the one worked on
    @Override
    public PaletteContent getPaletteContent() {
        return PaletteContent.ROWS;
    }

    @Override
    public int getPaletteRevision() {
        return treeViewerManager.getRevision();
    }

    @Override
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        treeViewerManager.buildTreeRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getPaletteTemplates() {
        return TreePlannerSetting.ROW_TEMPLATES;
    }

    // The tree list holds no numbers to step
    @Override
    public void stepPaletteRow(String key, int steps) {
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_TREES;
    }
}
