package launcher.runtime.menueventsmanager;

import application.runtime.menueventsmanager.menus.settings.SettingsBindingBranch;
import application.runtime.menueventsmanager.menus.settings.SettingsMenuBranch;
import application.runtime.menueventsmanager.menus.settings.SettingsOptionBranch;
import engine.root.ManagerPackage;
import launcher.runtime.menueventsmanager.menus.LauncherMenuBranch;

public class LauncherMenuEventsManager extends ManagerPackage {

    /*
     * Owns the launcher's menu callback branches: its own, and the game's
     * settings branches, so the launcher's Options is the game's settings
     * menu itself. Branches live in the context's registry, so the launcher's
     * menu actions reach them.
     */

    @Override
    protected void create() {
        create(LauncherMenuBranch.class);
        create(SettingsMenuBranch.class);
        create(SettingsOptionBranch.class);
        create(SettingsBindingBranch.class);
    }
}
