package application.bootstrap.entitypipeline.animationtreemanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.Arrays;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.animationtree.AnimationBlendMode;
import application.bootstrap.entitypipeline.animationtree.AnimationLayerStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationNodeStruct;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeData;
import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class AnimationTreeBuilder extends BuilderPackage {

    /*
     * Parses one animation tree JSON file into an AnimationTreeData and
     * wraps it in an AnimationTreeHandle. Every name in the file — rig,
     * clips, bones, movement states, parameters, and blend modes — is
     * resolved and validated here, so the runtime only ever walks indices.
     * The first layer is the base: a full-body, unmasked OVERRIDE layer at
     * full weight that must reach a node from every EntityState, through
     * "states" or its "default". A node's blend falls back to its layer's,
     * and a layer's to the engine default. Bootstrap-only.
     */

    // Internal
    private RigManager rigManager;
    private AnimationManager animationManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.rigManager = get(RigManager.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Build \\

    AnimationTreeHandle build(File file, String treeName) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        RigHandle rigHandle = rigManager.getRigHandleFromRigName(JsonUtility.validateString(json, "rig"));
        JsonArray layersJson = JsonUtility.validateArray(json, "layers");

        if (layersJson.size() == 0)
            throwException("Animation tree \"" + treeName + "\" declares no layers. File: " + file.getName());

        AnimationLayerStruct[] layers = new AnimationLayerStruct[layersJson.size()];

        for (int i = 0; i < layers.length; i++)
            layers[i] = parseLayer(layersJson.get(i).getAsJsonObject(), i == 0, rigHandle, file);

        AnimationTreeData animationTreeData = new AnimationTreeData(treeName, rigHandle, layers);

        AnimationTreeHandle handle = create(AnimationTreeHandle.class);
        handle.constructor(animationTreeData);

        return handle;
    }

    // Layer Parsing \\

    private AnimationLayerStruct parseLayer(JsonObject layerJson, boolean base, RigHandle rigHandle, File file) {

        String layerName = JsonUtility.validateString(layerJson, "name");
        AnimationBlendMode blendMode = JsonUtility.hasString(layerJson, "mode")
                ? parseEnum(AnimationBlendMode.class, layerJson.get("mode").getAsString(), layerName, file)
                : AnimationBlendMode.OVERRIDE;
        float weight = JsonUtility.getFloat(layerJson, "weight", EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT);
        float blendDuration = JsonUtility.getFloat(layerJson, "blend", EngineSetting.ANIMATION_BLEND_SECONDS);

        if (weight < 0f || weight > 1f)
            throwException("Layer \"" + layerName + "\" weight must be between 0 and 1. File: " + file.getName());

        if (blendDuration < 0f)
            throwException("Layer \"" + layerName + "\" has a negative \"blend\". File: " + file.getName());

        if (base)
            validateBaseLayer(layerJson, layerName, blendMode, weight, file);

        JsonObject nodesJson = JsonUtility.validateObject(layerJson, "nodes");

        if (nodesJson.size() == 0)
            throwException("Layer \"" + layerName + "\" declares no nodes. File: " + file.getName());

        AnimationNodeStruct[] nodes = new AnimationNodeStruct[nodesJson.size()];
        Object2IntOpenHashMap<String> nodeName2NodeIndex = new Object2IntOpenHashMap<>();
        nodeName2NodeIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (String nodeName : nodesJson.keySet()) {

            int nodeIndex = nodeName2NodeIndex.size();

            nodes[nodeIndex] = parseNode(
                    nodeName,
                    JsonUtility.validateObject(nodesJson, nodeName),
                    blendDuration,
                    rigHandle,
                    file);
            nodeName2NodeIndex.put(nodeName, nodeIndex);
        }

        return new AnimationLayerStruct(
                layerName,
                blendMode,
                weight,
                blendDuration,
                parseBoneMask(layerJson, layerName, rigHandle, file),
                nodes,
                parseStateNodes(layerJson, layerName, nodeName2NodeIndex, base, file),
                parseTransitionBlends(layerJson, layerName, nodes, nodeName2NodeIndex, file));
    }

    private void validateBaseLayer(
            JsonObject layerJson,
            String layerName,
            AnimationBlendMode blendMode,
            float weight,
            File file) {

        if (blendMode != AnimationBlendMode.OVERRIDE)
            throwException("Base layer \"" + layerName + "\" must use the override mode. File: " + file.getName());

        if (layerJson.has("mask"))
            throwException("Base layer \"" + layerName + "\" cannot declare a \"mask\". File: " + file.getName());

        if (weight != EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT)
            throwException("Base layer \"" + layerName + "\" must play at full weight. File: " + file.getName());
    }

    // Mask Parsing \\

    private float[] parseBoneMask(JsonObject layerJson, String layerName, RigHandle rigHandle, File file) {

        float[] boneMask = new float[rigHandle.getBoneCount()];

        if (!JsonUtility.hasObject(layerJson, "mask")) {
            Arrays.fill(boneMask, EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT);
            return boneMask;
        }

        JsonObject maskJson = layerJson.getAsJsonObject("mask");

        if (maskJson.size() == 0)
            throwException("Layer \"" + layerName + "\" declares an empty \"mask\". File: " + file.getName());

        for (String boneName : maskJson.keySet()) {

            if (!rigHandle.hasBone(boneName))
                throwException("Layer \"" + layerName + "\" masks unknown bone \"" + boneName
                        + "\". File: " + file.getName());

            float boneWeight = maskJson.get(boneName).getAsFloat();

            if (boneWeight < 0f || boneWeight > 1f)
                throwException("Layer \"" + layerName + "\" mask weight for bone \"" + boneName
                        + "\" must be between 0 and 1. File: " + file.getName());

            boneMask[rigHandle.getBoneIndex(boneName)] = boneWeight;
        }

        return boneMask;
    }

    // Node Parsing \\

    private AnimationNodeStruct parseNode(
            String nodeName,
            JsonObject nodeJson,
            float layerBlendDuration,
            RigHandle rigHandle,
            File file) {

        boolean single = JsonUtility.hasString(nodeJson, "clip");
        JsonArray clipsJson = single ? null : JsonUtility.validateArray(nodeJson, "clips");
        int clipCount = single ? 1 : clipsJson.size();

        if (clipCount == 0)
            throwException("Node \"" + nodeName + "\" lists no clips. File: " + file.getName());

        AnimationParameter parameter = JsonUtility.hasString(nodeJson, "parameter")
                ? parseEnum(AnimationParameter.class, nodeJson.get("parameter").getAsString(), nodeName, file)
                : null;

        if (!single && parameter == null)
            throwException("Node \"" + nodeName + "\" blends several clips but names no \"parameter\". File: "
                    + file.getName());

        AnimationClipHandle[] clips = new AnimationClipHandle[clipCount];
        float[] values = new float[clipCount];

        if (single)
            clips[0] = resolveClip(nodeJson.get("clip").getAsString(), nodeName, rigHandle, file);
        else
            parseBlendSpace(clipsJson, clips, values, nodeName, rigHandle, file);

        boolean looping = resolveLooping(clips, nodeName, file);
        float rate = JsonUtility.getFloat(nodeJson, "rate", EngineSetting.DEFAULT_ANIMATION_NODE_RATE);
        boolean rateScale = JsonUtility.getBoolean(nodeJson, "rate_scale", false);
        float blendDuration = JsonUtility.getFloat(nodeJson, "blend", layerBlendDuration);
        float delay = JsonUtility.getFloat(nodeJson, "delay", EngineSetting.DEFAULT_ANIMATION_NODE_DELAY);

        if (rate <= 0f)
            throwException("Node \"" + nodeName + "\" must have a positive \"rate\". File: " + file.getName());

        if (rateScale && parameter == null)
            throwException("Node \"" + nodeName + "\" uses \"rate_scale\" without a \"parameter\". File: "
                    + file.getName());

        if (blendDuration < 0f || delay < 0f)
            throwException("Node \"" + nodeName + "\" has a negative \"blend\" or \"delay\". File: "
                    + file.getName());

        return new AnimationNodeStruct(
                nodeName,
                parameter,
                clips,
                values,
                looping,
                rate,
                rateScale,
                blendDuration,
                delay);
    }

    private void parseBlendSpace(
            JsonArray clipsJson,
            AnimationClipHandle[] clips,
            float[] values,
            String nodeName,
            RigHandle rigHandle,
            File file) {

        for (int i = 0; i < clips.length; i++) {

            JsonObject entryJson = clipsJson.get(i).getAsJsonObject();

            clips[i] = resolveClip(JsonUtility.validateString(entryJson, "clip"), nodeName, rigHandle, file);
            values[i] = JsonUtility.validateFloat(entryJson, "value");

            if (i > 0 && values[i] <= values[i - 1])
                throwException("Node \"" + nodeName + "\" clip " + i
                        + " is out of order — values must be strictly increasing. File: " + file.getName());
        }
    }

    private AnimationClipHandle resolveClip(String clipName, String nodeName, RigHandle rigHandle, File file) {

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (clip.getRigHandle() != rigHandle)
            throwException("Node \"" + nodeName + "\" clip \"" + clipName
                    + "\" targets a different rig than its tree. File: " + file.getName());

        return clip;
    }

    private boolean resolveLooping(AnimationClipHandle[] clips, String nodeName, File file) {

        boolean looping = clips[0].isLooping();

        for (AnimationClipHandle clip : clips)
            if (clip.isLooping() != looping)
                throwException("Node \"" + nodeName + "\" mixes looping and one-shot clips. File: "
                        + file.getName());

        return looping;
    }

    // State Parsing \\

    private int[] parseStateNodes(
            JsonObject layerJson,
            String layerName,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            boolean base,
            File file) {

        int[] stateNodes = new int[EntityState.VALUES.length];
        int defaultNode = JsonUtility.hasString(layerJson, "default")
                ? resolveNodeIndex(layerJson.get("default").getAsString(), layerName, nodeName2NodeIndex, file)
                : EngineSetting.INDEX_NOT_FOUND;

        Arrays.fill(stateNodes, defaultNode);

        if (JsonUtility.hasObject(layerJson, "states")) {

            JsonObject statesJson = layerJson.getAsJsonObject("states");

            for (String stateName : statesJson.keySet()) {

                EntityState state = parseEnum(EntityState.class, stateName, layerName, file);

                stateNodes[state.ordinal()] = resolveNodeIndex(
                        statesJson.get(stateName).getAsString(),
                        layerName,
                        nodeName2NodeIndex,
                        file);
            }
        }

        if (base)
            for (EntityState state : EntityState.VALUES)
                if (stateNodes[state.ordinal()] == EngineSetting.INDEX_NOT_FOUND)
                    throwException("Base layer \"" + layerName + "\" has no node for state \""
                            + state.name().toLowerCase() + "\" and no \"default\". File: " + file.getName());

        return stateNodes;
    }

    // Transition Parsing \\

    private float[][] parseTransitionBlends(
            JsonObject layerJson,
            String layerName,
            AnimationNodeStruct[] nodes,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            File file) {

        float[][] transitionBlends = new float[nodes.length][nodes.length];

        for (int from = 0; from < nodes.length; from++)
            for (int to = 0; to < nodes.length; to++)
                transitionBlends[from][to] = nodes[to].getBlendDuration();

        if (!JsonUtility.hasArray(layerJson, "transitions"))
            return transitionBlends;

        JsonArray transitionsJson = layerJson.getAsJsonArray("transitions");

        for (int i = 0; i < transitionsJson.size(); i++) {

            JsonObject transitionJson = transitionsJson.get(i).getAsJsonObject();
            int from = resolveNodeIndex(
                    JsonUtility.validateString(transitionJson, "from"), layerName, nodeName2NodeIndex, file);
            int to = resolveNodeIndex(
                    JsonUtility.validateString(transitionJson, "to"), layerName, nodeName2NodeIndex, file);
            float blendDuration = JsonUtility.validateFloat(transitionJson, "blend");

            if (blendDuration < 0f)
                throwException("Layer \"" + layerName + "\" transition " + i
                        + " has a negative \"blend\". File: " + file.getName());

            transitionBlends[from][to] = blendDuration;
        }

        return transitionBlends;
    }

    // Utility \\

    private int resolveNodeIndex(
            String nodeName,
            String layerName,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            File file) {

        int nodeIndex = nodeName2NodeIndex.getInt(nodeName);

        if (nodeIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Layer \"" + layerName + "\" references unknown node \"" + nodeName
                    + "\". File: " + file.getName());

        return nodeIndex;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String name, String owner, File file) {

        for (E constant : enumClass.getEnumConstants())
            if (constant.name().equalsIgnoreCase(name))
                return constant;

        return throwException("\"" + owner + "\" names unknown " + enumClass.getSimpleName() + " \"" + name
                + "\". File: " + file.getName());
    }
}
