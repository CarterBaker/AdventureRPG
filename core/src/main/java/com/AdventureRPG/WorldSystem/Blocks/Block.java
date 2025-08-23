package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.TextureManager.TextureManager;

public class Block {

    public final String name;
    public final int id;

    public final int upTex;
    public final int northTex;
    public final int southTex;
    public final int eastTex;
    public final int westTex;
    public final int downTex;

    public final int upMat;
    public final int northMat;
    public final int southMat;
    public final int eastMat;
    public final int westMat;
    public final int downMat;

    public final Type type;

    public Block(TextureManager textureManager, MaterialManager materialManager, Builder builder, int id) {

        this.name = builder.name;
        this.id = id;

        if (builder.texture != null) {
            this.upTex = textureManager.getIDFromTexture(builder.texture);
            this.northTex = textureManager.getIDFromTexture(builder.texture);
            this.southTex = textureManager.getIDFromTexture(builder.texture);
            this.eastTex = textureManager.getIDFromTexture(builder.texture);
            this.westTex = textureManager.getIDFromTexture(builder.texture);
            this.downTex = textureManager.getIDFromTexture(builder.texture);
        }

        else {
            this.upTex = textureManager.getIDFromTexture(builder.upTex);
            this.northTex = (builder.northTex != null) ? textureManager.getIDFromTexture(builder.northTex) : this.upTex;
            this.southTex = (builder.southTex != null) ? textureManager.getIDFromTexture(builder.southTex) : this.northTex;
            this.eastTex = (builder.eastTex != null) ? textureManager.getIDFromTexture(builder.eastTex) : this.southTex;
            this.westTex = (builder.westTex != null) ? textureManager.getIDFromTexture(builder.westTex) : this.eastTex;
            this.downTex = (builder.downTex != null) ? textureManager.getIDFromTexture(builder.downTex) : this.westTex;
        }

        if (builder.material != null) {
            this.upMat = textureManager.getIDFromTexture(builder.material);
            this.northMat = textureManager.getIDFromTexture(builder.material);
            this.southMat = textureManager.getIDFromTexture(builder.material);
            this.eastMat = textureManager.getIDFromTexture(builder.material);
            this.westMat = textureManager.getIDFromTexture(builder.material);
            this.downMat = textureManager.getIDFromTexture(builder.material);
        }

        else {
            this.upMat = (builder.upMat != null) ? textureManager.getIDFromTexture(builder.upMat) : this.upTex;
            this.northMat = (builder.northMat != null) ? textureManager.getIDFromTexture(builder.northMat) : this.upMat;
            this.southMat = (builder.southMat != null) ? textureManager.getIDFromTexture(builder.southMat) : this.northMat;
            this.eastMat = (builder.eastMat != null) ? textureManager.getIDFromTexture(builder.eastMat) : this.southMat;
            this.westMat = (builder.westMat != null) ? textureManager.getIDFromTexture(builder.westMat) : this.eastMat;
            this.downMat = (builder.downMat != null) ? textureManager.getIDFromTexture(builder.downMat) : this.westMat;
        }

        this.type = builder.type;
    }
}
