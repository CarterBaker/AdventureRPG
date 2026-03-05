package com.internal.bootstrap.geometrypipeline.meshmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private MeshManager meshManager;
    private UVProvider uvProvider;
    private int meshDataCount;

    // Builders — all 4 owned and driven by this loader
    private InternalBuilder internalBuildSystem;
    private com.internal.bootstrap.geometrypipeline.vaomanager.InternalBuilder vaoBuildSystem;
    private com.internal.bootstrap.geometrypipeline.vbomanager.InternalBuilder vboBuildSystem;
    private com.internal.bootstrap.geometrypipeline.ibomanager.InternalBuilder iboBuildSystem;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MESH_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Mesh JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to list mesh files in directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        // Buffer builders created here — loader has sole ownership.
        // VAO must be first so VBO/IBO builders can rely on VAO
        // already being registered when they run.
        this.vaoBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.vaomanager.InternalBuilder.class);
        this.vboBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.vbomanager.InternalBuilder.class);
        this.iboBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.ibomanager.InternalBuilder.class);
        // Mesh builder last — assembles from whatever the buffer builders registered
        this.internalBuildSystem = create(InternalBuilder.class);
        this.meshDataCount = 0;
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
    }

    @Override
    protected void awake() {
        this.uvProvider = meshManager.createUVProvider();
    }

    @Override
    protected void load(File file) {

        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        // Feed all 3 buffer builders the file — each extracts only its own
        // section, resolves any string refs via the registry below, and
        // pushes the result to its manager. Mesh builder then assembles.
        vaoBuildSystem.build(resourceName, file, resourceName2File);
        vboBuildSystem.build(resourceName, file, resourceName2File);
        iboBuildSystem.build(resourceName, file, resourceName2File);

        try {
            int meshID = meshDataCount++;
            MeshHandle meshHandle = internalBuildSystem.buildMeshHandle(root, file, meshID, uvProvider);
            if (meshHandle != null)
                meshManager.addMeshHandle(resourceName, meshID, meshHandle);
        } catch (RuntimeException ex) {
            throwException("Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    // On-Demand Loading \\

    /*
     * Resolves a resource name to its File and delegates to the engine's
     * request(File), which removes it from the pending queue if present
     * and calls load() immediately. Crashes if the resource name is unknown
     * to this loader — it was never scanned and cannot be loaded on demand.
     */
    public void request(String resourceName) {

        File file = resourceName2File.get(resourceName);

        if (file == null)
            throwException(
                    "On-demand mesh load failed — resource not found in scan registry: \"" + resourceName + "\"");

        request(file);
    }
}