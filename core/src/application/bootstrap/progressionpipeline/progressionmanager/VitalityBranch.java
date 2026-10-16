package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class VitalityBranch extends BranchPackage {

    /*
     * Health for ProgressionManager, kept as the share of a being's health it
     * has left. wound() is the one path health is lost: a wound that bleeds
     * also freshens the bleeding, and any wound holds regeneration off for a
     * while. Once that while has passed, regenerate() restores health at the
     * being's regeneration stat each second, and bleeding fades with time and
     * faster with every share of health regained. restore() heals in full. A
     * defeated being neither heals nor bleeds.
     */

    // Internal
    private StatManager statManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.statManager = get(StatManager.class);
    }

    // Regenerate \\

    void regenerate(EntityInstance entity, float deltaTime) {

        StatisticsHandle statisticsHandle = entity.getStatisticsHandle();

        if (statisticsHandle.isDefeated())
            return;

        float delay = statisticsHandle.getRegenerationDelay();

        if (delay > 0f) {
            statisticsHandle.setRegenerationDelay(Math.max(0f, delay - deltaTime));
            fadeBleed(statisticsHandle, 0f, deltaTime);
            return;
        }

        float health = statisticsHandle.getHealth();
        float regained = Math.min(
                EngineSetting.VITALITY_FULL - health,
                Math.max(0f, entity.getStat(statManager.getHealthRegenStatID())) * deltaTime);

        statisticsHandle.setHealth(health + regained);
        fadeBleed(statisticsHandle, regained, deltaTime);
    }

    private void fadeBleed(StatisticsHandle statisticsHandle, float regained, float deltaTime) {

        float bleed = statisticsHandle.getBleed();

        if (bleed <= 0f)
            return;

        statisticsHandle.setBleed(Math.max(0f, bleed
                - EngineSetting.BLEED_FADE_PER_SECOND * deltaTime
                - regained * EngineSetting.BLEED_HEAL_SHARE));
    }

    // Wound \\

    // Takes a share of a being's health — true when it leaves the being with none
    boolean wound(EntityInstance entity, float share, boolean bleeding) {

        StatisticsHandle statisticsHandle = entity.getStatisticsHandle();

        if (statisticsHandle.isDefeated() || share <= 0f)
            return false;

        statisticsHandle.setHealth(Math.max(0f, statisticsHandle.getHealth() - share));
        statisticsHandle.setRegenerationDelay(EngineSetting.REGENERATION_DELAY_SECONDS);

        if (bleeding)
            statisticsHandle.setBleed(Math.min(
                    EngineSetting.VITALITY_FULL,
                    statisticsHandle.getBleed() + share * EngineSetting.BLEED_PER_WOUND));

        return statisticsHandle.getHealth() <= 0f;
    }

    // Restore \\

    void restore(EntityInstance entity) {

        StatisticsHandle statisticsHandle = entity.getStatisticsHandle();

        if (statisticsHandle.isDefeated())
            return;

        statisticsHandle.setHealth(EngineSetting.VITALITY_FULL);
        statisticsHandle.setBleed(0f);
        statisticsHandle.setRegenerationDelay(0f);
    }
}
