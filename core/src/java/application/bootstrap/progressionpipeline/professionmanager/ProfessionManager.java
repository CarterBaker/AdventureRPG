package application.bootstrap.progressionpipeline.professionmanager;

import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ProfessionManager extends ManagerPackage {

    /*
     * Owns the profession palette: every calling a being can hold, what
     * unlocks it and what it lends. IDs are assigned in registration order,
     * which is also the order professions are listed in, and a name declared
     * twice is rejected. Every profession is resolved in awake(), after the
     * attributes, stats, trades, skills and deeds its requirements and effects
     * name.
     */

    // Palette
    private Object2IntOpenHashMap<String> professionName2ProfessionID;
    private ObjectArrayList<ProfessionHandle> professionID2ProfessionHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.professionName2ProfessionID = RegistryUtility.createNameIndex();
        this.professionID2ProfessionHandle = RegistryUtility.createPalette();
        create(ProfessionLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        validateRequiredProfessions();
    }

    // Internal \\

    // A profession another requires is given its ID when first named — every one of them must have been defined
    private void validateRequiredProfessions() {

        for (int professionID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                professionID < professionID2ProfessionHandle.size();
                professionID++)
            if (professionID2ProfessionHandle.get(professionID) == null)
                throwException("A profession is required by name but never defined: \""
                        + findProfessionName((short) professionID) + "\"");
    }

    private String findProfessionName(short professionID) {

        for (String professionName : professionName2ProfessionID.keySet())
            if (professionName2ProfessionID.getInt(professionName) == professionID)
                return professionName;

        return EngineSetting.PROGRESSION_TEXT_NONE;
    }

    // Management \\

    short registerProfessionName(String professionName) {
        return (short) RegistryUtility.registerID(
                professionName2ProfessionID, professionID2ProfessionHandle, professionName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addProfessionHandle(ProfessionHandle professionHandle) {

        short professionID = professionHandle.getProfessionID();

        if (professionID2ProfessionHandle.get(professionID) != null)
            throwException("Duplicate profession name: '" + professionHandle.getProfessionName()
                    + "' is declared more than once — every profession name must be unique");

        professionID2ProfessionHandle.set(professionID, professionHandle);
    }

    // Accessible \\

    public boolean hasProfession(String professionName) {
        return RegistryUtility.getHandle(
                professionName2ProfessionID, professionID2ProfessionHandle, professionName) != null;
    }

    public short getProfessionIDFromProfessionName(String professionName) {

        if (!hasProfession(professionName) && internalLoader != null)
            ((ProfessionLoader) internalLoader).request(professionName);

        if (!hasProfession(professionName))
            throwException("Profession not found after load: \"" + professionName + "\"");

        return (short) professionName2ProfessionID.getInt(professionName);
    }

    public ProfessionHandle getProfessionHandleFromProfessionID(short professionID) {

        ProfessionHandle handle = RegistryUtility.getHandle(professionID2ProfessionHandle, professionID);

        if (handle == null)
            throwException("Profession ID not found: " + professionID);

        return handle;
    }

    public ProfessionHandle getProfessionHandleFromProfessionName(String professionName) {
        return getProfessionHandleFromProfessionID(getProfessionIDFromProfessionName(professionName));
    }

    // Every profession ID lies below this — the length of an array indexed by profession ID
    public int getProfessionIDCount() {
        return professionID2ProfessionHandle.size();
    }
}
