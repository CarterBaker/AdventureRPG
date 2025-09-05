package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.ShaderManager.ShaderData;
import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.AdventureRPG.ShaderManager.UniversalUniform;
import com.AdventureRPG.ShaderManager.UniversalUniformType;
import com.badlogic.gdx.graphics.TextureArray;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.HashMap;
import java.util.Map;

public class MaterialData implements ShaderData {

    public final int id;
    public final String name;

    public final Material material;
    public final TextureArray textureArray;
    public final ShaderProgram shaderProgram;

    public final Map<String, UniformAttribute> uniforms;

    public final UniversalUniform universalUniform;

    public MaterialData(
            int id,
            String name,
            Material material,
            TextureArray textureArray,
            ShaderProgram shaderProgram,
            Map<String, UniformAttribute> uniforms,
            UniversalUniform universalUniform) {

        this.id = id;
        this.name = name;

        this.material = material;
        this.textureArray = textureArray;
        this.shaderProgram = shaderProgram;

        this.uniforms = uniforms != null ? uniforms : new HashMap<>();

        this.universalUniform = universalUniform;
    }

    public void setUniversalUniform(UniversalUniformType type) {
        universalUniform.setUniversalUniform(this, type);
    }

    public void updateUniversalUniforms() {
        setUniversalUniform(UniversalUniformType.u_inverseView);
        setUniversalUniform(UniversalUniformType.u_inverseProjection);
        setUniversalUniform(UniversalUniformType.u_time);
    }

    @Override
    public Map<String, UniformAttribute> getUniforms() {
        return uniforms;
    }
}
