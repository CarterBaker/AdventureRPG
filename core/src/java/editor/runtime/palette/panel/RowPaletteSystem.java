package editor.runtime.palette.panel;

import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.bootstrap.propertiespipeline.properties.PaletteRowSource;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.palette.PaletteSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RowPaletteSystem extends SystemPackage implements PaletteView {

    /*
     * The Palette tab's view of what a window's tools pick from: the rows the
     * window lists (its parts, bones, gear, layers, nodes or maps, the one
     * picked marked), through the window's own row templates, listed again
     * whenever the window's palette revision moves, filling the tab under its
     * header; or one line saying there is nothing to pick from, for a window
     * that lists none in what it works on now. The image view lays the same
     * rows under the colour wheel. The steps the rows' sliders ask for go
     * back to the window listed.
     */

    // Internal
    private PaletteSystem paletteSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;

    // Source — the window listed and its revision when last listed
    private PaletteRowSource shownSource;
    private int shownRevision;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Base \\

    @Override
    protected void create() {

        // Source
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        // Rows
        this.rows = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.paletteSystem = get(PaletteSystem.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PaletteProvider provider, boolean force) {

        if (force)
            paletteSystem.placeRows(false);

        return fillRows(provider, force);
    }

    @Override
    public void clear() {
        characterViewPanelSystem.clear(paletteSystem.getMenu(), PaletteSetting.ENTRY_ROWS);
        this.shownSource = null;
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    // Rows \\

    // The window's rows listed when they moved, or whenever forced; a window listing none says so
    boolean fillRows(PaletteProvider provider, boolean force) {

        PaletteRowSource source = provider instanceof PaletteRowSource rowSource
                && provider.getPaletteContent() != PaletteContent.NONE
                ? rowSource
                : null;
        int revision = source != null ? source.getPaletteRevision() : 0;

        if (!force && source == shownSource && revision == shownRevision)
            return false;

        this.shownSource = source;
        this.shownRevision = revision;

        rows.clear();

        if (source != null)
            source.buildPaletteRows(rows);
        else
            rows.add(CharacterViewRowStruct.header(EditorSetting.PALETTE_HEADER_NONE));

        characterViewPanelSystem.fill(
                paletteSystem.getMenu(),
                PaletteSetting.ENTRY_ROWS,
                rows,
                source != null ? source.getPaletteTemplates() : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES);

        return true;
    }

    // Whether a key names one of the rows listed
    public boolean listsRow(String key) {

        for (int i = 0; i < rows.size(); i++)
            if (key.equals(rows.get(i).getKey()))
                return true;

        return false;
    }

    // The steps a listed row's slider asks for, handed to the window whose rows are listed
    public void stepRow(String key, int steps) {
        if (shownSource != null)
            shownSource.stepPaletteRow(key, steps);
    }
}
