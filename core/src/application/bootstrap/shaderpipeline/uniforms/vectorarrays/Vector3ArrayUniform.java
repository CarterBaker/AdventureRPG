package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import engine.util.memory.BufferUtility;

public final class Vector3ArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL vec3 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector3ArrayUniform(int elementCount) {

        super(UniformType.VECTOR3, elementCount, new Vector3[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR3_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector3[]) value)[i] = new Vector3();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3ArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector3 vector = (Vector3) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
            uniformBuffer.put(vector.z);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform3fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector3[] target = (Vector3[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector3) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector3[] vectors)
            applyValue(vectors);
        else
            throwException("Vector3ArrayUniform expects Vector3[], got " + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
