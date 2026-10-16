package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightSessionInstance extends InstancePackage {

    /*
     * One drafting table in use: where the ships drawn at it are kept, the
     * blueprint on the sheet and the name it was last saved under, and the
     * history every change can be undone and redone through, one stroke or
     * press at a time. It holds the tool in hand and what it works on — the
     * deck picked, whether walls stand full or half height, whether an
     * opening is a door or a window, the mast picked, the furniture item and
     * the quarter turn something is set down at — and the cursor, the octant
     * of the sheet the pointer or the movement bindings last put it on. The
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
    private boolean discardArmed;

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

    // Tool
    private ShipwrightTool tool;
    private int deck;
    private boolean halfWalls;
    private boolean windows;
    private int mastIndex;
    private int itemIndex;
    private int spin;

    // Cursor — the octant of the sheet the cursor stands on
    private int cursorU;
    private int cursorV;

    // Compile
    private ShipLayoutStruct layout;
    private ShipSurveyStruct survey;
    private int revision;

    // Survey — due once the blueprint has rested since it last compiled
    private boolean surveyDue;
    private float surveyCountdown;

    // Library — the saved ship Open last showed
    private int libraryIndex;

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
        this.tool = ShipwrightTool.DECK;
        this.halfWalls = false;
        this.windows = false;
        this.itemIndex = 0;
        this.spin = 0;

        // Cursor
        this.cursorU = EngineSetting.SHIP_AFT_MARGIN + EngineSetting.SHIP_SHEET_LENGTH / 2;
        this.cursorV = EngineSetting.SHIP_SHEET_HALF_BREADTH;

        // Library
        this.libraryIndex = EngineSetting.INDEX_NOT_FOUND;

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
        this.discardArmed = false;
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
        this.discardArmed = false;
        markChanged();
    }

    // Asks once before unsaved work is thrown away; the next try goes ahead
    public void armDiscard() {
        this.discardArmed = true;
    }

    public boolean isDiscardArmed() {
        return discardArmed;
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

    public void setTool(ShipwrightTool tool) {
        this.tool = tool;
        markChanged();
    }

    public void setDeck(int deck) {
        this.deck = deck;
        markChanged();
    }

    public void setHalfWalls(boolean halfWalls) {
        this.halfWalls = halfWalls;
        markChanged();
    }

    public void setWindows(boolean windows) {
        this.windows = windows;
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

    // Library \\

    public void setLibraryIndex(int libraryIndex) {
        this.libraryIndex = libraryIndex;
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

    public ShipwrightTool getTool() {
        return tool;
    }

    // The deck every tool works on, always one of the blueprint's
    public int getDeck() {
        return Math.min(deck, blueprint.getDeckCount() - 1);
    }

    public boolean isHalfWalls() {
        return halfWalls;
    }

    public boolean isWindows() {
        return windows;
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

    public ShipLayoutStruct getLayout() {
        return layout;
    }

    public ShipSurveyStruct getSurvey() {
        return survey;
    }

    public int getRevision() {
        return revision;
    }

    public int getLibraryIndex() {
        return libraryIndex;
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
