package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class SeasonRangeStruct extends StructPackage {

    /*
     * One named season within a calendar: the month/day it starts on, its
     * dayLength (fraction of a day/night cycle that is daylight at this
     * season's center date), and the sky tint/sunrise colors that season
     * blends toward at that same center date. A season runs from its own
     * start date up to the next season's start date, wrapping for the
     * final season in the list.
     */

    private final String name;
    private final int startMonth;
    private final int startDayOfMonth;
    private final float dayLength;
    private final Vector3 tintColor;
    private final Vector3 sunriseColor;

    public SeasonRangeStruct(
            String name,
            int startMonth,
            int startDayOfMonth,
            float dayLength,
            Vector3 tintColor,
            Vector3 sunriseColor) {

        this.name = name;
        this.startMonth = startMonth;
        this.startDayOfMonth = startDayOfMonth;
        this.dayLength = dayLength;
        this.tintColor = tintColor;
        this.sunriseColor = sunriseColor;
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

    public Vector3 getTintColor() {
        return tintColor;
    }

    public Vector3 getSunriseColor() {
        return sunriseColor;
    }
}