package program.bootstrap.calendarpipeline;

import program.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import program.bootstrap.calendarpipeline.clockmanager.ClockManager;
import program.core.engine.PipelinePackage;

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