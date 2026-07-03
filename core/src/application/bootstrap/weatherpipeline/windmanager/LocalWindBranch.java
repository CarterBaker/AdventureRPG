package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class LocalWindBranch extends BranchPackage {

    /*
     * Recomputes local wind every frame by rotating the fixed global wind
     * direction by a smoothly-blended seasonal offset, then scaling global
     * wind speed by a seasonal multiplier and the active weather's
     * windSpeedScale. Season blending mirrors SkyColorBranch's triangleWave
     * factors so wind never pops at a season boundary.
     */

    // Settings
    private float springDirectionOffset;
    private float summerDirectionOffset;
    private float fallDirectionOffset;
    private float winterDirectionOffset;
    private float springSpeedMul;
    private float summerSpeedMul;
    private float fallSpeedMul;
    private float winterSpeedMul;

    // Internal
    private ClockManager clockManager;
    private WeatherManager weatherManager;

    // Wind
    private WindHandle windHandle;

    // Scratch
    private float[] seasonFactors;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.springDirectionOffset = EngineSetting.WIND_SPRING_DIRECTION_OFFSET;
        this.summerDirectionOffset = EngineSetting.WIND_SUMMER_DIRECTION_OFFSET;
        this.fallDirectionOffset = EngineSetting.WIND_FALL_DIRECTION_OFFSET;
        this.winterDirectionOffset = EngineSetting.WIND_WINTER_DIRECTION_OFFSET;
        this.springSpeedMul = EngineSetting.WIND_SPRING_SPEED_MUL;
        this.summerSpeedMul = EngineSetting.WIND_SUMMER_SPEED_MUL;
        this.fallSpeedMul = EngineSetting.WIND_FALL_SPEED_MUL;
        this.winterSpeedMul = EngineSetting.WIND_WINTER_SPEED_MUL;

        // Scratch
        this.seasonFactors = new float[4];
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.weatherManager = get(WeatherManager.class);
    }

    // Assignment \\

    void assignData(WindHandle windHandle) {
        this.windHandle = windHandle;
    }

    // Local Wind \\

    void updateLocalWind() {

        float yearProgress = (float) clockManager.getClockHandle().getVisualYearProgress();
        float[] season = computeSeasonFactors(yearProgress);

        float directionOffset = season[0] * springDirectionOffset +
                season[1] * summerDirectionOffset +
                season[2] * fallDirectionOffset +
                season[3] * winterDirectionOffset;

        float speedMultiplier = season[0] * springSpeedMul +
                season[1] * summerSpeedMul +
                season[2] * fallSpeedMul +
                season[3] * winterSpeedMul;

        Vector3 globalDirection = windHandle.getGlobalWindDirection();
        float globalAngle = (float) Math.atan2(globalDirection.z, globalDirection.x);
        float localAngle = globalAngle + (float) Math.toRadians(directionOffset);

        windHandle.setLocalWindDirection(
                (float) Math.cos(localAngle),
                0.0f,
                (float) Math.sin(localAngle));

        float weatherSpeedScale = weatherManager.getWindSpeedScale();

        windHandle.setLocalWindSpeed(
                windHandle.getGlobalWindSpeed() * speedMultiplier * weatherSpeedScale);
    }

    // Season Factors \\

    private float[] computeSeasonFactors(float y) {

        seasonFactors[0] = triangleWave(y, 0.00f, 0.25f, 0.50f);
        seasonFactors[1] = triangleWave(y, 0.25f, 0.50f, 0.75f);
        seasonFactors[2] = triangleWave(y, 0.50f, 0.75f, 1.00f);
        seasonFactors[3] = Math.max(0.0f, 1.0f - seasonFactors[0] - seasonFactors[1] - seasonFactors[2]);

        return seasonFactors;
    }

    private float triangleWave(float y, float start, float peak, float end) {
        if (y <= start || y >= end)
            return 0.0f;
        return y <= peak ? smoothstep(start, peak, y) : 1.0f - smoothstep(peak, end, y);
    }

    private float smoothstep(float edge0, float edge1, float t) {
        t = Math.max(0.0f, Math.min(1.0f, (t - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }
}