package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3Double;
import engine.util.memory.BufferUtility;

public final class Vector3DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL dvec3 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector3DoubleArrayUniform(int elementCount) {

        super(UniformType.VECTOR3_DOUBLE, elementCount, new Vector3Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR3_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector3Double[]) value)[i] = new Vector3Double();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector3Double vector = (Vector3Double) value[i];
            uniformBuffer.put((float) vector.x);
            uniformBuffer.put((float) vector.y);
            uniformBuffer.put((float) vector.z);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform3fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector3Double[] target = (Vector3Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector3Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
