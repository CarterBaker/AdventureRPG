package application.bootstrap.entitypipeline.placementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.inventory.EquipmentSlot;
import application.bootstrap.entitypipeline.inventory.InventoryHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.physicspipeline.util.BlockCastStruct;
import application.bootstrap.worldpipeline.blockmanager.BlockPlacementSystem;
import application.bootstrap.worldpipeline.covering.CoveringHandle;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CoverageBranch extends BranchPackage {

    /*
     * Tends coverings for PlacementManager. A sower in the main hand lays its
     * covering over the block it is used on when the block hosts it and is
     * bare, or grows the same covering already there, by the levels it
     * nurtures; an item that only nurtures, a moisture pouch, grows whatever
     * already covers the block. Either takes one of the held item, but only
     * once the coverage has changed, so a fully grown cell costs nothing. A
     * whole block broken under a fully grown covering hands out the
     * covering's drop. Every coverage edit goes through BlockPlacementSystem.
     */

    // Internal
    private BlockPlacementSystem blockPlacementSystem;
    private CoveringManager coveringManager;
    private ItemManager itemManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.blockPlacementSystem = get(BlockPlacementSystem.class);
        this.coveringManager = get(CoveringManager.class);
        this.itemManager = get(ItemManager.class);
    }

    // Tend \\

    // The sower or nurturer in the main hand used on the block that was hit — true when its coverage changed
    boolean tryTend(EntityInstance entity, BlockCastStruct castStruct) {

        InventoryHandle inventoryHandle = entity.getInventoryHandle();
        ItemDefinitionHandle held = inventoryHandle.getMainHand().getItemDefinitionHandle();
        short coverage = blockPlacementSystem.getCoverage(castStruct);
        short tended = held.isSower()
                ? resolveSown(held, castStruct, coverage)
                : resolveNurtured(held, coverage);

        if (tended == coverage || !blockPlacementSystem.editCoverage(castStruct, tended))
            return false;

        inventoryHandle.takeOne(EquipmentSlot.MAIN_HAND);

        return true;
    }

    // The sower's covering laid or grown on the block, the coverage unchanged when the block refuses it
    private short resolveSown(ItemDefinitionHandle held, BlockCastStruct castStruct, short coverage) {

        CoveringHandle coveringHandle = coveringManager.getCoveringHandleFromCoveringName(held.getSowsCoveringName());

        if (!coveringHandle.canHost(castStruct.getBlock().getBlockID()))
            return coverage;

        if (CoverageUtility.isCovered(coverage)
                && CoverageUtility.getCoveringID(coverage) != coveringHandle.getCoveringID())
            return coverage;

        return CoverageUtility.pack(
                coveringHandle.getCoveringID(), CoverageUtility.getLevel(coverage) + held.getNurtureLevels());
    }

    // Whatever covers the block grown by the levels the item nurtures, a bare block left bare
    private short resolveNurtured(ItemDefinitionHandle held, short coverage) {

        if (!CoverageUtility.isCovered(coverage))
            return coverage;

        return CoverageUtility.addLevels(coverage, held.getNurtureLevels());
    }

    // Drop \\

    // The item a fully grown covering over the struck cell hands out when its block breaks whole
    String resolveDrop(BlockCastStruct castStruct) {

        short coverage = blockPlacementSystem.getCoverage(castStruct);
        CoveringHandle coveringHandle = coveringManager.getCoveringHandleFromCoverage(coverage);

        if (coveringHandle == null || !coveringHandle.hasDrop() || !CoverageUtility.isFull(coverage))
            return EngineSetting.COVERING_DROP_NONE;

        return coveringHandle.getDropItemName();
    }

    // The drop handed to the breaker when their inventory has room for it, lost otherwise like the covering itself
    void giveDrop(EntityInstance entity, String dropItemName) {

        if (dropItemName.equals(EngineSetting.COVERING_DROP_NONE))
            return;

        entity.getInventoryHandle().give(itemManager.createItem(dropItemName));
    }
}
