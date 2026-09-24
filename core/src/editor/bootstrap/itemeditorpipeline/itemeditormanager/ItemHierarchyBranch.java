package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import application.bootstrap.menupipeline.hierarchy.HierarchyTabProvider;
import application.bootstrap.menupipeline.hierarchymanager.HierarchyManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemHierarchyBranch extends BranchPackage implements HierarchyTabProvider {

    /*
     * The hierarchy's Items tab. Every item in the game is listed under the
     * definition file that declares it, with unsaved items marked and the
     * active item expanded to its parts. Clicking an item opens it; clicking a
     * part opens its item and selects the part for building.
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

        ObjectArrayList<ItemEntryStruct> entries = itemEditorManager.getItemEntries();
        ItemDocumentInstance activeDocument = itemEditorManager.getActiveDocument();
        HierarchyNodeStruct fileNode = null;

        entries.sort((a, b) -> {
            int byFile = String.CASE_INSENSITIVE_ORDER.compare(a.getDefinitionName(), b.getDefinitionName());
            return byFile != 0 ? byFile : String.CASE_INSENSITIVE_ORDER.compare(a.getLocalName(), b.getLocalName());
        });

        for (int i = 0; i < entries.size(); i++) {

            ItemEntryStruct entry = entries.get(i);

            if (fileNode == null || !fileNode.getNodeKey().equals(toFileKey(entry.getDefinitionName()))) {
                fileNode = createFileNode(entry.getDefinitionName(), activeDocument);
                roots.add(fileNode);
            }

            fileNode.addChild(createItemNode(entry, activeDocument));
        }
    }

    private HierarchyNodeStruct createFileNode(String definitionName, ItemDocumentInstance activeDocument) {

        boolean active = activeDocument != null
                && activeDocument.getEntry().getDefinitionName().equals(definitionName);

        return new HierarchyNodeStruct(toFileKey(definitionName), definitionName, active, true);
    }

    private HierarchyNodeStruct createItemNode(ItemEntryStruct entry, ItemDocumentInstance activeDocument) {

        ItemDocumentInstance document = itemEditorManager.getDocument(entry.getItemName());
        boolean active = document != null && document == activeDocument;
        String label = document != null && document.isDirty()
                ? entry.getLocalName() + EditorSetting.ITEM_EDITOR_DIRTY_MARKER
                : entry.getLocalName();

        HierarchyNodeStruct itemNode = new HierarchyNodeStruct(toItemKey(entry.getItemName()), label, active, active);

        if (document != null)
            addPartNodes(itemNode, document, active);

        return itemNode;
    }

    private void addPartNodes(HierarchyNodeStruct itemNode, ItemDocumentInstance document, boolean active) {

        SubVoxelModelStruct model = document.getModel();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            itemNode.addChild(new HierarchyNodeStruct(
                    toPartKey(document.getItemName(), partIndex),
                    model.getPart(partIndex).getPartName(),
                    active && partIndex == document.getSelectedPartIndex()));
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

    private String toFileKey(String definitionName) {
        return EditorSetting.HIERARCHY_FILE_KEY_PREFIX + definitionName;
    }

    private String toItemKey(String itemName) {
        return EditorSetting.HIERARCHY_ITEM_KEY_PREFIX + itemName;
    }

    private String toPartKey(String itemName, int partIndex) {
        return EditorSetting.HIERARCHY_PART_KEY_PREFIX + itemName + EditorSetting.HIERARCHY_KEY_SEPARATOR + partIndex;
    }
}
