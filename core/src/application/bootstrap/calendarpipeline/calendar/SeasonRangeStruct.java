package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonRangeStruct extends StructPackage {

    /*
     * One named season's position within the calendar year: the month
     * and day it starts on, and dayLength — the fraction of a day/night
     * cycle that is daylight at this season's center date, used to bend
     * the calendar's own visual time-of-day curve. The season's climate
     * and sky-color values live on its own SeasonHandle in the weather
     * pipeline instead; this struct only anchors a season to a point in
     * the calendar year. A season runs from its own start date up to the
     * next season's start date, wrapping for the final season in the
     * list.
     */

    private final String name;
    private final int startMonth;
    private final int startDayOfMonth;
    private final float dayLength;

    public SeasonRangeStruct(String name, int startMonth, int startDayOfMonth, float dayLength) {
        this.name = name;
        this.startMonth = startMonth;
        this.startDayOfMonth = startDayOfMonth;
        this.dayLength = dayLength;
    }

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