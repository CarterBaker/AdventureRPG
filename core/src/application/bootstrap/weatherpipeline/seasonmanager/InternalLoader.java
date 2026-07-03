package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the season JSON directory and loads all four season climate
     * definitions into SeasonManager. Filenames must match the Season enum
     * names exactly (SPRING.json, SUMMER.json, FALL.json, WINTER.json) — no
     * on-demand loading, since the full set is required at bootstrap.
     */

    // Internal
    private File root;
    private SeasonManager seasonManager;
    private InternalBuilder internalBuilder;

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

        FileUtility.verifyDirectory(root, "Season root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(fileQueue::offer);
        } catch (IOException e) {
            throwException("Failed to walk season directory: " + root.getAbsolutePath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        seasonManager.addSeason(internalBuilder.build(file, root));
    }
}