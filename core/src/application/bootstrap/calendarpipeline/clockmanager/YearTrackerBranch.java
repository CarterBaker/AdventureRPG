package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;

class YearTrackerBranch extends BranchPackage {

    /*
     * Detects year and age rollovers from total days with offset.
     * totalDaysWithOffset already measures elapsed days from year 0 of the
     * calendar's starting age, so dividing by totalDaysInYear yields the
     * absolute current year directly — no additional startYear term is
     * added on top. Returns true when an age boundary is crossed (the year
     * has wrapped back to 0 within the new age).
     */

    // Internal
    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Tracking
    private int lastYear;

    // Internal \\

    @Override
    protected void create() {
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
        int startAge = calendarHandle.getStartAge();
        int yearsPerAge = calendarHandle.getYearsPerAge();

        long yearsPerAgeDays = (long) yearsPerAge * totalDaysInYear;
        long dayOfAge = totalDaysWithOffset % yearsPerAgeDays;
        int currentYear = (int) (dayOfAge / totalDaysInYear);

        if (lastYear == currentYear)
            return false;

        lastYear = currentYear;

        int currentAge = (int) (totalDaysWithOffset / yearsPerAgeDays) + startAge;

        clockHandle.setCurrentYear(currentYear);
        clockHandle.setCurrentAge(currentAge);

        return currentYear == 0;
    }
}