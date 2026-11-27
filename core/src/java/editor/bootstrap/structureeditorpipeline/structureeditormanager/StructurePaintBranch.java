package editor.bootstrap.structureeditorpipeline.structureeditormanager;

import application.bootstrap.contentpipeline.contentmanager.ContentManager;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.SubBlockShape;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import editor.bootstrap.structureeditorpipeline.structureeditor.StructureEditorLayer;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class StructurePaintBranch extends BranchPackage {

    /*
     * Paints one cell of a structure on the Coverage or Contents layer, the
     * rules StructureBuilder holds a structure to applied here, so a cell is
     * only ever painted with what the game will load. A covering grows only
     * over a block it names among its hosts, at the level the Brush's
     * Strength gives; a content fills only a whole block it may be held by.
     * A cell laid in an architecture block slot takes neither, since the
     * block an architecture lays there may host neither. A painted cell is a
     * new entry; one that cannot take the choice is handed back unchanged.
     */

    // Internal
    private BlockManager blockManager;
    private CoveringManager coveringManager;
    private ContentManager contentManager;

    // Base \\

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.contentManager = get(ContentManager.class);
    }

    // Paint \\

    // A cell painted with a covering or a content, emptied of it when the choice is none
    ArpgObjectStruct paint(StructureEditorLayer layer, ArpgObjectStruct entry, String choice, int level) {

        if (choice.isEmpty())
            return clear(layer, entry);

        if (!canTake(layer, entry, choice))
            return entry;

        ArpgObjectStruct painted = entry.deepCopy();
        painted.addProperty(layer.field, choice);

        if (layer == StructureEditorLayer.COVERAGE)
            painted.addProperty("coverage", (long) level);

        return painted;
    }

    // A cell emptied of its covering or its content
    ArpgObjectStruct clear(StructureEditorLayer layer, ArpgObjectStruct entry) {

        if (!entry.has(layer.field))
            return entry;

        ArpgObjectStruct cleared = entry.deepCopy();
        cleared.remove(layer.field);

        if (layer == StructureEditorLayer.COVERAGE)
            cleared.remove("coverage");

        return cleared;
    }

    // Whether a cell's block may take a covering or a content as the game lays them
    boolean canTake(StructureEditorLayer layer, ArpgObjectStruct entry, String choice) {

        BlockHandle blockHandle = resolveBlock(entry);

        if (blockHandle == null || entry.has("slot") || blockHandle.getGeometry() != DynamicGeometryType.FULL)
            return false;

        short blockID = blockHandle.getBlockID();

        if (layer == StructureEditorLayer.COVERAGE)
            return coveringManager.hasCovering(choice)
                    && coveringManager.getCoveringHandleFromCoveringName(choice).canHost(blockID);

        return resolveMask(entry) == SubBlockUtility.MASK_FULL
                && contentManager.hasContent(choice)
                && contentManager.getContentHandleFromContentName(choice).canHost(blockID);
    }

    // Utility \\

    // The block a cell is laid with as authored, null when its name is not a block
    BlockHandle resolveBlock(ArpgObjectStruct entry) {

        String blockName = ArpgUtility.getString(entry, "block", "");

        return blockManager.hasBlock(blockName) ? blockManager.getBlockHandleFromBlockName(blockName) : null;
    }

    // The octants a cell fills: its named shape, its raw mask or the whole cell
    int resolveMask(ArpgObjectStruct entry) {

        if (entry.has("shape"))
            return ArpgUtility.getEnum(entry, "shape", SubBlockShape.class, SubBlockShape.FULL).getMask();

        return ArpgUtility.getInt(entry, "mask", SubBlockUtility.MASK_FULL);
    }
}
