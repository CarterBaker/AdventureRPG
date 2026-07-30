package application.bootstrap.weatherpipeline.windmanager;

import application.bootstrap.calendarpipeline.clock.ClockInstance;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
import application.bootstrap.weatherpipeline.wind.WindHandle;
import application.bootstrap.weatherpipeline.wind.WindInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class LocalWindBranch extends BranchPackage {

    /*
     * Recomputes one grid's local wind every frame. Direction is the
     * global prevailing airflow rotated by the active season's prevailing
     * offset plus a shared gust wobble. Speed is the season's base speed
     * varied by a two-layer gust oscillation, shaped by that grid's own
     * diurnal curve, then scaled by that grid's own current weather.
     */

    private ClockManager clockManager;
    private SeasonManager seasonManager;

    private WindHandle globalWindHandle;

    private String lastSeasonName;
    private SeasonHandle activeSeason;

    private float elapsedTime;

    @Override
    protected void get() {
        this.clockManager = get(ClockManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    // Assignment \\

    void assignGlobalWind(WindHandle globalWindHandle) {
        this.globalWindHandle = globalWindHandle;
    }

    // Time \\

    void advanceTime(float deltaTime) {
        elapsedTime += deltaTime;
        resolveActiveSeason();
    }

    private void resolveActiveSeason() {

        String currentSeasonName = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeasonName == null || currentSeasonName.equals(lastSeasonName))
            return;

        lastSeasonName = currentSeasonName;
        activeSeason = seasonManager.getSeasonHandleFromSeasonName(currentSeasonName);
    }

    // Local Wind \\

    void updateLocalWind(WindInstance windInstance, WeatherInstance weatherInstance, ClockInstance clockInstance) {

        float baseWindSpeed = EngineSetting.WIND_GLOBAL_SPEED;
        float windVariance = 0f;
        float seasonalDirectionOffsetDegrees = 0f;

        if (activeSeason != null) {
            baseWindSpeed = activeSeason.getBaseWindSpeed();
            windVariance = activeSeason.getWindVariance();
            seasonalDirectionOffsetDegrees = activeSeason.getPrevailingWindDirectionDegrees();
        }

        boolean weatherResolved = weatherInstance != null && weatherInstance.isConfigured();

        float weatherTurbulence = weatherResolved
                ? weatherInstance.getBlendedWindTurbulenceScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE;

        float weatherSpeedScale = weatherResolved
                ? weatherInstance.getBlendedWindSpeedScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE;

        updateDirection(windInstance, seasonalDirectionOffsetDegrees, weatherTurbulence);
        updateSpeed(windInstance, baseWindSpeed, windVariance, weatherTurbulence, weatherSpeedScale, clockInstance);
    }

    private void updateDirection(WindInstance windInstance, float seasonalDirectionOffsetDegrees,
            float weatherTurbulence) {

        Vector3 globalDirection = globalWindHandle.getGlobalWindDirection();
        float globalAngle = (float) Math.atan2(globalDirection.z, globalDirection.x);

        float seasonalOffsetRadians = (float) Math.toRadians(seasonalDirectionOffsetDegrees);
        float gustWobbleRadians = (float) Math.toRadians(
                Math.sin(elapsedTime * EngineSetting.WIND_GUST_DIRECTION_FREQUENCY)
                        * EngineSetting.WIND_GUST_DIRECTION_WOBBLE_DEGREES * weatherTurbulence);

        float localAngle = globalAngle + seasonalOffsetRadians + gustWobbleRadians;

        windInstance.setLocalWindDirection(
                (float) Math.cos(localAngle),
                0.0f,
                (float) Math.sin(localAngle));
    }

    private void updateSpeed(
            WindInstance windInstance,
            float baseWindSpeed,
            float windVariance,
            float weatherTurbulence,
            float weatherSpeedScale,
            ClockInstance clockInstance) {

        float speedGust = (float) (Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY) * 0.6
                + Math.sin(elapsedTime * EngineSetting.WIND_GUST_SPEED_FREQUENCY_SECONDARY + 1.7) * 0.4)
                * weatherTurbulence;

        float seasonalSpeed = baseWindSpeed + speedGust * windVariance;

        float diurnalFactor = 1f + computeDiurnalFactor(clockInstance) * EngineSetting.WIND_DIURNAL_STRENGTH;

        float speedBeforeWeather = Math.max(
                EngineSetting.WIND_MIN_SPEED_FLOOR,
                seasonalSpeed * diurnalFactor);

        windInstance.setLocalWindSpeed(speedBeforeWeather * weatherSpeedScale);
    }

    private float computeDiurnalFactor(ClockInstance clockInstance) {

        double visualTimeOfDay = clockInstance.getVisualTimeOfDay();
        double angle = (visualTimeOfDay - EngineSetting.WIND_DIURNAL_PEAK_TIME) * Math.PI * 2.0;

        return (float) Math.cos(angle);
    }
}