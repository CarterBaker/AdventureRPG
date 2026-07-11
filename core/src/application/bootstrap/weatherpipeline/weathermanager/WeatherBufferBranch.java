package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class WeatherBufferBranch extends BranchPackage {

        /*
         * Pushes the centre weather sample to the WeatherData UBO and all eight
         * directional samples (N, NE, E, SE, S, SW, W, NW) to the
         * WeatherRegionData UBO every frame. Wired to RegionSampleBranch via
         * assignData() after awake.
         *
         * pushRegion() takes a direction suffix ("North", "Northeast", ...) and
         * builds each uniform name by concatenation rather than taking 15
         * separate name parameters per call site — the field set grew from 3 to
         * 15 once the sky dome needed the full CloudData shading surface per
         * direction (see WeatherSampleStruct/RegionSampleBranch/Clouds.glsl),
         * and a positional 15-string-parameter call is unreadable and easy to
         * mis-order. The uniform names this produces ("u_cloudCoverage" +
         * "North" = "u_cloudCoverageNorth") match WeatherRegionData.json's
         * existing naming convention exactly.
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
                weatherData.updateUniform("u_cloudTopColor", center.getCloudTopColor());
                weatherData.updateUniform("u_cloudShadowColor", center.getCloudShadowColor());
                weatherData.updateUniform("u_cloudAltitude", center.getCloudAltitude());
                weatherData.updateUniform("u_cloudDensity", center.getCloudDensity());
                weatherData.updateUniform("u_cloudShadeStrength", center.getCloudShadeStrength());
                weatherData.updateUniform("u_cloudRimLightStrength", center.getCloudRimLightStrength());
                weatherData.updateUniform("u_cloudAmbientOcclusionStrength", center.getCloudAmbientOcclusionStrength());
                weatherData.updateUniform("u_cloudBrightnessMultiplier", center.getCloudBrightnessMultiplier());
                weatherData.updateUniform("u_cloudToonBands", center.getCloudToonBands());
                weatherData.updateUniform("u_cloudDensityNoiseScale", center.getCloudDensityNoiseScale());
                weatherData.updateUniform("u_cloudNoiseWarpStrength", center.getCloudNoiseWarpStrength());
                weatherData.updateUniform("u_cloudCoverageBias", center.getCloudCoverageBias());
                weatherData.updateUniform("u_cloudSilhouetteSoftness", center.getCloudSilhouetteSoftness());
                weatherData.updateUniform("u_precipitationIntensity", center.getPrecipitationIntensity());
                weatherData.updateUniform("u_windSpeedScale", center.getWindSpeedScale());
                weatherData.updateUniform("u_fogDensityScale", center.getFogDensityScale());

                uboManager.push(weatherData);
        }

        private void pushWeatherRegionData() {

                pushRegion(regionSampleBranch.getNorthSample(), "North");
                pushRegion(regionSampleBranch.getNortheastSample(), "Northeast");
                pushRegion(regionSampleBranch.getEastSample(), "East");
                pushRegion(regionSampleBranch.getSoutheastSample(), "Southeast");
                pushRegion(regionSampleBranch.getSouthSample(), "South");
                pushRegion(regionSampleBranch.getSouthwestSample(), "Southwest");
                pushRegion(regionSampleBranch.getWestSample(), "West");
                pushRegion(regionSampleBranch.getNorthwestSample(), "Northwest");

                uboManager.push(weatherRegionData);
        }

        private void pushRegion(WeatherSampleStruct sample, String directionSuffix) {

                weatherRegionData.updateUniform("u_cloudCoverage" + directionSuffix, sample.getCloudCoverage());
                weatherRegionData.updateUniform("u_cloudColor" + directionSuffix, sample.getCloudColor());
                weatherRegionData.updateUniform("u_cloudTopColor" + directionSuffix, sample.getCloudTopColor());
                weatherRegionData.updateUniform("u_cloudShadowColor" + directionSuffix, sample.getCloudShadowColor());
                weatherRegionData.updateUniform("u_cloudAltitude" + directionSuffix, sample.getCloudAltitude());
                weatherRegionData.updateUniform("u_cloudDensity" + directionSuffix, sample.getCloudDensity());
                weatherRegionData.updateUniform("u_cloudShadeStrength" + directionSuffix,
                                sample.getCloudShadeStrength());
                weatherRegionData.updateUniform("u_cloudRimLightStrength" + directionSuffix,
                                sample.getCloudRimLightStrength());
                weatherRegionData.updateUniform(
                                "u_cloudAmbientOcclusionStrength" + directionSuffix,
                                sample.getCloudAmbientOcclusionStrength());
                weatherRegionData.updateUniform(
                                "u_cloudBrightnessMultiplier" + directionSuffix, sample.getCloudBrightnessMultiplier());
                weatherRegionData.updateUniform("u_cloudToonBands" + directionSuffix, sample.getCloudToonBands());
                weatherRegionData.updateUniform(
                                "u_cloudDensityNoiseScale" + directionSuffix, sample.getCloudDensityNoiseScale());
                weatherRegionData.updateUniform(
                                "u_cloudNoiseWarpStrength" + directionSuffix, sample.getCloudNoiseWarpStrength());
                weatherRegionData.updateUniform("u_cloudCoverageBias" + directionSuffix, sample.getCloudCoverageBias());
                weatherRegionData.updateUniform(
                                "u_cloudSilhouetteSoftness" + directionSuffix, sample.getCloudSilhouetteSoftness());
        }
}