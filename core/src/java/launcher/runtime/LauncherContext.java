package launcher.runtime;

import application.runtime.menu.MenuSystem;
import engine.root.ContextPackage;
import launcher.runtime.menu.LauncherMenuSystem;
import launcher.runtime.menueventsmanager.LauncherMenuEventsManager;
import launcher.runtime.window.LauncherWindowSystem;

public class LauncherContext extends ContextPackage {

    /*
     * Context paired with the launcher's borderless, transparent window. Binds
     * the menu render target, owns the launcher's menu branches, opens its
     * menus, and gives the window its body through LauncherWindowSystem.
     * Nothing else runs — no world, player or sky — and the launcher must
     * never pin the cursor.
     */

    // Runtime
    private MenuSystem menuSystem;
    private LauncherMenuEventsManager launcherMenuEventsManager;
    private LauncherMenuSystem launcherMenuSystem;
    private LauncherWindowSystem launcherWindowSystem;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.menuSystem = create(MenuSystem.class);
        this.launcherMenuEventsManager = create(LauncherMenuEventsManager.class);
        this.launcherMenuSystem = create(LauncherMenuSystem.class);
        this.launcherWindowSystem = create(LauncherWindowSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
