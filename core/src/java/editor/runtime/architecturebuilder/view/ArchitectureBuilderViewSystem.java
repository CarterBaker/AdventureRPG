package editor.runtime.architecturebuilder.view;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import editor.runtime.architecturebuilder.ArchitectureBuilderSetting;
import editor.runtime.voxelpreview.VoxelPreviewSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureBuilderViewSystem extends SystemPackage {

    /*
     * Lays out what the Architecture Builder shows of the layer picked into
     * the window's VoxelPreviewSystem: every slot it takes in, side by side
     * along x a gap apart, a block slot as a square wall of its block's
     * pieces and a furniture slot as the piece of furniture standing there,
     * each stood by the corner of its model grid. A block slot left as
     * authored shows nothing. The layout is made again only when the
     * builder's revision moves.
     */

    // Internal
    private ArchitectureBuilderManager architectureBuilderManager;
    private VoxelPreviewSystem voxelPreviewSystem;

    // Layout — the slots the layer takes in, and how far along x the next one stands
    private ObjectArrayList<String> slotKeys;
    private float cursorX;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Layout
        this.slotKeys = new ObjectArrayList<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.architectureBuilderManager = get(ArchitectureBuilderManager.class);
        this.voxelPreviewSystem = get(VoxelPreviewSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        int revision = architectureBuilderManager.getRevision();

        if (revision == shownRevision)
            return;

        this.shownRevision = revision;
        layOut();
    }

    // Every slot the layer picked takes in, laid along x from the corner
    private void layOut() {

        voxelPreviewSystem.begin();
        slotKeys.clear();
        this.cursorX = 0f;

        if (architectureBuilderManager.getArchitectureName() != null)
            architectureBuilderManager.collectPreviewSlots(slotKeys);

        for (int i = 0; i < slotKeys.size(); i++) {

            String slotKey = slotKeys.get(i);
            ItemDefinitionHandle item = architectureBuilderManager.resolvePreviewItem(slotKey);

            if (item == null)
                continue;

            float width = architectureBuilderManager.isBlockSlot(slotKey) ? layWall(item) : layItem(item);

            this.cursorX += width + ArchitectureBuilderSetting.PREVIEW_GAP_BLOCKS;
        }

        voxelPreviewSystem.finish();
    }

    // A square wall of a block's pieces standing at the cursor, its width in blocks
    private float layWall(ItemDefinitionHandle piece) {

        float pieceBlocks = (float) EngineSetting.BLOCK_PIECE_SIZE / EngineSetting.SUB_VOXEL_RESOLUTION;
        int pieces = ArchitectureBuilderSetting.PREVIEW_WALL_PIECES;

        for (int column = 0; column < pieces; column++)
            for (int row = 0; row < pieces; row++)
                voxelPreviewSystem.addItem(
                        piece, cursorX + column * pieceBlocks, row * pieceBlocks, 0f,
                        EngineSetting.DEFAULT_BLOCK_ORIENTATION);

        return pieces * pieceBlocks;
    }

    // A piece of furniture standing at the cursor, its width in blocks
    private float layItem(ItemDefinitionHandle item) {

        ItemShapeStruct shape = item.getShape();

        voxelPreviewSystem.addItem(item, cursorX, 0f, 0f, EngineSetting.DEFAULT_BLOCK_ORIENTATION);

        return (shape.getOffsetX() + shape.getSizeX()) / (float) EngineSetting.SUB_VOXEL_RESOLUTION;
    }
}
