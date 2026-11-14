package lwjgl3;

import java.io.File;

import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class Lwjgl3BakeUtility extends EngineUtility {

    /*
     * The command behind the texture bake tool. Every tile under the texture
     * root is checked against the source checksum its definition holds and
     * baked again through TextureBakeUtility when they differ; every file it
     * writes is read back before it counts. Baked files no current tile owns
     * are removed, so the baked folder always mirrors the sources.
     */

    // Bake \\

    static boolean bake(File assetRoot, boolean force) {

        File bakeRoot = new File(assetRoot, EngineSetting.BAKED_TEXTURE_PATH);
        AliasLibraryStruct library = TextureBakeUtility.loadAliasLibrary(
                new File(assetRoot, EngineSetting.BLOCK_TEXTURE_ALIAS_PATH));
        ObjectArrayList<TextureSourceStruct> sources = TextureBakeUtility.collectSources(
                new File(assetRoot, EngineSetting.BLOCK_TEXTURE_PATH), library);
        TextureCompressionScratchStruct scratch = new TextureCompressionScratchStruct();
        ObjectOpenHashSet<File> keptFiles = new ObjectOpenHashSet<>();
        boolean passed = true;
        int baked = 0;

        for (int i = 0; i < sources.size(); i++) {

            TextureSourceStruct source = sources.get(i);

            try {

                long checksum = TextureBakeUtility.computeChecksum(source);

                if (!force && TextureBakeUtility.isBaked(bakeRoot, source.getTileName(), checksum)) {
                    keepBakedFiles(bakeRoot, source.getTileName(), keptFiles);
                    continue;
                }

                TextureBakeStruct bake = TextureBakeUtility.bakeSource(source, library, checksum, scratch);
                ObjectArrayList<File> written = TextureBakeUtility.writeBake(bakeRoot, bake);

                for (int f = 0; f < written.size(); f++)
                    keptFiles.add(written.get(f).getAbsoluteFile());

                System.out.println("baked " + source.getTileName() + " at " + bake.getSize());
                baked++;
            } catch (InternalException e) {
                System.err.println("FAILED " + source.getTileName() + ": " + e.getMessage());
                keepBakedFiles(bakeRoot, source.getTileName(), keptFiles);
                passed = false;
            }
        }

        int removed = removeOrphans(bakeRoot, keptFiles);

        System.out.println("baked " + baked + ", unchanged " + (sources.size() - baked) + ", removed " + removed
                + " of " + sources.size() + " tile(s)");
        return passed;
    }

    private static void keepBakedFiles(File bakeRoot, String tileName, ObjectOpenHashSet<File> keptFiles) {

        keptFiles.add(TextureBakeUtility.resolveDefinitionFile(bakeRoot, tileName).getAbsoluteFile());

        for (int tier = 0; tier < EngineSetting.TEXTURE_TIER_COUNT; tier++)
            keptFiles.add(TextureBakeUtility.resolveTierFile(bakeRoot, tileName, tier).getAbsoluteFile());
    }

    // Every baked file no current tile owns, deleted along with any folder it leaves empty
    private static int removeOrphans(File bakeRoot, ObjectOpenHashSet<File> keptFiles) {

        if (!bakeRoot.isDirectory())
            return 0;

        ObjectArrayList<File> files = FileUtility.collectFiles(bakeRoot, EngineSetting.ARPG_FILE_EXTENSIONS);
        int removed = 0;

        for (int i = 0; i < files.size(); i++) {

            File file = files.get(i).getAbsoluteFile();

            if (keptFiles.contains(file) || !file.delete())
                continue;

            System.out.println("removed " + file.getPath());
            removed++;

            for (File parent = file.getParentFile(); !parent.equals(bakeRoot.getAbsoluteFile())
                    && parent.list() != null && parent.list().length == 0; parent = parent.getParentFile())
                parent.delete();
        }

        return removed;
    }
}
