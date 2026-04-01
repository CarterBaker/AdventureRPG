package com.internal.bootstrap.shaderpipeline.spritemanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteData;
import com.internal.bootstrap.shaderpipeline.sprite.SpriteHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Discovers sprite image files in scan(), processes one file per load() call,
 * and self-releases when the queue empties. Default mesh and material are
 * resolved in awake() before batching begins.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private SpriteManager spriteManager;
    private MeshManager meshManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private InternalBuilder internalBuilder;

    // Cached Defaults
    private MeshHandle defaultMeshHandle;
    private int defaultMaterialID;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> spriteName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SPRITE_PATH);
        this.spriteName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Sprite directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.TEXTURE_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        spriteName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk sprite directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.spriteManager = get(SpriteManager.class);
        this.meshManager = get(MeshManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void awake() {

        this.defaultMeshHandle = meshManager.getMeshHandleFromMeshName(EngineSetting.SPRITE_DEFAULT_MESH);

        if (defaultMeshHandle == null)
            throwException("Default sprite mesh not found: '" + EngineSetting.SPRITE_DEFAULT_MESH + "'");

        this.defaultMaterialID = materialManager.getMaterialIDFromMaterialName(
                EngineSetting.SPRITE_DEFAULT_MATERIAL);

        if (defaultMaterialID == -1)
            throwException("Default sprite material not found: '" + EngineSetting.SPRITE_DEFAULT_MATERIAL + "'");
    }

    // Load \\

    @Override
    protected void load(File file) {

        String spriteName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            BufferedImage image = internalBuilder.loadImage(file);
            int gpuHandle = GLSLUtility.pushSprite(image);
            float[] border = internalBuilder.parseCompanionBorder(file);

            MaterialInstance material = materialManager.cloneMaterial(defaultMaterialID);
            material.setUniform("u_sprite", gpuHandle);

            ModelInstance modelInstance = modelManager.createModel(defaultMeshHandle, material);

            SpriteData data = new SpriteData(
                    spriteName,
                    gpuHandle,
                    image.getWidth(),
                    image.getHeight(),
                    border[0], border[1], border[2], border[3],
                    modelInstance);

            SpriteHandle handle = create(SpriteHandle.class);
            handle.constructor(data);

            spriteManager.addSpriteHandle(spriteName, handle);
        } catch (RuntimeException e) {
            throwException("Failed to load sprite: " + file.getAbsolutePath(), e);
        }
    }

    // On-Demand \\

    void request(String spriteName) {

        File file = spriteName2File.get(spriteName);

        if (file == null)
            throwException("On-demand sprite load failed — not found in scan registry: \"" + spriteName + "\"");

        request(file);
    }

    // Accessors \\

    MeshHandle getDefaultMeshHandle() {
        return defaultMeshHandle;
    }

    int getDefaultMaterialID() {
        return defaultMaterialID;
    }
}