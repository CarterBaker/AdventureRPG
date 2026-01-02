package com.AdventureRPG.core.shaderpipeline.passmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.shaderpipeline.processingpass.ProcessingPassHandle;
import com.AdventureRPG.core.util.FileUtility;

class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private PassManager passManager;
    private InternalBuildSystem internalBuildSystem;

    private int passCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.PASS_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);

        this.passCount = 0;
    }

    @Override
    protected void get() {

        // Internal
        this.passManager = get(PassManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Pass Management \\

    void loadPasses() {
        assignMeshData();
        loadAllFiles();
    }

    // Necessary for the internal builder to have data correctly assigned
    private void assignMeshData() {
        internalBuildSystem.assignMeshData();
    }

    // Load \\

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throwException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> buildPassFromFile(path.toFile()));
        }

        catch (IOException e) {
            throwException("PassManager failed to load one or more files: ", e);
        }
    }

    private void buildPassFromFile(File file) {

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
            compilePass(internalBuildSystem.buildPass(root, file, passCount++));
    }

    private void compilePass(ProcessingPassHandle processingPassHandle) {
        passManager.addPass(processingPassHandle);
    }
}
