package com.internal.bootstrap.calendarpipeline.calendarmanager;

import java.io.File;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;

class InternalLoadManager extends ManagerPackage {

    // Internal
    private File calendarFile;
    private CalendarManager calendarManager;
    private InternalBuildSystem internalBuildSystem;

    // Base \\

    @Override
    protected void create() {
        this.calendarFile = new File(EngineSetting.CALENDAR_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.calendarManager = get(CalendarManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Calendar Loading \\

    void loadCalendarData() {
        validateCalendarFile();

        try {
            CalendarHandle calendarHandle = internalBuildSystem.buildCalendarHandle(calendarFile);

            if (calendarHandle != null)
                calendarManager.addCalendarHandle(calendarHandle);
            else
                throwException("Failed to build calendar handle from file");
        } catch (RuntimeException ex) {
            throwException(
                    "Failed to load calendar from file: " + calendarFile.getAbsolutePath(), ex);
        }
    }

    // Validation \\

    private void validateCalendarFile() {
        if (!calendarFile.exists() || !calendarFile.isFile())
            throwException("Calendar JSON file not found: " + calendarFile.getAbsolutePath());
    }
}