package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.vectors.Vector3;

public final class Vector3Uniform extends UniformAttributeStruct<Object> {

    public Vector3Uniform() {
        super(UniformType.VECTOR3, new Vector3());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Vector3 vector)
            EngineContext.gl.glUniform3f(handle, vector.x, vector.y, vector.z);
        else
            throw new IllegalArgumentException("push(int, Vector3): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Vector3 vector)
            ((Vector3) this.value).set(vector);
        else
            throw new IllegalArgumentException("applyValue(Vector3): got " + value.getClass());
    }
}
