package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ProgressionSoundBranch extends BranchPackage {

    /*
     * Sounds a player's progress where the player stands: a level gained, a
     * skill or trade risen a step, a profession opened to it. Every being
     * progresses the same way, but only a player is heard doing it.
     */

    // Internal
    private PlayerManager playerManager;
    private SoundCueManager soundCueManager;

    // Cues
    private int levelUpCueID;
    private int skillCueID;
    private int professionCueID;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.levelUpCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_LEVEL_UP);
        this.skillCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SKILL);
        this.professionCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_PROFESSION);
    }

    // Moments \\

    void levelUp(EntityInstance entity) {
        playFor(entity, levelUpCueID);
    }

    void skill(EntityInstance entity) {
        playFor(entity, skillCueID);
    }

    void profession(EntityInstance entity) {
        playFor(entity, professionCueID);
    }

    private void playFor(EntityInstance entity, int cueID) {
        if (playerManager.isPlayer(entity))
            soundCueManager.playCueAt(cueID, entity.getWorldHandle(), entity.getWorldPositionStruct());
    }
}
