package editor.runtime.maps;

import editor.bootstrap.propertiespipeline.properties.MapsProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MapsContext extends ContextPackage implements CharacterViewRowSource {

    /*
     * The one docker window listing the maps of the image worked in last
     * among the windows whose image carries them, laid out the way the Tools
     * tab lays out its tools: every map possible as an entry — normal in
     * both conventions, height, displacement, bump, occlusion, cavity,
     * curvature, thickness, roughness, glossiness, metallic, specular,
     * diffuse, emissive, opacity and tint beside the albedo — and below it
     * the settings of the one picked, with Generate, which makes the map
     * from the image by those settings, and Clear, which erases it. The
     * Image Editor and the character tools' textures list here alike; the
     * state lives in their managers, so every window follows. The list is
     * made again whenever the window it follows changes or that window's
     * list may have.
     */

    // Internal
    private PropertiesManager propertiesManager;
    private ActiveWindowManager activeWindowManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;

    // Shown — the maps followed and their revision when last asked, and this window's own revision over both
    private MapsProvider shownMaps;
    private int shownMapsRevision;
    private int revision;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
    }

    @Override
    protected void get() {
        this.propertiesManager = get(PropertiesManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    // Moves whenever the window followed changes or its own list moves
    @Override
    public int getRowRevision() {

        MapsProvider maps = propertiesManager.getMaps();
        int mapsRevision = maps != null ? maps.getMapsRevision() : 0;

        if (maps != shownMaps || mapsRevision != shownMapsRevision) {
            this.shownMaps = maps;
            this.shownMapsRevision = mapsRevision;
            revision++;
        }

        return revision;
    }

    @Override
    public String getRowTitle() {

        return activeWindowManager.nameContext(EditorSetting.TAB_TITLE_MAPS, propertiesManager.getMapsWindow());
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        MapsProvider maps = propertiesManager.getMaps();

        if (maps != null)
            maps.buildMapsRows(rows);
        else
            rows.add(CharacterViewRowStruct.header(EditorSetting.MAPS_HEADER_NONE));
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {

        MapsProvider maps = propertiesManager.getMaps();

        return maps != null ? maps.getMapsTemplates() : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {

        MapsProvider maps = propertiesManager.getMaps();

        if (maps != null)
            maps.stepMapsRow(key, steps);
    }
}
