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
     * direction offset (see SeasonData.getPrevailingWindDirectionDegrees()),
     * plus a small continuous gust wobble. Speed is the active season's own
     * base wind speed (see SeasonData.getBaseWindSpeed()) varied by a slow
     * two-layer gust oscillation scaled by the season's windVariance,
     * shaped by a diurnal (time-of-day) curve, floored above zero, then
     * scaled by the active weather's windSpeedScale.
     *
     * Season identity comes from ClockHandle.getCurrentSeason() — the same
     * calendar-driven named season WeatherManager already resolves its
     * weather pool against — so wind and weather never disagree about which
     * season is currently active. Season changes snap immediately, matching
     * WeatherManager's own pool-resolution behavior; continuous life within
     * a season comes entirely from the gust and diurnal terms below, driven
     * by windVariance, rather than from blending between two named seasons.
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

        // Internal
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

        updateDirection(seasonalDirectionOffsetDegrees);
        updateSpeed(baseWindSpeed, windVariance);
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    private void updateDirection(float seasonalDirectionOffsetDegrees) {

        Vector3 globalDirection = windHandle.getGlobalWindDirection();
        float globalAngle = (float) Math.atan2(globalDirection.z, globalDirection.x);

        float seasonalOffsetRadians = (float) Math.toRadians(seasonalDirectionOffsetDegrees);
        float gustWobbleRadians = (float) Math.toRadians(
                Math.sin(elapsedTime * EngineSetting.WIND_GUST_DIRECTION_FREQUENCY)
                        * EngineSetting.WIND_GUST_DIRECTION_WOBBLE_DEGREES);

        float localAngle = globalAngle + seasonalOffsetRadians + gustWobbleRadians;

        windHandle.setLocalWindDirection(
                (float) Math.cos(localAngle),
                0.0f,
                (float) Math.sin(localAngle));
    }

    private void updateSpeed(float baseWindSpeed, float windVariance) {

        float speedGust = (float) (Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY) * 0.6
                + Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY_SECONDARY + 1.7) * 0.4);

        float seasonalSpeed = baseWindSpeed + speedGust * windVariance;

        float diurnalFactor = 1f + computeDiurnalFactor() * EngineSetting.WIND_DIURNAL_STRENGTH;

        float speedBeforeWeather = Math.max(
                EngineSetting.WIND_MIN_SPEED_FLOOR,
                seasonalSpeed * diurnalFactor);

        float weatherSpeedScale = weatherManager.getWindSpeedScale();

        windHandle.setLocalWindSpeed(speedBeforeWeather * weatherSpeedScale);
    }

    // Diurnal \\

    /*
     * Bell-shaped curve over the daily cycle, peaking at
     * WIND_DIURNAL_PEAK_TIME. Returns roughly [-1, 1] — cosine of the
     * angular distance from the peak around the full day.
     */
    private float computeDiurnalFactor() {

        double visualTimeOfDay = clockManager.getClockHandle().getVisualTimeOfDay();
        double angle = (visualTimeOfDay - EngineSetting.WIND_DIURNAL_PEAK_TIME) * Math.PI * 2.0;

        return (float) Math.cos(angle);
    }
}