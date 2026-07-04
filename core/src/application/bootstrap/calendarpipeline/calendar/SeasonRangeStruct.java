package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class SeasonRangeStruct extends StructPackage {

    /*
     * One named season within a calendar's year, paired with the exact
     * month + day-of-month it begins on. A season runs from its own start
     * date up to (but not including) the next season's start date — the
     * final season in the list wraps around, continuing into the days
     * before the first season's start date.
     */

    // Internal
    private final String name;
    private final int startMonth;
    private final int startDayOfMonth;

    // Constructor \\

    public SeasonRangeStruct(String name, int startMonth, int startDayOfMonth) {

        // Internal
        this.name = name;
        this.startMonth = startMonth;
        this.startDayOfMonth = startDayOfMonth;
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
}