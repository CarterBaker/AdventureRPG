package editor.runtime.architecturebuilder.view;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinition.ItemShapeStruct;
import editor.bootstrap.architecturebuilderpipeline.architecturebuildermanager.ArchitectureBuilderManager;
import editor.runtime.architecturebuilder.ArchitectureBuilderSetting;
import editor.runtime.viewcamera.ViewCameraSubject;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureBuilderViewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Lays out what the Architecture Builder shows of the layer picked: every
     * slot it takes in, side by side along x a gap apart, a block slot as a
     * square wall of its block's pieces and a furniture slot as the piece of
     * furniture standing there, each placed by the corner of its model grid
     * in blocks. A block slot left as authored shows nothing. The layout is
     * made again only when the builder's revision moves, and holds its
     * bounds for the shared view camera.
     */

    // Internal
    private ArchitectureBuilderManager architectureBuilderManager;

    // Shown — every item drawn and the corner of its model grid, in blocks
    private ObjectArrayList<ItemDefinitionHandle> items;
    private FloatArrayList cornersX;
    private FloatArrayList cornersY;
    private ObjectArrayList<String> slotKeys;
    private int slotCount;
    private float width;
    private float height;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Shown
        this.items = new ObjectArrayList<>();
        this.cornersX = new FloatArrayList();
        this.cornersY = new FloatArrayList();
        this.slotKeys = new ObjectArrayList<>();
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.architectureBuilderManager = get(ArchitectureBuilderManager.class);
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

    // Every slot the layer picked takes in, laid along x from the origin
    private void layOut() {

        items.clear();
        cornersX.clear();
        cornersY.clear();
        slotKeys.clear();
        this.slotCount = 0;
        this.width = 0f;
        this.height = 0f;

        if (architectureBuilderManager.getArchitectureName() == null)
            return;

        architectureBuilderManager.collectPreviewSlots(slotKeys);

        for (int i = 0; i < slotKeys.size(); i++) {

            String slotKey = slotKeys.get(i);
            ItemDefinitionHandle item = architectureBuilderManager.resolvePreviewItem(slotKey);

            if (item == null)
                continue;

            float slotWidth = architectureBuilderManager.isBlockSlot(slotKey) ? layWall(item) : layItem(item);

            this.width += slotWidth + ArchitectureBuilderSetting.PREVIEW_GAP_BLOCKS;
            this.slotCount++;
        }

        this.width = Math.max(0f, width - ArchitectureBuilderSetting.PREVIEW_GAP_BLOCKS);
    }

    // A square wall of a block's pieces standing at the cursor, its width in blocks
    private float layWall(ItemDefinitionHandle piece) {

        float pieceBlocks = (float) EngineSetting.BLOCK_PIECE_SIZE / EngineSetting.SUB_VOXEL_RESOLUTION;
        int pieces = ArchitectureBuilderSetting.PREVIEW_WALL_PIECES;

        for (int column = 0; column < pieces; column++)
            for (int row = 0; row < pieces; row++)
                place(piece, width + column * pieceBlocks, row * pieceBlocks);

        this.height = Math.max(height, pieces * pieceBlocks);

        return pieces * pieceBlocks;
    }

    // A piece of furniture standing at the cursor, its width in blocks
    private float layItem(ItemDefinitionHandle item) {

        ItemShapeStruct shape = item.getShape();
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        place(item, width, 0f);
        this.height = Math.max(height, (shape.getOffsetY() + shape.getSizeY()) / resolution);

        return (shape.getOffsetX() + shape.getSizeX()) / resolution;
    }

    private void place(ItemDefinitionHandle item, float cornerX, float cornerY) {

        items.add(item);
        cornersX.add(cornerX);
        cornersY.add(cornerY);
    }

    // Accessible \\

    // True while anything is shown
    @Override
    public boolean isShown() {
        return !items.isEmpty();
    }

    // The middle of what is shown on an axis, in blocks
    @Override
    public float getCenter(int axis) {
        return switch (axis) {
            case EngineSetting.AXIS_X -> width * 0.5f;
            case EngineSetting.AXIS_Y -> height * 0.5f;
            default -> ArchitectureBuilderSetting.PREVIEW_DEPTH_BLOCKS * 0.5f;
        };
    }

    // Half the diagonal of what is shown, in blocks, never smaller than a block
    @Override
    public float getBoundingRadius() {
        return Math.max(
                ArchitectureBuilderSetting.PREVIEW_MIN_RADIUS_BLOCKS,
                0.5f * (float) Math.sqrt(width * width + height * height
                        + ArchitectureBuilderSetting.PREVIEW_DEPTH_BLOCKS * ArchitectureBuilderSetting.PREVIEW_DEPTH_BLOCKS));
    }

    // Every item drawn, in the order laid
    public ObjectArrayList<ItemDefinitionHandle> getItems() {
        return items;
    }

    public float getCornerX(int index) {
        return cornersX.getFloat(index);
    }

    public float getCornerY(int index) {
        return cornersY.getFloat(index);
    }

    // How many slots show anything
    public int getSlotCount() {
        return slotCount;
    }
}
