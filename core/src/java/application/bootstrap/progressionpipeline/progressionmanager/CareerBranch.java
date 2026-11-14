package application.bootstrap.progressionpipeline.progressionmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.CareerHandle;
import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import application.bootstrap.progressionpipeline.profession.RequirementGroupStruct;
import application.bootstrap.progressionpipeline.profession.RequirementStruct;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class CareerBranch extends BranchPackage {

    /*
     * Professions for ProgressionManager. markPending() flags a being whose
     * requirements may have moved; evaluate() then checks each profession it
     * has not unlocked, once, and unlocks any whose groups it now meets in
     * full. A profession once unlocked stays unlocked. choose() is how a being
     * starts out — the profession is unlocked and taken up, and the trades it
     * starts newcomers with are seeded. switchTo() takes up any unlocked
     * profession; grant() records that the world itself handed a profession
     * over, as a captain recruits a guard, for the requirements that ask for
     * it. resolveRequirementValue() is the one reading of what a requirement
     * compares, so the character page shows the same progress the check uses.
     */

    // Internal
    private ProfessionManager professionManager;
    private ProficiencyBranch proficiencyBranch;
    private ProgressionSoundBranch progressionSoundBranch;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.professionManager = get(ProfessionManager.class);
        this.proficiencyBranch = get(ProficiencyBranch.class);
        this.progressionSoundBranch = get(ProgressionSoundBranch.class);
    }

    // Evaluate \\

    void markPending(EntityInstance entity) {
        entity.getCareerHandle().setPending(true);
    }

    void evaluate(EntityInstance entity) {

        CareerHandle careerHandle = entity.getCareerHandle();

        careerHandle.setPending(false);

        for (short professionID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                professionID < careerHandle.getProfessionCount();
                professionID++)
            if (!careerHandle.isUnlocked(professionID) && meetsAnyGroup(entity, professionID)) {
                careerHandle.setUnlocked(professionID, true);
                progressionSoundBranch.profession(entity);
            }
    }

    private boolean meetsAnyGroup(EntityInstance entity, short professionID) {

        ProfessionHandle professionHandle = professionManager.getProfessionHandleFromProfessionID(professionID);

        for (int i = 0; i < professionHandle.getRequirementGroupCount(); i++)
            if (meetsGroup(entity, professionID, professionHandle.getRequirementGroup(i)))
                return true;

        return false;
    }

    private boolean meetsGroup(EntityInstance entity, short professionID, RequirementGroupStruct group) {

        for (int i = 0; i < group.size(); i++)
            if (!isMet(entity, professionID, group.get(i)))
                return false;

        return true;
    }

    boolean isMet(EntityInstance entity, short professionID, RequirementStruct requirement) {
        return switch (requirement.getKind()) {
            case PROFESSION -> entity.getCareerHandle().isUnlocked(requirement.getTargetID());
            case GRANTED -> entity.getCareerHandle().isGranted(professionID);
            default -> requirement.accepts(resolveRequirementValue(entity, requirement));
        };
    }

    // The value a requirement compares — a level, a morality, a trade or skill, a tally, or 1 for a profession held
    float resolveRequirementValue(EntityInstance entity, RequirementStruct requirement) {
        return switch (requirement.getKind()) {
            case LEVEL -> entity.getLevel();
            case MORALITY -> entity.getConductHandle().getMorality().getValue();
            case TRADE -> entity.getProficiencyHandle().getTrade(requirement.getTargetID()).getValue();
            case SKILL -> entity.getProficiencyHandle().getSkill(requirement.getTargetID()).getValue();
            case DEED -> entity.getConductHandle().getDeedCount(requirement.getTargetID());
            case PROFESSION -> entity.getCareerHandle().isUnlocked(requirement.getTargetID()) ? 1f : 0f;
            case GRANTED -> 0f;
        };
    }

    // Choose \\

    void choose(EntityInstance entity, short professionID) {

        CareerHandle careerHandle = entity.getCareerHandle();
        ValueMapStruct startingTrades = professionManager.getProfessionHandleFromProfessionID(professionID)
                .getStartingTrades();

        careerHandle.setUnlocked(professionID, true);
        activate(careerHandle, professionID);

        for (int i = 0; i < startingTrades.size(); i++)
            proficiencyBranch.seedTrade(entity, startingTrades.getID(i), startingTrades.getValue(i));
    }

    // Takes up an unlocked profession — false when it is still locked
    boolean switchTo(EntityInstance entity, short professionID) {

        CareerHandle careerHandle = entity.getCareerHandle();

        if (!careerHandle.isUnlocked(professionID))
            return false;

        activate(careerHandle, professionID);

        return true;
    }

    void grant(EntityInstance entity, short professionID) {
        entity.getCareerHandle().setGranted(professionID, true);
        markPending(entity);
    }

    private void activate(CareerHandle careerHandle, short professionID) {

        if (careerHandle.getActiveProfessionID() == professionID)
            return;

        careerHandle.setActiveProfessionID(professionID);
        careerHandle.markChanged();
    }
}
