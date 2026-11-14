package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import application.bootstrap.progressionpipeline.professionmanager.ProfessionManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.bootstrap.progressionpipeline.trademanager.TradeManager;
import application.bootstrap.progressionpipeline.util.ValueMapStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class CreatorProgressionBranch extends BranchPackage {

    /*
     * Fills the creator's Attributes and Profession tabs and the attribute
     * list beside the preview. The creation points are staged into the
     * attributes with a minus and a plus, none rising past the creation cap,
     * and the list beside the preview shows every attribute as it will stand.
     * The Profession tab lists every profession a character may start out in,
     * by the title the character's gender gives it, and shows what the chosen
     * one lends. Nothing is spent until the character is created: apply() then
     * spends the staged points for good and takes up the chosen profession;
     * points left unstaged stay to be spent later.
     */

    // Internal
    private MenuManager menuManager;
    private AttributeManager attributeManager;
    private ProfessionManager professionManager;
    private StatManager statManager;
    private TradeManager tradeManager;
    private ProgressionManager progressionManager;
    private CharacterCreatorBranch characterCreatorBranch;

    // Colors
    private MenuColorStruct selectedColor;
    private MenuColorStruct selectedLabelColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.selectedColor = new MenuColorStruct(RuntimeSetting.CREATOR_TAB_ACTIVE_COLOR);
        this.selectedLabelColor = new MenuColorStruct(RuntimeSetting.CREATOR_TAB_ACTIVE_LABEL_COLOR);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.attributeManager = get(AttributeManager.class);
        this.professionManager = get(ProfessionManager.class);
        this.statManager = get(StatManager.class);
        this.tradeManager = get(TradeManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.characterCreatorBranch = get(CharacterCreatorBranch.class);
    }

    // Stats \\

    // Every attribute as it will stand once the character is made, staged points counted
    void populateStats(CreatorSessionStruct session) {

        ProgressionHandle progressionHandle = session.getPlayer().getProgressionHandle();

        menuManager.ejectAll(session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_STATS);

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < attributeManager.getAttributeIDCount();
                attributeID++) {

            AttributeHandle attributeHandle = attributeManager.getAttributeHandleFromAttributeID(attributeID);
            String value = String.valueOf(
                    progressionHandle.getAttribute(attributeID) + progressionHandle.getStaged(attributeID));

            menuManager.inject(
                    session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_STATS, RuntimeSetting.MENU_CREATOR_STAT_ROW,
                    row -> {
                        row.findChildById(RuntimeSetting.ELEMENT_CREATOR_STAT_NAME)
                                .setFontText(attributeHandle.getDisplayName());
                        row.findChildById(RuntimeSetting.ELEMENT_CREATOR_STAT_VALUE).setFontText(value);
                    });
        }
    }

    // Attributes \\

    void populateAttributes(CreatorSessionStruct session) {

        ProgressionHandle progressionHandle = session.getPlayer().getProgressionHandle();

        characterCreatorBranch.injectSectionHeader(session, String.format(
                RuntimeSetting.CREATOR_FORMAT_POINTS,
                progressionHandle.getUnspentPoints() - progressionHandle.getStagedTotal(),
                EngineSetting.ATTRIBUTE_CREATION_MAX));

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < attributeManager.getAttributeIDCount();
                attributeID++)
            injectAttributeRow(session, attributeManager.getAttributeHandleFromAttributeID(attributeID));
    }

    private void injectAttributeRow(CreatorSessionStruct session, AttributeHandle attributeHandle) {

        ProgressionHandle progressionHandle = session.getPlayer().getProgressionHandle();
        short attributeID = attributeHandle.getAttributeID();
        String value = String.valueOf(progressionHandle.getAttribute(attributeID)
                + progressionHandle.getStaged(attributeID));

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS,
                RuntimeSetting.MENU_CREATOR_ATTRIBUTE_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(attributeHandle.getDisplayName());
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE).setFontText(value);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_PREVIOUS)
                            .setActionArgOverride(attributeHandle.getAttributeName());
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_NEXT)
                            .setActionArgOverride(attributeHandle.getAttributeName());
                });
    }

    public void raiseAttribute(String attributeName, WindowInstance window) {
        stage(attributeName, 1, window);
    }

    public void lowerAttribute(String attributeName, WindowInstance window) {
        stage(attributeName, -1, window);
    }

    private void stage(String attributeName, int points, WindowInstance window) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null
                || !progressionManager.stageCreationAttribute(
                        session.getPlayer(),
                        attributeManager.getAttributeIDFromAttributeName(attributeName),
                        points))
            return;

        characterCreatorBranch.refreshOptions(session);
        populateStats(session);
    }

    // Professions \\

    void populateProfessions(CreatorSessionStruct session) {

        EntityInstance player = session.getPlayer();

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_PROFESSION);

        for (short professionID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                professionID < professionManager.getProfessionIDCount();
                professionID++) {

            ProfessionHandle professionHandle = professionManager.getProfessionHandleFromProfessionID(professionID);

            if (professionHandle.isStarting())
                injectProfessionRow(session, professionHandle, player.getGenderID());
        }

        populateProfessionDetails(session, professionManager.getProfessionHandleFromProfessionID(
                session.getProfessionID()));
    }

    private void injectProfessionRow(CreatorSessionStruct session, ProfessionHandle professionHandle, short genderID) {
        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS,
                RuntimeSetting.MENU_CREATOR_PROFESSION_ROW,
                button -> {
                    ElementInstance label = button.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL);
                    button.setActionArgOverride(professionHandle.getProfessionName());
                    label.setFontText(professionHandle.getTitle(genderID));

                    if (professionHandle.getProfessionID() != session.getProfessionID())
                        return;

                    button.setColorOverride(selectedColor);
                    label.setColorOverride(selectedLabelColor);
                });
    }

    // What the chosen profession is about, and everything it lends
    private void populateProfessionDetails(CreatorSessionStruct session, ProfessionHandle professionHandle) {

        characterCreatorBranch.injectSectionHeader(
                session, professionHandle.getTitle(session.getPlayer().getGenderID()));

        if (!professionHandle.getDescription().isEmpty())
            injectDetail(session, professionHandle.getDescription());

        ValueMapStruct statBonuses = professionHandle.getStatBonuses();

        for (int i = 0; i < statBonuses.size(); i++)
            injectDetail(session, String.format(
                    RuntimeSetting.CREATOR_FORMAT_STAT_BONUS,
                    statManager.getStatHandleFromStatID(statBonuses.getID(i)).getDisplayName(),
                    statBonuses.getValue(i) * EngineSetting.PERCENT_MAX));

        ValueMapStruct tradeLearning = professionHandle.getTradeLearning();

        for (int i = 0; i < tradeLearning.size(); i++)
            injectDetail(session, String.format(
                    RuntimeSetting.CREATOR_FORMAT_TRADE_LEARNING,
                    tradeManager.getTradeHandleFromTradeID(tradeLearning.getID(i)).getDisplayName(),
                    tradeLearning.getValue(i)));

        ValueMapStruct startingTrades = professionHandle.getStartingTrades();

        for (int i = 0; i < startingTrades.size(); i++)
            injectDetail(session, String.format(
                    RuntimeSetting.CREATOR_FORMAT_STARTING_TRADE,
                    tradeManager.getTradeHandleFromTradeID(startingTrades.getID(i)).getDisplayName(),
                    startingTrades.getValue(i)));
    }

    private void injectDetail(CreatorSessionStruct session, String text) {
        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_DETAIL_LINE,
                line -> line.setFontText(text));
    }

    public void selectProfession(String professionName, WindowInstance window) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null)
            return;

        session.setProfessionID(professionManager.getProfessionIDFromProfessionName(professionName));
        characterCreatorBranch.refreshOptions(session);
    }

    // The profession a new character starts out in until another is chosen — the first a character may start in
    short resolveDefaultProfession() {

        for (short professionID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                professionID < professionManager.getProfessionIDCount();
                professionID++)
            if (professionManager.getProfessionHandleFromProfessionID(professionID).isStarting())
                return professionID;

        return throwException("No profession is marked \"starting\" — a character needs one to start out in.");
    }

    // Apply \\

    // Spends the staged creation points and takes up the chosen profession, once the character is made
    void apply(CreatorSessionStruct session) {

        EntityInstance player = session.getPlayer();

        progressionManager.confirmAttributes(player);
        progressionManager.chooseProfession(player, session.getProfessionID());
    }

    void clear(CreatorSessionStruct session) {
        progressionManager.clearStagedAttributes(session.getPlayer());
    }
}
