package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.bootstrap.calendarpipeline.clock.ClockHandle;
import com.internal.core.engine.BranchPackage;

class MonthTrackerBranch extends BranchPackage {

    // Internal
    private ClockHandle clockHandle;

    // Tracking
    private int lastMonth;

    // Internal \\

    @Override
    protected void create() {

        // Tracking
        this.lastMonth = -1;
    }

    // Assignment \\

    void assignData(ClockHandle clockHandle) {
        this.clockHandle = clockHandle;
    }

    // Month Tracker \\

    /*
     * Month is already set by DayTrackerBranch before this is called.
     * Returns true when the month resets to 0 — signals a year rollover
     * to YearTrackerBranch.
     */
    boolean advanceTime() {

        int currentMonth = clockHandle.getCurrentMonth();

        if (lastMonth == currentMonth)
            return false;

        lastMonth = currentMonth;

        return (currentMonth == 0);
    }
}