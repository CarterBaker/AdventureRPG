package application.bootstrap.entitypipeline.animationtree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class AnimationLayerStruct extends StructPackage {

    /*
     * Immutable layer of an animation tree: the node for each entity state and
     * each entity action, the per-bone mask weights, the cross-fade between
     * each pair of nodes, and how long the whole layer fades in or out. An
     * action's node takes the layer over for as long as the action lasts.
     */

    // Identity
    private final String layerName;

    // Blend
    private final AnimationBlendMode blendMode;
    private final float weight;
    private final float blendDuration;
    private final float[] boneMask;

    // Graph
    private final AnimationNodeStruct[] nodes;
    private final int[] stateNodes;
    private final int[] actionNodes;
    private final float[][] transitionBlends;

    // Constructor \\

    public AnimationLayerStruct(
            String layerName,
            AnimationBlendMode blendMode,
            float weight,
            float blendDuration,
            float[] boneMask,
            AnimationNodeStruct[] nodes,
            int[] stateNodes,
            int[] actionNodes,
            float[][] transitionBlends) {

        // Identity
        this.layerName = layerName;

        // Blend
        this.blendMode = blendMode;
        this.weight = weight;
        this.blendDuration = blendDuration;
        this.boneMask = boneMask;

        // Graph
        this.nodes = nodes;
        this.stateNodes = stateNodes;
        this.actionNodes = actionNodes;
        this.transitionBlends = transitionBlends;
    }

    // Accessible \\

    public String getLayerName() {
        return layerName;
    }

    public AnimationBlendMode getBlendMode() {
        return blendMode;
    }

    public boolean isAdditive() {
        return blendMode == AnimationBlendMode.ADDITIVE;
    }

    public float getWeight() {
        return weight;
    }

    public float getBlendDuration() {
        return blendDuration;
    }

    public float getBoneMask(int boneIndex) {
        return boneMask[boneIndex];
    }

    public int getNodeCount() {
        return nodes.length;
    }

    public AnimationNodeStruct getNode(int nodeIndex) {
        return nodes[nodeIndex];
    }

    public int getStateNode(int stateOrdinal) {
        return stateNodes[stateOrdinal];
    }

    public boolean hasStateNode(int stateOrdinal) {
        return stateNodes[stateOrdinal] != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getActionNode(int actionOrdinal) {
        return actionNodes[actionOrdinal];
    }

    public boolean hasActionNode(int actionOrdinal) {
        return actionNodes[actionOrdinal] != EngineSetting.INDEX_NOT_FOUND;
    }

    public float getTransitionBlend(int fromNode, int toNode) {
        return transitionBlends[fromNode][toNode];
    }
}
