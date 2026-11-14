package editor.runtime.treeplanner;

import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.treeplanner.menu.TreePlannerMenuSystem;
import engine.root.ContextPackage;

public class TreePlannerContext extends ContextPackage implements PropertiesProvider, FileMenuProvider {

    /*
     * Editor tab for making trees, the way the Biome Planner is for biomes:
     * every tree species the Info files hold, the selected one highlighted,
     * and the generator it grows by drawn as a graph of nodes from its form
     * through its trunk, limbs and twigs to its leaves, with its wood and its
     * growth beside them. Clicking a node opens its fields, each stepped down
     * or up live in every window; the Tree Viewer shows the species at every
     * detail the world draws it at. New and Duplicate make trees through the
     * Info Panel, where every field of one is edited.
     */

    // Internal
    private TreePlannerMenuSystem treePlannerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.treePlannerMenuSystem = create(TreePlannerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
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
        return EditorSetting.FILE_MENU_TREE_PLANNER;
    }
}
