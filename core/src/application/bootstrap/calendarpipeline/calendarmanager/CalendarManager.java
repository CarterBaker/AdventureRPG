package application.bootstrap.calendarpipeline.calendarmanager;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CalendarManager extends ManagerPackage {

    /*
     * Owns the calendar palette for the engine lifetime. Supports on-demand
     * loading via CalendarLoader on a cache miss. Keyed by full calendar name
     * e.g. "standard/Overworld", with IDs assigned in registration order.
     */

    // Palette
    private Object2IntOpenHashMap<String> calendarName2CalendarID;
    private ObjectArrayList<CalendarHandle> calendarID2CalendarHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.calendarName2CalendarID = RegistryUtility.createNameIndex();
        this.calendarID2CalendarHandle = RegistryUtility.createPalette();
        create(CalendarLoader.class);
    }

    // Management \\

    void addCalendarHandle(CalendarHandle calendarHandle) {
        RegistryUtility.registerHandle(
                calendarName2CalendarID, calendarID2CalendarHandle,
                calendarHandle.getCalendarName(), calendarHandle,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasCalendar(String calendarName) {
        return RegistryUtility.getHandle(calendarName2CalendarID, calendarID2CalendarHandle, calendarName) != null;
    }

    public short getCalendarIDFromCalendarName(String calendarName) {

        if (!hasCalendar(calendarName))
            ((CalendarLoader) internalLoader).request(calendarName);

        if (!hasCalendar(calendarName))
            throwException("[CalendarManager] Calendar could not be loaded: \"" + calendarName + "\"");

        return (short) calendarName2CalendarID.getInt(calendarName);
    }

    public CalendarHandle getCalendarHandleFromCalendarID(short calendarID) {
        return RegistryUtility.getHandle(calendarID2CalendarHandle, calendarID);
    }

    public CalendarHandle getCalendarHandleFromCalendarName(String calendarName) {
        return getCalendarHandleFromCalendarID(getCalendarIDFromCalendarName(calendarName));
    }
}
