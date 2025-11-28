package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderDefinitionInstance extends InstanceFrame {

    // Internal
    public final String shaderName;
    public final ShaderDataInstance vert;
    public final ShaderDataInstance frag;
    private final ObjectArrayList<ShaderDataInstance> includes;

    public ShaderDefinitionInstance(
            String shaderName,
            ShaderDataInstance vert,
            ShaderDataInstance frag) {

        // Internal
        this.shaderName = shaderName;
        this.vert = vert;
        this.frag = frag;
        this.includes = new ObjectArrayList<>();
    }

    // Utility \\

    public void addInclude(ShaderDataInstance include) {
        includes.add(include);
    }

    public ObjectArrayList<ShaderDataInstance> getIncludes() {
        return includes;
    }
}