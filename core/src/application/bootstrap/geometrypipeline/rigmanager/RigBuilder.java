package application.bootstrap.geometrypipeline.rigmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class RigBuilder extends BuilderPackage {

    /*
     * Parses a rig ARPG file into a RigData and wraps it in a RigHandle.
     * Bones must appear in parent-before-child order — "parent" is
     * resolved against bones already processed earlier in the same file,
     * and the first bone must be the root ("parent" omitted). Bootstrap-only.
     */

    // Build \\

    RigHandle build(File file, String rigName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct bonesArray = ArpgUtility.validateArray(arpg, "bones");

        if (bonesArray.size() == 0)
            throwException("Rig has no bones: " + file.getName());

        RigBoneStruct[] bones = new RigBoneStruct[bonesArray.size()];
        Object2IntOpenHashMap<String> boneName2Index = new Object2IntOpenHashMap<>();
        boneName2Index.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < bonesArray.size(); i++)
            bones[i] = parseBone(bonesArray.get(i).getAsObject(), i, boneName2Index, file);

        RigData rigData = new RigData(bones, boneName2Index);

        RigHandle handle = create(RigHandle.class);
        handle.constructor(rigData);

        return handle;
    }

    // Parse \\

    private RigBoneStruct parseBone(
            ArpgObjectStruct boneArpg,
            int boneIndex,
            Object2IntOpenHashMap<String> boneName2Index,
            File file) {

        String name = ArpgUtility.validateString(boneArpg, "name");

        if (boneName2Index.containsKey(name))
            throwException("Duplicate bone name \"" + name + "\" in rig: " + file.getName());

        int parentIndex = parseParentIndex(boneArpg, boneIndex, name, boneName2Index, file);
        Vector3 position = parseVector3(boneArpg, "position", file, true);
        Vector3 rotation = parseVector3(boneArpg, "rotation", file, false);
        Vector3 size = parseSize(boneArpg, file);

        boneName2Index.put(name, boneIndex);

        return new RigBoneStruct(name, parentIndex, position, rotation, size);
    }

    private int parseParentIndex(
            ArpgObjectStruct boneArpg,
            int boneIndex,
            String name,
            Object2IntOpenHashMap<String> boneName2Index,
            File file) {

        boolean hasParent = boneArpg.has("parent") && !boneArpg.get("parent").isNull();

        if (!hasParent) {

            if (boneIndex != 0)
                throwException("Only the first bone may omit \"parent\" (root). Offending bone: \""
                        + name + "\" in rig: " + file.getName());

            return EngineSetting.INDEX_NOT_FOUND;
        }

        if (boneIndex == 0)
            throwException("Root bone \"" + name + "\" must not declare a \"parent\" in rig: " + file.getName());

        String parentName = boneArpg.get("parent").getAsString();
        int parentIndex = boneName2Index.getInt(parentName);

        if (parentIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Bone \"" + name + "\" references parent \"" + parentName
                    + "\" which is not yet defined. Bones must be declared parent-before-child. Rig: "
                    + file.getName());

        return parentIndex;
    }

    // Vector Parsing \\

    private Vector3 parseVector3(ArpgObjectStruct arpg, String key, File file, boolean required) {

        if (!arpg.has(key) || arpg.get(key).isNull()) {

            if (required)
                throwException("Bone missing required \"" + key + "\" in rig: " + file.getName());

            return new Vector3(0f, 0f, 0f);
        }

        ArpgObjectStruct vector = arpg.getAsObject(key);

        return new Vector3(
                vector.has("x") ? vector.get("x").getAsFloat() : 0f,
                vector.has("y") ? vector.get("y").getAsFloat() : 0f,
                vector.has("z") ? vector.get("z").getAsFloat() : 0f);
    }

    private Vector3 parseSize(ArpgObjectStruct arpg, File file) {

        if (!arpg.has("size") || arpg.get("size").isNull())
            return new Vector3(
                    EngineSetting.DEFAULT_BONE_SIZE,
                    EngineSetting.DEFAULT_BONE_SIZE,
                    EngineSetting.DEFAULT_BONE_SIZE);

        ArpgObjectStruct size = arpg.getAsObject("size");

        return new Vector3(
                size.has("x") ? size.get("x").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE,
                size.has("y") ? size.get("y").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE,
                size.has("z") ? size.get("z").getAsFloat() : EngineSetting.DEFAULT_BONE_SIZE);
    }
}