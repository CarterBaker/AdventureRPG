package application.runtime.menueventsmanager.menus.charactercreator;

import application.runtime.RuntimeSetting;

public enum CreatorTab {

    /*
     * The sections of the character creator, in the order their tabs are
     * listed. Each tab is one page of the scrolling option list; a tab with
     * placeholder text is a section still to come and shows that text.
     */

    APPEARANCE(RuntimeSetting.CREATOR_TAB_APPEARANCE, null),
    HAIR(RuntimeSetting.CREATOR_TAB_HAIR, null),
    BODY(RuntimeSetting.CREATOR_TAB_BODY, null),
    CLASS(RuntimeSetting.CREATOR_TAB_CLASS, RuntimeSetting.CREATOR_PLACEHOLDER_CLASS),
    PROFESSION(RuntimeSetting.CREATOR_TAB_PROFESSION, RuntimeSetting.CREATOR_PLACEHOLDER_PROFESSION),
    SKILLS(RuntimeSetting.CREATOR_TAB_SKILLS, RuntimeSetting.CREATOR_PLACEHOLDER_SKILLS);

    // Values
    public static final CreatorTab[] VALUES = values();

    // Internal
    private final String title;
    private final String placeholderText;

    // Constructor \\

    CreatorTab(String title, String placeholderText) {
        this.title = title;
        this.placeholderText = placeholderText;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public String getPlaceholderText() {
        return placeholderText;
    }

    public boolean isPlaceholder() {
        return placeholderText != null;
    }
}
