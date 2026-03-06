package com.internal.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;

import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;

class InternalLoader extends LoaderPackage {

    // Internal
    private CalendarManager calendarManager;
    private InternalBuilder internalBuilder;

    // Base \\

    @Override
    protected void scan() {
        File calendarFile = new File(EngineSetting.CALENDAR_JSON_PATH);
        if (!calendarFile.exists() || !calendarFile.isFile())
            throwException("Calendar JSON file not found: " + calendarFile.getAbsolutePath());
        fileQueue.offer(calendarFile);
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.calendarManager = get(CalendarManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        CalendarHandle calendarHandle = internalBuilder.build(file);
        if (calendarHandle == null)
            throwException("Failed to build calendar handle from file: " + file.getAbsolutePath());
        calendarManager.addCalendarHandle(calendarHandle);
    }

    // On-Demand \\

    /*
     * Forces the calendar file to load immediately regardless of batch cadence.
     * Called by CalendarManager.getCalendarHandle() on first access.
     */
    void loadNow() {
        File calendarFile = new File(EngineSetting.CALENDAR_JSON_PATH);
        request(calendarFile);
    }
}