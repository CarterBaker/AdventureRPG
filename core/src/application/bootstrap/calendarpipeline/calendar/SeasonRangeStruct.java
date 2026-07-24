package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonRangeStruct extends StructPackage {

    /*
     * One named season within a calendar's year: the exact month + day-of-
     * month it begins on, plus its dayLength — the fraction (0-1) of a full
     * day/night cycle that is daylight at this season's center date. A season
     * runs from its own start date up to (but not including) the next
     * season's start date — the final season in the list wraps around,
     * continuing into the days before the first season's start date.
     * dayLength is consumed by SeasonBlendBranch to blend the actual daylight
     * fraction smoothly between one season's center and the next.
     */

    // Internal
    private final String name;
    private final int startMonth;
    private final int startDayOfMonth;
    private final float dayLength;

    // Constructor \\

    public SeasonRangeStruct(String name, int startMonth, int startDayOfMonth, float dayLength) {

        // Internal
        this.name = name;
        this.startMonth = startMonth;
        this.startDayOfMonth = startDayOfMonth;
        this.dayLength = dayLength;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int getStartMonth() {
        return startMonth;
    }

    public int getStartDayOfMonth() {
        return startDayOfMonth;
    }

    public float getDayLength() {
        return dayLength;
    }
}