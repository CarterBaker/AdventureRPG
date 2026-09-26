package application.bootstrap.weatherpipeline.weathermanager;

import java.io.File;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class WeatherLoader extends LoaderPackage {

    /*
     * Scans the weather JSON directory and loads all weather definitions into
     * WeatherManager. Supports on-demand loading for weathers not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private WeatherManager weatherManager;
    private WeatherBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(WeatherBuilder.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.WEATHER_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Weather root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        WeatherHandle weatherHandle = internalBuilder.build(file, root);

        if (weatherHandle != null)
            weatherManager.addWeatherHandle(weatherHandle);
    }

    // On-Demand \\

    void request(String weatherName) {

        File file = resourceName2File.get(weatherName);

        if (file == null)
            throwException("On-demand weather load failed — no file found for: \"" + weatherName + "\"");

        request(file);
    }
}