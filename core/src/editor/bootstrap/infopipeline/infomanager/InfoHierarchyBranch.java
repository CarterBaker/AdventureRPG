package editor.bootstrap.infopipeline.infomanager;

import application.bootstrap.menupipeline.hierarchy.HierarchyNodeStruct;
import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoHierarchyBranch extends BranchPackage {

    /*
     * Builds one schema's hierarchy tab: every file under the folders it sits
     * in, marked while it holds unsaved edits, and in an array layout every
     * named entry beneath its file. Node keys name the folder, file, or entry
     * so a click can be routed back to InfoManager.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Nodes \\

    void buildNodes(InfoSchemaHandle schema, ObjectArrayList<HierarchyNodeStruct> roots) {

        ObjectArrayList<InfoDocumentInstance> documents = infoManager.getDocuments(schema);
        Object2ObjectOpenHashMap<String, HierarchyNodeStruct> folderName2FolderNode = new Object2ObjectOpenHashMap<>();

        for (int i = 0; i < documents.size(); i++) {

            InfoDocumentInstance document = documents.get(i);
            HierarchyNodeStruct fileNode = createFileNode(document);
            HierarchyNodeStruct folderNode = resolveFolderNode(
                    schema, document.getFolderName(), roots, folderName2FolderNode);

            if (folderNode != null)
                folderNode.addChild(fileNode);
            else
                roots.add(fileNode);

            if (schema.isArrayLayout())
                addEntryNodes(fileNode, document);
        }
    }

    private HierarchyNodeStruct resolveFolderNode(
            InfoSchemaHandle schema,
            String folderName,
            ObjectArrayList<HierarchyNodeStruct> roots,
            Object2ObjectOpenHashMap<String, HierarchyNodeStruct> folderName2FolderNode) {

        if (folderName.isEmpty())
            return null;

        HierarchyNodeStruct folderNode = folderName2FolderNode.get(folderName);

        if (folderNode != null)
            return folderNode;

        int separator = folderName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);
        String parentName = separator != EngineSetting.INDEX_NOT_FOUND ? folderName.substring(0, separator) : "";
        String label = separator != EngineSetting.INDEX_NOT_FOUND ? folderName.substring(separator + 1) : folderName;
        HierarchyNodeStruct parentNode = resolveFolderNode(schema, parentName, roots, folderName2FolderNode);

        folderNode = new HierarchyNodeStruct(
                EditorSetting.HIERARCHY_FOLDER_KEY_PREFIX + folderName,
                label,
                infoManager.isFolderSelected(schema, folderName),
                true);

        if (parentNode != null)
            parentNode.addChild(folderNode);
        else
            roots.add(folderNode);

        folderName2FolderNode.put(folderName, folderNode);
        return folderNode;
    }

    private HierarchyNodeStruct createFileNode(InfoDocumentInstance document) {

        String label = document.isDirty()
                ? document.getFileName() + EditorSetting.INFO_DIRTY_MARKER
                : document.getFileName();

        return new HierarchyNodeStruct(
                EditorSetting.HIERARCHY_FILE_KEY_PREFIX + document.getDefinitionName(),
                label,
                infoManager.isFileSelected(document),
                true);
    }

    private void addEntryNodes(HierarchyNodeStruct fileNode, InfoDocumentInstance document) {

        ObjectArrayList<String> entryNames = document.getEntryNames();

        for (int i = 0; i < entryNames.size(); i++)
            fileNode.addChild(new HierarchyNodeStruct(
                    EditorSetting.HIERARCHY_ENTRY_KEY_PREFIX + document.getDefinitionName()
                            + EditorSetting.HIERARCHY_KEY_SEPARATOR + entryNames.get(i),
                    entryNames.get(i),
                    infoManager.isEntrySelected(document, entryNames.get(i))));
    }
}
