package editor.bootstrap.stamppipeline.stampmanager;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.stamppipeline.stamp.StampData;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.runtime.EditorSetting;
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
     * chosen; placing it, Escape, or deleting it puts it down again.
     */

    // Internal
    private InfoManager infoManager;
    private StampThumbnailBranch stampThumbnailBranch;

    // Palette
    private Object2IntOpenHashMap<String> stampName2StampID;
    private ObjectArrayList<StampHandle> stampID2StampHandle;
    private ObjectArrayList<StampHandle> stamps;

    // Active
    private StampHandle activeStamp;

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
        create(StampLoader.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
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

    // Null while no stamp is in use
    public StampHandle getActiveStamp() {
        return activeStamp;
    }

    public int getRevision() {
        return revision;
    }
}
