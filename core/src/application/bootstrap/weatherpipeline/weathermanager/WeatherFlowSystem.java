package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

class WeatherFlowSystem extends SystemPackage {

    /*
     * Scrolls the static weather image across the world. The offset is a
     * closed-form function of the world's shared epoch time, so every player
     * and every window reads the same weather at the same moment, and it
     * advances smoothly every frame. Prevailing speed is a kph figure scaled
     * to the world's size against a real planet and to its rotation, and a
     * two-wave cross-stream meander swings the heading so storms do not
     * always arrive from the same bearing.
     */

    // Internal
    private WorldManager worldManager;
    private ClockManager clockManager;

    // Flow
    private double flowSeconds;
    private double offsetXBlocks;
    private double offsetZBlocks;

    // Base \\

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void awake() {

        if (worldManager.getActiveWorld() == null)
            throwException("WeatherFlowSystem could not resolve an active world.");
    }

    @Override
    protected void update() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        this.flowSeconds = (internal.getTime() - clockManager.getClockHandle().getWorldEpochStart())
                / EngineSetting.MILLIS_PER_SECOND;

        double prevailingSpeed = resolvePrevailingSpeedBlocksPerSecond(activeWorld);
        double lateralSpeed = Math.abs(prevailingSpeed)
                * Math.tan(Math.toRadians(EngineSetting.WEATHER_FLOW_MEANDER_ANGLE_DEGREES));

        this.offsetXBlocks = prevailingSpeed * flowSeconds;
        this.offsetZBlocks = lateralSpeed * resolveMeanderDisplacementSeconds();
    }

    // Speed \\

    private double resolvePrevailingSpeedBlocksPerSecond(WorldHandle activeWorld) {

        double worldCircumferenceMeters = activeWorld.getWorldScale().x * (double) EngineSetting.BLOCK_SIZE;
        double worldScaleRatio = worldCircumferenceMeters / EngineSetting.WEATHER_FLOW_REFERENCE_CIRCUMFERENCE_METERS;
        double metersPerSecond = EngineSetting.WEATHER_FLOW_SPEED_KPH * EngineSetting.KPH_TO_METERS_PER_SECOND
                * worldScaleRatio;

        return -(metersPerSecond / EngineSetting.BLOCK_SIZE) * activeWorld.getRotationSpeed();
    }

    // Meander \\

    private double resolveMeanderDisplacementSeconds() {

        double primaryOmega = (Math.PI * 2.0) / EngineSetting.WEATHER_FLOW_MEANDER_PERIOD_SECONDS;
        double secondaryOmega = (Math.PI * 2.0) / EngineSetting.WEATHER_FLOW_MEANDER_SECONDARY_PERIOD_SECONDS;
        double secondaryWeight = EngineSetting.WEATHER_FLOW_MEANDER_SECONDARY_WEIGHT;

        return (1.0 - secondaryWeight) / primaryOmega * Math.sin(primaryOmega * flowSeconds)
                + secondaryWeight / secondaryOmega
                        * Math.sin(secondaryOmega * flowSeconds + EngineSetting.WEATHER_FLOW_MEANDER_SECONDARY_PHASE);
    }

    // Accessible \\

    double getOffsetXBlocks() {
        return offsetXBlocks;
    }

    double getOffsetZBlocks() {
        return offsetZBlocks;
    }
}
