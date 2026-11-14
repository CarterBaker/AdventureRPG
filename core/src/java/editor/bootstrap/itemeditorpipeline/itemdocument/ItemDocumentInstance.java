package editor.bootstrap.itemeditorpipeline.itemdocument;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import editor.bootstrap.itemeditorpipeline.itementry.ItemEntryStruct;
import engine.root.InstancePackage;

public class ItemDocumentInstance extends InstancePackage {

    /*
     * One item mesh open in the editor: the item that last opened it, the
     * model being edited, and the selected part. The revision moves on every
     * model change so viewports know when to rebuild; dirty marks changes not
     * yet saved.
     */

    // Identity
    private ItemEntryStruct entry;

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
        return entry.getItemName();
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
