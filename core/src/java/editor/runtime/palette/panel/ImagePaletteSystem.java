package editor.runtime.palette.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.runtime.palette.PaletteSetting;
import editor.runtime.palette.wheel.ColorWheelSystem;
import engine.root.SystemPackage;

public class ImagePaletteSystem extends SystemPackage implements PaletteView {

    /*
     * The Palette tab's view of a window that paints, as a paint program's
     * colour docker sits over its channels: the colour wheel at the top, the
     * paint colour every painting window shares, and under it the window's
     * rows (the maps of the image it paints, and what else its tools pick
     * from) through RowPaletteSystem, the one path every palette's rows take.
     */

    // Internal
    private MenuManager menuManager;
    private PaletteSystem paletteSystem;
    private RowPaletteSystem rowPaletteSystem;
    private ColorWheelSystem colorWheelSystem;

    // Wheel
    private ElementInstance wheel;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.paletteSystem = get(PaletteSystem.class);
        this.rowPaletteSystem = get(RowPaletteSystem.class);
        this.colorWheelSystem = get(ColorWheelSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PaletteProvider provider, boolean force) {

        if (wheel == null)
            showWheel();

        return rowPaletteSystem.fillRows(provider, force);
    }

    @Override
    public void clear() {

        if (wheel != null)
            menuManager.eject(paletteSystem.getMenu(), PaletteSetting.ENTRY_WHEEL, wheel);

        colorWheelSystem.bind(null);
        rowPaletteSystem.clear();
        this.wheel = null;
    }

    private void showWheel() {

        this.wheel = menuManager.inject(
                paletteSystem.getMenu(),
                PaletteSetting.ENTRY_WHEEL,
                PaletteSetting.TEMPLATE_WHEEL);
        colorWheelSystem.bind(wheel);
        paletteSystem.placeRows(true);
    }
}
