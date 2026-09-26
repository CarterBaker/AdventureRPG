package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.itemeditorpipeline.util.ItemEditorTool;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemEditBranch extends BranchPackage {

    /*
     * Performs every change to an item's model. An edit that would push the
     * item past the mesh vertex limit is undone and refused, so a saved item can
     * always be loaded by the game. With a brush texture chosen, Place and Paint
     * build with the part using that texture, creating it when none does.
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

    void applyTool(
            ItemDocumentInstance document,
            ItemEditorTool tool,
            SubVoxelHitStruct hit,
            String brushTextureName) {

        switch (tool) {
            case PLACE -> place(document, hit, brushTextureName);
            case ERASE -> erase(document, hit);
            case PAINT -> paint(document, hit, brushTextureName);
        }
    }

    private void place(ItemDocumentInstance document, SubVoxelHitStruct hit, String brushTextureName) {

        if (!hit.hasPlacement())
            return;

        build(document, hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ(), brushTextureName);
    }

    private void erase(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editCell(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), EngineSetting.INDEX_NOT_FOUND);
    }

    private void paint(ItemDocumentInstance document, SubVoxelHitStruct hit, String brushTextureName) {

        if (!hit.hasTarget())
            return;

        build(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), brushTextureName);
    }

    private void build(ItemDocumentInstance document, int x, int y, int z, String brushTextureName) {

        SubVoxelModelStruct model = document.getModel();
        int partCount = model.getPartCount();
        int partIndex = resolveBuildPart(document, brushTextureName);

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (!editCell(document, x, y, z, partIndex)) {

            if (model.getPartCount() > partCount)
                model.removePart(model.getPartCount() - 1);

            return;
        }

        document.selectPart(partIndex);
    }

    private int resolveBuildPart(ItemDocumentInstance document, String brushTextureName) {

        SubVoxelModelStruct model = document.getModel();
        int selectedPart = document.getSelectedPartIndex();

        if (brushTextureName == null || model.getPart(selectedPart).getTextureName().equals(brushTextureName))
            return selectedPart;

        int texturePart = subVoxelManager.findTexturePart(model, brushTextureName);

        if (texturePart != EngineSetting.INDEX_NOT_FOUND)
            return texturePart;

        if (model.getPartCount() >= EngineSetting.SUB_VOXEL_MAX_PARTS) {
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_PART_LIMIT);
            return EngineSetting.INDEX_NOT_FOUND;
        }

        return subVoxelManager.addTexturePart(model, brushTextureName);
    }

    private boolean editCell(ItemDocumentInstance document, int x, int y, int z, int partIndex) {

        SubVoxelModelStruct model = document.getModel();
        int previousPart = model.getCellPart(x, y, z);

        if (previousPart == partIndex)
            return false;

        writeCell(model, x, y, z, partIndex);

        if (!subVoxelManager.fitsMeshLimit(model)) {
            writeCell(model, x, y, z, previousPart);
            itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_MESH_LIMIT);
            return false;
        }

        document.markEdited();
        itemEditorManager.notifyChanged();
        return true;
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

    void renameSelectedPart(ItemDocumentInstance document, String partName) {

        document.getModel().getPart(document.getSelectedPartIndex()).setPartName(partName);
        document.markEdited();
        itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_RENAMED + partName);
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
