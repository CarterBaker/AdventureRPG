package com.internal.bootstrap.calendarpipeline.calendarmanager;

import com.internal.bootstrap.calendarpipeline.calendar.CalendarHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class CalendarManager extends ManagerPackage {

    /*
     * Owns the calendar palette for the engine lifetime. Supports on-demand
     * loading via InternalLoader on a cache miss. Keyed by full calendar name
     * e.g. "standard/Overworld".
     */

    // Palette
    private Object2ObjectOpenHashMap<String, CalendarHandle> calendarName2CalendarHandle;
    private Short2ObjectOpenHashMap<CalendarHandle> calendarID2CalendarHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.calendarName2CalendarHandle = new Object2ObjectOpenHashMap<>();
        this.calendarID2CalendarHandle = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addCalendarHandle(CalendarHandle calendarHandle) {

        short id = RegistryUtility.toShortID(calendarHandle.getCalendarName());

        calendarName2CalendarHandle.put(calendarHandle.getCalendarName(), calendarHandle);
        calendarID2CalendarHandle.put(id, calendarHandle);
    }

    // Accessible \\

    public boolean hasCalendar(String calendarName) {
        return calendarName2CalendarHandle.containsKey(calendarName);
    }

    public short getCalendarIDFromCalendarName(String calendarName) {

        if (!calendarName2CalendarHandle.containsKey(calendarName))
            ((InternalLoader) internalLoader).request(calendarName);

        return RegistryUtility.toShortID(calendarName);
    }

    public CalendarHandle getCalendarHandleFromCalendarID(short calendarID) {
        return calendarID2CalendarHandle.get(calendarID);
    }

    public CalendarHandle getCalendarHandleFromCalendarName(String calendarName) {

        CalendarHandle handle = calendarName2CalendarHandle.get(calendarName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(calendarName);
            handle = calendarName2CalendarHandle.get(calendarName);
        }

        if (handle == null)
            throwException("[CalendarManager] Calendar could not be loaded: \"" + calendarName + "\"");

        return handle;
    }
}