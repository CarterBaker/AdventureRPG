package editor.runtime.placementicon;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.geometrypipeline.modelmanager.ModelManager;
import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.settlementplannerpipeline.settlementplannermanager.SettlementPlannerManager;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PlacementIconSystem extends SystemPackage {

    /*
     * Draws one simple icon per hand placement for whichever view of the
     * world its context shows, at the window position the view works out: a
     * house, upright, for a settlement and a banner turned the way it faces
     * for a structure, each in its own color, and the placement the
     * Settlement Planner is editing larger and in its highlight. Icon quads
     * are pooled with their own materials and grow only to the most icons a
     * frame has needed; begin() hands the pool out again each frame.
     */

    // Internal
    private MeshManager meshManager;
    private ModelManager modelManager;
    private MaterialManager materialManager;
    private RenderManager renderManager;
    private SettlementPlannerManager settlementPlannerManager;

    // Models
    private MeshHandle quadMesh;
    private int iconMaterialID;
    private ObjectArrayList<ModelInstance> iconModels;
    private int drawn;

    // Icon
    private Vector2 iconCenter;
    private Vector2 iconDirection;
    private Vector2 iconScale;
    private Vector4 settlementColor;
    private Vector4 structureColor;
    private Vector4 editedColor;

    // Base \\

    @Override
    protected void create() {

        // Models
        this.iconModels = new ObjectArrayList<>();

        // Icon
        this.iconCenter = new Vector2();
        this.iconDirection = new Vector2();
        this.iconScale = new Vector2();
        this.settlementColor = toColor(EditorSetting.PLACEMENT_ICON_COLOR_SETTLEMENT);
        this.structureColor = toColor(EditorSetting.PLACEMENT_ICON_COLOR_STRUCTURE);
        this.editedColor = toColor(EditorSetting.PLACEMENT_ICON_COLOR_EDITED);
    }

    @Override
    protected void get() {
        this.meshManager = get(MeshManager.class);
        this.modelManager = get(ModelManager.class);
        this.materialManager = get(MaterialManager.class);
        this.renderManager = get(RenderManager.class);
        this.settlementPlannerManager = get(SettlementPlannerManager.class);
    }

    @Override
    protected void awake() {
        this.quadMesh = meshManager.getMeshHandleFromMeshName(EditorSetting.PLACEMENT_ICON_MESH);
        this.iconMaterialID = materialManager.getMaterialIDFromMaterialName(EditorSetting.PLACEMENT_ICON_MATERIAL);
    }

    // Draw \\

    public void begin() {
        drawn = 0;
    }

    // One placement's icon centred on a window position, y up, skipped when it falls off the window
    public void push(
            WindowInstance window,
            FBOInstance fbo,
            int depth,
            float screenX,
            float screenY,
            WorldPlacementStruct placement) {

        boolean edited = settlementPlannerManager.isEdited(placement);
        boolean settlement = placement.getKind() == WorldPlacementKind.SETTLEMENT;
        float width = window.getWidth();
        float height = window.getHeight();
        float size = edited
                ? EditorSetting.PLACEMENT_ICON_EDITED_SIZE_PIXELS
                : EditorSetting.PLACEMENT_ICON_SIZE_PIXELS;

        if (screenX < -size || screenY < -size || screenX > width + size || screenY > height + size)
            return;

        iconCenter.set(screenX / width * 2f - 1f, screenY / height * 2f - 1f);
        iconScale.set(size / width * 2f, size / height * 2f);

        // A house stands upright; a banner faces its structure's front, world z running down the window
        Direction3Vector facing = settlement
                ? Direction3Vector.SOUTH
                : StructurePlacementUtility.rotateDirection(Direction3Vector.NORTH, placement.getQuarterTurns());

        iconDirection.set(facing.x, -facing.z);

        MaterialInstance material = acquireIconModel(drawn).getMaterial();
        material.setUniform(EditorSetting.PLACEMENT_ICON_UNIFORM_CENTER, iconCenter);
        material.setUniform(EditorSetting.PLACEMENT_ICON_UNIFORM_DIRECTION, iconDirection);
        material.setUniform(EditorSetting.PLACEMENT_ICON_UNIFORM_SCALE, iconScale);
        material.setUniform(
                EditorSetting.PLACEMENT_ICON_UNIFORM_SHAPE,
                settlement
                        ? EditorSetting.PLACEMENT_ICON_SHAPE_SETTLEMENT
                        : EditorSetting.PLACEMENT_ICON_SHAPE_STRUCTURE);
        material.setUniform(
                EditorSetting.PLACEMENT_ICON_UNIFORM_FILL_COLOR,
                edited ? editedColor : settlement ? settlementColor : structureColor);

        renderManager.pushRenderCall(iconModels.get(drawn), fbo, depth, window);
        drawn++;
    }

    private ModelInstance acquireIconModel(int index) {

        if (index < iconModels.size())
            return iconModels.get(index);

        ModelInstance iconModel = modelManager.createModel(quadMesh, materialManager.cloneMaterial(iconMaterialID));
        iconModels.add(iconModel);

        return iconModel;
    }

    // Utility \\

    private Vector4 toColor(float[] channels) {
        return new Vector4(channels[0], channels[1], channels[2], channels[3]);
    }
}
