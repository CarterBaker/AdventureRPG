package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Orchestrates the full texture-array loading pipeline. Discovers atlas
 * directories in scan(), processes one directory per load() call — building
 * the atlas, seeding any companion UBO, pushing to GPU, and clearing heap
 * images — then self-releases when the queue empties.
 *
 * UBO seeding is optional — gated by whether a UBO handle exists under the
 * PascalCase form of the array name. Only alias IDs present in atlas source
 * files are written.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private TextureManager textureManager;
    private UBOManager uboManager;
    private AliasLibrarySystem aliasLibrarySystem;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> arrayName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.arrayName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root,
                "Texture root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(p -> Files.isDirectory(p) && !p.equals(root.toPath()))
                    .map(Path::toFile)
                    .forEach(directory -> {
                        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
                        arrayName2File.put(arrayName, directory);
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
        TextureArrayStruct arrayStruct = internalBuilder.build(imageFiles, directory, arrayName);

        if (arrayStruct == null)
            return;

        seedUBO(arrayName, arrayStruct);
        pushToGPU(arrayStruct);
        clearHeapImages(arrayStruct);
    }

    // On-Demand \\

    void request(String arrayName) {

        File directory = arrayName2File.get(arrayName);

        if (directory == null)
            throwException("On-demand texture load failed — array not found: \"" + arrayName + "\"");

        request(directory);
    }

    // UBO Seeding \\

    private void seedUBO(String arrayName, TextureArrayStruct arrayStruct) {

        UBOHandle ubo = uboManager.findUBOHandle(FileUtility.toPascalCase(arrayName));

        if (ubo == null)
            return;

        IntIterator it = arrayStruct.getFoundAliasIds().iterator();

        while (it.hasNext()) {
            int aliasId = it.nextInt();
            String uniformName = aliasLibrarySystem.getUniformName(aliasId);
            if (uniformName != null && !uniformName.isEmpty())
                ubo.updateUniform(uniformName, aliasId);
        }

        TextureTileStruct firstTile = arrayStruct.getTileCoordinateMap().values().iterator().next();
        float uvScaleX = firstTile.getTileWidth() / (float) arrayStruct.getAtlasPixelSize();
        float uvScaleY = firstTile.getTileHeight() / (float) arrayStruct.getAtlasPixelSize();
        ubo.updateUniform(EngineSetting.TEXTURE_UV_SCALE_UNIFORM, new Vector2(uvScaleX, uvScaleY));

        uboManager.push(ubo);
    }

    // GPU Upload \\

    private void pushToGPU(TextureArrayStruct arrayStruct) {

        int gpuHandle = GLSLUtility.pushTextureArray(arrayStruct.getRawImageArray());
        int atlasPixelSize = arrayStruct.getAtlasPixelSize();
        float invAtlas = 1.0f / atlasPixelSize;

        for (TextureTileStruct tile : arrayStruct.getTileCoordinateMap().values()) {
            float u0 = tile.getAtlasX() * invAtlas;
            float v0 = tile.getAtlasY() * invAtlas;
            float u1 = (tile.getAtlasX() + tile.getTileWidth()) * invAtlas;
            float v1 = (tile.getAtlasY() + tile.getTileHeight()) * invAtlas;
            textureManager.registerTile(tile, u0, v0, u1, v1, arrayStruct, gpuHandle);
        }
    }

    private void clearHeapImages(TextureArrayStruct arrayStruct) {
        for (TextureTileStruct tile : arrayStruct.getTileCoordinateMap().values())
            tile.clearImages();
        arrayStruct.clearAtlases();
    }
}