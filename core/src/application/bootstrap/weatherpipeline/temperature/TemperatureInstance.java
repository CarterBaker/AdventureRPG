package application.bootstrap.weatherpipeline.temperature;

import engine.root.EngineSetting;
import engine.root.InstancePackage;

public class TemperatureInstance extends InstancePackage {

    /*
     * One grid's own current ambient temperature, resolved each frame from
     * that grid's local weather. Owned directly by GridInstance and read
     * by SkyColorSystem to bias that same grid's sky/cloud colors.
     */

    private float temperature;

    public void constructor() {
        this.temperature = EngineSetting.DEFAULT_BASE_TEMPERATURE;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getTemperature() {
        return temperature;
    }
}