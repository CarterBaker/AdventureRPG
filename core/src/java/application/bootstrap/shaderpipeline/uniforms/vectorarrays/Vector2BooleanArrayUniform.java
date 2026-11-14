package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2Boolean;
import engine.util.memory.BufferUtility;

public final class Vector2BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL bvec2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public Vector2BooleanArrayUniform(int elementCount) {

        super(UniformType.VECTOR2_BOOLEAN, elementCount, new Vector2Boolean[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount * EngineSetting.VECTOR2_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector2Boolean[]) value)[i] = new Vector2Boolean();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2BooleanArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector2Boolean vector = (Vector2Boolean) value[i];
            uniformBuffer.put(vector.x ? 1 : 0);
            uniformBuffer.put(vector.y ? 1 : 0);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform2iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector2Boolean[] target = (Vector2Boolean[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector2Boolean) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}
