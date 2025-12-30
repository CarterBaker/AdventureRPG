package com.AdventureRPG.core.shaderpipeline.shadermanager;

import java.io.File;

import com.AdventureRPG.core.engine.DataPackage;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOData;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderData extends DataPackage {

    // Internal
    private final ShaderType shaderType;
    private final File shaderFile;

    private String version;
    private final ObjectArrayList<ShaderData> includes;
    private final ObjectArrayList<UBOData> bufferBlocks;
    private final ObjectArrayList<UniformData> uniforms;

    public ShaderData(
            ShaderType shaderType,
            String name,
            File shaderFile) {

        // Internal
        super(
                name,
                0);

        this.shaderType = shaderType;
        this.shaderFile = shaderFile;

        this.version = null;
        this.bufferBlocks = new ObjectArrayList<>();
        this.uniforms = new ObjectArrayList<>();
        this.includes = new ObjectArrayList<>();
    }

    // Utility \\

    public ShaderType shaderType() {
        return shaderType;
    }

    public File shaderFile() {
        return shaderFile;
    }

    // Accessible \\

    // Version
    String getVersion() {
        return version;
    }

    void setVersion(String version) {
        this.version = version;
    }

    // Includes
    void addIncludes(ShaderData include) {
        includes.add(include);
    }

    ObjectArrayList<ShaderData> getIncludes() {
        return includes;
    }

    // Buffers
    public void addBufferBlock(UBOData block) {
        bufferBlocks.add(block);
    }

    public ObjectArrayList<UBOData> getBufferBlocks() {
        return bufferBlocks;
    }

    // Uniforms
    void addUniform(UniformData uniform) {
        uniforms.add(uniform);
    }

    ObjectArrayList<UniformData> getUniforms() {
        return uniforms;
    }
}
