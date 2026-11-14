package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class SubVoxelPartUtility extends EngineUtility {

    /*
     * Finds and creates parts by texture. A part created for a texture is
     * named after the texture's last path segment, numbered when that name is
     * already taken, so every part of a model keeps a unique name.
     */

    // Parts \\

    static int findTexturePart(SubVoxelModelStruct model, String textureName) {

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            if (model.getPart(partIndex).getTextureName().equals(textureName))
                return partIndex;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    static int addTexturePart(SubVoxelModelStruct model, String textureName) {
        return model.addPart(new SubVoxelPartStruct(resolvePartName(model, textureName), textureName));
    }

    // Naming \\

    private static String resolvePartName(SubVoxelModelStruct model, String textureName) {

        String baseName = textureName.substring(textureName.lastIndexOf('/') + 1);
        String partName = baseName;
        int suffix = 1;

        while (hasPartName(model, partName))
            partName = baseName + (++suffix);

        return partName;
    }

    private static boolean hasPartName(SubVoxelModelStruct model, String partName) {

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            if (model.getPart(partIndex).getPartName().equals(partName))
                return true;

        return false;
    }
}
