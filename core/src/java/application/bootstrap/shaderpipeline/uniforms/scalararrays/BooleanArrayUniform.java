package application.bootstrap.shaderpipeline.uniforms.scalararrays;

import java.nio.IntBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.memory.BufferUtility;

public final class BooleanArrayUniform extends UniformAttributeStruct<Object[]> {

    /*
     * GLSL bool array uniform. Elements are packed into a preallocated direct
     * buffer on push, so uploads never allocate.
     */

    // Internal
    private final int elementCount;
    private final IntBuffer uniformBuffer;

    // Constructor \\

    public BooleanArrayUniform(int elementCount) {

        super(UniformType.BOOL, elementCount, new Boolean[elementCount]);

        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtility.newIntBuffer(elementCount);

        for (int i = 0; i < elementCount; i++)
            ((Boolean[]) value)[i] = false;
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new BooleanArrayUniform(elementCount);
    }

    // Push \\

    @Override
    protected void push(int handle, Object[] value) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put((Boolean) value[i] ? 1 : 0);

        uniformBuffer.flip();
        EngineContext.gl20.glUniform1iv(handle, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Object[] value) {

        Boolean[] target = (Boolean[]) this.value;

        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            target[i] = (Boolean) value[i];
    }

    public int elementCount() {
        return elementCount;
    }
}
