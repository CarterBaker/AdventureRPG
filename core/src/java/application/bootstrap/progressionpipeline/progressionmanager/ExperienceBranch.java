package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class ExperienceBranch extends BranchPackage {

    /*
     * Levels and experience for ProgressionManager. An even-level kill is
     * worth experience in proportion to the level it was made at, and a level
     * takes one more such kill to clear than the level before it, so every
     * level is a little harder than the last, forever. The window above a
     * level widens with it: a kill above the killer counts at its own level up
     * to the top of the window, a kill below the killer counts for nothing,
     * and gear can be wielded up to the same top. award() banks experience as
     * progress through the current level and carries any overflow into the
     * levels it clears. setLevel() is the one place a level changes: it keeps
     * the being's inventory refusing gear above its window, marks its stats
     * and its profession requirements to be checked again, and a gained level
     * also grants its attribute points, heals in full and counts a level-up
     * for anything showing one.
     */

    // Internal
    private CareerBranch careerBranch;
    private VitalityBranch vitalityBranch;
    private ProgressionSoundBranch progressionSoundBranch;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.careerBranch = get(CareerBranch.class);
        this.vitalityBranch = get(VitalityBranch.class);
        this.progressionSoundBranch = get(ProgressionSoundBranch.class);
    }

    // Curve \\

    double toKillExperience(int level) {
        return EngineSetting.EXPERIENCE_PER_KILL_LEVEL * (double) level;
    }

    double toKillsForLevel(int level) {
        return EngineSetting.KILLS_FOR_FIRST_LEVEL
                + EngineSetting.KILLS_ADDED_PER_LEVEL * (double) (level - EngineSetting.LEVEL_START);
    }

    double toLevelExperience(int level) {
        return toKillsForLevel(level) * toKillExperience(level);
    }

    // Window \\

    int toWindow(int level) {
        return EngineSetting.LEVEL_WINDOW_BASE + (int) Math.floor(level * EngineSetting.LEVEL_WINDOW_PER_LEVEL);
    }

    int toWieldLevel(int level) {
        return level + toWindow(level);
    }

    // The lowest level whose window reaches an item level
    int toRequiredLevel(int itemLevel) {

        int level = Math.max(EngineSetting.LEVEL_START, itemLevel - toWindow(itemLevel));

        while (toWieldLevel(level) < itemLevel)
            level++;

        return level;
    }

    // The level a kill counts at for its killer — LEVEL_NONE for a victim below the killer
    int toKillLevel(int killerLevel, int victimLevel) {

        if (victimLevel < killerLevel)
            return EngineSetting.LEVEL_NONE;

        return Math.min(victimLevel, toWieldLevel(killerLevel));
    }

    // Award \\

    void award(EntityInstance entity, double experience) {

        if (experience <= 0.0)
            return;

        ProgressionHandle progressionHandle = entity.getProgressionHandle();
        double progress = progressionHandle.getProgress()
                + experience / toLevelExperience(progressionHandle.getLevel());

        while (progress >= 1.0) {

            double carried = (progress - 1.0) * toLevelExperience(progressionHandle.getLevel());

            gainLevel(entity);
            progress = carried / toLevelExperience(progressionHandle.getLevel());
        }

        progressionHandle.setProgress(progress);
    }

    private void gainLevel(EntityInstance entity) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        setLevel(entity, progressionHandle.getLevel() + 1);
        progressionHandle.setUnspentPoints(
                progressionHandle.getUnspentPoints() + EngineSetting.ATTRIBUTE_POINTS_PER_LEVEL);
        progressionHandle.countLevelUp();
        vitalityBranch.restore(entity);
        progressionSoundBranch.levelUp(entity);
    }

    // Level \\

    void setLevel(EntityInstance entity, int level) {

        ProgressionHandle progressionHandle = entity.getProgressionHandle();

        progressionHandle.setLevel(Math.max(EngineSetting.LEVEL_START, level));
        progressionHandle.markChanged();
        entity.getInventoryHandle().setWieldLevel(toWieldLevel(progressionHandle.getLevel()));
        careerBranch.markPending(entity);
    }
}
