package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3Int;
import engine.util.memory.BufferUtility;

public final class Vector3IntArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL ivec3 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector3IntArrayUniform(int elementCount) {

        super(UniformType.VECTOR3_INT, elementCount, new Vector3Int[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR3_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector3Int[]) value)[i] = new Vector3Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3IntArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector3Int vector = (Vector3Int) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
            uniformBuffer.put(vector.z);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform3iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector3Int[] target = (Vector3Int[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector3Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
