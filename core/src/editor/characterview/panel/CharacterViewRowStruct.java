package editor.characterview.panel;

import editor.characterview.util.CharacterViewRowKind;
import engine.root.StructPackage;

public class CharacterViewRowStruct extends StructPackage {

    /*
     * One row of a character tool's side panel: its kind, the key every one
     * of its buttons hands back to the tool, the label it shows, the value
     * beside it for numbers and choices, and whether an item is selected.
     */

    // Identity
    private final CharacterViewRowKind kind;
    private final String key;

    // Display
    private final String label;
    private final String value;
    private final boolean selected;

    // Constructor \\

    public CharacterViewRowStruct(
            CharacterViewRowKind kind,
            String key,
            String label,
            String value,
            boolean selected) {

        // Identity
        this.kind = kind;
        this.key = key;

        // Display
        this.label = label;
        this.value = value;
        this.selected = selected;
    }

    public static CharacterViewRowStruct header(String label) {
        return new CharacterViewRowStruct(CharacterViewRowKind.HEADER, "", label, "", false);
    }

    public static CharacterViewRowStruct item(String key, String label, boolean selected) {
        return new CharacterViewRowStruct(CharacterViewRowKind.ITEM, key, label, "", selected);
    }

    public static CharacterViewRowStruct number(String key, String label, String value) {
        return new CharacterViewRowStruct(CharacterViewRowKind.NUMBER, key, label, value, false);
    }

    public static CharacterViewRowStruct choice(String key, String label, String value) {
        return new CharacterViewRowStruct(CharacterViewRowKind.CHOICE, key, label, value, false);
    }

    public static CharacterViewRowStruct action(String key, String label) {
        return new CharacterViewRowStruct(CharacterViewRowKind.ACTION, key, label, "", false);
    }

    // Accessible \\

    public CharacterViewRowKind getKind() {
        return kind;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public boolean isSelected() {
        return selected;
    }
}
