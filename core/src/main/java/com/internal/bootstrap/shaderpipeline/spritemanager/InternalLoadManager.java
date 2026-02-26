package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelHandle;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

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

    // Base \\

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

        FileUtility.verifyDirectory(root,
                "Sprite directory not found: " + root.getAbsolutePath());

        resolveDefaults();

        List<File> spriteFiles = collectSpriteFiles();

        for (File file : spriteFiles)
            processSpriteFile(file);
    }

    // Default Resolution \\

    private void resolveDefaults() {

        int meshID = meshManager.getMeshHandleIDFromMeshName(EngineSetting.SPRITE_DEFAULT_MESH);
        this.defaultMeshHandle = meshManager.getMeshHandleFromMeshHandleID(meshID);

        if (defaultMeshHandle == null)
            throwException("Default sprite mesh not found: '" + EngineSetting.SPRITE_DEFAULT_MESH + "'");

        this.defaultMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.SPRITE_DEFAULT_MATERIAL);

        if (defaultMaterialID == -1)
            throwException("Default sprite material not found: '" + EngineSetting.SPRITE_DEFAULT_MATERIAL + "'");
    }

    // File Collection \\

    private List<File> collectSpriteFiles() {
        try (var stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.TEXTURE_FILE_EXTENSIONS
                            .contains(FileUtility.getExtension(f)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throwException("Failed to walk sprite directory: " + root.getAbsolutePath(), e);
            return null;
        }
    }

    // File Processing \\

    private void processSpriteFile(File file) {

        String spriteName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            SpriteData spriteData = internalBuildSystem.buildSpriteData(file, spriteName);

            int gpuHandle = GLSLUtility.pushSprite(spriteData.getImage());

            MaterialHandle materialHandle = materialManager.cloneMaterial(defaultMaterialID);
            materialHandle.setUniform("u_sprite", gpuHandle);

            ModelHandle modelHandle = modelManager.createModel(defaultMeshHandle, materialHandle);

            SpriteHandle spriteHandle = create(SpriteHandle.class);
            spriteHandle.constructor(
                    spriteName,
                    gpuHandle,
                    spriteData.getWidth(),
                    spriteData.getHeight(),
                    modelHandle);

            spriteManager.addSprite(spriteName, spriteHandle);

        } catch (RuntimeException e) {
            throwException("Failed to load sprite: " + file.getAbsolutePath(), e);
        }
    }
}