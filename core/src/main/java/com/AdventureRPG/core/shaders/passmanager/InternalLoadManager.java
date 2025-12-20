package com.AdventureRPG.core.shaders.passmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.engine.EngineSetting;
import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.shaders.processingpass.ProcessingPass;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;

class InternalLoadManager extends ManagerFrame {

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
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.passCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.passManager = gameEngine.get(PassManager.class);
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
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
            throw new FileException.FileNotFoundException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> buildPassFromFile(path.toFile()));
        }

        catch (IOException e) {
            throw new FileException.FileReadException("PassManager failed to load one or more files: ", e);
        }
    }

    private void buildPassFromFile(File file) {

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
            compilePass(internalBuildSystem.buildPass(root, file, passCount++));
    }

    private void compilePass(ProcessingPass processingPass) {
        passManager.addPass(processingPass);
    }
}
