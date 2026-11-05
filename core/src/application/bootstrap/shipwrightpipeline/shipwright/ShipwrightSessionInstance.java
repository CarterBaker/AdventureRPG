package application.bootstrap.shipwrightpipeline.shipwright;

import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightSessionInstance extends InstancePackage {

    /*
     * One drafting table in use: where the ships drawn at it are kept, the
     * blueprint on the sheet and the name it was last saved under, and the
     * history every change can be undone and redone through, one stroke or
     * press at a time. It holds the catalogue's tab open, the option each
     * tab last held, the tool in hand and what it works on — the deck picked,
     * the wall, half wall, railing or lip painted, which opening, cover or
     * gun platform is set down, whether a climb is a stair, a
     * ladder or a boarding ladder, the mast picked, the furniture item and
     * the quarter turn something is set down at — and the cursor, the octant
     * of the sheet the pointer or the movement bindings last put it on, with
     * whatever stands under it and why the thing in hand could not be set
     * down there, worked out again whenever either changes. The
     * blueprint compiles into this session's own layout whenever it changes,
     * and is surveyed once it has rested, and the sheet and the top and side
     * views are drawn into its own pixels: the sheet whenever anything shown
     * changes, the views whenever it compiles, each with a revision its viewer
     * uploads by; the cursor is laid over the sheet as it is drawn on screen.
     * A name being typed is kept apart from the blueprint until it is
     * confirmed.
     */

    // Storage
    private ShipwrightStorage storage;

    // Blueprint
    private ShipBlueprintStruct blueprint;
    private String savedName;
    private boolean dirty;

    // History
    private ObjectArrayList<ShipBlueprintStruct> undoStack;
    private ObjectArrayList<ShipBlueprintStruct> redoStack;

    // Stroke — the blueprint as it stood before the stroke under way began, and the octant it last worked
    private ShipBlueprintStruct strokeSnapshot;
    private boolean strokeErasing;
    private boolean strokeChanged;
    private int strokeU;
    private int strokeV;
    private int strokeSteps;

    // Tool — the tab open, the entry each tab last held by the tab's ordinal, and the tool in hand
    private ShipwrightTab tab;
    private int[] tabEntries;
    private ShipwrightTool tool;
    private int deck;
    private ShipWall wall;
    private ShipOpening opening;
    private ShipClimb climb;
    private int mastIndex;
    private int itemIndex;
    private int spin;

    // Cursor — the octant of the sheet the cursor stands on
    private int cursorU;
    private int cursorV;

    // Inspect — whatever stands under the cursor, and why the thing in hand could not be set down there
    private ShipPickStruct hover;
    private String placementProblem;

    // Compile
    private ShipLayoutStruct layout;
    private ShipSurveyStruct survey;
    private int revision;

    // Survey — due once the blueprint has rested since it last compiled
    private boolean surveyDue;
    private float surveyCountdown;

    // Status
    private String status;

    // Naming
    private boolean naming;
    private StringBuilder nameBuffer;

    // Sheet — RGBA texels, rows from the bottom up
    private byte[] sheetPixels;
    private int sheetWidth;
    private int sheetHeight;

    // Views — RGBA texels, rows from the bottom up
    private byte[] sidePixels;
    private byte[] topPixels;
    private int sideWidth;
    private int sideHeight;
    private int topWidth;
    private int topHeight;
    private int viewRevision;

    // Internal \\

    @Override
    protected void create() {

        // History
        this.undoStack = new ObjectArrayList<>();
        this.redoStack = new ObjectArrayList<>();

        // Compile
        this.layout = new ShipLayoutStruct(new ShipOctantGridStruct());
        this.survey = new ShipSurveyStruct();

        // Inspect
        this.hover = new ShipPickStruct();

        // Tool
        this.tabEntries = new int[ShipwrightTab.VALUES.length];
        this.placementProblem = EngineSetting.SHIP_PROBLEM_NONE;

        // Naming
        this.nameBuffer = new StringBuilder();

        // Sheet
        this.sheetPixels = new byte[EngineSetting.SHIPWRIGHT_SHEET_TEXELS_X * EngineSetting.SHIPWRIGHT_SHEET_TEXELS_Y
                * EngineSetting.COLOR_CHANNEL_COUNT];

        // Views
        this.sidePixels = new byte[EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X * EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y
                * EngineSetting.COLOR_CHANNEL_COUNT];
        this.topPixels = new byte[EngineSetting.SHIPWRIGHT_TOP_TEXELS_X * EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y
                * EngineSetting.COLOR_CHANNEL_COUNT];
    }

    // Constructor \\

    public void constructor(ShipwrightStorage storage, ShipBlueprintStruct blueprint) {

        // Storage
        this.storage = storage;

        // Tool
        Arrays.fill(tabEntries, EngineSetting.INDEX_NOT_FOUND);
        this.tab = ShipwrightTab.STRUCTURE;
        this.tool = ShipwrightTool.DECK;
        this.wall = ShipWall.FULL;
        this.opening = ShipOpening.DOOR;
        this.climb = ShipClimb.STAIR;
        this.itemIndex = 0;
        this.spin = 0;

        // Cursor
        this.cursorU = EngineSetting.SHIP_AFT_MARGIN + EngineSetting.SHIP_SHEET_LENGTH / 2;
        this.cursorV = EngineSetting.SHIP_SHEET_HALF_BREADTH;

        // Status
        this.status = EngineSetting.SHIPWRIGHT_STATUS_NONE;

        load(blueprint, EngineSetting.SHIP_NAME_NONE);
    }

    // Blueprint \\

    // Puts a blueprint on the sheet, forgetting the one before and all its history
    public void load(ShipBlueprintStruct blueprint, String savedName) {

        this.blueprint = blueprint;
        this.savedName = savedName;
        this.dirty = false;
        this.strokeSnapshot = null;
        this.deck = 0;
        this.mastIndex = EngineSetting.INDEX_NOT_FOUND;
        this.naming = false;
        undoStack.clear();
        redoStack.clear();
        markChanged();
    }

    public void markSaved(String savedName) {
        this.savedName = savedName;
        this.dirty = false;
    }

    // Every change to the blueprint, or to what is shown of it, moves the revision on
    public void markChanged() {
        revision++;
    }

    public void markDirty() {
        this.dirty = true;
        markChanged();
    }

    // History \\

    public void pushUndo(ShipBlueprintStruct snapshot) {

        undoStack.add(snapshot);
        redoStack.clear();

        if (undoStack.size() > EngineSetting.SHIPWRIGHT_UNDO_DEPTH)
            undoStack.remove(0);
    }

    // The blueprint as it stood before the last change, the present kept to redo — false when there is none
    public boolean undo() {

        if (undoStack.isEmpty())
            return false;

        redoStack.add(blueprint.copy());
        blueprint.set(undoStack.remove(undoStack.size() - 1));
        markDirty();

        return true;
    }

    public boolean redo() {

        if (redoStack.isEmpty())
            return false;

        undoStack.add(blueprint.copy());
        blueprint.set(redoStack.remove(redoStack.size() - 1));
        markDirty();

        return true;
    }

    // Takes back the last change without keeping it to redo, as when it would not build
    public void discardLast() {

        if (undoStack.isEmpty())
            return;

        blueprint.set(undoStack.remove(undoStack.size() - 1));
        markChanged();
    }

    // Stroke \\

    public void beginStroke(boolean erasing) {

        this.strokeSnapshot = blueprint.copy();
        this.strokeErasing = erasing;
        this.strokeChanged = false;
        this.strokeU = EngineSetting.INDEX_NOT_FOUND;
        this.strokeV = EngineSetting.INDEX_NOT_FOUND;
        this.strokeSteps = 0;
    }

    // Moves the stroke onto an octant, false when it already lies there
    public boolean moveStroke(int u, int v) {

        if (u == strokeU && v == strokeV)
            return false;

        this.strokeU = u;
        this.strokeV = v;
        strokeSteps++;

        return true;
    }

    // True while the stroke works the octant it began on
    public boolean isStrokeStart() {
        return strokeSteps == 1;
    }

    public void markStrokeChanged() {
        this.strokeChanged = true;
        markChanged();
    }

    public boolean isStrokeChanged() {
        return strokeChanged;
    }

    public boolean isStroking() {
        return strokeSnapshot != null;
    }

    public boolean isStrokeErasing() {
        return strokeErasing;
    }

    // The blueprint as it stood when the stroke began, the stroke left finished
    public ShipBlueprintStruct endStroke() {

        ShipBlueprintStruct snapshot = strokeSnapshot;
        this.strokeSnapshot = null;

        return snapshot;
    }

    // Tool \\

    // Takes up a tool, its tab open
    public void setTool(ShipwrightTab tab, ShipwrightTool tool) {
        this.tab = tab;
        this.tool = tool;
        markChanged();
    }

    public void setTab(ShipwrightTab tab) {
        this.tab = tab;
        markChanged();
    }

    // Keeps the entry a tab held, to take up again when the tab is opened
    public void setTabEntry(ShipwrightTab tab, int entry) {
        tabEntries[tab.ordinal()] = entry;
    }

    public void setDeck(int deck) {
        this.deck = deck;
        markChanged();
    }

    public void setWall(ShipWall wall) {
        this.wall = wall;
        markChanged();
    }

    public void setOpening(ShipOpening opening) {
        this.opening = opening;
        markChanged();
    }

    public void setClimb(ShipClimb climb) {
        this.climb = climb;
        markChanged();
    }

    public void setMastIndex(int mastIndex) {
        this.mastIndex = mastIndex;
        markChanged();
    }

    public void setItemIndex(int itemIndex) {
        this.itemIndex = itemIndex;
        markChanged();
    }

    public void setSpin(int spin) {
        this.spin = spin;
        markChanged();
    }

    // Cursor \\

    // Moves the cursor onto an octant of the sheet, true when it moved
    public boolean moveCursor(int u, int v) {

        if (u == cursorU && v == cursorV)
            return false;

        this.cursorU = u;
        this.cursorV = v;
        markChanged();

        return true;
    }

    // Inspect \\

    // Why the thing in hand could not be set down under the cursor, SHIP_PROBLEM_NONE when it could
    public void setPlacementProblem(String placementProblem) {
        this.placementProblem = placementProblem;
    }

    // Status \\

    public void setStatus(String status) {
        this.status = status;
        markChanged();
    }

    // Naming \\

    public void beginNaming() {

        this.naming = true;
        nameBuffer.setLength(0);
        nameBuffer.append(blueprint.getDisplayName());
        markChanged();
    }

    public void endNaming() {
        this.naming = false;
        markChanged();
    }

    // Survey \\

    public void scheduleSurvey(float delay) {
        this.surveyDue = true;
        this.surveyCountdown = delay;
    }

    // Counts the rest down, true once a survey is due and the blueprint has rested long enough
    public boolean tickSurvey(float deltaTime) {

        if (!surveyDue)
            return false;

        surveyCountdown -= deltaTime;

        return surveyCountdown <= 0f;
    }

    public void clearSurvey() {
        this.surveyDue = false;
    }

    public boolean isSurveyDue() {
        return surveyDue;
    }

    // Draw \\

    public void setSheetSize(int sheetWidth, int sheetHeight) {
        this.sheetWidth = sheetWidth;
        this.sheetHeight = sheetHeight;
    }

    public void setViewSizes(int sideWidth, int sideHeight, int topWidth, int topHeight) {
        this.sideWidth = sideWidth;
        this.sideHeight = sideHeight;
        this.topWidth = topWidth;
        this.topHeight = topHeight;
        viewRevision++;
    }

    // Accessible \\

    public ShipwrightStorage getStorage() {
        return storage;
    }

    public ShipBlueprintStruct getBlueprint() {
        return blueprint;
    }

    public String getSavedName() {
        return savedName;
    }

    public boolean isSaved() {
        return !savedName.isEmpty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public ShipwrightTab getTab() {
        return tab;
    }

    // The entry a tab last held, INDEX_NOT_FOUND before it held any
    public int getTabEntry(ShipwrightTab tab) {
        return tabEntries[tab.ordinal()];
    }

    public ShipwrightTool getTool() {
        return tool;
    }

    // The deck every tool works on, always one of the blueprint's
    public int getDeck() {
        return Math.min(deck, blueprint.getDeckCount() - 1);
    }

    // The wall the walls tool paints: full or half height, or a railing
    public ShipWall getWall() {
        return wall;
    }

    public ShipOpening getOpening() {
        return opening;
    }

    public ShipClimb getClimb() {
        return climb;
    }

    public int getMastIndex() {
        return mastIndex;
    }

    public boolean hasMastPicked() {
        return mastIndex >= 0 && mastIndex < blueprint.getMasts().size();
    }

    // The furniture item picked: the helm, the capstan, then every furnishing table in the drafting table's list
    public int getItemIndex() {
        return itemIndex;
    }

    public int getSpin() {
        return spin;
    }

    public int getCursorU() {
        return cursorU;
    }

    public int getCursorV() {
        return cursorV;
    }

    public ShipPickStruct getHover() {
        return hover;
    }

    public String getPlacementProblem() {
        return placementProblem;
    }

    public ShipLayoutStruct getLayout() {
        return layout;
    }

    public ShipSurveyStruct getSurvey() {
        return survey;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatus() {
        return status;
    }

    public boolean isNaming() {
        return naming;
    }

    public StringBuilder getNameBuffer() {
        return nameBuffer;
    }

    public byte[] getSheetPixels() {
        return sheetPixels;
    }

    public int getSheetWidth() {
        return sheetWidth;
    }

    public int getSheetHeight() {
        return sheetHeight;
    }

    public byte[] getSidePixels() {
        return sidePixels;
    }

    public byte[] getTopPixels() {
        return topPixels;
    }

    public int getSideWidth() {
        return sideWidth;
    }

    public int getSideHeight() {
        return sideHeight;
    }

    public int getTopWidth() {
        return topWidth;
    }

    public int getTopHeight() {
        return topHeight;
    }

    public int getViewRevision() {
        return viewRevision;
    }
}
