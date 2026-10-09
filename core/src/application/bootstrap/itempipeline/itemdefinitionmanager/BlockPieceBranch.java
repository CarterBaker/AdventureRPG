package application.bootstrap.itempipeline.itemdefinitionmanager;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelPartStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.itempipeline.itemdefinition.EquipmentType;
import application.bootstrap.itempipeline.itemdefinition.ItemCategory;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import application.bootstrap.itempipeline.itemdefinition.ItemStat;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
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
     * Its model is generated here and registered as a mesh under the piece's
     * own item name.
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

        String itemName = ItemRegistryUtility.toBlockPieceName(blockHandle.getBlockName());
        SubVoxelModelStruct model = buildModel(blockHandle.getItemTextureName());
        MeshHandle meshHandle = subVoxelManager.createMeshHandle(itemName, model);
        int materialID = materialManager.getMaterialIDFromMaterialName(EngineSetting.DEFAULT_ITEM_MATERIAL);

        ItemDefinitionData itemDefinitionData = new ItemDefinitionData(
                itemName,
                blockHandle.getLocalName(),
                itemDefinitionManager.registerItemName(itemName),
                blockHandle.getLocalName(),
                EngineSetting.BLOCK_PIECE_DESCRIPTION,
                ItemCategory.MATERIAL,
                EngineSetting.BLOCK_PIECE_WEIGHT,
                false,
                true,
                EquipmentType.NONE,
                new float[ItemStat.VALUES.length],
                new ItemShapeStruct(model),
                null,
                meshHandle,
                null,
                null,
                materialID,
                EngineSetting.TOOL_NONE,
                EngineSetting.DEFAULT_TOOL_TIER,
                EngineSetting.BLOCK_PIECE_STACK_SIZE,
                blockHandle.getBlockID(),
                new ObjectArrayList<>(),
                EngineSetting.ITEM_PICK_UP_AS_SELF,
                EngineSetting.ITEM_PLANTS_NONE);

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
