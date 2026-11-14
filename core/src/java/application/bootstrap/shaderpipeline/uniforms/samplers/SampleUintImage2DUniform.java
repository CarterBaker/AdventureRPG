package application.bootstrap.shaderpipeline.uniforms.samplers;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;

public final class SampleUintImage2DUniform extends UniformAttributeStruct<Integer> {

    /*
     * GLSL usampler2D uniform. Holds an integer texture handle a shader reads
     * with texelFetch, binds it to the unit the render pass assigns, and
     * uploads that unit.
     */

    // Internal
    private int textureUnit;

    // Constructor \\

    public SampleUintImage2DUniform() {
        super(UniformType.SAMPLE_UINT_IMAGE_2D, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new SampleUintImage2DUniform();
    }

    // Sampler \\

    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        this.textureUnit = unit;
        EngineContext.gl20.glActiveTexture(EngineSetting.GL_TEXTURE0 + unit);
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, value);
    }

    // Push \\

    @Override
    protected void push(int handle, Integer value) {
        EngineContext.gl20.glUniform1i(handle, textureUnit);
    }

    // Accessible \\

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}
