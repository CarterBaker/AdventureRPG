package application.runtime.menueventsmanager.menus.character;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.deedmanager.DeedManager;
import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import application.bootstrap.progressionpipeline.profession.RequirementGroupStruct;
import application.bootstrap.progressionpipeline.profession.RequirementStruct;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.skillmanager.SkillManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class CharacterProfessionBranch extends BranchPackage {

    /*
     * Fills the sheet's Profession page. Every profession is listed by the
     * title the player's gender gives it, marked active, unlocked or locked,
     * with what it is about. A locked one lists what unlocks it — each way
     * when there is more than one — every requirement beside where the player
     * stands on it, in the met or unmet colour, read through
     * ProgressionManager so the page shows exactly what the check uses.
     * Choosing an unlocked profession takes it up.
     */

    // Internal
    private MenuManager menuManager;
    private ProfessionManager professionManager;
    private ProgressionManager progressionManager;
    private TradeManager tradeManager;
    private SkillManager skillManager;
    private DeedManager deedManager;
    private CharacterBranch characterBranch;

    // Colors
    private MenuColorStruct metColor;
    private MenuColorStruct unmetColor;
    private MenuColorStruct activeColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.metColor = new MenuColorStruct(RuntimeSetting.CHARACTER_MET_COLOR);
        this.unmetColor = new MenuColorStruct(RuntimeSetting.CHARACTER_INEPT_COLOR);
        this.activeColor = new MenuColorStruct(RuntimeSetting.CHARACTER_TAB_ACTIVE_LABEL_COLOR);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.tradeManager = get(TradeManager.class);
        this.skillManager = get(SkillManager.class);
        this.deedManager = get(DeedManager.class);
        this.characterBranch = get(CharacterBranch.class);
    }

    // Populate \\

    void populate(CharacterSessionStruct session) {

        for (short professionID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                professionID < professionManager.getProfessionIDCount();
                professionID++) {

            ProfessionHandle professionHandle = professionManager.getProfessionHandleFromProfessionID(professionID);

            injectProfessionRow(session, professionHandle);

            if (!session.getPlayer().getCareerHandle().isUnlocked(professionID))
                populateRequirements(session, professionHandle);
        }
    }

    private void injectProfessionRow(CharacterSessionStruct session, ProfessionHandle professionHandle) {

        EntityInstance player = session.getPlayer();
        CareerHandle careerHandle = player.getCareerHandle();
        short professionID = professionHandle.getProfessionID();
        boolean active = careerHandle.getActiveProfessionID() == professionID;
        String state = active
                ? RuntimeSetting.CHARACTER_PROFESSION_ACTIVE
                : careerHandle.isUnlocked(professionID)
                        ? RuntimeSetting.CHARACTER_PROFESSION_UNLOCKED
                        : RuntimeSetting.CHARACTER_PROFESSION_LOCKED;

        menuManager.inject(
                session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_OPTIONS,
                RuntimeSetting.MENU_CHARACTER_PROFESSION_ROW,
                row -> {
                    row.setActionArgOverride(professionHandle.getProfessionName());
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NAME)
                            .setFontText(professionHandle.getTitle(player.getGenderID()));
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_VALUE).setFontText(state);
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NOTE)
                            .setFontText(professionHandle.getDescription());

                    if (active)
                        row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_VALUE).setColorOverride(activeColor);
                });
    }

    // Requirements \\

    private void populateRequirements(CharacterSessionStruct session, ProfessionHandle professionHandle) {

        int groupCount = professionHandle.getRequirementGroupCount();

        for (int i = 0; i < groupCount; i++) {

            RequirementGroupStruct group = professionHandle.getRequirementGroup(i);

            if (groupCount > 1)
                characterBranch.injectValueRow(
                        session,
                        String.format(RuntimeSetting.CHARACTER_FORMAT_WAY, i + 1),
                        RuntimeSetting.CHARACTER_TEXT_NONE,
                        RuntimeSetting.CHARACTER_TEXT_NONE,
                        null);

            for (int j = 0; j < group.size(); j++)
                injectRequirement(session, professionHandle.getProfessionID(), group.get(j));
        }
    }

    private void injectRequirement(CharacterSessionStruct session, short professionID, RequirementStruct requirement) {

        EntityInstance player = session.getPlayer();
        boolean met = progressionManager.isRequirementMet(player, professionID, requirement);

        characterBranch.injectValueRow(
                session,
                toRequirementText(requirement),
                toStandingText(player, professionID, requirement),
                RuntimeSetting.CHARACTER_TEXT_NONE,
                met ? metColor : unmetColor);
    }

    // What a requirement asks, in words
    private String toRequirementText(RequirementStruct requirement) {
        return switch (requirement.getKind()) {
            case LEVEL -> String.format(RuntimeSetting.CHARACTER_FORMAT_REQUIRE_LEVEL, (int) requirement.getMin());
            case MORALITY -> toMoralityText(requirement);
            case TRADE -> String.format(
                    RuntimeSetting.CHARACTER_FORMAT_REQUIRE_PROFICIENCY,
                    tradeManager.getTradeHandleFromTradeID(requirement.getTargetID()).getDisplayName(),
                    (int) requirement.getMin());
            case SKILL -> String.format(
                    RuntimeSetting.CHARACTER_FORMAT_REQUIRE_PROFICIENCY,
                    skillManager.getSkillHandleFromSkillID(requirement.getTargetID()).getDisplayName(),
                    (int) requirement.getMin());
            case DEED -> String.format(
                    RuntimeSetting.CHARACTER_FORMAT_REQUIRE_DEED,
                    deedManager.getDeedHandleFromDeedID(requirement.getTargetID()).getDisplayName(),
                    (int) requirement.getMin());
            case PROFESSION -> String.format(
                    RuntimeSetting.CHARACTER_FORMAT_REQUIRE_PROFESSION,
                    professionManager.getProfessionHandleFromProfessionID(requirement.getTargetID())
                            .getDisplayName());
            case GRANTED -> RuntimeSetting.CHARACTER_TEXT_REQUIRE_GRANTED;
        };
    }

    private String toMoralityText(RequirementStruct requirement) {

        boolean hasMin = requirement.getMin() > EngineSetting.STAT_DEFAULT_MIN;
        boolean hasMax = requirement.getMax() < EngineSetting.STAT_DEFAULT_MAX;

        if (hasMin && hasMax)
            return String.format(
                    RuntimeSetting.CHARACTER_FORMAT_REQUIRE_MORALITY_RANGE,
                    (int) requirement.getMin(), (int) requirement.getMax());

        return hasMin
                ? String.format(RuntimeSetting.CHARACTER_FORMAT_REQUIRE_MORALITY_MIN, (int) requirement.getMin())
                : String.format(RuntimeSetting.CHARACTER_FORMAT_REQUIRE_MORALITY_MAX, (int) requirement.getMax());
    }

    // Where the player stands on a requirement
    private String toStandingText(EntityInstance player, short professionID, RequirementStruct requirement) {
        return switch (requirement.getKind()) {
            case PROFESSION, GRANTED -> progressionManager.isRequirementMet(player, professionID, requirement)
                    ? RuntimeSetting.CHARACTER_TEXT_YES
                    : RuntimeSetting.CHARACTER_TEXT_NO;
            default -> String.valueOf((int) Math.floor(progressionManager.getRequirementValue(player, requirement)));
        };
    }

    // Switch \\

    public void switchProfession(String professionName, WindowInstance window) {

        CharacterSessionStruct session = characterBranch.getSession(window);

        if (session == null)
            return;

        EntityInstance player = session.getPlayer();
        ProfessionHandle professionHandle = professionManager.getProfessionHandleFromProfessionName(professionName);

        if (!progressionManager.switchProfession(player, professionHandle.getProfessionID()))
            return;

        characterBranch.refresh(session);
        characterBranch.setNotice(session, String.format(
                RuntimeSetting.CHARACTER_FORMAT_SWITCHED, professionHandle.getTitle(player.getGenderID())));
    }
}
