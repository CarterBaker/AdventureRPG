package editor.bootstrap.itemeditorpipeline.itemdocument;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.InstancePackage;

public class ItemDocumentInstance extends InstancePackage {

    /*
     * One item open in the editor: its name, model, and selected part. The
     * revision moves on every model change; dirty marks unsaved edits and saved
     * marks that a file exists to reload from.
     */

    // Identity
    private String itemName;

    // Model
    private SubVoxelModelStruct model;

    // Selection
    private int selectedPartIndex;

    // State
    private boolean dirty;
    private boolean saved;
    private int revision;

    // Constructor \\

    public void constructor(String itemName, SubVoxelModelStruct model, boolean saved) {

        // Identity
        this.itemName = itemName;

        // Model
        this.model = model;

        // State
        this.dirty = !saved;
        this.saved = saved;
    }

    // Management \\

    public void markEdited() {

        this.dirty = true;
        this.revision++;
    }

    public void markSaved() {

        this.dirty = false;
        this.saved = true;
    }

    public void replaceModel(SubVoxelModelStruct model) {

        this.model = model;
        this.selectedPartIndex = Math.max(0, Math.min(selectedPartIndex, model.getPartCount() - 1));
        this.revision++;
    }

    public void selectPart(int partIndex) {

        if (!model.hasPart(partIndex))
            throwException("Item '" + itemName + "' has no part at index " + partIndex + ".");

        this.selectedPartIndex = partIndex;
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
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

    public boolean isSaved() {
        return saved;
    }

    public int getRevision() {
        return revision;
    }
}
