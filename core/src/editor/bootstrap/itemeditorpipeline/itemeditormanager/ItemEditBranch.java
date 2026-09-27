package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.util.function.IntPredicate;

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
     * Performs every change to an item's model, its cubes and its walls alike.
     * An edit that would push the item past the mesh vertex limit is undone
     * and refused, so a saved item can always be loaded by the game. With a
     * brush texture chosen, Place, Wall and Paint build with the part using
     * that texture, creating it when none does.
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
            case WALL -> wall(document, hit, brushTextureName);
            case ERASE -> erase(document, hit);
            case PAINT -> paint(document, hit, brushTextureName);
        }
    }

    private void place(ItemDocumentInstance document, SubVoxelHitStruct hit, String brushTextureName) {

        if (!hit.hasPlacement())
            return;

        build(document, brushTextureName, partIndex -> editCell(
                document, hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ(), partIndex));
    }

    private void wall(ItemDocumentInstance document, SubVoxelHitStruct hit, String brushTextureName) {

        if (!hit.hasWallPlacement())
            return;

        build(document, brushTextureName, partIndex -> editWall(
                document, hit.getWallPlaceAxis(), hit.getWallPlaceX(), hit.getWallPlaceY(), hit.getWallPlaceZ(),
                partIndex));
    }

    private void erase(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editTarget(document, hit, EngineSetting.INDEX_NOT_FOUND);
    }

    private void paint(ItemDocumentInstance document, SubVoxelHitStruct hit, String brushTextureName) {

        if (!hit.hasTarget())
            return;

        build(document, brushTextureName, partIndex -> editTarget(document, hit, partIndex));
    }

    private boolean editTarget(ItemDocumentInstance document, SubVoxelHitStruct hit, int partIndex) {

        if (hit.isTargetWall())
            return editWall(
                    document, hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), partIndex);

        return editCell(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), partIndex);
    }

    // Resolves the brush part, applies the edit with it, and drops a part the refused edit created
    private void build(ItemDocumentInstance document, String brushTextureName, IntPredicate edit) {

        SubVoxelModelStruct model = document.getModel();
        int partCount = model.getPartCount();
        int partIndex = resolveBuildPart(document, brushTextureName);

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (!edit.test(partIndex)) {

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
            return refuseEdit();
        }

        return acceptEdit(document);
    }

    private boolean editWall(ItemDocumentInstance document, int axis, int x, int y, int z, int partIndex) {

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

    private boolean acceptEdit(ItemDocumentInstance document) {

        document.markEdited();
        itemEditorManager.notifyChanged();
        return true;
    }

    private boolean refuseEdit() {

        itemEditorManager.setStatusMessage(EditorSetting.ITEM_EDITOR_MESSAGE_MESH_LIMIT);
        return false;
    }

    private void writeCell(SubVoxelModelStruct model, int x, int y, int z, int partIndex) {

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            model.clearCell(x, y, z);
        else
            model.setCell(x, y, z, partIndex);
    }

    private void writeWall(SubVoxelModelStruct model, int axis, int x, int y, int z, int partIndex) {

        if (partIndex == EngineSetting.INDEX_NOT_FOUND)
            model.clearWall(axis, x, y, z);
        else
            model.setWall(axis, x, y, z, partIndex);
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
