package program.bootstrap.shaderpipeline.pass;

import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.core.engine.DataPackage;

public class PassData extends DataPackage {

    /*
     * Complete pass record. Identity and mesh are shared references — never
     * cloned. Material and ModelInstance are per-instance state — deep-copied
     * when a PassInstance is created. Owned by PassHandle (registered) or
     * PassInstance (cloned) for their respective lifetimes.
     */

    // Identity
    private final String passName;
    private final int passID;

    // Shared — never cloned
    private final MeshHandle meshHandle;

    // Per-instance state
    private final MaterialInstance material;
    private final ModelInstance modelInstance;

    // Constructor \\

    public PassData(
            String passName,
            int passID,
            MeshHandle meshHandle,
            MaterialInstance material,
            ModelInstance modelInstance) {

        this.passName = passName;
        this.passID = passID;
        this.meshHandle = meshHandle;
        this.material = material;
        this.modelInstance = modelInstance;
    }

    // Utility \\

    public <T> void setUniform(String uniformName, T value) {
        material.setUniform(uniformName, value);
    }

    // Accessible \\

    public String getPassName() {
        return passName;
    }

    public int getPassID() {
        return passID;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public MaterialInstance getMaterial() {
        return material;
    }

    public ModelInstance getModelInstance() {
        return modelInstance;
    }
}