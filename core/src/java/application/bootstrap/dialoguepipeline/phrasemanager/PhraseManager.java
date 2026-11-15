package application.bootstrap.dialoguepipeline.phrasemanager;

import application.bootstrap.dialoguepipeline.phrase.PhraseHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PhraseManager extends ManagerPackage {

    /*
     * Owns the phrase palette: every named piece of speech a line may call for.
     * IDs are assigned in registration order, and a name declared twice is
     * rejected. A phrase may call for itself or for one declared later, so a
     * phrase's own lines register the names they call for without loading them,
     * and awake() catches a name no file declares once every phrase is in.
     */

    // Palette
    private Object2IntOpenHashMap<String> phraseName2PhraseID;
    private ObjectArrayList<PhraseHandle> phraseID2PhraseHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.phraseName2PhraseID = RegistryUtility.createNameIndex();
        this.phraseID2PhraseHandle = RegistryUtility.createPalette();
        create(PhraseLoader.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();

        for (Object2IntMap.Entry<String> entry : phraseName2PhraseID.object2IntEntrySet())
            if (phraseID2PhraseHandle.get(entry.getIntValue()) == null)
                throwException("Phrase \"" + entry.getKey() + "\" is named but no file declares it.");
    }

    // Management \\

    short registerPhraseName(String phraseName) {
        return (short) RegistryUtility.registerID(
                phraseName2PhraseID, phraseID2PhraseHandle, phraseName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addPhraseHandle(PhraseHandle phraseHandle) {

        short phraseID = phraseHandle.getPhraseID();

        if (phraseID2PhraseHandle.get(phraseID) != null)
            throwException("Duplicate phrase name: '" + phraseHandle.getPhraseName()
                    + "' is declared more than once — every phrase name must be unique");

        phraseID2PhraseHandle.set(phraseID, phraseHandle);
    }

    // Accessible \\

    public boolean hasPhrase(String phraseName) {
        return RegistryUtility.getHandle(phraseName2PhraseID, phraseID2PhraseHandle, phraseName) != null;
    }

    public short getPhraseIDFromPhraseName(String phraseName) {

        if (!hasPhrase(phraseName) && internalLoader != null)
            ((PhraseLoader) internalLoader).request(phraseName);

        if (!hasPhrase(phraseName))
            throwException("Phrase not found after load: \"" + phraseName + "\"");

        return (short) phraseName2PhraseID.getInt(phraseName);
    }

    public PhraseHandle getPhraseHandleFromPhraseID(short phraseID) {

        PhraseHandle handle = RegistryUtility.getHandle(phraseID2PhraseHandle, phraseID);

        if (handle == null)
            throwException("Phrase ID not found: " + phraseID);

        return handle;
    }

    public PhraseHandle getPhraseHandleFromPhraseName(String phraseName) {
        return getPhraseHandleFromPhraseID(getPhraseIDFromPhraseName(phraseName));
    }

    // Every phrase ID lies below this — the length of an array indexed by phrase ID
    public int getPhraseIDCount() {
        return phraseID2PhraseHandle.size();
    }
}
