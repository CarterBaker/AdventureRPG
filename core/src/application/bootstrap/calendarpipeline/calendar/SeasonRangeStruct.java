package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonRangeStruct extends StructPackage {

    /*
     * Anchors one season in the calendar year: its start month and day, and the
     * daylight fraction at its center that bends the time-of-day curve. A
     * season runs until the next one starts, wrapping at year end; its climate
     * lives on its SeasonHandle.
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