package editor.bootstrap.shortcutpipeline.shortcutmanager;

import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.shortcutpipeline.shortcut.ShortcutHandle;
import editor.bootstrap.shortcutpipeline.util.ShortcutModifier;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.runtime.menueventsmanager.menus.ExportDialogBranch;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import engine.input.Input;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShortcutManager extends ManagerPackage {

    /*
     * Owns every editor shortcut and is the one route from a key to the tool
     * it belongs to. Shortcuts load from ARPG, one tool's to a file, and are
     * registered by their qualified names. Each frame the focused window —
     * or, for a tab's chrome, the content it frames — is matched to its
     * context's shortcuts, and the first one pressed runs its action through
     * ElementHitSystem, exactly as a menu button would, inside that context's
     * crash boundary. A tool reads a held shortcut by the ID its name
     * resolves to once, so a gesture's modifiers are bound in data like
     * every key. Two shortcuts of one tool on the same keys are rejected at
     * load, and nothing is pressed while a dialog takes typed text.
     */

    // Internal
    private WindowManager windowManager;
    private InputManager inputManager;
    private TabManager tabManager;
    private ElementHitSystem elementHitSystem;
    private NameDialogBranch nameDialogBranch;
    private ExportDialogBranch exportDialogBranch;

    // Palette
    private Object2IntOpenHashMap<String> shortcutName2ShortcutID;
    private ObjectArrayList<ShortcutHandle> shortcutID2ShortcutHandle;
    private Object2ObjectOpenHashMap<Class<? extends ContextPackage>, ObjectArrayList<ShortcutHandle>>
            contextClass2PressedShortcuts;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.shortcutName2ShortcutID = RegistryUtility.createNameIndex();
        this.shortcutID2ShortcutHandle = RegistryUtility.createPalette();
        this.contextClass2PressedShortcuts = new Object2ObjectOpenHashMap<>();
        create(ShortcutLoader.class);
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.tabManager = get(TabManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.exportDialogBranch = get(ExportDialogBranch.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    int registerShortcutName(String shortcutName) {
        return RegistryUtility.registerID(
                shortcutName2ShortcutID, shortcutID2ShortcutHandle, shortcutName,
                EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addShortcutHandle(ShortcutHandle shortcutHandle) {

        int shortcutID = shortcutHandle.getShortcutID();

        if (shortcutID2ShortcutHandle.get(shortcutID) != null)
            throwException("Duplicate shortcut name: '" + shortcutHandle.getShortcutName()
                    + "' is declared more than once — every shortcut name must be unique in its file");

        shortcutID2ShortcutHandle.set(shortcutID, shortcutHandle);

        if (shortcutHandle.hasAction())
            addPressedShortcut(shortcutHandle);
    }

    private void addPressedShortcut(ShortcutHandle shortcutHandle) {

        elementHitSystem.verifyAction(shortcutHandle.getActionClass(), shortcutHandle.getActionMethod());

        ObjectArrayList<ShortcutHandle> pressedShortcuts = contextClass2PressedShortcuts.get(
                shortcutHandle.getContextClass());

        if (pressedShortcuts == null) {
            pressedShortcuts = new ObjectArrayList<>();
            contextClass2PressedShortcuts.put(shortcutHandle.getContextClass(), pressedShortcuts);
        }

        for (int i = 0; i < pressedShortcuts.size(); i++) {

            ShortcutHandle other = pressedShortcuts.get(i);

            if (other.getKey() == shortcutHandle.getKey() && other.getModifiers() == shortcutHandle.getModifiers())
                throwException("Shortcuts '" + other.getShortcutName() + "' and '"
                        + shortcutHandle.getShortcutName() + "' are both pressed with the same keys in one tool.");
        }

        pressedShortcuts.add(shortcutHandle);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = resolveShortcutWindow();

        if (window == null || isWithheld())
            return;

        ObjectArrayList<ShortcutHandle> pressedShortcuts = contextClass2PressedShortcuts.get(
                window.getContext().getClass());

        if (pressedShortcuts == null)
            return;

        Input input = inputManager.getRawInput(window);
        int heldModifiers = ShortcutModifier.resolveHeld(input);

        for (int i = 0; i < pressedShortcuts.size(); i++) {

            ShortcutHandle shortcut = pressedShortcuts.get(i);

            if (!shortcut.isPressed(input, heldModifiers))
                continue;

            elementHitSystem.executeAction(
                    shortcut.getActionClass(),
                    shortcut.getActionMethod(),
                    shortcut.getActionArg(),
                    window);
            return;
        }
    }

    // The window the keys belong to: the focused one, or the content of the tab whose chrome is focused
    private WindowInstance resolveShortcutWindow() {

        WindowInstance focused = windowManager.getFocusedWindow();

        if (focused == null)
            return null;

        WindowInstance window = tabManager.resolveContentWindow(focused);

        return window != null && window.getContext() != null ? window : null;
    }

    // A dialog taking typed text keeps every key to itself
    private boolean isWithheld() {
        return nameDialogBranch.isOpen() || exportDialogBranch.isOpen();
    }

    // Accessible \\

    public boolean hasShortcut(String shortcutName) {
        return RegistryUtility.getHandle(shortcutName2ShortcutID, shortcutID2ShortcutHandle, shortcutName) != null;
    }

    public int getShortcutIDFromShortcutName(String shortcutName) {

        if (!hasShortcut(shortcutName))
            throwException("Shortcut not found: \"" + shortcutName + "\"");

        return shortcutName2ShortcutID.getInt(shortcutName);
    }

    public ShortcutHandle getShortcutHandleFromShortcutID(int shortcutID) {

        ShortcutHandle handle = RegistryUtility.getHandle(shortcutID2ShortcutHandle, shortcutID);

        if (handle == null)
            throwException("Shortcut ID not found: " + shortcutID);

        return handle;
    }

    // True while the held shortcut's key and modifiers are down on the window's input
    public boolean isHeld(int shortcutID, WindowInstance window) {

        Input input = inputManager.getRawInput(window);

        return getShortcutHandleFromShortcutID(shortcutID).isHeld(input, ShortcutModifier.resolveHeld(input));
    }
}
