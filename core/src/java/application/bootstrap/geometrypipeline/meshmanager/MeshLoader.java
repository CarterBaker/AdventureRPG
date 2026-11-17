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
     * before assembling the final MeshHandle via MeshBuilder; MeshManager
     * assigns each mesh its ID as it registers. Supports on-demand loading
     * for meshes not yet in the palette at runtime, among them a file from
     * outside the mesh directory made known under a name of its own, as the
     * editor's workspace models are.
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
    private Object2ObjectOpenHashMap<File, String> file2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MESH_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.file2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Mesh ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
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

        String resourceName = resolveResourceName(file);

        vaoBuildSystem.build(resourceName, file, resourceName2File);

        VAOHandle vaoHandle = vaoManager.getVAOHandleDirect(resourceName);

        if (vaoHandle == null)
            return;

        VAOInstance vaoInstance = vaoManager.createVAOInstance(vaoHandle);

        vboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);
        iboBuildSystem.build(resourceName, file, resourceName2File, vaoInstance);

        try {
            MeshHandle meshHandle = internalBuilder.buildMeshHandle(resourceName, file, vaoInstance);
            if (meshHandle != null)
                meshManager.addMeshHandle(resourceName, meshHandle);
        } catch (RuntimeException ex) {
            throwException("Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    // A file's mesh name: the one it was made known under, else its path below the mesh directory
    private String resolveResourceName(File file) {

        String resourceName = file2ResourceName.get(file);

        return resourceName != null ? resourceName : FileUtility.getPathWithFileNameWithoutExtension(root, file);
    }

    // On-Demand \\

    // A file from outside the mesh directory made known under a name, loaded the first time the name is asked for
    void registerFile(String resourceName, File file) {
        resourceName2File.put(resourceName, file);
        file2ResourceName.put(file, resourceName);
    }

    void request(String resourceName) {

        File file = resourceName2File.get(resourceName);

        if (file == null)
            throwException("On-demand load failed — resource not found in scan registry: \""
                    + resourceName + "\"");

        request(file);
    }
}