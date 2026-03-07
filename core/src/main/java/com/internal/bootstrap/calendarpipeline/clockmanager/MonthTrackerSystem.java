package com.internal.bootstrap.calendarpipeline.clockmanager;

import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
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
        this.STARTING_MONTH = EngineSetting.STARTING_MONTH;
        this.lastMonth = -1;
    }

    // Month Tracker \\

    void assignTimeData(CalendarHandle calendarHandle, ClockHandle clockHandle) {
        this.calendarHandle = calendarHandle;
        this.clockHandle = clockHandle;
    }

    boolean advanceTime() {

        int currentMonth = clockHandle.getCurrentMonth();

        if (lastMonth == currentMonth)
            return false;

        lastMonth = currentMonth;

        // Months are 0-indexed — year rolls when we return to month 0
        return (currentMonth == 0);
    }
}