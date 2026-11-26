package editor.runtime.palette.panel;

import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.palette.PaletteSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EmptyPaletteSystem extends SystemPackage implements PaletteView {

    /*
     * The Palette tab's view of a window with nothing to pick from in what it
     * works on now, as the World Editor's height layer: one line saying so,
     * written once as the view takes the tab over.
     */

    // Internal
    private PaletteSystem paletteSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.rows = new ObjectArrayList<>();
        rows.add(CharacterViewRowStruct.header(EditorSetting.PALETTE_HEADER_NONE));
    }

    @Override
    protected void get() {
        this.paletteSystem = get(PaletteSystem.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PaletteProvider provider, boolean force) {

        if (!force)
            return false;

        paletteSystem.placeRows(false);
        characterViewPanelSystem.fill(
                paletteSystem.getMenu(),
                PaletteSetting.ENTRY_ROWS,
                rows,
                EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES);

        return true;
    }

    @Override
    public void clear() {
        characterViewPanelSystem.clear(paletteSystem.getMenu(), PaletteSetting.ENTRY_ROWS);
    }
}
