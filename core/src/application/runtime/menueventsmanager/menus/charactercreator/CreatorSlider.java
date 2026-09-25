package application.runtime.menueventsmanager.menus.charactercreator;

import application.runtime.RuntimeSetting;

public enum CreatorSlider {

    /*
     * The body and head proportions the creator exposes as sliders. Each
     * slider's name rides as its drag argument, so the one drag handler
     * knows which value the track it received controls.
     */

    HEIGHT(RuntimeSetting.CREATOR_SLIDER_HEIGHT),
    WEIGHT(RuntimeSetting.CREATOR_SLIDER_BUILD),
    HEAD_WIDTH(RuntimeSetting.CREATOR_SLIDER_HEAD_WIDTH),
    HEAD_LENGTH(RuntimeSetting.CREATOR_SLIDER_HEAD_HEIGHT),
    HEAD_DEPTH(RuntimeSetting.CREATOR_SLIDER_HEAD_DEPTH);

    // Internal
    private final String label;

    // Constructor \\

    CreatorSlider(String label) {
        this.label = label;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public boolean isHeadProportion() {
        return this == HEAD_WIDTH || this == HEAD_LENGTH || this == HEAD_DEPTH;
    }
}
