package application.runtime.menueventsmanager.menus.charactercreator;

import java.util.Arrays;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CreatorAppearanceBranch extends BranchPackage {

    /*
     * Fills the Appearance and Hair tabs and applies their choices to the
     * player's AppearanceHandle. Every feature slot is a stepper over the
     * features FeatureManager holds that fit the character, sorted by name,
     * led by None for a slot that may be left empty — a new feature file
     * shows up here with no code change. Skin and hair colors are swatch
     * grids painted from the character's palettes.
     */

    // Face slots, in the order the Appearance tab lists them
    private static final FeatureSlot[] FACE_SLOTS = {
            FeatureSlot.HEAD, FeatureSlot.EYES, FeatureSlot.BROWS, FeatureSlot.NOSE, FeatureSlot.MOUTH };

    // Internal
    private MenuManager menuManager;
    private FeatureManager featureManager;
    private CharacterCreatorBranch characterCreatorBranch;

    // Colors
    private MenuColorStruct swatchSelectedColor;
    private MenuColorStruct swatchUnselectedColor;

    // Base \\

    @Override
    protected void create() {

        // Colors
        this.swatchSelectedColor = new MenuColorStruct(RuntimeSetting.CREATOR_SWATCH_SELECTED_COLOR);
        this.swatchUnselectedColor = new MenuColorStruct(RuntimeSetting.CREATOR_SWATCH_UNSELECTED_COLOR);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.featureManager = get(FeatureManager.class);
        this.characterCreatorBranch = get(CharacterCreatorBranch.class);
    }

    // Populate \\

    void populateFeatures(CreatorSessionStruct session) {

        if (!session.getPlayer().hasAppearance())
            return;

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_FEATURES);

        for (FeatureSlot featureSlot : FACE_SLOTS)
            injectFeatureRow(session, appearance, featureSlot);

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_SKIN);
        injectSwatches(session, CreatorPalette.SKIN, appearance.getSkinColor());
    }

    void populateHair(CreatorSessionStruct session) {

        if (!session.getPlayer().hasAppearance())
            return;

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_HAIRSTYLE);
        injectFeatureRow(session, appearance, FeatureSlot.HAIR);

        characterCreatorBranch.injectSectionHeader(session, RuntimeSetting.CREATOR_SECTION_HAIR_COLOR);
        injectSwatches(session, CreatorPalette.HAIR, appearance.getHairColor());
    }

    // Features \\

    private void injectFeatureRow(CreatorSessionStruct session, AppearanceHandle appearance, FeatureSlot featureSlot) {
        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_FEATURE_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(toTitle(featureSlot.name()));
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE)
                            .setFontText(toFeatureTitle(appearance.getFeature(featureSlot)));
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_PREVIOUS)
                            .setActionArgOverride(featureSlot.name());
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_NEXT)
                            .setActionArgOverride(featureSlot.name());
                });
    }

    public void previousFeature(String featureSlotName, WindowInstance window) {
        cycleFeature(window, FeatureSlot.valueOf(featureSlotName), -1);
    }

    public void nextFeature(String featureSlotName, WindowInstance window) {
        cycleFeature(window, FeatureSlot.valueOf(featureSlotName), 1);
    }

    private void cycleFeature(WindowInstance window, FeatureSlot featureSlot, int step) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null || !session.getPlayer().hasAppearance())
            return;

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();
        ObjectArrayList<FeatureHandle> options = resolveOptions(
                session.getPlayer().getEntityData().getAppearanceData(), featureSlot, !featureSlot.isRequired());
        int index = Math.floorMod(options.indexOf(appearance.getFeature(featureSlot)) + step, options.size());

        applyFeature(appearance, featureSlot, options.get(index));
        characterCreatorBranch.refreshOptions(session);
    }

    private void applyFeature(AppearanceHandle appearance, FeatureSlot featureSlot, FeatureHandle featureHandle) {

        if (featureHandle == null)
            appearance.clearFeature(featureSlot);
        else
            appearance.setFeature(featureHandle);
    }

    private ObjectArrayList<FeatureHandle> resolveOptions(
            AppearanceData appearanceData,
            FeatureSlot featureSlot,
            boolean includeNone) {

        ObjectArrayList<FeatureHandle> options = new ObjectArrayList<>();
        ObjectArrayList<FeatureHandle> slotFeatures = featureManager.getFeatureHandlesForSlot(featureSlot);

        for (int i = 0; i < slotFeatures.size(); i++)
            if (appearanceData.isCompatible(slotFeatures.get(i)))
                options.add(slotFeatures.get(i));

        options.sort((first, second) -> first.getFeatureName().compareTo(second.getFeatureName()));

        if (includeNone)
            options.add(0, null);

        return options;
    }

    // Colors \\

    private void injectSwatches(CreatorSessionStruct session, CreatorPalette palette, Color currentColor) {

        Color[] colors = resolvePalette(session.getPlayer().getEntityData().getAppearanceData(), palette);
        ElementInstance row = null;

        for (int i = 0; i < colors.length; i++) {

            if (i % RuntimeSetting.CREATOR_SWATCHES_PER_ROW == 0)
                row = menuManager.inject(
                        session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS,
                        RuntimeSetting.MENU_CREATOR_SWATCH_ROW);

            Color color = colors[i];
            String argument = palette.name() + RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR + i;

            menuManager.inject(row, RuntimeSetting.MENU_CREATOR_SWATCH, swatch -> {
                swatch.setActionArgOverride(argument);
                swatch.setColorOverride(new MenuColorStruct(color));
                swatch.findChildById(RuntimeSetting.ELEMENT_CREATOR_SWATCH_RING).setColorOverride(
                        isSameColor(color, currentColor) ? swatchSelectedColor : swatchUnselectedColor);
            });
        }
    }

    public void pickColor(String argument, WindowInstance window) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null || !session.getPlayer().hasAppearance())
            return;

        String[] parts = argument.split(RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR);
        CreatorPalette palette = CreatorPalette.valueOf(parts[0]);
        Color color = resolvePalette(session.getPlayer().getEntityData().getAppearanceData(), palette)[Integer
                .parseInt(parts[1])];

        applyColor(session.getPlayer().getAppearanceHandle(), palette, color);
        characterCreatorBranch.refreshOptions(session);
    }

    private void applyColor(AppearanceHandle appearance, CreatorPalette palette, Color color) {

        if (palette == CreatorPalette.SKIN)
            appearance.setSkinColor(color.r, color.g, color.b);
        else
            appearance.setHairColor(color.r, color.g, color.b);
    }

    private Color[] resolvePalette(AppearanceData appearanceData, CreatorPalette palette) {
        return palette == CreatorPalette.SKIN ? appearanceData.getSkinPalette() : appearanceData.getHairPalette();
    }

    private boolean isSameColor(Color first, Color second) {
        return Math.abs(first.r - second.r) <= RuntimeSetting.CREATOR_COLOR_MATCH_EPSILON
                && Math.abs(first.g - second.g) <= RuntimeSetting.CREATOR_COLOR_MATCH_EPSILON
                && Math.abs(first.b - second.b) <= RuntimeSetting.CREATOR_COLOR_MATCH_EPSILON;
    }

    // Randomize \\

    void randomizeAppearance(EntityInstance player) {

        if (!player.hasAppearance())
            return;

        AppearanceHandle appearance = player.getAppearanceHandle();
        AppearanceData appearanceData = player.getEntityData().getAppearanceData();

        for (FeatureSlot featureSlot : FeatureSlot.values()) {

            ObjectArrayList<FeatureHandle> options = resolveOptions(appearanceData, featureSlot, false);

            if (!options.isEmpty())
                appearance.setFeature(options.get(randomIndex(options.size())));
        }

        for (CreatorPalette palette : CreatorPalette.values()) {
            Color[] colors = resolvePalette(appearanceData, palette);
            applyColor(appearance, palette, colors[randomIndex(colors.length)]);
        }
    }

    private int randomIndex(int count) {
        return (int) (Math.random() * count);
    }

    // Text \\

    // A feature is titled by its file name split into words, less the trailing slot word
    private String toFeatureTitle(FeatureHandle featureHandle) {

        if (featureHandle == null)
            return RuntimeSetting.CREATOR_FEATURE_NONE;

        String featureName = featureHandle.getFeatureName();
        String fileName = featureName.substring(
                featureName.lastIndexOf(RuntimeSetting.CREATOR_FEATURE_PATH_SEPARATOR) + 1);
        String[] words = fileName.split(RuntimeSetting.CREATOR_WORD_BOUNDARY_PATTERN);
        int wordCount = words.length > 1 ? words.length - 1 : words.length;

        return String.join(RuntimeSetting.CREATOR_WORD_SEPARATOR, Arrays.copyOf(words, wordCount));
    }

    private String toTitle(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }
}
