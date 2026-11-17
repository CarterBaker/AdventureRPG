package editor.bootstrap.stamppipeline.stampmanager;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.stamppipeline.stamp.StampData;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StampManager extends ManagerPackage {

    /*
     * Owns every stamp and the one in use, the part the Sculpt Brush places
     * in place of a stroke, the way a creature kit holds its parts. A stamp
     * is a body part with its own bones, weights, morphs, UVs and layers, and
     * the loops it joins by. Stamps load from ARPG, one to a file, and are
     * listed by kind and then display name. The game's stamps are fixed: the
     * Hierarchy and Info Panel edit and delete them but make none, and every
     * change reaches here live, rebuilding the stamp in place under its ID. A
     * stamp picked in the Hierarchy becomes the stamp in use, and one chosen
     * in the Stamps tab is shown in the Info Panel. Every stamp has an emblem,
     * kept through StampThumbnailBranch. No stamp is in use until one is
     * chosen; placing it, Escape, or deleting it puts it down again. While
     * the Image Editor is the canvas worked last, the stamps shown are
     * pictures instead, read from the workspace through StampImageBranch,
     * one of them in use for the Image Editor's Brush to press the same way.
     */

    // Internal
    private InfoManager infoManager;
    private StampThumbnailBranch stampThumbnailBranch;
    private StampImageBranch stampImageBranch;
    private ToolManager toolManager;

    // Palette
    private Object2IntOpenHashMap<String> stampName2StampID;
    private ObjectArrayList<StampHandle> stampID2StampHandle;
    private ObjectArrayList<StampHandle> stamps;

    // Active
    private StampHandle activeStamp;
    private String activeImageStamp;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.stampName2StampID = RegistryUtility.createNameIndex();
        this.stampID2StampHandle = RegistryUtility.createPalette();
        this.stamps = new ObjectArrayList<>();
        this.stampThumbnailBranch = create(StampThumbnailBranch.class);
        this.stampImageBranch = create(StampImageBranch.class);
        create(StampLoader.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_STAMPS, this::rebuildStamp);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_STAMPS, this::retireStamp);
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_STAMPS, entry -> selectStamp(entry.getEntryName()));
    }

    // Management \\

    int registerStampName(String stampName) {
        return RegistryUtility.registerID(
                stampName2StampID, stampID2StampHandle, stampName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // A stamp built for the first time is listed; one built again takes its new data in place
    void addStampData(StampData stampData) {

        StampHandle stampHandle = RegistryUtility.getHandle(stampID2StampHandle, stampData.getStampID());

        if (stampHandle == null) {
            stampHandle = create(StampHandle.class);
            stampID2StampHandle.set(stampData.getStampID(), stampHandle);
        }

        stampHandle.constructor(stampData);
        stampThumbnailBranch.invalidate(stampData.getStampID());

        if (!stamps.contains(stampHandle))
            stamps.add(stampHandle);

        stamps.sort((a, b) -> a.getCategory() != b.getCategory()
                ? a.getCategory().compareTo(b.getCategory())
                : String.CASE_INSENSITIVE_ORDER.compare(a.getDisplayName(), b.getDisplayName()));

        notifyChanged();
    }

    // Live Edit \\

    private void rebuildStamp(InfoEntryStruct entry) {

        try {
            ((StampLoader) internalLoader).build(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(EditorSetting.STAMP_MESSAGE_REFUSED + e.getMessage());
        }
    }

    // A deleted stamp leaves the list, put down if it was in use
    private void retireStamp(InfoEntryStruct entry) {

        StampHandle stampHandle = RegistryUtility.getHandle(
                stampName2StampID, stampID2StampHandle, entry.getEntryName());

        if (stampHandle == null || !stamps.remove(stampHandle))
            return;

        stampThumbnailBranch.release(stampHandle.getStampID());

        if (stampHandle == activeStamp)
            this.activeStamp = null;

        notifyChanged();
    }

    // Selection \\

    public void selectStamp(String stampName) {

        if (!hasStamp(stampName))
            return;

        StampHandle stampHandle = getStampHandleFromStampName(stampName);

        if (stampHandle == activeStamp)
            return;

        this.activeStamp = stampHandle;
        notifyChanged();
    }

    // The stamp in use put down, so no part is armed
    public void clearActiveStamp() {

        if (activeStamp == null)
            return;

        this.activeStamp = null;
        notifyChanged();
    }

    // Pictures \\

    // Whether the stamps shown are the Image Editor's pictures: the Image Editor is the canvas worked last
    public boolean showsImages() {
        return toolManager.getCanvas() == ToolCanvas.IMAGE_EDITOR;
    }

    // The workspace's pictures read again, the one in use put down when it is gone
    public void refreshImages() {

        if (!stampImageBranch.scan())
            return;

        if (activeImageStamp != null && !stampImageBranch.hasName(activeImageStamp))
            this.activeImageStamp = null;

        notifyChanged();
    }

    public void selectImageStamp(String name) {

        if (!stampImageBranch.hasName(name) || name.equals(activeImageStamp))
            return;

        this.activeImageStamp = name;
        notifyChanged();
    }

    // The picture in use put down, so the Image Editor's Brush paints again
    public void clearActiveImageStamp() {

        if (activeImageStamp == null)
            return;

        this.activeImageStamp = null;
        notifyChanged();
    }

    // The stamp a tile names chosen, a picture or a part by the stamps shown
    public void chooseStamp(String name) {

        if (showsImages()) {
            selectImageStamp(name);
            return;
        }

        selectStamp(name);
        showActiveStamp();
    }

    // The next stamp along among those shown
    public void stepStamp(int step) {

        if (!showsImages()) {
            cycleStamp(step);
            showActiveStamp();
            return;
        }

        ObjectArrayList<String> names = stampImageBranch.getNames();
        int index = activeImageStamp != null ? names.indexOf(activeImageStamp) : step > 0 ? -1 : names.size();

        if (!names.isEmpty())
            selectImageStamp(names.get(Math.floorMod(index + step, names.size())));
    }

    // The next stamp along, the first or last when none is in use
    public void cycleStamp(int step) {

        int index = activeStamp != null ? stamps.indexOf(activeStamp) : step > 0 ? -1 : stamps.size();

        if (!stamps.isEmpty())
            selectStamp(stamps.get(Math.floorMod(index + step, stamps.size())).getStampName());
    }

    // The stamp in use picked in the Hierarchy, so the Info Panel shows it
    public void showActiveStamp() {

        if (activeStamp == null)
            return;

        String stampName = activeStamp.getStampName();

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_STAMPS, stampName, stampName);
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    // Accessible \\

    public StampHandle getStampHandleFromStampName(String stampName) {

        StampHandle stampHandle = RegistryUtility.getHandle(stampName2StampID, stampID2StampHandle, stampName);

        if (stampHandle == null)
            throwException("Stamp not found: \"" + stampName + "\"");

        return stampHandle;
    }

    // True for a stamp listed under a name, false for none or one deleted
    public boolean hasStamp(String stampName) {

        StampHandle stampHandle = RegistryUtility.getHandle(stampName2StampID, stampID2StampHandle, stampName);

        return stampHandle != null && stamps.contains(stampHandle);
    }

    // Main thread — the stamp's emblem texture, drawn first when it is missing or the stamp was rebuilt
    public int getThumbnail(StampHandle stampHandle) {
        return stampThumbnailBranch.getThumbnail(stampHandle);
    }

    public ObjectArrayList<StampHandle> getStamps() {
        return stamps;
    }

    public ObjectArrayList<String> getImageStamps() {
        return stampImageBranch.getNames();
    }

    // Null while no picture is in use
    public String getActiveImageStamp() {
        return activeImageStamp;
    }

    // Main thread — the picture's emblem texture, 0 when it cannot be read
    public int getImageThumbnail(String name) {
        return stampImageBranch.getThumbnail(name);
    }

    // The picture's pixels, null when it cannot be read
    public Pixmap getImagePixmap(String name) {
        return stampImageBranch.getPixmap(name);
    }

    // Null while no stamp is in use
    public StampHandle getActiveStamp() {
        return activeStamp;
    }

    public int getRevision() {
        return revision;
    }
}
