package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.HandlePackage;

public class AnimationTreeHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded animation tree. Registered and owned
     * by AnimationTreeManager for the engine lifetime and shared by every
     * entity template that names it — playback state never lives here, it
     * lives on each entity's AnimationStateHandle.
     */

    // Internal
    private AnimationTreeData animationTreeData;

    // Constructor \\

    public void constructor(AnimationTreeData animationTreeData) {

        // Internal
        this.animationTreeData = animationTreeData;
    }

    // Accessible \\

    public AnimationTreeData getAnimationTreeData() {
        return animationTreeData;
    }

    public String getTreeName() {
        return animationTreeData.getTreeName();
    }

    public RigHandle getRigHandle() {
        return animationTreeData.getRigHandle();
    }

    public int getLayerCount() {
        return animationTreeData.getLayerCount();
    }

    public AnimationLayerStruct getLayer(int layerIndex) {
        return animationTreeData.getLayer(layerIndex);
    }
}
