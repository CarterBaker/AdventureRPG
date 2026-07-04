package application.bootstrap.calendarpipeline.clockmanager;

import application.bootstrap.calendarpipeline.clock.ClockHandle;
import engine.root.BranchPackage;

class MonthTrackerBranch extends BranchPackage {

    /*
     * Detects month rollovers by comparing the current month set by
     * DayTrackerBranch against the last seen value. Returns true whenever
     * the month actually changed — not only when it wraps to 0 (the first
     * month). YearTrackerBranch does its own direct "did the year actually
     * change" comparison downstream, so it's cheap and correct to let it
     * check on every month change, rather than gating it behind
     * currentMonth == 0 — that would miss any year (or multi-year) rollover
     * that lands on a non-first month after a long absence.
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

        return true;
    }
}