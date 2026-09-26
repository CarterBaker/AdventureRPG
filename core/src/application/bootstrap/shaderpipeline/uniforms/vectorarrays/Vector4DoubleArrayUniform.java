package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector4Double;
import engine.util.memory.BufferUtility;

public final class Vector4DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dvec4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector4DoubleArrayUniform(int elementCount) {

        super(UniformType.VECTOR4_DOUBLE, elementCount, new Vector4Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR4_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector4Double[]) value)[i] = new Vector4Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector4Double vector = (Vector4Double) value[i];
            uniformBuffer.put((float) vector.x);
            uniformBuffer.put((float) vector.y);
            uniformBuffer.put((float) vector.z);
            uniformBuffer.put((float) vector.w);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform4fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector4Double[] target = (Vector4Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector4Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
