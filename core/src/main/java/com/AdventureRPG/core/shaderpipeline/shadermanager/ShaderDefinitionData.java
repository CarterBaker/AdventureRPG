package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderDefinitionData extends DataPackage {

    // Internal
    private String shaderName;
    private ShaderData vert;
    private ShaderData frag;
    private ObjectArrayList<ShaderData> includes;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.includes = new ObjectArrayList<>();
    }

    public void awake(
            String shaderName,
            ShaderData vert,
            ShaderData frag) {

        // Internal
        this.shaderName = shaderName;
        this.vert = vert;
        this.frag = frag;
    }

    // Utility \\

    // Internal
    public String getShaderName() {
        return shaderName;
    }

    public ShaderData getVert() {
        return vert;
    }

    public ShaderData getFrag() {
        return frag;
    }

    // Includes
    public void addInclude(ShaderData include) {
        includes.add(include);
    }

    public ObjectArrayList<ShaderData> getIncludes() {
        return includes;
    }
}