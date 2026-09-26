package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector4Int;
import engine.util.memory.BufferUtility;

public final class Vector4IntArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL ivec4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector4IntArrayUniform(int elementCount) {

        super(UniformType.VECTOR4_INT, elementCount, new Vector4Int[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR4_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector4Int[]) value)[i] = new Vector4Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4IntArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector4Int vector = (Vector4Int) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
            uniformBuffer.put(vector.z);
            uniformBuffer.put(vector.w);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform4iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector4Int[] target = (Vector4Int[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector4Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
