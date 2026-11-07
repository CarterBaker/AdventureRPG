package application.bootstrap.geometrypipeline.rigmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigIkChainStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class RigBuilder extends BuilderPackage {

    /*
     * Parses a rig ARPG file into a RigData and wraps it in a RigHandle, or
     * parses a rig's ARPG as it stands in memory into a RigData alone, for
     * its handle to take in place when its bind pose is edited. Bones must
     * appear in parent-before-child order — "parent" is resolved against
     * bones already processed earlier in the same rig, and the first bone
     * must be the root ("parent" omitted). Its optional "ik" handles each
     * name three "bones", root, middle and end, each the parent of the next,
     * and the "pole" their middle joint bends toward.
     */

    // Build \\

    RigHandle build(File file, String rigName) {

        RigData rigData = buildData(ArpgUtility.loadObject(file), rigName, file.getName());

        RigHandle handle = create(RigHandle.class);
        handle.constructor(rigData);

        return handle;
    }

    RigData buildData(ArpgObjectStruct arpg, String rigName, String sourceName) {

        ArpgArrayStruct bonesArray = ArpgUtility.validateArray(arpg, "bones");

        if (bonesArray.size() == 0)
            throwException("Rig has no bones: " + sourceName);

        RigBoneStruct[] bones = new RigBoneStruct[bonesArray.size()];
        Object2IntOpenHashMap<String> boneName2Index = new Object2IntOpenHashMap<>();
        boneName2Index.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < bonesArray.size(); i++)
            bones[i] = parseBone(bonesArray.get(i).getAsObject(), i, boneName2Index, sourceName);

        return new RigData(rigName, bones, boneName2Index, parseIkChains(arpg, bones, boneName2Index, sourceName));
    }

    // Parse \\

    private RigBoneStruct parseBone(
            ArpgObjectStruct boneArpg,
            int boneIndex,
            Object2IntOpenHashMap<String> boneName2Index,
            String sourceName) {

        String name = ArpgUtility.validateString(boneArpg, "name");

        if (boneName2Index.containsKey(name))
            throwException("Duplicate bone name \"" + name + "\" in rig: " + sourceName);

        int parentIndex = parseParentIndex(boneArpg, boneIndex, name, boneName2Index, sourceName);
        Vector3 position = parseVector3(boneArpg, "position", sourceName, true);
        Vector3 rotation = parseVector3(boneArpg, "rotation", sourceName, false);
        Vector3 size = parseSize(boneArpg, sourceName);

        boneName2Index.put(name, boneIndex);

        return new RigBoneStruct(name, parentIndex, position, rotation, size);
    }

    private int parseParentIndex(
            ArpgObjectStruct boneArpg,
            int boneIndex,
            String name,
            Object2IntOpenHashMap<String> boneName2Index,
            String sourceName) {

        boolean hasParent = boneArpg.has("parent") && !boneArpg.get("parent").isNull();

        if (!hasParent) {

            if (boneIndex != 0)
                throwException("Only the first bone may omit \"parent\" (root). Offending bone: \""
                        + name + "\" in rig: " + sourceName);

            return EngineSetting.INDEX_NOT_FOUND;
        }

        if (boneIndex == 0)
            throwException("Root bone \"" + name + "\" must not declare a \"parent\" in rig: " + sourceName);

        String parentName = boneArpg.get("parent").getAsString();
        int parentIndex = boneName2Index.getInt(parentName);

        if (parentIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Bone \"" + name + "\" references parent \"" + parentName
                    + "\" which is not yet defined. Bones must be declared parent-before-child. Rig: "
                    + sourceName);

        return parentIndex;
    }

    // IK \\

    private RigIkChainStruct[] parseIkChains(
            ArpgObjectStruct arpg,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            String sourceName) {

        if (!ArpgUtility.hasArray(arpg, "ik"))
            return new RigIkChainStruct[0];

        ArpgArrayStruct chainsArray = arpg.getAsArray("ik");
        RigIkChainStruct[] chains = new RigIkChainStruct[chainsArray.size()];

        for (int i = 0; i < chains.length; i++) {

            chains[i] = parseIkChain(chainsArray.get(i).getAsObject(), bones, boneName2Index, sourceName);

            for (int j = 0; j < i; j++)
                if (chains[j].getName().equals(chains[i].getName()))
                    throwException("Duplicate IK handle name \"" + chains[i].getName() + "\" in rig: " + sourceName);
        }

        return chains;
    }

    private RigIkChainStruct parseIkChain(
            ArpgObjectStruct chainArpg,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            String sourceName) {

        String name = ArpgUtility.validateString(chainArpg, "name");
        ArpgArrayStruct bonesArray = ArpgUtility.validateArray(chainArpg, "bones");

        if (bonesArray.size() != EngineSetting.RIG_IK_CHAIN_BONES)
            throwException("IK handle \"" + name + "\" must name exactly " + EngineSetting.RIG_IK_CHAIN_BONES
                    + " bones, root to end, in rig: " + sourceName);

        int[] chain = new int[EngineSetting.RIG_IK_CHAIN_BONES];

        for (int i = 0; i < chain.length; i++) {

            String boneName = bonesArray.get(i).getAsString();
            chain[i] = boneName2Index.getInt(boneName);

            if (chain[i] == EngineSetting.INDEX_NOT_FOUND)
                throwException("IK handle \"" + name + "\" names unknown bone \"" + boneName + "\" in rig: "
                        + sourceName);

            if (i > 0 && bones[chain[i]].getParentIndex() != chain[i - 1])
                throwException("IK handle \"" + name + "\" bone \"" + boneName
                        + "\" is not the child of the bone before it in rig: " + sourceName);
        }

        Vector3 pole = parseVector3(chainArpg, "pole", sourceName, true);

        if (pole.length() <= 0f)
            throwException("IK handle \"" + name + "\" has no pole direction in rig: " + sourceName);

        return new RigIkChainStruct(name, chain[0], chain[1], chain[2], pole.divide(pole.length()));
    }

    // Vector Parsing \\

    private Vector3 parseVector3(ArpgObjectStruct arpg, String key, String sourceName, boolean required) {

        if (!arpg.has(key) || arpg.get(key).isNull()) {

            if (required)
                throwException("Bone missing required \"" + key + "\" in rig: " + sourceName);

            return new Vector3(0f, 0f, 0f);
        }

        ArpgObjectStruct vector = arpg.getAsObject(key);

        return new Vector3(
                vector.has("x") ? vector.get("x").getAsFloat() : 0f,
                vector.has("y") ? vector.get("y").getAsFloat() : 0f,
                vector.has("z") ? vector.get("z").getAsFloat() : 0f);
    }

    private Vector3 parseSize(ArpgObjectStruct arpg, String sourceName) {

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