package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderDefinitionData extends DataPackage {

    // Internal
    public final String shaderName;
    public final ShaderData vert;
    public final ShaderData frag;
    private final ObjectArrayList<ShaderData> includes;

    public ShaderDefinitionData(
            String shaderName,
            ShaderData vert,
            ShaderData frag) {

        // Internal
        this.shaderName = shaderName;
        this.vert = vert;
        this.frag = frag;
        this.includes = new ObjectArrayList<>();
    }

    // Utility \\

    public void addInclude(ShaderData include) {
        includes.add(include);
    }

    public ObjectArrayList<ShaderData> getIncludes() {
        return includes;
    }
}