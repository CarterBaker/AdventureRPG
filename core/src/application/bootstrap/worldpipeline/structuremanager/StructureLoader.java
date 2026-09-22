package application.bootstrap.worldpipeline.structuremanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructureLoader extends LoaderPackage {

    /*
     * Scans the structure JSON directory and loads every definition into
     * StructureManager. During scan() each file is peeked for whether it can
     * place itself — a "spawn" rule or hand-authored "locations" — and those
     * names are buffered and handed to StructureManager in get(), because
     * placement needs the full catalogue of self-placing structures up front
     * while everything else (roads, buildings that only appear inside a
     * layout) is only ever reached by name and can wait to be asked for.
     * Like biomes, structures are resolved from world-generation workers, so
     * on-demand requests build and register directly instead of going
     * through the shared file queue the main thread drains, and are
     * synchronized so two workers asking for the same definition build it
     * once.
     */

    // Internal
    private File root;
    private StructureManager structureManager;
    private StructureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Placeable Scan Buffer — filled in scan(), handed off and cleared in get()
    private ObjectArrayList<String> scannedPlaceableNames;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(StructureBuilder.class);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        structureManager.registerPlaceableNames(scannedPlaceableNames);
        scannedPlaceableNames = null;
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.STRUCTURE_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.scannedPlaceableNames = new ObjectArrayList<>();

        FileUtility.verifyDirectory(root, "Structure root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .sorted()
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        scanPlaceable(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk structure directory: " + root.getAbsolutePath(), e);
        }
    }

    // Placeable Scan \\

    private void scanPlaceable(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);

            if (json.has("spawn") || json.has("locations"))
                scannedPlaceableNames.add(resourceName);

        } catch (Exception e) {
            throwException("Failed to peek structure definition: " + file.getPath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        loadFile(file);
    }

    // On-Demand \\

    /*
     * Resolves and registers a single structure by name directly, bypassing
     * the base loader's file-queue request path for the same reason
     * BiomeLoader does: that queue is drained by the main thread while this
     * is called from WorldStreaming workers. Leaving the file queued is
     * harmless — registration is idempotent.
     */
    synchronized void request(String structureName) {

        if (structureManager.hasStructure(structureName))
            return;

        File file = resourceName2File.get(structureName);

        if (file == null)
            throwException("On-demand structure load failed — no file found for: \"" + structureName + "\"");

        loadFile(file);
    }

    private synchronized void loadFile(File file) {

        String structureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        if (structureManager.hasStructure(structureName))
            return;

        StructureHandle structureHandle = internalBuilder.build(file, root);

        if (structureHandle != null)
            structureManager.addStructure(structureHandle);
    }
}
