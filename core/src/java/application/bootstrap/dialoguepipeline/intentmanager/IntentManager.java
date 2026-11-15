package application.bootstrap.dialoguepipeline.intentmanager;

import application.bootstrap.dialoguepipeline.intent.IntentHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class IntentManager extends ManagerPackage {

    /*
     * Owns the intent palette: everything a speaker can mean. IDs are assigned in
     * registration order, and a name declared twice is rejected. Every intent is
     * resolved in awake(), so the understanding model holds one weight per intent
     * ID, and the intent the engine falls back on when it cannot tell what was
     * meant is cached by ID there.
     */

    // Palette
    private Object2IntOpenHashMap<String> intentName2IntentID;
    private ObjectArrayList<IntentHandle> intentID2IntentHandle;

    // Engine Intents
    private short confusedIntentID;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.intentName2IntentID = RegistryUtility.createNameIndex();
        this.intentID2IntentHandle = RegistryUtility.createPalette();
        create(IntentLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        // Engine Intents
        this.confusedIntentID = getIntentIDFromIntentName(EngineSetting.DIALOGUE_INTENT_CONFUSED);
    }

    // Management \\

    short registerIntentName(String intentName) {
        return (short) RegistryUtility.registerID(
                intentName2IntentID, intentID2IntentHandle, intentName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addIntentHandle(IntentHandle intentHandle) {

        short intentID = intentHandle.getIntentID();

        if (intentID2IntentHandle.get(intentID) != null)
            throwException("Duplicate intent name: '" + intentHandle.getIntentName()
                    + "' is declared more than once — every intent name must be unique");

        intentID2IntentHandle.set(intentID, intentHandle);
    }

    // Accessible \\

    public boolean hasIntent(String intentName) {
        return RegistryUtility.getHandle(intentName2IntentID, intentID2IntentHandle, intentName) != null;
    }

    public short getIntentIDFromIntentName(String intentName) {

        if (!hasIntent(intentName) && internalLoader != null)
            ((IntentLoader) internalLoader).request(intentName);

        if (!hasIntent(intentName))
            throwException("Intent not found after load: \"" + intentName + "\"");

        return (short) intentName2IntentID.getInt(intentName);
    }

    public IntentHandle getIntentHandleFromIntentID(short intentID) {

        IntentHandle handle = RegistryUtility.getHandle(intentID2IntentHandle, intentID);

        if (handle == null)
            throwException("Intent ID not found: " + intentID);

        return handle;
    }

    public IntentHandle getIntentHandleFromIntentName(String intentName) {
        return getIntentHandleFromIntentID(getIntentIDFromIntentName(intentName));
    }

    // Every intent ID lies below this — the length of an array indexed by intent ID
    public int getIntentIDCount() {
        return intentID2IntentHandle.size();
    }

    // Engine Intents \\

    public short getConfusedIntentID() {
        return confusedIntentID;
    }
}
