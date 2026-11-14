package application.runtime.menueventsmanager.menus.charactercreator;

import application.runtime.RuntimeSetting;

public enum CreatorSlider {

    /*
     * The values the creator exposes as sliders: the body's height and
     * weight, the head's proportions, and, indexed, every shape slider and
     * detail amount of the character's appearance, and the hue, saturation
     * and brightness of each of its colors. A slider's name and index
     * ride as its drag argument, so the one drag handler knows which value
     * the track it received controls. Indexed sliders take their label from
     * the appearance.
     */

    HEIGHT(RuntimeSetting.CREATOR_SLIDER_HEIGHT),
    WEIGHT(RuntimeSetting.CREATOR_SLIDER_BUILD),
    HEAD_WIDTH(RuntimeSetting.CREATOR_SLIDER_HEAD_WIDTH),
    HEAD_LENGTH(RuntimeSetting.CREATOR_SLIDER_HEAD_HEIGHT),
    HEAD_DEPTH(RuntimeSetting.CREATOR_SLIDER_HEAD_DEPTH),
    SHAPE(null),
    DETAIL(null),
    COLOR_HUE(RuntimeSetting.CREATOR_SLIDER_HUE),
    COLOR_SATURATION(RuntimeSetting.CREATOR_SLIDER_SATURATION),
    COLOR_BRIGHTNESS(RuntimeSetting.CREATOR_SLIDER_BRIGHTNESS);

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
}
