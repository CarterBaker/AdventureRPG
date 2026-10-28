package editor.bootstrap.brushpipeline.brushmanager;

import editor.bootstrap.brushpipeline.brush.BrushData;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BrushManager extends ManagerPackage {

    /*
     * Owns every brush and the one in use, shared by every stroking tool and
     * window the way a paint program's brush is. A brush is only a tip: its
     * shape and how hard, dense and widely spaced it stamps; what a stroke
     * does, and how strongly and widely, is the tool's. Brushes load from
     * ARPG, one to a file, and are listed by their display names. They are
     * content like any other: the Hierarchy and Info Panel edit, create, copy
     * and delete them, and every change reaches here live, rebuilding the
     * brush in place under its ID, so a stroke under way takes the edit at
     * once. The brush in use falls back to the default when it is deleted.
     */

    // Internal
    private InfoManager infoManager;

    // Palette
    private Object2IntOpenHashMap<String> brushName2BrushID;
    private ObjectArrayList<BrushHandle> brushID2BrushHandle;
    private ObjectArrayList<BrushHandle> brushes;

    // Active
    private BrushHandle activeBrush;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.brushName2BrushID = RegistryUtility.createNameIndex();
        this.brushID2BrushHandle = RegistryUtility.createPalette();
        this.brushes = new ObjectArrayList<>();
        create(BrushLoader.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        this.activeBrush = getBrushHandleFromBrushName(EditorSetting.BRUSH_DEFAULT);

        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_BRUSHES, this::rebuildBrush);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_BRUSHES, this::retireBrush);
    }

    // Management \\

    int registerBrushName(String brushName) {
        return RegistryUtility.registerID(
                brushName2BrushID, brushID2BrushHandle, brushName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // A brush built for the first time is listed; one built again takes its new data in place
    void addBrushData(BrushData brushData) {

        BrushHandle brushHandle = RegistryUtility.getHandle(brushID2BrushHandle, brushData.getBrushID());

        if (brushHandle == null) {
            brushHandle = create(BrushHandle.class);
            brushID2BrushHandle.set(brushData.getBrushID(), brushHandle);
        }

        brushHandle.constructor(brushData);

        if (!brushes.contains(brushHandle))
            brushes.add(brushHandle);

        brushes.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDisplayName(), b.getDisplayName()));
        notifyChanged();
    }

    // Live Edit \\

    private void rebuildBrush(InfoEntryStruct entry) {

        try {
            ((BrushLoader) internalLoader).build(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(EditorSetting.BRUSH_MESSAGE_REFUSED + e.getMessage());
        }
    }

    // A deleted brush leaves the list; whoever strokes with it moves to the default
    private void retireBrush(InfoEntryStruct entry) {

        BrushHandle brushHandle = RegistryUtility.getHandle(
                brushName2BrushID, brushID2BrushHandle, entry.getEntryName());

        if (brushHandle == null || !brushes.remove(brushHandle))
            return;

        if (brushHandle == activeBrush)
            this.activeBrush = getBrushHandleFromBrushName(EditorSetting.BRUSH_DEFAULT);

        notifyChanged();
    }

    // Selection \\

    public void selectBrush(String brushName) {

        if (!hasBrush(brushName))
            return;

        BrushHandle brushHandle = getBrushHandleFromBrushName(brushName);

        if (brushHandle == activeBrush)
            return;

        this.activeBrush = brushHandle;
        notifyChanged();
    }

    public void cycleBrush(int step) {

        int index = brushes.indexOf(activeBrush);

        if (!brushes.isEmpty())
            selectBrush(brushes.get(Math.floorMod(index + step, brushes.size())).getBrushName());
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    // Accessible \\

    public BrushHandle getBrushHandleFromBrushName(String brushName) {

        BrushHandle brushHandle = RegistryUtility.getHandle(brushName2BrushID, brushID2BrushHandle, brushName);

        if (brushHandle == null)
            throwException("Brush not found: \"" + brushName + "\"");

        return brushHandle;
    }

    // True for a brush listed under a name, false for none or one deleted
    public boolean hasBrush(String brushName) {

        BrushHandle brushHandle = RegistryUtility.getHandle(brushName2BrushID, brushID2BrushHandle, brushName);

        return brushHandle != null && brushes.contains(brushHandle);
    }

    public ObjectArrayList<BrushHandle> getBrushes() {
        return brushes;
    }

    public BrushHandle getActiveBrush() {
        return activeBrush;
    }

    public int getRevision() {
        return revision;
    }
}
