package editor.runtime.properties.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.properties.PropertiesSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class PropertiesSystem extends SystemPackage {

    /*
     * Binds the Properties tab to a UI render target and opens its menu, then
     * hands its rows to the view of whatever the window worked in last
     * shows — its value rows, the content entry selected in the Hierarchy,
     * or the biome palette — the Info entry when no window has been worked in
     * yet. When the window or what it shows changes, the outgoing view takes
     * its rows away and the incoming one fills them at once; otherwise a view
     * fills them again only when what it shows moves, and the title is
     * written after each fill. The toolbar and status line always work on the
     * entry selected in the Hierarchy, the status drawn in the danger color
     * while it reports a problem.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private InfoManager infoManager;
    private PropertiesManager propertiesManager;
    private RowPropertiesSystem rowPropertiesSystem;
    private InfoPropertiesSystem infoPropertiesSystem;
    private BiomePropertiesSystem biomePropertiesSystem;

    // Menus
    private MenuInstance propertiesMenu;

    // Colors
    private MenuColorStruct problemColor;

    // Views
    private PropertiesView[] content2View;

    // Shown
    private PropertiesProvider shownProvider;
    private PropertiesContent shownContent;
    private int shownStatusRevision;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.problemColor = new MenuColorStruct(ThemeColor.DANGER, EngineSetting.COLOR_CHANNEL_MAX);

        // Shown
        this.shownStatusRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.infoManager = get(InfoManager.class);
        this.propertiesManager = get(PropertiesManager.class);
        this.rowPropertiesSystem = get(RowPropertiesSystem.class);
        this.infoPropertiesSystem = get(InfoPropertiesSystem.class);
        this.biomePropertiesSystem = get(BiomePropertiesSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.propertiesMenu = menuManager.openMenu(PropertiesSetting.MENU_PROPERTIES, window);

        // Views
        this.content2View = new PropertiesView[PropertiesContent.VALUES.length];
        content2View[PropertiesContent.ROWS.ordinal()] = rowPropertiesSystem;
        content2View[PropertiesContent.INFO.ordinal()] = infoPropertiesSystem;
        content2View[PropertiesContent.BIOMES.ordinal()] = biomePropertiesSystem;
    }

    // Update \\

    @Override
    protected void update() {

        PropertiesProvider provider = propertiesManager.getProvider();
        PropertiesContent content = provider != null ? provider.getPropertiesContent() : PropertiesContent.INFO;
        boolean switched = provider != shownProvider || content != shownContent;

        if (switched) {

            if (shownContent != null)
                content2View[shownContent.ordinal()].clear();

            this.shownProvider = provider;
            this.shownContent = content;
        }

        PropertiesView view = content2View[content.ordinal()];

        if (view.refresh(provider, switched))
            setEntryText(PropertiesSetting.ENTRY_TITLE, view.getTitle(provider));

        refreshStatus();
    }

    private void refreshStatus() {

        if (infoManager.getRevision() == shownStatusRevision)
            return;

        this.shownStatusRevision = infoManager.getRevision();

        ElementInstance label = propertiesMenu.getEntryPoint(PropertiesSetting.ENTRY_STATUS);

        if (label == null)
            return;

        label.setFontText(infoManager.getStatusText());
        label.setColorOverride(infoManager.hasStatusProblem() ? problemColor : null);
    }

    private void setEntryText(int entryPoint, String text) {

        ElementInstance label = propertiesMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(text);
    }

    // Accessible \\

    public MenuInstance getMenu() {
        return propertiesMenu;
    }
}
