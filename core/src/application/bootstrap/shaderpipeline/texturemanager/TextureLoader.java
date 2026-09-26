package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;

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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TextureLoader extends LoaderPackage {

    /*
     * Discovers atlas directories and loads one per load() call: builds the
     * atlas, seeds its optional companion UBO with the aliases present, uploads
     * it, and frees the heap images.
     */

    // Internal
    private File root;
    private TextureManager textureManager;
    private UBOManager uboManager;
    private AliasLibrarySystem aliasLibrarySystem;
    private TextureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> arrayName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.arrayName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root,
                "Texture root directory not found: " + root.getAbsolutePath());

        for (File directory : FileUtility.collectAllSubdirectories(root)) {
            String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
            arrayName2File.put(arrayName, directory);
            queueFile(directory);
        }
    }

    @Override
    protected void create() {
        this.aliasLibrarySystem = create(AliasLibrarySystem.class);
        this.internalBuilder = create(TextureBuilder.class);
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

        ObjectArrayList<File> imageFiles = FileUtility.collectFilesShallow(
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

        int gpuHandle = TextureGLSLUtility.pushTextureArray(arrayStruct.getRawImageArray());
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