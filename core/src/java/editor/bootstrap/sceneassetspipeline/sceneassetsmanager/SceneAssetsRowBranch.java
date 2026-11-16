package editor.bootstrap.sceneassetspipeline.sceneassetsmanager;

import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.shaderpipeline.material.MaterialHandle;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetKind;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetRowStruct;
import editor.bootstrap.sceneassetspipeline.sceneassets.SceneAssetSectionStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

class SceneAssetsRowBranch extends BranchPackage {

    /*
     * Turns what the Dev window shows into the Scene Assets tab's rows. The
     * block pointed at is told in full: the block, the biome its chunk lies
     * in, the face the ray hit with its texture, every face's texture and
     * the pack it comes from, the block's material and that material's
     * shader, and the cell it stands in. What a scan saw is told section by
     * section in name order: each block with its material, each biome, each
     * texture its blocks' faces use with its pack and the first block using
     * it, each material with its shader, and each shader with how many of
     * those materials it draws. A block, biome, material or shader row names
     * its content so the Info Panel can open it; a block's file is the part
     * of its name before its own.
     */

    // Internal
    private BlockManager blockManager;
    private BiomeManager biomeManager;
    private TextureManager textureManager;
    private MaterialManager materialManager;

    // Scratch — every texture the seen blocks use with the first block using it and how many do, every material they
    // use, and how many of those each shader draws
    private Int2ObjectOpenHashMap<String> tileID2FirstBlock;
    private Int2IntOpenHashMap tileID2BlockCount;
    private IntOpenHashSet materialIDs;
    private Int2IntOpenHashMap shaderID2MaterialCount;
    private Int2ObjectOpenHashMap<String> shaderID2ShaderName;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.tileID2FirstBlock = new Int2ObjectOpenHashMap<>();
        this.tileID2BlockCount = new Int2IntOpenHashMap();
        this.materialIDs = new IntOpenHashSet();
        this.shaderID2MaterialCount = new Int2IntOpenHashMap();
        this.shaderID2ShaderName = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.textureManager = get(TextureManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Pointed \\

    // Everything about the block the ray hit, its biome given
    void describePointed(SceneAssetSectionStruct section, BlockCastStruct cast, BiomeHandle biome) {

        BlockHandle block = cast.getBlock();
        Direction3Vector face = cast.getHitFace();

        section.clear();
        section.add(blockRow(EditorSetting.SCENE_ASSETS_ROW_BLOCK, block));
        section.add(biomeRow(EditorSetting.SCENE_ASSETS_ROW_BIOME, biome));
        section.add(textRow(EditorSetting.SCENE_ASSETS_ROW_FACE, String.format(
                EditorSetting.SCENE_ASSETS_FORMAT_FACE, ArpgUtility.toEnumName(face), describeTile(block, face))));

        for (int i = 0; i < Direction3Vector.VALUES.length; i++) {

            Direction3Vector each = Direction3Vector.VALUES[i];

            section.add(textRow(String.format(EditorSetting.SCENE_ASSETS_FORMAT_FACE_TEXTURE,
                    ArpgUtility.toEnumName(each)), describeTile(block, each)));
        }

        if (block.getMaterialID() != EngineSetting.INDEX_NOT_FOUND) {

            MaterialHandle material = materialManager.getMaterialHandleFromMaterialID(block.getMaterialID());

            section.add(materialRow(EditorSetting.SCENE_ASSETS_ROW_MATERIAL, material));
            section.add(shaderRow(EditorSetting.SCENE_ASSETS_ROW_SHADER, material.getShaderHandle().getShaderName(),
                    material.getShaderHandle().getShaderName()));
        }

        section.add(textRow(EditorSetting.SCENE_ASSETS_ROW_CELL, String.format(EditorSetting.SCENE_ASSETS_FORMAT_CELL,
                Coordinate2Long.unpackX(cast.getChunkCoordinate()) * EngineSetting.CHUNK_SIZE + cast.getBlockX(),
                cast.getSubChunkY() * EngineSetting.CHUNK_SIZE + cast.getBlockY(),
                Coordinate2Long.unpackY(cast.getChunkCoordinate()) * EngineSetting.CHUNK_SIZE + cast.getBlockZ())));
    }

    // The ray met nothing within reach
    void describeNothing(SceneAssetSectionStruct section) {
        section.clear();
        section.add(textRow(EditorSetting.SCENE_ASSETS_TEXT_NOTHING_POINTED, EditorSetting.SCENE_ASSETS_TEXT_EMPTY));
    }

    // Seen \\

    // Every section but the pointed block's filled again from the blocks and biomes the scan saw
    void describeSeen(SceneAssetSectionStruct[] sections, ShortOpenHashSet blockIDs, ShortOpenHashSet biomeIDs) {

        SceneAssetSectionStruct blocks = sections[SceneAssetKind.BLOCKS.ordinal()];
        SceneAssetSectionStruct biomes = sections[SceneAssetKind.BIOMES.ordinal()];

        blocks.clear();
        biomes.clear();
        tileID2FirstBlock.clear();
        tileID2BlockCount.clear();
        materialIDs.clear();

        for (ShortIterator it = blockIDs.iterator(); it.hasNext(); ) {

            BlockHandle block = blockManager.getBlockHandleFromBlockID(it.nextShort());

            blocks.add(blockRow(block.getBlockName(), block));
            gatherBlock(block);
        }

        for (ShortIterator it = biomeIDs.iterator(); it.hasNext(); ) {

            BiomeHandle biome = biomeManager.getBiomeHandleFromBiomeID(it.nextShort());

            biomes.add(biomeRow(biome.getBiomeName(), biome));
        }

        describeTextures(sections[SceneAssetKind.TEXTURES.ordinal()]);
        describeMaterials(sections[SceneAssetKind.MATERIALS.ordinal()], sections[SceneAssetKind.SHADERS.ordinal()]);

        for (int i = 0; i < sections.length; i++)
            if (sections[i].getKind() != SceneAssetKind.POINTED)
                sections[i].sort();
    }

    // The block's textures, each counted once per block, and its material
    private void gatherBlock(BlockHandle block) {

        for (int i = 0; i < Direction3Vector.VALUES.length; i++) {

            int tileID = block.getTextureForFace(Direction3Vector.VALUES[i]);

            if (tileID == EngineSetting.INDEX_NOT_FOUND || isTileOnEarlierFace(block, i, tileID))
                continue;

            tileID2FirstBlock.putIfAbsent(tileID, block.getBlockName());
            tileID2BlockCount.addTo(tileID, 1);
        }

        if (block.getMaterialID() != EngineSetting.INDEX_NOT_FOUND)
            materialIDs.add(block.getMaterialID());
    }

    private boolean isTileOnEarlierFace(BlockHandle block, int face, int tileID) {

        for (int i = 0; i < face; i++)
            if (block.getTextureForFace(Direction3Vector.VALUES[i]) == tileID)
                return true;

        return false;
    }

    private void describeTextures(SceneAssetSectionStruct textures) {

        textures.clear();

        for (Int2IntOpenHashMap.Entry entry : tileID2BlockCount.int2IntEntrySet()) {

            TextureHandle texture = textureManager.getTextureHandleFromTileID(entry.getIntKey());
            String firstBlock = tileID2FirstBlock.get(entry.getIntKey());
            int count = entry.getIntValue();

            textures.add(textRow(texture.getTileName(), count > 1
                    ? String.format(EditorSetting.SCENE_ASSETS_FORMAT_TEXTURE_BLOCKS,
                            texture.getArrayName(), firstBlock, count - 1)
                    : String.format(EditorSetting.SCENE_ASSETS_FORMAT_TEXTURE_BLOCK,
                            texture.getArrayName(), firstBlock)));
        }
    }

    private void describeMaterials(SceneAssetSectionStruct materials, SceneAssetSectionStruct shaders) {

        materials.clear();
        shaders.clear();
        shaderID2MaterialCount.clear();
        shaderID2ShaderName.clear();

        for (int materialID : materialIDs) {

            MaterialHandle material = materialManager.getMaterialHandleFromMaterialID(materialID);
            int shaderID = material.getShaderHandle().getShaderID();

            materials.add(materialRow(material.getMaterialName(), material));
            shaderID2MaterialCount.addTo(shaderID, 1);
            shaderID2ShaderName.put(shaderID, material.getShaderHandle().getShaderName());
        }

        for (Int2IntOpenHashMap.Entry entry : shaderID2MaterialCount.int2IntEntrySet())
            shaders.add(shaderRow(shaderID2ShaderName.get(entry.getIntKey()), shaderID2ShaderName.get(
                    entry.getIntKey()), String.format(EditorSetting.SCENE_ASSETS_FORMAT_SHADER_MATERIALS,
                    entry.getIntValue())));
    }

    // Rows \\

    // The texture a face is drawn with and the pack it comes from, or none for an untextured face
    private String describeTile(BlockHandle block, Direction3Vector face) {

        int tileID = block.getTextureForFace(face);

        if (tileID == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.SCENE_ASSETS_TEXT_NO_TEXTURE;

        TextureHandle texture = textureManager.getTextureHandleFromTileID(tileID);

        return String.format(EditorSetting.SCENE_ASSETS_FORMAT_TILE, texture.getTileName(), texture.getArrayName());
    }

    // A block's row: its file is its name before its own, its own the entry within that file
    private SceneAssetRowStruct blockRow(String label, BlockHandle block) {

        String blockName = block.getBlockName();
        int separator = blockName.lastIndexOf(EngineSetting.PATH_SEPARATOR);

        return new SceneAssetRowStruct(
                label,
                label.equals(blockName) ? describeMaterial(block) : blockName,
                EditorSetting.INFO_SCHEMA_BLOCKS,
                separator < 0 ? blockName : blockName.substring(0, separator),
                block.getLocalName());
    }

    private String describeMaterial(BlockHandle block) {
        return block.getMaterialID() != EngineSetting.INDEX_NOT_FOUND
                ? materialManager.getMaterialHandleFromMaterialID(block.getMaterialID()).getMaterialName()
                : EditorSetting.SCENE_ASSETS_TEXT_EMPTY;
    }

    private SceneAssetRowStruct biomeRow(String label, BiomeHandle biome) {
        return new SceneAssetRowStruct(
                label,
                label.equals(biome.getBiomeName()) ? EditorSetting.SCENE_ASSETS_TEXT_EMPTY : biome.getBiomeName(),
                EditorSetting.INFO_SCHEMA_BIOMES,
                biome.getBiomeName(),
                biome.getBiomeName());
    }

    private SceneAssetRowStruct materialRow(String label, MaterialHandle material) {
        return new SceneAssetRowStruct(
                label,
                label.equals(material.getMaterialName())
                        ? material.getShaderHandle().getShaderName()
                        : material.getMaterialName(),
                EditorSetting.INFO_SCHEMA_MATERIALS,
                material.getMaterialName(),
                material.getMaterialName());
    }

    private SceneAssetRowStruct shaderRow(String label, String shaderName, String value) {
        return new SceneAssetRowStruct(label, value, EditorSetting.INFO_SCHEMA_SHADERS, shaderName, shaderName);
    }

    private SceneAssetRowStruct textRow(String label, String value) {
        return new SceneAssetRowStruct(label, value, null, null, null);
    }
}
