package application.runtime.menueventsmanager.menus.character;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.progression.ProgressionHandle;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import application.bootstrap.progressionpipeline.attributemanager.AttributeManager;
import application.bootstrap.progressionpipeline.progressionmanager.ProgressionManager;
import application.bootstrap.progressionpipeline.stat.StatHandle;
import application.bootstrap.progressionpipeline.statmanager.StatManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.util.ProgressionDisplayBranch;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

public class CharacterAttributeBranch extends BranchPackage {

    /*
     * Fills the sheet's Attributes page and spends attribute points. Each
     * attribute shows its value with a minus and a plus beside it that stage a
     * point out of or into it; staged points count for nothing yet, and the
     * page previews every stat as it would stand once they are spent next to
     * where it stands now. Confirm warns the first time that spent points are
     * permanent and spends them the second; Clear lets a staging go. Any
     * staging change disarms the warning, so nothing is spent by accident.
     */

    // Internal
    private MenuManager menuManager;
    private AttributeManager attributeManager;
    private StatManager statManager;
    private ProgressionManager progressionManager;
    private ProgressionDisplayBranch progressionDisplayBranch;
    private CharacterBranch characterBranch;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.attributeManager = get(AttributeManager.class);
        this.statManager = get(StatManager.class);
        this.progressionManager = get(ProgressionManager.class);
        this.progressionDisplayBranch = get(ProgressionDisplayBranch.class);
        this.characterBranch = get(CharacterBranch.class);
    }

    // Populate \\

    void populate(CharacterSessionStruct session) {

        EntityInstance player = session.getPlayer();
        ProgressionHandle progressionHandle = player.getProgressionHandle();
        int staged = progressionHandle.getStagedTotal();

        characterBranch.injectSectionHeader(session, String.format(
                RuntimeSetting.CHARACTER_FORMAT_POINTS, progressionHandle.getUnspentPoints() - staged, staged));

        for (short attributeID = EngineSetting.REGISTRY_RESERVED_ID + 1;
                attributeID < attributeManager.getAttributeIDCount();
                attributeID++)
            injectAttributeRow(session, attributeManager.getAttributeHandleFromAttributeID(attributeID));

        populatePreview(session, staged > 0);
    }

    private void injectAttributeRow(CharacterSessionStruct session, AttributeHandle attributeHandle) {

        ProgressionHandle progressionHandle = session.getPlayer().getProgressionHandle();
        short attributeID = attributeHandle.getAttributeID();
        int staged = progressionHandle.getStaged(attributeID);
        String value = staged > 0
                ? String.format(
                        RuntimeSetting.CHARACTER_FORMAT_STAGED,
                        progressionHandle.getAttribute(attributeID), staged)
                : String.valueOf(progressionHandle.getAttribute(attributeID));

        menuManager.inject(
                session.getSheetMenu(), RuntimeSetting.ENTRY_CHARACTER_OPTIONS,
                RuntimeSetting.MENU_CHARACTER_ATTRIBUTE_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NAME)
                            .setFontText(attributeHandle.getDisplayName());
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_VALUE).setFontText(value);
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_NOTE)
                            .setFontText(attributeHandle.getDescription());
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_LOWER)
                            .setActionArgOverride(attributeHandle.getAttributeName());
                    row.findChildById(RuntimeSetting.ELEMENT_CHARACTER_ROW_RAISE)
                            .setActionArgOverride(attributeHandle.getAttributeName());
                });
    }

    // Every stat the page shows, as it stands and, while points are staged, as it would stand once they are spent
    private void populatePreview(CharacterSessionStruct session, boolean staged) {

        EntityInstance player = session.getPlayer();
        float[] preview = session.getPreview();

        characterBranch.injectSectionHeader(session, RuntimeSetting.CHARACTER_SECTION_STATS);

        if (staged)
            progressionManager.previewStats(player, preview);

        for (short statID = EngineSetting.REGISTRY_RESERVED_ID + 1; statID < statManager.getStatIDCount(); statID++) {

            StatHandle statHandle = statManager.getStatHandleFromStatID(statID);

            if (!statHandle.isShown())
                continue;

            String current = progressionDisplayBranch.formatStat(player, statID);
            String value = staged
                    ? String.format(
                            RuntimeSetting.CHARACTER_FORMAT_PREVIEW,
                            current,
                            progressionDisplayBranch.formatStatValue(statID, preview[statID], player.getLevel()))
                    : current;

            characterBranch.injectValueRow(
                    session, statHandle.getDisplayName(), value, RuntimeSetting.CHARACTER_TEXT_NONE, null);
        }
    }

    // Stage \\

    public void raiseAttribute(String attributeName, WindowInstance window) {
        stage(attributeName, 1, window);
    }

    public void lowerAttribute(String attributeName, WindowInstance window) {
        stage(attributeName, -1, window);
    }

    private void stage(String attributeName, int points, WindowInstance window) {

        CharacterSessionStruct session = characterBranch.getSession(window);

        if (session == null
                || !progressionManager.stageAttribute(
                        session.getPlayer(),
                        attributeManager.getAttributeIDFromAttributeName(attributeName),
                        points))
            return;

        session.setConfirmArmed(false);
        characterBranch.refresh(session);
    }

    // Confirm \\

    public void confirmAttributes(WindowInstance window) {

        CharacterSessionStruct session = characterBranch.getSession(window);

        if (session == null)
            return;

        if (session.getPlayer().getProgressionHandle().getStagedTotal() == 0) {
            characterBranch.setNotice(session, RuntimeSetting.CHARACTER_TEXT_NOTHING_STAGED);
            return;
        }

        if (!session.isConfirmArmed()) {
            session.setConfirmArmed(true);
            characterBranch.setNotice(session, RuntimeSetting.CHARACTER_TEXT_CONFIRM_WARNING);
            return;
        }

        session.setConfirmArmed(false);
        progressionManager.confirmAttributes(session.getPlayer());
        characterBranch.refresh(session);
        characterBranch.setNotice(session, RuntimeSetting.CHARACTER_TEXT_CONFIRMED);
    }

    public void clearAttributes(WindowInstance window) {

        CharacterSessionStruct session = characterBranch.getSession(window);

        if (session == null)
            return;

        session.setConfirmArmed(false);
        progressionManager.clearStagedAttributes(session.getPlayer());
        characterBranch.refresh(session);
    }
}
