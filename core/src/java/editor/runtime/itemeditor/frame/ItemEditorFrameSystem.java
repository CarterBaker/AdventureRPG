package editor.runtime.itemeditor.frame;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class ItemEditorFrameSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * What this window's camera frames: the active item's model, however
     * many blocks it spans, about the middle of its blocks and held in a
     * sphere reaching their corners, so a larger or smaller model frames the
     * same.
     */

    // Internal
    private ItemEditorManager itemEditorManager;

    // Base \\

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
    }

    // Accessible \\

    @Override
    public boolean isShown() {
        return itemEditorManager.getActiveDocument() != null;
    }

    @Override
    public float getCenter(int axis) {

        SubVoxelModelStruct model = getModel();

        return 0.5f * switch (axis) {
            case EngineSetting.AXIS_X -> model.getBlocksX();
            case EngineSetting.AXIS_Y -> model.getBlocksY();
            default -> model.getBlocksZ();
        };
    }

    @Override
    public float getBoundingRadius() {

        SubVoxelModelStruct model = getModel();
        float x = model.getBlocksX();
        float y = model.getBlocksY();
        float z = model.getBlocksZ();

        return 0.5f * (float) Math.sqrt(x * x + y * y + z * z);
    }

    private SubVoxelModelStruct getModel() {
        return itemEditorManager.getActiveDocument().getModel();
    }
}
