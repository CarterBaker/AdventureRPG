package application.bootstrap.shaderpipeline.ubo;

import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UBOHandle extends HandlePackage {

    /*
     * Persistent UBO record owned exclusively by UBOManager. Wraps UBOData
     * which carries both source descriptor and runtime state. External systems
     * receive a UBOInstance via UBOManager.createUBOInstance(). All GPU
     * operations go through UBOManager — never called directly here.
     */

    // Internal
    private UBOData uboData;

    // Internal \\

    public void constructor(UBOData uboData) {
        this.uboData = uboData;
    }

    // Source Phase \\

    public void addUniformDeclaration(UniformData uniform) {
        uboData.addUniformDeclaration(uniform);
    }

    // Runtime Phase \\

    public void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes) {
        uboData.initRuntime(bufferID, gpuHandle, bindingPoint, totalSizeBytes);
    }

    public void addCompiledUniform(String name, UniformStruct<?> uniform) {
        uboData.addCompiledUniform(name, uniform);
    }

    // Utility \\

    public void updateUniform(String name, Object value) {
        uboData.updateUniform(name, value);
    }

    // Accessible \\

    public UBOData getUBOData() {
        return uboData;
    }

    public String getBlockName() {
        return uboData.getBlockName();
    }

    public int getRequestedBinding() {
        return uboData.getRequestedBinding();
    }

    public ObjectArrayList<UniformData> getUniformDeclarations() {
        return uboData.getUniformDeclarations();
    }

    public int getBufferID() {
        return uboData.getBufferID();
    }

    public int getGpuHandle() {
        return uboData.getGpuHandle();
    }

    public int getBindingPoint() {
        return uboData.getBindingPoint();
    }

    public int getTotalSizeBytes() {
        return uboData.getTotalSizeBytes();
    }

    public ObjectArrayList<String> getUniformKeys() {
        return uboData.getUniformKeys();
    }

    public UniformStruct<?> getCompiledUniform(String name) {
        return uboData.getCompiledUniform(name);
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms() {
        return uboData.getCompiledUniforms();
    }
}