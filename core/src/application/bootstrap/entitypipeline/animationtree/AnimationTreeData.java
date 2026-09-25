package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.DataPackage;

public class AnimationTreeData extends DataPackage {

    /*
     * Immutable animation tree definition — the JSON graph that decides
     * which clips an entity plays for each movement state and how they mix.
     * Layers evaluate in order on one rig: the first is a full-body OVERRIDE
     * layer that covers every EntityState, and each layer after it blends
     * over the pose so far through its own bone mask.
     */

    // Identity
    private final String treeName;
    private final RigHandle rigHandle;

    // Layers — evaluated in order, base first
    private final AnimationLayerStruct[] layers;

    // Constructor \\

    public AnimationTreeData(
            String treeName,
            RigHandle rigHandle,
            AnimationLayerStruct[] layers) {

        // Identity
        this.treeName = treeName;
        this.rigHandle = rigHandle;

        // Layers
        this.layers = layers;
    }

    // Accessible \\

    public String getTreeName() {
        return treeName;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getLayerCount() {
        return layers.length;
    }

    public AnimationLayerStruct getLayer(int layerIndex) {
        return layers[layerIndex];
    }
}
