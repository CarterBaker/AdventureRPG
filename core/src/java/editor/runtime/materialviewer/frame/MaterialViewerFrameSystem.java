package editor.runtime.materialviewer.frame;

import editor.runtime.materialviewer.MaterialViewerSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.SystemPackage;

public class MaterialViewerFrameSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * What this window's camera frames: the shape the material is shown on,
     * standing at the origin, held in a sphere a share wider than the shape
     * radius every shape fits, so the frame holds whichever shape is chosen.
     */

    // Accessible \\

    @Override
    public boolean isShown() {
        return true;
    }

    @Override
    public float getCenter(int axis) {
        return 0f;
    }

    @Override
    public float getBoundingRadius() {
        return MaterialViewerSetting.SHAPE_RADIUS * MaterialViewerSetting.FRAME_RADIUS_SHARE;
    }
}
