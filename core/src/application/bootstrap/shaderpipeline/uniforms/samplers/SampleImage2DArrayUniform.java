package application.bootstrap.shaderpipeline.uniforms.samplers;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;

public final class SampleImage2DArrayUniform extends UniformAttributeStruct<Integer> {

    private int textureUnit = 0;

    public SampleImage2DArrayUniform() {
        super(UniformType.SAMPLE_IMAGE_2D_ARRAY, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new SampleImage2DArrayUniform();
    }

    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        this.textureUnit = unit;
        EngineContext.gl20.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        EngineContext.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, value);
    }

    @Override
    protected void push(int handle, Integer value) {
        EngineContext.gl20.glUniform1i(handle, textureUnit);
    }

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}
