package com.internal.bootstrap.shaderpipeline.passmanager;

import java.io.File;
import java.util.List;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

/*
 * Drives the pass bootstrap sequence: file discovery and handle assembly.
 * Released after bootstrap so no parsing state persists into the runtime loop.
 */
class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private PassManager passManager;
    private InternalBuildSystem internalBuildSystem;

    private int passCount;

    // Internal \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.PASS_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.passCount = 0;
    }

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Bootstrap \\

    void loadPasses() {
        FileUtility.verifyDirectory(root, "Pass directory not found: " + root.getAbsolutePath());

        List<File> jsonFiles = FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < jsonFiles.size(); i++)
            passManager.addPass(internalBuildSystem.buildPass(root, jsonFiles.get(i), passCount++));
    }
}