package application.bootstrap.geometrypipeline.rigmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigData;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigIkChainStruct;
import application.bootstrap.geometrypipeline.rig.RigSpringChainStruct;
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
     * and the "pole" their middle joint bends toward. Its optional "springs"
     * each name at least two "bones", each the parent of the next, the first
     * hung from a bone the body moves, none in another chain or an IK
     * handle, with the "stiffness", "damping", "gravity" and "drag" the
     * runtime simulates them by, each within its range.
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

        RigIkChainStruct[] ikChains = parseIkChains(arpg, bones, boneName2Index, sourceName);
        RigSpringChainStruct[] springChains = parseSpringChains(arpg, bones, boneName2Index, ikChains, sourceName);

        return new RigData(rigName, bones, boneName2Index, ikChains, springChains);
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

    // Springs \\

    private RigSpringChainStruct[] parseSpringChains(
            ArpgObjectStruct arpg,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            RigIkChainStruct[] ikChains,
            String sourceName) {

        if (!ArpgUtility.hasArray(arpg, "springs"))
            return new RigSpringChainStruct[0];

        ArpgArrayStruct chainsArray = arpg.getAsArray("springs");
        RigSpringChainStruct[] chains = new RigSpringChainStruct[chainsArray.size()];
        boolean[] sprung = new boolean[bones.length];

        for (int i = 0; i < chains.length; i++) {

            chains[i] = parseSpringChain(
                    chainsArray.get(i).getAsObject(), bones, boneName2Index, ikChains, sprung, sourceName);

            for (int j = 0; j < i; j++)
                if (chains[j].getName().equals(chains[i].getName()))
                    throwException("Duplicate spring chain name \"" + chains[i].getName() + "\" in rig: "
                            + sourceName);
        }

        return chains;
    }

    private RigSpringChainStruct parseSpringChain(
            ArpgObjectStruct chainArpg,
            RigBoneStruct[] bones,
            Object2IntOpenHashMap<String> boneName2Index,
            RigIkChainStruct[] ikChains,
            boolean[] sprung,
            String sourceName) {

        String name = ArpgUtility.validateString(chainArpg, "name");
        ArpgArrayStruct bonesArray = ArpgUtility.validateArray(chainArpg, "bones");

        if (bonesArray.size() < EngineSetting.RIG_SPRING_MIN_BONES)
            throwException("Spring chain \"" + name + "\" must name at least " + EngineSetting.RIG_SPRING_MIN_BONES
                    + " bones, root to tip, in rig: " + sourceName);

        int[] chain = new int[bonesArray.size()];
        int[][] carried = new int[chain.length][];

        for (int i = 0; i < chain.length; i++) {

            String boneName = bonesArray.get(i).getAsString();
            chain[i] = boneName2Index.getInt(boneName);

            if (chain[i] == EngineSetting.INDEX_NOT_FOUND)
                throwException("Spring chain \"" + name + "\" names unknown bone \"" + boneName + "\" in rig: "
                        + sourceName);

            if (i == 0 && bones[chain[i]].isRoot())
                throwException("Spring chain \"" + name + "\" cannot start at the root bone \"" + boneName
                        + "\" — it must hang from a bone the body moves. Rig: " + sourceName);

            if (i > 0 && bones[chain[i]].getParentIndex() != chain[i - 1])
                throwException("Spring chain \"" + name + "\" bone \"" + boneName
                        + "\" is not the child of the bone before it in rig: " + sourceName);

            if (sprung[chain[i]])
                throwException("Bone \"" + boneName + "\" belongs to more than one spring chain in rig: "
                        + sourceName);

            if (isIkBone(ikChains, chain[i]))
                throwException("Spring chain \"" + name + "\" bone \"" + boneName
                        + "\" is also posed by an IK handle in rig: " + sourceName);

            sprung[chain[i]] = true;
            carried[i] = resolveCarried(bones, chain[i]);
        }

        return new RigSpringChainStruct(
                name,
                chain,
                carried,
                parseSpringValue(chainArpg, "stiffness", EngineSetting.RIG_SPRING_STIFFNESS_MAX, name, sourceName),
                parseSpringValue(chainArpg, "damping", EngineSetting.RIG_SPRING_DAMPING_MAX, name, sourceName),
                parseSpringValue(chainArpg, "gravity", EngineSetting.RIG_SPRING_GRAVITY_MAX, name, sourceName),
                parseSpringValue(chainArpg, "drag", EngineSetting.RIG_SPRING_DRAG_MAX, name, sourceName));
    }

    private float parseSpringValue(ArpgObjectStruct chainArpg, String key, float max, String name, String sourceName) {

        float value = ArpgUtility.validateFloat(chainArpg, key);

        if (value < 0f || value > max)
            throwException("Spring chain \"" + name + "\" \"" + key + "\" must lie between 0 and " + max
                    + ", found " + value + " in rig: " + sourceName);

        return value;
    }

    private boolean isIkBone(RigIkChainStruct[] ikChains, int boneIndex) {

        for (RigIkChainStruct chain : ikChains)
            if (chain.getRootBone() == boneIndex || chain.getMiddleBone() == boneIndex
                    || chain.getEndBone() == boneIndex)
                return true;

        return false;
    }

    // A bone and every bone below it, which in parent-before-child order all come after it
    private int[] resolveCarried(RigBoneStruct[] bones, int boneIndex) {

        boolean[] below = new boolean[bones.length];
        int count = 1;

        below[boneIndex] = true;

        for (int i = boneIndex + 1; i < bones.length; i++)
            if (!bones[i].isRoot() && below[bones[i].getParentIndex()]) {
                below[i] = true;
                count++;
            }

        int[] carried = new int[count];
        int next = 0;

        for (int i = boneIndex; i < bones.length; i++)
            if (below[i])
                carried[next++] = i;

        return carried;
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