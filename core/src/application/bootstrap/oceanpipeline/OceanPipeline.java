package application.bootstrap.oceanpipeline;

import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulencemanager.TurbulenceManager;
import engine.root.PipelinePackage;

public class OceanPipeline extends PipelinePackage {

    /*
     * Registers the tide and turbulence managers. The pipeline is created
     * after the calendar and weather pipelines so both managers update after
     * the clock has advanced and after every weather pattern and grid wind
     * has resolved for the frame, and always read current values.
     */

    @Override
    protected void create() {
        create(TideManager.class);
        create(TurbulenceManager.class);
    }
}
