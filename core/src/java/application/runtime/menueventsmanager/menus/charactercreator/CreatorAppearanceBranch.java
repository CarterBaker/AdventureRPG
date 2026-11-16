package application.runtime.menueventsmanager.menus.charactercreator;

import java.util.Arrays;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.appearance.AppearanceOptionKind;
import application.bootstrap.entitypipeline.appearance.AppearanceOptionStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceSectionStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceTabStruct;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlayStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedVariantStruct;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CreatorAppearanceBranch extends BranchPackage {

    /*
     * Fills the look tabs as the player's race lays them out — each section
     * under its header, listing the options it has for this character — and
     * applies their choices to the player's AppearanceHandle. Colors are
     * swatch grids painted from the race's palettes; overlays a style row
     * and how far they show; the slider groups, the build, the head and the
     * detail amounts are sliders owned by CreatorBodyBranch, as are the race
     * and gender rows. A feature is a stepper over the features
     * FeatureManager holds that fit the character, sorted by name, led by
     * None where the slot may be bare — a new feature file shows up here
     * with no code change. Only what the race offers the player is listed or
     * randomized.
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

    // Tabs \\

    // The look tabs the race lays out, or one for the body of a race with no appearance
    int getLookTabCount(EntityInstance player) {
        return player.hasAppearance() ? player.getRaceData().getAppearanceData().getCreatorTabCount() : 1;
    }

    String getLookTabTitle(EntityInstance player, int index) {
        return creatorBodyBranch.toTitle(player.hasAppearance()
                ? player.getRaceData().getAppearanceData().getCreatorTab(index).getTabName()
                : EngineSetting.APPEARANCE_CREATOR_TAB_BODY);
    }

    // Populate \\

    // Every section of the look tab on show that has something to offer the character, under its header, a section of
    // several options heading each color among them by name
    void populateLook(CreatorSessionStruct session) {

        EntityInstance player = session.getPlayer();

        if (!player.hasAppearance()) {
            creatorBodyBranch.populateBareBody(session);
            return;
        }

        AppearanceTabStruct tab = player.getRaceData().getAppearanceData().getCreatorTab(session.getLookTabIndex());

        for (int s = 0; s < tab.getSectionCount(); s++) {

            AppearanceSectionStruct section = tab.getSection(s);
            int shownCount = countShown(player, section);

            if (shownCount == 0)
                continue;

            characterCreatorBranch.injectSectionHeader(session, creatorBodyBranch.toTitle(section.getSectionName()));

            for (int o = 0; o < section.getOptionCount(); o++)
                if (isShown(player, section.getOption(o)))
                    injectOption(session, section.getOption(o), shownCount > 1);
        }
    }

    private int countShown(EntityInstance player, AppearanceSectionStruct section) {

        int count = 0;

        for (int o = 0; o < section.getOptionCount(); o++)
            if (isShown(player, section.getOption(o)))
                count++;

        return count;
    }

    // Whether an option has anything to offer this character: a member the race offers the player (an overlay only
    // where the gender's model places it), a slider group with a slider the player is offered, a race among several
    private boolean isShown(EntityInstance player, AppearanceOptionStruct option) {

        AppearanceData appearanceData = player.getRaceData().getAppearanceData();

        if (!appearanceData.isOffered(option.getKind(), option.getIndex()))
            return false;

        return switch (option.getKind()) {
            case RACE -> creatorBodyBranch.hasRaceChoice();
            case GENDER, BUILD, HEAD, COLOR, DETAIL, FEATURE -> true;
            case OVERLAY -> player.getRaceGender().getOverlay(SkinnedOverlay.VALUES[option.getIndex()]) != null;
            case SLIDERS -> creatorBodyBranch.hasShapeGroup(appearanceData, option.getGroupName());
        };
    }

    private void injectOption(CreatorSessionStruct session, AppearanceOptionStruct option, boolean labelled) {
        switch (option.getKind()) {
            case RACE -> creatorBodyBranch.injectRaceRow(session);
            case GENDER -> creatorBodyBranch.injectGenderRow(session);
            case BUILD -> creatorBodyBranch.injectBuild(session);
            case HEAD -> creatorBodyBranch.injectHeadShape(session);
            case COLOR -> injectColor(session, SkinnedColor.VALUES[option.getIndex()], labelled);
            case DETAIL -> creatorBodyBranch.injectSlider(session, CreatorSlider.DETAIL, option.getIndex());
            case OVERLAY -> injectOverlay(session, SkinnedOverlay.VALUES[option.getIndex()]);
            case FEATURE -> injectFeature(session, FeatureSlot.VALUES[option.getIndex()]);
            case SLIDERS -> creatorBodyBranch.injectShapeGroup(session, option.getGroupName());
        }
    }

    // A color's swatches from the race's palette, then its hue, saturation and brightness for any color between
    private void injectColor(CreatorSessionStruct session, SkinnedColor skinnedColor, boolean labelled) {

        if (labelled)
            characterCreatorBranch.injectSectionHeader(
                    session, creatorBodyBranch.toTitle(ArpgUtility.toEnumName(skinnedColor)));

        injectSwatches(session, skinnedColor, session.getPlayer().getAppearanceHandle().getColor(skinnedColor));

        for (CreatorSlider slider : RuntimeSetting.CREATOR_COLOR_SLIDERS)
            creatorBodyBranch.injectSlider(session, slider, skinnedColor.ordinal());
    }

    // Overlays \\

    // An overlay the worn gender's model places: the style it is painted in, then how far it shows
    private void injectOverlay(CreatorSessionStruct session, SkinnedOverlay skinnedOverlay) {
        injectStyleRow(session, skinnedOverlay, session.getPlayer().getRaceGender().getOverlay(skinnedOverlay));
        creatorBodyBranch.injectSlider(session, CreatorSlider.DETAIL, skinnedOverlay.getDetail().ordinal());
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

    // Features \\

    // A feature slot's row, then the variant the feature worn there shows when it offers several
    private void injectFeature(CreatorSessionStruct session, FeatureSlot featureSlot) {

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();

        injectFeatureRow(session, appearance, featureSlot);

        if (appearance.hasFeature(featureSlot) && appearance.getFeature(featureSlot).hasVariants())
            injectVariantRow(session, appearance, featureSlot);
    }

    // The variant a worn feature shows, stepped through every variant it offers
    private void injectVariantRow(CreatorSessionStruct session, AppearanceHandle appearance, FeatureSlot featureSlot) {

        SkinnedVariantStruct variants = appearance.getFeature(featureSlot).getVariants();
        String variantName = variants.getName(variants.resolve(appearance.getFeatureVariant(featureSlot)));

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_VARIANT_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(toTitle(featureSlot.name()) + RuntimeSetting.CREATOR_VARIANT_SUFFIX);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE)
                            .setFontText(variantName);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_PREVIOUS)
                            .setActionArgOverride(featureSlot.name());
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_NEXT)
                            .setActionArgOverride(featureSlot.name());
                });
    }

    public void previousVariant(String slotName, WindowInstance window) {
        cycleVariant(window, FeatureSlot.valueOf(slotName), -1);
    }

    public void nextVariant(String slotName, WindowInstance window) {
        cycleVariant(window, FeatureSlot.valueOf(slotName), 1);
    }

    private void cycleVariant(WindowInstance window, FeatureSlot featureSlot, int step) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null || !session.getPlayer().hasAppearance())
            return;

        AppearanceHandle appearance = session.getPlayer().getAppearanceHandle();

        if (!appearance.hasFeature(featureSlot) || !appearance.getFeature(featureSlot).hasVariants())
            return;

        SkinnedVariantStruct variants = appearance.getFeature(featureSlot).getVariants();

        appearance.setFeatureVariant(
                featureSlot,
                Math.floorMod(variants.resolve(appearance.getFeatureVariant(featureSlot)) + step, variants.getCount()));
        characterCreatorBranch.refreshOptions(session);
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
                session.getPlayer().getRaceData().getAppearanceData(), featureSlot,
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

        Color[] colors = session.getPlayer().getRaceData().getAppearanceData().getPalette(skinnedColor);
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
        Color color = session.getPlayer().getRaceData().getAppearanceData()
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

    // Every color, overlay style, feature and variant the race offers the player, each picked at random
    void randomizeAppearance(EntityInstance player) {

        if (!player.hasAppearance())
            return;

        AppearanceHandle appearance = player.getAppearanceHandle();
        AppearanceData appearanceData = player.getRaceData().getAppearanceData();

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            ObjectArrayList<FeatureHandle> options = resolveOptions(appearanceData, featureSlot, false);

            if (appearanceData.isOffered(AppearanceOptionKind.FEATURE, featureSlot.ordinal()) && !options.isEmpty())
                appearance.setFeature(options.get(randomIndex(options.size())));
        }

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES) {

            if (!appearanceData.isOffered(AppearanceOptionKind.COLOR, skinnedColor.ordinal()))
                continue;

            Color[] colors = appearanceData.getPalette(skinnedColor);
            Color color = colors[randomIndex(colors.length)];
            appearance.setColor(skinnedColor, color.r, color.g, color.b);
        }

        for (SkinnedOverlay skinnedOverlay : SkinnedOverlay.VALUES) {

            SkinnedOverlayStruct overlay = player.getRaceGender().getOverlay(skinnedOverlay);

            if (overlay != null && appearanceData.isOffered(AppearanceOptionKind.OVERLAY, skinnedOverlay.ordinal()))
                appearance.setOverlayStyle(skinnedOverlay, randomIndex(overlay.getStyleCount()));
        }

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (appearanceData.isOffered(AppearanceOptionKind.FEATURE, featureSlot.ordinal())
                    && appearance.hasFeature(featureSlot)
                    && appearance.getFeature(featureSlot).hasVariants())
                appearance.setFeatureVariant(
                        featureSlot,
                        randomIndex(appearance.getFeature(featureSlot).getVariants().getCount()));
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
