package editor.bootstrap.tabpipeline.util;

import engine.root.StructPackage;

public class TabDragLayoutStruct extends StructPackage {

    /*
     * Pure rect math for drop zone positioning. Computes the screen-space
     * origin and dimensions of the half-panel a tab would occupy after a
     * directed split into a given leaf. No allocation, no state.
     */

    // Internal \\

    // Zone Rect \\

    public float zoneX(float leafX, float leafW, DropZone zone) {

        if (zone == DropZone.RIGHT)
            return leafX + leafW * 0.5f;

        return leafX;
    }

    public float zoneY(float leafY, float leafH, DropZone zone) {

        if (zone == DropZone.BOTTOM)
            return leafY + leafH * 0.5f;

        return leafY;
    }

    public float zoneW(float leafW, DropZone zone) {

        if (zone == DropZone.LEFT || zone == DropZone.RIGHT)
            return leafW * 0.5f;

        return leafW;
    }

    public float zoneH(float leafH, DropZone zone) {

        if (zone == DropZone.TOP || zone == DropZone.BOTTOM)
            return leafH * 0.5f;

        return leafH;
    }
}