package editor.bootstrap.itemeditorpipeline.itemdocument;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import engine.root.InstancePackage;

public class ItemDocumentInstance extends InstancePackage {

    /*
     * One item open in the editor: a game item's mesh, with the item that
     * last opened it, or a workspace item by its name in the workspace's
     * Items folder; the model being edited; and the selected part. The
     * revision moves on every model change so viewports know when to
     * rebuild; dirty marks changes not yet saved.
     */

    // Identity — the game item, or the workspace item's name; exactly one is set
    private ItemEntryStruct entry;
    private String workspaceName;

    // Model
    private SubVoxelModelStruct model;

    // Selection
    private int selectedPartIndex;

    // State
    private boolean dirty;
    private int revision;

    // Constructor \\

    public void constructor(ItemEntryStruct entry, SubVoxelModelStruct model, boolean dirty) {

        // Identity
        this.entry = entry;

        // Model
        this.model = model;

        // State
        this.dirty = dirty;
    }

    // A workspace item, kept in the workspace's Items folder until it is imported
    public void constructWorkspace(String workspaceName, SubVoxelModelStruct model) {

        // Identity
        this.workspaceName = workspaceName;

        // Model
        this.model = model;
    }

    // Management \\

    public void setEntry(ItemEntryStruct entry) {
        this.entry = entry;
    }

    public void markEdited() {

        this.dirty = true;
        this.revision++;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void replaceModel(SubVoxelModelStruct model) {

        this.model = model;
        this.selectedPartIndex = Math.max(0, Math.min(selectedPartIndex, model.getPartCount() - 1));
        this.revision++;
    }

    public void selectPart(int partIndex) {

        if (!model.hasPart(partIndex))
            throwException("Item '" + getItemName() + "' has no part at index " + partIndex + ".");

        this.selectedPartIndex = partIndex;
    }

    // Accessible \\

    public ItemEntryStruct getEntry() {
        return entry;
    }

    public String getItemName() {
        return entry != null ? entry.getItemName() : workspaceName;
    }

    // The name Delete asks to be typed back: the item's name within its definition, or the workspace item's
    public String getLocalName() {
        return entry != null ? entry.getLocalName() : workspaceName;
    }

    public boolean isWorkspace() {
        return workspaceName != null;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public SubVoxelModelStruct getModel() {
        return model;
    }

    public int getSelectedPartIndex() {
        return selectedPartIndex;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getRevision() {
        return revision;
    }
}
