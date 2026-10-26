package editor.runtime.menueventsmanager.menus.settings;

import java.util.function.Consumer;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class EditorSettingsBranch extends BranchPackage {

    /*
     * The editor's settings dialog, opened from the toolbar over the whole
     * window. Its tabs list every page under the group it belongs to, and
     * the option list shows one page at a time — this branch owns the tabs,
     * the list, the shared rows and the footer's status, and each page's
     * branch fills it. Every change applies as it is made; the world
     * settings a Dev Mode tab streams by apply once the dialog closes, which
     * also writes the editor's settings file. Escape, the close button or
     * Done closes it, unless a page is waiting for keys, which Escape then
     * cancels. The page last shown opens again next time.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private SettingsSystem settingsSystem;
    private NameDialogBranch nameDialogBranch;
    private EditorSettingsOptionBranch editorSettingsOptionBranch;
    private EditorSettingsShortcutBranch editorSettingsShortcutBranch;
    private EditorSettingsColorBranch editorSettingsColorBranch;

    // Colors
    private MenuColorStruct activeTabColor;
    private MenuColorStruct activeTabMarkerColor;

    // Sliders
    private DimensionVector2Struct[] knobPositions;
    private ElementInstance placedTrack;
    private int placedStep;

    // Dialog
    private MenuInstance dialog;
    private ElementInstance statusLabel;
    private EditorSettingsTab activeTab;
    private boolean worldSettingsChanged;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.activeTabColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.SETTINGS_TAB_ACTIVE_ALPHA);
        this.activeTabMarkerColor = new MenuColorStruct(ThemeColor.ACCENT, EngineSetting.MENU_THEME_ALPHA_DEFAULT);

        // Sliders
        this.knobPositions = new DimensionVector2Struct[EditorSetting.SETTINGS_SLIDER_STEPS + 1];

        for (int i = 0; i < knobPositions.length; i++)
            knobPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(EngineSetting.PERCENT_MAX * i / EditorSetting.SETTINGS_SLIDER_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.editorSettingsOptionBranch = get(EditorSettingsOptionBranch.class);
        this.editorSettingsShortcutBranch = get(EditorSettingsShortcutBranch.class);
        this.editorSettingsColorBranch = get(EditorSettingsColorBranch.class);
    }

    @Override
    protected void update() {

        if (dialog == null)
            return;

        WindowInstance window = dialog.getWindow();

        if (!window.hasCompositeTarget()) {
            close();
            return;
        }

        fitToWindow(window);

        if (!nameDialogBranch.isOpen())
            updateKeys(inputManager.getRawInput(window));
    }

    // Open / Close \\

    public void open(WindowInstance window) {

        if (dialog != null)
            return;

        this.dialog = menuManager.openMenuWindow(EditorSetting.MENU_EDITOR_SETTINGS_DIALOG, window.getGLWindow());
        this.statusLabel = requireEntryPoint(EditorSetting.ENTRY_SETTINGS_DIALOG_STATUS);
        this.worldSettingsChanged = false;

        showTab(activeTab != null ? activeTab : EditorSettingsTab.VALUES[0]);
    }

    // Applies what waits for the dialog to close, writes the settings file and closes it
    public void close() {

        if (dialog == null)
            return;

        cancelCaptures();

        if (worldSettingsChanged)
            settingsSystem.onRenderSettingsChanged();

        settingsSystem.saveSettings();

        menuManager.closeMenuWindow(dialog);
        this.dialog = null;
        this.statusLabel = null;
        this.placedTrack = null;
        this.worldSettingsChanged = false;
    }

    public boolean isOpen() {
        return dialog != null;
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = dialog.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Settings dialog '" + EditorSetting.MENU_EDITOR_SETTINGS_DIALOG + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Tabs \\

    public void selectTab(String tabName) {

        EditorSettingsTab tab = EditorSettingsTab.valueOf(tabName);

        if (dialog == null || tab == activeTab)
            return;

        showTab(tab);
    }

    private void showTab(EditorSettingsTab tab) {

        this.activeTab = tab;

        cancelCaptures();
        setStatus(EditorSetting.SETTINGS_STATUS_NONE);
        populateTabs();
        refreshOptions();

        dialog.getEntryPoint(EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS).setScrollY(0f);
    }

    private void populateTabs() {

        menuManager.ejectAll(dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_TABS);

        String group = null;

        for (EditorSettingsTab tab : EditorSettingsTab.VALUES) {

            if (!tab.getGroup().equals(group)) {
                group = tab.getGroup();
                injectTabGroup(group);
            }

            injectTab(tab);
        }
    }

    private void injectTabGroup(String group) {
        menuManager.inject(
                dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_TABS, EditorSetting.MENU_EDITOR_SETTINGS_TAB_GROUP,
                header -> header.findChildById(EditorSetting.ELEMENT_SETTINGS_TAB_GROUP_LABEL).setFontText(group));
    }

    private void injectTab(EditorSettingsTab tab) {
        menuManager.inject(
                dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_TABS, EditorSetting.MENU_EDITOR_SETTINGS_TAB,
                button -> {
                    button.setActionArgOverride(tab.name());
                    button.findChildById(EditorSetting.ELEMENT_SETTINGS_TAB_LABEL).setFontText(tab.getTitle());

                    if (tab != activeTab)
                        return;

                    button.setColorOverride(activeTabColor);
                    button.findChildById(EditorSetting.ELEMENT_SETTINGS_TAB_MARKER)
                            .setColorOverride(activeTabMarkerColor);
                });
    }

    // Options \\

    // Fills the option list with the page on show, keeping where it is scrolled to
    void refreshOptions() {

        menuManager.ejectAll(dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS);
        this.placedTrack = null;

        switch (activeTab) {
            case GENERAL -> editorSettingsOptionBranch.populateGeneral();
            case SHORTCUTS -> editorSettingsShortcutBranch.populateShortcuts();
            case COLORS -> editorSettingsColorBranch.populateColors();
            case GRAPHICS -> editorSettingsOptionBranch.populateGraphics();
            case EFFECTS -> editorSettingsOptionBranch.populateEffects();
            case GAMEPLAY -> editorSettingsOptionBranch.populateGameplay();
            case CONTROLS -> editorSettingsOptionBranch.populateControls();
        }
    }

    ElementInstance injectRow(String masterKey, Consumer<ElementInstance> customizer) {
        return menuManager.inject(dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS, masterKey, customizer);
    }

    void injectSectionHeader(String title) {
        injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_SECTION_HEADER,
                header -> header.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(title));
    }

    void injectNote(String text) {
        injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_NOTE,
                note -> note.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(text));
    }

    void injectActionRow(EditorSettingsAction action, String label) {
        injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_ACTION_ROW,
                row -> {
                    row.setActionArgOverride(action.name());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(label);
                });
    }

    public void runAction(String actionName, WindowInstance window) {

        if (dialog == null)
            return;

        switch (EditorSettingsAction.valueOf(actionName)) {
            case RESET_CONTROLS -> editorSettingsOptionBranch.resetBindings();
            case ENTER_HEX -> editorSettingsColorBranch.openHexDialog(window);
            case RESET_COLOR -> editorSettingsColorBranch.resetSelectedColor();
            case RESET_COLORS -> editorSettingsColorBranch.resetColors();
        }
    }

    void markWorldSettingsChanged() {
        this.worldSettingsChanged = true;
    }

    // Sliders \\

    // The share of the track the pointer is over, held to the track's ends
    float resolveTrackFraction(WindowInstance window, ElementInstance track) {
        return Math.clamp(
                (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW(),
                0f,
                1f);
    }

    // Moves the knob to the step nearest the share; false when it already sits there, so its value text still stands
    boolean placeKnob(ElementInstance track, float fraction) {

        int step = Math.round(Math.clamp(fraction, 0f, 1f) * EditorSetting.SETTINGS_SLIDER_STEPS);

        if (track == placedTrack && step == placedStep)
            return false;

        this.placedTrack = track;
        this.placedStep = step;
        track.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_KNOB).setPositionOverride(knobPositions[step]);

        return true;
    }

    // Keys \\

    private void updateKeys(Input rawInput) {

        if (editorSettingsShortcutBranch.updateCapture(rawInput) || editorSettingsOptionBranch.updateCapture(rawInput))
            return;

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE))
            close();
    }

    private void cancelCaptures() {
        editorSettingsOptionBranch.cancelCapture();
        editorSettingsShortcutBranch.cancelCapture();
    }

    // Update \\

    // The dialog covers its OS window whatever size the window is given
    private void fitToWindow(WindowInstance window) {

        WindowInstance osWindow = window.getGLWindow();

        if (!window.isPlacedAt(0f, 0f, osWindow.getWidth(), osWindow.getHeight()))
            window.place(0f, 0f, osWindow.getWidth(), osWindow.getHeight());
    }

    // Accessible \\

    void setStatus(String text) {
        statusLabel.setFontText(text);
    }
}
