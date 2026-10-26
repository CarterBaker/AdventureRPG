package editor.bootstrap.settlementplannerpipeline.settlementplannermanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.settlementmanager.SettlementManager;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structuremanager.StructureManager;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerArchitectureStruct;
import editor.bootstrap.settlementplannerpipeline.settlementplanner.SettlementPlannerEntryStruct;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SettlementCatalogueBranch extends BranchPackage {

    /*
     * Everything the settlement planner can design: every settlement type,
     * then every structure, each by name, gathered the first time it is asked
     * for and again whenever a settlement type is edited live, since its
     * shown name may change. For the chosen settlement it also lists the
     * architectures that build it, led by the biome's own choice; a structure
     * has none to choose. Lookups by name and kind find an entry again when a
     * placement is brought back into the planner.
     */

    // Internal
    private SettlementManager settlementManager;
    private StructureManager structureManager;
    private ArchitectureManager architectureManager;

    // Catalogue
    private ObjectArrayList<SettlementPlannerEntryStruct> entries;
    private ObjectArrayList<SettlementPlannerArchitectureStruct> architectures;

    // Base \\

    @Override
    protected void create() {
        this.entries = new ObjectArrayList<>();
        this.architectures = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.settlementManager = get(SettlementManager.class);
        this.structureManager = get(StructureManager.class);
        this.architectureManager = get(ArchitectureManager.class);
    }

    // Entries \\

    ObjectArrayList<SettlementPlannerEntryStruct> getEntries() {

        if (entries.isEmpty())
            gatherEntries();

        return entries;
    }

    // Gathered again from the live palettes, keeping their order
    void refreshEntries() {
        entries.clear();
        gatherEntries();
    }

    private void gatherEntries() {

        ObjectArrayList<SettlementHandle> settlements = settlementManager.getSettlementHandles();

        for (int i = 0; i < settlements.size(); i++)
            entries.add(new SettlementPlannerEntryStruct(
                    WorldPlacementKind.SETTLEMENT,
                    settlements.get(i).getSettlementName(),
                    settlements.get(i).getDisplayName()));

        StructureHandle[] structures = structureManager.getStructureHandles();

        for (int i = 0; i < structures.length; i++)
            entries.add(new SettlementPlannerEntryStruct(
                    WorldPlacementKind.STRUCTURE,
                    structures[i].getStructureName(),
                    structures[i].getStructureName()));
    }

    int indexOfEntry(WorldPlacementKind kind, String name) {

        ObjectArrayList<SettlementPlannerEntryStruct> catalogue = getEntries();

        for (int i = 0; i < catalogue.size(); i++)
            if (catalogue.get(i).getKind() == kind && catalogue.get(i).getName().equals(name))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Architectures \\

    // The biome's own choice, then every architecture that builds the entry; none at all for a structure
    void gatherArchitectures(SettlementPlannerEntryStruct entry) {

        architectures.clear();

        if (entry == null || !entry.isSettlement())
            return;

        architectures.add(new SettlementPlannerArchitectureStruct(
                "", EditorSetting.SETTLEMENT_PLANNER_ARCHITECTURE_BIOME));

        short settlementID = settlementManager.getSettlementIDFromSettlementName(entry.getName());
        ObjectArrayList<ArchitectureHandle> handles = architectureManager.getArchitectureHandles();

        for (int i = 0; i < handles.size(); i++)
            if (handles.get(i).buildsSettlement(settlementID))
                architectures.add(new SettlementPlannerArchitectureStruct(
                        handles.get(i).getArchitectureName(), handles.get(i).getDisplayName()));
    }

    ObjectArrayList<SettlementPlannerArchitectureStruct> getArchitectures() {
        return architectures;
    }

    // A settlement no architecture builds cannot stand anywhere, so it is never planned or placed
    boolean isBuildable(SettlementPlannerEntryStruct entry) {
        return !entry.isSettlement() || architectures.size() > 1;
    }

    int indexOfArchitecture(String architectureName) {

        for (int i = 0; i < architectures.size(); i++)
            if (architectures.get(i).getArchitectureName().equals(architectureName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Footprint \\

    // How far the entry stands out from its column: a settlement's lot radius, a structure's reach
    int resolveRadiusBlocks(SettlementPlannerEntryStruct entry) {
        return entry.isSettlement()
                ? settlementManager.getSettlementHandleFromSettlementName(entry.getName()).getRadiusBlocks()
                : structureManager.getStructureHandleFromStructureName(entry.getName()).getHorizontalReachBlocks();
    }
}
