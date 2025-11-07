package com.AdventureRPG.WorldManager.Blocks;

import com.AdventureRPG.MaterialSystem.MaterialData;
import com.AdventureRPG.MaterialSystem.MaterialSystem;
import com.AdventureRPG.TextureSystem.TextureSystem;
import com.AdventureRPG.TextureSystem.TextureSystem.UVRect;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldManager.WorldManager;

public class Block {

    // Game Manager
    private final TextureSystem textureSystem;
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

    public Block(WorldManager worldManager, Builder builder, int id) {

        // Game Manager
        this.textureSystem = worldManager.textureSystem;
        this.materialSystem = worldManager.materialSystem;

        // Data
        this.name = builder.name;
        this.id = id;

        // Textures
        if (builder.texture != null) {
            this.upTex = textureSystem.getIDFromTexture(builder.texture);
            this.northTex = textureSystem.getIDFromTexture(builder.texture);
            this.southTex = textureSystem.getIDFromTexture(builder.texture);
            this.eastTex = textureSystem.getIDFromTexture(builder.texture);
            this.westTex = textureSystem.getIDFromTexture(builder.texture);
            this.downTex = textureSystem.getIDFromTexture(builder.texture);
        }

        else {
            this.upTex = textureSystem.getIDFromTexture(builder.upTex);
            this.northTex = (builder.northTex != null) ? textureSystem.getIDFromTexture(builder.northTex) : this.upTex;
            this.southTex = (builder.southTex != null) ? textureSystem.getIDFromTexture(builder.southTex)
                    : this.northTex;
            this.eastTex = (builder.eastTex != null) ? textureSystem.getIDFromTexture(builder.eastTex) : this.southTex;
            this.westTex = (builder.westTex != null) ? textureSystem.getIDFromTexture(builder.westTex) : this.eastTex;
            this.downTex = (builder.downTex != null) ? textureSystem.getIDFromTexture(builder.downTex) : this.westTex;
        }

        // Cache UVRects
        this.upUV = textureSystem.getUVRect(upTex);
        this.northUV = textureSystem.getUVRect(northTex);
        this.southUV = textureSystem.getUVRect(southTex);
        this.eastUV = textureSystem.getUVRect(eastTex);
        this.westUV = textureSystem.getUVRect(westTex);
        this.downUV = textureSystem.getUVRect(downTex);

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
