package application.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import engine.input.Input;
import engine.input.Keys;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SettingsMenuBranch extends BranchPackage {

    /*
     * Runs the settings menu a main menu's Options opens. Opening hides that
     * menu; Back or Escape applies any pending world change through
     * SettingsSystem, saves the settings file, and shows that menu again. The
     * option list shows one SettingsTab at a time — this branch owns the tabs
     * and clears the list, and the section branches fill it. Display settings
     * move the game's own window, so an editor preview shows a note instead.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private WindowManager windowManager;
    private SettingsSystem settingsSystem;
    private SettingsOptionBranch settingsOptionBranch;
    private SettingsBindingBranch settingsBindingBranch;

    // Colors
    private MenuColorStruct activeTabColor;
    private MenuColorStruct activeTabLabelColor;

    // Per-window
    private Int2ObjectOpenHashMap<SettingsSessionStruct> windowID2Session;

    // Scratch
    private ObjectArrayList<SettingsSessionStruct> pendingCloses;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.activeTabColor = new MenuColorStruct(RuntimeSetting.SETTINGS_TAB_ACTIVE_COLOR);
        this.activeTabLabelColor = new MenuColorStruct(RuntimeSetting.SETTINGS_TAB_ACTIVE_LABEL_COLOR);

        // Per-window
        this.windowID2Session = new Int2ObjectOpenHashMap<>();

        // Scratch
        this.pendingCloses = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.settingsOptionBranch = get(SettingsOptionBranch.class);
        this.settingsBindingBranch = get(SettingsBindingBranch.class);
    }

    @Override
    protected void update() {

        if (windowID2Session.isEmpty())
            return;

        for (SettingsSessionStruct session : windowID2Session.values())
            updateKeyboard(session);

        for (int i = 0; i < pendingCloses.size(); i++)
            closeMenu(pendingCloses.get(i).getWindow());

        pendingCloses.clear();
    }

    // Open / Close \\

    public void openMenu(MenuInstance parentMenu, WindowInstance window) {

        int windowID = window.getWindowID();

        if (windowID2Session.containsKey(windowID))
            return;

        parentMenu.hide();

        SettingsSessionStruct session = new SettingsSessionStruct(
                menuManager.openMenu(RuntimeSetting.MENU_SETTINGS, window),
                parentMenu);

        windowID2Session.put(windowID, session);

        showTab(session, SettingsTab.values()[0]);
    }

    public void closeMenu(WindowInstance window) {

        SettingsSessionStruct session = windowID2Session.remove(window.getWindowID());

        if (session == null)
            return;

        if (session.isRenderSettingsChanged())
            settingsSystem.onRenderSettingsChanged();

        settingsSystem.saveSettings();

        menuManager.closeMenu(session.getSettingsMenu());
        session.getParentMenu().show();
    }

    // Tabs \\

    public void selectTab(String tabName, WindowInstance window) {

        SettingsSessionStruct session = getSession(window);
        SettingsTab tab = SettingsTab.valueOf(tabName);

        if (session == null || session.getActiveTab() == tab)
            return;

        showTab(session, tab);
    }

    private void showTab(SettingsSessionStruct session, SettingsTab tab) {

        session.setActiveTab(tab);
        session.setCapturingBinding(null);

        populateTabs(session);
        refreshOptions(session);

        session.getSettingsMenu().getEntryPoint(RuntimeSetting.ENTRY_SETTINGS_OPTIONS).setScrollY(0f);
    }

    private void populateTabs(SettingsSessionStruct session) {

        clearEntryPoint(session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_TABS);

        for (SettingsTab tab : SettingsTab.values())
            menuManager.inject(
                    session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_TABS, RuntimeSetting.MENU_SETTINGS_TAB,
                    button -> {
                        ElementInstance label = button.findChildById(RuntimeSetting.ELEMENT_SETTINGS_TAB_LABEL);
                        button.setActionArgOverride(tab.name());
                        label.setFontText(tab.getTitle());

                        if (tab != session.getActiveTab())
                            return;

                        button.setColorOverride(activeTabColor);
                        label.setColorOverride(activeTabLabelColor);
                    });
    }

    // Options \\

    void refreshOptions(SettingsSessionStruct session) {

        clearEntryPoint(session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS);

        SettingsTab tab = session.getActiveTab();

        switch (tab) {
            case DISPLAY -> populateDisplay(session);
            case GRAPHICS -> settingsOptionBranch.populateGraphics(session);
            case CONTROLS -> {
                settingsOptionBranch.populateMouse(session);
                settingsBindingBranch.populateBindings(session);
            }
            case AUDIO, GAMEPLAY, ACCESSIBILITY -> injectPlaceholder(
                    session,
                    tab.getTitle(),
                    RuntimeSetting.SETTINGS_PLACEHOLDER_COMING_SOON,
                    tab.getPlaceholderText());
        }
    }

    private void populateDisplay(SettingsSessionStruct session) {

        if (session.getWindow() != windowManager.getMainWindow()) {
            injectPlaceholder(
                    session,
                    SettingsTab.DISPLAY.getTitle(),
                    RuntimeSetting.SETTINGS_PLACEHOLDER_GAME_WINDOW_ONLY,
                    RuntimeSetting.SETTINGS_PLACEHOLDER_DISPLAY_UNAVAILABLE);
            return;
        }

        settingsOptionBranch.populateDisplay(session);
    }

    void injectSectionHeader(SettingsSessionStruct session, String title) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_SECTION_HEADER,
                header -> header.setFontText(title));
    }

    void injectNote(SettingsSessionStruct session, String text) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS, RuntimeSetting.MENU_SETTINGS_NOTE,
                note -> note.setFontText(text));
    }

    private void injectPlaceholder(SettingsSessionStruct session, String title, String subtitle, String text) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_PLACEHOLDER,
                note -> {
                    note.findChildById(RuntimeSetting.ELEMENT_SETTINGS_PLACEHOLDER_TITLE).setFontText(title);
                    note.findChildById(RuntimeSetting.ELEMENT_SETTINGS_PLACEHOLDER_SUBTITLE).setFontText(subtitle);
                    note.findChildById(RuntimeSetting.ELEMENT_SETTINGS_PLACEHOLDER_TEXT).setFontText(text);
                });
    }

    private void clearEntryPoint(MenuInstance menu, int entryPoint) {

        ElementInstance container = menu.getEntryPoint(entryPoint);

        while (!container.getChildren().isEmpty())
            menuManager.eject(menu, entryPoint, container.getChildren().get(0));
    }

    // Keyboard \\

    private void updateKeyboard(SettingsSessionStruct session) {

        Input rawInput = inputManager.getRawInput(session.getWindow());

        if (session.isCapturing()) {
            settingsBindingBranch.updateCapture(session, rawInput);
            return;
        }

        if (rawInput.isKeyClicked(Keys.ESCAPE))
            pendingCloses.add(session);
    }

    // Accessible \\

    public SettingsSessionStruct getSession(WindowInstance window) {
        return windowID2Session.get(window.getWindowID());
    }
}
