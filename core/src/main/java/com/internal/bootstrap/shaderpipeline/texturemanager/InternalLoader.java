package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.internal.bootstrap.shaderpipeline.Texture.TextureArrayData;
import com.internal.bootstrap.shaderpipeline.Texture.TextureTileData;
import com.internal.bootstrap.shaderpipeline.texturemanager.aliassystem.AliasLibrarySystem;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.mathematics.vectors.Vector2;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Orchestrates the full texture-array loading pipeline: discovers atlas
 * directories in scan(), processes one directory per load() call — building
 * the atlas, seeding any companion UBO, pushing to GPU, and clearing heap
 * images — then self-releases when the queue empties.
 *
 * UBO seeding is optional — gated by whether a UBO handle exists under the
 * PascalCase form of the array name (e.g. "blocks/stone" → "BlocksStone").
 * If no handle is found the array has no UBO and seeding is skipped silently.
 * When a handle is found, only the alias IDs actually present in the atlas
 * source files are written — aliases not found in this atlas are skipped,
 * preventing writes to uniforms the UBO does not declare.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private TextureManager textureManager;
    private UBOManager uboManager;
    private AliasLibrarySystem aliasLibrarySystem;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "[TextureManager] Root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(p -> Files.isDirectory(p) && !p.equals(root.toPath()))
                    .map(Path::toFile)
                    .forEach(directory -> {
                        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
                        resourceName2File.put(arrayName, directory);
                        fileQueue.offer(directory);
                    });
        } catch (IOException e) {
            throwException("Failed to walk texture directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.aliasLibrarySystem = create(AliasLibrarySystem.class);
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        aliasLibrarySystem.loadAliases();
    }

    // Load \\

    @Override
    protected void load(File directory) {

        List<File> imageFiles = FileUtility.collectFilesShallow(directory, EngineSetting.TEXTURE_FILE_EXTENSIONS);

        if (imageFiles.isEmpty())
            return;

        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);

        TextureArrayData textureArrayData = internalBuilder.build(imageFiles, directory, arrayName);

        seedUBO(arrayName, textureArrayData);
        pushTextureToGPU(textureArrayData);
        clearTextureImages(textureArrayData);
    }

    // On-Demand Loading \\

    void request(String arrayName) {

        File directory = resourceName2File.get(arrayName);

        if (directory == null)
            throwException(
                    "[TextureManager] On-demand texture load failed — array not found in scan registry: \""
                            + arrayName + "\"");

        request(directory);
    }

    // UBO Seeding \\

    /*
     * Converts the array name to PascalCase and asks UBOManager for a matching
     * handle. If none exists this array has no UBO — skip silently.
     * Only alias IDs that were actually found in the atlas source files are
     * written — no uniform is touched unless this atlas provided that layer.
     */
    private void seedUBO(String arrayName, TextureArrayData textureArrayData) {

        String uboName = FileUtility.toPascalCase(arrayName);
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(uboName);

        if (ubo == null)
            return;

        IntIterator it = textureArrayData.getFoundAliasIds().iterator();
        while (it.hasNext()) {
            int aliasId = it.nextInt();
            String uniformName = aliasLibrarySystem.getUniformName(aliasId);
            if (uniformName != null && !uniformName.isEmpty())
                ubo.updateUniform(uniformName, aliasId);
        }

        float uvPerBlock = 1.0f / textureArrayData.getAtlasSize();
        ubo.updateUniform(EngineSetting.TEXTURE_UV_SCALE_UNIFORM, new Vector2(uvPerBlock, uvPerBlock));

        ubo.push();
    }

    // GPU Upload \\

    private void pushTextureToGPU(TextureArrayData textureArrayData) {

        int gpuHandle = GLSLUtility.pushTextureArray(textureArrayData.getRawImageArray());
        int atlasSize = textureArrayData.getAtlasSize();
        float tileSize = 1.0f / atlasSize;

        for (var entry : textureArrayData.getTileCoordinateMap().object2ObjectEntrySet()) {
            TextureTileData tile = entry.getValue();
            float u0 = tile.getAtlasX() * tileSize;
            float v0 = tile.getAtlasY() * tileSize;
            textureManager.registerTile(tile, u0, v0, u0 + tileSize, v0 + tileSize, textureArrayData, gpuHandle);
        }
    }

    private void clearTextureImages(TextureArrayData textureArrayData) {
        for (TextureTileData tile : textureArrayData.getTileCoordinateMap().values())
            tile.clearImages();
        textureArrayData.clearAtlases();
    }
}