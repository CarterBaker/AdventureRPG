package application.bootstrap.calendarpipeline;

import application.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.core.engine.PipelinePackage;

public class CalendarPipeline extends PipelinePackage {

    /*
     * Registers the calendar and clock managers. CalendarManager is registered
     * first since ClockManager depends on it to resolve the active world's
     * calendar definition during awake.
     */

    @Override
    protected void create() {
        create(CalendarManager.class);
        create(ClockManager.class);
    }
}