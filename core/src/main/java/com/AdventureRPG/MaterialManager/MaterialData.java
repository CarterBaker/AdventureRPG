package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.HashMap;
import java.util.Map;

public class MaterialData {

    public final int id;
    public final String name;

    public final Material material;
    public final TextureArray textureArray;
    public final ShaderProgram shaderProgram;

    // Map of uniform name -> value
    public final Map<String, UniformAttribute> uniforms;

    public MaterialData(
            int id,
            String name,
            Material material,
            TextureArray textureArray,
            ShaderProgram shaderProgram,
            Map<String, UniformAttribute> uniforms) {

        this.id = id;
        this.name = name;

        this.material = material;
        this.textureArray = textureArray;
        this.shaderProgram = shaderProgram;

        this.uniforms = uniforms != null ? uniforms : new HashMap<>();
    }
}
