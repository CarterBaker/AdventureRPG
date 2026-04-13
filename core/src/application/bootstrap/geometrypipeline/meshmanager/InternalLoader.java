package application.bootstrap.geometrypipeline.meshmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.core.engine.LoaderPackage;
import application.core.settings.EngineSetting;
import application.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Drives the full mesh bootstrap pipeline per file. Creates a shared
     * VAOInstance per mesh, then delegates to the VAO, VBO, and IBO builders
     * before assembling the final MeshHandle via InternalBuilder. IDs are
     * derived from resource names via RegistryUtility. Supports on-demand
     * loading for meshes not yet in the palette at runtime.
     */

    // Internal
    private File root;
    private MeshManager meshManager;
    private VAOManager vaoManager;

    // Builders
    private InternalBuilder internalBuilder;
    private application.bootstrap.geometrypipeline.vaomanager.InternalBuilder vaoBuildSystem;
    private application.bootstrap.geometrypipeline.vbomanager.InternalBuilder vboBuildSystem;
    private application.bootstrap.geometrypipeline.ibomanager.InternalBuilder iboBuildSystem;

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
                application.bootstrap.geometrypipeline.vaomanager.InternalBuilder.class);
        this.vboBuildSystem = create(
                application.bootstrap.geometrypipeline.vbomanager.InternalBuilder.class);
        this.iboBuildSystem = create(
                application.bootstrap.geometrypipeline.ibomanager.InternalBuilder.class);
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {

        this.meshManager = get(MeshManager.class);
        this.vaoManager = get(VAOManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        vaoBuildSystem.build(resourceName, file, resourceName2File);

        VAOHandle vaoHandle = vaoManager.getVAOHandleDirect(resourceName);

        if (vaoHandle == null)
            return;

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoHandle);

        vboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);
        iboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);

        try {
            MeshHandle meshHandle = internalBuilder.buildMeshHandle(root, file, vaoInstance);
            if (meshHandle != null)
                meshManager.addMeshHandle(resourceName, meshHandle);
        } catch (RuntimeException ex) {
            throwException("Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    // On-Demand \\

    void request(String resourceName) {

        File file = resourceName2File.get(resourceName);

        if (file == null)
            throwException("On-demand load failed — resource not found in scan registry: \""
                    + resourceName + "\"");

        request(file);
    }
}