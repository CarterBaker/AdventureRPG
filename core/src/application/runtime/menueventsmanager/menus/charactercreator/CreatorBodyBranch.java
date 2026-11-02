package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.util.mathematics.vectors.Vector3;

public class CreatorBodyBranch extends BranchPackage {

    /*
     * Fills the Body tab with the character's gender, its build and head
     * sliders and the body group of its shape sliders, and owns every slider
     * the creator shows — the Appearance and Hair tabs inject theirs through
     * injectSlider() — applying each to the player. The gender steps through
     * only the genders the race allows. Height and weight span the ranges the
     * entity template allows, the head sliders the creator's head proportion
     * range, a shape slider its own range and a detail its creator range. A
     * slider shows its value beside its name and moves its knob with the
     * cursor while dragged.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private CharacterCreatorBranch characterCreatorBranch;
    private GenderManager genderManager;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.characterCreatorBranch = get(CharacterCreatorBranch.class);
        this.genderManager = get(GenderManager.class);
    }

    // Populate \\

    void populateBody(CreatorSessionStruct session) {

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_GENDER);
        injectGenderRow(session);

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_BUILD);
        injectSlider(session, CreatorSlider.HEIGHT, 0);
        injectSlider(session, CreatorSlider.WEIGHT, 0);

        if (!session.getPlayer().hasAppearance())
            return;

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_HEAD_SHAPE);
        injectSlider(session, CreatorSlider.HEAD_WIDTH, 0);
        injectSlider(session, CreatorSlider.HEAD_LENGTH, 0);
        injectSlider(session, CreatorSlider.HEAD_DEPTH, 0);

        injectShapeGroups(session, true);
    }

    // Every shape slider of the body group, or of every other group, under a header per group in the order the
    // template first lists each
    void injectShapeGroups(CreatorSessionStruct session, boolean bodyGroup) {

        AppearanceData appearanceData = session.getPlayer().getEntityData().getAppearanceData();

        for (int i = 0; i < appearanceData.getShapeCount(); i++) {

            String groupName = appearanceData.getShape(i).getGroupName();

            if (groupName.equals(RuntimeSetting.CREATOR_SHAPE_GROUP_BODY) != bodyGroup
                    || isGroupListedBefore(appearanceData, groupName, i))
                continue;

            characterCreatorBranch.injectSectionHeader(session, toTitle(groupName));

            for (int j = i; j < appearanceData.getShapeCount(); j++)
                if (appearanceData.getShape(j).getGroupName().equals(groupName))
                    injectSlider(session, CreatorSlider.SHAPE, j);
        }
    }

    private boolean isGroupListedBefore(AppearanceData appearanceData, String groupName, int shapeIndex) {

        for (int i = 0; i < shapeIndex; i++)
            if (appearanceData.getShape(i).getGroupName().equals(groupName))
                return true;

        return false;
    }

    void injectSlider(CreatorSessionStruct session, CreatorSlider slider, int index) {

        String argument = slider.name() + RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR + index;
        String label = resolveLabel(slider, index, session.getPlayer());

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_SLIDER_ROW,
                row -> {
                    ElementInstance track = row.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_TRACK);
                    track.setOnDragArgOverride(argument);
                    track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_LABEL).setFontText(label);
                    refreshSlider(track, slider, index, session.getPlayer());
                });
    }

    // Gender \\

    private void injectGenderRow(CreatorSessionStruct session) {

        String genderName = genderManager.getGenderHandleFromGenderID(session.getPlayer().getGenderID())
                .getDisplayName();

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_GENDER_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(RuntimeSetting.CREATOR_SECTION_GENDER);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE).setFontText(genderName);
                });
    }

    public void previousGender(WindowInstance window) {
        cycleGender(window, -1);
    }

    public void nextGender(WindowInstance window) {
        cycleGender(window, 1);
    }

    private void cycleGender(WindowInstance window, int step) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null)
            return;

        EntityInstance player = session.getPlayer();
        EntityData entityData = player.getEntityData();
        int index = 0;

        for (int i = 0; i < entityData.getGenderCount(); i++)
            if (entityData.getGenderID(i) == player.getGenderID())
                index = i;

        player.setGenderID(entityData.getGenderID(Math.floorMod(index + step, entityData.getGenderCount())));
        characterCreatorBranch.refreshOptions(session);
    }

    // Drag \\

    public void dragSlider(String argument, WindowInstance window, ElementInstance track) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null)
            return;

        String[] parts = argument.split(RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR);
        CreatorSlider slider = CreatorSlider.valueOf(parts[0]);
        int index = Integer.parseInt(parts[1]);
        float fraction = (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW();

        applyFraction(slider, index, session.getPlayer(), Math.max(0f, Math.min(1f, fraction)));
        refreshSlider(track, slider, index, session.getPlayer());
    }

    private void refreshSlider(ElementInstance track, CreatorSlider slider, int index, EntityInstance player) {

        float fraction = resolveFraction(slider, index, player);

        track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_KNOB).setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(fraction * RuntimeSetting.CREATOR_PERCENT_SCALE),
                DimensionValueStruct.ofAbsolute(0f)));
        track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_VALUE)
                .setFontText(formatValue(slider, index, player));
    }

    // Values \\

    private float resolveFraction(CreatorSlider slider, int index, EntityInstance player) {

        float min = resolveMin(slider, index, player);
        float range = resolveMax(slider, index, player) - min;

        return range > 0f ? (resolveValue(slider, index, player) - min) / range : 0f;
    }

    private void applyFraction(CreatorSlider slider, int index, EntityInstance player, float fraction) {

        float min = resolveMin(slider, index, player);
        float value = min + (resolveMax(slider, index, player) - min) * fraction;

        switch (slider) {
            case HEIGHT -> applyHeight(player, value);
            case WEIGHT -> player.setWeight(value);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> applyHeadProportion(slider, player, value);
            case SHAPE -> player.getAppearanceHandle().setShapeValue(index, value);
            case DETAIL -> player.getAppearanceHandle().setDetail(SkinnedDetail.VALUES[index], value);
        }
    }

    private void applyHeight(EntityInstance player, float height) {

        Vector3 size = player.getSize();
        size.y = height;
        player.setSize(size);
    }

    private void applyHeadProportion(CreatorSlider slider, EntityInstance player, float proportion) {

        AppearanceHandle appearance = player.getAppearanceHandle();
        Vector3 headProportion = appearance.getHeadProportion();

        appearance.setHeadProportion(
                slider == CreatorSlider.HEAD_WIDTH ? proportion : headProportion.x,
                slider == CreatorSlider.HEAD_LENGTH ? proportion : headProportion.y,
                slider == CreatorSlider.HEAD_DEPTH ? proportion : headProportion.z);
    }

    private float resolveValue(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getSize().y;
            case WEIGHT -> player.getWeight();
            case HEAD_WIDTH -> player.getAppearanceHandle().getHeadProportion().x;
            case HEAD_LENGTH -> player.getAppearanceHandle().getHeadProportion().y;
            case HEAD_DEPTH -> player.getAppearanceHandle().getHeadProportion().z;
            case SHAPE -> player.getAppearanceHandle().getShapeValue(index);
            case DETAIL -> player.getAppearanceHandle().getDetail(SkinnedDetail.VALUES[index]);
        };
    }

    private float resolveMin(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getEntityData().getSizeMin().y;
            case WEIGHT -> player.getEntityData().getWeightMin();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN;
            case SHAPE -> player.getEntityData().getAppearanceData().getShape(index).getMin();
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_MIN[index];
        };
    }

    private float resolveMax(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getEntityData().getSizeMax().y;
            case WEIGHT -> player.getEntityData().getWeightMax();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX;
            case SHAPE -> player.getEntityData().getAppearanceData().getShape(index).getMax();
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_MAX[index];
        };
    }

    private String formatValue(CreatorSlider slider, int index, EntityInstance player) {

        float value = resolveValue(slider, index, player);

        return switch (slider) {
            case HEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_HEIGHT, value);
            case WEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_WEIGHT, value);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH, SHAPE, DETAIL -> String.format(
                    RuntimeSetting.CREATOR_FORMAT_PERCENT, value * RuntimeSetting.CREATOR_PERCENT_SCALE);
        };
    }

    // Text \\

    private String resolveLabel(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case SHAPE -> toTitle(player.getEntityData().getAppearanceData().getShape(index).getMorphName());
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_LABELS[index];
            default -> slider.getLabel();
        };
    }

    // A lower-case name split at its separators into capitalised words
    private String toTitle(String name) {

        String[] words = name.split(RuntimeSetting.CREATOR_SHAPE_NAME_SEPARATOR);

        for (int i = 0; i < words.length; i++)
            if (!words[i].isEmpty())
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);

        return String.join(RuntimeSetting.CREATOR_WORD_SEPARATOR, words);
    }

    // Randomize \\

    void randomizeBody(EntityInstance player) {

        EntityData entityData = player.getEntityData();

        player.setGenderID(entityData.getGenderID((int) (Math.random() * entityData.getGenderCount())));
        player.setSize(entityData.getRandomSize());
        player.setWeight(entityData.getRandomWeight());

        if (!player.hasAppearance())
            return;

        player.getAppearanceHandle().setHeadProportion(
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX),
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX),
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX));

        AppearanceData appearanceData = entityData.getAppearanceData();

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            player.getAppearanceHandle().setShapeValue(
                    i,
                    randomBetween(appearanceData.getShape(i).getMin(), appearanceData.getShape(i).getMax()));
    }

    private float randomBetween(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }
}
