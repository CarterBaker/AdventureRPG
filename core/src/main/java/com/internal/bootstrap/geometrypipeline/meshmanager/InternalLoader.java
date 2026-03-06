package com.internal.bootstrap.geometrypipeline.meshmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vao.VAOInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private MeshManager meshManager;
    private VAOManager vaoManager;
    private TextureManager textureManager;
    private int meshDataCount;

    // Builders
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
        this.vaoBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.vaomanager.InternalBuilder.class);
        this.vboBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.vbomanager.InternalBuilder.class);
        this.iboBuildSystem = create(
                com.internal.bootstrap.geometrypipeline.ibomanager.InternalBuilder.class);
        this.internalBuildSystem = create(InternalBuilder.class);
        this.meshDataCount = 0;
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.vaoManager = get(VAOManager.class);
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void load(File file) {

        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        vaoBuildSystem.build(resourceName, file, resourceName2File);

        VAOHandle vaoHandle = vaoManager.getVAOHandleDirect(resourceName);

        if (vaoHandle == null) {
            meshDataCount++;
            return;
        }

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoHandle);

        vboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);
        iboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);

        try {
            int meshID = meshDataCount++;
            MeshHandle meshHandle = internalBuildSystem.buildMeshHandle(
                    root, file, meshID, vaoInstance, textureManager);
            if (meshHandle != null)
                meshManager.addMeshHandle(resourceName, meshID, meshHandle);
        } catch (RuntimeException ex) {
            throwException("Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    // On-Demand Loading \\

    public void request(String resourceName) {

        File file = resourceName2File.get(resourceName);

        if (file == null)
            throwException(
                    "On-demand load failed — resource not found in scan registry: \"" + resourceName + "\"");

        request(file);
    }
}