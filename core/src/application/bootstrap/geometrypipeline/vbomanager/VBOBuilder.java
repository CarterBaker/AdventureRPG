package application.bootstrap.geometrypipeline.vbomanager;

import java.io.File;

import application.bootstrap.geometrypipeline.vao.VAOInstance;
import application.bootstrap.geometrypipeline.vbo.VBOHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class VBOBuilder extends BuilderPackage {

    /*
     * Parses the 'vbo' field from mesh ARPG and uploads vertex data into a
     * VBOHandle. Supports direct vertex arrays and string references to other
     * registered meshes. Skips files whose VBO contains quad objects — those
     * are handled by quad expansion in the mesh builder. Bootstrap-only.
     */

    // Internal
    private VBOManager vboManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.vboManager = get(VBOManager.class);
    }

    // Build \\

    public void build(
            String resourceName,
            File file,
            Object2ObjectOpenHashMap<String, File> registry,
            VAOInstance vaoInstance) {

        if (vboManager.hasVBO(resourceName))
            return;

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        if (!arpg.has("vbo") || arpg.get("vbo").isNull())
            return;

        ArpgElementStruct vboEl = arpg.get("vbo");

        if (vboEl.isArray() && containsQuadObjects(vboEl.getAsArray()))
            return;

        if (vboEl.isValue() && vboEl.getAsValue().isString()) {
            String refName = vboEl.getAsString();
            resolveRef(refName, resourceName, file, registry, vaoInstance);
            vboManager.registerVBO(resourceName, vboManager.getVBOHandleDirect(refName));
            return;
        }

        if (vboEl.isArray()) {
            vboManager.registerVBO(resourceName, buildFromData(vboEl.getAsArray(), vaoInstance, file));
            return;
        }

        throwException("VBO must be a string reference or vertex array in file: " + file.getName());
    }

    // Resolution \\

    private void resolveRef(
            String refName,
            String sourceResourceName,
            File sourceFile,
            Object2ObjectOpenHashMap<String, File> registry,
            VAOInstance vaoInstance) {

        if (vboManager.hasVBO(refName))
            return;

        File refFile = registry.get(refName);

        if (refFile == null)
            throwException("Referenced VBO '" + refName + "' not found. Source: " + sourceFile.getName());

        ArpgObjectStruct refArpg = ArpgUtility.loadObject(refFile);

        if (!refArpg.has("vbo") || refArpg.get("vbo").isNull())
            throwException("Referenced VBO file '" + refName + "' has no 'vbo' field.");

        ArpgElementStruct refEl = refArpg.get("vbo");

        if (!refEl.isArray())
            throwException("Referenced VBO '" + refName + "' must contain a vertex array.");

        vboManager.registerVBO(refName, buildFromData(refEl.getAsArray(), vaoInstance, refFile));
    }

    // Creation \\

    private VBOHandle buildFromData(
            ArpgArrayStruct verticesArray,
            VAOInstance vaoInstance,
            File file) {

        if (verticesArray.size() == 0)
            throwException("Vertex data array cannot be empty in file: " + file.getName());

        int floatsPerVertex = vaoInstance.getVAOData().getVertStride();
        float[] vertices = new float[verticesArray.size() * floatsPerVertex];
        int index = 0;

        for (ArpgElementStruct vertexEl : verticesArray) {
            ArpgArrayStruct vertex = vertexEl.getAsArray();
            if (vertex.size() != floatsPerVertex)
                throwException("Vertex attribute count mismatch. Expected " + floatsPerVertex
                        + " floats but got " + vertex.size() + " in file: " + file.getName());
            for (ArpgElementStruct val : vertex)
                vertices[index++] = val.getAsFloat();
        }

        return VBOGLSLUtility.uploadVertexData(vaoInstance, create(VBOHandle.class), vertices);
    }

    // Utility \\

    private boolean containsQuadObjects(ArpgArrayStruct array) {
        for (ArpgElementStruct el : array)
            if (el.isObject())
                return true;
        return false;
    }
}