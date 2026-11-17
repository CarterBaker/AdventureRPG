package application.runtime.menueventsmanager.menus.charactercreator;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceHandle;
import application.bootstrap.entitypipeline.entity.EntityData;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.entitypipeline.race.RaceData;
import application.bootstrap.entitypipeline.race.RaceHandle;
import application.bootstrap.entitypipeline.racemanager.RaceManager;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.progressionpipeline.gendermanager.GenderManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CreatorBodyBranch extends BranchPackage {

    /*
     * Owns the race and gender rows and every slider the creator shows — the
     * build, the head's shape, each slider group and the detail amounts,
     * injected wherever the race's layout lists them — applying each to the
     * player. The race steps through the playable races (listed only when
     * there are several), the gender through the genders the race allows.
     * Height spans the range the entity template allows scaled to the race,
     * weight the template's range, the head sliders the creator's head
     * proportion range, a shape slider its own range and a detail its
     * creator range. A slider shows its value beside its name and moves its
     * knob with the cursor while dragged.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private CharacterCreatorBranch characterCreatorBranch;
    private GenderManager genderManager;
    private RaceManager raceManager;
    private PlayerManager playerManager;

    // Scratch
    private ObjectArrayList<RaceHandle> playableRaces;

    // Base \\

    @Override
    protected void create() {
        this.playableRaces = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.characterCreatorBranch = get(CharacterCreatorBranch.class);
        this.genderManager = get(GenderManager.class);
        this.raceManager = get(RaceManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    // Populate \\

    // The look of a race with no appearance: the race among the playable, the gender and the build
    void populateBareBody(CreatorSessionStruct session) {

        if (hasRaceChoice()) {
            characterCreatorBranch.injectSectionHeader(session, EngineSetting.APPEARANCE_CREATOR_SECTION_RACE);
            injectRaceRow(session);
        }

        characterCreatorBranch.injectSectionHeader(session, EngineSetting.APPEARANCE_CREATOR_SECTION_GENDER);
        injectGenderRow(session);
        characterCreatorBranch.injectSectionHeader(session, EngineSetting.APPEARANCE_CREATOR_SECTION_BUILD);
        injectBuild(session);
    }

    void injectBuild(CreatorSessionStruct session) {
        injectSlider(session, CreatorSlider.HEIGHT, 0);
        injectSlider(session, CreatorSlider.WEIGHT, 0);
    }

    void injectHeadShape(CreatorSessionStruct session) {
        injectSlider(session, CreatorSlider.HEAD_WIDTH, 0);
        injectSlider(session, CreatorSlider.HEAD_LENGTH, 0);
        injectSlider(session, CreatorSlider.HEAD_DEPTH, 0);
    }

    // Whether any slider of a group is offered to the player
    boolean hasShapeGroup(AppearanceData appearanceData, String groupName) {

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            if (isInGroup(appearanceData, i, groupName))
                return true;

        return false;
    }

    // Every slider of a group offered to the player, in the order the race lists them
    void injectShapeGroup(CreatorSessionStruct session, String groupName) {

        AppearanceData appearanceData = session.getPlayer().getRaceData().getAppearanceData();

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            if (isInGroup(appearanceData, i, groupName))
                injectSlider(session, CreatorSlider.SHAPE, i);
    }

    private boolean isInGroup(AppearanceData appearanceData, int shapeIndex, String groupName) {
        return appearanceData.getShape(shapeIndex).isShownToPlayer()
                && appearanceData.getShape(shapeIndex).getGroupName().equals(groupName);
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

    // Race \\

    // Whether the player may choose among more than one race
    boolean hasRaceChoice() {

        raceManager.collectPlayableRaces(playableRaces);
        return playableRaces.size() > 1;
    }

    void injectRaceRow(CreatorSessionStruct session) {

        String raceTitle = toRaceTitle(session.getPlayer().getRaceData().getRaceName());

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_RACE_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(EngineSetting.APPEARANCE_CREATOR_SECTION_RACE);
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_VALUE).setFontText(raceTitle);
                });
    }

    public void previousRace(WindowInstance window) {
        cycleRace(window, -1);
    }

    public void nextRace(WindowInstance window) {
        cycleRace(window, 1);
    }

    // The player made as the next playable race, in its default gender and look, the tabs listed again for it
    private void cycleRace(WindowInstance window, int step) {

        CreatorSessionStruct session = characterCreatorBranch.getSession(window);

        if (session == null)
            return;

        raceManager.collectPlayableRaces(playableRaces);

        if (playableRaces.isEmpty())
            return;

        EntityInstance player = session.getPlayer();
        int index = 0;

        for (int i = 0; i < playableRaces.size(); i++)
            if (playableRaces.get(i).getRaceData() == player.getRaceData())
                index = i;

        player.setRaceData(playableRaces.get(Math.floorMod(index + step, playableRaces.size())).getRaceData());
        characterCreatorBranch.refreshTabs(session);
    }

    // A race is titled by its file name split into words
    private String toRaceTitle(String raceName) {

        String fileName = raceName.substring(raceName.lastIndexOf(RuntimeSetting.CREATOR_FEATURE_PATH_SEPARATOR) + 1);

        return String.join(
                RuntimeSetting.CREATOR_WORD_SEPARATOR,
                fileName.split(RuntimeSetting.CREATOR_WORD_BOUNDARY_PATTERN));
    }

    // Gender \\

    void injectGenderRow(CreatorSessionStruct session) {

        String genderName = genderManager.getGenderHandleFromGenderID(session.getPlayer().getGenderID())
                .getDisplayName();

        menuManager.inject(
                session.getCreatorMenu(), RuntimeSetting.ENTRY_CREATOR_OPTIONS, RuntimeSetting.MENU_CREATOR_GENDER_ROW,
                row -> {
                    row.findChildById(RuntimeSetting.ELEMENT_CREATOR_ROW_LABEL)
                            .setFontText(EngineSetting.APPEARANCE_CREATOR_SECTION_GENDER);
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
        RaceData raceData = player.getRaceData();
        int index = 0;

        for (int i = 0; i < raceData.getGenderCount(); i++)
            if (raceData.getGenderID(i) == player.getGenderID())
                index = i;

        player.setGenderID(raceData.getGenderID(Math.floorMod(index + step, raceData.getGenderCount())));
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
        playerManager.focusCharacterPreview(window.getWindowID(), resolveFocus(slider, index, session.getPlayer()));
    }

    // The bones a slider moves, which the preview closes in on while it is worked: a shape slider's own and those of
    // the sliders it drives, the head for its proportions, none (the whole body) for the rest
    private int[] resolveFocus(CreatorSlider slider, int index, EntityInstance player) {

        if (!player.hasAppearance())
            return null;

        AppearanceData appearanceData = player.getRaceData().getAppearanceData();

        return switch (slider) {
            case SHAPE -> appearanceData.getShapeFocusBones(index);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> appearanceData.getHeadFocusBones();
            default -> null;
        };
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
            case COLOR_HUE, COLOR_SATURATION, COLOR_BRIGHTNESS -> applyColor(slider, player, index, value);
        }
    }

    // One of a color's hue, saturation and brightness moved, the other two held
    private void applyColor(CreatorSlider slider, EntityInstance player, int index, float value) {

        Color color = player.getAppearanceHandle().getColor(SkinnedColor.VALUES[index]);

        color.setHSB(
                slider == CreatorSlider.COLOR_HUE ? value : color.getHue(),
                slider == CreatorSlider.COLOR_SATURATION ? value : color.getSaturation(),
                slider == CreatorSlider.COLOR_BRIGHTNESS ? value : color.getBrightness());
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
            case COLOR_HUE -> player.getAppearanceHandle().getColor(SkinnedColor.VALUES[index]).getHue();
            case COLOR_SATURATION -> player.getAppearanceHandle().getColor(SkinnedColor.VALUES[index]).getSaturation();
            case COLOR_BRIGHTNESS -> player.getAppearanceHandle().getColor(SkinnedColor.VALUES[index]).getBrightness();
        };
    }

    private float resolveMin(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getEntityData().getSizeMin().y * player.getRaceScale();
            case WEIGHT -> player.getEntityData().getWeightMin();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN;
            case SHAPE -> player.getRaceData().getAppearanceData()
                    .getShapeMin(player.getGenderID(), index);
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_MIN[index];
            case COLOR_HUE, COLOR_SATURATION, COLOR_BRIGHTNESS -> 0f;
        };
    }

    private float resolveMax(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case HEIGHT -> player.getEntityData().getSizeMax().y * player.getRaceScale();
            case WEIGHT -> player.getEntityData().getWeightMax();
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH -> RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX;
            case SHAPE -> player.getRaceData().getAppearanceData()
                    .getShapeMax(player.getGenderID(), index);
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_MAX[index];
            case COLOR_HUE, COLOR_SATURATION, COLOR_BRIGHTNESS -> 1f;
        };
    }

    private String formatValue(CreatorSlider slider, int index, EntityInstance player) {

        float value = resolveValue(slider, index, player);

        return switch (slider) {
            case HEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_HEIGHT, value);
            case WEIGHT -> String.format(RuntimeSetting.CREATOR_FORMAT_WEIGHT, value);
            case COLOR_HUE -> String.format(
                    RuntimeSetting.CREATOR_FORMAT_DEGREES, value * RuntimeSetting.CREATOR_HUE_DEGREES);
            case HEAD_WIDTH, HEAD_LENGTH, HEAD_DEPTH, SHAPE, DETAIL, COLOR_SATURATION, COLOR_BRIGHTNESS ->
                    String.format(RuntimeSetting.CREATOR_FORMAT_PERCENT, value * RuntimeSetting.CREATOR_PERCENT_SCALE);
        };
    }

    // Text \\

    private String resolveLabel(CreatorSlider slider, int index, EntityInstance player) {
        return switch (slider) {
            case SHAPE -> toTitle(
                    player.getRaceData().getAppearanceData().getShape(index).getShapeName());
            case DETAIL -> RuntimeSetting.CREATOR_DETAIL_LABELS[index];
            default -> slider.getLabel();
        };
    }

    // A lower-case name split at its separators into capitalised words
    String toTitle(String name) {

        String[] words = name.split(RuntimeSetting.CREATOR_SHAPE_NAME_SEPARATOR);

        for (int i = 0; i < words.length; i++)
            if (!words[i].isEmpty())
                words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);

        return String.join(RuntimeSetting.CREATOR_WORD_SEPARATOR, words);
    }

    // Randomize \\

    // The gender, size and weight, the head's proportions and every slider offered to the player, each at random
    // within its range, the size as the template allows it scaled to the race
    void randomizeBody(EntityInstance player) {

        EntityData entityData = player.getEntityData();
        RaceData raceData = player.getRaceData();

        player.setGenderID(raceData.getGenderID((int) (Math.random() * raceData.getGenderCount())));
        player.setSize(entityData.getRandomSize().multiply(player.getRaceScale()));
        player.setWeight(entityData.getRandomWeight());

        if (!player.hasAppearance())
            return;

        player.getAppearanceHandle().setHeadProportion(
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX),
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX),
                randomBetween(RuntimeSetting.CREATOR_HEAD_PROPORTION_MIN, RuntimeSetting.CREATOR_HEAD_PROPORTION_MAX));

        AppearanceData appearanceData = raceData.getAppearanceData();

        for (int i = 0; i < appearanceData.getShapeCount(); i++)
            if (appearanceData.getShape(i).isShownToPlayer())
                player.getAppearanceHandle().setShapeValue(
                        i,
                        randomBetween(
                                appearanceData.getShapeMin(player.getGenderID(), i),
                                appearanceData.getShapeMax(player.getGenderID(), i)));
    }

    private float randomBetween(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }
}
