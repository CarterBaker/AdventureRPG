package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.colorwheel.ColorWheelContext;
import engine.root.BranchPackage;

public class ColorWheelBranch extends BranchPackage {

    /*
     * Menu event handler for the Color Wheel tab: the wheel turned or its
     * triangle picked in, every frame it stays pressed, in the Color Wheel it
     * was pressed in.
     */

    // Wheel \\

    public void dragWheel(WindowInstance window, ElementInstance element) {
        if (window.getContext() instanceof ColorWheelContext colorWheelContext)
            colorWheelContext.dragWheel(element);
    }
}
