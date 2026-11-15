package application.bootstrap.dialoguepipeline.dialoguemanager;

import application.bootstrap.dialoguepipeline.mood.MoodHandle;
import application.bootstrap.dialoguepipeline.moodmanager.MoodManager;
import application.bootstrap.dialoguepipeline.relation.RelationHandle;
import application.bootstrap.dialoguepipeline.relationmanager.RelationManager;
import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import application.bootstrap.dialoguepipeline.traitmanager.TraitManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.dialoguepipeline.util.DialogueProfileStruct;
import application.bootstrap.dialoguepipeline.wordmanager.WordManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.DispositionHandle;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class DialogueBeingBranch extends BranchPackage {

    /*
     * Joins a living being to the talks held with it, for DialogueManager.
     * dispose() gives a being its nature as it enters the world: the traits
     * its definition always has, as many more drawn from its pool by its
     * identity — never one it has or one opposed to one it has — a name drawn
     * from the dictionary's names for its gender the same way, and every mood
     * resting where its traits lean it. relate() binds two beings, each
     * holding what the other is to it. read() copies a being into an
     * exchange's profile as it stands toward a speaker, its moods first
     * faded toward rest for the game time since they were last settled, in
     * one step however long that was. write() takes back what a turn
     * changed: the regard it moved, through ProgressionManager, and the
     * moods it left.
     */

    // Internal
    private ProgressionManager progressionManager;
    private ProfessionManager professionManager;
    private GenderManager genderManager;
    private TraitManager traitManager;
    private MoodManager moodManager;
    private RelationManager relationManager;
    private WordManager wordManager;

    // Scratch
    private IntArrayList nameScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.nameScratch = new IntArrayList();
    }

    @Override
    protected void get() {

        // Internal
        this.progressionManager = get(ProgressionManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.genderManager = get(GenderManager.class);
        this.traitManager = get(TraitManager.class);
        this.moodManager = get(MoodManager.class);
        this.relationManager = get(RelationManager.class);
        this.wordManager = get(WordManager.class);
    }

    // Dispose \\

    void dispose(EntityInstance entity, short[] traitIDs, short[] traitPoolIDs, int traitDraws, long identity) {

        DispositionHandle dispositionHandle = entity.getDispositionHandle();

        for (int i = 0; i < traitIDs.length; i++)
            dispositionHandle.addTrait(traitIDs[i]);

        drawTraits(dispositionHandle, traitPoolIDs, traitDraws, identity);
        dispositionHandle.setName(drawName(entity, identity));
        dispositionHandle.setMoodTime(progressionManager.getTime());

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                moodID < dispositionHandle.getMoodIDCount(); moodID++)
            dispositionHandle.setMoodLevel(moodID, resolveMoodRest(dispositionHandle, moodID));
    }

    // Traits drawn from the pool by the identity, each try a fresh roll, until enough are held or the tries run out
    private void drawTraits(DispositionHandle dispositionHandle, short[] traitPoolIDs, int traitDraws, long identity) {

        int held = 0;

        for (int i = 0; held < traitDraws && i < traitPoolIDs.length * EngineSetting.DIALOGUE_TRAIT_DRAW_TRIES; i++) {

            short traitID = traitPoolIDs[Math.min(
                    traitPoolIDs.length - 1,
                    (int) (BiomeFieldUtility.hash01(identity ^ (EngineSetting.DIALOGUE_TRAIT_SALT + i))
                            * traitPoolIDs.length))];

            if (dispositionHandle.hasTrait(traitID) || isOpposed(dispositionHandle, traitID))
                continue;

            dispositionHandle.addTrait(traitID);
            held++;
        }
    }

    private boolean isOpposed(DispositionHandle dispositionHandle, short traitID) {

        for (int i = 0; i < dispositionHandle.getTraitCount(); i++)
            if (traitManager.areOpposed(dispositionHandle.getTraitID(i), traitID))
                return true;

        return false;
    }

    // A dictionary name for the being's gender, drawn by its identity — any name when none is kept for its gender,
    // none at all when the dictionary keeps no names
    private String drawName(EntityInstance entity, long identity) {

        if (!wordManager.hasTag(EngineSetting.DIALOGUE_TAG_NAME))
            return EngineSetting.DIALOGUE_TEXT_NONE;

        IntArrayList nameIDs = wordManager.getWordIDsFromTagID(
                wordManager.getTagIDFromTagName(EngineSetting.DIALOGUE_TAG_NAME));
        String genderName = genderManager.getGenderHandleFromGenderID(entity.getGenderID()).getGenderName();

        nameScratch.clear();

        if (wordManager.hasTag(genderName)) {

            int genderTagID = wordManager.getTagIDFromTagName(genderName);

            for (int i = 0; i < nameIDs.size(); i++)
                if (carriesTag(nameIDs.getInt(i), genderTagID))
                    nameScratch.add(nameIDs.getInt(i));
        }

        IntArrayList pool = nameScratch.isEmpty() ? nameIDs : nameScratch;

        if (pool.isEmpty())
            return EngineSetting.DIALOGUE_TEXT_NONE;

        return wordManager.getWordHandleFromWordID(pool.getInt(Math.min(
                pool.size() - 1,
                (int) (BiomeFieldUtility.hash01(identity ^ EngineSetting.DIALOGUE_NAME_SALT) * pool.size()))))
                .getWordName();
    }

    private boolean carriesTag(int wordID, int tagID) {

        int[] tagIDs = wordManager.getWordHandleFromWordID(wordID).getTagIDs();

        for (int i = 0; i < tagIDs.length; i++)
            if (tagIDs[i] == tagID)
                return true;

        return false;
    }

    // Relate \\

    // The other becomes the relation to the being, and the being the relation's inverse to the other
    void relate(EntityInstance entity, EntityInstance other, short relationID) {

        RelationHandle relationHandle = relationManager.getRelationHandleFromRelationID(relationID);
        RelationHandle inverseHandle = relationManager.getRelationHandleFromRelationID(relationHandle.getInverseID());

        entity.getDispositionHandle().setRelation(
                other.getConductHandle().getIdentity(), relationID, relationHandle.getBond());
        other.getDispositionHandle().setRelation(
                entity.getConductHandle().getIdentity(), inverseHandle.getRelationID(), inverseHandle.getBond());
    }

    // Read \\

    // The listener as it stands toward the speaker right now, copied into the exchange's profile
    void read(DialogueExchangeStruct exchange, EntityInstance listener, EntityInstance speaker) {

        DialogueProfileStruct profile = exchange.getProfile();
        DispositionHandle dispositionHandle = listener.getDispositionHandle();
        short professionID = listener.getCareerHandle().getActiveProfessionID();
        String speakerName = speaker.getDispositionHandle().getName();

        settle(dispositionHandle);
        profile.clear();
        profile.setName(dispositionHandle.getName());
        profile.setSpeakerName(speakerName.isEmpty() ? EngineSetting.DIALOGUE_SPEAKER_NAME_DEFAULT : speakerName);
        profile.setProfessionID(professionID);
        profile.setAffinity(progressionManager.getOpinion(listener, speaker));
        profile.setMorality(progressionManager.getMoralityShade(listener));
        profile.setRelationID(dispositionHandle.getRelationID(speaker.getConductHandle().getIdentity()));

        if (professionID != EngineSetting.REGISTRY_RESERVED_ID)
            profile.setTitle(professionManager.getProfessionHandleFromProfessionID(professionID)
                    .getTitle(listener.getGenderID()));

        for (int i = 0; i < dispositionHandle.getTraitCount(); i++) {

            TraitHandle traitHandle = traitManager.getTraitHandleFromTraitID(dispositionHandle.getTraitID(i));

            profile.addTrait(traitHandle.getTraitID(), traitHandle.isWordless());
        }

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                moodID < dispositionHandle.getMoodIDCount(); moodID++)
            profile.setMoodLevel(moodID, dispositionHandle.getMoodLevel(moodID));
    }

    // Every mood faded toward where the being's traits rest it, by the game time since it was last settled
    private void settle(DispositionHandle dispositionHandle) {

        double time = progressionManager.getTime();
        double elapsed = time - dispositionHandle.getMoodTime();

        dispositionHandle.setMoodTime(time);

        if (elapsed <= 0.0)
            return;

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                moodID < dispositionHandle.getMoodIDCount(); moodID++) {

            MoodHandle moodHandle = moodManager.getMoodHandleFromMoodID(moodID);
            float rest = resolveMoodRest(dispositionHandle, moodID);
            float kept = (float) Math.pow(0.5, elapsed / moodHandle.getHalfLifeSeconds());

            dispositionHandle.setMoodLevel(moodID, rest + (dispositionHandle.getMoodLevel(moodID) - rest) * kept);
        }
    }

    // The level a mood rests at in a being, the sum of what its traits lean it to, never past full
    private float resolveMoodRest(DispositionHandle dispositionHandle, short moodID) {

        float rest = 0f;

        for (int i = 0; i < dispositionHandle.getTraitCount(); i++)
            rest += traitManager.getTraitHandleFromTraitID(dispositionHandle.getTraitID(i))
                    .getMoodLeans().find(moodID, 0f);

        return Math.min(1f, rest);
    }

    // Write \\

    // What the turn changed in the exchange's profile taken back by the listener
    void write(DialogueExchangeStruct exchange, EntityInstance listener, EntityInstance speaker) {

        DialogueProfileStruct profile = exchange.getProfile();
        DispositionHandle dispositionHandle = listener.getDispositionHandle();

        progressionManager.shiftAffinity(listener, speaker, exchange.getAffinityShift());
        dispositionHandle.setMoodTime(progressionManager.getTime());

        for (short moodID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                moodID < dispositionHandle.getMoodIDCount(); moodID++)
            dispositionHandle.setMoodLevel(moodID, profile.getMoodLevel(moodID));
    }
}
