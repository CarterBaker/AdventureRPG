package editor.runtime.recorder.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSetting;
import editor.runtime.recorder.RecorderTabProvider;
import engine.root.SystemPackage;

public class RecorderRowSystem extends SystemPackage {

    /*
     * Binds one Recorder tab to a UI render target and opens the menu its
     * context names, and holds what every Recorder tab fills its panels
     * with: header, text, setting and action rows on any of the menu's entry
     * points, the times and channel lists they show, and the inspector of
     * what is selected on a clip — its node, its sound or its transition —
     * which the Bin's clip panel and the Keys' inspector both show. While a
     * pane tab is focused, its window or its chrome, it is the pane the
     * recorder's keys and deletes act on. Created first in every Recorder
     * tab, so the menu is open before any panel looks for it.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private WindowManager windowManager;
    private TabManager tabManager;
    private RecorderManager recorderManager;
    private SoundManager soundManager;
    private TransitionManager transitionManager;

    // Tab
    private String menuName;
    private RecorderPane pane;

    // Menus
    private MenuInstance menu;

    // Scratch
    private StringBuilder channelScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.channelScratch = new StringBuilder();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.windowManager = get(WindowManager.class);
        this.tabManager = get(TabManager.class);
        this.recorderManager = get(RecorderManager.class);
        this.soundManager = get(SoundManager.class);
        this.transitionManager = get(TransitionManager.class);
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
        if (pane != null && pane != recorderManager.getPane() && isFocused())
            recorderManager.showPane(pane);
    }

    private boolean isFocused() {

        WindowInstance focused = windowManager.getFocusedWindow();

        return focused != null && tabManager.resolveContentWindow(focused) == context.getWindow();
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

    // A row naming a setting and its value, pressed to change it; its value's label comes back
    public ElementInstance injectSetting(int entryPoint, String label, String value, String settingKey) {

        ElementInstance row = menuManager.inject(menu, entryPoint, RecorderSetting.TEMPLATE_SETTING_ROW);
        ElementInstance valueLabel = row.findChildById(RecorderSetting.ELEMENT_ROW_VALUE);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
        valueLabel.setFontText(value);
        return valueLabel;
    }

    public void injectAction(int entryPoint, String label, String settingKey) {

        ElementInstance row = menuManager.inject(menu, entryPoint, RecorderSetting.TEMPLATE_ACTION_ROW);

        row.setActionArgOverride(settingKey);
        row.findChildById(RecorderSetting.ELEMENT_ROW_LABEL).setFontText(label);
    }

    // Inspector \\

    // The node selected on the clip: what its character does across it, and how it is changed
    public void injectNode(int entryPoint) {

        int selected = recorderManager.getSelectedNode();

        if (selected < 0 || selected >= recorderManager.getNodes().size())
            return;

        RecordingNodeStruct node = recorderManager.getNodes().get(selected);

        injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_NODE_HEADER,
                RecorderSetting.TEXT_HEADER_NODE, node.getTrack().getTrackLabel()));
        injectText(entryPoint, String.format(RecorderSetting.FORMAT_NODE,
                node.getLabel(), formatTime(node.getStart()), formatTime(node.getEnd())));

        if (node.getKind() == RecorderNodeKind.MOVEMENT) {
            injectSetting(entryPoint, RecorderSetting.TEXT_NODE_DOING, node.getLabel(),
                    EditorSetting.RECORDER_KEY_NODE_NEXT);
            injectAction(entryPoint, RecorderSetting.TEXT_NODE_PREVIOUS,
                    EditorSetting.RECORDER_KEY_NODE_PREVIOUS);
            injectAction(entryPoint, RecorderSetting.TEXT_REMOVE_MOVEMENT,
                    EditorSetting.RECORDER_KEY_NODE_REMOVE);
        } else
            injectAction(entryPoint, RecorderSetting.TEXT_REMOVE_GESTURE,
                    EditorSetting.RECORDER_KEY_NODE_REMOVE);

        if (node.isMovable())
            injectText(entryPoint, node.getKind() == RecorderNodeKind.MOVEMENT
                    ? RecorderSetting.TEXT_NODE_MOVE_HINT
                    : RecorderSetting.TEXT_GESTURE_MOVE_HINT);

        injectText(entryPoint, RecorderSetting.TEXT_NODE_SHARED);
    }

    // The sound selected on the Sounds layer: what it is, when and how it is heard within its clip, and how it is
    // changed
    public void injectSound(int entryPoint) {

        RecordingSoundTrackStruct track = recorderManager.getKeySoundTrack();
        RecordingClipStruct clip = recorderManager.getKeyClip();
        int selected = recorderManager.getSelectedSound();

        if (track == null || clip == null || selected < 0 || selected >= track.size())
            return;

        String soundName = track.getSoundName(selected);
        float start = clip.toTimelineTime(track.getTime(selected));

        injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_SOUND_HEADER, soundName));
        injectText(entryPoint, String.format(RecorderSetting.FORMAT_SOUND_SPAN,
                formatTime(Math.max(start, clip.getStart())),
                formatTime(Math.min(start + track.getDuration(selected), clip.getEnd()))));
        injectText(entryPoint, track.isSpatial(selected)
                ? RecorderSetting.TEXT_SOUND_PLACED
                : RecorderSetting.TEXT_SOUND_UNPLACED);

        if (!soundManager.hasSound(soundName))
            injectText(entryPoint, RecorderSetting.TEXT_SOUND_MISSING);
        else if (recorderManager.isSoundLooping(selected))
            injectText(entryPoint, RecorderSetting.TEXT_SOUND_LOOPS);

        injectSetting(entryPoint, RecorderSetting.TEXT_SOUND_VOLUME,
                String.format(RecorderSetting.FORMAT_SOUND_PERCENT,
                        track.getVolume(selected) * EditorSetting.RECORDER_SOUND_PERCENT),
                EditorSetting.RECORDER_KEY_SOUND_VOLUME);
        injectSetting(entryPoint, RecorderSetting.TEXT_SOUND_PITCH,
                String.format(RecorderSetting.FORMAT_SOUND_PERCENT,
                        track.getPitch(selected) * EditorSetting.RECORDER_SOUND_PERCENT),
                EditorSetting.RECORDER_KEY_SOUND_PITCH);
        injectSetting(entryPoint, RecorderSetting.TEXT_SOUND_LENGTH,
                String.format(RecorderSetting.FORMAT_SOUND_SECONDS, track.getDuration(selected)),
                EditorSetting.RECORDER_KEY_SOUND_LENGTH);
        injectAction(entryPoint, RecorderSetting.TEXT_SOUND_SEEK, EditorSetting.RECORDER_KEY_SOUND_SEEK);
        injectAction(entryPoint, RecorderSetting.TEXT_SOUND_COPY, EditorSetting.RECORDER_KEY_SOUND_COPY);
        injectAction(entryPoint, RecorderSetting.TEXT_SOUND_REPLACE, EditorSetting.RECORDER_KEY_SOUND_REPLACE);
        injectAction(entryPoint, RecorderSetting.TEXT_SOUND_REMOVE, EditorSetting.RECORDER_KEY_SOUND_REMOVE);
        injectAction(entryPoint, RecorderSetting.TEXT_SOUND_REMOVE_ALL, EditorSetting.RECORDER_KEY_SOUND_REMOVE_ALL);
        injectText(entryPoint, RecorderSetting.TEXT_SOUND_MOVE_HINT);
        injectText(entryPoint, RecorderSetting.TEXT_SOUND_SHARED);
    }

    // The transition selected on a clip: which it is, the edge it lies on and when it runs, how it looks, and how
    // it is changed
    public void injectTransition(int entryPoint) {

        RecordingClipStruct clip = recorderManager.getSelectedTransitionClip();

        if (clip == null)
            return;

        RecorderClipEdge edge = recorderManager.getSelectedTransitionEdge();
        TransitionHandle transitionHandle = recorderManager.resolveTransitionHandle(clip, edge);
        float from = clip.resolveTransitionStart(edge);
        float seconds = clip.resolveTransitionSeconds(edge);

        injectHeader(entryPoint, String.format(RecorderSetting.FORMAT_TRANSITION_HEADER,
                transitionHandle.getDisplayName()));
        injectText(entryPoint, String.format(RecorderSetting.FORMAT_TRANSITION_SPAN,
                edge.getLabel().toLowerCase(), formatTime(from), formatTime(from + seconds)));
        injectText(entryPoint, String.format(RecorderSetting.FORMAT_TRANSITION_LOOK,
                transitionHandle.getKind().getLabel(), transitionHandle.getEasing().getLabel().toLowerCase()));

        if (!transitionManager.hasTransition(clip.getTransitionName(edge)))
            injectText(entryPoint, RecorderSetting.TEXT_TRANSITION_MISSING);

        injectSetting(entryPoint, RecorderSetting.TEXT_TRANSITION_LENGTH,
                String.format(RecorderSetting.FORMAT_TRANSITION_SECONDS, seconds),
                EditorSetting.RECORDER_KEY_TRANSITION_LENGTH);
        injectAction(entryPoint, RecorderSetting.TEXT_TRANSITION_REPLACE,
                EditorSetting.RECORDER_KEY_TRANSITION_REPLACE);
        injectAction(entryPoint, RecorderSetting.TEXT_TRANSITION_REMOVE, EditorSetting.RECORDER_KEY_TRANSITION_REMOVE);
        injectText(entryPoint, RecorderSetting.TEXT_TRANSITION_DRAG_HINT);
        injectText(entryPoint, RecorderSetting.TEXT_TRANSITION_SOUND);
    }

    // Text \\

    public String formatTime(float seconds) {

        int minutes = (int) (seconds / RecorderSetting.SECONDS_PER_MINUTE);

        return String.format(
                RecorderSetting.FORMAT_TIME, minutes, seconds - minutes * RecorderSetting.SECONDS_PER_MINUTE);
    }

    public String toOnOff(boolean on) {
        return on ? RecorderSetting.TEXT_ON : RecorderSetting.TEXT_OFF;
    }

    // Every channel in a mask by its label, in channel order
    public String describeChannels(int channelMask) {

        channelScratch.setLength(0);

        for (RecorderChannel channel : RecorderChannel.VALUES) {

            if (!channel.isIn(channelMask))
                continue;

            if (!channelScratch.isEmpty())
                channelScratch.append(RecorderSetting.TEXT_CHANNEL_SEPARATOR);

            channelScratch.append(channel.getLabel());
        }

        return channelScratch.toString();
    }
}
