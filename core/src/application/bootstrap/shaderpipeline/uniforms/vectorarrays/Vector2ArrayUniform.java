package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2;
import engine.util.memory.BufferUtility;

public final class Vector2ArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL vec2 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector2ArrayUniform(int elementCount) {

        super(UniformType.VECTOR2, elementCount, new Vector2[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR2_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector2[]) value)[i] = new Vector2();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2ArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector2 vector = (Vector2) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform2fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector2[] target = (Vector2[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector2) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector2[] vectors)
            applyValue(vectors);
        else
            throwException("Vector2ArrayUniform expects Vector2[], got " + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
