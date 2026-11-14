package application.runtime.sound;

import application.runtime.RuntimeSetting;

public enum AmbienceLayer {

    /*
     * The loops a window hears around its player, each held by
     * AmbienceSoundSystem at its own intensity every frame: the open wind and
     * its gale, trees stirring calm, windy or in a storm, rain light and
     * heavy, the open sea and a shore, birds by day and crickets by night,
     * and the hush under water.
     */

    WIND(RuntimeSetting.SOUND_CUE_WIND),
    WIND_STRONG(RuntimeSetting.SOUND_CUE_WIND_STRONG),
    TREES_CALM(RuntimeSetting.SOUND_CUE_TREES_CALM),
    TREES_WINDY(RuntimeSetting.SOUND_CUE_TREES_WINDY),
    TREES_STORMY(RuntimeSetting.SOUND_CUE_TREES_STORMY),
    RAIN(RuntimeSetting.SOUND_CUE_RAIN),
    RAIN_HEAVY(RuntimeSetting.SOUND_CUE_RAIN_HEAVY),
    OCEAN(RuntimeSetting.SOUND_CUE_OCEAN),
    SHORE(RuntimeSetting.SOUND_CUE_SHORE),
    BIRDS(RuntimeSetting.SOUND_CUE_BIRDS),
    CRICKETS(RuntimeSetting.SOUND_CUE_CRICKETS),
    UNDERWATER(RuntimeSetting.SOUND_CUE_UNDERWATER);

    // Values
    public static final AmbienceLayer[] VALUES = values();

    // Internal
    private final String cueName;

    // Constructor \\

    AmbienceLayer(String cueName) {
        this.cueName = cueName;
    }

    // Accessible \\

    public String getCueName() {
        return cueName;
    }
}
