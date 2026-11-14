package application.bootstrap.shaderpipeline.uniforms.vectorarrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector4;
import engine.util.memory.BufferUtility;

public final class Vector4ArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL vec4 array uniform. Elements are packed into a preallocated
     * direct buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Vector4ArrayUniform(int elementCount) {

        super(UniformType.VECTOR4, elementCount, new Vector4[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount * EngineSetting.VECTOR4_COMPONENT_COUNT);

        for (int i = 0; i < elementCount; i++)
            ((Vector4[]) value)[i] = new Vector4();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4ArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {
            Vector4 vector = (Vector4) value[i];
            uniformBuffer.put(vector.x);
            uniformBuffer.put(vector.y);
            uniformBuffer.put(vector.z);
            uniformBuffer.put(vector.w);
        }

        uniformBuffer.flip();
        EngineContext.gl20.glUniform4fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Vector4[] target = (Vector4[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i].set((Vector4) value[i]);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector4[] vectors)
            applyValue(vectors);
        else
            throwException("Vector4ArrayUniform expects Vector4[], got " + value.getClass().getSimpleName());
    }

    public int elementCount() {
        return elementCount;
    }
}
