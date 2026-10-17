package editor.animationeditor.menu;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.animationeditor.AnimationEditorSetting;
import editor.bootstrap.animationeditorpipeline.animationeditormanager.AnimationEditorManager;
import editor.characterview.panel.CharacterViewPanelSystem;
import editor.characterview.panel.CharacterViewRowStruct;
import editor.characterview.panel.CharacterViewTemplateStruct;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationEditorMenuSystem extends SystemPackage {

    /*
     * Opens the Animation Editor toolbar, its two panels, its timeline and its
     * status bar over the viewport. When the editor's revision moves it
     * rewrites the status line, refills both panels and marks every key on
     * the timeline — the selected bone's bright, every other bone's dim. The
     * playhead follows the playhead every frame, set from positions built
     * once, so playback never allocates.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private AnimationEditorManager animationEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;

    // Menus
    private MenuInstance toolbarMenu;
    private CharacterViewTemplateStruct templates;

    // Rows
    private ObjectArrayList<CharacterViewRowStruct> rows;

    // Timeline
    private DimensionVector2Struct[] timelinePositions;
    private FloatArrayList selectedKeyTimes;
    private FloatArrayList otherKeyTimes;
    private int shownPlayheadStep;

    // Status
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        // Menus
        this.templates = new CharacterViewTemplateStruct(
                AnimationEditorSetting.TEMPLATE_HEADER_ROW,
                AnimationEditorSetting.TEMPLATE_ITEM_ROW,
                AnimationEditorSetting.TEMPLATE_ITEM_ROW_SELECTED,
                AnimationEditorSetting.TEMPLATE_NUMBER_ROW,
                AnimationEditorSetting.TEMPLATE_CHOICE_ROW,
                AnimationEditorSetting.TEMPLATE_ACTION_ROW,
                AnimationEditorSetting.ELEMENT_ROW_LABEL,
                AnimationEditorSetting.ELEMENT_ROW_VALUE,
                AnimationEditorSetting.ELEMENT_ROW_VALUE_BUTTON,
                AnimationEditorSetting.ELEMENT_ROW_DECREASE,
                AnimationEditorSetting.ELEMENT_ROW_INCREASE);

        // Rows
        this.rows = new ObjectArrayList<>();

        // Timeline
        this.timelinePositions = new DimensionVector2Struct[AnimationEditorSetting.TIMELINE_STEPS + 1];

        for (int i = 0; i < timelinePositions.length; i++)
            timelinePositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(
                            AnimationEditorSetting.TIMELINE_PERCENT * i / AnimationEditorSetting.TIMELINE_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));

        this.selectedKeyTimes = new FloatArrayList();
        this.otherKeyTimes = new FloatArrayList();
        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        // Status
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.characterViewPanelSystem = get(CharacterViewPanelSystem.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.toolbarMenu = menuManager.openMenu(AnimationEditorSetting.MENU_TOOLBAR, window);
    }

    // Update \\

    @Override
    protected void update() {

        movePlayhead();

        if (animationEditorManager.getRevision() == shownRevision)
            return;

        shownRevision = animationEditorManager.getRevision();

        ElementInstance statusLabel = toolbarMenu.getEntryPoint(AnimationEditorSetting.ENTRY_STATUS);

        if (statusLabel != null)
            statusLabel.setFontText(animationEditorManager.getStatusText());

        rows.clear();
        animationEditorManager.buildListRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, AnimationEditorSetting.ENTRY_LIST, rows, templates);

        rows.clear();
        animationEditorManager.buildPropertyRows(rows);
        characterViewPanelSystem.fill(toolbarMenu, AnimationEditorSetting.ENTRY_PROPERTIES, rows, templates);

        refreshKeyMarkers();
    }

    // Timeline \\

    private void movePlayhead() {

        ElementInstance playhead = toolbarMenu.getEntryPoint(AnimationEditorSetting.ENTRY_PLAYHEAD);
        int step = toTimelineStep(animationEditorManager.getTime());

        if (playhead == null || step == shownPlayheadStep)
            return;

        shownPlayheadStep = step;
        playhead.setPositionOverride(timelinePositions[step]);
    }

    private void refreshKeyMarkers() {

        menuManager.ejectAll(toolbarMenu, AnimationEditorSetting.ENTRY_KEYS);
        animationEditorManager.collectKeyTimes(selectedKeyTimes, otherKeyTimes);

        for (int i = 0; i < otherKeyTimes.size(); i++)
            injectMarker(AnimationEditorSetting.TEMPLATE_OTHER_KEY_MARKER, otherKeyTimes.getFloat(i));

        for (int i = 0; i < selectedKeyTimes.size(); i++)
            injectMarker(AnimationEditorSetting.TEMPLATE_KEY_MARKER, selectedKeyTimes.getFloat(i));
    }

    private void injectMarker(String template, float time) {

        DimensionVector2Struct position = timelinePositions[toTimelineStep(time)];

        menuManager.inject(
                toolbarMenu,
                AnimationEditorSetting.ENTRY_KEYS,
                template,
                marker -> marker.setPositionOverride(position));
    }

    private int toTimelineStep(float time) {

        float fraction = time / animationEditorManager.getTimelineSpan();
        int step = Math.round(fraction * AnimationEditorSetting.TIMELINE_STEPS);

        return Math.max(0, Math.min(AnimationEditorSetting.TIMELINE_STEPS, step));
    }
}
