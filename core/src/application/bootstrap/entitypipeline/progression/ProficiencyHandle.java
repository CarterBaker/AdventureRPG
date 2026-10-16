package application.bootstrap.entitypipeline.progression;

import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProficiencyStruct;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class ProficiencyHandle extends HandlePackage {

    /*
     * Per-being trades and skills, each a ProficiencyStruct indexed by its
     * registry ID. The revision counts every change in a whole step of any of
     * them — all their bonuses read — so stats are rebuilt only when a step is
     * crossed, never for the fractions between. Only ProgressionManager
     * changes it. Lives on EntityInstance.
     */

    // Internal
    private TradeManager tradeManager;
    private SkillManager skillManager;

    // Proficiencies
    private ProficiencyStruct[] trades;
    private ProficiencyStruct[] skills;

    // Revision
    private int revision;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
    }

    @Override
    protected void awake() {

        // Proficiencies
        this.trades = createProficiencies(tradeManager.getTradeIDCount());
        this.skills = createProficiencies(skillManager.getSkillIDCount());
    }

    private ProficiencyStruct[] createProficiencies(int count) {

        ProficiencyStruct[] proficiencies = new ProficiencyStruct[count];

        for (int id = EngineSetting.REGISTRY_RESERVED_ID + 1; id < count; id++)
            proficiencies[id] = new ProficiencyStruct();

        return proficiencies;
    }

    // Management \\

    public void reset(double time) {

        for (int tradeID = EngineSetting.REGISTRY_RESERVED_ID + 1; tradeID < trades.length; tradeID++)
            trades[tradeID].reset(0f, time);

        for (int skillID = EngineSetting.REGISTRY_RESERVED_ID + 1; skillID < skills.length; skillID++)
            skills[skillID].reset(0f, time);

        markChanged();
    }

    public void markChanged() {
        revision++;
    }

    // Accessible \\

    public ProficiencyStruct getTrade(short tradeID) {
        return trades[tradeID];
    }

    public ProficiencyStruct getSkill(short skillID) {
        return skills[skillID];
    }

    public int getTradeCount() {
        return trades.length;
    }

    public int getSkillCount() {
        return skills.length;
    }

    public int getRevision() {
        return revision;
    }
}
