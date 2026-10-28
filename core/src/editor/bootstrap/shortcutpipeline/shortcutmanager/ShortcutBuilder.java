package editor.bootstrap.shortcutpipeline.shortcutmanager;

import java.io.File;

import editor.bootstrap.shortcutpipeline.shortcut.ShortcutData;
import editor.bootstrap.shortcutpipeline.shortcut.ShortcutHandle;
import editor.bootstrap.shortcutpipeline.util.ShortcutModifier;
import editor.runtime.EditorSetting;
import engine.input.InputNameUtility;
import engine.root.BuilderPackage;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShortcutBuilder extends BuilderPackage {

    /*
     * Parses one shortcut ARPG file into a ShortcutHandle per entry of its
     * "pressed" and "held". The file names the tool context its shortcuts
     * work in by its class, and "shared" makes them work in every window
     * that takes the tools too. Every shortcut needs a name, qualified by the
     * file's own; "key" names a key as the editor reads it out, ignoring
     * case, and "modifiers" the modifiers held with it. A pressed shortcut
     * needs a key and the "class" and "method" it runs, with "arg" as the
     * argument a menu button would hand over; a held one runs nothing and
     * needs a key or a modifier to hold.
     */

    // Internal
    private ShortcutManager shortcutManager;

    // Base \\

    @Override
    protected void get() {
        this.shortcutManager = get(ShortcutManager.class);
    }

    // Build \\

    ObjectArrayList<ShortcutHandle> build(File file, String setName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        Class<? extends ContextPackage> contextClass = parseContextClass(
                ArpgUtility.validateString(arpg, "context"), setName);
        ObjectArrayList<ShortcutHandle> shortcutHandles = new ObjectArrayList<>();

        if (ArpgUtility.getBoolean(arpg, "shared", false))
            shortcutManager.shareContext(contextClass);

        if (ArpgUtility.hasArray(arpg, "pressed")) {

            ArpgArrayStruct pressedArpg = ArpgUtility.validateArray(arpg, "pressed");

            for (int i = 0; i < pressedArpg.size(); i++)
                shortcutHandles.add(buildPressed(pressedArpg.get(i).getAsObject(), setName, contextClass));
        }

        if (ArpgUtility.hasArray(arpg, "held")) {

            ArpgArrayStruct heldArpg = ArpgUtility.validateArray(arpg, "held");

            for (int i = 0; i < heldArpg.size(); i++)
                shortcutHandles.add(buildHeld(heldArpg.get(i).getAsObject(), setName, contextClass));
        }

        return shortcutHandles;
    }

    private ShortcutHandle buildPressed(
            ArpgObjectStruct shortcutArpg,
            String setName,
            Class<? extends ContextPackage> contextClass) {

        String shortcutName = parseShortcutName(shortcutArpg, setName);

        return createShortcut(
                shortcutName,
                contextClass,
                parseKey(ArpgUtility.validateString(shortcutArpg, "key"), shortcutName),
                parseModifiers(shortcutArpg),
                ArpgUtility.validateString(shortcutArpg, "class"),
                ArpgUtility.validateString(shortcutArpg, "method"),
                ArpgUtility.getString(shortcutArpg, "arg", null));
    }

    private ShortcutHandle buildHeld(
            ArpgObjectStruct shortcutArpg,
            String setName,
            Class<? extends ContextPackage> contextClass) {

        String shortcutName = parseShortcutName(shortcutArpg, setName);
        int key = ArpgUtility.hasString(shortcutArpg, "key")
                ? parseKey(ArpgUtility.validateString(shortcutArpg, "key"), shortcutName)
                : EngineSetting.INDEX_NOT_FOUND;
        int modifiers = parseModifiers(shortcutArpg);

        if (key == EngineSetting.INDEX_NOT_FOUND && modifiers == ShortcutModifier.NONE)
            throwException("Shortcut '" + shortcutName + "' is held, so it needs a key or a modifier to hold.");

        return createShortcut(shortcutName, contextClass, key, modifiers, null, null, null);
    }

    private ShortcutHandle createShortcut(
            String shortcutName,
            Class<? extends ContextPackage> contextClass,
            int key,
            int modifiers,
            String actionClass,
            String actionMethod,
            String actionArg) {

        ShortcutData shortcutData = new ShortcutData(
                shortcutName,
                shortcutManager.registerShortcutName(shortcutName),
                contextClass,
                key,
                modifiers,
                actionClass,
                actionMethod,
                actionArg);

        ShortcutHandle shortcutHandle = create(ShortcutHandle.class);
        shortcutHandle.constructor(shortcutData);

        return shortcutHandle;
    }

    // Parse \\

    private Class<? extends ContextPackage> parseContextClass(String contextClassName, String setName) {

        try {
            Class<?> type = Class.forName(contextClassName);

            if (!ContextPackage.class.isAssignableFrom(type))
                throwException("Shortcut file '" + setName + "' names context '" + contextClassName
                        + "', which is not a context.");

            return type.asSubclass(ContextPackage.class);
        } catch (ClassNotFoundException e) {
            return throwException("Shortcut file '" + setName + "' names context class not found: "
                    + contextClassName, e);
        }
    }

    private String parseShortcutName(ArpgObjectStruct shortcutArpg, String setName) {
        return setName + EditorSetting.SHORTCUT_NAME_SEPARATOR + ArpgUtility.validateString(shortcutArpg, "name");
    }

    private int parseKey(String keyName, String shortcutName) {

        int key = InputNameUtility.findKey(keyName);

        if (key == EngineSetting.INDEX_NOT_FOUND)
            throwException("Shortcut '" + shortcutName + "' names unknown key '" + keyName + "'.");

        return key;
    }

    private int parseModifiers(ArpgObjectStruct shortcutArpg) {

        if (!ArpgUtility.hasArray(shortcutArpg, "modifiers"))
            return ShortcutModifier.NONE;

        ArpgArrayStruct modifiersArpg = ArpgUtility.validateArray(shortcutArpg, "modifiers");
        int modifiers = ShortcutModifier.NONE;

        for (int i = 0; i < modifiersArpg.size(); i++)
            modifiers |= ArpgUtility.toEnum(modifiersArpg.get(i).getAsString(), ShortcutModifier.class).getBit();

        return modifiers;
    }
}
