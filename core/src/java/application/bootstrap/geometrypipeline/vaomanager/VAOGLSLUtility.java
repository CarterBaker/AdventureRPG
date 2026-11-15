package application.bootstrap.geometrypipeline.vaomanager;

import application.bootstrap.geometrypipeline.vao.VAOData;
import application.bootstrap.geometrypipeline.vao.VAOHandle;
import application.bootstrap.geometrypipeline.vao.VAOInstance;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

class VAOGLSLUtility extends EngineUtility {

    /*
     * GL creation and deletion operations for VAOManager.
     * Package-private — only VAOManager may call these.
     */

    // Instance Creation \\

    static VAOInstance createVAOInstance(VAOInstance vaoInstance, VAOHandle template) {

        VAOData vaoData = createData(template.getVAOData());
        vaoInstance.constructor(vaoData);

        return vaoInstance;
    }

    static int cloneVAO(VAOData layout, int vertexHandle, int indexHandle) {

        GL30 gl30 = EngineContext.gl30;
        GL20 gl20 = EngineContext.gl20;

        int vao = gl30.glGenVertexArray();

        gl30.glBindVertexArray(vao);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, vertexHandle);

        VAOAttributeUtility.pointAttributes(layout);

        gl20.glBindBuffer(EngineSetting.GL_ELEMENT_ARRAY_BUFFER, indexHandle);
        gl20.glBindBuffer(EngineSetting.GL_ARRAY_BUFFER, 0);
        gl30.glBindVertexArray(0);

        return vao;
    }

    private static VAOData createData(VAOData layout) {

        GL30 gl30 = EngineContext.gl30;
        int[] attrSizes = layout.getAttrSizes();

        int vao = gl30.glGenVertexArray();

        gl30.glBindVertexArray(vao);

        for (int i = 0; i < attrSizes.length; i++)
            gl30.glEnableVertexAttribArray(i);

        gl30.glBindVertexArray(0);

        return new VAOData(vao, attrSizes, layout.getAttrTypes(), layout.getAttrNormalized());
    }

    // Removal \\

    static void removeVAOData(VAOData vaoData) {
        removeVAOHandle(vaoData.getAttributeHandle());
    }

    static void removeVAOInstance(VAOInstance vaoInstance) {
        removeVAOData(vaoInstance.getVAOData());
    }

    static void removeVAOHandle(int vao) {

        if (vao == 0)
            return;

        EngineContext.gl30.glDeleteVertexArray(vao);
    }
}
