package editor.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.shortcutpipeline.shortcut.ShortcutHandle;
import editor.bootstrap.shortcutpipeline.shortcutmanager.ShortcutManager;
import editor.bootstrap.shortcutpipeline.util.ShortcutModifier;
import editor.runtime.EditorSetting;
import engine.input.Input;
import engine.input.InputCode;
import engine.input.InputNameUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EditorSettingsShortcutBranch extends BranchPackage {

    /*
     * Fills the settings dialog's Shortcuts page with every tool's shortcuts,
     * grouped under the file that declares them in alphabetical order, and
     * rebinds them. Clicking a
     * shortcut waits for its new keys: a key pressed with whatever modifiers
     * are held, or, for a held shortcut, modifiers alone once they are let
     * go; Escape cancels. Keys another pressed shortcut of the same tool
     * already uses are refused, naming it. A rebind is written into its
     * shortcut file through the Info Panel's own save, with every check that
     * save makes, and takes effect at once; a file with unsaved Info Panel
     * edits is left alone until they are saved or reverted.
     */

    // Internal
    private ShortcutManager shortcutManager;
    private InfoManager infoManager;
    private EditorSettingsBranch editorSettingsBranch;

    // Colors
    private MenuColorStruct captureColor;

    // Capture
    private ShortcutHandle capturingShortcut;
    private int capturedModifiers;

    // Scratch
    private ObjectArrayList<String> setNames;
    private StringBuilder textBuilder;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.captureColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.SETTINGS_CAPTURE_ALPHA);

        // Scratch
        this.setNames = new ObjectArrayList<>();
        this.textBuilder = new StringBuilder();
    }

    @Override
    protected void get() {
        this.shortcutManager = get(ShortcutManager.class);
        this.infoManager = get(InfoManager.class);
        this.editorSettingsBranch = get(EditorSettingsBranch.class);
    }

    // Populate \\

    void populateShortcuts() {

        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_SHORTCUTS);
        collectSetNames();

        for (int i = 0; i < setNames.size(); i++) {

            String setName = setNames.get(i);
            editorSettingsBranch.injectSectionHeader(toWords(setName));

            for (int shortcutID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                    shortcutID < shortcutManager.getShortcutIDCount();
                    shortcutID++) {

                ShortcutHandle shortcut = shortcutManager.getShortcutHandleFromShortcutID(shortcutID);

                if (resolveSetName(shortcut).equals(setName))
                    injectShortcutRow(shortcut);
            }
        }
    }

    // Every shortcut file's name, in alphabetical order
    private void collectSetNames() {

        setNames.clear();

        for (int shortcutID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                shortcutID < shortcutManager.getShortcutIDCount();
                shortcutID++) {

            String setName = resolveSetName(shortcutManager.getShortcutHandleFromShortcutID(shortcutID));

            if (!setNames.contains(setName))
                setNames.add(setName);
        }

        setNames.sort(String.CASE_INSENSITIVE_ORDER);
    }

    private void injectShortcutRow(ShortcutHandle shortcut) {

        boolean capturing = capturingShortcut == shortcut;

        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_SHORTCUT_ROW,
                row -> {
                    row.setActionArgOverride(shortcut.getShortcutName());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(describeShortcut(shortcut));
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_VALUE).setFontText(capturing
                            ? EditorSetting.SETTINGS_SHORTCUT_CAPTURE_PROMPT
                            : formatChord(shortcut.getKey(), shortcut.getModifiers()));

                    if (capturing)
                        row.setColorOverride(captureColor);
                });
    }

    // Selection \\

    public void selectShortcut(String shortcutName) {

        if (!editorSettingsBranch.isOpen())
            return;

        ShortcutHandle shortcut = shortcutManager.getShortcutHandleFromShortcutID(
                shortcutManager.getShortcutIDFromShortcutName(shortcutName));
        String setName = resolveSetName(shortcut);

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_SHORTCUTS, setName)) {
            editorSettingsBranch.setStatus(String.format(EditorSetting.SETTINGS_SHORTCUT_FORMAT_UNSAVED, setName));
            return;
        }

        this.capturingShortcut = shortcut;
        this.capturedModifiers = ShortcutModifier.NONE;

        editorSettingsBranch.setStatus(shortcut.hasAction()
                ? EditorSetting.SETTINGS_STATUS_CAPTURE_PRESSED
                : EditorSetting.SETTINGS_STATUS_CAPTURE_HELD);
        editorSettingsBranch.refreshOptions();
    }

    // Capture \\

    // Takes the keys pressed for the shortcut waiting on them; false when none is waiting
    boolean updateCapture(Input rawInput) {

        if (capturingShortcut == null)
            return false;

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE)) {
            cancelCapture();
            editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_NONE);
            editorSettingsBranch.refreshOptions();
            return true;
        }

        int heldModifiers = ShortcutModifier.resolveHeld(rawInput);
        int key = resolvePressedKey(rawInput);

        if (key != EngineSetting.INDEX_NOT_FOUND) {
            rebind(key, heldModifiers);
            return true;
        }

        if (capturingShortcut.hasAction())
            return true;

        this.capturedModifiers |= heldModifiers;

        if (heldModifiers == ShortcutModifier.NONE && capturedModifiers != ShortcutModifier.NONE)
            rebind(EngineSetting.INDEX_NOT_FOUND, capturedModifiers);

        return true;
    }

    // The first named key other than a modifier or Escape pressed this frame, INDEX_NOT_FOUND for none
    private int resolvePressedKey(Input rawInput) {

        for (int key = 0; key < EngineSetting.INPUT_KEY_CODE_COUNT; key++)
            if (rawInput.isKeyClicked(key)
                    && key != EditorSetting.KEY_ESCAPE
                    && !ShortcutModifier.isModifierKey(key)
                    && InputNameUtility.hasName(InputCode.key(key)))
                return key;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    void cancelCapture() {
        this.capturingShortcut = null;
        this.capturedModifiers = ShortcutModifier.NONE;
    }

    // Rebind \\

    private void rebind(int key, int modifiers) {

        ShortcutHandle shortcut = capturingShortcut;
        ShortcutHandle clash = shortcutManager.findPressedClash(shortcut, key, modifiers);

        cancelCapture();

        if (clash != null)
            editorSettingsBranch.setStatus(String.format(
                    EditorSetting.SETTINGS_SHORTCUT_FORMAT_CLASH,
                    formatChord(key, modifiers),
                    toWords(resolveLocalName(clash))));
        else if (key == shortcut.getKey() && modifiers == shortcut.getModifiers())
            editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_NONE);
        else if (writeChord(shortcut, key, modifiers)) {
            shortcutManager.rebindShortcut(shortcut.getShortcutID(), key, modifiers);
            editorSettingsBranch.setStatus(String.format(
                    EditorSetting.SETTINGS_SHORTCUT_FORMAT_SAVED,
                    toWords(resolveLocalName(shortcut)),
                    formatChord(key, modifiers)));
        }

        editorSettingsBranch.refreshOptions();
    }

    // Writes the keys into the shortcut's file and saves it; on a refusal the file is put back and the reason shown
    private boolean writeChord(ShortcutHandle shortcut, int key, int modifiers) {

        String setName = resolveSetName(shortcut);
        String localName = resolveLocalName(shortcut);
        InfoEntryStruct entry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_SHORTCUTS, setName, setName);

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_SHORTCUTS, setName)) {
            editorSettingsBranch.setStatus(String.format(EditorSetting.SETTINGS_SHORTCUT_FORMAT_UNSAVED, setName));
            return false;
        }

        if (entry == null || findShortcutArpg(entry.getArpg(), localName) == null) {
            editorSettingsBranch.setStatus(String.format(
                    EditorSetting.SETTINGS_SHORTCUT_FORMAT_MISSING,
                    localName,
                    setName));
            return false;
        }

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_SHORTCUTS, setName, setName,
                arpg -> writeChordArpg(findShortcutArpg(arpg, localName), key, modifiers));

        if (infoManager.saveDocument(EditorSetting.INFO_SCHEMA_SHORTCUTS, setName))
            return true;

        String refusal = infoManager.getRefusal();

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_SHORTCUTS, setName);
        editorSettingsBranch.setStatus(EditorSetting.SETTINGS_SHORTCUT_REFUSED_PREFIX + refusal);

        return false;
    }

    // The entry naming the shortcut among the file's pressed and held shortcuts, null for none
    private ArpgObjectStruct findShortcutArpg(ArpgObjectStruct arpg, String localName) {

        ArpgObjectStruct shortcutArpg = findShortcutArpg(arpg, "pressed", localName);

        return shortcutArpg != null ? shortcutArpg : findShortcutArpg(arpg, "held", localName);
    }

    private ArpgObjectStruct findShortcutArpg(ArpgObjectStruct arpg, String listKey, String localName) {

        if (!ArpgUtility.hasArray(arpg, listKey))
            return null;

        ArpgArrayStruct shortcutsArpg = arpg.getAsArray(listKey);

        for (int i = 0; i < shortcutsArpg.size(); i++) {

            ArpgObjectStruct shortcutArpg = shortcutsArpg.get(i).getAsObject();

            if (localName.equals(ArpgUtility.getString(shortcutArpg, "name", null)))
                return shortcutArpg;
        }

        return null;
    }

    // Writes the keys after the shortcut's name, ahead of its action, as the shortcut files lay them out
    private void writeChordArpg(ArpgObjectStruct shortcutArpg, int key, int modifiers) {

        ObjectArrayList<String> trailingKeys = new ObjectArrayList<>();
        ObjectArrayList<ArpgElementStruct> trailingElements = new ObjectArrayList<>();

        for (String fieldKey : shortcutArpg.keySet())
            if (!fieldKey.equals("name") && !fieldKey.equals("key") && !fieldKey.equals("modifiers")) {
                trailingKeys.add(fieldKey);
                trailingElements.add(shortcutArpg.get(fieldKey));
            }

        shortcutArpg.remove("key");
        shortcutArpg.remove("modifiers");

        for (int i = 0; i < trailingKeys.size(); i++)
            shortcutArpg.remove(trailingKeys.get(i));

        if (key != EngineSetting.INDEX_NOT_FOUND)
            shortcutArpg.addProperty("key", InputNameUtility.getName(InputCode.key(key)));

        if (modifiers != ShortcutModifier.NONE)
            shortcutArpg.add("modifiers", writeModifiersArpg(modifiers));

        for (int i = 0; i < trailingKeys.size(); i++)
            shortcutArpg.add(trailingKeys.get(i), trailingElements.get(i));
    }

    private ArpgArrayStruct writeModifiersArpg(int modifiers) {

        ArpgArrayStruct modifiersArpg = new ArpgArrayStruct();

        for (ShortcutModifier modifier : ShortcutModifier.VALUES)
            if ((modifiers & modifier.getBit()) != 0)
                modifiersArpg.add(ArpgUtility.toEnumName(modifier));

        return modifiersArpg;
    }

    // Text \\

    // The keys as the editor reads them out, modifiers first: "Ctrl + Shift + S"
    private String formatChord(int key, int modifiers) {

        textBuilder.setLength(0);

        for (ShortcutModifier modifier : ShortcutModifier.VALUES) {

            if ((modifiers & modifier.getBit()) == 0)
                continue;

            if (!textBuilder.isEmpty())
                textBuilder.append(EngineSetting.INPUT_NAME_COMBO_SEPARATOR);

            textBuilder.append(modifier.getLabel());
        }

        if (key != EngineSetting.INDEX_NOT_FOUND) {

            if (!textBuilder.isEmpty())
                textBuilder.append(EngineSetting.INPUT_NAME_COMBO_SEPARATOR);

            textBuilder.append(InputNameUtility.getName(InputCode.key(key)));
        }

        return textBuilder.toString();
    }

    private String describeShortcut(ShortcutHandle shortcut) {

        String label = toWords(resolveLocalName(shortcut));

        return shortcut.hasAction() ? label : label + EditorSetting.SETTINGS_SHORTCUT_HELD_SUFFIX;
    }

    // A name as words, parted where a capital follows a lower-case letter or digit: "ExtendSelection" reads
    // "Extend Selection"
    private String toWords(String name) {

        textBuilder.setLength(0);

        for (int i = 0; i < name.length(); i++) {

            char character = name.charAt(i);

            if (i > 0 && Character.isUpperCase(character) && Character.isLetterOrDigit(name.charAt(i - 1))
                    && !Character.isUpperCase(name.charAt(i - 1)))
                textBuilder.append(EngineSetting.INPUT_NAME_WORD_SEPARATOR);

            textBuilder.append(character);
        }

        return textBuilder.toString();
    }

    // Utility \\

    // The shortcut file's name, the qualified name up to its last separator
    private String resolveSetName(ShortcutHandle shortcut) {

        String shortcutName = shortcut.getShortcutName();

        return shortcutName.substring(0, shortcutName.lastIndexOf(EditorSetting.SHORTCUT_NAME_SEPARATOR));
    }

    // The shortcut's own name within its file
    private String resolveLocalName(ShortcutHandle shortcut) {

        String shortcutName = shortcut.getShortcutName();

        return shortcutName.substring(
                shortcutName.lastIndexOf(EditorSetting.SHORTCUT_NAME_SEPARATOR)
                        + EditorSetting.SHORTCUT_NAME_SEPARATOR.length());
    }
}
