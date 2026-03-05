package com.internal.bootstrap.calendarpipeline.calendarmanager;

import com.internal.core.engine.ManagerPackage;

public class CalendarManager extends ManagerPackage {

    // Data
    private CalendarHandle calendarHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
    }

    // Calendar Management \\

    void addCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
    }

    // Accessible \\

    /*
     * Lazily resolves on first access — safe to call from awake() before
     * the batch loader has processed the calendar file.
     */
    public CalendarHandle getCalendarHandle() {
        if (calendarHandle == null)
            ((InternalLoadManager) internalLoader).loadNow();
        if (calendarHandle == null)
            throwException("[CalendarManager] Calendar could not be loaded.");
        return calendarHandle;
    }
}