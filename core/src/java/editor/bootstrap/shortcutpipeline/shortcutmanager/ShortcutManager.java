package editor.bootstrap.shortcutpipeline.shortcutmanager;

import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.shortcutpipeline.shortcut.ShortcutData;
import editor.bootstrap.shortcutpipeline.shortcut.ShortcutHandle;
import editor.bootstrap.shortcutpipeline.util.ShortcutModifier;
import editor.bootstrap.shortcutpipeline.util.ShortcutUtility;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.menueventsmanager.menus.ExportDialogBranch;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import editor.runtime.menueventsmanager.menus.settings.EditorSettingsBranch;
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
     * registered by their qualified names. Each frame the active window, the
     * one ActiveWindowManager names whether or not the pointer is over it, is
     * matched to its context's shortcuts, and the first one pressed runs its action through
     * ElementHitSystem, exactly as a menu button would, inside that context's
     * crash boundary. A tool reads a held shortcut by the ID its name
     * resolves to once, so a gesture's modifiers are bound in data like
     * every key. Two shortcuts of one tool on the same keys are rejected at
     * load, and nothing is pressed while a dialog takes typed text or the
     * settings dialog is open. A shortcut rebound there takes its new keys in
     * place, under the same ID, once its file is saved. A file marked shared
     * holds keys that also work in every window ToolManager opens, the shared
     * tool keys, after that window's own, so a window's own key wins where the
     * two share one. The shortcuts that carry a hint are read out for the
     * hint bar through appendHints(), the window's own first, and a rebind
     * moves the revision so the bar reads them again. A window may claim the
     * keys of some of its shortcuts, as Musical Typing does: while it does,
     * the one check at dispatch skips every other shortcut on the same chord
     * there, and a shortcut marked claimed runs only while its window claims
     * it.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private InputManager inputManager;
    private ToolManager toolManager;
    private ElementHitSystem elementHitSystem;
    private NameDialogBranch nameDialogBranch;
    private ExportDialogBranch exportDialogBranch;
    private EditorSettingsBranch editorSettingsBranch;

    // Palette
    private Object2IntOpenHashMap<String> shortcutName2ShortcutID;
    private ObjectArrayList<ShortcutHandle> shortcutID2ShortcutHandle;
    private Object2ObjectOpenHashMap<Class<? extends ContextPackage>, ObjectArrayList<ShortcutHandle>>
            contextClass2PressedShortcuts;
    private Object2ObjectOpenHashMap<Class<? extends ContextPackage>, ObjectArrayList<ShortcutHandle>>
            contextClass2HintedShortcuts;
    private ObjectArrayList<Class<? extends ContextPackage>> sharedContextClasses;

    // Claims — the window claiming the keys of its shortcuts, and those shortcuts
    private WindowInstance claimWindow;
    private int[] claimShortcutIDs;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.shortcutName2ShortcutID = RegistryUtility.createNameIndex();
        this.shortcutID2ShortcutHandle = RegistryUtility.createPalette();
        this.contextClass2PressedShortcuts = new Object2ObjectOpenHashMap<>();
        this.contextClass2HintedShortcuts = new Object2ObjectOpenHashMap<>();
        this.sharedContextClasses = new ObjectArrayList<>();
        create(ShortcutLoader.class);
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.inputManager = get(InputManager.class);
        this.toolManager = get(ToolManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.exportDialogBranch = get(ExportDialogBranch.class);
        this.editorSettingsBranch = get(EditorSettingsBranch.class);
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

        if (shortcutHandle.getHint() != null)
            findOrCreateShortcuts(contextClass2HintedShortcuts, shortcutHandle.getContextClass()).add(shortcutHandle);
    }

    // A file's keys made to work in every window ToolManager opens as well as in its own tool
    void shareContext(Class<? extends ContextPackage> contextClass) {
        if (!sharedContextClasses.contains(contextClass))
            sharedContextClasses.add(contextClass);
    }

    private void addPressedShortcut(ShortcutHandle shortcutHandle) {

        elementHitSystem.verifyAction(shortcutHandle.getActionClass(), shortcutHandle.getActionMethod());

        ObjectArrayList<ShortcutHandle> pressedShortcuts = findOrCreateShortcuts(
                contextClass2PressedShortcuts, shortcutHandle.getContextClass());

        ShortcutHandle clash = findPressedClash(
                shortcutHandle,
                shortcutHandle.getKey(),
                shortcutHandle.getModifiers());

        if (clash != null)
            throwException("Shortcuts '" + clash.getShortcutName() + "' and '"
                    + shortcutHandle.getShortcutName() + "' are both pressed with the same keys in one tool.");

        pressedShortcuts.add(shortcutHandle);
    }

    private ObjectArrayList<ShortcutHandle> findOrCreateShortcuts(
            Object2ObjectOpenHashMap<Class<? extends ContextPackage>, ObjectArrayList<ShortcutHandle>> shortcuts,
            Class<? extends ContextPackage> contextClass) {

        ObjectArrayList<ShortcutHandle> contextShortcuts = shortcuts.get(contextClass);

        if (contextShortcuts == null) {
            contextShortcuts = new ObjectArrayList<>();
            shortcuts.put(contextClass, contextShortcuts);
        }

        return contextShortcuts;
    }

    // Gives the shortcut new keys in place, under the same ID, so every tool reading it follows at once
    public void rebindShortcut(int shortcutID, int key, int modifiers) {

        ShortcutHandle shortcutHandle = getShortcutHandleFromShortcutID(shortcutID);
        ShortcutData shortcutData = shortcutHandle.getShortcutData();

        if (shortcutHandle.hasAction() && key == EngineSetting.INDEX_NOT_FOUND)
            throwException("Shortcut '" + shortcutHandle.getShortcutName() + "' is pressed, so it needs a key.");

        if (key == EngineSetting.INDEX_NOT_FOUND && modifiers == ShortcutModifier.NONE)
            throwException("Shortcut '" + shortcutHandle.getShortcutName()
                    + "' is held, so it needs a key or a modifier to hold.");

        ShortcutHandle clash = findPressedClash(shortcutHandle, key, modifiers);

        if (clash != null)
            throwException("Shortcut '" + shortcutHandle.getShortcutName() + "' cannot take the keys of '"
                    + clash.getShortcutName() + "' in the same tool.");

        shortcutHandle.constructor(new ShortcutData(
                shortcutData.getShortcutName(),
                shortcutData.getShortcutID(),
                shortcutData.getContextClass(),
                key,
                modifiers,
                shortcutData.getActionClass(),
                shortcutData.getActionMethod(),
                shortcutData.getActionArg(),
                shortcutData.isClaimed(),
                shortcutData.getHint(),
                shortcutData.getCanvasMask(),
                shortcutData.getToolMask()));
        revision++;
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = activeWindowManager.getActiveWindow();

        if (window == null || window.getContext() == null || isWithheld())
            return;

        Input input = inputManager.getRawInput(window);
        int heldModifiers = ShortcutModifier.resolveHeld(input);

        ObjectArrayList<ShortcutHandle> ownShortcuts = findShortcuts(
                contextClass2PressedShortcuts, window.getContext().getClass());

        if (runPressed(ownShortcuts, window, input, heldModifiers) || !toolManager.isToolWindow(window))
            return;

        for (int i = 0; i < sharedContextClasses.size(); i++) {

            ObjectArrayList<ShortcutHandle> sharedShortcuts = contextClass2PressedShortcuts.get(
                    sharedContextClasses.get(i));

            if (runPressed(sharedShortcuts, window, input, heldModifiers))
                return;
        }
    }

    // A tool's shortcuts: its own context's, else the nearest context it extends that names some, so a file naming a
    // shared base keys every tab built on it
    private ObjectArrayList<ShortcutHandle> findShortcuts(
            Object2ObjectOpenHashMap<Class<? extends ContextPackage>, ObjectArrayList<ShortcutHandle>> shortcuts,
            Class<?> contextClass) {

        for (Class<?> type = contextClass; type != null; type = type.getSuperclass()) {

            ObjectArrayList<ShortcutHandle> contextShortcuts = shortcuts.get(type);

            if (contextShortcuts != null)
                return contextShortcuts;
        }

        return null;
    }

    // The first of the shortcuts pressed this frame run; false when none was
    private boolean runPressed(
            ObjectArrayList<ShortcutHandle> pressedShortcuts,
            WindowInstance window,
            Input input,
            int heldModifiers) {

        if (pressedShortcuts == null)
            return false;

        for (int i = 0; i < pressedShortcuts.size(); i++) {

            ShortcutHandle shortcut = pressedShortcuts.get(i);

            if (!shortcut.isPressed(input, heldModifiers) || isClaimedAway(shortcut, window))
                continue;

            elementHitSystem.executeAction(
                    shortcut.getActionClass(),
                    shortcut.getActionMethod(),
                    shortcut.getActionArg(),
                    window);
            return true;
        }

        return false;
    }

    // Claims \\

    // The window claims the chords of the shortcuts given, until it lets them go
    public void claimKeys(WindowInstance window, int[] shortcutIDs) {
        this.claimWindow = window;
        this.claimShortcutIDs = shortcutIDs;
    }

    public void releaseKeys(WindowInstance window) {

        if (claimWindow != window)
            return;

        this.claimWindow = null;
        this.claimShortcutIDs = null;
    }

    // The one claim check at dispatch: a claimed shortcut waits for its window to claim it, and while the window
    // claims keys every other shortcut on one of their chords gives way
    private boolean isClaimedAway(ShortcutHandle shortcut, WindowInstance window) {

        boolean claiming = window == claimWindow;

        if (shortcut.isClaimed() || !claiming)
            return shortcut.isClaimed() && !claiming;

        for (int i = 0; i < claimShortcutIDs.length; i++) {

            ShortcutHandle claimed = shortcutID2ShortcutHandle.get(claimShortcutIDs[i]);

            if (claimed != shortcut && claimed.getKey() == shortcut.getKey()
                    && claimed.getModifiers() == shortcut.getModifiers())
                return true;
        }

        return false;
    }

    // A dialog taking typed text, or the settings dialog, keeps every key to itself
    private boolean isWithheld() {
        return nameDialogBranch.isOpen() || exportDialogBranch.isOpen() || editorSettingsBranch.isOpen();
    }

    // Hints \\

    // The hints of a window's keys on its canvas with its tool, its own then the shared tool keys where it takes them
    public void appendHints(StringBuilder textBuilder, WindowInstance window, ToolCanvas canvas, EditorTool tool) {

        if (window == null || window.getContext() == null)
            return;

        appendHints(textBuilder, findShortcuts(contextClass2HintedShortcuts, window.getContext().getClass()),
                canvas, tool);

        if (!toolManager.isToolWindow(window))
            return;

        for (int i = 0; i < sharedContextClasses.size(); i++)
            appendHints(textBuilder, contextClass2HintedShortcuts.get(sharedContextClasses.get(i)), canvas, tool);
    }

    private void appendHints(
            StringBuilder textBuilder,
            ObjectArrayList<ShortcutHandle> hintedShortcuts,
            ToolCanvas canvas,
            EditorTool tool) {

        if (hintedShortcuts == null)
            return;

        for (int i = 0; i < hintedShortcuts.size(); i++) {

            ShortcutHandle shortcut = hintedShortcuts.get(i);

            if (!shortcut.isHintShown(canvas, tool))
                continue;

            if (!textBuilder.isEmpty())
                textBuilder.append(EditorSetting.HINT_BAR_ENTRY_SEPARATOR);

            ShortcutUtility.appendChord(textBuilder, shortcut.getKey(), shortcut.getModifiers());
            textBuilder.append(EditorSetting.HINT_BAR_KEY_SEPARATOR).append(shortcut.getHint());
        }
    }

    // Accessible \\

    // The pressed shortcut of the same tool already on these keys, other than the shortcut itself; null for none
    public ShortcutHandle findPressedClash(ShortcutHandle shortcutHandle, int key, int modifiers) {

        if (!shortcutHandle.hasAction())
            return null;

        ObjectArrayList<ShortcutHandle> pressedShortcuts = contextClass2PressedShortcuts.get(
                shortcutHandle.getContextClass());

        if (pressedShortcuts == null)
            return null;

        for (int i = 0; i < pressedShortcuts.size(); i++) {

            ShortcutHandle other = pressedShortcuts.get(i);

            if (other != shortcutHandle && other.getKey() == key && other.getModifiers() == modifiers)
                return other;
        }

        return null;
    }

    // Moves whenever a shortcut takes new keys, which the hint bar reads out again
    public int getRevision() {
        return revision;
    }

    // Every shortcut ID lies below this, in the order their files declare them
    public int getShortcutIDCount() {
        return shortcutID2ShortcutHandle.size();
    }

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

    // True on the frame the shortcut's key goes down on the window's input with exactly its modifiers held
    public boolean isPressed(int shortcutID, WindowInstance window) {

        Input input = inputManager.getRawInput(window);

        return getShortcutHandleFromShortcutID(shortcutID).isPressed(input, ShortcutModifier.resolveHeld(input));
    }

    // True while the held shortcut's key and modifiers are down on the window's input
    public boolean isHeld(int shortcutID, WindowInstance window) {

        Input input = inputManager.getRawInput(window);

        return getShortcutHandleFromShortcutID(shortcutID).isHeld(input, ShortcutModifier.resolveHeld(input));
    }
}
