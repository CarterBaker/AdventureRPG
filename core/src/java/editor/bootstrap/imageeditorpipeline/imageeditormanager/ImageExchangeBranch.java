package editor.bootstrap.imageeditorpipeline.imageeditormanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.filemenupipeline.util.WorkspaceUtility;
import editor.bootstrap.imageeditorpipeline.imageproject.ImageProjectInstance;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.imagepipeline.util.ImageMap;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ImageExchangeBranch extends BranchPackage {

    /*
     * Moves an image between the workspace and the game, the way a texturing
     * tool exports its channels and an engine imports them. Export writes
     * every map the image holds, the albedo its layers flattened and each
     * map once generated, as one PNG apiece in the workspace's Exports
     * folder, named for the image and the map's suffix. Import takes those
     * PNGs whose suffix names a texture alias into the game's textures, as
     * the tile asked for in the pack asked for, its old images there
     * replaced, bakes the tile's streamed files from them, live where the
     * tile already plays, and clears the PNGs it took from the workspace; the
     * image's own file stays there to be worked and imported again.
     */

    // Internal
    private ImageManager imageManager;
    private TextureManager textureManager;

    // Directory
    private File exportRoot;
    private File textureRoot;
    private File bakeRoot;

    // Scratch
    private TextureCompressionScratchStruct compressionScratch;
    private ObjectArrayList<File> fileScratch;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.exportRoot = WorkspaceUtility.resolveFolder(internal.path, EditorSetting.IMAGE_EXPORT_DIRECTORY);
        this.textureRoot = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.bakeRoot = new File(EngineSetting.BAKED_TEXTURE_PATH);

        // Scratch
        this.compressionScratch = new TextureCompressionScratchStruct();
        this.fileScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.imageManager = get(ImageManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Export \\

    // Every map the image holds written as a PNG, any it wrote before cleared first; how many were written, or -1 when
    // one could not be
    int export(ImageProjectInstance project) {

        File folder = resolveExportFolder(project.getImageName());

        clearExports(folder);
        folder.mkdirs();

        int written = 0;

        for (ImageMap map : ImageMap.VALUES) {

            ImageDocumentInstance document = map == ImageMap.ALBEDO ? project.getComposite() : project.getMap(map);

            if (document == null)
                continue;

            if (!imageManager.writeImage(resolveExportFile(folder, project.getImageName(), map), document.getPixmap()))
                return EngineSetting.INDEX_NOT_FOUND;

            written++;
        }

        return written;
    }

    // Import \\

    // Whether the image has exported maps waiting to be imported
    boolean hasExports(String imageName) {

        collectExports(resolveExportFolder(imageName), fileScratch);
        return !fileScratch.isEmpty();
    }

    // Whether a pack names a folder of the game's textures
    boolean isPack(String packName) {
        return !packName.isEmpty() && new File(textureRoot, packName).isDirectory();
    }

    // Whether a name can be a tile's: a file name with no separator in it, which splits a tile's name from its alias
    boolean isTileName(String tileName) {
        return FileUtility.isValidFileName(tileName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !tileName.contains(EditorSetting.IMAGE_IMPORT_ALIAS_SEPARATOR);
    }

    // The image's exported maps a texture alias takes laid into the pack as the tile and baked, then cleared from the
    // workspace; null when it imported, else why it could not
    String importImage(String imageName, String packName, String tileName) {

        File folder = resolveExportFolder(imageName);
        File packFolder = new File(textureRoot, packName);
        AliasLibraryStruct library = textureManager.getAliasLibrary();

        collectExports(folder, fileScratch);
        clearTileSources(packFolder, tileName);

        try {
            for (int i = 0; i < fileScratch.size(); i++) {

                File file = fileScratch.get(i);
                String suffix = toSuffix(file);

                if (library.findAlias(suffix) != null)
                    Files.copy(file.toPath(), new File(packFolder, toFileName(tileName, suffix)).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            return EditorSetting.IMAGE_IMPORT_MESSAGE_NOT_COPIED + e.getMessage();
        }

        String textureName = TextureBakeUtility.toTileName(packName, tileName);
        TextureSourceStruct source = TextureBakeUtility.collectTileSource(textureRoot, textureName, library);

        if (source.getSourceCount() == 0)
            return EditorSetting.IMAGE_IMPORT_MESSAGE_NO_MAPS;

        bake(textureName, source, library);
        clearExports(folder);

        return null;
    }

    // The tile baked from its images and written where the game streams it, and shown at once
    private void bake(String textureName, TextureSourceStruct source, AliasLibraryStruct library) {

        BufferedImage[] images = new BufferedImage[source.getSourceCount()];
        AliasStruct[] aliases = new AliasStruct[source.getSourceCount()];

        for (int i = 0; i < images.length; i++) {
            images[i] = TextureBakeUtility.readImage(source.getFile(i));
            aliases[i] = source.getAlias(i);
        }

        TextureBakeStruct bake = TextureBakeUtility.bakeImages(
                textureName,
                images,
                aliases,
                library,
                TextureBakeUtility.computeChecksum(source),
                compressionScratch);

        TextureBakeUtility.writeBake(bakeRoot, bake);
        textureManager.applyTileBake(bake);
    }

    // Files \\

    private File resolveExportFolder(String imageName) {
        return new File(exportRoot, imageName);
    }

    private File resolveExportFile(File folder, String imageName, ImageMap map) {
        return new File(folder, toFileName(FileUtility.getFileName(imageName), map.getExportSuffix()));
    }

    private static String toFileName(String stem, String suffix) {
        return stem + EditorSetting.IMAGE_IMPORT_ALIAS_SEPARATOR + suffix + EditorSetting.IMAGE_EXPORT_EXTENSION;
    }

    // The suffix after an exported PNG's last separator, the map it holds
    private static String toSuffix(File file) {

        String name = FileUtility.getFileName(file);

        return name.substring(name.lastIndexOf(EditorSetting.IMAGE_IMPORT_ALIAS_SEPARATOR) + 1);
    }

    private void collectExports(File folder, ObjectArrayList<File> out) {

        out.clear();

        if (folder.isDirectory())
            out.addAll(FileUtility.collectFilesShallow(folder, EngineSetting.TEXTURE_FILE_EXTENSIONS));
    }

    // Every PNG an earlier export left in the folder removed, and the folder with them once it is empty
    private void clearExports(File folder) {

        collectExports(folder, fileScratch);

        for (int i = 0; i < fileScratch.size(); i++)
            fileScratch.get(i).delete();

        String[] left = folder.list();

        if (left != null && left.length == 0)
            folder.delete();
    }

    // The tile's images already in the pack removed, so a map the new import does not bring is not kept from before
    private void clearTileSources(File packFolder, String tileName) {

        ObjectArrayList<File> files = FileUtility.collectFilesShallow(
                packFolder,
                EngineSetting.TEXTURE_FILE_EXTENSIONS);

        for (int i = 0; i < files.size(); i++)
            if (FileUtility.splitFileNameByUnderscore(FileUtility.getFileName(files.get(i)))[0].equals(tileName))
                files.get(i).delete();
    }

    // Accessible \\

    File getExportFolder(String imageName) {
        return resolveExportFolder(imageName);
    }
}
