package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.palette.PaletteContext;
import engine.root.BranchPackage;

public class ColorWheelBranch extends BranchPackage {

    /*
     * Menu event handler for the Palette tab's colour wheel: the wheel turned
     * or its triangle picked in, every frame it stays pressed, in the Palette
     * it was pressed in.
     */

    // Wheel \\

    public void dragWheel(WindowInstance window, ElementInstance element) {
        if (window.getContext() instanceof PaletteContext paletteContext)
            paletteContext.dragWheel(element);
    }
}
