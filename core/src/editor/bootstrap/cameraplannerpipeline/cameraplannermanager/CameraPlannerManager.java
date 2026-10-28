package editor.bootstrap.cameraplannerpipeline.cameraplannermanager;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CameraPlannerManager extends ManagerPackage {

    /*
     * Owns the camera planner's shared design, the camera the World Editor
     * stands wherever its Place tool clicks while it places cameras: the
     * name it is told apart by, each new camera taking the next free number,
     * and its view, the height above the ground it looks out from and its
     * yaw and pitch. A placed, selected or picked
     * camera is edited from then on: every change to its name or view, and
     * every move to exact coordinates, is applied to it live through the
     * World Editor, and it is pinned through PlayerManager, so the main menu
     * of every Preview tab looks out from it as it is edited. Every camera
     * of the active world is listed to pick from; the main menu picks one of
     * them at random each time it opens. Deselect lets the camera go, and
     * every preview with it.
     */

    // Internal
    private WorldManager worldManager;
    private PlayerManager playerManager;
    private WorldEditorManager worldEditorManager;
    private ToolManager toolManager;
    private TabManager tabManager;

    // Design
    private String name;
    private float height;
    private float yaw;
    private float pitch;

    // World
    private WorldHandle worldHandle;
    private WorldPlacementStruct editedPlacement;
    private int shownPlacementRevision;

    // Cameras
    private ObjectArrayList<WorldPlacementStruct> cameras;

    // Status
    private String statusMessage;
    private int revision;
    private int rowRevision;

    // Base \\

    @Override
    protected void create() {

        // Design
        this.name = EditorSetting.CAMERA_PLANNER_DEFAULT_NAME;
        this.height = EditorSetting.CAMERA_PLANNER_DEFAULT_HEIGHT;
        this.yaw = EditorSetting.CAMERA_PLANNER_DEFAULT_YAW;
        this.pitch = EditorSetting.CAMERA_PLANNER_DEFAULT_PITCH;

        // World
        this.shownPlacementRevision = EngineSetting.INDEX_NOT_FOUND;

        // Cameras
        this.cameras = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.playerManager = get(PlayerManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.toolManager = get(ToolManager.class);
        this.tabManager = get(TabManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        syncWorld();
        syncPlacements();
    }

    // The planner designs for the active world, nothing of another world kept
    private void syncWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        if (activeWorld == worldHandle)
            return;

        this.worldHandle = activeWorld;
        setEditedPlacement(null);
        this.shownPlacementRevision = EngineSetting.INDEX_NOT_FOUND;
        notifyChanged();
    }

    // A camera reloaded or cleared away from under the planner is let go, and the world's cameras gathered again
    private void syncPlacements() {

        int placementRevision = worldManager.getPlacementRevision();

        if (placementRevision == shownPlacementRevision)
            return;

        this.shownPlacementRevision = placementRevision;

        WorldPlacementStruct[] placements = worldManager.getPlacements(worldHandle);

        cameras.clear();

        for (int i = 0; i < placements.length; i++)
            if (placements[i].getKind() == WorldPlacementKind.CAMERA)
                cameras.add(placements[i]);

        if (editedPlacement != null && !cameras.contains(editedPlacement))
            detach();

        rowRevision++;
        notifyChanged();
    }

    // Placing \\

    // The World Editor set to place cameras, with the Place tool when a World Editor is the tools' target
    public void place() {

        worldEditorManager.showCameras();

        if (toolManager.getCanvas() == ToolCanvas.CAMERAS)
            toolManager.selectTool(EditorTool.PLACE);

        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_PLACING);
    }

    // A new camera of the design's view standing at a world column, named apart from every camera of the world
    public WorldPlacementStruct createNewPlacement(long worldX, long worldZ) {

        int number = cameras.size() + 1;

        while (isNameTaken(toDefaultName(number)))
            number++;

        this.name = toDefaultName(number);

        return createPlacement(worldX, worldZ);
    }

    private String toDefaultName(int number) {
        return EditorSetting.CAMERA_PLANNER_DEFAULT_NAME + EditorSetting.CAMERA_PLANNER_NAME_SEPARATOR + number;
    }

    private boolean isNameTaken(String cameraName) {

        for (int i = 0; i < cameras.size(); i++)
            if (cameras.get(i).getName().equals(cameraName))
                return true;

        return false;
    }

    // The design standing at a world column
    private WorldPlacementStruct createPlacement(long worldX, long worldZ) {
        return new WorldPlacementStruct(
                WorldPlacementKind.CAMERA,
                name,
                "",
                EngineSetting.WORLD_PLACEMENT_LAYOUT_CELL,
                worldX,
                worldZ,
                0,
                height,
                yaw,
                pitch);
    }

    // Preview \\

    // A Preview tab opened beside the planner, its main menu looking out from the edited camera, or else a random one
    public void preview(WindowInstance window) {

        tabManager.openPreview(window.getGLWindow());
        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_PREVIEWING
                + (editedPlacement != null ? editedPlacement.getName() : EditorSetting.CAMERA_PLANNER_PREVIEW_RANDOM));
    }

    // Selection \\

    public void selectCamera(int index) {

        if (index < 0 || index >= cameras.size())
            return;

        worldEditorManager.showCameras();
        editPlacement(cameras.get(index));
    }

    // A standing camera brought into the planner, every later change to the design applied to it live
    public void editPlacement(WorldPlacementStruct placement) {

        loadDesign(placement);
        setEditedPlacement(placement);
        rowRevision++;
        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_EDITING + placement.getName());
    }

    // The camera being edited let go, and every preview with it; the design stays as it was
    public void detach() {

        if (editedPlacement == null)
            return;

        setEditedPlacement(null);
        rowRevision++;
        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_RELEASED);
    }

    // The single call site of every change to the edited camera, pinned for every Preview tab's main menu
    private void setEditedPlacement(WorldPlacementStruct placement) {
        this.editedPlacement = placement;
        playerManager.pinMenuCamera(placement);
    }

    private void loadDesign(WorldPlacementStruct placement) {
        this.name = placement.getName();
        this.height = placement.getHeight();
        this.yaw = placement.getYaw();
        this.pitch = placement.getPitch();
    }

    // True while a camera is edited; otherwise says so in the status line
    public boolean requireEditedPlacement() {

        if (editedPlacement != null)
            return true;

        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_NOTHING_EDITED);

        return false;
    }

    public void removeEditedPlacement() {

        if (!requireEditedPlacement())
            return;

        WorldPlacementStruct removed = editedPlacement;
        String refusal = worldEditorManager.removePlacement(removed);

        if (refusal != null) {
            setStatusMessage(refusal);
            return;
        }

        setEditedPlacement(null);
        rowRevision++;
        setStatusMessage(EditorSetting.CAMERA_PLANNER_MESSAGE_REMOVED + removed.getName());
    }

    // View \\

    public void stepHeight(int step) {
        this.height = clampHeight(height + step * EditorSetting.CAMERA_PLANNER_HEIGHT_STEP);
        applyDesign();
    }

    public void stepYaw(int step) {
        this.yaw = wrapYaw(yaw + step * EditorSetting.CAMERA_PLANNER_YAW_STEP);
        applyDesign();
    }

    public void stepPitch(int step) {
        this.pitch = clampPitch(pitch + step * EditorSetting.CAMERA_PLANNER_PITCH_STEP);
        applyDesign();
    }

    public boolean isNameValid(String text) {
        return !text.trim().isEmpty();
    }

    public boolean isValueValid(String text) {

        try {
            return Float.isFinite(Float.parseFloat(text.trim()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setName(String text) {

        if (!isNameValid(text))
            return;

        this.name = text.trim();
        applyDesign();
    }

    public void setHeight(String text) {

        if (!isValueValid(text))
            return;

        this.height = clampHeight(Float.parseFloat(text.trim()));
        applyDesign();
    }

    public void setYaw(String text) {

        if (!isValueValid(text))
            return;

        this.yaw = wrapYaw(Float.parseFloat(text.trim()));
        applyDesign();
    }

    public void setPitch(String text) {

        if (!isValueValid(text))
            return;

        this.pitch = clampPitch(Float.parseFloat(text.trim()));
        applyDesign();
    }

    // The design's values written so the text dialog can start from them
    public String getName() {
        return name;
    }

    public String formatHeight() {
        return Float.toString(height);
    }

    public String formatYaw() {
        return Float.toString(yaw);
    }

    public String formatPitch() {
        return Float.toString(pitch);
    }

    private float clampHeight(float value) {
        return Math.max(0f, Math.min(EngineSetting.MENU_CAMERA_HEIGHT_MAX, value));
    }

    // A yaw held to one turn, from 0 up to but never reaching a full turn
    private float wrapYaw(float value) {

        float wrapped = value % EngineSetting.MENU_CAMERA_YAW_TURN;
        wrapped = wrapped < 0f ? wrapped + EngineSetting.MENU_CAMERA_YAW_TURN : wrapped;

        return wrapped < EngineSetting.MENU_CAMERA_YAW_TURN ? wrapped : 0f;
    }

    private float clampPitch(float value) {
        return Math.max(
                -EngineSetting.CAMERA_MAX_PITCH_DEGREES,
                Math.min(EngineSetting.CAMERA_MAX_PITCH_DEGREES, value));
    }

    // Placement \\

    public boolean isCoordinateValid(String text) {

        if (editedPlacement == null)
            return false;

        try {
            Long.parseLong(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void setEditedX(String text) {
        if (isCoordinateValid(text))
            reportMove(applyDesignAt(Long.parseLong(text.trim()), editedPlacement.getWorldZ()));
    }

    public void setEditedZ(String text) {
        if (isCoordinateValid(text))
            reportMove(applyDesignAt(editedPlacement.getWorldX(), Long.parseLong(text.trim())));
    }

    private void reportMove(String refusal) {
        setStatusMessage(refusal != null ? refusal : EditorSetting.CAMERA_PLANNER_MESSAGE_MOVED);
    }

    // The edited camera's coordinates written so the coordinate dialog can start from them
    public String formatEditedX() {
        return editedPlacement != null ? Long.toString(editedPlacement.getWorldX()) : "";
    }

    public String formatEditedZ() {
        return editedPlacement != null ? Long.toString(editedPlacement.getWorldZ()) : "";
    }

    // Design \\

    // A changed design applied to the camera being edited; refused, the design goes back to what stands
    private void applyDesign() {

        if (editedPlacement == null) {
            notifyChanged();
            return;
        }

        String refusal = applyDesignAt(editedPlacement.getWorldX(), editedPlacement.getWorldZ());

        if (refusal == null)
            return;

        loadDesign(editedPlacement);
        setStatusMessage(refusal);
    }

    // The design swapped in for the edited camera at a block column, returning why it was refused or null
    private String applyDesignAt(long worldX, long worldZ) {

        WorldPlacementStruct design = createPlacement(worldX, worldZ);
        String refusal = worldEditorManager.replacePlacement(editedPlacement, design);

        if (refusal == null)
            setEditedPlacement(design);

        rowRevision++;
        notifyChanged();

        return refusal;
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    private void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder(describeDesign());

        if (statusMessage != null)
            status.append(EditorSetting.CAMERA_PLANNER_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    // What the design is called and how it looks out, for the planner's and the World Editor's status lines
    public String describeDesign() {
        return name + EditorSetting.CAMERA_PLANNER_STATUS_SEPARATOR + describeView();
    }

    public String describeView() {
        return EditorSetting.CAMERA_PLANNER_STATUS_HEIGHT + formatValue(height)
                + EditorSetting.CAMERA_PLANNER_STATUS_YAW + formatValue(yaw)
                + EditorSetting.CAMERA_PLANNER_STATUS_PITCH + formatValue(pitch);
    }

    // The edited camera and where it stands, or how to have one
    public String describeEdited() {
        return editedPlacement != null
                ? describePlacement(editedPlacement)
                : EditorSetting.CAMERA_PLANNER_STATUS_NOTHING_EDITED;
    }

    // A camera's name and exact block column
    public String describePlacement(WorldPlacementStruct placement) {
        return placement.getName() + EditorSetting.CAMERA_PLANNER_STATUS_COLUMN_X + placement.getWorldX()
                + EditorSetting.CAMERA_PLANNER_STATUS_Z + placement.getWorldZ();
    }

    // How many cameras the main menu picks from
    public String describeCount() {
        return cameras.isEmpty()
                ? EditorSetting.CAMERA_PLANNER_STATUS_NONE
                : EditorSetting.CAMERA_PLANNER_STATUS_COUNT + cameras.size();
    }

    private String formatValue(float value) {
        return String.format(EditorSetting.CAMERA_PLANNER_VALUE_FORMAT, value);
    }

    // Accessible \\

    public ObjectArrayList<WorldPlacementStruct> getCameras() {
        return cameras;
    }

    public boolean isEdited(WorldPlacementStruct placement) {
        return placement == editedPlacement;
    }

    public int getRevision() {
        return revision;
    }

    public int getRowRevision() {
        return rowRevision;
    }
}
