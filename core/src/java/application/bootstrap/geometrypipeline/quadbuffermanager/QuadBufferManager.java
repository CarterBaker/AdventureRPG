package application.bootstrap.geometrypipeline.quadbuffermanager;

import application.bootstrap.geometrypipeline.ibo.IBOData;
import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.quadbuffer.QuadBufferData;
import application.bootstrap.geometrypipeline.quadbuffer.QuadBufferInstance;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vaomanager.VAOManager;
import application.bootstrap.geometrypipeline.vbo.VBOData;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class QuadBufferManager extends ManagerPackage {

    /*
     * Creates, uploads, empties and removes QuadBufferInstances, the GPU side
     * of packed quad records. Every quad draw shares one attribute-less VAO
     * instance, made on the first upload, since its shader pulls each quad
     * from the buffer texture by vertex index; VAOManager caches the
     * context-local clone per window. A layout's texel format follows from
     * its stride, and a draw mesh is rebuilt only when its quad count moves.
     */

    // Internal
    private VAOManager vaoManager;
    private VAOInstance quadVAOInstance;

    // Draw
    private IBOData emptyIBOData;
    private Vector3 zeroBounds;

    // Base \\

    @Override
    protected void create() {

        // Draw
        this.emptyIBOData = new IBOData(EngineSetting.GL_HANDLE_NONE, 0);
        this.zeroBounds = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.vaoManager = get(VAOManager.class);
    }

    // Management \\

    public QuadBufferInstance createQuadBuffer(IntArrayList records, int stride) {

        // The buffer is bound and given storage before the texture views it, since a name never bound is no object
        int bufferHandle = QuadBufferGLSLUtility.createBuffer();
        int capacityInts = QuadBufferGLSLUtility.upload(bufferHandle, 0, records.elements(), records.size());
        int textureHandle = QuadBufferGLSLUtility.createTexture(bufferHandle, resolveFormat(stride));

        QuadBufferData data = new QuadBufferData(bufferHandle, textureHandle);
        data.setCapacityInts(capacityInts);
        data.setMeshData(buildMeshData(bufferHandle, records.size() / stride * EngineSetting.QUAD_VERTEX_COUNT));

        QuadBufferInstance quadBufferInstance = create(QuadBufferInstance.class);
        quadBufferInstance.constructor(data);

        return quadBufferInstance;
    }

    public void upload(QuadBufferInstance quadBufferInstance, IntArrayList records, int stride) {

        QuadBufferData data = quadBufferInstance.getQuadBufferData();
        int vertexCount = records.size() / stride * EngineSetting.QUAD_VERTEX_COUNT;

        data.setCapacityInts(QuadBufferGLSLUtility.upload(
                data.getBufferHandle(),
                data.getCapacityInts(),
                records.elements(),
                records.size()));

        MeshData meshData = data.getMeshData();

        if (meshData == null || meshData.getVertexCount() != vertexCount)
            data.setMeshData(buildMeshData(data.getBufferHandle(), vertexCount));
    }

    // A pooled buffer gives its storage back until its next records arrive
    public void empty(QuadBufferInstance quadBufferInstance) {

        QuadBufferData data = quadBufferInstance.getQuadBufferData();

        QuadBufferGLSLUtility.empty(data.getBufferHandle());
        data.setCapacityInts(0);
    }

    // Removal \\

    public void removeQuadBuffer(QuadBufferInstance quadBufferInstance) {

        QuadBufferData data = quadBufferInstance.getQuadBufferData();

        QuadBufferGLSLUtility.delete(data.getBufferHandle(), data.getTextureHandle());
    }

    // Utility \\

    private MeshData buildMeshData(int bufferHandle, int vertexCount) {

        if (quadVAOInstance == null)
            quadVAOInstance = vaoManager.createVAOInstance(vaoManager.getVAOHandleFromVAOName(EngineSetting.QUAD_VAO));

        return new MeshData(
                quadVAOInstance.getVAOData(),
                new VBOData(bufferHandle, vertexCount),
                emptyIBOData,
                zeroBounds,
                zeroBounds);
    }

    private int resolveFormat(int stride) {

        if (stride % EngineSetting.QUAD_RECORD_TEXEL_INTS == 0)
            return EngineSetting.GL_RGBA32UI;

        if (stride % EngineSetting.QUAD_RECORD_LIQUID_INTS == 0)
            return EngineSetting.GL_RG32UI;

        return throwException("A quad record stride of " + stride + " ints fits no buffer texel format.");
    }
}
