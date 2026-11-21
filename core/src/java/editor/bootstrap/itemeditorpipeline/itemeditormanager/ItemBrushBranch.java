package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.worldpipeline.util.TerrainCarveUtility;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushField;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.itemeditorpipeline.itemdocument.ItemDocumentInstance;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ItemBrushBranch extends BranchPackage {

    /*
     * Works out which sub-voxels the item's tools reach, as a voxel editor's
     * brush does, the edits themselves left to ItemEditBranch. A stamp is the
     * Brushes tab's tip laid flat on the face the pointer struck, sized by the
     * Tools tab's Size in sub-voxels, every column of it as deep as the tip
     * covers it times Strength of the Tools tab's Depth, one layer unless
     * raised, so a soft tip thins toward its feathered edge and a speckled
     * one leaves gaps where its density does; Brush builds the columns
     * outward from the face into empty cells and, inverted, digs them inward
     * through filled ones. A stroke is held to the layer of cells its
     * press began on, the way a voxel editor drags along a face, so building
     * never climbs toward the camera; stamps are laid along it every brush
     * spacing. Fill gathers the run of cubes of one part joined face to face.
     */

    // Internal
    private ItemEditBranch itemEditBranch;
    private ToolManager toolManager;
    private BrushManager brushManager;

    // Stroke — whether one is held, whether it builds or digs, the axis its plane faces along and which way, the
    // layer of cells it is held to and where on that layer the last stamp was laid
    private boolean stroking;
    private boolean strokeAdding;
    private int strokeAxis;
    private int strokeSign;
    private int strokeLayer;
    private int lastU;
    private int lastV;

    // Scratch — the stamp's centre cell, the axis and way the face struck faces, the cells a stamp or fill reaches,
    // a ray's components and the cells a fill has met
    private int[] center;
    private int[] cell;
    private int faceAxis;
    private int faceSign;
    private IntArrayList reachedCells;
    private float[] rayOrigin;
    private float[] rayDirection;
    private BitSet visited;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.center = new int[EngineSetting.AXIS_COUNT];
        this.cell = new int[EngineSetting.AXIS_COUNT];
        this.reachedCells = new IntArrayList();
        this.rayOrigin = new float[EngineSetting.AXIS_COUNT];
        this.rayDirection = new float[EngineSetting.AXIS_COUNT];
        this.visited = new BitSet();
    }

    @Override
    protected void get() {
        this.itemEditBranch = get(ItemEditBranch.class);
        this.toolManager = get(ToolManager.class);
        this.brushManager = get(BrushManager.class);
    }

    // Stroke \\

    // A stroke begun on the face struck, held to its layer, its first stamp laid there
    void beginStroke(ItemDocumentInstance document, boolean adding, SubVoxelHitStruct hit) {

        if (!resolveCenter(adding, hit))
            return;

        this.stroking = true;
        this.strokeAdding = adding;
        this.strokeAxis = faceAxis;
        this.strokeSign = faceSign;
        this.strokeLayer = center[faceAxis];
        this.lastU = center[toUAxis(faceAxis)];
        this.lastV = center[toVAxis(faceAxis)];

        stamp(document);
    }

    // The pointer's ray met with the stroke's layer, a stamp laid every brush spacing from the last one toward it
    void strokeTo(ItemDocumentInstance document, Vector3 origin, Vector3 direction) {

        if (!stroking || !projectOnLayer(origin, direction))
            return;

        int uAxis = toUAxis(strokeAxis);
        int vAxis = toVAxis(strokeAxis);
        float du = cell[uAxis] - lastU;
        float dv = cell[vAxis] - lastV;
        float distance = (float) Math.sqrt(du * du + dv * dv);
        float spacing = BrushUtility.resolveSpacing(brushManager.getActiveBrush(), getRadius());
        int steps = (int) (distance / spacing);

        if (steps == 0)
            return;

        int startU = lastU;
        int startV = lastV;

        this.faceAxis = strokeAxis;
        this.faceSign = strokeSign;

        for (int i = 1; i <= steps; i++) {

            float share = i * spacing / distance;

            center[strokeAxis] = strokeLayer;
            center[uAxis] = startU + Math.round(du * share);
            center[vAxis] = startV + Math.round(dv * share);
            stamp(document);
        }

        this.lastU = center[uAxis];
        this.lastV = center[vAxis];
    }

    void endStroke() {
        this.stroking = false;
    }

    private void stamp(ItemDocumentInstance document) {

        collectStamp(document.getModel(), strokeAdding);
        itemEditBranch.editCells(
                document,
                reachedCells,
                strokeAdding ? document.getSelectedPartIndex() : EngineSetting.INDEX_NOT_FOUND);
    }

    // The cell of the stroke's layer the ray crosses, in cell; false when the ray runs along the layer or away from it
    private boolean projectOnLayer(Vector3 origin, Vector3 direction) {

        toArray(origin, rayOrigin);
        toArray(direction, rayDirection);

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        float along = rayDirection[strokeAxis];

        if (Math.abs(along) < EditorSetting.ITEM_EDITOR_STROKE_PARALLEL_EPSILON)
            return false;

        float distance = ((strokeLayer + EditorSetting.ITEM_EDITOR_CELL_MIDDLE) / resolution
                - rayOrigin[strokeAxis]) / along;

        if (distance < 0f)
            return false;

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            cell[axis] = (int) Math.floor((rayOrigin[axis] + rayDirection[axis] * distance) * resolution);

        cell[strokeAxis] = strokeLayer;
        return true;
    }

    // Preview \\

    // The cells the tool's stamp would reach where the pointer is, laid into the preview as cubes of its one part;
    // false when it would reach none
    boolean buildPreview(
            ItemDocumentInstance document,
            boolean adding,
            SubVoxelHitStruct hit,
            SubVoxelModelStruct preview) {

        preview.clear();

        if (!resolveCenter(adding, hit))
            return false;

        collectStamp(document.getModel(), adding);

        for (int i = 0; i < reachedCells.size(); i++)
            preview.setCellPartAt(reachedCells.getInt(i), 0);

        return !reachedCells.isEmpty();
    }

    // Stamp \\

    // The stamp's centre and the face it lies on: the empty cell before the face struck for Brush, the cube struck for
    // Erase; a ray that only met the floor builds up from it
    private boolean resolveCenter(boolean adding, SubVoxelHitStruct hit) {

        if (adding ? !hit.hasPlacement() : !hit.hasTarget() || hit.isTargetWall())
            return false;

        resolveFace(hit);

        if (adding) {
            center[0] = hit.getPlaceX();
            center[1] = hit.getPlaceY();
            center[2] = hit.getPlaceZ();
        } else {
            center[0] = hit.getTargetX();
            center[1] = hit.getTargetY();
            center[2] = hit.getTargetZ();
        }

        return true;
    }

    // The axis the face struck faces along and which way it faces, out of what was struck toward the pointer
    private void resolveFace(SubVoxelHitStruct hit) {

        this.faceAxis = EngineSetting.AXIS_Y;
        this.faceSign = 1;

        if (!hit.hasTarget() || !hit.hasPlacement())
            return;

        cell[0] = hit.getPlaceX() - hit.getTargetX();
        cell[1] = hit.getPlaceY() - hit.getTargetY();
        cell[2] = hit.getPlaceZ() - hit.getTargetZ();

        if (hit.isTargetWall()) {
            this.faceAxis = hit.getTargetAxis();
            this.faceSign = cell[faceAxis] >= 0 ? 1 : -1;
            return;
        }

        for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
            if (cell[axis] != 0) {
                this.faceAxis = axis;
                this.faceSign = cell[axis];
                return;
            }
    }

    // Every cell the tip reaches about the centre on the face: each column as deep as its coverage times Strength of
    // the Depth reaches, built outward into empty cells or dug inward through filled ones
    private void collectStamp(SubVoxelModelStruct model, boolean adding) {

        reachedCells.clear();

        BrushHandle brush = brushManager.getActiveBrush();
        float radius = getRadius();
        float strength = toolManager.getOption(ToolCanvas.ITEM, ToolOption.STRENGTH);
        float density = brush.getValue(BrushField.DENSITY);
        int reach = (int) Math.ceil(radius);
        int deepest = Math.max(1, Math.round(toolManager.getOption(ToolCanvas.ITEM, ToolOption.DEPTH)));
        int uAxis = toUAxis(faceAxis);
        int vAxis = toVAxis(faceAxis);
        int step = adding ? faceSign : -faceSign;

        for (int dv = -reach; dv <= reach; dv++) {
            for (int du = -reach; du <= reach; du++) {

                float coverage = BrushUtility.resolveCoverage(brush, du, dv, radius) * strength;

                if (coverage <= 0f)
                    continue;

                cell[faceAxis] = center[faceAxis];
                cell[uAxis] = center[uAxis] + du;
                cell[vAxis] = center[vAxis] + dv;

                if (density < 1f && TerrainCarveUtility.rollCell(
                        EditorSetting.ITEM_EDITOR_DENSITY_SEED, EditorSetting.ITEM_EDITOR_DENSITY_SALT,
                        cell[0], cell[1], cell[2]) >= density)
                    continue;

                int depth = Math.max(1, (int) Math.ceil(coverage * deepest));

                for (int layer = 0; layer < depth && model.isInside(cell[0], cell[1], cell[2]); layer++) {

                    if (model.isFilled(cell[0], cell[1], cell[2]) != adding)
                        reachedCells.add(model.toCellIndex(cell[0], cell[1], cell[2]));

                    cell[faceAxis] += step;
                }
            }
        }
    }

    // Fill \\

    // Every cube of the struck cube's part joined to it face to face moved into the selected part; a struck wall alone
    void fill(ItemDocumentInstance document, SubVoxelHitStruct hit) {

        if (!hit.hasTarget())
            return;

        if (hit.isTargetWall()) {
            itemEditBranch.editWall(
                    document, hit.getTargetAxis(), hit.getTargetX(), hit.getTargetY(), hit.getTargetZ(),
                    document.getSelectedPartIndex());
            return;
        }

        SubVoxelModelStruct model = document.getModel();
        int part = model.getCellPart(hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());

        if (part == document.getSelectedPartIndex())
            return;

        collectRun(model, part, hit.getTargetX(), hit.getTargetY(), hit.getTargetZ());
        itemEditBranch.editCells(document, reachedCells, document.getSelectedPartIndex());
    }

    // The run of the part's cubes joined face to face from the cell, walked breadth first through reachedCells
    private void collectRun(SubVoxelModelStruct model, int part, int x, int y, int z) {

        reachedCells.clear();
        visited.clear();

        int first = model.toCellIndex(x, y, z);

        visited.set(first);
        reachedCells.add(first);

        for (int i = 0; i < reachedCells.size(); i++) {

            int index = reachedCells.getInt(i);

            cell[0] = index % model.getSizeX();
            cell[1] = index / model.getSizeX() % model.getSizeY();
            cell[2] = index / (model.getSizeX() * model.getSizeY());

            for (int axis = 0; axis < EngineSetting.AXIS_COUNT; axis++)
                for (int side = -1; side <= 1; side += 2) {

                    cell[axis] += side;
                    addToRun(model, part);
                    cell[axis] -= side;
                }
        }
    }

    private void addToRun(SubVoxelModelStruct model, int part) {

        if (!model.isInside(cell[0], cell[1], cell[2]))
            return;

        int index = model.toCellIndex(cell[0], cell[1], cell[2]);

        if (visited.get(index) || model.getCellPartAt(index) != part)
            return;

        visited.set(index);
        reachedCells.add(index);
    }

    // Utility \\

    private float getRadius() {
        return toolManager.getOption(ToolCanvas.ITEM, ToolOption.SIZE);
    }

    private static int toUAxis(int axis) {
        return (axis + 1) % EngineSetting.AXIS_COUNT;
    }

    private static int toVAxis(int axis) {
        return (axis + 2) % EngineSetting.AXIS_COUNT;
    }

    private static void toArray(Vector3 vector, float[] components) {
        components[0] = vector.x;
        components[1] = vector.y;
        components[2] = vector.z;
    }
}
