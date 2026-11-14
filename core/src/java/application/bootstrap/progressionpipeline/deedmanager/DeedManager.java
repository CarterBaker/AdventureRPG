package application.bootstrap.progressionpipeline.deedmanager;

import application.bootstrap.progressionpipeline.deed.DeedHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DeedManager extends ManagerPackage {

    /*
     * Owns the deed palette: every notable action the world weighs, and what
     * doing one is worth in experience, morality and trade. Code performs a
     * deed by the ID it resolved once from its name; the data decides its
     * worth. IDs are assigned in registration order, and a name declared twice
     * is rejected. Every deed is resolved in awake(), so a being's deed counts
     * can be held in one array indexed by ID.
     */

    // Palette
    private Object2IntOpenHashMap<String> deedName2DeedID;
    private ObjectArrayList<DeedHandle> deedID2DeedHandle;

    // Engine Deeds
    private short killDeedID;
    private short tradePracticeDeedID;
    private short tradeAdvanceDeedID;
    private short skillAdvanceDeedID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.deedName2DeedID = RegistryUtility.createNameIndex();
        this.deedID2DeedHandle = RegistryUtility.createPalette();
        create(DeedLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Engine Deeds
        this.killDeedID = getDeedIDFromDeedName(EngineSetting.DEED_KILL);
        this.tradePracticeDeedID = getDeedIDFromDeedName(EngineSetting.DEED_TRADE_PRACTICE);
        this.tradeAdvanceDeedID = getDeedIDFromDeedName(EngineSetting.DEED_TRADE_ADVANCE);
        this.skillAdvanceDeedID = getDeedIDFromDeedName(EngineSetting.DEED_SKILL_ADVANCE);
    }

    // Management \\

    short registerDeedName(String deedName) {
        return (short) RegistryUtility.registerID(
                deedName2DeedID, deedID2DeedHandle, deedName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addDeedHandle(DeedHandle deedHandle) {

        short deedID = deedHandle.getDeedID();

        if (deedID2DeedHandle.get(deedID) != null)
            throwException("Duplicate deed name: '" + deedHandle.getDeedName()
                    + "' is declared more than once — every deed name must be unique");

        deedID2DeedHandle.set(deedID, deedHandle);
    }

    // Accessible \\

    public boolean hasDeed(String deedName) {
        return RegistryUtility.getHandle(deedName2DeedID, deedID2DeedHandle, deedName) != null;
    }

    public short getDeedIDFromDeedName(String deedName) {

        if (!hasDeed(deedName) && internalLoader != null)
            ((DeedLoader) internalLoader).request(deedName);

        if (!hasDeed(deedName))
            throwException("Deed not found after load: \"" + deedName + "\"");

        return (short) deedName2DeedID.getInt(deedName);
    }

    public DeedHandle getDeedHandleFromDeedID(short deedID) {

        DeedHandle handle = RegistryUtility.getHandle(deedID2DeedHandle, deedID);

        if (handle == null)
            throwException("Deed ID not found: " + deedID);

        return handle;
    }

    public DeedHandle getDeedHandleFromDeedName(String deedName) {
        return getDeedHandleFromDeedID(getDeedIDFromDeedName(deedName));
    }

    // Every deed ID lies below this — the length of an array indexed by deed ID
    public int getDeedIDCount() {
        return deedID2DeedHandle.size();
    }

    // Engine Deeds \\

    public short getKillDeedID() {
        return killDeedID;
    }

    public short getTradePracticeDeedID() {
        return tradePracticeDeedID;
    }

    public short getTradeAdvanceDeedID() {
        return tradeAdvanceDeedID;
    }

    public short getSkillAdvanceDeedID() {
        return skillAdvanceDeedID;
    }
}
