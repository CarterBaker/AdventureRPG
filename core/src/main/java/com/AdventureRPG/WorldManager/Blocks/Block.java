package com.AdventureRPG.WorldManager.Blocks;

import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialData;
import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.TextureManager.TextureManager;
import com.AdventureRPG.Core.RenderPipeline.TextureManager.TextureManager.UVRect;
import com.AdventureRPG.Core.Util.Methematics.Extras.Direction3Int;

public class Block {

    // Game Manager
    private final TextureManager textureManager;
    private final MaterialSystem materialSystem;

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

    // Cached MaterialData (pre-fetched, avoids runtime lookups)
    public final MaterialData upMaterialData;
    public final MaterialData northMaterialData;
    public final MaterialData southMaterialData;
    public final MaterialData eastMaterialData;
    public final MaterialData westMaterialData;
    public final MaterialData downMaterialData;

    public final Type type;

    public Block(
            TextureManager textureManager,
            MaterialSystem materialSystem,
            Builder builder,
            int id) {

        // Game Manager
        this.textureManager = textureManager;
        this.materialSystem = materialSystem;

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
            this.upMat = materialSystem.getByName(builder.material).id;
            this.northMat = materialSystem.getByName(builder.material).id;
            this.southMat = materialSystem.getByName(builder.material).id;
            this.eastMat = materialSystem.getByName(builder.material).id;
            this.westMat = materialSystem.getByName(builder.material).id;
            this.downMat = materialSystem.getByName(builder.material).id;

            this.upMaterialData = materialSystem.getById(upMat);
            this.northMaterialData = materialSystem.getById(northMat);
            this.southMaterialData = materialSystem.getById(southMat);
            this.eastMaterialData = materialSystem.getById(eastMat);
            this.westMaterialData = materialSystem.getById(westMat);
            this.downMaterialData = materialSystem.getById(downMat);
        }

        else {

            if (builder.upMat != null) {
                this.upMat = materialSystem.getByName(builder.upMat).id;
                this.upMaterialData = materialSystem.getById(upMat);
            } else {
                this.upMat = this.upTex;
                this.upMaterialData = materialSystem.getFirstMaterialUsingID(upMat);
            }

            if (builder.northMat != null) {
                this.northMat = materialSystem.getByName(builder.northMat).id;
                this.northMaterialData = materialSystem.getById(northMat);
            } else {
                this.northMat = this.northTex;
                this.northMaterialData = materialSystem.getFirstMaterialUsingID(northMat);
            }

            if (builder.southMat != null) {
                this.southMat = materialSystem.getByName(builder.southMat).id;
                this.southMaterialData = materialSystem.getById(southMat);
            } else {
                this.southMat = this.southTex;
                this.southMaterialData = materialSystem.getFirstMaterialUsingID(southMat);
            }

            if (builder.eastMat != null) {
                this.eastMat = materialSystem.getByName(builder.eastMat).id;
                this.eastMaterialData = materialSystem.getById(eastMat);
            } else {
                this.eastMat = this.eastTex;
                this.eastMaterialData = materialSystem.getFirstMaterialUsingID(eastMat);
            }

            if (builder.westMat != null) {
                this.westMat = materialSystem.getByName(builder.westMat).id;
                this.westMaterialData = materialSystem.getById(westMat);
            } else {
                this.westMat = this.westTex;
                this.westMaterialData = materialSystem.getFirstMaterialUsingID(westMat);
            }

            if (builder.downMat != null) {
                this.downMat = materialSystem.getByName(builder.downMat).id;
                this.downMaterialData = materialSystem.getById(downMat);
            } else {
                this.downMat = this.downTex;
                this.downMaterialData = materialSystem.getFirstMaterialUsingID(downMat);
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

    public MaterialData getMaterialDataForSide(Direction3Int side) {

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
