package editor.runtime.sceneassets.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetRowStruct;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetSectionStruct;
import editor.bootstrap.sceneassetspipeline.sceneassetsmanager.SceneAssetsManager;
import editor.runtime.EditorSetting;
import editor.runtime.sceneassets.SceneAssetsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class SceneAssetsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the scene's assets.
     * Every frame it keeps the scene watched, so a Dev window feeds it only
     * while a Scene Assets tab is open; when the revision moves it refills
     * the list — a head for every section that holds anything, with how many
     * it holds, pressed to open or close it, and beneath an opened one a row
     * for each asset beside what it says of it, a row naming content pressed
     * to open it in the Properties tab.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private SceneAssetsManager sceneAssetsManager;

    // Menus
    private MenuInstance assetsMenu;
    private ElementInstance titleLabel;

    // State
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.sceneAssetsManager = get(SceneAssetsManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.assetsMenu = menuManager.openMenu(EditorSetting.SCENE_ASSETS_MENU, window);
        this.titleLabel = requireEntryPoint(SceneAssetsSetting.ENTRY_TITLE);

        requireEntryPoint(SceneAssetsSetting.ENTRY_ROWS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = assetsMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Scene assets menu '" + EditorSetting.SCENE_ASSETS_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        sceneAssetsManager.watch();

        if (sceneAssetsManager.getRevision() == shownRevision)
            return;

        this.shownRevision = sceneAssetsManager.getRevision();
        refreshRows();
    }

    // Rows \\

    private void refreshRows() {

        menuManager.ejectAll(assetsMenu, SceneAssetsSetting.ENTRY_ROWS);
        titleLabel.setFontText(String.format(SceneAssetsSetting.FORMAT_TITLE, sceneAssetsManager.getStatusText()));

        if (!sceneAssetsManager.isOwned()) {
            injectText(SceneAssetsSetting.TEXT_HINT_OPEN_DEV);
            return;
        }

        SceneAssetSectionStruct[] sections = sceneAssetsManager.getSections();

        for (int i = 0; i < sections.length; i++)
            if (sections[i].size() > 0)
                injectSection(sections[i], i);
    }

    // A section's head, and with it opened a row for each of its assets
    private void injectSection(SceneAssetSectionStruct section, int sectionIndex) {

        ElementInstance head = menuManager.inject(
                assetsMenu, SceneAssetsSetting.ENTRY_ROWS, SceneAssetsSetting.TEMPLATE_SECTION_ROW);

        head.setActionArgOverride(Integer.toString(sectionIndex));
        head.findChildById(SceneAssetsSetting.ELEMENT_ROW_LABEL).setFontText(
                (section.isOpened() ? SceneAssetsSetting.TEXT_SECTION_OPENED : SceneAssetsSetting.TEXT_SECTION_CLOSED)
                        + section.getKind().getLabel());
        head.findChildById(SceneAssetsSetting.ELEMENT_ROW_VALUE).setFontText(Integer.toString(section.size()));

        if (!section.isOpened())
            return;

        for (int i = 0; i < section.size(); i++)
            injectRow(section.get(i), sectionIndex, i);
    }

    private void injectRow(SceneAssetRowStruct asset, int sectionIndex, int rowIndex) {

        ElementInstance row = menuManager.inject(assetsMenu, SceneAssetsSetting.ENTRY_ROWS, asset.isContent()
                ? SceneAssetsSetting.TEMPLATE_LINK_ROW
                : SceneAssetsSetting.TEMPLATE_ROW);

        if (asset.isContent())
            row.setActionArgOverride(String.format(SceneAssetsSetting.FORMAT_ROW_KEY, sectionIndex, rowIndex));

        row.findChildById(SceneAssetsSetting.ELEMENT_ROW_LABEL).setFontText(asset.getLabel());
        row.findChildById(SceneAssetsSetting.ELEMENT_ROW_VALUE).setFontText(asset.getValue());
    }

    private void injectText(String text) {
        menuManager.inject(assetsMenu, SceneAssetsSetting.ENTRY_ROWS, SceneAssetsSetting.TEMPLATE_TEXT_ROW)
                .findChildById(SceneAssetsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }
}
