package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector4Boolean;
import engine.util.memory.BufferUtility;

public final class Vector4BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL bvec4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector4BooleanArrayUniform(int elementCount) {

        super(UniformType.VECTOR4_BOOLEAN, elementCount, new Vector4Boolean[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR4_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector4Boolean[]) value)[i] = new Vector4Boolean();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4BooleanArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector4Boolean vector = (Vector4Boolean) value[i];
            uniformBuffer.put(vector.x ? 1 : 0);
            uniformBuffer.put(vector.y ? 1 : 0);
            uniformBuffer.put(vector.z ? 1 : 0);
            uniformBuffer.put(vector.w ? 1 : 0);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform4iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector4Boolean[] target = (Vector4Boolean[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector4Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
