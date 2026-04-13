package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;
import engine.settings.EngineSetting;

class YearTrackerBranch extends BranchPackage {

    /*
     * Detects year and age rollovers from total days with offset. Updates the
     * current year and age on the ClockHandle when a year change is detected.
     * Returns true when an age boundary is crossed.
     */

    // Internal
    private int STARTING_YEAR;
    private int STARTING_AGE;
    private int YEARS_PER_AGE;
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Tracking
    private int lastYear;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.STARTING_YEAR = EngineSetting.STARTING_YEAR;
        this.STARTING_AGE = EngineSetting.STARTING_AGE;
        this.YEARS_PER_AGE = EngineSetting.YEARS_PER_AGE;

        // Tracking
        this.lastYear = -1;
    }

    // Assignment \\

    void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle) {
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
    }

    // Year Tracker \\

    boolean advanceTime() {

        long totalDaysWithOffset = clockHandle.getTotalDaysWithOffset();
        int totalDaysInYear = calendarHandle.getTotalDaysInYear();
        long yearsPerAgeDays = (long) YEARS_PER_AGE * totalDaysInYear;
        long dayOfAge = totalDaysWithOffset % yearsPerAgeDays;
        int currentYear = (int) (dayOfAge / totalDaysInYear) + STARTING_YEAR;

        if (lastYear == currentYear)
            return false;

        lastYear = currentYear;

        int currentAge = (int) (totalDaysWithOffset / yearsPerAgeDays) + STARTING_AGE;
        int yearsElapsed = currentYear - STARTING_YEAR;

        clockHandle.setCurrentYear(currentYear);
        clockHandle.setCurrentAge(currentAge);

        return yearsElapsed % YEARS_PER_AGE == 0;
    }
}