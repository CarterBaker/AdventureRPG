package application.bootstrap.entitypipeline.animationtree;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class AnimationLayerStruct extends StructPackage {

    /*
     * Immutable layer of an animation tree. stateNodes maps every
     * EntityState ordinal to the node this layer plays for it, or
     * EngineSetting.INDEX_NOT_FOUND where the layer rests and fades out.
     * boneMask is this layer's per-bone blend weight, indexed like the rig
     * — 1.0 everywhere for an unmasked layer, 0.0 for any bone a mask
     * leaves out. transitionBlends[from][to] is the cross-fade between two
     * of its nodes, the target node's own blend unless a transition in
     * JSON overrides it. blendDuration is how long the whole layer takes to
     * fade in or out as its states come and go.
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

    public float getTransitionBlend(int fromNode, int toNode) {
        return transitionBlends[fromNode][toNode];
    }
}
