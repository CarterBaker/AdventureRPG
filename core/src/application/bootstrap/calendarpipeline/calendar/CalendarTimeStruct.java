package application.bootstrap.calendarpipeline.calendar;

import engine.root.StructPackage;

public class CalendarTimeStruct extends StructPackage {

    /*
     * The shape of a day and a year for this calendar — hours per day,
     * minutes per hour, days in a lunar cycle, the visual midday offset used
     * when bending raw time-of-day into a sunrise/noon/sunset curve, and how
     * many years make up one age. All of this used to be fixed, engine-wide
     * constants; now each calendar defines its own.
     */

    // Internal
    private final int daysPerDay;
    private final int hoursPerDay;
    private final int minutesPerHour;
    private final int lunarCycleDays;
    private final float middayOffset;
    private final int yearsPerAge;

    // Constructor \\

    public CalendarTimeStruct(
            int daysPerDay,
            int hoursPerDay,
            int minutesPerHour,
            int lunarCycleDays,
            float middayOffset,
            int yearsPerAge) {

        // Internal
        this.daysPerDay = daysPerDay;
        this.hoursPerDay = hoursPerDay;
        this.minutesPerHour = minutesPerHour;
        this.lunarCycleDays = lunarCycleDays;
        this.middayOffset = middayOffset;
        this.yearsPerAge = yearsPerAge;
    }

    // Accessible \\

    public int getDaysPerDay() {
        return daysPerDay;
    }

    public int getHoursPerDay() {
        return hoursPerDay;
    }

    public int getMinutesPerHour() {
        return minutesPerHour;
    }

    public int getLunarCycleDays() {
        return lunarCycleDays;
    }

    public float getMiddayOffset() {
        return middayOffset;
    }

    public int getYearsPerAge() {
        return yearsPerAge;
    }
}