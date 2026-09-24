package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemHierarchyBranch extends BranchPackage implements HierarchyTabProvider {

    /*
     * The hierarchy's Items tab. Lists saved and open items, with an open
     * item's parts as children. Clicking an item opens it; clicking a part opens
     * its item and selects the part.
     */

    // Internal
    private HierarchyManager hierarchyManager;
    private ItemEditorManager itemEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.hierarchyManager = get(HierarchyManager.class);
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    @Override
    protected void awake() {
        hierarchyManager.registerTabProvider(this);
    }

    // Hierarchy \\

    @Override
    public String getTabName() {
        return EditorSetting.HIERARCHY_TAB_ITEMS;
    }

    @Override
    public int getRevision() {
        return itemEditorManager.getRevision();
    }

    @Override
    public void buildNodes(ObjectArrayList<HierarchyNodeStruct> roots) {

        ObjectArrayList<String> itemNames = itemEditorManager.getItemNames();
        ItemDocumentInstance activeDocument = itemEditorManager.getActiveDocument();

        for (int i = 0; i < itemNames.size(); i++) {

            String itemName = itemNames.get(i);
            ItemDocumentInstance document = itemEditorManager.getDocument(itemName);
            boolean active = document != null && document == activeDocument;

            HierarchyNodeStruct itemNode = new HierarchyNodeStruct(
                    toItemKey(itemName), resolveItemLabel(itemName, document), active);

            if (document != null)
                addPartNodes(itemNode, document, active);

            roots.add(itemNode);
        }
    }

    private void addPartNodes(HierarchyNodeStruct itemNode, ItemDocumentInstance document, boolean active) {

        SubVoxelModelStruct model = document.getModel();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            itemNode.addChild(new HierarchyNodeStruct(
                    toPartKey(document.getItemName(), partIndex),
                    model.getPart(partIndex).getPartName(),
                    active && partIndex == document.getSelectedPartIndex()));
    }

    private String resolveItemLabel(String itemName, ItemDocumentInstance document) {
        return document != null && document.isDirty()
                ? itemName + EditorSetting.ITEM_EDITOR_DIRTY_MARKER
                : itemName;
    }

    @Override
    public void selectNode(String nodeKey) {

        if (nodeKey.startsWith(EditorSetting.HIERARCHY_ITEM_KEY_PREFIX)) {
            itemEditorManager.openItem(nodeKey.substring(EditorSetting.HIERARCHY_ITEM_KEY_PREFIX.length()));
            return;
        }

        if (!nodeKey.startsWith(EditorSetting.HIERARCHY_PART_KEY_PREFIX))
            return;

        String partKey = nodeKey.substring(EditorSetting.HIERARCHY_PART_KEY_PREFIX.length());
        int separator = partKey.lastIndexOf(EditorSetting.HIERARCHY_KEY_SEPARATOR);

        itemEditorManager.openItem(partKey.substring(0, separator));
        itemEditorManager.selectPart(Integer.parseInt(partKey.substring(separator + 1)));
    }

    // Keys \\

    private String toItemKey(String itemName) {
        return EditorSetting.HIERARCHY_ITEM_KEY_PREFIX + itemName;
    }

    private String toPartKey(String itemName, int partIndex) {
        return EditorSetting.HIERARCHY_PART_KEY_PREFIX + itemName + EditorSetting.HIERARCHY_KEY_SEPARATOR + partIndex;
    }
}
