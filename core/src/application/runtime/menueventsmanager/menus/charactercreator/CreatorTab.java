package application.runtime.menueventsmanager.menus.charactercreator;

import application.runtime.RuntimeSetting;

public enum CreatorTab {

    /*
     * The sections of the character creator, in the order their tabs are
     * listed. Each tab is one page of the scrolling option list: the face and
     * the hair, the body and gender, the attribute points creation grants, and
     * the profession the character starts out in.
     */

    APPEARANCE(RuntimeSetting.CREATOR_TAB_APPEARANCE),
    HAIR(RuntimeSetting.CREATOR_TAB_HAIR),
    BODY(RuntimeSetting.CREATOR_TAB_BODY),
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

    public String getTitle() {
        return title;
    }
}
