package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.core.engine.BranchPackage;

class MonthTrackerBranch extends BranchPackage {

    /*
     * Detects month rollovers by comparing the current month set by
     * DayTrackerBranch against the last seen value. Returns true when the
     * month resets to 0, signalling a year rollover to YearTrackerBranch.
     */

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

    boolean advanceTime() {

        int currentMonth = clockHandle.getCurrentMonth();

        if (lastMonth == currentMonth)
            return false;

        lastMonth = currentMonth;

        return currentMonth == 0;
    }
}