package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ItemEditBranch extends BranchPackage {

    /*
     * Performs every change to an item's model, its cubes, its walls and its
     * parts. An edit that would push the item past the mesh vertex limit is
     * undone whole and refused, so a saved item can always be loaded by the
     * game. Cubes change as the list of cells a stamp or a fill reached,
     * each moved into one part or emptied; a wall is placed or taken away on
     * its own. Every part is drawn in its one material, the material dropped
     * on it; a new part takes the material chosen in the Materials tab and a
     * name no part holds yet. Pick selects the part the struck cube or wall
     * belongs to.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;

    // Scratch — the part each cell held before the edit, to put back when it is refused
    private IntArrayList previousParts;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.previousParts = new IntArrayList();
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Cells \\

    // Every cell listed, by its grid index, moved into the part or emptied for INDEX_NOT_FOUND, as one edit
    boolean editCells(ItemDocumentInstance document, IntArrayList cells, int partIndex) {

        if (cells.isEmpty())
            return false;

        SubVoxelModelStruct model = document.getModel();

        previousParts.clear();

        for (int i = 0; i < cells.size(); i++) {
            previousParts.add(model.getCellPartAt(cells.getInt(i)));
            model.setCellPartAt(cells.getInt(i), partIndex);
        }

        if (!subVoxelManager.fitsMeshLimit(model)) {

            for (int i = 0; i < cells.size(); i++)
                model.setCellPartAt(cells.getInt(i), previousParts.getInt(i));

            return refuseEdit();
        }

        return acceptEdit(document);
    }

    // Walls \\

    void placeWall(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasWallPlacement())
            return;

        editWall(
                document, hit.getWallPlaceAxis(), hit.getWallPlaceX(), hit.getWallPlaceY(), hit.getWallPlaceZ(),
                document.getSelectedPartIndex());
    }

    void eraseWall(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget() || !hit.isTargetWall())
            return;

        editWall(
                document, hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(),
                EngineSetting.INDEX_NOT_FOUND);
    }

    // The wall moved into the part, or taken away for INDEX_NOT_FOUND
    boolean editWall(ItemDocumentInstance document, int axis, int x, int y, int z, int partIndex) {

        SubVoxelModelStruct model = document.getModel();
        int previousPart = model.getWallPart(axis, x, y, z);

        if (previousPart == partIndex)
            return false;

        writeWall(model, axis, x, y, z, partIndex);

        if (!subVoxelManager.fitsMeshLimit(model)) {
            writeWall(model, axis, x, y, z, previousPart);
            return refuseEdit();
        }

        return acceptEdit(document);
    }

    private void writeWall(SubVoxelModelStruct model, int axis, int x, int y, int z, int partIndex) {

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            model.clearWall(axis, x, y, z);
        else
            model.setWall(axis, x, y, z, partIndex);
    }

    // Pick \\

    // The part the struck cube or wall belongs to made the selected part, as an eyedropper takes up a color
    void pick(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        SubVoxelModelStruct model = document.getModel();

        itemEditorManager.selectPart(hit.isTargetWall()
                ? model.getWallPart(hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ())
                : model.getCellPart(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ()));
    }

    // Edits \\

    private boolean acceptEdit(ItemDocumentInstance document) {

        document.markEdited();
        itemEditorManager.notifyChanged();
        return true;
    }

    private boolean refuseEdit() {

        itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_MESH_LIMIT);
        return false;
    }

    // Parts \\

    // A new part in the material chosen, named Part and the first number no part's name takes yet
    void addPart(ItemDocumentInstance document, String materialName) {

        SubVoxelModelStruct model = document.getModel();

        if (model.getPartCount() >= EngineSetting.SUB_VOXEL_MAX_PARTS) {
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_PART_LIMIT);
            return;
        }

        int number = model.getPartCount() + 1;

        while (hasPartNamed(model, EditorSetting.ITEM_EDITOR_PART_NAME_PREFIX + number))
            number++;

        int partIndex = model.addPart(
                new SubVoxelPartStruct(EditorSetting.ITEM_EDITOR_PART_NAME_PREFIX + number, materialName));

        document.selectPart(partIndex);
        document.markEdited();
        itemEditorManager.notifyChanged();
    }

    void removeSelectedPart(ItemDocumentInstance document) {

        if (document.getModel().getPartCount() <= 1) {
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_LAST_PART);
            return;
        }

        SubVoxelModelStruct model = new SubVoxelModelStruct(document.getModel());
        model.removePart(document.getSelectedPartIndex());

        if (!subVoxelManager.fitsMeshLimit(model)) {
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_MESH_LIMIT);
            return;
        }

        document.replaceModel(model);
        document.markEdited();
        itemEditorManager.notifyChanged();
    }

    void renameSelectedPart(ItemDocumentInstance document, String partName) {

        document.getModel().getPart(document.getSelectedPartIndex()).setPartName(partName);
        document.markEdited();
        itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RENAMED + partName);
    }

    // The part drawn in the material, every cube and wall of it at once
    void setPartMaterial(ItemDocumentInstance document, int partIndex, String materialName) {

        SubVoxelPartStruct part = document.getModel().getPart(partIndex);

        if (part.getTextureName().equals(materialName))
            return;

        part.setTextureName(materialName);
        document.markEdited();
        itemEditorManager.setStatusMessage(
                EditorSetting.ITEM_EDITOR_MESSAGE_MATERIAL + part.getPartName() + EditorSetting.ITEM_EDITOR_MATERIAL_TO
                        + materialName);
    }

    // Utility \\

    static boolean hasPartNamed(SubVoxelModelStruct model, String partName) {

        for (int i = 0; i < model.getPartCount(); i++)
            if (model.getPart(i).getPartName().equals(partName))
                return true;

        return false;
    }
}
