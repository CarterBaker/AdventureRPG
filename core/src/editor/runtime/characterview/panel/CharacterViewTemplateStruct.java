package editor.runtime.characterview.panel;

import engine.root.StructPackage;

public class CharacterViewTemplateStruct extends StructPackage {

    /*
     * The row templates one character tool's menu declares, one per row kind
     * and a second for a selected item, and the ids of the label, the value
     * and the button it sits on, the step buttons inside them, and a number
     * row's slider track and knob. A tool that lists no text rows declares
     * no text template. Each tool's
     * templates call its own menu branch, so the panels it fills route back
     * to it.
     */

    // Templates
    private final String headerTemplate;
    private final String itemTemplate;
    private final String selectedItemTemplate;
    private final String numberTemplate;
    private final String textTemplate;
    private final String choiceTemplate;
    private final String actionTemplate;

    // Elements
    private final String labelElement;
    private final String valueElement;
    private final String valueButtonElement;
    private final String decreaseElement;
    private final String increaseElement;
    private final String trackElement;
    private final String knobElement;

    // Constructor \\

    public CharacterViewTemplateStruct(
            String headerTemplate,
            String itemTemplate,
            String selectedItemTemplate,
            String numberTemplate,
            String textTemplate,
            String choiceTemplate,
            String actionTemplate,
            String labelElement,
            String valueElement,
            String valueButtonElement,
            String decreaseElement,
            String increaseElement,
            String trackElement,
            String knobElement) {

        // Templates
        this.headerTemplate = headerTemplate;
        this.itemTemplate = itemTemplate;
        this.selectedItemTemplate = selectedItemTemplate;
        this.numberTemplate = numberTemplate;
        this.textTemplate = textTemplate;
        this.choiceTemplate = choiceTemplate;
        this.actionTemplate = actionTemplate;

        // Elements
        this.labelElement = labelElement;
        this.valueElement = valueElement;
        this.valueButtonElement = valueButtonElement;
        this.decreaseElement = decreaseElement;
        this.increaseElement = increaseElement;
        this.trackElement = trackElement;
        this.knobElement = knobElement;
    }

    // Accessible \\

    public String getHeaderTemplate() {
        return headerTemplate;
    }

    public String getItemTemplate() {
        return itemTemplate;
    }

    public String getSelectedItemTemplate() {
        return selectedItemTemplate;
    }

    public String getNumberTemplate() {
        return numberTemplate;
    }

    // Null for a tool that lists no text rows
    public String getTextTemplate() {
        return textTemplate;
    }

    public String getChoiceTemplate() {
        return choiceTemplate;
    }

    public String getActionTemplate() {
        return actionTemplate;
    }

    public String getLabelElement() {
        return labelElement;
    }

    public String getValueElement() {
        return valueElement;
    }

    public String getValueButtonElement() {
        return valueButtonElement;
    }

    public String getDecreaseElement() {
        return decreaseElement;
    }

    public String getIncreaseElement() {
        return increaseElement;
    }

    public String getTrackElement() {
        return trackElement;
    }

    public String getKnobElement() {
        return knobElement;
    }
}
