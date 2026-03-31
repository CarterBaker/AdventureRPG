package com.internal.bootstrap.shaderpipeline.uniforms.samplers;

import com.internal.platform.Gdx;
import com.internal.platform.graphics.GL20;
import com.internal.platform.graphics.GL30;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

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
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit);
        Gdx.gl30.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, value);
    }

    @Override
    protected void push(int handle, Integer value) {
        Gdx.gl.glUniform1i(handle, textureUnit);
    }

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}