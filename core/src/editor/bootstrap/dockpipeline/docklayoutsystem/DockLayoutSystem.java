package editor.bootstrap.dockpipeline.docklayoutsystem;

import editor.bootstrap.dockpipeline.container.ContainerInstance;
import editor.bootstrap.dockpipeline.dockmanager.DockManager;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import editor.bootstrap.dockpipeline.tab.TabInstance;
import editor.bootstrap.dockpipeline.tabgroup.TabGroupInstance;
import editor.bootstrap.dockpipeline.tabmanager.TabManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DockLayoutSystem extends SystemPackage {

    private DockManager dockManager;
    private TabManager tabManager;

    @Override
    protected void get() {
        this.dockManager = get(DockManager.class);
        this.tabManager = get(TabManager.class);
    }

    @Override
    protected void update() {
        if (!dockManager.isLayoutDirty())
            return;

        ObjectArrayList<ContainerInstance> containers = dockManager.getContainers();

        for (int i = 0; i < containers.size(); i++) {
            ContainerInstance container = containers.get(i);
            resizeNodeTree(container.getRootNode());
        }

        dockManager.clearLayoutDirty();
    }

    private void resizeNodeTree(NodeInstance node) {
        if (node == null)
            return;

        if (node.isLeaf()) {
            TabGroupInstance group = node.getTabGroup();
            if (group == null)
                return;

            TabInstance activeTab = group.getActiveTab();
            if (activeTab == null)
                return;

            tabManager.resizeTab(
                    activeTab,
                    node.getX(),
                    node.getY() + EngineSetting.DOCK_TAB_BAR_HEIGHT,
                    node.getWidth(),
                    node.getHeight() - EngineSetting.DOCK_TAB_BAR_HEIGHT);
            return;
        }

        resizeNodeTree(node.getChildA());
        resizeNodeTree(node.getChildB());
    }
}
