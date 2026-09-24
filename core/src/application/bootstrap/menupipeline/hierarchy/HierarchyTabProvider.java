package application.bootstrap.menupipeline.hierarchy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public interface HierarchyTabProvider {

    /*
     * Supplies one tab of every hierarchy panel. Panels rebuild the tab when its
     * revision changes and route row clicks back through selectNode().
     */

    String getTabName();

    int getRevision();

    void buildNodes(ObjectArrayList<HierarchyNodeStruct> roots);

    void selectNode(String nodeKey);
}
