package editor.runtime.palette.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import editor.bootstrap.propertiespipeline.properties.MapsProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.palette.PaletteSetting;
import editor.runtime.palette.wheel.ColorWheelSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImagePaletteSystem extends SystemPackage implements PaletteView {

    /*
     * The Palette tab's view of a window that paints an image, as a paint
     * program's colour docker sits over its channels: the colour wheel at the
     * top, the paint colour every painting window shares, and under it the
     * maps of the image the window paints — every map it may carry, the one
     * picked, its settings, Generate and Clear — through the window's own row
     * templates, listed again whenever the window's maps revision moves. The
     * steps the maps' sliders ask for go back to that window.
     */

    // Internal
    private MenuManager menuManager;
    private PaletteSystem paletteSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private ColorWheelSystem colorWheelSystem;

    // Wheel
    private ElementInstance wheel;

    // Maps — the window listed and its maps revision when last listed
    private MapsProvider shownMaps;
    private int shownRevision;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Base \\

    @Override
    protected void create() {

        // Maps
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Rows
        this.rows = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.paletteSystem = get(PaletteSystem.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
        this.colorWheelSystem = get(ColorWheelSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PaletteProvider provider, boolean force) {

        MapsProvider maps = provider instanceof MapsProvider mapsProvider ? mapsProvider : null;
        int revision = maps != null ? maps.getMapsRevision() : 0;

        if (!force && maps == shownMaps && revision == shownRevision)
            return false;

        if (wheel == null)
            showWheel();

        this.shownMaps = maps;
        this.shownRevision = revision;

        rows.clear();

        if (maps != null)
            maps.buildMapsRows(rows);
        else
            rows.add(CharacterViewRowStruct.header(EditorSetting.MAPS_HEADER_NONE));

        characterViewPanelSystem.fill(
                paletteSystem.getMenu(),
                PaletteSetting.ENTRY_ROWS,
                rows,
                maps != null ? maps.getMapsTemplates() : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES);

        return true;
    }

    @Override
    public void clear() {

        if (wheel != null)
            menuManager.eject(paletteSystem.getMenu(), PaletteSetting.ENTRY_WHEEL, wheel);

        colorWheelSystem.bind(null);
        characterViewPanelSystem.clear(paletteSystem.getMenu(), PaletteSetting.ENTRY_ROWS);
        this.wheel = null;
        this.shownMaps = null;
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    private void showWheel() {

        this.wheel = menuManager.inject(
                paletteSystem.getMenu(),
                PaletteSetting.ENTRY_WHEEL,
                PaletteSetting.TEMPLATE_WHEEL);
        colorWheelSystem.bind(wheel);
        paletteSystem.placeRows(true);
    }

    // Rows \\

    // Whether a key names one of the maps rows listed
    public boolean listsRow(String key) {

        for (int i = 0; i < rows.size(); i++)
            if (key.equals(rows.get(i).getKey()))
                return true;

        return false;
    }

    // The steps a maps row's slider asks for, handed to the window whose maps are listed
    public void stepRow(String key, int steps) {
        if (shownMaps != null)
            shownMaps.stepMapsRow(key, steps);
    }
}
