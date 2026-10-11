package application.bootstrap.geometrypipeline.quadbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import engine.root.DataPackage;

public class QuadBufferData extends DataPackage {

    /*
     * Holds the mutable state of one quad buffer — the GPU buffer of packed
     * quad records, the buffer texture its shader pulls them through, the
     * storage the buffer holds in ints, and the attribute-less draw mesh
     * whose vertex count is four per quad. All mutation is driven by
     * QuadBufferManager.
     */

    // GPU Handles
    private final int bufferHandle;
    private final int textureHandle;

    // Storage
    private int capacityInts;

    // Draw
    private MeshData meshData;

    // Constructor \\

    public QuadBufferData(int bufferHandle, int textureHandle) {

        // GPU Handles
        this.bufferHandle = bufferHandle;
        this.textureHandle = textureHandle;
    }

    // Accessible \\

    public int getBufferHandle() {
        return bufferHandle;
    }

    public int getTextureHandle() {
        return textureHandle;
    }

    public int getCapacityInts() {
        return capacityInts;
    }

    public void setCapacityInts(int capacityInts) {
        this.capacityInts = capacityInts;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public void setMeshData(MeshData meshData) {
        this.meshData = meshData;
    }
}
