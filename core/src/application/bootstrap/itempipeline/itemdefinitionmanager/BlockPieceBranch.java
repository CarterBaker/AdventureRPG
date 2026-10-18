package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.contentpipeline.content.ContentHandle;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemStation;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class BlockPieceBranch extends BranchPackage {

    /*
     * Builds the block piece of a breakable block: the small item a broken
     * sub-block becomes, a cube BLOCK_PIECE_SIZE sub-voxels wide drawn with
     * the block's item texture, stacking up to BLOCK_PIECE_STACK_SIZE. A piece
     * only ever lives in a container; in the world it is a sub-block again.
     * A content piece — the ore, rock or root a whole block hands out in
     * place of its own pieces — is built the same way from the content's
     * item texture, but builds nothing back. Every model is generated here
     * and registered as a mesh under the piece's own item name.
     */

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private SubVoxelManager subVoxelManager;
    private MaterialManager materialManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    ItemDefinitionHandle build(BlockHandle blockHandle) {

        if (!blockHandle.hasPiece())
            return throwException("Block '" + blockHandle.getBlockName()
                    + "' has no block piece — only breakable solid blocks with an item texture break into pieces.");

        return buildPiece(
                ItemRegistryUtility.toBlockPieceName(blockHandle.getBlockName()),
                blockHandle.getLocalName(),
                blockHandle.getItemTextureName(),
                EngineSetting.BLOCK_PIECE_DESCRIPTION,
                blockHandle.getBlockID());
    }

    ItemDefinitionHandle buildContentPiece(ContentHandle contentHandle) {

        if (!contentHandle.hasPiece())
            return throwException("Content '" + contentHandle.getContentName()
                    + "' has no content piece — only a content with an item texture breaks into pieces.");

        return buildPiece(
                ItemRegistryUtility.toContentPieceName(contentHandle.getContentName()),
                contentHandle.getDisplayName(),
                contentHandle.getItemTextureName(),
                EngineSetting.CONTENT_PIECE_DESCRIPTION,
                EngineSetting.BLOCK_PIECE_NONE);
    }

    // A cube of the item texture under the piece's own item name, building the block it names, if any
    private ItemDefinitionHandle buildPiece(
            String itemName,
            String localName,
            String itemTextureName,
            String description,
            short blockID) {

        SubVoxelModelStruct model = buildModel(itemTextureName);
        MeshHandle meshHandle = subVoxelManager.createMeshHandle(itemName, model);
        int materialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.DEFAULT_ITEM_MATERIAL);

        ItemDefinitionData itemDefinitionData = new ItemDefinitionData(
                itemName,
                localName,
                itemDefinitionManager.registerItemName(itemName),
                localName,
                description,
                ItemCategory.MATERIAL,
                EngineSetting.BLOCK_PIECE_WEIGHT,
                false,
                true,
                EquipmentType.NONE,
                ValueMapStruct.EMPTY,
                new ItemShapeStruct(model),
                null,
                meshHandle,
                null,
                null,
                materialID,
                EngineSetting.TOOL_NONE,
                EngineSetting.DEFAULT_TOOL_TIER,
                EngineSetting.WEAPON_NONE,
                EngineSetting.SKILL_NONE,
                EngineSetting.BLOCK_PIECE_STACK_SIZE,
                blockID,
                new ObjectArrayList<>(),
                EngineSetting.ITEM_PICK_UP_AS_SELF,
                EngineSetting.ITEM_PLANTS_NONE,
                EngineSetting.ITEM_SOWS_NONE,
                EngineSetting.ITEM_NURTURES_NONE,
                ItemStation.NONE);

        ItemDefinitionHandle item = create(ItemDefinitionHandle.class);
        item.constructor(itemDefinitionData);

        return item;
    }

    // Model \\

    private SubVoxelModelStruct buildModel(String textureName) {

        SubVoxelModelStruct model = new SubVoxelModelStruct();
        int partIndex = model.addPart(new SubVoxelPartStruct(EngineSetting.BLOCK_PIECE_PART_NAME, textureName));
        int size = EngineSetting.BLOCK_PIECE_SIZE;

        for (int z = 0; z < size; z++)
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                    model.setCell(x, y, z, partIndex);

        return model;
    }
}
