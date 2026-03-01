package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.io.File;
import java.util.List;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

/*
 * Discovers sprite image files, delegates image loading and GPU upload to
 * subsystems, and registers each resulting SpriteHandle with the SpriteManager.
 * Default mesh and material are resolved once at load time and shared across
 * all sprite model instances.
 */
class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private SpriteManager spriteManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private InternalBuildSystem internalBuildSystem;

    // Cached defaults
    private MeshHandle defaultMeshHandle;
    private int defaultMaterialID;

    // Internal \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.SPRITE_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Loading \\

    void loadSprites() {

        FileUtility.verifyDirectory(root, "Sprite directory not found: " + root.getAbsolutePath());

        resolveDefaults();

        List<File> spriteFiles = FileUtility.collectFiles(root, EngineSetting.TEXTURE_FILE_EXTENSIONS);

        for (int i = 0; i < spriteFiles.size(); i++)
            processSpriteFile(spriteFiles.get(i));
    }

    // Default Resolution \\

    private void resolveDefaults() {

        int meshID = meshManager.getMeshHandleIDFromMeshName(EngineSetting.SPRITE_DEFAULT_MESH);
        this.defaultMeshHandle = meshManager.getMeshHandleFromMeshHandleID(meshID);

        if (defaultMeshHandle == null)
            throwException("Default sprite mesh not found: '" + EngineSetting.SPRITE_DEFAULT_MESH + "'");

        this.defaultMaterialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.SPRITE_DEFAULT_MATERIAL);

        if (defaultMaterialID == -1)
            throwException("Default sprite material not found: '" + EngineSetting.SPRITE_DEFAULT_MATERIAL + "'");
    }

    // File Processing \\

    private void processSpriteFile(File file) {

        String spriteName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {

            SpriteData spriteData = internalBuildSystem.buildSpriteData(file, spriteName);
            int gpuHandle = GLSLUtility.pushSprite(spriteData.getImage());

            MaterialInstance material = materialManager.cloneMaterial(defaultMaterialID);
            material.setUniform("u_sprite", gpuHandle);

            ModelInstance modelInstance = modelManager.createModel(defaultMeshHandle, material);

            SpriteHandle spriteHandle = create(SpriteHandle.class);
            spriteHandle.constructor(spriteName, gpuHandle, spriteData.getWidth(), spriteData.getHeight(),
                    modelInstance);

            spriteManager.addSprite(spriteName, spriteHandle);

        } catch (RuntimeException e) {
            throwException("Failed to load sprite: " + file.getAbsolutePath(), e);
        }
    }
}