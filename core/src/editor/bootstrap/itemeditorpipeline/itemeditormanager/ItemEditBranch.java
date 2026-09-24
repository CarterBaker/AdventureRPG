package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemEditBranch extends BranchPackage {

    /*
     * Performs every change to an item's model. An edit that would push the
     * item past the mesh vertex limit is undone and refused, so a saved item can
     * always be loaded by the game.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;

    // Base \\

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
    }

    // Tools \\

    void applyTool(ItemDocumentInstance document, ItemEditorTool tool, SubVoxelHitStruct hit) {

        switch (tool) {
            case PLACE -> place(document, hit);
            case ERASE -> erase(document, hit);
            case PAINT -> paint(document, hit);
        }
    }

    private void place(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasPlacement())
            return;

        editCell(document, hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ(), document.getSelectedPartIndex());
    }

    private void erase(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editCell(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), EngineSetting.INDEX_NOT_FOUND);
    }

    private void paint(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editCell(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), document.getSelectedPartIndex());
    }

    private void editCell(ItemDocumentInstance document, int x, int y, int z, int partIndex) {

        SubVoxelModelStruct model = document.getModel();
        int previousPart = model.getCellPart(x, y, z);

        if (previousPart == partIndex)
            return;

        writeCell(model, x, y, z, partIndex);

        if (!subVoxelManager.fitsMeshLimit(model)) {
            writeCell(model, x, y, z, previousPart);
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_MESH_LIMIT);
            return;
        }

        document.markEdited();
        itemEditorManager.notifyChanged();
    }

    private void writeCell(SubVoxelModelStruct model, int x, int y, int z, int partIndex) {

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            model.clearCell(x, y, z);
        else
            model.setCell(x, y, z, partIndex);
    }

    // Parts \\

    void addPart(ItemDocumentInstance document, String partName) {

        SubVoxelModelStruct model = document.getModel();

        if (model.getPartCount() >= EngineSetting.SUB_VOXEL_MAX_PARTS) {
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_PART_LIMIT);
            return;
        }

        String textureName = model.getPart(document.getSelectedPartIndex()).getTextureName();
        int partIndex = model.addPart(new SubVoxelPartStruct(partName, textureName));

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

    void cycleTexture(ItemDocumentInstance document, ObjectArrayList<String> textureNames, int direction) {

        SubVoxelPartStruct part = document.getModel().getPart(document.getSelectedPartIndex());
        int current = Math.max(0, textureNames.indexOf(part.getTextureName()));
        int next = Math.floorMod(current + direction, textureNames.size());

        part.setTextureName(textureNames.get(next));
        document.markEdited();
        itemEditorManager.notifyChanged();
    }
}
