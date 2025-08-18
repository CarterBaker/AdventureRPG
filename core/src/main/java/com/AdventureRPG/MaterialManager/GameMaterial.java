package com.AdventureRPG.MaterialManager;

import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public final class GameMaterial {
    public final String id; // logical/material name
    public final Material material; // LibGDX material
    public final ShaderProgram shader; // nullable

    public GameMaterial(String id, Material material, ShaderProgram shader) {
        this.id = id;
        this.material = material;
        this.shader = shader;
    }
}
