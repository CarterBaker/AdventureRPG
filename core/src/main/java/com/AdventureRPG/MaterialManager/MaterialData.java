package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.badlogic.gdx.graphics.g3d.Material;

import java.util.HashMap;
import java.util.Map;

public class MaterialData {

    public final int id;
    public final String name;
    public final int shaderID;
    public final Material material;

    // Map of uniform name -> value
    public final Map<String, UniformAttribute> uniforms;

    // Map of texture array name -> texture ID
    public final Map<String, Integer> textureIDs;

    public MaterialData(int id, String name, int shaderID, Material material,
            Map<String, UniformAttribute> uniforms,
            Map<String, Integer> textureIDs) {
        this.id = id;
        this.name = name;
        this.shaderID = shaderID;
        this.material = material;
        this.uniforms = uniforms != null ? uniforms : new HashMap<>();
        this.textureIDs = textureIDs != null ? textureIDs : new HashMap<>();
    }
}
