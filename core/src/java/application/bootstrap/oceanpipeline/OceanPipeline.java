package application.bootstrap.oceanpipeline;

import application.bootstrap.oceanpipeline.exposuremanager.ExposureManager;
import application.bootstrap.oceanpipeline.tidemanager.TideManager;
import application.bootstrap.oceanpipeline.turbulencemanager.TurbulenceManager;
import application.bootstrap.oceanpipeline.wavemanager.WaveManager;
import engine.root.PipelinePackage;

public class OceanPipeline extends PipelinePackage {

    /*
     * Registers the tide, turbulence, exposure and wave managers, in the order
     * each one reads the last. The pipeline is created after the calendar and
     * weather pipelines so every manager updates after the clock has advanced
     * and after every weather pattern and grid wind has resolved for the
     * frame, and always reads current values.
     */

    @Override
    protected void create() {
        create(TideManager.class);
        create(TurbulenceManager.class);
        create(ExposureManager.class);
        create(WaveManager.class);
    }
}
