package editor.runtime.treeplanner;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.treeplanner.menu.TreePlannerMenuSystem;
import engine.root.ContextPackage;

public class TreePlannerContext extends ContextPackage implements PropertiesProvider {

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

    // Internal \\

    @Override
    protected void create() {
        this.treePlannerMenuSystem = create(TreePlannerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
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
}
