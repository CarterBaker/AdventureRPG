package application.runtime.menueventsmanager.menus.character;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProficiencyHandle;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.skill.SkillHandle;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.trade.TradeHandle;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ProficiencyStruct;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.util.ProgressionDisplayBranch;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class CharacterProficiencyBranch extends BranchPackage {

    /*
     * Fills the sheet's Trades and Skills pages. Trades are listed under their
     * groups in the order they are defined, each at its whole step, with the
     * peak it once reached while it lies below it, so a craft gone rusty shows
     * how far it can quickly come back. Skills show their own step and the
     * effective step the trades they feed lift them to. Anything below zero —
     * ineptitude — is named in the warning colour.
     */

    // Internal
    private TradeManager tradeManager;
    private SkillManager skillManager;
    private ProgressionManager progressionManager;
    private ProgressionDisplayBranch progressionDisplayBranch;
    private CharacterBranch characterBranch;

    // Colors
    private MenuColorStruct ineptColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.ineptColor = new MenuColorStruct(RuntimeSetting.CHARACTER_INEPT_COLOR);
    }

    @Override
    protected void get() {
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.progressionDisplayBranch = get(ProgressionDisplayBranch.class);
        this.characterBranch = get(CharacterBranch.class);
    }

    // Trades \\

    void populateTrades(CharacterSessionStruct session) {

        ProficiencyHandle proficiencyHandle = session.getPlayer().getProficiencyHandle();
        String group = null;

        for (short tradeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                tradeID < tradeManager.getTradeIDCount();
                tradeID++) {

            TradeHandle tradeHandle = tradeManager.getTradeHandleFromTradeID(tradeID);
            ProficiencyStruct trade = proficiencyHandle.getTrade(tradeID);

            if (!tradeHandle.getGroup().equals(group)) {
                group = tradeHandle.getGroup();
                characterBranch.injectSectionHeader(session, group);
            }

            characterBranch.injectValueRow(
                    session,
                    tradeHandle.getDisplayName(),
                    progressionDisplayBranch.formatStep(trade.getValue()),
                    resolvePeakNote(trade),
                    trade.getValue() < 0f ? ineptColor : null);
        }
    }

    private String resolvePeakNote(ProficiencyStruct proficiency) {

        if (proficiency.getValue() >= proficiency.getPeak())
            return RuntimeSetting.CHARACTER_TEXT_NONE;

        return String.format(
                RuntimeSetting.CHARACTER_FORMAT_PEAK, progressionDisplayBranch.formatStep(proficiency.getPeak()));
    }

    // Skills \\

    void populateSkills(CharacterSessionStruct session) {

        EntityInstance player = session.getPlayer();
        ProficiencyHandle proficiencyHandle = player.getProficiencyHandle();

        characterBranch.injectSectionHeader(session, RuntimeSetting.CHARACTER_SECTION_SKILLS);

        for (short skillID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                skillID < skillManager.getSkillIDCount();
                skillID++) {

            SkillHandle skillHandle = skillManager.getSkillHandleFromSkillID(skillID);
            ProficiencyStruct skill = proficiencyHandle.getSkill(skillID);

            characterBranch.injectValueRow(
                    session,
                    skillHandle.getDisplayName(),
                    progressionDisplayBranch.formatStep(skill.getValue()),
                    String.format(
                            RuntimeSetting.CHARACTER_FORMAT_EFFECTIVE,
                            progressionDisplayBranch.formatStep(progressionManager.getEffectiveSkill(player, skillID))),
                    skill.getValue() < 0f ? ineptColor : null);
        }
    }
}
