package com.internal.bootstrap.itempipeline.tooltypemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the tool type JSON directory and loads all tool type definitions
     * into ToolTypeManager. Pre-registers tool type names during scan so that
     * on-demand loading can resolve names to files before full load completes.
     */

    // Internal
    private File root;
    private ToolTypeManager toolTypeManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> toolTypeName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TOOL_TYPE_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.toolTypeName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Tool type directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        preRegisterToolTypeNames(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk tool type directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.toolTypeManager = get(ToolTypeManager.class);
    }

    // Pre-Registration \\

    private void preRegisterToolTypeNames(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray toolArray = json.getAsJsonArray("tools");

            if (toolArray == null)
                return;

            for (int i = 0; i < toolArray.size(); i++) {
                JsonObject toolJson = toolArray.get(i).getAsJsonObject();
                if (!toolJson.has("name"))
                    continue;
                String localName = toolJson.get("name").getAsString();
                String toolTypeName = resourceName + "/" + localName;
                toolTypeName2ResourceName.put(toolTypeName, resourceName);
            }
        } catch (Exception e) {
            throwException("Failed to pre-register tool type names from: " + file.getPath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<ToolTypeHandle> tools = internalBuilder.build(file, root);

        for (int i = 0; i < tools.size(); i++)
            toolTypeManager.addToolType(tools.get(i));
    }

    // On-Demand \\

    void request(String toolTypeName) {

        String resourceName = toolTypeName2ResourceName.get(toolTypeName);

        if (resourceName == null)
            throwException("On-demand tool type load failed — no file found for: \"" + toolTypeName + "\"");

        request(resourceName2File.get(resourceName));
    }
}