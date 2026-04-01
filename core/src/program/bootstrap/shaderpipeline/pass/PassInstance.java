package program.bootstrap.shaderpipeline.pass;

import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.ubo.UBOInstance;
import program.core.engine.InstancePackage;

public class PassInstance extends InstancePackage {

    /*
     * Runtime pass handed to external systems by PassManager.clonePass().
     * Wraps a PassData built from a deep-copied MaterialInstance and a fresh
     * ModelInstance. Safe to mutate — discard when no longer needed.
     */

    // Internal
    private PassData passData;

    // Internal \\

    public void constructor(PassData passData) {
        this.passData = passData;
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        passData.getMaterial().setUBO(ubo);
    }

    public <T> void setUniform(String uniformName, T value) {
        passData.setUniform(uniformName, value);
    }

    // Accessible \\

    public PassData getPassData() {
        return passData;
    }

    public String getPassName() {
        return passData.getPassName();
    }

    public int getPassID() {
        return passData.getPassID();
    }

    public MeshHandle getMeshHandle() {
        return passData.getMeshHandle();
    }

    public MaterialInstance getMaterial() {
        return passData.getMaterial();
    }

    public ModelInstance getModelInstance() {
        return passData.getModelInstance();
    }
}