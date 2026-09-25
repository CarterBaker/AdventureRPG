package editor.bootstrap.infopipeline.infotab;

import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoTabInstance extends InstancePackage implements HierarchyTabProvider {

    /*
     * One schema's tab in every hierarchy panel. Holds nothing of its own:
     * nodes, clicks, and the revision all route through InfoManager, so every
     * tab reflects the same shared info state.
     */

    // Internal
    private InfoSchemaHandle schema;
    private InfoManager infoManager;

    // Constructor \\

    public void constructor(InfoSchemaHandle schema, InfoManager infoManager) {

        // Internal
        this.schema = schema;
        this.infoManager = infoManager;
    }

    // Hierarchy \\

    @Override
    public String getTabName() {
        return schema.getTabName();
    }

    @Override
    public int getRevision() {
        return infoManager.getRevision();
    }

    @Override
    public void buildNodes(ObjectArrayList<HierarchyNodeStruct> roots) {
        infoManager.buildNodes(schema, roots);
    }

    @Override
    public void selectTab() {
        infoManager.selectTab(schema);
    }

    @Override
    public void selectNode(String nodeKey) {
        infoManager.selectNode(schema, nodeKey);
    }
}
