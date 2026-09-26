package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2Double;
import engine.util.memory.BufferUtility;

public final class Vector2DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dvec2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector2DoubleArrayUniform(int elementCount) {

        super(UniformType.VECTOR2_DOUBLE, elementCount, new Vector2Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR2_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector2Double[]) value)[i] = new Vector2Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector2Double vector = (Vector2Double) value[i];
            uniformBuffer.put((float) vector.x);
            uniformBuffer.put((float) vector.y);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform2fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector2Double[] target = (Vector2Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector2Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
