package program.bootstrap.shaderpipeline.uniforms.vectors;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector4Boolean;

public final class Vector4BooleanUniform extends UniformAttributeStruct<Vector4Boolean> {

    public Vector4BooleanUniform() {
        super(UniformType.VECTOR4_BOOLEAN, new Vector4Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4BooleanUniform();
    }

    @Override
    protected void push(int handle, Vector4Boolean value) {
        CoreContext.gl.glUniform4i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0, value.w ? 1 : 0);
    }

    @Override
    protected void applyValue(Vector4Boolean value) {
        this.value.set(value);
    }
}
