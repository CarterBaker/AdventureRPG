package com.AdventureRPG.worldmanager.blocks;

import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialSystem;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.AdventureRPG.worldmanager.WorldManager;

public class Builder {

    // Reference
    public String name = null;

    // Texture
    public String texture = null;

    public String upTex = null;
    public String northTex = null;
    public String southTex = null;
    public String eastTex = null;
    public String westTex = null;
    public String downTex = null;

    // Texture
    public String material = null;

    public String upMat = null;
    public String northMat = null;
    public String southMat = null;
    public String eastMat = null;
    public String westMat = null;
    public String downMat = null;

    // State
    public Type type;

    // Base \\

    public Builder name(String name) {
        this.name = name;
        return this;
    }

    public Builder texture(String texture) {
        this.texture = texture;
        return this;
    }

    public Builder upTex(String upTex) {
        this.upTex = upTex;
        return this;
    }

    public Builder northTex(String northTex) {
        this.northTex = northTex;
        return this;
    }

    public Builder southTex(String southTex) {
        this.southTex = southTex;
        return this;
    }

    public Builder eastTex(String eastTex) {
        this.eastTex = eastTex;
        return this;
    }

    public Builder westTex(String westTex) {
        this.westTex = westTex;
        return this;
    }

    public Builder downTex(String downTex) {
        this.downTex = downTex;
        return this;
    }

    public Builder material(String material) {
        this.material = material;
        return this;
    }

    public Builder upMat(String upMat) {
        this.upMat = upMat;
        return this;
    }

    public Builder northMat(String northMat) {
        this.northMat = northMat;
        return this;
    }

    public Builder southMat(String southMat) {
        this.southMat = southMat;
        return this;
    }

    public Builder eastMat(String eastMat) {
        this.eastMat = eastMat;
        return this;
    }

    public Builder westMat(String westMat) {
        this.westMat = westMat;
        return this;
    }

    public Builder downMat(String downMat) {
        this.downMat = downMat;
        return this;
    }

    public Builder type(Type type) {
        this.type = type;
        return this;
    }

    public Block build(
            TextureManager textureManager,
            MaterialSystem materialSystem,
            int id) {
        return new Block(
                textureManager,
                materialSystem,
                this,
                id);
    }
}
