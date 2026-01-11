package com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager;

import com.AdventureRPG.core.engine.ManagerPackage;

public class CalendarManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private CalendarHandle calendarHandle;

    // Base \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
    }

    @Override
    protected void awake() {
        loadCalendarData();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
    }

    // Calendar Management \\

    private void loadCalendarData() {
        internalLoadManager.loadCalendarData();
    }

    void addCalendarHandle(CalendarHandle calendarHandle) {
        this.calendarHandle = calendarHandle;
    }

    // Accessible \\

    public CalendarHandle getCalendarHandle() {
        return calendarHandle;
    }
}