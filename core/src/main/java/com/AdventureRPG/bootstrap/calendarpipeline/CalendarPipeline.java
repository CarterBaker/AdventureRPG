package com.AdventureRPG.bootstrap.calendarpipeline;

import com.AdventureRPG.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import com.AdventureRPG.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.AdventureRPG.core.engine.PipelinePackage;

public class CalendarPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Calendar Pipeline
        create(CalendarManager.class);
        create(ClockManager.class);
    }
}
