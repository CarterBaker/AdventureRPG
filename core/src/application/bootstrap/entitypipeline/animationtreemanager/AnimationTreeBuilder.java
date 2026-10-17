package application.bootstrap.entitypipeline.animationtreemanager;

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
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

class AnimationTreeBuilder extends BuilderPackage {

    /*
     * Parses one animation tree ARPG file into an AnimationTreeData and
     * wraps it in an AnimationTreeHandle. Every name in the file — rig,
     * clips, bones, movement states, parameters, and blend modes — is
     * resolved and validated here, so the runtime only ever walks indices.
     * The first layer is the base: a full-body, unmasked OVERRIDE layer at
     * full weight that must reach a node from every EntityState, through
     * "states" or its "default". Any later layer may map EntityActions to
     * one-shot nodes through "actions"; such a node plays at the action's own
     * progress while the action lasts. A node's blend falls back to its
     * layer's, and a layer's to the engine default. Bootstrap-only, apart
     * from the unregistered preview builds an editor makes of a tree it is
     * changing.
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
        return build(ArpgUtility.loadObject(file), treeName, file.getName());
    }

    AnimationTreeHandle build(ArpgObjectStruct arpg, String treeName, String sourceName) {

        RigHandle rigHandle = rigManager.getRigHandleFromRigName(ArpgUtility.validateString(arpg, "rig"));
        ArpgArrayStruct layersArpg = ArpgUtility.validateArray(arpg, "layers");

        if (layersArpg.size() == 0)
            throwException("Animation tree \"" + treeName + "\" declares no layers. Source: " + sourceName);

        AnimationLayerStruct[] layers = new AnimationLayerStruct[layersArpg.size()];

        for (int i = 0; i < layers.length; i++)
            layers[i] = parseLayer(layersArpg.get(i).getAsObject(), i == 0, rigHandle, sourceName);

        AnimationTreeData animationTreeData = new AnimationTreeData(treeName, rigHandle, layers);

        AnimationTreeHandle handle = create(AnimationTreeHandle.class);
        handle.constructor(animationTreeData);

        return handle;
    }

    // Layer Parsing \\

    private AnimationLayerStruct parseLayer(
            ArpgObjectStruct layerArpg,
            boolean base,
            RigHandle rigHandle,
            String sourceName) {

        String layerName = ArpgUtility.validateString(layerArpg, "name");
        AnimationBlendMode blendMode = ArpgUtility.hasString(layerArpg, "mode")
                ? parseEnum(AnimationBlendMode.class, layerArpg.get("mode").getAsString(), layerName, sourceName)
                : AnimationBlendMode.OVERRIDE;
        float weight = ArpgUtility.getFloat(layerArpg, "weight", EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT);
        float blendDuration = ArpgUtility.getFloat(layerArpg, "blend", EngineSetting.ANIMATION_BLEND_SECONDS);

        if (weight < 0f || weight > 1f)
            throwException("Layer \"" + layerName + "\" weight must be between 0 and 1. Source: " + sourceName);

        if (blendDuration < 0f)
            throwException("Layer \"" + layerName + "\" has a negative \"blend\". Source: " + sourceName);

        if (base)
            validateBaseLayer(layerArpg, layerName, blendMode, weight, sourceName);

        ArpgObjectStruct nodesArpg = ArpgUtility.validateObject(layerArpg, "nodes");

        if (nodesArpg.size() == 0)
            throwException("Layer \"" + layerName + "\" declares no nodes. Source: " + sourceName);

        AnimationNodeStruct[] nodes = new AnimationNodeStruct[nodesArpg.size()];
        Object2IntOpenHashMap<String> nodeName2NodeIndex = new Object2IntOpenHashMap<>();
        nodeName2NodeIndex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (String nodeName : nodesArpg.keySet()) {

            int nodeIndex = nodeName2NodeIndex.size();

            nodes[nodeIndex] = parseNode(
                    nodeName,
                    ArpgUtility.validateObject(nodesArpg, nodeName),
                    blendDuration,
                    rigHandle,
                    sourceName);
            nodeName2NodeIndex.put(nodeName, nodeIndex);
        }

        return new AnimationLayerStruct(
                layerName,
                blendMode,
                weight,
                blendDuration,
                parseBoneMask(layerArpg, layerName, rigHandle, sourceName),
                nodes,
                parseStateNodes(layerArpg, layerName, nodeName2NodeIndex, base, sourceName),
                parseActionNodes(layerArpg, layerName, nodes, nodeName2NodeIndex, sourceName),
                parseTransitionBlends(layerArpg, layerName, nodes, nodeName2NodeIndex, sourceName));
    }

    private void validateBaseLayer(
            ArpgObjectStruct layerArpg,
            String layerName,
            AnimationBlendMode blendMode,
            float weight,
            String sourceName) {

        if (blendMode != AnimationBlendMode.OVERRIDE)
            throwException("Base layer \"" + layerName + "\" must use the override mode. Source: " + sourceName);

        if (layerArpg.has("mask"))
            throwException("Base layer \"" + layerName + "\" cannot declare a \"mask\". Source: " + sourceName);

        if (weight != EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT)
            throwException("Base layer \"" + layerName + "\" must play at full weight. Source: " + sourceName);

        if (layerArpg.has("actions"))
            throwException("Base layer \"" + layerName + "\" cannot declare \"actions\" — play them on a layer "
                    + "above it. Source: " + sourceName);
    }

    // Mask Parsing \\

    private float[] parseBoneMask(
            ArpgObjectStruct layerArpg,
            String layerName,
            RigHandle rigHandle,
            String sourceName) {

        float[] boneMask = new float[rigHandle.getBoneCount()];

        if (!ArpgUtility.hasObject(layerArpg, "mask")) {
            Arrays.fill(boneMask, EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT);
            return boneMask;
        }

        ArpgObjectStruct maskArpg = layerArpg.getAsObject("mask");

        if (maskArpg.size() == 0)
            throwException("Layer \"" + layerName + "\" declares an empty \"mask\". Source: " + sourceName);

        for (String boneName : maskArpg.keySet()) {

            if (!rigHandle.hasBone(boneName))
                throwException("Layer \"" + layerName + "\" masks unknown bone \"" + boneName
                        + "\". Source: " + sourceName);

            float boneWeight = maskArpg.get(boneName).getAsFloat();

            if (boneWeight < 0f || boneWeight > 1f)
                throwException("Layer \"" + layerName + "\" mask weight for bone \"" + boneName
                        + "\" must be between 0 and 1. Source: " + sourceName);

            boneMask[rigHandle.getBoneIndex(boneName)] = boneWeight;
        }

        return boneMask;
    }

    // Node Parsing \\

    private AnimationNodeStruct parseNode(
            String nodeName,
            ArpgObjectStruct nodeArpg,
            float layerBlendDuration,
            RigHandle rigHandle,
            String sourceName) {

        boolean single = ArpgUtility.hasString(nodeArpg, "clip");
        ArpgArrayStruct clipsArpg = single ? null : ArpgUtility.validateArray(nodeArpg, "clips");
        int clipCount = single ? 1 : clipsArpg.size();

        if (clipCount == 0)
            throwException("Node \"" + nodeName + "\" lists no clips. Source: " + sourceName);

        AnimationParameter parameter = ArpgUtility.hasString(nodeArpg, "parameter")
                ? parseEnum(AnimationParameter.class, nodeArpg.get("parameter").getAsString(), nodeName, sourceName)
                : null;

        if (!single && parameter == null)
            throwException("Node \"" + nodeName + "\" blends several clips but names no \"parameter\". Source: "
                    + sourceName);

        AnimationClipHandle[] clips = new AnimationClipHandle[clipCount];
        float[] values = new float[clipCount];

        if (single)
            clips[0] = resolveClip(nodeArpg.get("clip").getAsString(), nodeName, rigHandle, sourceName);
        else
            parseBlendSpace(clipsArpg, clips, values, nodeName, rigHandle, sourceName);

        boolean looping = resolveLooping(clips, nodeName, sourceName);
        float rate = ArpgUtility.getFloat(nodeArpg, "rate", EngineSetting.DEFAULT_ANIMATION_NODE_RATE);
        boolean rateScale = ArpgUtility.getBoolean(nodeArpg, "rate_scale", false);
        float blendDuration = ArpgUtility.getFloat(nodeArpg, "blend", layerBlendDuration);
        float delay = ArpgUtility.getFloat(nodeArpg, "delay", EngineSetting.DEFAULT_ANIMATION_NODE_DELAY);

        if (rate <= 0f)
            throwException("Node \"" + nodeName + "\" must have a positive \"rate\". Source: " + sourceName);

        if (rateScale && parameter == null)
            throwException("Node \"" + nodeName + "\" uses \"rate_scale\" without a \"parameter\". Source: "
                    + sourceName);

        if (blendDuration < 0f || delay < 0f)
            throwException("Node \"" + nodeName + "\" has a negative \"blend\" or \"delay\". Source: "
                    + sourceName);

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
            ArpgArrayStruct clipsArpg,
            AnimationClipHandle[] clips,
            float[] values,
            String nodeName,
            RigHandle rigHandle,
            String sourceName) {

        for (int i = 0; i < clips.length; i++) {

            ArpgObjectStruct entryArpg = clipsArpg.get(i).getAsObject();

            clips[i] = resolveClip(ArpgUtility.validateString(entryArpg, "clip"), nodeName, rigHandle, sourceName);
            values[i] = ArpgUtility.validateFloat(entryArpg, "value");

            if (i > 0 && values[i] <= values[i - 1])
                throwException("Node \"" + nodeName + "\" clip " + i
                        + " is out of order — values must be strictly increasing. Source: " + sourceName);
        }
    }

    private AnimationClipHandle resolveClip(String clipName, String nodeName, RigHandle rigHandle, String sourceName) {

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (clip.getRigHandle() != rigHandle)
            throwException("Node \"" + nodeName + "\" clip \"" + clipName
                    + "\" targets a different rig than its tree. Source: " + sourceName);

        return clip;
    }

    private boolean resolveLooping(AnimationClipHandle[] clips, String nodeName, String sourceName) {

        boolean looping = clips[0].isLooping();

        for (AnimationClipHandle clip : clips)
            if (clip.isLooping() != looping)
                throwException("Node \"" + nodeName + "\" mixes looping and one-shot clips. Source: "
                        + sourceName);

        return looping;
    }

    // State Parsing \\

    private int[] parseStateNodes(
            ArpgObjectStruct layerArpg,
            String layerName,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            boolean base,
            String sourceName) {

        int[] stateNodes = new int[EntityState.VALUES.length];
        int defaultNode = ArpgUtility.hasString(layerArpg, "default")
                ? resolveNodeIndex(layerArpg.get("default").getAsString(), layerName, nodeName2NodeIndex, sourceName)
                : EngineSetting.INDEX_NOT_FOUND;

        Arrays.fill(stateNodes, defaultNode);

        if (ArpgUtility.hasObject(layerArpg, "states")) {

            ArpgObjectStruct statesArpg = layerArpg.getAsObject("states");

            for (String stateName : statesArpg.keySet()) {

                EntityState state = parseEnum(EntityState.class, stateName, layerName, sourceName);

                stateNodes[state.ordinal()] = resolveNodeIndex(
                        statesArpg.get(stateName).getAsString(),
                        layerName,
                        nodeName2NodeIndex,
                        sourceName);
            }
        }

        if (base)
            for (EntityState state : EntityState.VALUES)
                if (stateNodes[state.ordinal()] == EngineSetting.INDEX_NOT_FOUND)
                    throwException("Base layer \"" + layerName + "\" has no node for state \""
                            + state.name().toLowerCase() + "\" and no \"default\". Source: " + sourceName);

        return stateNodes;
    }

    // Action Parsing \\

    private int[] parseActionNodes(
            ArpgObjectStruct layerArpg,
            String layerName,
            AnimationNodeStruct[] nodes,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            String sourceName) {

        int[] actionNodes = new int[EntityAction.VALUES.length];
        Arrays.fill(actionNodes, EngineSetting.INDEX_NOT_FOUND);

        if (!ArpgUtility.hasObject(layerArpg, "actions"))
            return actionNodes;

        ArpgObjectStruct actionsArpg = layerArpg.getAsObject("actions");

        for (String actionName : actionsArpg.keySet()) {

            EntityAction action = parseEnum(EntityAction.class, actionName, layerName, sourceName);

            if (action == EntityAction.NONE)
                throwException("Layer \"" + layerName + "\" maps the \"none\" action — a layer falls back to "
                        + "its states when no action plays. Source: " + sourceName);

            int nodeIndex = resolveNodeIndex(
                    actionsArpg.get(actionName).getAsString(),
                    layerName,
                    nodeName2NodeIndex,
                    sourceName);

            if (nodes[nodeIndex].isLooping())
                throwException("Layer \"" + layerName + "\" plays action \"" + actionName
                        + "\" with a looping node — an action plays once. Source: " + sourceName);

            actionNodes[action.ordinal()] = nodeIndex;
        }

        return actionNodes;
    }

    // Transition Parsing \\

    private float[][] parseTransitionBlends(
            ArpgObjectStruct layerArpg,
            String layerName,
            AnimationNodeStruct[] nodes,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            String sourceName) {

        float[][] transitionBlends = new float[nodes.length][nodes.length];

        for (int from = 0; from < nodes.length; from++)
            for (int to = 0; to < nodes.length; to++)
                transitionBlends[from][to] = nodes[to].getBlendDuration();

        if (!ArpgUtility.hasArray(layerArpg, "transitions"))
            return transitionBlends;

        ArpgArrayStruct transitionsArpg = layerArpg.getAsArray("transitions");

        for (int i = 0; i < transitionsArpg.size(); i++) {

            ArpgObjectStruct transitionArpg = transitionsArpg.get(i).getAsObject();
            int from = resolveNodeIndex(
                    ArpgUtility.validateString(transitionArpg, "from"), layerName, nodeName2NodeIndex, sourceName);
            int to = resolveNodeIndex(
                    ArpgUtility.validateString(transitionArpg, "to"), layerName, nodeName2NodeIndex, sourceName);
            float blendDuration = ArpgUtility.validateFloat(transitionArpg, "blend");

            if (blendDuration < 0f)
                throwException("Layer \"" + layerName + "\" transition " + i
                        + " has a negative \"blend\". Source: " + sourceName);

            transitionBlends[from][to] = blendDuration;
        }

        return transitionBlends;
    }

    // Utility \\

    private int resolveNodeIndex(
            String nodeName,
            String layerName,
            Object2IntOpenHashMap<String> nodeName2NodeIndex,
            String sourceName) {

        int nodeIndex = nodeName2NodeIndex.getInt(nodeName);

        if (nodeIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Layer \"" + layerName + "\" references unknown node \"" + nodeName
                    + "\". Source: " + sourceName);

        return nodeIndex;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String name, String owner, String sourceName) {

        for (E constant : enumClass.getEnumConstants())
            if (constant.name().equalsIgnoreCase(name))
                return constant;

        return throwException("\"" + owner + "\" names unknown " + enumClass.getSimpleName() + " \"" + name
                + "\". Source: " + sourceName);
    }
}
