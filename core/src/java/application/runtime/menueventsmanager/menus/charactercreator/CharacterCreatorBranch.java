package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.MainMenuBranch;
import engine.input.Input;
import engine.input.Keys;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterCreatorBranch extends BranchPackage {

    /*
     * Runs the character creator a main menu's New Game opens. Opening hides
     * that menu, streams the play world through MainMenuBranch, rolls a fresh
     * character through SaveManager, and turns the window's player into the
     * creator's live preview; Back returns to the menu and its own world,
     * Create names the character, spends its staged creation points, takes up
     * its chosen profession and begins play; backing out lets the staging go.
     * The option list shows one tab at a time: the race's own look tabs,
     * laid out by its appearance, then the attributes and the profession.
     * This branch owns the tabs and clears the list, and the section
     * branches fill it; a race changed lists its own tabs again. Enter
     * creates and Escape backs out.
     */

    // Internal
    private MenuManager menuManager;
    private SaveManager saveManager;
    private PlayerManager playerManager;
    private InputManager inputManager;
    private CreatorNameBranch creatorNameBranch;
    private CreatorAppearanceBranch creatorAppearanceBranch;
    private CreatorBodyBranch creatorBodyBranch;
    private CreatorProgressionBranch creatorProgressionBranch;
    private MainMenuBranch mainMenuBranch;

    // Colors
    private MenuColorStruct activeTabColor;
    private MenuColorStruct activeTabLabelColor;

    // Per-window
    private Int2ObjectOpenHashMap<CreatorSessionStruct> windowID2Session;

    // Scratch
    private ObjectArrayList<CreatorSessionStruct> pendingConfirms;
    private ObjectArrayList<CreatorSessionStruct> pendingCloses;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.activeTabColor = new MenuColorStruct(RuntimeSetting.CREATOR_TAB_ACTIVE_COLOR);
        this.activeTabLabelColor = new MenuColorStruct(RuntimeSetting.CREATOR_TAB_ACTIVE_LABEL_COLOR);

        // Per-window
        this.windowID2Session = new Int2ObjectOpenHashMap<>();

        // Scratch
        this.pendingConfirms = new ObjectArrayList<>();
        this.pendingCloses = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.saveManager = get(SaveManager.class);
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputManager.class);
        this.creatorNameBranch = get(CreatorNameBranch.class);
        this.creatorAppearanceBranch = get(CreatorAppearanceBranch.class);
        this.creatorBodyBranch = get(CreatorBodyBranch.class);
        this.creatorProgressionBranch = get(CreatorProgressionBranch.class);
        this.mainMenuBranch = get(MainMenuBranch.class);
    }

    @Override
    protected void update() {

        if (windowID2Session.isEmpty())
            return;

        for (CreatorSessionStruct session : windowID2Session.values())
            updateKeyboard(session);

        for (int i = 0; i < pendingConfirms.size(); i++)
            confirmCharacter(pendingConfirms.get(i).getWindow());

        for (int i = 0; i < pendingCloses.size(); i++)
            closeMenu(pendingCloses.get(i).getWindow());

        pendingConfirms.clear();
        pendingCloses.clear();
    }

    // Open / Close \\

    public void openMenu(MenuInstance parentMenu, WindowInstance window) {

        int windowID = window.getWindowID();

        if (windowID2Session.containsKey(windowID))
            return;

        parentMenu.hide();
        mainMenuBranch.streamPlayWorld();
        saveManager.newCharacter(window);
        playerManager.beginCharacterPreview(windowID);

        CreatorSessionStruct session = new CreatorSessionStruct(
                menuManager.openMenu(RuntimeSetting.MENU_CREATOR, window),
                parentMenu,
                playerManager.getPlayerForWindow(windowID),
                creatorProgressionBranch.resolveDefaultProfession(),
                saveManager.getDefaultCharacterName());

        windowID2Session.put(windowID, session);

        creatorProgressionBranch.populateStats(session);
        creatorNameBranch.refreshName(session);
        showTab(session, CreatorTab.LOOK, 0);
    }

    public void closeMenu(WindowInstance window) {

        CreatorSessionStruct session = windowID2Session.remove(window.getWindowID());

        if (session == null)
            return;

        creatorProgressionBranch.clear(session);
        menuManager.closeMenu(session.getCreatorMenu());
        playerManager.endCharacterPreview(window.getWindowID());
        mainMenuBranch.streamMenuWorld();
        session.getParentMenu().show();
    }

    // Create \\

    public void confirmCharacter(WindowInstance window) {

        CreatorSessionStruct session = getSession(window);

        if (session == null)
            return;

        String characterName = creatorNameBranch.resolveName(session);

        if (characterName.isEmpty()) {
            creatorNameBranch.showStatus(session, RuntimeSetting.CREATOR_STATUS_NAME_EMPTY);
            return;
        }

        if (!saveManager.createCharacter(window, characterName)) {
            creatorNameBranch.showStatus(session, RuntimeSetting.CREATOR_STATUS_NAME_TAKEN);
            return;
        }

        creatorProgressionBranch.apply(session);
        windowID2Session.remove(window.getWindowID());
        playerManager.endCharacterPreview(window.getWindowID());
        menuManager.closeMenu(session.getCreatorMenu());
        mainMenuBranch.closeMenu(session.getParentMenu());
    }

    public void randomizeCharacter(WindowInstance window) {

        CreatorSessionStruct session = getSession(window);

        if (session == null)
            return;

        creatorAppearanceBranch.randomizeAppearance(session.getPlayer());
        creatorBodyBranch.randomizeBody(session.getPlayer());
        refreshOptions(session);
        creatorProgressionBranch.populateStats(session);
    }

    // Preview \\

    public void rotatePreview(WindowInstance window) {

        if (getSession(window) == null)
            return;

        playerManager.rotateCharacterPreview(
                window.getWindowID(),
                inputManager.getRawInput(window).getDeltaX() * RuntimeSetting.CREATOR_ROTATE_DEGREES_PER_PIXEL);
    }

    // Tabs \\

    public void selectTab(String argument, WindowInstance window) {

        CreatorSessionStruct session = getSession(window);
        String[] parts = argument.split(RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR);
        CreatorTab tab = CreatorTab.valueOf(parts[0]);
        int lookTabIndex = Integer.parseInt(parts[1]);

        if (session == null || session.getActiveTab() == tab && session.getLookTabIndex() == lookTabIndex)
            return;

        showTab(session, tab, lookTabIndex);
    }

    // The tabs listed again for the race the player is now, the look tab on show held within its tabs
    public void refreshTabs(CreatorSessionStruct session) {
        showTab(session, session.getActiveTab(), Math.min(
                session.getLookTabIndex(),
                creatorAppearanceBranch.getLookTabCount(session.getPlayer()) - 1));
    }

    private void showTab(CreatorSessionStruct session, CreatorTab tab, int lookTabIndex) {

        session.setActiveTab(tab);
        session.setLookTabIndex(lookTabIndex);

        populateTabs(session);
        refreshOptions(session);

        session.getCreatorMenu().getEntryPoint(RuntimeSetting.ENTRY_CREATOR_OPTIONS).setScrollY(0f);
    }

    // The race's look tabs, then every other tab
    private void populateTabs(CreatorSessionStruct session) {

        clearEntryPoint(session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_TABS);

        for (int i = 0; i < creatorAppearanceBranch.getLookTabCount(session.getPlayer()); i++)
            injectTab(session, CreatorTab.LOOK, i, creatorAppearanceBranch.getLookTabTitle(session.getPlayer(), i));

        for (CreatorTab tab : CreatorTab.VALUES)
            if (tab != CreatorTab.LOOK)
                injectTab(session, tab, 0, tab.getTitle());
    }

    private void injectTab(CreatorSessionStruct session, CreatorTab tab, int lookTabIndex, String title) {

        boolean active = tab == session.getActiveTab() && lookTabIndex == session.getLookTabIndex();

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_TABS, RuntimeSetting.MENU_CREATOR_TAB,
                button -> {
                    ElementInstance label = button.findChildById(RuntimeSetting.ELEMENT_CREATOR_TAB_LABEL);
                    button.setActionArgOverride(tab.name() + RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR + lookTabIndex);
                    label.setFontText(title);

                    if (!active)
                        return;

                    button.setColorOverride(activeTabColor);
                    label.setColorOverride(activeTabLabelColor);
                });
    }

    // Options \\

    public void refreshOptions(CreatorSessionStruct session) {

        clearEntryPoint(session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS);

        switch (session.getActiveTab()) {
            case LOOK -> creatorAppearanceBranch.populateLook(session);
            case ATTRIBUTES -> creatorProgressionBranch.populateAttributes(session);
            case PROFESSION -> creatorProgressionBranch.populateProfessions(session);
        }
    }

    public void injectSectionHeader(CreatorSessionStruct session, String title) {
        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS,
                RuntimeSetting.MENU_CREATOR_SECTION_HEADER,
                header -> header.setFontText(title));
    }

    private void clearEntryPoint(MenuInstance menu, int entryPoint) {

        ElementInstance container = menu.getEntryPoint(entryPoint);

        while (!container.getChildren().isEmpty())
            menuManager.eject(menu, entryPoint, container.getChildren().get(0));
    }

    // Keyboard \\

    private void updateKeyboard(CreatorSessionStruct session) {

        Input rawInput = inputManager.getRawInput(session.getWindow());

        if (rawInput.isKeyClicked(Keys.ENTER) || rawInput.isKeyClicked(Keys.NUMPAD_ENTER)) {
            pendingConfirms.add(session);
            return;
        }

        if (rawInput.isKeyClicked(Keys.ESCAPE)) {
            pendingCloses.add(session);
            return;
        }

        creatorNameBranch.updateNameInput(session, rawInput);
    }

    // Accessible \\

    public CreatorSessionStruct getSession(WindowInstance window) {
        return windowID2Session.get(window.getWindowID());
    }
}
