package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the season JSON directory and loads every named season climate
     * definition found there into SeasonManager. No fixed set is required —
     * the active calendar decides which season names actually get used.
     * Supports on-demand loading for a season not yet in the palette.
     */

    // Internal
    private File root;
    private SeasonManager seasonManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> seasonName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.seasonManager = get(SeasonManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SEASON_JSON_PATH);
        this.seasonName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Season root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String seasonName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        seasonName2File.put(seasonName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk season directory: " + root.getAbsolutePath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        SeasonHandle seasonHandle = internalBuilder.build(file, root);

        if (seasonHandle != null)
            seasonManager.addSeason(seasonHandle);
    }

    // On-Demand \\

    void request(String seasonName) {

        File file = seasonName2File.get(seasonName);

        if (file == null)
            throwException("On-demand season load failed — no file found for: \"" + seasonName + "\"");

        request(file);
    }
}