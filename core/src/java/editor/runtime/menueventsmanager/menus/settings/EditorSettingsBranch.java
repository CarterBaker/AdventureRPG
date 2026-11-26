package editor.runtime.menueventsmanager.menus.settings;

import java.util.function.Consumer;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import editor.runtime.menueventsmanager.menus.search.SearchFieldStruct;
import editor.runtime.slider.SliderUtility;
import engine.input.Buttons;
import engine.input.Input;
import engine.input.InputListener;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorSettingsBranch extends BranchPackage implements InputListener {

    /*
     * The editor's settings dialog, opened from the toolbar over the whole
     * window. Its tabs list every page under the group it belongs to, and
     * the option list shows one page at a time — this branch owns the tabs,
     * the list, the shared rows and the footer's status, and each page's
     * branch fills it. Every change applies as it is made; the world
     * settings a Preview tab streams by apply once the dialog closes, which
     * also writes the editor's settings file. Escape, the close button or
     * Done closes it, unless a page is waiting for keys, which Escape then
     * cancels. The page last shown opens again next time. The search box in
     * its header, once clicked, takes the typing: while it holds a query the
     * list shows every page's rows whose name or value holds it, each page
     * under its own header, a section's rows kept whole when the section's
     * name or its page's holds it; Escape clears it and a click elsewhere
     * puts it down.
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
    private ElementInstance placedTrack;
    private int placedStep;

    // Search — the dialog's search box, the window input it types through and the rows a search takes away
    private SearchFieldStruct searchField;
    private Input textInput;
    private ObjectArrayList<ElementInstance> unmatchedRows;

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

        // Search
        this.searchField = new SearchFieldStruct(EditorSetting.SEARCH_SETTINGS_PLACEHOLDER);
        this.unmatchedRows = new ObjectArrayList<>();
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

        searchField.bind(findElement(EditorSetting.ELEMENT_SETTINGS_SEARCH_LABEL));
        this.textInput = inputManager.getRawInput(dialog.getWindow());
        textInput.addListener(this);

        showTab(activeTab != null ? activeTab : EditorSettingsTab.VALUES[0]);
    }

    // Applies what waits for the dialog to close, writes the settings file and closes it
    public void close() {

        if (dialog == null)
            return;

        cancelCaptures();

        textInput.removeListener(this);
        this.textInput = null;
        searchField.clear();
        searchField.blur();
        searchField.bind(null);

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

    private ElementInstance findElement(String id) {

        ObjectArrayList<ElementInstance> elements = dialog.getElements();

        for (int i = 0; i < elements.size(); i++) {

            ElementInstance found = elements.get(i).getElementData().getId().equals(id)
                    ? elements.get(i)
                    : elements.get(i).findChildById(id);

            if (found != null)
                return found;
        }

        return null;
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

        if (dialog == null || tab == activeTab && searchField.isEmpty())
            return;

        searchField.clear();
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

    // Fills the option list with the page on show, or with every page's rows the search matches, keeping where it is
    // scrolled to
    void refreshOptions() {

        menuManager.ejectAll(dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS);
        this.placedTrack = null;

        if (searchField.isEmpty()) {
            populate(activeTab);
            return;
        }

        int matched = 0;

        for (EditorSettingsTab tab : EditorSettingsTab.VALUES)
            matched += populateMatches(tab);

        setStatus(matched > 0
                ? matched + EditorSetting.SETTINGS_STATUS_SEARCH_COUNT
                : EditorSetting.SETTINGS_STATUS_SEARCH_NONE);
    }

    private void populate(EditorSettingsTab tab) {
        switch (tab) {
            case GENERAL -> editorSettingsOptionBranch.populateGeneral();
            case SHORTCUTS -> editorSettingsShortcutBranch.populateShortcuts();
            case COLORS -> editorSettingsColorBranch.populateColors();
            case GRAPHICS -> editorSettingsOptionBranch.populateGraphics();
            case EFFECTS -> editorSettingsOptionBranch.populateEffects();
            case GAMEPLAY -> editorSettingsOptionBranch.populateGameplay();
            case CONTROLS -> editorSettingsOptionBranch.populateControls();
        }
    }

    // A page laid under a header of its own name, then every row the search does not match taken away, and the
    // headers left with no row; returns the rows kept
    private int populateMatches(EditorSettingsTab tab) {

        ObjectArrayList<ElementInstance> rows = dialog.getEntryPoint(EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS)
                .getChildren();
        int pageStart = rows.size();

        injectSectionHeader(tab.getTitle());
        populate(tab);

        boolean pageMatches = searchField.matches(tab.getTitle());
        ElementInstance header = null;
        boolean headerMatches = false;
        boolean headerKept = false;
        int kept = 0;

        unmatchedRows.clear();

        for (int i = pageStart + 1; i < rows.size(); i++) {

            ElementInstance row = rows.get(i);

            if (isSectionHeader(row)) {

                if (header != null && !headerKept)
                    unmatchedRows.add(header);

                header = row;
                headerMatches = matchesRow(row);
                headerKept = false;
                continue;
            }

            if (pageMatches || headerMatches || matchesRow(row)) {
                headerKept = true;
                kept++;
            } else {
                unmatchedRows.add(row);
            }
        }

        if (header != null && !headerKept)
            unmatchedRows.add(header);

        if (kept == 0)
            unmatchedRows.add(rows.get(pageStart));

        for (int i = 0; i < unmatchedRows.size(); i++)
            menuManager.eject(dialog, EditorSetting.ENTRY_SETTINGS_DIALOG_OPTIONS, unmatchedRows.get(i));

        unmatchedRows.clear();

        return kept;
    }

    private boolean isSectionHeader(ElementInstance row) {
        return row.getElementData().getId().equals(EditorSetting.ELEMENT_SETTINGS_SECTION_HEADER);
    }

    private boolean matchesRow(ElementInstance row) {

        ElementInstance label = row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL);
        ElementInstance value = row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_VALUE);

        return label != null && searchField.matches(label.getText())
                || value != null && searchField.matches(value.getText());
    }

    // Search \\

    // The search box pressed: it takes the typing until it is put down
    public void focusSearch() {

        if (dialog == null)
            return;

        cancelCaptures();
        searchField.focus();
    }

    @Override
    public void onChar(char character) {

        if (dialog != null && !nameDialogBranch.isOpen() && searchField.type(character))
            refreshOptions();
    }

    // True when the search box took the keys this frame
    private boolean updateSearch(Input rawInput) {

        if (!searchField.isFocused())
            return false;

        if (rawInput.isMouseClicked(Buttons.LEFT) && !isPointerOver(EditorSetting.ELEMENT_SETTINGS_SEARCH)) {
            searchField.blur();
            return false;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {

            searchField.blur();

            if (searchField.clear())
                showTab(activeTab);

            return true;
        }

        if (rawInput.isKeyClicked(EditorSetting.KEY_BACKSPACE) && searchField.erase())
            refreshOptions();

        return true;
    }

    private boolean isPointerOver(String id) {

        ElementInstance element = findElement(id);
        WindowInstance window = dialog.getWindow();
        float x = inputManager.getHoverMouseX(window);
        float y = inputManager.getHoverMouseY(window);

        return element != null
                && x >= element.getComputedLeft()
                && x <= element.getComputedLeft() + element.getComputedW()
                && y >= element.getComputedTop()
                && y <= element.getComputedTop() + element.getComputedH();
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
        return SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track);
    }

    // Moves the knob to the step nearest the share; false when it already sits there, so its value text still stands
    boolean placeKnob(ElementInstance track, float fraction) {

        int step = SliderUtility.resolveStep(fraction);

        if (track == placedTrack && step == placedStep)
            return false;

        this.placedTrack = track;
        this.placedStep = step;
        SliderUtility.placeKnob(track, EditorSetting.ELEMENT_SETTINGS_SLIDER_KNOB, step);

        return true;
    }

    // Keys \\

    private void updateKeys(Input rawInput) {

        if (editorSettingsShortcutBranch.updateCapture(rawInput)
                || editorSettingsOptionBranch.updateCapture(rawInput)
                || updateSearch(rawInput))
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
