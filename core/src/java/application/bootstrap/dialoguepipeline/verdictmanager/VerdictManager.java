package application.bootstrap.dialoguepipeline.verdictmanager;

import application.bootstrap.dialoguepipeline.verdict.VerdictHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VerdictManager extends ManagerPackage {

    /*
     * Owns the verdict palette: every judgement a trainer can pass on a turn of
     * talk, and what each is worth to the three dialogue models. IDs are assigned
     * in registration order, and a name declared twice is rejected. Every verdict
     * is resolved in awake().
     */

    // Palette
    private Object2IntOpenHashMap<String> verdictName2VerdictID;
    private ObjectArrayList<VerdictHandle> verdictID2VerdictHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.verdictName2VerdictID = RegistryUtility.createNameIndex();
        this.verdictID2VerdictHandle = RegistryUtility.createPalette();
        create(VerdictLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    short registerVerdictName(String verdictName) {
        return (short) RegistryUtility.registerID(
                verdictName2VerdictID, verdictID2VerdictHandle, verdictName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addVerdictHandle(VerdictHandle verdictHandle) {

        short verdictID = verdictHandle.getVerdictID();

        if (verdictID2VerdictHandle.get(verdictID) != null)
            throwException("Duplicate verdict name: '" + verdictHandle.getVerdictName()
                    + "' is declared more than once — every verdict name must be unique");

        verdictID2VerdictHandle.set(verdictID, verdictHandle);
    }

    // Accessible \\

    public boolean hasVerdict(String verdictName) {
        return RegistryUtility.getHandle(verdictName2VerdictID, verdictID2VerdictHandle, verdictName) != null;
    }

    public short getVerdictIDFromVerdictName(String verdictName) {

        if (!hasVerdict(verdictName) && internalLoader != null)
            ((VerdictLoader) internalLoader).request(verdictName);

        if (!hasVerdict(verdictName))
            throwException("Verdict not found after load: \"" + verdictName + "\"");

        return (short) verdictName2VerdictID.getInt(verdictName);
    }

    public VerdictHandle getVerdictHandleFromVerdictID(short verdictID) {

        VerdictHandle handle = RegistryUtility.getHandle(verdictID2VerdictHandle, verdictID);

        if (handle == null)
            throwException("Verdict ID not found: " + verdictID);

        return handle;
    }

    public VerdictHandle getVerdictHandleFromVerdictName(String verdictName) {
        return getVerdictHandleFromVerdictID(getVerdictIDFromVerdictName(verdictName));
    }

    // Every verdict ID lies below this — the length of an array indexed by verdict ID
    public int getVerdictIDCount() {
        return verdictID2VerdictHandle.size();
    }
}
