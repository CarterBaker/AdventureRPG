package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class LocalWindBranch extends BranchPackage {

    /*
     * Recomputes local wind every frame. Direction is the fixed global
     * prevailing airflow rotated by the active season's own prevailing
     * offset plus a continuous gust wobble. Speed is the season's own base
     * speed varied by a two-layer gust oscillation, shaped by a diurnal
     * curve, then scaled by the active weather's windSpeedScale. Gust
     * amplitude and direction wobble both scale with the active weather's
     * windTurbulenceScale, so a storm reads as gustier and more erratic,
     * not just faster. In the other direction, RegionSampleBranch drags its
     * own noise sample point by this same WindHandle, so resolved weather
     * at a fixed location keeps changing as wind blows the pattern past it.
     */

    // Internal
    private ClockManager clockManager;
    private SeasonManager seasonManager;
    private WeatherManager weatherManager;

    // Wind
    private WindHandle windHandle;

    // Season Tracking
    private String lastSeasonName;
    private SeasonHandle activeSeason;

    // Time
    private float elapsedTime;

    // Internal \\

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
        this.weatherManager = get(WeatherManager.class);
    }

    // Assignment \\

    void assignData(WindHandle windHandle) {
        this.windHandle = windHandle;
    }

    // Local Wind \\

    void updateLocalWind() {

        elapsedTime += internal.getDeltaTime();

        resolveActiveSeason();

        float baseWindSpeed = EngineSetting.WIND_GLOBAL_SPEED;
        float windVariance = 0f;
        float seasonalDirectionOffsetDegrees = 0f;

        if (activeSeason != null) {
            baseWindSpeed = activeSeason.getBaseWindSpeed();
            windVariance = activeSeason.getWindVariance();
            seasonalDirectionOffsetDegrees = activeSeason.getPrevailingWindDirectionDegrees();
        }

        float weatherTurbulence = weatherManager.getWindTurbulenceScale();

        updateDirection(seasonalDirectionOffsetDegrees, weatherTurbulence);
        updateSpeed(baseWindSpeed, windVariance, weatherTurbulence);
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    private void updateDirection(float seasonalDirectionOffsetDegrees, float weatherTurbulence) {

        Vector3 globalDirection = windHandle.getGlobalWindDirection();
        float globalAngle = (float) Math.atan2(globalDirection.z, globalDirection.x);

        float seasonalOffsetRadians = (float) Math.toRadians(seasonalDirectionOffsetDegrees);
        float gustWobbleRadians = (float) Math.toRadians(
                Math.sin(elapsedTime * EngineSetting.WIND_GUST_DIRECTION_FREQUENCY)
                        * EngineSetting.WIND_GUST_DIRECTION_WOBBLE_DEGREES * weatherTurbulence);

        float localAngle = globalAngle + seasonalOffsetRadians + gustWobbleRadians;

        windHandle.setLocalWindDirection(
                (float) Math.cos(localAngle),
                0.0f,
                (float) Math.sin(localAngle));
    }

    private void updateSpeed(float baseWindSpeed, float windVariance, float weatherTurbulence) {

        float speedGust = (float) (Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY) * 0.6
                + Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY_SECONDARY + 1.7) * 0.4)
                * weatherTurbulence;

        float seasonalSpeed = baseWindSpeed + speedGust * windVariance;

        float diurnalFactor = 1f + computeDiurnalFactor() * EngineSetting.WIND_DIURNAL_STRENGTH;

        float speedBeforeWeather = Math.max(
                EngineSetting.WIND_MIN_SPEED_FLOOR,
                seasonalSpeed * diurnalFactor);

        float weatherSpeedScale = weatherManager.getWindSpeedScale();

        windHandle.setLocalWindSpeed(speedBeforeWeather * weatherSpeedScale);
    }

    /*
     * Bell-shaped curve over the daily cycle, peaking at
     * WIND_DIURNAL_PEAK_TIME. Returns roughly [-1, 1].
     */
    private float computeDiurnalFactor() {

        double visualTimeOfDay = clockManager.getClockHandle().getVisualTimeOfDay();
        double angle = (visualTimeOfDay - EngineSetting.WIND_DIURNAL_PEAK_TIME) * Math.PI * 2.0;

        return (float) Math.cos(angle);
    }
}