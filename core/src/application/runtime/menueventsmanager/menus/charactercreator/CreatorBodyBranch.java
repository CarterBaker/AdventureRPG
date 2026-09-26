package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.util.mathematics.vectors.Vector3;

public class CreatorBodyBranch extends BranchPackage {

    /*
     * Fills the Body tab with a slider per CreatorSlider and applies each
     * drag to the player. Height and weight span the ranges the entity
     * template allows; the head sliders span the creator's head proportion
     * range. A slider shows its value beside its name and moves its knob
     * with the cursor while dragged.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private CharacterCreatorBranch characterCreatorBranch;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.characterCreatorBranch = get(CharacterCreatorBranch.class);
    }

    // Populate \\

    void populateBody(CreatorSessionStruct session) {

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_BUILD);
        injectSlider(session, CreatorSlider.HEIGHT);
        injectSlider(session, CreatorSlider.WEIGHT);

        if (!session.getPlayer().hasAppearance())
            return;

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_HEAD_SHAPE);
        injectSlider(session, CreatorSlider.HEAD_WIDTH);
        injectSlider(session, CreatorSlider.HEAD_LENGTH);
        injectSlider(session, CreatorSlider.HEAD_DEPTH);
    }

    private void injectSlider(CreatorSessionStruct session, CreatorSlider slider) {
        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_SLIDER_ROW,
                row -> {
                    ElementInstance track = row.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_TRACK);
                    track.setOnDragArgOverride(slider.name());
                    track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_LABEL).setFontText(slider.getLabel());
                    refreshSlider(track, slider, session.getPlayer());
                });
    }

    // Drag \\

    public void dragSlider(String sliderName, WindowInstance window, ElementInstance track) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null)
            return;

        CreatorSlider slider = CreatorSlider.valueOf(sliderName);
        float fraction = (inputManager.getHoverMouseX(window) - track.getComputedLeft()) / track.getComputedW();

        applyFraction(slider, session.getPlayer(), Math.max(0f, Math.min(1f, fraction)));
        refreshSlider(track, slider, session.getPlayer());
    }

    private void refreshSlider(ElementInstance track, CreatorSlider slider, EntityInstance player) {

        float fraction = resolveFraction(slider, player);

        track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_KNOB).setPositionOverride(new DimensionVector2Struct(
                DimensionValueStruct.ofPercent(fraction * RuntimeSetting.CREATOR_PERCENT_SCALE),
                DimensionValueStruct.ofAbsolute(0f)));
        track.findChildById(RuntimeSetting.ELEMENT_CREATOR_SLIDER_VALUE).setFontText(formatValue(slider, player));
    }

    // Values \\

    private float resolveFraction(CreatorSlider slider, EntityInstance player) {

        float min = resolveMin(slider, player.getEntityData());
        float range = resolveMax(slider, player.getEntityData()) - min;

        return range > 0f ? (resolveValue(slider, player) - min) / range : 0f;
    }

    private void applyFraction(CreatorSlider slider, EntityInstance player, float fraction) {

        float min = resolveMin(slider, player.getEntityData());
        float value = min + (resolveMax(slider, player.getEntityData()) - min) * fraction;

        switch (slider) {
            case HEIGHT -> applyHeight(player, value);
            case WEIGHT -> player.setWeight(value);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> applyHeadProportion(slider, player, value);
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

    private float resolveValue(CreatorSlider slider, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getSize().y;
            case WEIGHT -> player.getWeight();
            case HEAD_WIDTH -> player.getAppearanceHandle().getHeadProportion().x;
            case HEAD_LENGTH -> player.getAppearanceHandle().getHeadProportion().y;
            case HEAD_DEPTH -> player.getAppearanceHandle().getHeadProportion().z;
        };
    }

    private float resolveMin(CreatorSlider slider, EntityData entityData) {
        return switch (slider) {
            case HEIGHT -> entityData.getSizeMin().y;
            case WEIGHT -> entityData.getWeightMin();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN;
        };
    }

    private float resolveMax(CreatorSlider slider, EntityData entityData) {
        return switch (slider) {
            case HEIGHT -> entityData.getSizeMax().y;
            case WEIGHT -> entityData.getWeightMax();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX;
        };
    }

    private String formatValue(CreatorSlider slider, EntityInstance player) {

        float value = resolveValue(slider, player);

        return switch (slider) {
            case HEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_HEIGHT, value);
            case WEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_WEIGHT, value);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> String.format(
                    RuntimeSetting.CREATOR_FORMAT_PERCENT, value * RuntimeSetting.CREATOR_PERCENT_SCALE);
        };
    }

    // Randomize \\

    void randomizeBody(EntityInstance player) {

        EntityData entityData = player.getEntityData();

        player.setSize(entityData.getRandomSize());
        player.setWeight(entityData.getRandomWeight());

        if (!player.hasAppearance())
            return;

        player.getAppearanceHandle().setHeadProportion(
                randomHeadProportion(),
                randomHeadProportion(),
                randomHeadProportion());
    }

    private float randomHeadProportion() {

        float min = RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN;
        return min + (float) Math.random() * (RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX - min);
    }
}
