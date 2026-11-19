package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.recorder.RecorderTabProvider;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderRowSystem extends SystemPackage {

    /*
     * Binds one Recorder tab to a UI render target and opens the menu its
     * context names, and holds what every Recorder tab fills its panels
     * with: header and text rows on any of the menu's entry points, the steps
     * across the view the strips, the graph and their playheads stand on, and
     * the times and the way down to the recording open they show. While a
     * pane tab is the active window, it is the pane the recorder's keys and
     * deletes act on. Created first in every Recorder tab, so the menu is open
     * before any panel looks for it.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private ActiveWindowManager activeWindowManager;
    private RecorderManager recorderManager;

    // Tab
    private String menuName;
    private RecorderPane pane;

    // Menus
    private MenuInstance menu;

    // Steps — every step across the view as a percentage of it, made once
    private DimensionVector2Struct[] stepPositions;

    // Scratch — text built for a row or a label
    private StringBuilder textScratch;

    // Base \\

    @Override
    protected void create() {

        // Steps
        this.stepPositions = new DimensionVector2Struct[RecorderSetting.TIMELINE_STEPS + 1];

        for (int i = 0; i < stepPositions.length; i++)
            stepPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(toStepPercent(i)),
                    DimensionValueStruct.ofAbsolute(0f));

        // Scratch
        this.textScratch = new StringBuilder();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof RecorderTabProvider))
            throwException("A Recorder tab's context must name the menu it shows.");

        RecorderTabProvider provider = (RecorderTabProvider) context;
        WindowInstance window = context.getWindow();

        this.menuName = provider.getRecorderMenu();
        this.pane = provider.getRecorderPane();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.menu = menuManager.openMenu(menuName, window);
    }

    // Update \\

    @Override
    protected void update() {
        if (pane != null && pane != recorderManager.getPane() && activeWindowManager.isActive(context.getWindow()))
            recorderManager.showPane(pane);
    }

    // Menu \\

    public MenuInstance getMenu() {
        return menu;
    }

    public ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = menu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Recorder menu '" + menuName + "' has no entry point " + entryPoint + ".");

        return element;
    }

    public ElementInstance requireChild(ElementInstance parent, String childId) {

        ElementInstance element = parent.findChildById(childId);

        if (element == null)
            throwException("Recorder menu '" + menuName + "' has no element '" + childId + "'.");

        return element;
    }

    // Rows \\

    public ElementInstance injectHeader(int entryPoint, String text) {

        ElementInstance label = menuManager.inject(menu, entryPoint, RecorderSetting.TEMPLATE_HEADER_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL);

        label.setFontText(text);
        return label;
    }

    public ElementInstance injectText(int entryPoint, String text) {

        ElementInstance label = menuManager.inject(menu, entryPoint, RecorderSetting.TEMPLATE_TEXT_ROW)
                .findChildById(RecorderSetting.ELEMENT_ROW_LABEL);

        label.setFontText(text);
        return label;
    }

    // Steps \\

    public DimensionVector2Struct getStepPosition(int step) {
        return stepPositions[step];
    }

    // The step across the view a moment of the timeline falls on, held to the view's ends
    public int toViewStep(float time) {

        int step = Math.round(recorderManager.getView().toFraction(time) * RecorderSetting.TIMELINE_STEPS);

        return Math.clamp(step, 0, RecorderSetting.TIMELINE_STEPS);
    }

    public float toStepPercent(int step) {
        return RecorderSetting.TIMELINE_PERCENT * step / RecorderSetting.TIMELINE_STEPS;
    }

    // The playhead line placed where the playhead stands across the view
    public void placePlayhead(RecorderPlayheadStruct playhead) {

        float time = recorderManager.getPlayhead();
        boolean inView = recorderManager.getView().contains(time);
        int step = inView ? toViewStep(time) : EngineSetting.INDEX_NOT_FOUND;

        playhead.place(inView, step, inView ? stepPositions[step] : null);
    }

    // Text \\

    public String formatTime(float seconds) {

        int minutes = (int) (seconds / RecorderSetting.SECONDS_PER_MINUTE);

        return String.format(
                RecorderSetting.FORMAT_TIME, minutes, seconds - minutes * RecorderSetting.SECONDS_PER_MINUTE);
    }

    // What is open: every recording the open one was opened inside, outermost first, down to it, marked while it
    // holds unsaved changes
    public String describeOpen() {

        RecordingInstance recording = recorderManager.getRecording();

        if (recording == null)
            return RecorderSetting.TEXT_NO_RECORDING;

        ObjectArrayList<String> parentNames = recorderManager.getParentNames();

        textScratch.setLength(0);

        for (int i = 0; i < parentNames.size(); i++)
            textScratch.append(parentNames.get(i)).append(RecorderSetting.TEXT_BREADCRUMB_SEPARATOR);

        textScratch.append(recording.getRecordingName());

        if (recorderManager.isUnsaved())
            textScratch.append(RecorderSetting.TEXT_UNSAVED_SUFFIX);

        return textScratch.toString();
    }
}
