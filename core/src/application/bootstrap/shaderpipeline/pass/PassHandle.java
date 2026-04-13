package application.bootstrap.shaderpipeline.pass;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.core.engine.HandlePackage;

public class PassHandle extends HandlePackage {

    /*
     * Compiled fullscreen pass owned by PassManager. Wraps PassData holding
     * the original material and ModelInstance. Retains the MeshHandle so
     * PassManager.clonePass() can construct new ModelInstances per PassInstance
     * without duplicating geometry.
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