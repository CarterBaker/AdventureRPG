package application.bootstrap.progressionpipeline.gendermanager;

import application.bootstrap.progressionpipeline.gender.GenderHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GenderManager extends ManagerPackage {

    /*
     * Owns the gender palette. A race names the genders it allows, a being
     * carries one of them, and a profession may title itself by it. IDs are
     * assigned in registration order, and a name declared twice is rejected.
     * Every gender is resolved in awake(), before any race or profession names
     * one.
     */

    // Palette
    private Object2IntOpenHashMap<String> genderName2GenderID;
    private ObjectArrayList<GenderHandle> genderID2GenderHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.genderName2GenderID = RegistryUtility.createNameIndex();
        this.genderID2GenderHandle = RegistryUtility.createPalette();
        create(GenderLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerGenderName(String genderName) {
        return (short) RegistryUtility.registerID(
                genderName2GenderID, genderID2GenderHandle, genderName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addGenderHandle(GenderHandle genderHandle) {

        short genderID = genderHandle.getGenderID();

        if (genderID2GenderHandle.get(genderID) != null)
            throwException("Duplicate gender name: '" + genderHandle.getGenderName()
                    + "' is declared more than once — every gender name must be unique");

        genderID2GenderHandle.set(genderID, genderHandle);
    }

    // Accessible \\

    public boolean hasGender(String genderName) {
        return RegistryUtility.getHandle(genderName2GenderID, genderID2GenderHandle, genderName) != null;
    }

    public short getGenderIDFromGenderName(String genderName) {

        if (!hasGender(genderName) && internalLoader != null)
            ((GenderLoader) internalLoader).request(genderName);

        if (!hasGender(genderName))
            throwException("Gender not found after load: \"" + genderName + "\"");

        return (short) genderName2GenderID.getInt(genderName);
    }

    public GenderHandle getGenderHandleFromGenderID(short genderID) {

        GenderHandle handle = RegistryUtility.getHandle(genderID2GenderHandle, genderID);

        if (handle == null)
            throwException("Gender ID not found: " + genderID);

        return handle;
    }

    public GenderHandle getGenderHandleFromGenderName(String genderName) {
        return getGenderHandleFromGenderID(getGenderIDFromGenderName(genderName));
    }

    // Every gender ID lies below this — the length of an array indexed by gender ID
    public int getGenderIDCount() {
        return genderID2GenderHandle.size();
    }
}
