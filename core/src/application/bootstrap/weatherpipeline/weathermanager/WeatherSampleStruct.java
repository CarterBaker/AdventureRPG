package application.bootstrap.weatherpipeline.weathermanager;

import engine.root.StructPackage;

class WeatherSampleStruct extends StructPackage {

    /*
     * Smoothed atmosphere values resolved at the reference coordinate —
     * precipitation, wind, humidity, visibility, fog scale, and temperature
     * modifier. RegionSampleBranch owns one live glide instance and one raw
     * target instance it lerps toward every frame.
     */

    private float precipitationIntensity;
    private float windSpeedScale;
    private float windTurbulenceScale;
    private float fogDensityScale;
    private float humidity;
    private float visibility;
    private float temperatureModifier;

    // Smoothing \\

    void copyFrom(WeatherSampleStruct source) {
        this.precipitationIntensity = source.precipitationIntensity;
        this.windSpeedScale = source.windSpeedScale;
        this.windTurbulenceScale = source.windTurbulenceScale;
        this.fogDensityScale = source.fogDensityScale;
        this.humidity = source.humidity;
        this.visibility = source.visibility;
        this.temperatureModifier = source.temperatureModifier;
    }

    void lerpToward(WeatherSampleStruct target, float alpha) {
        this.precipitationIntensity += (target.precipitationIntensity - this.precipitationIntensity) * alpha;
        this.windSpeedScale += (target.windSpeedScale - this.windSpeedScale) * alpha;
        this.windTurbulenceScale += (target.windTurbulenceScale - this.windTurbulenceScale) * alpha;
        this.fogDensityScale += (target.fogDensityScale - this.fogDensityScale) * alpha;
        this.humidity += (target.humidity - this.humidity) * alpha;
        this.visibility += (target.visibility - this.visibility) * alpha;
        this.temperatureModifier += (target.temperatureModifier - this.temperatureModifier) * alpha;
    }

    // Accessible \\

    float getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    void setPrecipitationIntensity(float precipitationIntensity) {
        this.precipitationIntensity = precipitationIntensity;
    }

    float getWindSpeedScale() {
        return windSpeedScale;
    }

    void setWindSpeedScale(float windSpeedScale) {
        this.windSpeedScale = windSpeedScale;
    }

    float getWindTurbulenceScale() {
        return windTurbulenceScale;
    }

    void setWindTurbulenceScale(float windTurbulenceScale) {
        this.windTurbulenceScale = windTurbulenceScale;
    }

    float getFogDensityScale() {
        return fogDensityScale;
    }

    void setFogDensityScale(float fogDensityScale) {
        this.fogDensityScale = fogDensityScale;
    }

    float getHumidity() {
        return humidity;
    }

    void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    float getVisibility() {
        return visibility;
    }

    void setVisibility(float visibility) {
        this.visibility = visibility;
    }

    float getTemperatureModifier() {
        return temperatureModifier;
    }

    void setTemperatureModifier(float temperatureModifier) {
        this.temperatureModifier = temperatureModifier;
    }
}