package application.runtime.menueventsmanager.menus.charactercreator;

import java.util.Arrays;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlayStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CreatorAppearanceBranch extends BranchPackage {

    /*
     * Fills the Appearance and Hair tabs and applies their choices to the
     * player's AppearanceHandle. Colors are swatch grids painted from the
     * character's palettes; the face's shape sliders, grouped as the
     * template groups them, and the detail amounts are sliders owned by
     * CreatorBodyBranch. The hairstyle is a stepper over the features
     * FeatureManager holds that fit the character, sorted by name, led by
     * None — a new feature file shows up here with no code change.
     */

    // Internal
    private MenuManager menuManager;
    private FeatureManager featureManager;
    private CharacterCreatorBranch characterCreatorBranch;
    private CreatorBodyBranch creatorBodyBranch;

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
        this.creatorBodyBranch = get(CreatorBodyBranch.class);
    }

    // Populate \\

    void populateAppearance(CreatorSessionStruct session) {

        if (!session.getPlayer().hasAppearance())
            return;

        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_SKIN, SkinnedColor.SKIN);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_EYE_COLOR, SkinnedColor.EYES);
        injectFeatureSection(
                session, RuntimeSetting.CREATOR_SECTION_FACE_FEATURES, RuntimeSetting.CREATOR_FACE_FEATURES);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_TEETH_COLOR, SkinnedColor.TEETH);
        creatorBodyBranch.injectShapeGroups(session, false);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_LIP_COLOR, SkinnedColor.LIPS);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_BLUSH_COLOR, SkinnedColor.BLUSH);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_FRECKLE_COLOR, SkinnedColor.FRECKLES);
        injectDetailSection(session, RuntimeSetting.CREATOR_SECTION_DETAILS, RuntimeSetting.CREATOR_APPEARANCE_DETAILS);
    }

    void populateHair(CreatorSessionStruct session) {

        if (!session.getPlayer().hasAppearance())
            return;

        injectFeatureSection(session, RuntimeSetting.CREATOR_SECTION_HAIRSTYLE, RuntimeSetting.CREATOR_HAIR_FEATURES);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_HAIR_COLOR, SkinnedColor.HAIR);
        injectDetailSection(session, RuntimeSetting.CREATOR_SECTION_HAIR_DETAILS, RuntimeSetting.CREATOR_HAIR_DETAILS);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_BROW_COLOR, SkinnedColor.BROWS);
        injectOverlaySection(session, RuntimeSetting.CREATOR_SECTION_BROWS, RuntimeSetting.CREATOR_BROW_OVERLAYS);
    }

    // The body's own colors and the marks painted over its skin, each in the style chosen and as far as it shows,
    // on the Body tab
    void populateBodyMarks(CreatorSessionStruct session) {

        if (!session.getPlayer().hasAppearance())
            return;

        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_NIPPLE_COLOR, SkinnedColor.NIPPLES);
        injectOverlaySection(session, RuntimeSetting.CREATOR_SECTION_NIPPLES, RuntimeSetting.CREATOR_NIPPLE_OVERLAYS);
        injectColorSection(session, RuntimeSetting.CREATOR_SECTION_BODY_HAIR_COLOR, SkinnedColor.BODY_HAIR);
        injectOverlaySection(
                session, RuntimeSetting.CREATOR_SECTION_BODY_HAIR, RuntimeSetting.CREATOR_BODY_HAIR_OVERLAYS);
    }

    // A color's swatches from the race's palette, then its hue, saturation and brightness for any color between
    private void injectColorSection(CreatorSessionStruct session, String title, SkinnedColor skinnedColor) {

        characterCreatorBranch.injectSectionHeader(session, title);
        injectSwatches(session, skinnedColor, session.getPlayer().getAppearanceHandle().getColor(skinnedColor));

        for (CreatorSlider slider : RuntimeSetting.CREATOR_COLOR_SLIDERS)
            creatorBodyBranch.injectSlider(session, slider, skinnedColor.ordinal());
    }

    // Overlays \\

    // Each overlay the worn gender's model places: the style it is painted in, then how far it shows
    private void injectOverlaySection(CreatorSessionStruct session, String title, SkinnedOverlay[] overlays) {

        EntityInstance player = session.getPlayer();
        boolean headed = false;

        for (SkinnedOverlay skinnedOverlay : overlays) {

            SkinnedOverlayStruct overlay = player.getRaceGender().getOverlay(skinnedOverlay);

            if (overlay == null)
                continue;

            if (!headed) {
                characterCreatorBranch.injectSectionHeader(session, title);
                headed = true;
            }

            injectStyleRow(session, skinnedOverlay, overlay);
            creatorBodyBranch.injectSlider(session, CreatorSlider.DETAIL, skinnedOverlay.getDetail().ordinal());
        }
    }

    private void injectStyleRow(
            CreatorSessionStruct session,
            SkinnedOverlay skinnedOverlay,
            SkinnedOverlayStruct overlay) {

        String styleName = overlay.getStyleName(resolveStyle(session.getPlayer(), skinnedOverlay, overlay));

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_STYLE_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(creatorBodyBranch.toTitle(ArpgUtility.toEnumName(skinnedOverlay)));
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE)
                            .setFontText(styleName);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_PREVIOUS)
                            .setActionArgOverride(skinnedOverlay.name());
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_NEXT)
                            .setActionArgOverride(skinnedOverlay.name());
                });
    }

    public void previousStyle(String overlayName, WindowInstance window) {
        cycleStyle(window, SkinnedOverlay.valueOf(overlayName), -1);
    }

    public void nextStyle(String overlayName, WindowInstance window) {
        cycleStyle(window, SkinnedOverlay.valueOf(overlayName), 1);
    }

    private void cycleStyle(WindowInstance window, SkinnedOverlay skinnedOverlay, int step) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null || !session.getPlayer().hasAppearance())
            return;

        EntityInstance player = session.getPlayer();
        SkinnedOverlayStruct overlay = player.getRaceGender().getOverlay(skinnedOverlay);

        if (overlay == null)
            return;

        player.getAppearanceHandle().setOverlayStyle(
                skinnedOverlay,
                Math.floorMod(resolveStyle(player, skinnedOverlay, overlay) + step, overlay.getStyleCount()));
        characterCreatorBranch.refreshOptions(session);
    }

    // The style an overlay is painted in, its model's default where none was chosen
    private int resolveStyle(EntityInstance player, SkinnedOverlay skinnedOverlay, SkinnedOverlayStruct overlay) {

        int styleIndex = player.getAppearanceHandle().getOverlayStyle(skinnedOverlay);

        return styleIndex >= 0 && styleIndex < overlay.getStyleCount() ? styleIndex : overlay.getDefaultStyle();
    }

    private void injectDetailSection(CreatorSessionStruct session, String title, int[] details) {

        characterCreatorBranch.injectSectionHeader(session, title);

        for (int detail : details)
            creatorBodyBranch.injectSlider(session, CreatorSlider.DETAIL, detail);
    }

    // Features \\

    private void injectFeatureSection(CreatorSessionStruct session, String title, FeatureSlot[] featureSlots) {

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();

        characterCreatorBranch.injectSectionHeader(session, title);

        for (FeatureSlot featureSlot : featureSlots)
            injectFeatureRow(session, appearance, featureSlot);
    }

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
                session.getPlayer().getEntityData().getRaceData().getAppearanceData(), featureSlot,
                featureSlot.isRemovable());
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

    private void injectSwatches(CreatorSessionStruct session, SkinnedColor skinnedColor, Color currentColor) {

        Color[] colors = session.getPlayer().getEntityData().getRaceData().getAppearanceData().getPalette(skinnedColor);
        ElementInstance row = null;

        for (int i = 0; i < colors.length; i++) {

            if (i % RuntimeSetting.CREATOR_SWATCHES_PER_ROW == 0)
                row = menuManager.inject(
                        session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS,
                        RuntimeSetting.MENU_CREATOR_SWATCH_ROW);

            Color color = colors[i];
            String argument = skinnedColor.name() + RuntimeSetting.CREATOR_ARGUMENT_SEPARATOR + i;

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
        SkinnedColor skinnedColor = SkinnedColor.valueOf(parts[0]);
        Color color = session.getPlayer().getEntityData().getRaceData().getAppearanceData()
                .getPalette(skinnedColor)[Integer.parseInt(parts[1])];

        session.getPlayer().getAppearanceHandle().setColor(skinnedColor, color.r, color.g, color.b);
        characterCreatorBranch.refreshOptions(session);
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
        AppearanceData appearanceData = player.getEntityData().getRaceData().getAppearanceData();

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            ObjectArrayList<FeatureHandle> options = resolveOptions(appearanceData, featureSlot, false);

            if (!options.isEmpty())
                appearance.setFeature(options.get(randomIndex(options.size())));
        }

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES) {
            Color[] colors = appearanceData.getPalette(skinnedColor);
            Color color = colors[randomIndex(colors.length)];
            appearance.setColor(skinnedColor, color.r, color.g, color.b);
        }

        for (SkinnedOverlay skinnedOverlay : SkinnedOverlay.VALUES) {

            SkinnedOverlayStruct overlay = player.getRaceGender().getOverlay(skinnedOverlay);

            if (overlay != null)
                appearance.setOverlayStyle(skinnedOverlay, randomIndex(overlay.getStyleCount()));
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
