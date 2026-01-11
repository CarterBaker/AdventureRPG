package com.AdventureRPG.bootstrap.calendarpipeline.clockmanager;

import com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager.CalendarHandle;
import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;

public class YearTrackerSystem extends SystemPackage {

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

    // Year Tracker \\

    void assignTimeData(CalendarHandle calendarHandle, ClockHandle clockHandle) {

        // Internal
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
    }

    boolean advanceTime() {

        // Calculate total days with offset
        long totalDaysWithOffset = clockHandle.getTotalDaysWithOffset();

        // Calculate year
        int totalDaysInYear = calendarHandle.getTotalDaysInYear();
        long dayOfAge = totalDaysWithOffset % (YEARS_PER_AGE * totalDaysInYear);
        int currentYear = (int) (dayOfAge / totalDaysInYear) + STARTING_YEAR;

        // Check if year has changed
        if (lastYear == currentYear)
            return false;

        lastYear = currentYear;

        // Calculate age
        int currentAge = (int) (totalDaysWithOffset / (YEARS_PER_AGE * totalDaysInYear)) + STARTING_AGE;

        // Update ClockHandle
        clockHandle.setCurrentYear(currentYear);
        clockHandle.setCurrentAge(currentAge);

        // Age changed if currentYear - STARTING_YEAR is a multiple of YEARS_PER_AGE
        int yearsElapsed = currentYear - STARTING_YEAR;
        return (yearsElapsed % YEARS_PER_AGE == 0);
    }
}