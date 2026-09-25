package application.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.StructPackage;

public class SettingsSessionStruct extends StructPackage {

    /*
     * One window's open settings menu: the settings menu, the menu it was
     * opened from and returns to, the tab on show, the binding waiting for a
     * key if one is, and whether a world setting changed and still needs
     * applying when the menu closes.
     */

    // Menus
    private final MenuInstance settingsMenu;
    private final MenuInstance parentMenu;

    // Tab
    private SettingsTab activeTab;

    // Capture
    private SettingsBinding capturingBinding;

    // Render
    private boolean renderSettingsChanged;

    // Constructor \\

    public SettingsSessionStruct(MenuInstance settingsMenu, MenuInstance parentMenu) {

        // Menus
        this.settingsMenu = settingsMenu;
        this.parentMenu = parentMenu;
    }

    // Accessible \\

    public MenuInstance getSettingsMenu() {
        return settingsMenu;
    }

    public MenuInstance getParentMenu() {
        return parentMenu;
    }

    public WindowInstance getWindow() {
        return settingsMenu.getWindow();
    }

    public SettingsTab getActiveTab() {
        return activeTab;
    }

    public void setActiveTab(SettingsTab activeTab) {
        this.activeTab = activeTab;
    }

    public SettingsBinding getCapturingBinding() {
        return capturingBinding;
    }

    public void setCapturingBinding(SettingsBinding capturingBinding) {
        this.capturingBinding = capturingBinding;
    }

    public boolean isCapturing() {
        return capturingBinding != null;
    }

    public boolean isRenderSettingsChanged() {
        return renderSettingsChanged;
    }

    public void markRenderSettingsChanged() {
        this.renderSettingsChanged = true;
    }
}
