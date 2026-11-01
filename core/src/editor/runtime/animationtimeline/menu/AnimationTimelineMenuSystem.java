package editor.runtime.animationtimeline.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.runtime.animationtimeline.AnimationTimelineSetting;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class AnimationTimelineMenuSystem extends SystemPackage {

    /*
     * Opens the Animation Timeline menu: the transport, the playhead's time
     * against the clip's length, a summary track marking every key — the
     * selected bone's bright, every other bone's dim — under the playhead,
     * and below it a dope sheet of one lane per bone of the clip's rig, each
     * marking that bone's keys, the selected bone's lane highlighted. A
     * lane's name picks its bone and its track picks it and moves the
     * playhead there. The playhead follows the playhead every frame, set from
     * positions built once, so playback never allocates; everything else is
     * rebuilt only when the editor's revision moves.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private AnimationEditorManager animationEditorManager;

    // Menus
    private MenuInstance timelineMenu;

    // Track
    private DimensionVector2Struct[] trackPositions;
    private FloatArrayList selectedKeyTimes;
    private FloatArrayList otherKeyTimes;
    private FloatArrayList laneKeyTimes;
    private int shownPlayheadStep;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Track
        this.trackPositions = new DimensionVector2Struct[AnimationTimelineSetting.TRACK_STEPS + 1];

        for (int i = 0; i < trackPositions.length; i++)
            trackPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(
                            AnimationTimelineSetting.TRACK_PERCENT * i / AnimationTimelineSetting.TRACK_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));

        this.selectedKeyTimes = new FloatArrayList();
        this.otherKeyTimes = new FloatArrayList();
        this.laneKeyTimes = new FloatArrayList();
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.timelineMenu = menuManager.openMenu(AnimationTimelineSetting.MENU_TIMELINE, window);
    }

    // Update \\

    @Override
    protected void update() {

        movePlayhead();

        if (animationEditorManager.getRevision() == shownRevision)
            return;

        this.shownRevision = animationEditorManager.getRevision();

        refreshTime();
        refreshSummary();
        refreshLanes();
    }

    // Playhead \\

    private void movePlayhead() {

        ElementInstance playhead = timelineMenu.getEntryPoint(AnimationTimelineSetting.ENTRY_PLAYHEAD);
        int step = toTrackStep(animationEditorManager.getTime());

        if (playhead == null || step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        playhead.setPositionOverride(trackPositions[step]);
    }

    private void refreshTime() {

        ElementInstance label = timelineMenu.getEntryPoint(AnimationTimelineSetting.ENTRY_TIME);

        if (label == null)
            return;

        label.setFontText(animationEditorManager.hasActiveDocument()
                ? CharacterViewUtility.formatNumber(animationEditorManager.getTime())
                        + EditorSetting.ANIMATION_EDITOR_TIME_SEPARATOR
                        + CharacterViewUtility.formatNumber(animationEditorManager.getActiveDocument().getDuration())
                        + EditorSetting.ANIMATION_EDITOR_TIME_SUFFIX
                : EditorSetting.ANIMATION_EDITOR_STATUS_NO_CLIP);
    }

    // Summary \\

    private void refreshSummary() {

        menuManager.ejectAll(timelineMenu, AnimationTimelineSetting.ENTRY_KEYS);
        animationEditorManager.collectKeyTimes(selectedKeyTimes, otherKeyTimes);

        ElementInstance keys = timelineMenu.getEntryPoint(AnimationTimelineSetting.ENTRY_KEYS);

        if (keys == null)
            return;

        injectMarkers(keys, otherKeyTimes, AnimationTimelineSetting.TEMPLATE_OTHER_KEY_MARKER);
        injectMarkers(keys, selectedKeyTimes, AnimationTimelineSetting.TEMPLATE_KEY_MARKER);
    }

    // Lanes \\

    // One lane per bone of the clip's rig, each carrying its bone's key and marking its keys
    private void refreshLanes() {

        menuManager.ejectAll(timelineMenu, AnimationTimelineSetting.ENTRY_LANES);

        int selected = animationEditorManager.getSelectedBoneIndex();

        for (int i = 0; i < animationEditorManager.getBoneCount(); i++)
            injectLane(i, i == selected);
    }

    private void injectLane(int boneIndex, boolean selected) {

        String key = CharacterViewUtility.toKey(EditorSetting.ANIMATION_EDITOR_KEY_BONE, boneIndex);
        ElementInstance lane = menuManager.inject(
                timelineMenu,
                AnimationTimelineSetting.ENTRY_LANES,
                selected ? AnimationTimelineSetting.TEMPLATE_LANE_SELECTED : AnimationTimelineSetting.TEMPLATE_LANE,
                element -> {
                    setChildArg(element, AnimationTimelineSetting.ELEMENT_LANE_NAME, key);
                    setChildArg(element, AnimationTimelineSetting.ELEMENT_LANE_TRACK, key);
                    setChildText(
                            element,
                            AnimationTimelineSetting.ELEMENT_LANE_LABEL,
                            animationEditorManager.getBoneName(boneIndex));
                });
        ElementInstance keys = lane.findChildById(AnimationTimelineSetting.ELEMENT_LANE_KEYS);

        if (keys == null)
            return;

        animationEditorManager.collectBoneKeyTimes(boneIndex, laneKeyTimes);
        injectMarkers(
                keys,
                laneKeyTimes,
                selected
                        ? AnimationTimelineSetting.TEMPLATE_KEY_MARKER
                        : AnimationTimelineSetting.TEMPLATE_OTHER_KEY_MARKER);
    }

    // Markers \\

    private void injectMarkers(ElementInstance container, FloatArrayList times, String template) {

        for (int i = 0; i < times.size(); i++) {

            DimensionVector2Struct position = trackPositions[toTrackStep(times.getFloat(i))];

            menuManager.inject(container, template, marker -> marker.setPositionOverride(position));
        }
    }

    private int toTrackStep(float time) {

        float fraction = time / animationEditorManager.getTimelineSpan();
        int step = Math.round(fraction * AnimationTimelineSetting.TRACK_STEPS);

        return Math.max(0, Math.min(AnimationTimelineSetting.TRACK_STEPS, step));
    }

    // Utility \\

    private void setChildArg(ElementInstance lane, String childID, String key) {

        ElementInstance child = lane.findChildById(childID);

        if (child != null)
            child.setActionArgOverride(key);
    }

    private void setChildText(ElementInstance lane, String childID, String text) {

        ElementInstance child = lane.findChildById(childID);

        if (child != null)
            child.setFontText(text);
    }
}
