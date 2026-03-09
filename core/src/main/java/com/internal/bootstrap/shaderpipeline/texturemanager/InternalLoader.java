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
 * PascalCase form of the array name. If no handle is found the array has no
 * UBO and seeding is skipped silently. When a handle is found, only the alias
 * IDs actually present in the atlas source files are written.
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

        FileUtility.verifyDirectory(root,
                "[TextureManager] Root directory not found: " + root.getAbsolutePath());

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

        List<File> imageFiles = FileUtility.collectFilesShallow(
                directory, EngineSetting.TEXTURE_FILE_EXTENSIONS);

        if (imageFiles.isEmpty())
            return;

        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
        TextureArrayData arrayData = internalBuilder.build(imageFiles, directory, arrayName);

        if (arrayData == null)
            return;

        seedUBO(arrayName, arrayData);
        pushToGPU(arrayData);
        clearHeapImages(arrayData);
    }

    // On-Demand Loading \\

    void request(String arrayName) {

        File directory = resourceName2File.get(arrayName);

        if (directory == null)
            throwException("[TextureManager] On-demand load failed — array not found: \""
                    + arrayName + "\"");

        request(directory);
    }

    // UBO Seeding \\

    /*
     * Converts the array name to PascalCase and asks UBOManager for a matching
     * handle. Only alias IDs actually found in the atlas source files are written.
     * UV scale is derived from tile pixel dimensions divided by atlas pixel size —
     * this gives the shader the normalised UV footprint of one tile regardless of
     * how many tiles are packed or what size they are.
     */
    private void seedUBO(String arrayName, TextureArrayData arrayData) {

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(FileUtility.toPascalCase(arrayName));

        if (ubo == null)
            return;

        IntIterator it = arrayData.getFoundAliasIds().iterator();
        while (it.hasNext()) {
            int aliasId = it.nextInt();
            String uniformName = aliasLibrarySystem.getUniformName(aliasId);
            if (uniformName != null && !uniformName.isEmpty())
                ubo.updateUniform(uniformName, aliasId);
        }

        TextureTileData firstTile = arrayData.getTileCoordinateMap().values().iterator().next();
        float uvScaleX = firstTile.getTileWidth() / (float) arrayData.getAtlasPixelSize();
        float uvScaleY = firstTile.getTileHeight() / (float) arrayData.getAtlasPixelSize();
        ubo.updateUniform(EngineSetting.TEXTURE_UV_SCALE_UNIFORM, new Vector2(uvScaleX, uvScaleY));

        ubo.push();
    }

    // GPU Upload \\

    /*
     * Uploads all alias layers as a texture array, then registers each tile with
     * its UV region derived from its packed pixel-space position and dimensions.
     */
    private void pushToGPU(TextureArrayData arrayData) {

        int gpuHandle = GLSLUtility.pushTextureArray(arrayData.getRawImageArray());
        int atlasPixelSize = arrayData.getAtlasPixelSize();
        float invAtlas = 1.0f / atlasPixelSize;

        for (TextureTileData tile : arrayData.getTileCoordinateMap().values()) {
            float u0 = tile.getAtlasX() * invAtlas;
            float v0 = tile.getAtlasY() * invAtlas;
            float u1 = (tile.getAtlasX() + tile.getTileWidth()) * invAtlas;
            float v1 = (tile.getAtlasY() + tile.getTileHeight()) * invAtlas;
            textureManager.registerTile(tile, u0, v0, u1, v1, arrayData, gpuHandle);
        }
    }

    private void clearHeapImages(TextureArrayData arrayData) {
        for (TextureTileData tile : arrayData.getTileCoordinateMap().values())
            tile.clearImages();
        arrayData.clearAtlases();
    }
}