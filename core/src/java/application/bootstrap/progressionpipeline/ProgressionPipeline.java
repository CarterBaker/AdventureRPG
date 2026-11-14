package application.bootstrap.progressionpipeline;

import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.damagetypemanager.DamageTypeManager;
import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.tiermanager.TierManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import engine.root.PipelinePackage;

public class ProgressionPipeline extends PipelinePackage {

    /*
     * Registers the progression pipeline in dependency order: the attributes
     * stats are fed by, the stats everything else names, genders, gear tiers,
     * damage types, then trades, the skills that feed them, the deeds that
     * practice them and the professions that require all of these.
     * ProgressionManager comes last and runs every being's levels, attributes,
     * trades, skills, conduct, profession and vitality by the same code,
     * player or not. The pipeline is created ahead of the item pipeline, whose
     * definitions name stats and skills, and of the combat pipeline, whose
     * weapons name damage types.
     */

    @Override
    protected void create() {
        create(AttributeManager.class);
        create(StatManager.class);
        create(GenderManager.class);
        create(TierManager.class);
        create(DamageTypeManager.class);
        create(TradeManager.class);
        create(SkillManager.class);
        create(DeedManager.class);
        create(ProfessionManager.class);
        create(ProgressionManager.class);
    }
}
