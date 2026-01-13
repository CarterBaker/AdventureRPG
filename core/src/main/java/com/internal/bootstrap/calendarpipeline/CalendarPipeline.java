package com.internal.bootstrap.calendarpipeline;

import com.internal.bootstrap.calendarpipeline.calendarmanager.CalendarManager;
import com.internal.bootstrap.calendarpipeline.clockmanager.ClockManager;
import com.internal.core.engine.PipelinePackage;

public class CalendarPipeline extends PipelinePackage {

    @Override
    protected void create() {

        // Calendar Pipeline
        create(CalendarManager.class);
        create(ClockManager.class);
    }
}
