package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.bootstrap.calendarpipeline.calendarmanager.CalendarHandle;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;

public class MonthTrackerSystem extends SystemPackage {

    // Internal
    private int STARTING_MONTH;

    private CalendarHandle calendarHandle;
    private ClockHandle clockHandle;

    // Tracking
    private int lastMonth;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.STARTING_MONTH = EngineSetting.STARTING_MONTH;

        // Tracking
        this.lastMonth = -1;
    }

    // Month Tracker \\

    void assignTimeData(CalendarHandle calendarHandle, ClockHandle clockHandle) {

        // Internal
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
    }

    boolean advanceTime() {

        int currentMonth = clockHandle.getCurrentMonth();

        // Check if month has changed
        if (lastMonth == currentMonth)
            return false;

        lastMonth = currentMonth;

        // Month changed - check if year rolled over
        // Year rolls over when we transition from last month (e.g., 12) to first month
        // (1)
        return (currentMonth == 1);
    }
}