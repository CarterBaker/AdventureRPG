package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;
import java.util.List;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

/*
 * Drives the material bootstrap sequence: file discovery and handle assembly.
 * Released after bootstrap so no parsing state persists into the runtime loop.
 */
public class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private MaterialManager materialManager;
    private InternalBuildSystem internalBuildSystem;

    private int materialCount;

    // Internal \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.MATERIAL_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.materialCount = 0;
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Bootstrap \\

    void loadMaterials() {
        FileUtility.verifyDirectory(root, "Material directory not found: " + root.getAbsolutePath());

        List<File> jsonFiles = FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS);

        for (int i = 0; i < jsonFiles.size(); i++)
            materialManager.addMaterial(
                    internalBuildSystem.buildMaterial(root, jsonFiles.get(i), materialCount++));
    }
}