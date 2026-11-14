package application.bootstrap.geometrypipeline.mesh;

import application.bootstrap.geometrypipeline.ibo.IBOHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class MeshHandle extends HandlePackage {

    /*
     * A GPU-resident static mesh assembled from ARPG, owned by MeshManager and
     * known by the name it registered under; callers receive ModelInstances
     * built from its MeshData. A non-null rig marks meshes that carry bone
     * attributes, whose raw extent scales them onto an entity's size. A
     * rigged mesh can be rebuilt in place through MeshManager, keeping its
     * handles, so everything drawing it shows the change. A rigged mesh also
     * holds its morph targets: their names, by index, and the texture their
     * deltas live in on the GPU, and which of its rig's bones any vertex
     * weighs onto, so a character simulates only the spring chains it wears.
     */

    // Identity
    private String meshName;

    // Internal
    private VAOInstance vaoInstance;
    private VBOHandle vboHandle;
    private IBOHandle iboHandle;
    private MeshData meshData;

    // Rig
    private RigHandle rigHandle;

    // Morphs
    private int morphTexture;
    private String[] morphNames;

    // Bones — by rig bone, whether any vertex weighs onto it
    private boolean[] weighedBones;

    // Constructor \\

    public void constructor(
            String meshName,
            VAOInstance vaoInstance,
            VBOHandle vboHandle,
            IBOHandle iboHandle,
            RigHandle rigHandle,
            Vector3 boundsMin,
            Vector3 boundsMax) {

        // Identity
        this.meshName = meshName;

        // Internal
        this.vaoInstance = vaoInstance;
        this.vboHandle = vboHandle;
        this.iboHandle = iboHandle;
        this.meshData = new MeshData(
                vaoInstance.getVAOData(),
                vboHandle.getVBOData(),
                iboHandle.getIBOData(),
                boundsMin,
                boundsMax);

        // Rig
        this.rigHandle = rigHandle;

        // Morphs
        if (morphNames == null)
            this.morphNames = EngineSetting.RIGGED_MESH_NO_MORPHS;
    }

    // Morphs \\

    public void setMorphTargets(int morphTexture, String[] morphNames) {
        this.morphTexture = morphTexture;
        this.morphNames = morphNames;
    }

    public int getMorphTexture() {
        return morphTexture;
    }

    public int getMorphCount() {
        return morphNames.length;
    }

    public String getMorphName(int morphIndex) {
        return morphNames[morphIndex];
    }

    // The index of a morph target by name, INDEX_NOT_FOUND when the mesh has none of that name
    public int getMorphIndex(String morphName) {

        for (int i = 0; i < morphNames.length; i++)
            if (morphNames[i].equals(morphName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Bones \\

    public void setWeighedBones(boolean[] weighedBones) {
        this.weighedBones = weighedBones;
    }

    // Whether any vertex of the mesh weighs onto a bone of its rig
    public boolean weighsOnto(int boneIndex) {
        return weighedBones != null && weighedBones[boneIndex];
    }

    // Accessible \\

    public String getMeshName() {
        return meshName;
    }

    public VAOInstance getVAOInstance() {
        return vaoInstance;
    }

    public VBOHandle getVBOHandle() {
        return vboHandle;
    }

    public IBOHandle getIBOHandle() {
        return iboHandle;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public int getAttributeHandle() {
        return meshData.getAttributeHandle();
    }

    public int[] getAttrSizes() {
        return meshData.getVAOData().getAttrSizes();
    }

    public int getVertexHandle() {
        return meshData.getVertexHandle();
    }

    public int getIndexHandle() {
        return meshData.getIndexHandle();
    }

    public int getIndexCount() {
        return meshData.getIndexCount();
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public boolean hasRig() {
        return rigHandle != null;
    }

    public Vector3 getBoundsMin() {
        return meshData.getBoundsMin();
    }

    public Vector3 getBoundsMax() {
        return meshData.getBoundsMax();
    }

    public float getWidth() {
        return meshData.getWidth();
    }

    public float getHeight() {
        return meshData.getHeight();
    }

    public float getLength() {
        return meshData.getLength();
    }
}