package application.bootstrap.geometrypipeline.subvoxelmanager;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class SubVoxelArpgUtility extends EngineUtility {

    /*
     * The single definition of the sub-voxel mesh format: a mesh ARPG file whose
     * "subvoxels" block holds a resolution and named, textured parts, each
     * listing its cubes as integer cells. MeshBuilder parses through here and
     * editor tools read and write through here.
     */

    // Detection \\

    static boolean hasSubVoxels(ArpgObjectStruct meshArpg) {
        return ArpgUtility.hasObject(meshArpg, "subvoxels");
    }

    // Parse \\

    static SubVoxelModelStruct parse(ArpgObjectStruct meshArpg) {

        ArpgObjectStruct subVoxelArpg = ArpgUtility.validateObject(meshArpg, "subvoxels");
        int resolution = ArpgUtility.validateInt(subVoxelArpg, "resolution");

        if (resolution != EngineSetting.SUB_VOXEL_RESOLUTION)
            throwException("Sub-voxel resolution " + resolution + " does not match the engine resolution "
                    + EngineSetting.SUB_VOXEL_RESOLUTION + ".");

        ArpgArrayStruct partsArpg = ArpgUtility.validateArray(subVoxelArpg, "parts");
        SubVoxelModelStruct model = new SubVoxelModelStruct();

        for (int i = 0; i < partsArpg.size(); i++)
            parsePart(model, partsArpg.get(i).getAsObject());

        return model;
    }

    private static void parsePart(SubVoxelModelStruct model, ArpgObjectStruct partArpg) {

        String partName = ArpgUtility.validateString(partArpg, "name");
        String textureName = ArpgUtility.validateString(partArpg, "texture");
        ArpgArrayStruct cubesArpg = ArpgUtility.validateArray(partArpg, "cubes");
        int partIndex = model.addPart(new SubVoxelPartStruct(partName, textureName));

        for (int i = 0; i < cubesArpg.size(); i++) {

            ArpgArrayStruct cubeArpg = cubesArpg.get(i).getAsArray();

            if (cubeArpg.size() != 3)
                throwException("Sub-voxel cube in part '" + partName + "' must have exactly 3 coordinates.");

            int x = cubeArpg.get(0).getAsInt();
            int y = cubeArpg.get(1).getAsInt();
            int z = cubeArpg.get(2).getAsInt();

            if (model.isFilled(x, y, z))
                throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") in part '" + partName
                        + "' is already owned by another cube.");

            model.setCell(x, y, z, partIndex);
        }
    }

    // Serialize \\

    static ArpgObjectStruct toMeshArpg(SubVoxelModelStruct model) {

        ArpgObjectStruct meshArpg = new ArpgObjectStruct();
        meshArpg.addProperty("vao", EngineSetting.SUB_VOXEL_VAO);
        meshArpg.add("subvoxels", toSubVoxelArpg(model));
        return meshArpg;
    }

    private static ArpgObjectStruct toSubVoxelArpg(SubVoxelModelStruct model) {

        ArpgArrayStruct partsArpg = new ArpgArrayStruct();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            partsArpg.add(toPartArpg(model, partIndex));

        ArpgObjectStruct subVoxelArpg = new ArpgObjectStruct();
        subVoxelArpg.addProperty("resolution", EngineSetting.SUB_VOXEL_RESOLUTION);
        subVoxelArpg.add("parts", partsArpg);
        return subVoxelArpg;
    }

    private static ArpgObjectStruct toPartArpg(SubVoxelModelStruct model, int partIndex) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        SubVoxelPartStruct part = model.getPart(partIndex);
        ArpgArrayStruct cubesArpg = new ArpgArrayStruct();

        for (int z = 0; z < resolution; z++)
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {

                    if (model.getCellPart(x, y, z) != partIndex)
                        continue;

                    ArpgArrayStruct cubeArpg = new ArpgArrayStruct();
                    cubeArpg.add(x);
                    cubeArpg.add(y);
                    cubeArpg.add(z);
                    cubesArpg.add(cubeArpg);
                }

        ArpgObjectStruct partArpg = new ArpgObjectStruct();
        partArpg.addProperty("name", part.getPartName());
        partArpg.addProperty("texture", part.getTextureName());
        partArpg.add("cubes", cubesArpg);
        return partArpg;
    }
}
