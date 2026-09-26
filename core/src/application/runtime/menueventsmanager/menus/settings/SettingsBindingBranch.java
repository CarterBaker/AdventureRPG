package application.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.settingspipeline.settingssystem.SettingsSystem;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.input.Binding;
import engine.input.BindingType;
import engine.input.Input;
import engine.input.InputCode;
import engine.input.InputNameUtility;
import engine.input.Keys;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;

public class SettingsBindingBranch extends BranchPackage {

    /*
     * Fills the Controls tab's binding rows and rebinds them. Clicking a row
     * waits for the next key or mouse button pressed on that window; Escape
     * cancels the wait and the menu's own click button is never taken. An
     * action given a key another listed action holds swaps keys with it, so
     * no action is ever left unbound. Reset Controls restores every default
     * binding. Bindings write through to Settings when the menu closes.
     */

    // Internal
    private MenuManager menuManager;
    private SettingsSystem settingsSystem;
    private SettingsMenuBranch settingsMenuBranch;

    // Colors
    private MenuColorStruct captureColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.captureColor = new MenuColorStruct(RuntimeSetting.SETTINGS_BINDING_CAPTURE_COLOR);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.settingsSystem = get(SettingsSystem.class);
        this.settingsMenuBranch = get(SettingsMenuBranch.class);
    }

    // Populate \\

    void populateBindings(SettingsSessionStruct session) {

        String section = null;

        for (SettingsBinding settingsBinding : SettingsBinding.VALUES) {

            if (!settingsBinding.getSection().equals(section)) {
                section = settingsBinding.getSection();
                settingsMenuBranch.injectSectionHeader(session, section);
            }

            injectBindingRow(session, settingsBinding);
        }

        injectResetRow(session);
    }

    private void injectBindingRow(SettingsSessionStruct session, SettingsBinding settingsBinding) {

        boolean capturing = session.getCapturingBinding() == settingsBinding;

        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_BINDING_ROW,
                row -> {
                    row.setActionArgOverride(settingsBinding.name());
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_BINDING_LABEL)
                            .setFontText(settingsBinding.getLabel());
                    row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_BINDING_VALUE).setFontText(capturing
                            ? RuntimeSetting.SETTINGS_BINDING_CAPTURE_PROMPT
                            : InputNameUtility.getName(settingsBinding.getBinding()));

                    if (capturing)
                        row.setColorOverride(captureColor);
                });
    }

    private void injectResetRow(SettingsSessionStruct session) {
        menuManager.inject(
                session.getSettingsMenu(), RuntimeSetting.ENTRY_SETTINGS_OPTIONS,
                RuntimeSetting.MENU_SETTINGS_ACTION_ROW,
                row -> row.findChildById(RuntimeSetting.ELEMENT_SETTINGS_ACTION_LABEL)
                        .setFontText(RuntimeSetting.SETTINGS_ACTION_RESET_BINDINGS));
    }

    // Selection \\

    public void selectBinding(String bindingName, WindowInstance window) {

        SettingsSessionStruct session = settingsMenuBranch.getSession(window);

        if (session == null)
            return;

        session.setCapturingBinding(SettingsBinding.valueOf(bindingName));
        settingsMenuBranch.refreshOptions(session);
    }

    public void resetBindings(WindowInstance window) {

        SettingsSessionStruct session = settingsMenuBranch.getSession(window);

        if (session == null)
            return;

        settingsSystem.resetBindings();
        session.setCapturingBinding(null);
        settingsMenuBranch.refreshOptions(session);
    }

    // Capture \\

    void updateCapture(SettingsSessionStruct session, Input rawInput) {

        if (rawInput.isKeyClicked(Keys.ESCAPE)) {
            session.setCapturingBinding(null);
            settingsMenuBranch.refreshOptions(session);
            return;
        }

        InputCode pressed = resolvePressedCode(rawInput);

        if (pressed == null)
            return;

        rebind(session.getCapturingBinding(), pressed);
        session.setCapturingBinding(null);
        settingsMenuBranch.refreshOptions(session);
    }

    private InputCode resolvePressedCode(Input rawInput) {

        for (int key = 0; key < EngineSetting.INPUT_KEY_CODE_COUNT; key++) {

            if (!rawInput.isKeyClicked(key))
                continue;

            InputCode inputCode = InputCode.key(key);

            if (InputNameUtility.hasName(inputCode))
                return inputCode;
        }

        for (int button = 0; button < EngineSetting.INPUT_MOUSE_BUTTON_COUNT; button++) {

            if (!rawInput.isMouseClicked(button) || isMenuButton(button))
                continue;

            InputCode inputCode = InputCode.mouse(button);

            if (InputNameUtility.hasName(inputCode))
                return inputCode;
        }

        return null;
    }

    private boolean isMenuButton(int button) {

        for (InputCode inputCode : KeyBindings.PRIMARY.getCodes())
            if (inputCode.type == BindingType.BUTTON && inputCode.code == button)
                return true;

        return false;
    }

    // Rebind \\

    private void rebind(SettingsBinding target, InputCode inputCode) {

        InputCode[] previousCodes = target.getBinding().getCodes();

        for (SettingsBinding settingsBinding : SettingsBinding.VALUES)
            if (settingsBinding != target && isBoundTo(settingsBinding.getBinding(), inputCode))
                settingsBinding.getBinding().set(previousCodes);

        target.getBinding().set(inputCode);
    }

    private boolean isBoundTo(Binding binding, InputCode inputCode) {

        InputCode[] codes = binding.getCodes();

        return codes.length == 1 && codes[0].matches(inputCode);
    }
}
