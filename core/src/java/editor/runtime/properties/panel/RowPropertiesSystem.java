package editor.runtime.properties.panel;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.properties.PropertiesSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RowPropertiesSystem extends SystemPackage implements PropertiesView {

    /*
     * The Properties tab's view of a character, face or animation window's
     * value rows: the rows its window lists, through CharacterViewPanelSystem,
     * listed again whenever the window's revision moves — values that moved
     * set in place, so a dragged slider keeps its row — and the steps their
     * sliders ask for handed back to that window. The title is the window's.
     */

    // Internal
    private CharacterViewPanelSystem characterViewPanelSystem;
    private PropertiesSystem propertiesSystem;

    // Source
    private CharacterViewRowSource shownSource;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Rows
        this.rows = new ObjectArrayList<>();

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
        this.propertiesSystem = get(PropertiesSystem.class);
    }

    // View \\

    @Override
    public boolean refresh(PropertiesProvider provider, boolean force) {

        CharacterViewRowSource source = resolveSource(provider);

        if (!force && source == shownSource && source.getRowRevision() == shownRevision)
            return false;

        this.shownSource = source;
        this.shownRevision = source.getRowRevision();

        rows.clear();
        source.buildRows(rows);
        characterViewPanelSystem.fill(
                propertiesSystem.getMenu(),
                PropertiesSetting.ENTRY_ROWS,
                rows,
                source.getRowTemplates());

        return true;
    }

    @Override
    public String getTitle(PropertiesProvider provider) {
        return resolveSource(provider).getRowTitle();
    }

    @Override
    public void clear() {

        characterViewPanelSystem.clear(propertiesSystem.getMenu(), PropertiesSetting.ENTRY_ROWS);
        this.shownSource = null;
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    private CharacterViewRowSource resolveSource(PropertiesProvider provider) {

        if (!(provider instanceof CharacterViewRowSource))
            throwException("A window that shows value rows in the Properties tab must supply them.");

        return (CharacterViewRowSource) provider;
    }

    // Slider \\

    // The steps a row's slider asks for, handed to the window whose rows are shown
    public void stepRow(String key, int steps) {
        if (shownSource != null)
            shownSource.stepRow(key, steps);
    }
}
