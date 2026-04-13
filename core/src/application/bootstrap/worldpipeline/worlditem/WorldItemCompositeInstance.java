package application.bootstrap.worldpipeline.worlditem;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.InstancePackage;

/*
 * Pairs a cloned MaterialInstance with a CompositeBufferInstance for a single
 * item definition. One instance per distinct item type, owned by
 * WorldItemRenderSystem. Render concern only — lives in worlditemrendersystem,
 * not worlditem.
 */
public class WorldItemCompositeInstance extends InstancePackage {

    private MaterialInstance material;
    private CompositeBufferInstance compositeBuffer;

    // Constructor \\

    public void constructor(MaterialInstance material, CompositeBufferInstance compositeBuffer) {
        this.material = material;
        this.compositeBuffer = compositeBuffer;
    }

    // Accessible \\

    public MaterialInstance getMaterial() {
        return material;
    }

    public CompositeBufferInstance getCompositeBuffer() {
        return compositeBuffer;
    }
}