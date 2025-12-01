package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderDataInstance extends InstanceFrame {

    // Internal
    private final ShaderType shaderType;
    private final String shaderName;
    private final File shaderFile;

    private int version;
    private final ObjectArrayList<LayoutDataInstance> layoutBlocks;
    private final ObjectArrayList<ShaderDataInstance> includes;
    private final ObjectArrayList<UniformDataInstance> uniforms;

    public ShaderDataInstance(
            ShaderType shaderType,
            String shaderName,
            File shaderFile) {

        // Internal
        this.shaderType = shaderType;
        this.shaderName = shaderName;
        this.shaderFile = shaderFile;

        this.version = 0;
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
    int getVersion() {
        return version;
    }

    void setVersion(int version) {
        this.version = version;
    }

    // Layouts
    public void addLayoutBlock(LayoutDataInstance block) {
        layoutBlocks.add(block);
    }

    public ObjectArrayList<LayoutDataInstance> getLayoutBlocks() {
        return layoutBlocks;
    }

    // Includes
    void addIncludes(ShaderDataInstance include) {
        includes.add(include);
    }

    ObjectArrayList<ShaderDataInstance> getIncludes() {
        return includes;
    }

    // Uniforms
    void addUniform(UniformDataInstance uniform) {
        uniforms.add(uniform);
    }

    ObjectArrayList<UniformDataInstance> getUniforms() {
        return uniforms;
    }
}
