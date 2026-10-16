package application.bootstrap.progressionpipeline.profession;

import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import engine.root.DataPackage;

public class ProfessionData extends DataPackage {

    /*
     * Immutable profession definition loaded from ARPG. A starting profession
     * can be chosen when a character is made; any other is unlocked by meeting
     * every requirement of any one of its groups. While active, it adds its
     * stat bonuses, multiplies how fast the trades it names are learned, and
     * gives an NPC holding it the weights its attribute points are spent by. A
     * being taking it up for the first time starts its trades at least at the
     * starting values named, or at most where those are below zero. A title
     * may replace the display name for a gender. Owned by ProfessionHandle for
     * the engine lifetime.
     */

    // Identity
    private final String professionName;
    private final short professionID;

    // Presentation
    private final String displayName;
    private final String description;
    private final short[] titleGenderIDs;
    private final String[] titles;

    // Access
    private final boolean starting;
    private final RequirementGroupStruct[] requirementGroups;

    // Effects
    private final ValueMapStruct statBonuses;
    private final ValueMapStruct tradeLearning;
    private final ValueMapStruct startingTrades;
    private final ValueMapStruct attributeWeights;

    // Constructor \\

    public ProfessionData(
            String professionName,
            short professionID,
            String displayName,
            String description,
            short[] titleGenderIDs,
            String[] titles,
            boolean starting,
            RequirementGroupStruct[] requirementGroups,
            ValueMapStruct statBonuses,
            ValueMapStruct tradeLearning,
            ValueMapStruct startingTrades,
            ValueMapStruct attributeWeights) {

        // Identity
        this.professionName = professionName;
        this.professionID = professionID;

        // Presentation
        this.displayName = displayName;
        this.description = description;
        this.titleGenderIDs = titleGenderIDs;
        this.titles = titles;

        // Access
        this.starting = starting;
        this.requirementGroups = requirementGroups;

        // Effects
        this.statBonuses = statBonuses;
        this.tradeLearning = tradeLearning;
        this.startingTrades = startingTrades;
        this.attributeWeights = attributeWeights;
    }

    // Accessible \\

    public String getProfessionName() {
        return professionName;
    }

    public short getProfessionID() {
        return professionID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    // The title a being of a gender holds this profession by, the display name when it names none for it
    public String getTitle(short genderID) {

        for (int i = 0; i < titleGenderIDs.length; i++)
            if (titleGenderIDs[i] == genderID)
                return titles[i];

        return displayName;
    }

    public boolean isStarting() {
        return starting;
    }

    public int getRequirementGroupCount() {
        return requirementGroups.length;
    }

    public RequirementGroupStruct getRequirementGroup(int index) {
        return requirementGroups[index];
    }

    public ValueMapStruct getStatBonuses() {
        return statBonuses;
    }

    public ValueMapStruct getTradeLearning() {
        return tradeLearning;
    }

    public ValueMapStruct getStartingTrades() {
        return startingTrades;
    }

    public ValueMapStruct getAttributeWeights() {
        return attributeWeights;
    }
}
