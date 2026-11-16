package application.runtime.menueventsmanager.menus.charactercreator;

import application.runtime.RuntimeSetting;

public enum CreatorTab {

    /*
     * The kinds of page the character creator lists, in the order their tabs
     * are listed. The look is the race's own: one tab per tab of its creator
     * layout, each titled and filled as the race lays it out. Then come the
     * attribute points creation grants and the profession the character
     * starts out in, the same for every race.
     */

    LOOK(null),
    ATTRIBUTES(RuntimeSetting.CREATOR_TAB_ATTRIBUTES),
    PROFESSION(RuntimeSetting.CREATOR_TAB_PROFESSION);

    // Values
    public static final CreatorTab[] VALUES = values();

    // Internal
    private final String title;

    // Constructor \\

    CreatorTab(String title) {
        this.title = title;
    }

    // Accessible \\

    // Null for the look, whose tabs the race titles
    public String getTitle() {
        return title;
    }
}
