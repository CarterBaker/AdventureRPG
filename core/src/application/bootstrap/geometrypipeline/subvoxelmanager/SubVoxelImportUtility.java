package application.bootstrap.geometrypipeline.subvoxelmanager;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SubVoxelImportUtility extends EngineUtility {

    /*
     * Converts an authored quad mesh into a sub-voxel model. A cell is filled
     * when its centre lies inside the mesh's closed surface — an odd number of
     * crossings along a slightly skewed ray — and takes the texture of the
     * first surface that ray meets, with one part per texture used.
     */

    // Layout
    private static final int FLOATS_PER_TRIANGLE = 9;

    // Detection \\

    static boolean hasQuads(JsonObject meshJson) {

        if (!meshJson.has("vbo") || !meshJson.get("vbo").isJsonArray())
            return false;

        for (JsonElement element : meshJson.getAsJsonArray("vbo"))
            if (element.isJsonObject())
                return true;

        return false;
    }

    // Import \\

    static SubVoxelModelStruct importQuads(JsonObject meshJson, String fallbackTextureName) {

        FloatArrayList triangles = new FloatArrayList();
        IntArrayList triangleTextures = new IntArrayList();
        ObjectArrayList<String> textureNames = new ObjectArrayList<>();

        collectTriangles(meshJson, fallbackTextureName, triangles, triangleTextures, textureNames);

        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int[] texture2Part = new int[textureNames.size()];
        Arrays.fill(texture2Part, EngineSetting.INDEX_NOT_FOUND);

        int resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        for (int z = 0; z < resolution; z++)
            for (int y = 0; y < resolution; y++)
                for (int x = 0; x < resolution; x++) {

                    int textureIndex = resolveInsideTexture(
                            (x + 0.5f) / resolution,
                            (y + 0.5f) / resolution,
                            (z + 0.5f) / resolution,
                            triangles,
                            triangleTextures);

                    if (textureIndex == EngineSetting.INDEX_NOT_FOUND)
                        continue;

                    if (texture2Part[textureIndex] == EngineSetting.INDEX_NOT_FOUND)
                        texture2Part[textureIndex] = SubVoxelPartUtility.addTexturePart(
                                model, textureNames.get(textureIndex));

                    model.setCell(x, y, z, texture2Part[textureIndex]);
                }

        if (model.getPartCount() == 0)
            SubVoxelPartUtility.addTexturePart(model, fallbackTextureName);

        return model;
    }

    // Triangles \\

    private static void collectTriangles(
            JsonObject meshJson,
            String fallbackTextureName,
            FloatArrayList triangles,
            IntArrayList triangleTextures,
            ObjectArrayList<String> textureNames) {

        for (JsonElement element : meshJson.getAsJsonArray("vbo")) {

            if (!element.isJsonObject() || !element.getAsJsonObject().has("quad"))
                continue;

            JsonObject quadJson = element.getAsJsonObject();
            JsonArray corners = quadJson.getAsJsonArray("quad");

            if (corners.size() != EngineSetting.QUAD_VERTEX_COUNT)
                throwException("Quad must have exactly " + EngineSetting.QUAD_VERTEX_COUNT + " corners.");

            String textureName = quadJson.has("texture") && !quadJson.get("texture").isJsonNull()
                    ? quadJson.get("texture").getAsString()
                    : fallbackTextureName;
            int textureIndex = textureNames.indexOf(textureName);

            if (textureIndex == EngineSetting.INDEX_NOT_FOUND) {
                textureNames.add(textureName);
                textureIndex = textureNames.size() - 1;
            }

            addTriangle(corners, 0, 1, 2, triangles);
            addTriangle(corners, 2, 3, 0, triangles);
            triangleTextures.add(textureIndex);
            triangleTextures.add(textureIndex);
        }
    }

    private static void addTriangle(JsonArray corners, int a, int b, int c, FloatArrayList triangles) {

        int[] order = { a, b, c };

        for (int i = 0; i < order.length; i++) {
            JsonArray corner = corners.get(order[i]).getAsJsonArray();
            triangles.add(corner.get(0).getAsFloat());
            triangles.add(corner.get(1).getAsFloat());
            triangles.add(corner.get(2).getAsFloat());
        }
    }

    // Inside Test \\

    private static int resolveInsideTexture(
            float originX,
            float originY,
            float originZ,
            FloatArrayList triangles,
            IntArrayList triangleTextures) {

        int crossings = 0;
        float nearestDistance = Float.MAX_VALUE;
        int nearestTexture = EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < triangleTextures.size(); i++) {

            float distance = intersect(originX, originY, originZ, triangles, i * FLOATS_PER_TRIANGLE);

            if (distance < 0f)
                continue;

            crossings++;

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestTexture = triangleTextures.getInt(i);
            }
        }

        return (crossings & 1) == 1 ? nearestTexture : EngineSetting.INDEX_NOT_FOUND;
    }

    private static float intersect(float originX, float originY, float originZ, FloatArrayList triangles, int base) {

        float directionX = 1f;
        float directionY = EngineSetting.SUB_VOXEL_IMPORT_RAY_Y;
        float directionZ = EngineSetting.SUB_VOXEL_IMPORT_RAY_Z;

        float ax = triangles.getFloat(base), ay = triangles.getFloat(base + 1), az = triangles.getFloat(base + 2);
        float e1x = triangles.getFloat(base + 3) - ax;
        float e1y = triangles.getFloat(base + 4) - ay;
        float e1z = triangles.getFloat(base + 5) - az;
        float e2x = triangles.getFloat(base + 6) - ax;
        float e2y = triangles.getFloat(base + 7) - ay;
        float e2z = triangles.getFloat(base + 8) - az;

        float px = directionY * e2z - directionZ * e2y;
        float py = directionZ * e2x - directionX * e2z;
        float pz = directionX * e2y - directionY * e2x;
        float determinant = e1x * px + e1y * py + e1z * pz;

        if (Math.abs(determinant) < EngineSetting.SUB_VOXEL_IMPORT_EPSILON)
            return -1f;

        float inverse = 1f / determinant;
        float tx = originX - ax, ty = originY - ay, tz = originZ - az;
        float u = (tx * px + ty * py + tz * pz) * inverse;

        if (u < 0f || u > 1f)
            return -1f;

        float qx = ty * e1z - tz * e1y;
        float qy = tz * e1x - tx * e1z;
        float qz = tx * e1y - ty * e1x;
        float v = (directionX * qx + directionY * qy + directionZ * qz) * inverse;

        if (v < 0f || u + v > 1f)
            return -1f;

        float distance = (e2x * qx + e2y * qy + e2z * qz) * inverse;
        return distance > EngineSetting.SUB_VOXEL_IMPORT_EPSILON ? distance : -1f;
    }
}
