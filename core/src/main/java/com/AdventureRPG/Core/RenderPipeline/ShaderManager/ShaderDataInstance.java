package com.AdventureRPG.core.renderpipeline.shadermanager;

import java.io.File;

import com.AdventureRPG.core.kernel.InstanceFrame;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderDataInstance extends InstanceFrame {

    // Internal
    private final ShaderType shaderType;
    private final String shaderName;
    private final File shaderFile;

    private String version;
    private final ObjectArrayList<ShaderDataInstance> includes;
    private final ObjectArrayList<LayoutDataInstance> layoutBlocks;
    private final ObjectArrayList<UniformDataInstance> uniforms;

    public ShaderDataInstance(
            ShaderType shaderType,
            String shaderName,
            File shaderFile) {

        // Internal
        this.shaderType = shaderType;
        this.shaderName = shaderName;
        this.shaderFile = shaderFile;

        this.version = null;
        this.layoutBlocks = new ObjectArrayList<>();
        this.uniforms = new ObjectArrayList<>();
        this.includes = new ObjectArrayList<>();
    }

    // Utility \\

    public ShaderType shaderType() {
        return shaderType;
    }

    String shaderName() {
        return shaderName;
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
    void addIncludes(ShaderDataInstance include) {
        includes.add(include);
    }

    ObjectArrayList<ShaderDataInstance> getIncludes() {
        return includes;
    }

    // Layouts
    public void addLayoutBlock(LayoutDataInstance block) {
        layoutBlocks.add(block);
    }

    public ObjectArrayList<LayoutDataInstance> getLayoutBlocks() {
        return layoutBlocks;
    }

    // Uniforms
    void addUniform(UniformDataInstance uniform) {
        uniforms.add(uniform);
    }

    ObjectArrayList<UniformDataInstance> getUniforms() {
        return uniforms;
    }
}
