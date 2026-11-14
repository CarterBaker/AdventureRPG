package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2Int;
import engine.util.memory.BufferUtility;

public final class Vector2IntArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL ivec2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector2IntArrayUniform(int elementCount) {

        super(UniformType.VECTOR2_INT, elementCount, new Vector2Int[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR2_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector2Int[]) value)[i] = new Vector2Int();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2IntArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector2Int vector = (Vector2Int) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform2iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector2Int[] target = (Vector2Int[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector2Int) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
