package application.bootstrap.shaderpipeline.uniforms.scalararrays;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.memory.BufferUtility;

public final class DoubleArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL double array uniform. Elements are packed into a preallocated direct
     * buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public DoubleArrayUniform(int elementCount) {

        super(UniformType.DOUBLE, elementCount, new Double[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newFloatBuffer(elementCount);

        for (int i = 0; i < elementCount; i++)
            ((Double[]) value)[i] = 0.0;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new DoubleArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put(((Double) value[i]).floatValue());

        uniformBuffer.flip();
        EngineContext.gl20.glUniform1fv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Double[] target = (Double[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i] = (Double) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
