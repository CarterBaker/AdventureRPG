package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class InternalBufferBranch extends BranchPackage {

    /*
     * Pushes the centre weather sample to the WeatherData UBO and the four
     * cardinal samples to the WeatherRegionData UBO every frame. Wired to
     * RegionSampleBranch via assignData() after awake.
     */

    // Internal
    private UBOManager uboManager;
    private RegionSampleBranch regionSampleBranch;

    // UBO
    private UBOHandle weatherData;
    private UBOHandle weatherRegionData;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        this.weatherData = uboManager.getUBOHandleFromUBOName(EngineSetting.WEATHER_DATA_UBO);
        this.weatherRegionData = uboManager.getUBOHandleFromUBOName(EngineSetting.WEATHER_REGION_DATA_UBO);
    }

    @Override
    protected void update() {
        pushWeatherData();
        pushWeatherRegionData();
    }

    // Assignment \\

    void assignData(RegionSampleBranch regionSampleBranch) {
        this.regionSampleBranch = regionSampleBranch;
    }

    // Push \\

    private void pushWeatherData() {

        WeatherSampleStruct center = regionSampleBranch.getCenterSample();

        weatherData.updateUniform("u_cloudCoverage", center.getCloudCoverage());
        weatherData.updateUniform("u_cloudColor", center.getCloudColor());
        weatherData.updateUniform("u_cloudType", center.getCloudType());
        weatherData.updateUniform("u_precipitationIntensity", center.getPrecipitationIntensity());
        weatherData.updateUniform("u_windSpeedScale", center.getWindSpeedScale());
        weatherData.updateUniform("u_fogDensityScale", center.getFogDensityScale());

        uboManager.push(weatherData);
    }

    private void pushWeatherRegionData() {

        pushRegion(regionSampleBranch.getNorthSample(),
                "u_cloudCoverageNorth", "u_cloudColorNorth", "u_cloudTypeNorth");
        pushRegion(regionSampleBranch.getEastSample(),
                "u_cloudCoverageEast", "u_cloudColorEast", "u_cloudTypeEast");
        pushRegion(regionSampleBranch.getSouthSample(),
                "u_cloudCoverageSouth", "u_cloudColorSouth", "u_cloudTypeSouth");
        pushRegion(regionSampleBranch.getWestSample(),
                "u_cloudCoverageWest", "u_cloudColorWest", "u_cloudTypeWest");

        uboManager.push(weatherRegionData);
    }

    private void pushRegion(
            WeatherSampleStruct sample,
            String coverageUniform,
            String colorUniform,
            String typeUniform) {

        weatherRegionData.updateUniform(coverageUniform, sample.getCloudCoverage());
        weatherRegionData.updateUniform(colorUniform, sample.getCloudColor());
        weatherRegionData.updateUniform(typeUniform, sample.getCloudType());
    }
}