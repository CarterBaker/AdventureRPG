package program.bootstrap.shaderpipeline.uniforms.vectors;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector3;

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
            CoreContext.gl.glUniform3f(handle, vector.x, vector.y, vector.z);
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
