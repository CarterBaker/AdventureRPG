package application.bootstrap.shaderpipeline.shadermanager;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.core.engine.ManagerPackage;
import application.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ShaderManager extends ManagerPackage {

    /*
     * Owns registration and retrieval for all compiled shader programs.
     * Source file parsing lives entirely in bootstrap and never reaches here.
     * Triggers on-demand loading when a requested shader name is not yet
     * in the palette.
     */

    // Internal
    private UBOManager uboManager;

    // Palette
    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private Int2ObjectOpenHashMap<ShaderHandle> shaderID2ShaderHandle;

    // Base \\

    @Override
    protected void create() {

        this.shaderName2ShaderID = new Object2IntOpenHashMap<>();
        this.shaderID2ShaderHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {

        for (ShaderHandle handle : shaderID2ShaderHandle.values())
            GLSLUtility.deleteShaderProgram(handle.getGpuHandle());

        shaderName2ShaderID.clear();
        shaderID2ShaderHandle.clear();
    }

    // Management \\

    void addShaderHandle(ShaderHandle handle) {
        int id = RegistryUtility.toIntID(handle.getShaderName());
        shaderName2ShaderID.put(handle.getShaderName(), id);
        shaderID2ShaderHandle.put(id, handle);
    }

    void bindShaderToUBO(ShaderHandle shader, String blockName) {
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(blockName);
        GLSLUtility.bindUniformBlock(shader.getGpuHandle(), blockName, ubo.getBindingPoint());
    }

    // Accessible \\

    public void request(String shaderName) {
        ((InternalLoader) internalLoader).request(shaderName);
    }

    public boolean hasShader(String shaderName) {
        return shaderName2ShaderID.containsKey(shaderName);
    }

    public int getShaderIDFromShaderName(String shaderName) {

        if (!shaderName2ShaderID.containsKey(shaderName))
            request(shaderName);

        return shaderName2ShaderID.getInt(shaderName);
    }

    public ShaderHandle getShaderHandleFromShaderID(int shaderID) {

        ShaderHandle handle = shaderID2ShaderHandle.get(shaderID);

        if (handle == null)
            throwException("Shader ID not found: " + shaderID);

        return handle;
    }

    public ShaderHandle getShaderHandleFromShaderName(String shaderName) {
        return getShaderHandleFromShaderID(getShaderIDFromShaderName(shaderName));
    }
}