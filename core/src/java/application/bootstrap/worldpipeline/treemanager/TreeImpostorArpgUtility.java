package application.bootstrap.worldpipeline.treemanager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32C;

import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionUtility;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

public final class TreeImpostorArpgUtility extends EngineUtility {

    /*
     * Reads and writes a species' baked impostor, one file per species under
     * the impostor directory named as the species is: its source checksum,
     * the shared tree's height and framing sphere, and its albedo and normal
     * chains. The source is the CRC32C of the bake's version and layout and of
     * the species' file as compact text, so a species whose file changed, or a
     * bake of another layout, reads as stale. A file that is missing, of
     * another layout or whose maps are not whole reads as none.
     */

    // Files \\

    public static File resolveFile(String treeName) {
        return ArpgUtility.resolveFile(new File(EngineSetting.TREE_IMPOSTOR_PATH), treeName);
    }

    // Source \\

    // The checksum an impostor baked from a species' file now carries
    public static long computeSource(ArpgObjectStruct treeArpg) {

        CRC32C crc = new CRC32C();

        crc.update(EngineSetting.TREE_IMPOSTOR_BAKE_VERSION);
        crc.update(EngineSetting.TREE_IMPOSTOR_FRAMES);
        crc.update(EngineSetting.TREE_IMPOSTOR_FRAME_TEXELS);
        crc.update(ArpgUtility.formatCompactText(treeArpg).getBytes(StandardCharsets.UTF_8));

        return crc.getValue();
    }

    // Read \\

    // A species' impostor as its file holds it, null when it has none whole
    public static TreeImpostorStruct tryRead(String treeName) {

        File file = resolveFile(treeName);

        if (!file.isFile())
            return null;

        ArpgObjectStruct impostorArpg = ArpgUtility.tryLoadObject(file);

        if (impostorArpg == null
                || !ArpgUtility.hasNumber(impostorArpg, "source")
                || !ArpgUtility.hasNumber(impostorArpg, "frames")
                || !ArpgUtility.hasNumber(impostorArpg, "frame_texels")
                || !ArpgUtility.hasBytes(impostorArpg, "albedo")
                || !ArpgUtility.hasBytes(impostorArpg, "normal"))
            return null;

        if (impostorArpg.get("frames").getAsInt() != EngineSetting.TREE_IMPOSTOR_FRAMES
                || impostorArpg.get("frame_texels").getAsInt() != EngineSetting.TREE_IMPOSTOR_FRAME_TEXELS)
            return null;

        byte[] albedo = impostorArpg.get("albedo").getAsBytes();
        byte[] normal = impostorArpg.get("normal").getAsBytes();
        int chainBytes = TextureCompressionUtility.getChainBytes(EngineSetting.TREE_IMPOSTOR_ATLAS_TEXELS);

        if (albedo.length != chainBytes || normal.length != chainBytes)
            return null;

        return new TreeImpostorStruct(
                impostorArpg.get("source").getAsLong(),
                ArpgUtility.getFloat(impostorArpg, "height", 0f),
                ArpgUtility.getFloat(impostorArpg, "center_y", 0f),
                ArpgUtility.getFloat(impostorArpg, "radius", 0f),
                albedo,
                normal);
    }

    // Write \\

    // A species' impostor written whole, false when it could not be
    public static boolean tryWrite(String treeName, TreeImpostorStruct impostor) {

        ArpgObjectStruct impostorArpg = new ArpgObjectStruct();

        impostorArpg.addProperty("source", impostor.getSource());
        impostorArpg.addProperty("frames", EngineSetting.TREE_IMPOSTOR_FRAMES);
        impostorArpg.addProperty("frame_texels", EngineSetting.TREE_IMPOSTOR_FRAME_TEXELS);
        impostorArpg.add("height", new ArpgValueStruct(impostor.getHeight()));
        impostorArpg.add("center_y", new ArpgValueStruct(impostor.getCenterY()));
        impostorArpg.add("radius", new ArpgValueStruct(impostor.getRadius()));
        impostorArpg.add("albedo", new ArpgValueStruct(impostor.getAlbedo()));
        impostorArpg.add("normal", new ArpgValueStruct(impostor.getNormal()));

        File file = resolveFile(treeName);
        File directory = file.getParentFile();

        if (directory != null && !directory.isDirectory() && !directory.mkdirs())
            return false;

        return ArpgUtility.tryWriteObject(file, impostorArpg);
    }
}
