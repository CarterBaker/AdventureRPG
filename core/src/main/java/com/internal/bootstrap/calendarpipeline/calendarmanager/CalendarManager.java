package com.internal.bootstrap.calendarpipeline.calendarmanager;

import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class CalendarManager extends ManagerPackage {

    // Palette — keyed by full calendar name e.g. "standard/Overworld"
    private Object2ObjectOpenHashMap<String, CalendarHandle> palette;

    // Base \\

    @Override
    protected void create() {
        this.palette = new Object2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Calendar Management \\

    void addCalendarHandle(CalendarHandle calendarHandle) {
        palette.put(calendarHandle.getCalendarName(), calendarHandle);
    }

    // Accessible \\

    /**
     * Returns the CalendarHandle for the given name.
     * On-demand loads if not yet in palette — safe to call from any awake().
     */
    public CalendarHandle getCalendar(String calendarName) {

        CalendarHandle handle = palette.get(calendarName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(calendarName);
            handle = palette.get(calendarName);
        }

        if (handle == null)
            throwException("[CalendarManager] Calendar could not be loaded: \"" + calendarName + "\"");

        return handle;
    }
}