package application.bootstrap.shaderpipeline.uniforms;

import engine.root.DataPackage;

public class UniformData extends DataPackage {

    /*
     * Parsed uniform descriptor. Holds type, name, and array count.
     * Created with new during shader bootstrap — discarded after
     * UniformAttributeStruct
     * creation.
     */

    // Internal
    private final UniformType uniformType;
    private final String uniformName;
    private final int count;

    // Constructor — single \\

    public UniformData(UniformType uniformType, String uniformName) {
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = 1;
    }

    // Constructor — array \\

    public UniformData(UniformType uniformType, String uniformName, int count) {
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = count;
    }

    // Accessible \\

    public UniformType getUniformType() {
        return uniformType;
    }

    public String getUniformName() {
        return uniformName;
    }

    public int getCount() {
        return count;
    }
}