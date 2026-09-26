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
import engine.input.Input;
import engine.input.Keys;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterCreatorBranch extends BranchPackage {

    /*
     * Runs the character creator a main menu's New Game opens. Opening hides
     * that menu, rolls a fresh character through SaveManager, and turns the
     * window's player into the creator's live preview; Back returns to the
     * menu, Create names the character and begins play. The option list shows
     * one CreatorTab at a time — this branch owns the tabs and clears the list,
     * and the section branches fill it. Enter creates and Escape backs out.
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
        saveManager.newCharacter(window);
        playerManager.beginCharacterPreview(windowID);

        CreatorSessionStruct session = new CreatorSessionStruct(
                menuManager.openMenu(RuntimeSetting.MENU_CREATOR, window),
                parentMenu,
                playerManager.getPlayerForWindow(windowID),
                saveManager.getDefaultCharacterName());

        windowID2Session.put(windowID, session);

        creatorProgressionBranch.populateStats(session);
        creatorNameBranch.refreshName(session);
        showTab(session, CreatorTab.VALUES[0]);
    }

    public void closeMenu(WindowInstance window) {

        CreatorSessionStruct session = windowID2Session.remove(window.getWindowID());

        if (session == null)
            return;

        menuManager.closeMenu(session.getCreatorMenu());
        playerManager.endCharacterPreview(window.getWindowID());
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

        windowID2Session.remove(window.getWindowID());
        playerManager.endCharacterPreview(window.getWindowID());
        menuManager.closeMenu(session.getCreatorMenu());
        menuManager.closeMenu(session.getParentMenu());
    }

    public void randomizeCharacter(WindowInstance window) {

        CreatorSessionStruct session = getSession(window);

        if (session == null)
            return;

        creatorAppearanceBranch.randomizeAppearance(session.getPlayer());
        creatorBodyBranch.randomizeBody(session.getPlayer());
        refreshOptions(session);
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

    public void selectTab(String tabName, WindowInstance window) {

        CreatorSessionStruct session = getSession(window);
        CreatorTab tab = CreatorTab.valueOf(tabName);

        if (session == null || session.getActiveTab() == tab)
            return;

        showTab(session, tab);
    }

    private void showTab(CreatorSessionStruct session, CreatorTab tab) {

        session.setActiveTab(tab);

        populateTabs(session);
        refreshOptions(session);

        session.getCreatorMenu().getEntryPoint(RuntimeSetting.ENTRY_CREATOR_OPTIONS).setScrollY(0f);
    }

    private void populateTabs(CreatorSessionStruct session) {

        clearEntryPoint(session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_TABS);

        for (CreatorTab tab : CreatorTab.VALUES)
            menuManager.inject(
                    session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_TABS, RuntimeSetting.MENU_CREATOR_TAB,
                    button -> {
                        ElementInstance label = button.findChildById(RuntimeSetting.ELEMENT_CREATOR_TAB_LABEL);
                        button.setActionArgOverride(tab.name());
                        label.setFontText(tab.getTitle());

                        if (tab != session.getActiveTab())
                            return;

                        button.setColorOverride(activeTabColor);
                        label.setColorOverride(activeTabLabelColor);
                    });
    }

    // Options \\

    public void refreshOptions(CreatorSessionStruct session) {

        clearEntryPoint(session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS);

        switch (session.getActiveTab()) {
            case APPEARANCE -> creatorAppearanceBranch.populateFeatures(session);
            case HAIR -> creatorAppearanceBranch.populateHair(session);
            case BODY -> creatorBodyBranch.populateBody(session);
            case CLASS, PROFESSION, SKILLS -> creatorProgressionBranch.populatePlaceholder(session);
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
