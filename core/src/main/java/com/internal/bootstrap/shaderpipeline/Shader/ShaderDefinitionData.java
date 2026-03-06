package com.internal.bootstrap.shaderpipeline.Shader;

import com.internal.core.engine.DataPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Bootstrap transfer container that assembles a complete shader program definition
 * from a JSON descriptor. Holds references to the vert and frag ShaderData objects
 * and the post-order flattened list of include dependencies used during source
 * preprocessing. Never leaves the shadermanager bootstrap path.
 */
public class ShaderDefinitionData extends DataPackage {

    // Internal
    private String shaderName;
    private ShaderData vert;
    private ShaderData frag;
    private ObjectArrayList<ShaderData> includes;

    // Internal \\

    @Override
    protected void get() {
        this.includes = new ObjectArrayList<>();
    }

    public void constructor(
            String shaderName,
            ShaderData vert,
            ShaderData frag) {
        this.shaderName = shaderName;
        this.vert = vert;
        this.frag = frag;
    }

    // Accessible \\

    public String getShaderName() {
        return shaderName;
    }

    public ShaderData getVert() {
        return vert;
    }

    public ShaderData getFrag() {
        return frag;
    }

    public void addInclude(ShaderData include) {
        includes.add(include);
    }

    public ObjectArrayList<ShaderData> getIncludes() {
        return includes;
    }
}