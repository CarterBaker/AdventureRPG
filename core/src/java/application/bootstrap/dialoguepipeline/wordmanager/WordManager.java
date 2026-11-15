package application.bootstrap.dialoguepipeline.wordmanager;

import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import application.bootstrap.dialoguepipeline.word.WordHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WordManager extends ManagerPackage {

    /*
     * Owns the dictionary: the word palette, the index of every form a word is
     * heard as, and the words carrying each tag. A word is found by any of its
     * forms, and a token that is no form of any word is still found when it is one
     * letter away from one — a letter dropped, added, mistyped or swapped with its
     * neighbour — through an index of every form with one letter removed, so a
     * slip of the keys does not lose a word. Only forms long enough to be told
     * apart by one letter are indexed, so short words are never mistaken for each
     * other. Tags are interned as they are first named and every word carrying one
     * is listed under it, for lines that draw a word by tag. IDs are assigned in
     * registration order; a name declared twice, or a form claimed by two words,
     * is rejected.
     */

    // Palette
    private Object2IntOpenHashMap<String> wordName2WordID;
    private ObjectArrayList<WordHandle> wordID2WordHandle;

    // Forms
    private Object2IntOpenHashMap<String> form2WordID;
    private Object2IntOpenHashMap<String> deletion2WordID;

    // Tags
    private Object2IntOpenHashMap<String> tagName2TagID;
    private ObjectArrayList<IntArrayList> tagID2WordIDs;
    private IntArrayList tagID2Seed;

    // Scratch
    private StringBuilder deletionScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.wordName2WordID = RegistryUtility.createNameIndex();
        this.wordID2WordHandle = RegistryUtility.createPalette();
        create(WordLoader.class);

        // Forms
        this.form2WordID = new Object2IntOpenHashMap<>();
        this.form2WordID.defaultReturnValue(EngineSetting.REGISTRY_RESERVED_ID);
        this.deletion2WordID = new Object2IntOpenHashMap<>();
        this.deletion2WordID.defaultReturnValue(EngineSetting.REGISTRY_RESERVED_ID);

        // Tags
        this.tagName2TagID = RegistryUtility.createNameIndex();
        this.tagID2WordIDs = RegistryUtility.createPalette();
        this.tagID2Seed = new IntArrayList();
        this.tagID2Seed.add(EngineSetting.REGISTRY_RESERVED_ID);

        // Scratch
        this.deletionScratch = new StringBuilder();
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
    }

    // Management \\

    int registerWordName(String wordName) {
        return RegistryUtility.registerID(
                wordName2WordID, wordID2WordHandle, wordName,
                EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addWordHandle(WordHandle wordHandle) {

        int wordID = wordHandle.getWordID();

        if (wordID2WordHandle.get(wordID) != null)
            throwException("Duplicate word name: '" + wordHandle.getWordName()
                    + "' is declared more than once — every word name must be unique");

        wordID2WordHandle.set(wordID, wordHandle);

        indexForm(wordHandle.getWordName(), wordHandle);

        String[] forms = wordHandle.getForms();
        int[] tagIDs = wordHandle.getTagIDs();

        for (int i = 0; i < forms.length; i++)
            indexForm(forms[i], wordHandle);

        for (int i = 0; i < tagIDs.length; i++)
            tagID2WordIDs.get(tagIDs[i]).add(wordID);
    }

    // Accessible \\

    public boolean hasWord(String wordName) {
        return RegistryUtility.getHandle(wordName2WordID, wordID2WordHandle, wordName) != null;
    }

    public int getWordIDFromWordName(String wordName) {

        if (!hasWord(wordName) && internalLoader != null)
            ((WordLoader) internalLoader).request(wordName);

        if (!hasWord(wordName))
            throwException("Word not found after load: \"" + wordName + "\"");

        return wordName2WordID.getInt(wordName);
    }

    public WordHandle getWordHandleFromWordID(int wordID) {

        WordHandle handle = RegistryUtility.getHandle(wordID2WordHandle, wordID);

        if (handle == null)
            throwException("Word ID not found: " + wordID);

        return handle;
    }

    public WordHandle getWordHandleFromWordName(String wordName) {
        return getWordHandleFromWordID(getWordIDFromWordName(wordName));
    }

    // Every word ID lies below this — the length of an array indexed by word ID
    public int getWordIDCount() {
        return wordID2WordHandle.size();
    }

    // Forms \\

    // A form is heard as its word, and so is the form with any one letter removed
    private void indexForm(String form, WordHandle wordHandle) {

        int claimedID = form2WordID.getInt(form);

        if (claimedID != EngineSetting.REGISTRY_RESERVED_ID)
            throwException("Form \"" + form + "\" is claimed by both word '"
                    + getWordHandleFromWordID(claimedID).getWordName() + "' and word '" + wordHandle.getWordName()
                    + "' — every form must belong to one word");

        form2WordID.put(form, wordHandle.getWordID());

        if (form.length() < EngineSetting.DIALOGUE_FUZZY_FORM_MIN_LENGTH)
            return;

        for (int i = 0; i < form.length(); i++)
            deletion2WordID.putIfAbsent(toDeletion(form, i), wordHandle.getWordID());
    }

    private String toDeletion(String form, int index) {

        deletionScratch.setLength(0);
        deletionScratch.append(form, 0, index).append(form, index + 1, form.length());

        return deletionScratch.toString();
    }

    // The word a lower-case token is a form of, or is one letter away from a form of — the reserved ID when it is
    // neither
    public int findWordID(String token) {

        int wordID = form2WordID.getInt(token);

        if (wordID != EngineSetting.REGISTRY_RESERVED_ID || token.length() < EngineSetting.DIALOGUE_FUZZY_MIN_LENGTH)
            return wordID;

        wordID = deletion2WordID.getInt(token);

        if (token.length() < EngineSetting.DIALOGUE_FUZZY_FORM_MIN_LENGTH)
            return wordID;

        for (int i = 0; i < token.length() && wordID == EngineSetting.REGISTRY_RESERVED_ID; i++) {

            String deletion = toDeletion(token, i);

            wordID = form2WordID.getInt(deletion);

            if (wordID == EngineSetting.REGISTRY_RESERVED_ID)
                wordID = deletion2WordID.getInt(deletion);
        }

        return wordID;
    }

    // Tags \\

    int registerTagName(String tagName) {

        int tagID = RegistryUtility.registerID(
                tagName2TagID, tagID2WordIDs, tagName,
                EngineSetting.REGISTRY_INT_ID_COUNT);

        if (tagID2WordIDs.get(tagID) != null)
            return tagID;

        tagID2WordIDs.set(tagID, new IntArrayList());
        tagID2Seed.add(DialogueFeatureUtility.hashName(tagName));

        return tagID;
    }

    // A tag no word carries yet is looked for in every file still to load before it is refused
    public int getTagIDFromTagName(String tagName) {

        if (!hasTag(tagName) && internalLoader != null)
            internalLoader.requestAll();

        if (!hasTag(tagName))
            throwException("No dictionary word carries the tag \"" + tagName + "\"");

        return tagName2TagID.getInt(tagName);
    }

    public boolean hasTag(String tagName) {
        return tagName2TagID.getInt(tagName) != EngineSetting.REGISTRY_ID_NONE;
    }

    public IntArrayList getWordIDsFromTagID(int tagID) {
        return tagID2WordIDs.get(tagID);
    }

    // The stable number of a tag's name, which the dialogue models weigh it by
    public int getTagSeed(int tagID) {
        return tagID2Seed.getInt(tagID);
    }
}
