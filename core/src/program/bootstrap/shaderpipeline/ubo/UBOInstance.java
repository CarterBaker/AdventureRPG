package program.bootstrap.shaderpipeline.ubo;

import program.bootstrap.shaderpipeline.uniforms.UniformStruct;
import program.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UBOInstance extends InstancePackage {

    /*
     * Runtime UBO handed to external systems by UBOManager.createUBOInstance().
     * Wraps a deep-copied UBOData with its own GPU buffer and staging buffer.
     * Shares the source handle's binding point so the shader block binding stays
     * valid. All GPU operations go through UBOManager — never called directly here.
     * Must be released via UBOManager.destroyInstance().
     */

    // Internal
    private UBOData uboData;

    // Internal \\

    public void constructor(UBOData uboData) {
        this.uboData = uboData;
    }

    // Runtime Phase \\

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

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms() {
        return uboData.getCompiledUniforms();
    }
}