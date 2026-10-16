package application.runtime.menueventsmanager.menus.character;

import application.runtime.RuntimeSetting;

public enum CharacterTab {

    /*
     * The pages of the character sheet, in the order their tabs are listed:
     * attributes to spend points on, the trades and the skills a character has
     * grown in, and the professions it holds or could earn.
     */

    ATTRIBUTES(RuntimeSetting.CHARACTER_TAB_ATTRIBUTES),
    TRADES(RuntimeSetting.CHARACTER_TAB_TRADES),
    SKILLS(RuntimeSetting.CHARACTER_TAB_SKILLS),
    PROFESSION(RuntimeSetting.CHARACTER_TAB_PROFESSION);

    // Values
    public static final CharacterTab[] VALUES = values();

    // Internal
    private final String title;

    // Constructor \\

    CharacterTab(String title) {
        this.title = title;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }
}
