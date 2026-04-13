package program.bootstrap.shaderpipeline.uniforms.samplers;

import program.core.engine.EngineContext;
import program.core.util.graphics.gl.GL20;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

public final class SampleImage2DUniform extends UniformAttributeStruct<Integer> {

    private int textureUnit = 0;

    public SampleImage2DUniform() {
        super(UniformType.SAMPLE_IMAGE_2D, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new SampleImage2DUniform();
    }

    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        this.textureUnit = unit;
        EngineContext.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        EngineContext.gl.glBindTexture(GL20.GL_TEXTURE_2D, value);
    }

    @Override
    protected void push(int handle, Integer value) {
        EngineContext.gl.glUniform1i(handle, textureUnit);
    }

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}
