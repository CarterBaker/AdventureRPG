package application.bootstrap.shaderpipeline.uniforms.scalararrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.memory.BufferUtility;

public final class IntegerArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL int array uniform. Elements are packed into a preallocated direct
     * buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public IntegerArrayUniform(int elementCount) {

        super(UniformType.INT, elementCount, new Integer[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount);

        for (int i = 0; i < elementCount; i++)
            ((Integer[]) value)[i] = 0;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new IntegerArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put((Integer) value[i]);

        uniformBuffer.flip();
        EngineContext.gl20.glUniform1iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Integer[] target = (Integer[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i] = (Integer) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
