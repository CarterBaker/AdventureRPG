package application.bootstrap.geometrypipeline.subvoxelmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.io.JsonUtility;

class SubVoxelJsonUtility extends EngineUtility {

    /*
     * The single definition of the sub-voxel mesh format: a mesh JSON whose
     * "subvoxels" block holds a resolution and named, textured parts, each
     * listing its cubes as integer cells. MeshBuilder parses through here and
     * editor tools read and write through here.
     */

    // Detection \\

    static boolean hasSubVoxels(JsonObject meshJson) {
        return JsonUtility.hasObject(meshJson, "subvoxels");
    }

    // Parse \\

    static SubVoxelModelStruct parse(JsonObject meshJson) {

        JsonObject subVoxelJson = JsonUtility.validateObject(meshJson, "subvoxels");
        int resolution = JsonUtility.validateInt(subVoxelJson, "resolution");

        if (resolution != EngineSetting.SUB_VOXEL_RESOLUTION)
            throwException("Sub-voxel resolution " + resolution + " does not match the engine resolution "
                    + EngineSetting.SUB_VOXEL_RESOLUTION + ".");

        JsonArray partsJson = JsonUtility.validateArray(subVoxelJson, "parts");
        SubVoxelModelStruct model = new SubVoxelModelStruct();

        for (int i = 0; i < partsJson.size(); i++)
            parsePart(model, partsJson.get(i).getAsJsonObject());

        return model;
    }

    private static void parsePart(SubVoxelModelStruct model, JsonObject partJson) {

        String partName = JsonUtility.validateString(partJson, "name");
        String textureName = JsonUtility.validateString(partJson, "texture");
        JsonArray cubesJson = JsonUtility.validateArray(partJson, "cubes");
        int partIndex = model.addPart(new SubVoxelPartStruct(partName, textureName));

        for (int i = 0; i < cubesJson.size(); i++) {

            JsonArray cubeJson = cubesJson.get(i).getAsJsonArray();

            if (cubeJson.size() != 3)
                throwException("Sub-voxel cube in part '" + partName + "' must have exactly 3 coordinates.");

            int x = cubeJson.get(0).getAsInt();
            int y = cubeJson.get(1).getAsInt();
            int z = cubeJson.get(2).getAsInt();

            if (model.isFilled(x, y, z))
                throwException("Sub-voxel cell (" + x + ", " + y + ", " + z + ") in part '" + partName
                        + "' is already owned by another cube.");

            model.setCell(x, y, z, partIndex);
        }
    }

    // Serialize \\

    static JsonObject toMeshJson(SubVoxelModelStruct model) {

        JsonObject meshJson = new JsonObject();
        meshJson.addProperty("vao", EngineSetting.SUB_VOXEL_VAO);
        meshJson.add("subvoxels", toSubVoxelJson(model));
        return meshJson;
    }

    private static JsonObject toSubVoxelJson(SubVoxelModelStruct model) {

        JsonArray partsJson = new JsonArray();

        for (int partIndex = 0; partIndex < model.getPartCount(); partIndex++)
            partsJson.add(toPartJson(model, partIndex));

        JsonObject subVoxelJson = new JsonObject();
        subVoxelJson.addProperty("resolution", EngineSetting.SUB_VOXEL_RESOLUTION);
        subVoxelJson.add("parts", partsJson);
        return subVoxelJson;
    }

    private static JsonObject toPartJson(SubVoxelModelStruct model, int partIndex) {

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        SubVoxelPartStruct part = model.getPart(partIndex);
        JsonArray cubesJson = new JsonArray();

        for (int z = 0; z < resolution; z++)
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {

                    if (model.getCellPart(x, y, z) != partIndex)
                        continue;

                    JsonArray cubeJson = new JsonArray();
                    cubeJson.add(x);
                    cubeJson.add(y);
                    cubeJson.add(z);
                    cubesJson.add(cubeJson);
                }

        JsonObject partJson = new JsonObject();
        partJson.addProperty("name", part.getPartName());
        partJson.addProperty("texture", part.getTextureName());
        partJson.add("cubes", cubesJson);
        return partJson;
    }
}
