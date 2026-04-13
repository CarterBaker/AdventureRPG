package application.bootstrap.renderpipeline.compositebatch;

import application.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CompositeBatchStruct extends StructPackage {

    /*
     * Groups all CompositeBufferInstances sharing the same material for one
     * draw pass. The MaterialInstance drives shader and UBO binding. Source
     * UBOs and uniforms are lazily cached on first access — safe since they
     * never change after bootstrap. Cleared after every draw flush.
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];
    private static final UniformStruct<?>[] EMPTY_UNIFORMS = new UniformStruct<?>[0];

    // Internal
    private final MaterialInstance material;
    private final ObjectArrayList<CompositeBufferInstance> buffers;

    // Cache
    private UBOHandle[] cachedSourceUBOs;
    private UniformStruct<?>[] cachedUniforms;

    // Constructor \\

    public CompositeBatchStruct(MaterialInstance material) {
        this.material = material;
        this.buffers = new ObjectArrayList<>();
    }

    // Management \\

    public void add(CompositeBufferInstance buffer) {
        buffers.add(buffer);
    }

    public void clear() {
        buffers.clear();
    }

    public boolean isEmpty() {
        return buffers.isEmpty();
    }

    // Accessible \\

    public MaterialInstance getMaterial() {
        return material;
    }

    public ObjectArrayList<CompositeBufferInstance> getBuffers() {
        return buffers;
    }

    /*
     * Lazily resolved on first call — source UBOs are owned by the
     * MaterialHandle and never change after bootstrap.
     */
    public UBOHandle[] getCachedSourceUBOs() {

        if (cachedSourceUBOs != null)
            return cachedSourceUBOs;

        var sourceUBOs = material.getSourceUBOs();

        if (sourceUBOs == null || sourceUBOs.isEmpty())
            cachedSourceUBOs = EMPTY_UBOS;
        else
            cachedSourceUBOs = sourceUBOs.values().toArray(new UBOHandle[0]);

        return cachedSourceUBOs;
    }

    /*
     * Lazily resolved on first call — uniforms are owned by the
     * MaterialInstance and never change after bootstrap.
     */
    public UniformStruct<?>[] getCachedUniforms() {

        if (cachedUniforms != null)
            return cachedUniforms;

        var uniforms = material.getUniforms();

        if (uniforms == null || uniforms.isEmpty())
            cachedUniforms = EMPTY_UNIFORMS;
        else
            cachedUniforms = uniforms.values().toArray(new UniformStruct<?>[0]);

        return cachedUniforms;
    }
}