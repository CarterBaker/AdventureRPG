package application.bootstrap.dialoguepipeline.moodmanager;

import application.bootstrap.dialoguepipeline.mood.MoodHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MoodManager extends ManagerPackage {

    /*
     * Owns the mood palette: every feeling a being can hold for a while. IDs are
     * assigned in registration order, and a name declared twice is rejected. Every
     * mood is resolved in awake(), so a being's mood levels are held in one array
     * indexed by ID; the reserved ID stands for calm, the state of a being no mood
     * has hold of.
     */

    // Palette
    private Object2IntOpenHashMap<String> moodName2MoodID;
    private ObjectArrayList<MoodHandle> moodID2MoodHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.moodName2MoodID = RegistryUtility.createNameIndex();
        this.moodID2MoodHandle = RegistryUtility.createPalette();
        create(MoodLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    short registerMoodName(String moodName) {
        return (short) RegistryUtility.registerID(
                moodName2MoodID, moodID2MoodHandle, moodName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addMoodHandle(MoodHandle moodHandle) {

        short moodID = moodHandle.getMoodID();

        if (moodID2MoodHandle.get(moodID) != null)
            throwException("Duplicate mood name: '" + moodHandle.getMoodName()
                    + "' is declared more than once — every mood name must be unique");

        moodID2MoodHandle.set(moodID, moodHandle);
    }

    // Accessible \\

    public boolean hasMood(String moodName) {
        return RegistryUtility.getHandle(moodName2MoodID, moodID2MoodHandle, moodName) != null;
    }

    public short getMoodIDFromMoodName(String moodName) {

        if (!hasMood(moodName) && internalLoader != null)
            ((MoodLoader) internalLoader).request(moodName);

        if (!hasMood(moodName))
            throwException("Mood not found after load: \"" + moodName + "\"");

        return (short) moodName2MoodID.getInt(moodName);
    }

    public MoodHandle getMoodHandleFromMoodID(short moodID) {

        MoodHandle handle = RegistryUtility.getHandle(moodID2MoodHandle, moodID);

        if (handle == null)
            throwException("Mood ID not found: " + moodID);

        return handle;
    }

    public MoodHandle getMoodHandleFromMoodName(String moodName) {
        return getMoodHandleFromMoodID(getMoodIDFromMoodName(moodName));
    }

    // Every mood ID lies below this — the length of an array indexed by mood ID
    public int getMoodIDCount() {
        return moodID2MoodHandle.size();
    }
}
