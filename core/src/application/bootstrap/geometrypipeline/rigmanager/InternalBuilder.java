package application.bootstrap.geometrypipeline.rigmanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses a rig JSON file into a RigData and wraps it in a RigHandle.
     * Bones must appear in parent-before-child order — "parent" is
     * resolved against bones already processed earlier in the same file,
     * and the first bone must be the root ("parent" omitted). Bootstrap-only.
     */

    // Build \\

    RigHandle build(File file, String rigName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        JsonArray bonesArray = JsonUtility.validateArray(json, "bones");

        if (bonesArray.size() == 0)
            throwException("Rig has no bones: " + file.getName());

        RigBoneStruct[] bones = new RigBoneStruct[bonesArray.size()];
        Object2IntOpenHashMap<String> boneName2Index = new Object2IntOpenHashMap<>();
        boneName2Index.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < bonesArray.size(); i++)
            bones[i] = parseBone(bonesArray.get(i).getAsJsonObject(), i, boneName2Index, file);

        RigData rigData = new RigData(bones, boneName2Index);

        RigHandle handle = create(RigHandle.class);
        handle.constructor(rigData);

        return handle;
    }

    // Parse \\

    private RigBoneStruct parseBone(
            JsonObject boneJson,
            int boneIndex,
            Object2IntOpenHashMap<String> boneName2Index,
            File file) {

        String name = JsonUtility.validateString(boneJson, "name");

        if (boneName2Index.containsKey(name))
            throwException("Duplicate bone name \"" + name + "\" in rig: " + file.getName());

        int parentIndex = parseParentIndex(boneJson, boneIndex, name, boneName2Index, file);
        Vector3 position = parseVector3(boneJson, "position", file, true);
        Vector3 rotation = parseVector3(boneJson, "rotation", file, false);
        Vector3 size = parseSize(boneJson, file);

        boneName2Index.put(name, boneIndex);

        return new RigBoneStruct(name, parentIndex, position, rotation, size);
    }

    private int parseParentIndex(
            JsonObject boneJson,
            int boneIndex,
            String name,
            Object2IntOpenHashMap<String> boneName2Index,
            File file) {

        boolean hasParent = boneJson.has("parent") && !boneJson.get("parent").isJsonNull();

        if (!hasParent) {

            if (boneIndex != 0)
                throwException("Only the first bone may omit \"parent\" (root). Offending bone: \""
                        + name + "\" in rig: " + file.getName());

            return EngineSetting.INDEX_NOT_FOUND;
        }

        if (boneIndex == 0)
            throwException("Root bone \"" + name + "\" must not declare a \"parent\" in rig: " + file.getName());

        String parentName = boneJson.get("parent").getAsString();
        int parentIndex = boneName2Index.getInt(parentName);

        if (parentIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Bone \"" + name + "\" references parent \"" + parentName
                    + "\" which is not yet defined. Bones must be declared parent-before-child. Rig: "
                    + file.getName());

        return parentIndex;
    }

    // Vector Parsing \\

    private Vector3 parseVector3(JsonObject json, String key, File file, boolean required) {

        if (!json.has(key) || json.get(key).isJsonNull()) {

            if (required)
                throwException("Bone missing required \"" + key + "\" in rig: " + file.getName());

            return new Vector3(0f, 0f, 0f);
        }

        JsonObject vector = json.getAsJsonObject(key);

        return new Vector3(
                vector.has("x") ? vector.get("x").getAsFloat() : 0f,
                vector.has("y") ? vector.get("y").getAsFloat() : 0f,
                vector.has("z") ? vector.get("z").getAsFloat() : 0f);
    }

    private Vector3 parseSize(JsonObject json, File file) {

        if (!json.has("size") || json.get("size").isJsonNull())
            return new Vector3(
                    EngineSetting.DEFAULT_BONE_SIZE,
                    EngineSetting.DEFAULT_BONE_SIZE,
                    EngineSetting.DEFAULT_BONE_SIZE);

        JsonObject size = json.getAsJsonObject("size");

        return new Vector3(
                size.has("x") ? size.get("x").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE,
                size.has("y") ? size.get("y").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE,
                size.has("z") ? size.get("z").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE);
    }
}