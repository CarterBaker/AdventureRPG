package com.internal.bootstrap.shaderpipeline.shadermanager;

import java.io.File;

import com.internal.bootstrap.shaderpipeline.ubomanager.UBOData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Bootstrap transfer container for a single parsed GLSL source file.
 * Carries the raw file reference, parsed version directive, discovered uniform
 * declarations, UBO block declarations, and direct include dependencies.
 * Lives with the shadermanager package — never handed outside bootstrap.
 */
public class ShaderData extends DataPackage {

    // Internal
    private ShaderType shaderType;
    private String shaderName;
    private File shaderFile;
    private String version;
    private ObjectArrayList<ShaderData> includes;
    private ObjectArrayList<UBOData> bufferBlocks;
    private ObjectArrayList<UniformData> uniforms;

    // Internal \\

    @Override
    protected void get() {
        this.version = null;
        this.includes = new ObjectArrayList<>();
        this.bufferBlocks = new ObjectArrayList<>();
        this.uniforms = new ObjectArrayList<>();
    }

    public void constructor(
            ShaderType shaderType,
            String shaderName,
            File shaderFile) {
        this.shaderType = shaderType;
        this.shaderName = shaderName;
        this.shaderFile = shaderFile;
    }

    // Accessible \\

    public ShaderType getShaderType() {
        return shaderType;
    }

    public String getShaderName() {
        return shaderName;
    }

    public File getShaderFile() {
        return shaderFile;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void addIncludes(ShaderData include) {
        includes.add(include);
    }

    public ObjectArrayList<ShaderData> getIncludes() {
        return includes;
    }

    public void addBufferBlock(UBOData block) {
        bufferBlocks.add(block);
    }

    public ObjectArrayList<UBOData> getBufferBlocks() {
        return bufferBlocks;
    }

    public void addUniform(UniformData uniform) {
        uniforms.add(uniform);
    }

    public ObjectArrayList<UniformData> getUniforms() {
        return uniforms;
    }
}