package editor.runtime.palette.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.EditorSetting;
import editor.runtime.palette.PaletteSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class PaletteSystem extends SystemPackage {

    /*
     * Binds the Palette tab to a UI render target and opens its menu, then
     * hands it to the view of whatever the window worked in last that has a
     * palette offers — the colour wheel over the rows of a window that
     * paints, the list a tool picks from, the biomes it paints, or nothing to
     * pick from — the colour wheel when no such window has been worked in
     * yet. When the window or what it offers changes, the
     * outgoing view takes its parts away and the incoming one fills the tab
     * at once; otherwise a view fills it again only when what it shows moves,
     * and the title is written after each fill, naming the window shown in
     * parentheses. The rows stand below the wheel's area while a view shows
     * the wheel and fill the tab under its header otherwise.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private PropertiesManager propertiesManager;
    private ActiveWindowManager activeWindowManager;
    private ImagePaletteSystem imagePaletteSystem;
    private RowPaletteSystem rowPaletteSystem;
    private BiomePaletteSystem biomePaletteSystem;

    // Menus
    private MenuInstance paletteMenu;

    // Views
    private PaletteView[] content2View;

    // Shown
    private PaletteProvider shownProvider;
    private PaletteContent shownContent;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.propertiesManager = get(PropertiesManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.imagePaletteSystem = get(ImagePaletteSystem.class);
        this.rowPaletteSystem = get(RowPaletteSystem.class);
        this.biomePaletteSystem = get(BiomePaletteSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.paletteMenu = menuManager.openMenu(PaletteSetting.MENU_PALETTE, window);

        // Views
        this.content2View = new PaletteView[PaletteContent.VALUES.length];
        content2View[PaletteContent.IMAGE.ordinal()] = imagePaletteSystem;
        content2View[PaletteContent.ROWS.ordinal()] = rowPaletteSystem;
        content2View[PaletteContent.BIOMES.ordinal()] = biomePaletteSystem;
        content2View[PaletteContent.NONE.ordinal()] = rowPaletteSystem;
    }

    // Update \\

    @Override
    protected void update() {

        PaletteProvider provider = propertiesManager.getPalette();
        PaletteContent content = provider != null ? provider.getPaletteContent() : PaletteContent.IMAGE;
        boolean switched = provider != shownProvider || content != shownContent;

        if (switched) {

            if (shownContent != null)
                content2View[shownContent.ordinal()].clear();

            this.shownProvider = provider;
            this.shownContent = content;
        }

        if (!content2View[content.ordinal()].refresh(provider, switched))
            return;

        ElementInstance title = paletteMenu.getEntryPoint(PaletteSetting.ENTRY_TITLE);

        if (title != null)
            title.setFontText(activeWindowManager.nameContext(
                    EditorSetting.TAB_TITLE_PALETTE,
                    propertiesManager.getPaletteWindow()));
    }

    // Layout \\

    // The rows below the wheel's area, or filling the tab under its header
    void placeRows(boolean belowWheel) {

        ElementInstance rows = paletteMenu.getEntryPoint(PaletteSetting.ENTRY_ROWS);

        if (rows == null)
            return;

        float top = PaletteSetting.HEADER_PIXELS + (belowWheel ? PaletteSetting.WHEEL_AREA_PIXELS : 0f);

        rows.setSizeOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercentWithOffset(EngineSetting.PERCENT_MAX, -PaletteSetting.ROWS_INSET_PIXELS),
                DimensionValueStruct.ofPercentWithOffset(EngineSetting.PERCENT_MAX, -top)));
    }

    // Accessible \\

    public MenuInstance getMenu() {
        return paletteMenu;
    }
}
