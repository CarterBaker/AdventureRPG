package launcher.runtime;

public class LauncherSetting {

    /*
     * Launcher-only constants — its asset manifest, its menu, the elements it
     * fills or grabs, its text, and how long the cursor must stay off every
     * menu before the window lets it through.
     */

    // Assets
    public static final String ASSET_MANIFEST = "Launcher";

    // Menus
    public static final String MENU_MAIN = "Launcher/Main";

    // Elements
    public static final int ELEMENT_INDEX_BOARD = 0;
    public static final int ELEMENT_INDEX_TITLE = 1;
    public static final String ELEMENT_VERSION = "version";

    // Text
    public static final String VERSION_FORMAT = "Version %s";

    // Passthrough — a menu opened this frame is laid out the next, so the cursor settles a frame before it passes
    public static final int PASSTHROUGH_SETTLE_FRAMES = 2;
}
