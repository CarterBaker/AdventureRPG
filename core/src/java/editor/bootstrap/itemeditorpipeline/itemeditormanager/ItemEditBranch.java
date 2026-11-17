package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemEditBranch extends BranchPackage {

    /*
     * Performs every change to an item's model, its cubes and its walls alike.
     * An edit that would push the item past the mesh vertex limit is undone
     * and refused, so a saved item can always be loaded by the game. Place
     * adds a cube of the selected part, Wall a double-sided wall on the
     * struck face or, near its edge, standing on that edge, Erase removes the
     * struck cube or wall, Brush moves it into the selected part and Pick
     * selects the part it belongs to.
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

    void applyTool(ItemDocumentInstance document, EditorTool tool, SubVoxelHitStruct hit) {

        switch (tool) {
            case PLACE -> place(document, hit);
            case WALL -> wall(document, hit);
            case ERASE -> erase(document, hit);
            case BRUSH -> paint(document, hit);
            case PICK -> pick(document, hit);
            default -> {
            }
        }
    }

    private void place(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasPlacement())
            return;

        editCell(document, hit.getPlaceX(), hit.getPlaceY(), hit.getPlaceZ(), document.getSelectedPartIndex());
    }

    private void wall(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasWallPlacement())
            return;

        editWall(
                document, hit.getWallPlaceAxis(), hit.getWallPlaceX(), hit.getWallPlaceY(), hit.getWallPlaceZ(),
                document.getSelectedPartIndex());
    }

    private void erase(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editTarget(document, hit, EngineSetting.INDEX_NOT_FOUND);
    }

    private void paint(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        editTarget(document, hit, document.getSelectedPartIndex());
    }

    // The part the struck cube or wall belongs to made the selected part, as an eyedropper takes up a color
    private void pick(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        SubVoxelModelStruct model = document.getModel();

        itemEditorManager.selectPart(hit.isTargetWall()
                ? model.getWallPart(hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ())
                : model.getCellPart(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ()));
    }

    private boolean editTarget(ItemDocumentInstance document, SubVoxelHitStruct hit, int partIndex) {

        if (hit.isTargetWall())
            return editWall(
                    document, hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), partIndex);

        return editCell(document, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(), partIndex);
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
