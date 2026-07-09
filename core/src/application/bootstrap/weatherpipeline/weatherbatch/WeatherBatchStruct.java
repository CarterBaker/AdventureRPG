package application.bootstrap.weatherpipeline.weatherbatch;

import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.weatherpipeline.cloudbuffer.CloudBufferInstance;
import engine.root.StructPackage;

public class WeatherBatchStruct extends StructPackage {

    /*
     * Pairs one CloudBufferInstance (one shared instanced buffer per cloud
     * archetype — see CloudBufferManager) with the MaterialInstance every
     * instance inside it draws with this frame. Source UBOs are lazily
     * cached on first access, same as SkinnedBatchStruct/RenderBatchStruct
     * — owned by the MaterialHandle and never change after bootstrap. A
     * fresh WeatherBatchStruct is created the first time a given cloud
     * archetype buffer is pushed for an fbo in a frame — never reused
     * across frames, since the queue it lives in is cleared every
     * rewindFrame().
     */

    private static final UBOHandle[] EMPTY_UBOS = new UBOHandle[0];

    // Internal
    private final CloudBufferInstance cloudBuffer;
    private final MaterialInstance material;

    // Cache
    private UBOHandle[] cachedSourceUBOs;

    // Constructor \\

    public WeatherBatchStruct(CloudBufferInstance cloudBuffer, MaterialInstance material) {
        this.cloudBuffer = cloudBuffer;
        this.material = material;
    }

    // Accessible \\

    public CloudBufferInstance getCloudBuffer() {
        return cloudBuffer;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

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
}