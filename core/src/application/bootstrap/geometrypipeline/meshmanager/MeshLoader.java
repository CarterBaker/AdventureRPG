package application.bootstrap.geometrypipeline.meshmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.ibomanager.IBOBuilder;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOBuilder;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbomanager.VBOBuilder;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class MeshLoader extends LoaderPackage {

    /*
     * Drives the full mesh bootstrap pipeline per file. Creates a shared
     * VAOInstance per mesh, then delegates to the VAO, VBO, and IBO builders
     * before assembling the final MeshHandle via MeshBuilder. IDs are
     * derived from resource names via RegistryUtility. Supports on-demand
     * loading for meshes not yet in the palette at runtime.
     */

    // Internal
    private File root;
    private MeshManager meshManager;
    private VAOManager vaoManager;

    // Builders
    private MeshBuilder internalBuilder;
    private VAOBuilder vaoBuildSystem;
    private VBOBuilder vboBuildSystem;
    private IBOBuilder iboBuildSystem;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MESH_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Mesh JSON directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {

        this.vaoBuildSystem = create(
                VAOBuilder.class);
        this.vboBuildSystem = create(
                VBOBuilder.class);
        this.iboBuildSystem = create(
                IBOBuilder.class);
        this.internalBuilder = create(MeshBuilder.class);
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