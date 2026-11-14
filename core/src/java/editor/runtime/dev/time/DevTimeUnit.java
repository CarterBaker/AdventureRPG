package editor.runtime.dev.time;

import editor.runtime.EditorSetting;

public enum DevTimeUnit {

    /*
     * Every part of the clock the time command can advance, in the order the
     * command console lays out its dials. Each knows the word typed for it,
     * the title its dial shows, the group its dial sits in, and whether it
     * runs in a circle — a cyclic unit's needle goes once around its dial,
     * while the year's and the age's needles turn a fixed number of steps per
     * turn.
     */

    HOUR(
            EditorSetting.DEV_TIME_UNIT_HOUR,
            EditorSetting.DEV_TIME_TITLE_HOUR,
            EditorSetting.DEV_TIME_GROUP_TIME_OF_DAY,
            true),
    MINUTE(
            EditorSetting.DEV_TIME_UNIT_MINUTE,
            EditorSetting.DEV_TIME_TITLE_MINUTE,
            EditorSetting.DEV_TIME_GROUP_TIME_OF_DAY,
            true),
    WEEKDAY(
            EditorSetting.DEV_TIME_UNIT_WEEKDAY,
            EditorSetting.DEV_TIME_TITLE_WEEKDAY,
            EditorSetting.DEV_TIME_GROUP_CALENDAR,
            true),
    DAY(
            EditorSetting.DEV_TIME_UNIT_DAY,
            EditorSetting.DEV_TIME_TITLE_DAY,
            EditorSetting.DEV_TIME_GROUP_CALENDAR,
            true),
    MONTH(
            EditorSetting.DEV_TIME_UNIT_MONTH,
            EditorSetting.DEV_TIME_TITLE_MONTH,
            EditorSetting.DEV_TIME_GROUP_CALENDAR,
            true),
    SEASON(
            EditorSetting.DEV_TIME_UNIT_SEASON,
            EditorSetting.DEV_TIME_TITLE_SEASON,
            EditorSetting.DEV_TIME_GROUP_CALENDAR,
            true),
    YEAR(
            EditorSetting.DEV_TIME_UNIT_YEAR,
            EditorSetting.DEV_TIME_TITLE_YEAR,
            EditorSetting.DEV_TIME_GROUP_YEARS,
            false),
    AGE(
            EditorSetting.DEV_TIME_UNIT_AGE,
            EditorSetting.DEV_TIME_TITLE_AGE,
            EditorSetting.DEV_TIME_GROUP_YEARS,
            false),
    MOON(
            EditorSetting.DEV_TIME_UNIT_MOON,
            EditorSetting.DEV_TIME_TITLE_MOON,
            EditorSetting.DEV_TIME_GROUP_MOON,
            true);

    // Values
    public static final DevTimeUnit[] VALUES = values();

    // Internal
    private final String unitName;
    private final String title;
    private final String groupTitle;
    private final boolean cyclic;

    // Constructor \\

    DevTimeUnit(String unitName, String title, String groupTitle, boolean cyclic) {
        this.unitName = unitName;
        this.title = title;
        this.groupTitle = groupTitle;
        this.cyclic = cyclic;
    }

    // Lookup \\

    // The unit typed by this name, in any case — null when none is
    public static DevTimeUnit fromUnitName(String unitName) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].unitName.equalsIgnoreCase(unitName))
                return VALUES[i];

        return null;
    }

    // Accessible \\

    public String getUnitName() {
        return unitName;
    }

    public String getTitle() {
        return title;
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public boolean isCyclic() {
        return cyclic;
    }
}
