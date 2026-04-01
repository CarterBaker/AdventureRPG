package program.bootstrap.shaderpipeline.uniforms.vectors;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector2Int;

public final class Vector2IntUniform extends UniformAttributeStruct<Vector2Int> {

    public Vector2IntUniform() {
        super(UniformType.VECTOR2_INT, new Vector2Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2IntUniform();
    }

    @Override
    protected void push(int handle, Vector2Int value) {
        CoreContext.gl.glUniform2i(handle, value.x, value.y);
    }

    @Override
    protected void applyValue(Vector2Int value) {
        this.value.set(value);
    }
}
