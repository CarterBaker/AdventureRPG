package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.TextureManager.TextureManager;
import com.AdventureRPG.TextureManager.TextureManager.UVRect;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class Block {

    // Game Manager
    private final TextureManager textureManager;
    private final MaterialManager materialManager;

    // Data
    public final String name;
    public final int id;

    // Texture IDs
    public final int upTex;
    public final int northTex;
    public final int southTex;
    public final int eastTex;
    public final int westTex;
    public final int downTex;

    // Cached UVRects (pre-fetched, avoids runtime lookups)
    public final UVRect upUV;
    public final UVRect northUV;
    public final UVRect southUV;
    public final UVRect eastUV;
    public final UVRect westUV;
    public final UVRect downUV;

    // Material IDs
    public final int upMat;
    public final int northMat;
    public final int southMat;
    public final int eastMat;
    public final int westMat;
    public final int downMat;

    // Cached MaterialData (pre-fetched, avoids runtime lookups)
    public final MaterialData upMaterialData;
    public final MaterialData northMaterialData;
    public final MaterialData southMaterialData;
    public final MaterialData eastMaterialData;
    public final MaterialData westMaterialData;
    public final MaterialData downMaterialData;

    public final Type type;

    public Block(WorldSystem worldSystem, Builder builder, int id) {

        // Game Manager
        this.textureManager = worldSystem.textureManager;
        this.materialManager = worldSystem.materialManager;

        // Data
        this.name = builder.name;
        this.id = id;

        // Textures
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
            this.southTex = (builder.southTex != null) ? textureManager.getIDFromTexture(builder.southTex)
                    : this.northTex;
            this.eastTex = (builder.eastTex != null) ? textureManager.getIDFromTexture(builder.eastTex) : this.southTex;
            this.westTex = (builder.westTex != null) ? textureManager.getIDFromTexture(builder.westTex) : this.eastTex;
            this.downTex = (builder.downTex != null) ? textureManager.getIDFromTexture(builder.downTex) : this.westTex;
        }

        // Cache UVRects
        this.upUV = textureManager.getUVRect(upTex);
        this.northUV = textureManager.getUVRect(northTex);
        this.southUV = textureManager.getUVRect(southTex);
        this.eastUV = textureManager.getUVRect(eastTex);
        this.westUV = textureManager.getUVRect(westTex);
        this.downUV = textureManager.getUVRect(downTex);

        // Materials
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
            this.southMat = (builder.southMat != null) ? textureManager.getIDFromTexture(builder.southMat)
                    : this.northMat;
            this.eastMat = (builder.eastMat != null) ? textureManager.getIDFromTexture(builder.eastMat) : this.southMat;
            this.westMat = (builder.westMat != null) ? textureManager.getIDFromTexture(builder.westMat) : this.eastMat;
            this.downMat = (builder.downMat != null) ? textureManager.getIDFromTexture(builder.downMat) : this.westMat;
        }

        // Cache MaterialData
        this.upMaterialData = materialManager.getById(upMat);
        this.northMaterialData = materialManager.getById(northMat);
        this.southMaterialData = materialManager.getById(southMat);
        this.eastMaterialData = materialManager.getById(eastMat);
        this.westMaterialData = materialManager.getById(westMat);
        this.downMaterialData = materialManager.getById(downMat);

        this.type = builder.type;
    }

    public int getTexIDForSide(Direction3Int side) {

        return switch (side) {
            case UP -> upTex;
            case DOWN -> downTex;
            case NORTH -> northTex;
            case SOUTH -> southTex;
            case EAST -> eastTex;
            case WEST -> westTex;
        };
    }

    public UVRect getUVForSide(Direction3Int side) {

        int texID = switch (side) {
            case UP -> upTex;
            case DOWN -> downTex;
            case NORTH -> northTex;
            case SOUTH -> southTex;
            case EAST -> eastTex;
            case WEST -> westTex;
        };

        return textureManager.getUVRect(texID);
    }

    public int getMatIDForSide(Direction3Int side) {

        return switch (side) {
            case UP -> upMat;
            case DOWN -> downMat;
            case NORTH -> northMat;
            case SOUTH -> southMat;
            case EAST -> eastMat;
            case WEST -> westMat;
        };
    }

    public MaterialData getMaterialDataForSide(Direction3Int side) {

        int matID = switch (side) {
            case UP -> upMat;
            case DOWN -> downMat;
            case NORTH -> northMat;
            case SOUTH -> southMat;
            case EAST -> eastMat;
            case WEST -> westMat;
        };

        return materialManager.getById(matID);
    }
}
