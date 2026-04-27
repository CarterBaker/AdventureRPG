package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.renderpipeline.fbo.FboData;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Drives the FBO bootstrap sequence: directory walked in scan(), all JSON
 * descriptors queued, data registered per load() call, self-releases when
 * the queue empties.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private InternalBuilder internalBuilder;
    private FboManager fboManager;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> fboName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void scan() {
        this.root = new File(EngineSetting.FBO_CATALOG_JSON_PATH);
        this.fboName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "FBO directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
                    .forEach(file -> {
                        JsonObject jsonRoot = JsonUtility.loadJsonObject(file);
                        JsonArray list = jsonRoot.has("fbos") ? JsonUtility.validateArray(jsonRoot, "fbos")
                                : new JsonArray();
                        if (list.size() == 0 && jsonRoot.has("name"))
                            list.add(jsonRoot);
                        for (int i = 0; i < list.size(); i++) {
                            String name = JsonUtility.validateString(list.get(i).getAsJsonObject(), "name");
                            fboName2File.put(name, file);
                        }
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk FBO directory: " + root.getAbsolutePath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        ObjectArrayList<FboData> dataList = internalBuilder.buildData(file);
        for (int i = 0; i < dataList.size(); i++)
            fboManager.addFboData(dataList.get(i));
    }

    // On-Demand \\

    void request(String fboName) {
        File file = fboName2File.get(fboName);

        if (file == null)
            throwException("On-demand FBO load failed — not found in scan registry: \"" + fboName + "\"");

        request(file);
    }

}