package editor.runtime.characterview.panel;

import editor.runtime.characterview.util.CharacterViewRowKind;
import engine.root.StructPackage;

public class CharacterViewRowStruct extends StructPackage {

    /*
     * One row of a character tool's side panel: its kind, the key every one
     * of its buttons hands back to the tool, the label it shows, the value
     * beside it for numbers and choices, and whether an item is selected. A
     * ranged number also carries its amount, the range it may take and the
     * step it moves by, so its slider spans that range instead of a count of
     * steps about where the value stood.
     */

    // Identity
    private final CharacterViewRowKind kind;
    private final String key;

    // Display
    private final String label;
    private final String value;
    private final boolean selected;

    // Range
    private final float amount;
    private final float rangeMin;
    private final float rangeMax;
    private final float rangeStep;

    // Constructor \\

    public CharacterViewRowStruct(
            CharacterViewRowKind kind,
            String key,
            String label,
            String value,
            boolean selected,
            float amount,
            float rangeMin,
            float rangeMax,
            float rangeStep) {

        // Identity
        this.kind = kind;
        this.key = key;

        // Display
        this.label = label;
        this.value = value;
        this.selected = selected;

        // Range
        this.amount = amount;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
        this.rangeStep = rangeStep;
    }

    public static CharacterViewRowStruct header(String label) {
        return unranged(CharacterViewRowKind.HEADER, "", label, "", false);
    }

    public static CharacterViewRowStruct item(String key, String label, boolean selected) {
        return unranged(CharacterViewRowKind.ITEM, key, label, "", selected);
    }

    public static CharacterViewRowStruct number(String key, String label, String value) {
        return unranged(CharacterViewRowKind.NUMBER, key, label, value, false);
    }

    // A number whose slider spans its range, stepping by the step given
    public static CharacterViewRowStruct ranged(
            String key,
            String label,
            String value,
            float amount,
            float rangeMin,
            float rangeMax,
            float rangeStep) {
        return new CharacterViewRowStruct(
                CharacterViewRowKind.NUMBER, key, label, value, false, amount, rangeMin, rangeMax, rangeStep);
    }

    public static CharacterViewRowStruct text(String key, String label, String value) {
        return unranged(CharacterViewRowKind.TEXT, key, label, value, false);
    }

    public static CharacterViewRowStruct choice(String key, String label, String value) {
        return unranged(CharacterViewRowKind.CHOICE, key, label, value, false);
    }

    public static CharacterViewRowStruct action(String key, String label) {
        return unranged(CharacterViewRowKind.ACTION, key, label, "", false);
    }

    private static CharacterViewRowStruct unranged(
            CharacterViewRowKind kind,
            String key,
            String label,
            String value,
            boolean selected) {
        return new CharacterViewRowStruct(kind, key, label, value, selected, 0f, 0f, 0f, 0f);
    }

    // Range \\

    public boolean hasRange() {
        return rangeStep > 0f && rangeMax > rangeMin;
    }

    // Where the amount stands across the range, held to its ends
    public float getFraction() {
        return Math.clamp((amount - rangeMin) / (rangeMax - rangeMin), 0f, 1f);
    }

    // The steps from the range's low end the amount stands, or a share of the range lies, at
    public int getAmountSteps() {
        return Math.round((Math.clamp(amount, rangeMin, rangeMax) - rangeMin) / rangeStep);
    }

    public int toRangeSteps(float fraction) {
        return Math.round(fraction * (rangeMax - rangeMin) / rangeStep);
    }

    public float toRangeFraction(int steps) {
        return Math.clamp(steps * rangeStep / (rangeMax - rangeMin), 0f, 1f);
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
