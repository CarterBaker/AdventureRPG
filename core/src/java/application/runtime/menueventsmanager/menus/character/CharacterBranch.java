package application.runtime.menueventsmanager.menus.character;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.inventory.InventoryBranch;
import application.runtime.menueventsmanager.util.ProgressionDisplayBranch;
import engine.root.BranchPackage;
import engine.settings.KeyBindings;

public class CharacterBranch extends BranchPackage {

    /*
     * Runs the character sheet the inventory's Character button opens over it.
     * The sheet sums up who the player is — level, experience, profession,
     * gender, morality and the points it has to spend — and shows one
     * CharacterTab at a time; this branch owns the tabs and the summary and
     * clears the page, and the section branches fill it. The sheet redraws
     * only once the character or its progress through its level moves, or a
     * page asks for it. While open it covers the inventory, which neither
     * shows nor draws beneath it; Back, Pause or the inventory key close it
     * and uncover the inventory, and closing lets any staged points go
     * unspent.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private PlayerManager playerManager;
    private ProgressionManager progressionManager;
    private StatManager statManager;
    private GenderManager genderManager;
    private InventoryBranch inventoryBranch;
    private ProgressionDisplayBranch progressionDisplayBranch;
    private CharacterAttributeBranch characterAttributeBranch;
    private CharacterProficiencyBranch characterProficiencyBranch;
    private CharacterProfessionBranch characterProfessionBranch;

    // Colors
    private MenuColorStruct activeTabColor;
    private MenuColorStruct activeTabLabelColor;

    // State
    private CharacterSessionStruct session;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.activeTabColor = new MenuColorStruct(RuntimeSetting.CHARACTER_TAB_ACTIVE_COLOR);
        this.activeTabLabelColor = new MenuColorStruct(RuntimeSetting.CHARACTER_TAB_ACTIVE_LABEL_COLOR);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.statManager = get(StatManager.class);
        this.genderManager = get(GenderManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
        this.progressionDisplayBranch = get(ProgressionDisplayBranch.class);
        this.characterAttributeBranch = get(CharacterAttributeBranch.class);
        this.characterProficiencyBranch = get(CharacterProficiencyBranch.class);
        this.characterProfessionBranch = get(CharacterProfessionBranch.class);
    }

    @Override
    protected void update() {

        if (session == null)
            return;

        WindowInstance window = session.getWindow();

        if (inventoryBranch.getSession() == null
                || inputManager.bindingClicked(KeyBindings.PAUSE, window)
                || inputManager.bindingClicked(KeyBindings.INVENTORY, window)) {
            closeMenu(window);
            return;
        }

        EntityInstance player = session.getPlayer();
        int revision = progressionDisplayBranch.getDisplayRevision(player);
        double progress = player.getProgressionHandle().getProgress();

        if (revision != session.getShownRevision() || progress != session.getShownProgress())
            refresh(session);
    }

    // Open / Close \\

    public void openMenu(WindowInstance window) {

        int windowID = window.getWindowID();

        if (session != null || !playerManager.hasPlayerForWindow(windowID))
            return;

        inventoryBranch.setCovered(true);

        this.session = new CharacterSessionStruct(
                menuManager.openMenu(RuntimeSetting.MENU_CHARACTER, window),
                playerManager.getPlayerForWindow(windowID),
                statManager.getStatIDCount());
    }

    public void closeMenu(WindowInstance window) {

        if (session == null)
            return;

        progressionManager.clearStagedAttributes(session.getPlayer());
        menuManager.closeMenu(session.getSheetMenu());
        inventoryBranch.setCovered(false);

        this.session = null;
    }

    // Tabs \\

    public void selectTab(String tabName, WindowInstance window) {

        CharacterTab tab = CharacterTab.valueOf(tabName);

        if (session == null || session.getActiveTab() == tab)
            return;

        session.setActiveTab(tab);
        session.setConfirmArmed(false);
        session.getSheetMenu().getEntryPoint(RuntimeSetting.ENTRY_CHARACTER_OPTIONS).setScrollY(0f);
        refresh(session);
    }

    private void populateTabs(CharacterSessionStruct session) {

        menuManager.ejectAll(session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_TABS);

        for (CharacterTab tab : CharacterTab.VALUES)
            menuManager.inject(
                    session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_TABS, RuntimeSetting.MENU_CHARACTER_TAB,
                    button -> {
                        ElementInstance label = button.findChildById(RuntimeSetting.ELEMENT_CHARACTER_TAB_LABEL);
                        button.setActionArgOverride(tab.name());
                        label.setFontText(tab.getTitle());

                        if (tab != session.getActiveTab())
                            return;

                        button.setColorOverride(activeTabColor);
                        label.setColorOverride(activeTabLabelColor);
                    });
    }

    // Refresh \\

    // Redraws the tabs, the summary and the page on show, and remembers the state of the character it drew
    void refresh(CharacterSessionStruct session) {

        MenuInstance menu = session.getSheetMenu();
        EntityInstance player = session.getPlayer();

        session.setShownRevision(progressionDisplayBranch.getDisplayRevision(player));
        session.setShownProgress(player.getProgressionHandle().getProgress());

        populateTabs(session);
        populateSummary(session);
        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_CHARACTER_OPTIONS);
        setNotice(session, RuntimeSetting.CHARACTER_TEXT_NONE);

        switch (session.getActiveTab()) {
            case ATTRIBUTES -> characterAttributeBranch.populate(session);
            case TRADES -> characterProficiencyBranch.populateTrades(session);
            case SKILLS -> characterProficiencyBranch.populateSkills(session);
            case PROFESSION -> characterProfessionBranch.populate(session);
        }
    }

    private void populateSummary(CharacterSessionStruct session) {

        MenuInstance menu = session.getSheetMenu();
        EntityInstance player = session.getPlayer();

        menuManager.ejectAll(menu, RuntimeSetting.ENTRY_CHARACTER_SUMMARY);

        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_LEVEL, String.valueOf(player.getLevel()));
        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_EXPERIENCE,
                progressionDisplayBranch.formatExperience(player));
        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_PROFESSION,
                progressionDisplayBranch.formatProfession(player));
        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_GENDER,
                genderManager.getGenderHandleFromGenderID(player.getGenderID()).getDisplayName());
        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_MORALITY,
                progressionDisplayBranch.resolveMoralityWord(player));
        injectSummaryRow(menu, RuntimeSetting.CHARACTER_SUMMARY_POINTS,
                String.valueOf(player.getProgressionHandle().getUnspentPoints()));
    }

    private void injectSummaryRow(MenuInstance menu, String name, String value) {
        menuManager.inject(
                menu, RuntimeSetting.ENTRY_CHARACTER_SUMMARY, RuntimeSetting.MENU_CHARACTER_SUMMARY_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NAME).setFontText(name);
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_VALUE).setFontText(value);
                });
    }

    // Page \\

    void injectSectionHeader(CharacterSessionStruct session, String title) {
        menuManager.inject(
                session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_OPTIONS,
                RuntimeSetting.MENU_CHARACTER_SECTION_HEADER,
                header -> header.setFontText(title));
    }

    // A line of the page reading a name, a value and a note beside it, in a colour when one is given
    void injectValueRow(
            CharacterSessionStruct session,
            String name,
            String value,
            String note,
            MenuColorStruct color) {
        menuManager.inject(
                session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_OPTIONS, RuntimeSetting.MENU_CHARACTER_VALUE_ROW,
                row -> {
                    ElementInstance nameLabel = row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NAME);

                    nameLabel.setFontText(name);
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_VALUE).setFontText(value);
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NOTE).setFontText(note);

                    if (color != null)
                        nameLabel.setColorOverride(color);
                });
    }

    void setNotice(CharacterSessionStruct session, String text) {
        session.getSheetMenu().getEntryPoint(RuntimeSetting.ENTRY_CHARACTER_NOTICE).setFontText(text);
    }

    // Accessible \\

    public CharacterSessionStruct getSession(WindowInstance window) {
        return session != null && session.getWindow() == window ? session : null;
    }
}
