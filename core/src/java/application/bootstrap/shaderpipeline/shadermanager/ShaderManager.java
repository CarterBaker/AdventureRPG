package application.bootstrap.shaderpipeline.shadermanager;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderManager extends ManagerPackage {

    /*
     * Owns registration and retrieval for all compiled shader programs.
     * Source file parsing lives entirely in bootstrap and never reaches here.
     * Triggers on-demand loading when a requested shader name is not yet
     * in the palette. Shader IDs are assigned in registration order.
     */

    // Internal
    private UBOManager uboManager;

    // Palette
    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private ObjectArrayList<ShaderHandle> shaderID2ShaderHandle;

    // Base \\

    @Override
    protected void create() {

        this.shaderName2ShaderID = RegistryUtility.createNameIndex();
        this.shaderID2ShaderHandle = RegistryUtility.createPalette();

        create(ShaderLoader.class);
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {

        for (int shaderID = 0; shaderID < shaderID2ShaderHandle.size(); shaderID++) {

            ShaderHandle handle = shaderID2ShaderHandle.get(shaderID);

            if (handle != null)
                ShaderGLSLUtility.deleteShaderProgram(handle.getGpuHandle());
        }

        RegistryUtility.clearPalette(shaderName2ShaderID, shaderID2ShaderHandle);
    }

    // Management \\

    int registerShaderName(String shaderName) {
        return RegistryUtility.registerID(
                shaderName2ShaderID, shaderID2ShaderHandle, shaderName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addShaderHandle(ShaderHandle handle) {
        shaderID2ShaderHandle.set(handle.getShaderID(), handle);
    }

    void bindShaderToUBO(ShaderHandle shader, String blockName) {
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(blockName);

        if (shader.claimBlockBinding(ubo.getBindingPoint()))
            ShaderGLSLUtility.bindUniformBlock(shader.getGpuHandle(), blockName, ubo.getBindingPoint());
    }

    // Accessible \\

    public void request(String shaderName) {
        ((ShaderLoader) internalLoader).request(shaderName);
    }

    public boolean hasShader(String shaderName) {
        return RegistryUtility.getHandle(shaderName2ShaderID, shaderID2ShaderHandle, shaderName) != null;
    }

    public int getShaderIDFromShaderName(String shaderName) {

        if (!hasShader(shaderName))
            request(shaderName);

        return shaderName2ShaderID.getInt(shaderName);
    }

    public ShaderHandle getShaderHandleFromShaderID(int shaderID) {

        ShaderHandle handle = RegistryUtility.getHandle(shaderID2ShaderHandle, shaderID);

        if (handle == null)
            throwException("Shader ID not found: " + shaderID);

        return handle;
    }

    public ShaderHandle getShaderHandleFromShaderName(String shaderName) {
        return getShaderHandleFromShaderID(getShaderIDFromShaderName(shaderName));
    }
}