package com.AdventureRPG.WorldPipeline.blocks;

import com.AdventureRPG.core.shaders.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaders.materials.Material;
import com.AdventureRPG.core.shaders.texturemanager.TextureManager;
import com.AdventureRPG.core.shaders.texturemanager.UVRect;
import com.AdventureRPG.core.util.Mathematics.Extras.Direction3Int;

public class Block {

    // Game Manager
    private final TextureManager textureManager;
    private final MaterialManager materialManager;

    // Data
    public final String name;
    public final int id;

    // Texture IDs
    public final int upTex, northTex, southTex, eastTex, westTex, downTex;

    // Cached UVRects (pre-fetched, avoids runtime lookups)
    public final UVRect upUV;
    public final UVRect northUV;
    public final UVRect southUV;
    public final UVRect eastUV;
    public final UVRect westUV;
    public final UVRect downUV;

    // Material IDs
    public final int upMat, northMat, southMat, eastMat, westMat, downMat;

    // Cached Material (pre-fetched, avoids runtime lookups)
    public final Material upMaterialData;
    public final Material northMaterialData;
    public final Material southMaterialData;
    public final Material eastMaterialData;
    public final Material westMaterialData;
    public final Material downMaterialData;

    public final Type type;

    public Block(
            TextureManager textureManager,
            MaterialManager materialManager,
            Builder builder,
            int id) {

        // Game Manager
        this.textureManager = textureManager;
        this.materialManager = materialManager;

        // Data
        this.name = builder.name;
        this.id = id;

        // Textures
        if (builder.texture != null) {
            this.upTex = textureManager.getTileIDFromTextureName(builder.texture);
            this.northTex = textureManager.getTileIDFromTextureName(builder.texture);
            this.southTex = textureManager.getTileIDFromTextureName(builder.texture);
            this.eastTex = textureManager.getTileIDFromTextureName(builder.texture);
            this.westTex = textureManager.getTileIDFromTextureName(builder.texture);
            this.downTex = textureManager.getTileIDFromTextureName(builder.texture);
        }

        else {
            this.upTex = textureManager.getTileIDFromTextureName(builder.upTex);
            this.northTex = (builder.northTex != null) ? textureManager.getTileIDFromTextureName(builder.northTex)
                    : this.upTex;
            this.southTex = (builder.southTex != null) ? textureManager.getTileIDFromTextureName(builder.southTex)
                    : this.northTex;
            this.eastTex = (builder.eastTex != null) ? textureManager.getTileIDFromTextureName(builder.eastTex)
                    : this.southTex;
            this.westTex = (builder.westTex != null) ? textureManager.getTileIDFromTextureName(builder.westTex)
                    : this.eastTex;
            this.downTex = (builder.downTex != null) ? textureManager.getTileIDFromTextureName(builder.downTex)
                    : this.westTex;
        }

        // Cache UVRects
        this.upUV = textureManager.getTextureArrayUVfromTileID(upTex);
        this.northUV = textureManager.getTextureArrayUVfromTileID(northTex);
        this.southUV = textureManager.getTextureArrayUVfromTileID(southTex);
        this.eastUV = textureManager.getTextureArrayUVfromTileID(eastTex);
        this.westUV = textureManager.getTextureArrayUVfromTileID(westTex);
        this.downUV = textureManager.getTextureArrayUVfromTileID(downTex);

        // Materials
        if (builder.material != null) {
            this.upMat = materialManager.getMaterialIDFromMaterialName(builder.material);
            this.northMat = materialManager.getMaterialIDFromMaterialName(builder.material);
            this.southMat = materialManager.getMaterialIDFromMaterialName(builder.material);
            this.eastMat = materialManager.getMaterialIDFromMaterialName(builder.material);
            this.westMat = materialManager.getMaterialIDFromMaterialName(builder.material);
            this.downMat = materialManager.getMaterialIDFromMaterialName(builder.material);

            this.upMaterialData = materialManager.getMaterialFromMaterialID(upMat);
            this.northMaterialData = materialManager.getMaterialFromMaterialID(northMat);
            this.southMaterialData = materialManager.getMaterialFromMaterialID(southMat);
            this.eastMaterialData = materialManager.getMaterialFromMaterialID(eastMat);
            this.westMaterialData = materialManager.getMaterialFromMaterialID(westMat);
            this.downMaterialData = materialManager.getMaterialFromMaterialID(downMat);
        }

        else {

            if (builder.upMat != null) {
                this.upMat = materialManager.getMaterialIDFromMaterialName(builder.upMat);
                this.upMaterialData = materialManager.getMaterialFromMaterialID(upMat);
            } else {
                this.upMat = this.upTex;
                this.upMaterialData = materialManager.getMaterialFromMaterialID(upMat);
            }

            if (builder.northMat != null) {
                this.northMat = materialManager.getMaterialIDFromMaterialName(builder.northMat);
                this.northMaterialData = materialManager.getMaterialFromMaterialID(northMat);
            } else {
                this.northMat = this.northTex;
                this.northMaterialData = materialManager.getMaterialFromMaterialID(northMat);
            }

            if (builder.southMat != null) {
                this.southMat = materialManager.getMaterialIDFromMaterialName(builder.southMat);
                this.southMaterialData = materialManager.getMaterialFromMaterialID(southMat);
            } else {
                this.southMat = this.southTex;
                this.southMaterialData = materialManager.getMaterialFromMaterialID(southMat);
            }

            if (builder.eastMat != null) {
                this.eastMat = materialManager.getMaterialIDFromMaterialName(builder.eastMat);
                this.eastMaterialData = materialManager.getMaterialFromMaterialID(eastMat);
            } else {
                this.eastMat = this.eastTex;
                this.eastMaterialData = materialManager.getMaterialFromMaterialID(eastMat);
            }

            if (builder.westMat != null) {
                this.westMat = materialManager.getMaterialIDFromMaterialName(builder.westMat);
                this.westMaterialData = materialManager.getMaterialFromMaterialID(westMat);
            } else {
                this.westMat = this.westTex;
                this.westMaterialData = materialManager.getMaterialFromMaterialID(westMat);
            }

            if (builder.downMat != null) {
                this.downMat = materialManager.getMaterialIDFromMaterialName(builder.downMat);
                this.downMaterialData = materialManager.getMaterialFromMaterialID(downMat);
            } else {
                this.downMat = this.downTex;
                this.downMaterialData = materialManager.getMaterialFromMaterialID(downMat);
            }
        }

        this.type = builder.type;
    }

    public UVRect getUVForSide(Direction3Int side) {

        return switch (side) {
            case UP -> upUV;
            case DOWN -> downUV;
            case NORTH -> northUV;
            case SOUTH -> southUV;
            case EAST -> eastUV;
            case WEST -> westUV;
        };
    }

    public Material getMaterialDataForSide(Direction3Int side) {

        return switch (side) {
            case UP -> upMaterialData;
            case DOWN -> downMaterialData;
            case NORTH -> northMaterialData;
            case SOUTH -> southMaterialData;
            case EAST -> eastMaterialData;
            case WEST -> westMaterialData;
        };
    }

}
