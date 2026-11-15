package application.bootstrap.geometrypipeline.vaomanager;

import application.bootstrap.geometrypipeline.vao.VAOData;
import engine.graphics.gl.GL20;
import engine.root.EngineContext;
import engine.root.EngineUtility;

public class VAOAttributeUtility extends EngineUtility {

    /*
     * The one place a vertex layout becomes GL attribute pointers. Enables
     * every attribute of a layout from location 0 and points it into the
     * array buffer bound at the time, each with its own component type,
     * normalisation and byte offset, so float and packed layouts take the
     * same path wherever a VAO is built over a mesh's vertex buffer.
     */

    // Attributes \\

    public static void pointAttributes(VAOData vaoData) {

        GL20 gl20 = EngineContext.gl20;
        int[] attrSizes = vaoData.getAttrSizes();
        int[] attrTypes = vaoData.getAttrTypes();
        boolean[] attrNormalized = vaoData.getAttrNormalized();
        int[] attrOffsets = vaoData.getAttrOffsets();
        int strideBytes = vaoData.getStrideBytes();

        for (int i = 0; i < attrSizes.length; i++) {
            gl20.glEnableVertexAttribArray(i);
            gl20.glVertexAttribPointer(i, attrSizes[i], attrTypes[i], attrNormalized[i], strideBytes, attrOffsets[i]);
        }
    }
}
