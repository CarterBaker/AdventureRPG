package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3Boolean;
import engine.util.memory.BufferUtility;

public final class Vector3BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL bvec3 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector3BooleanArrayUniform(int elementCount) {

        super(UniformType.VECTOR3_BOOLEAN, elementCount, new Vector3Boolean[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR3_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector3Boolean[]) value)[i] = new Vector3Boolean();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3BooleanArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector3Boolean vector = (Vector3Boolean) value[i];
            uniformBuffer.put(vector.x ? 1 : 0);
            uniformBuffer.put(vector.y ? 1 : 0);
            uniformBuffer.put(vector.z ? 1 : 0);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform3iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector3Boolean[] target = (Vector3Boolean[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector3Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
