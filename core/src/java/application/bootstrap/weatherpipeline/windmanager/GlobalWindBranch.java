package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.weatherpipeline.wind.WindHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class GlobalWindBranch extends BranchPackage {

    /*
     * Resolves the planet's fixed prevailing airflow once, at assignment —
     * this represents the world's rotation-driven global wind and never
     * changes at runtime. LocalWindBranch reads this value every frame and
     * blends it with season and weather; this branch has no per-frame work.
     */

    // Internal
    private WindHandle windHandle;

    // Assignment \\

    void assignData(WindHandle windHandle) {

        this.windHandle = windHandle;

        resolveGlobalWind();
    }

    // Global Wind \\

    private void resolveGlobalWind() {

        float radians = (float) Math.toRadians(EngineSetting.WIND_GLOBAL_DIRECTION_DEGREES);

        windHandle.setGlobalWindDirection(
                (float) Math.cos(radians),
                0.0f,
                (float) Math.sin(radians));

        windHandle.setGlobalWindSpeed(EngineSetting.WIND_GLOBAL_SPEED);
    }
}