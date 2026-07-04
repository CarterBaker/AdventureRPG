package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;

class YearTrackerBranch extends BranchPackage {

    /*
     * Detects year and age rollovers from total days with offset. Updates the
     * current year and age on the ClockHandle when a year change is detected.
     * Returns true when an age boundary is crossed. Starting year/age and
     * years-per-age all come from the active calendar now, rather than fixed
     * engine-wide constants.
     */

    // Internal
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Tracking
    private int lastYear;

    // Internal \\

    @Override
    protected void create() {

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
        int startYear = calendarHandle.getStartYear();
        int startAge = calendarHandle.getStartAge();
        int yearsPerAge = calendarHandle.getYearsPerAge();

        long yearsPerAgeDays = (long) yearsPerAge * totalDaysInYear;
        long dayOfAge = totalDaysWithOffset % yearsPerAgeDays;
        int currentYear = (int) (dayOfAge / totalDaysInYear) + startYear;

        if (lastYear == currentYear)
            return false;

        lastYear = currentYear;

        int currentAge = (int) (totalDaysWithOffset / yearsPerAgeDays) + startAge;
        int yearsElapsed = currentYear - startYear;

        clockHandle.setCurrentYear(currentYear);
        clockHandle.setCurrentAge(currentAge);

        return yearsElapsed % yearsPerAge == 0;
    }
}