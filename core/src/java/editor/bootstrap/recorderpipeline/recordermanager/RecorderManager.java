package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;
import java.util.Arrays;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.combatpipeline.projectilemanager.ProjectileManager;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongTempoStruct;
import application.bootstrap.musicpipeline.util.NoteScale;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import application.bootstrap.musicpipeline.util.SongMidiUtility;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderClipEdge;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveKind;
import editor.bootstrap.recorderpipeline.recording.RecorderDragKind;
import editor.bootstrap.recorderpipeline.recording.RecorderExportResult;
import editor.bootstrap.recorderpipeline.recording.RecorderLayerView;
import editor.bootstrap.recorderpipeline.recording.RecorderNodeKind;
import editor.bootstrap.recorderpipeline.recording.RecorderPane;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingBoneTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingDragStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingNodeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingPlayingStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingSoundTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTransitionStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingViewStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.RecordingCurveUtility;
import editor.bootstrap.tabpipeline.tab.TabHandle;
import editor.bootstrap.tabpipeline.tabmanager.TabManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.dev.DevContext;
import editor.runtime.EditorSetting;
import editor.runtime.recorder.RecorderSoundHost;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.curves.CurveKeyMode;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecorderManager extends ManagerPackage {

    /*
     * Owns the recorder: the media files saved in the media directory beside
     * the player's saves, video and music alike, the one open, its transport,
     * and the editing of its bin and timeline. A session — recording, playing,
     * paused or exporting — drives the first Dev window that has not crashed,
     * found among TabManager's open tabs as console commands find it, and that
     * window's DevRecorderSystem carries the session out inside its own crash
     * boundary from what this holds: which channels play from the timeline,
     * which the take records live, and the playhead. A take records the armed
     * channels from the playhead until stopped, the clips beneath playing
     * every other channel; it then joins the bin and is laid on the timeline
     * where it was recorded, over every clip it was recorded against. Takes
     * are laid again by dragging them from the Layers tab's Takes view, or at
     * the playhead, and clips are moved by
     * dragging, trimmed by dragging an edge or at the playhead, cut in two at
     * the playhead or, with the Cut tool chosen for the timeline in the Tools
     * panel, wherever it is pressed, stamped, muted, given or stripped of
     * channels, and removed, each by one method here; what is dragged snaps to
     * the timeline's edges, and the cut tool shows where it would cut under
     * the pointer. A press on the lanes is read once, where it began, for what
     * it takes hold of. The nodes of every clip playing the player — what it
     * does, run by run — are rebuilt after every edit, and are retimed, moved,
     * changed and removed here too. The view of the timeline zooms and
     * scrolls, and pages along after a playing playhead. Every value a take
     * records is laid on a curve of keys as soon as the take is stopped, and
     * every take of a recording opened that has none yet, one track a frame so
     * nothing stalls, and the curves play from then on: a recording is made of
     * keys. The Keys tab shows one layer of the selected clip's take — the
     * camera, the player, a resident, item or vehicle, the weather or the
     * clock, a row for each value it holds, or the take's Sounds layer, a row
     * for each sound it heard with a bar for every time it was heard — and
     * the Keys Graph tab the curve of the value selected. Sounds are selected, retimed along
     * their rows, copied to the playhead, added from the sound library, given
     * another volume, pitch, length or sound, and removed one by one or every
     * one of a sound at once, each by one method here, and a change to any of
     * them stops whatever the timeline was sounding so it is heard again as it
     * now stands. Keys are added, deleted, retimed on their rows, moved and
     * bent by their handles on the graph, given a mode or a typed value, and a
     * curve is laid again from the recording, each by one method here; a key
     * or handle dragged changes its curve as it moves. Any number of a curve's
     * keys are selected at once — picked one by one or boxed on the graph or a
     * row, added to or taken away from — and move and delete together; the
     * last picked is the one whose handles are drawn. A key clicked twice on
     * the graph lets its handles go their own ways, and twice again joins them
     * on one line; a handle dragged with snapping lands on its key's level and
     * on the moments and values of the curve's other keys within reach. The
     * Layers tab lists the take's tracks, the layer picked the one the Keys
     * tab shows, or every take in the bin.
     * What is selected in the pane worked in last is deleted as one: on the
     * Keys the keys, else the sound, else the node; on the Timeline the
     * transition, else the node, else the clips. A sound dragged
     * in from the sound library, or added at the playhead, is laid as a take
     * of its own holding that sound alone, the bin keeping one such take for
     * each sound however often it is laid; a clip of such a take selected
     * selects its sound, so its volume, pitch and length are changed straight
     * from the clip. Sounds add up rather than give way: while the timeline
     * plays or a take records, every clip covering the playhead sounds
     * together, and an export mixes every one of them into its video's sound.
     * The player cannot be recorded without the camera that follows it. While
     * a session plays the world, residents, thrown items and vehicles are
     * held. While a session runs, its Dev window is the one heard whichever
     * window has focus, so the world is heard from its camera and its ambience
     * and music are what a take records; while the timeline plays the sound
     * channel, they hold silent, as the take already holds them. A take and
     * the timeline playing run on whether or not the game is paused, so the
     * pause menu that lets the cursor out of the Dev window never stops a
     * take; only an export waits while the game is paused, and for a few
     * frames after, so no paused frame reaches a video. A media file with no
     * picture plays without a Dev window, its sound carried by a Recorder tab;
     * looping, the playhead runs round the range marked, or the whole media
     * file when none is. Stopping ends every session and
     * the Dev window goes back to just where it was. A transition dragged in
     * from the transitions tool is laid on the clip edge it is let go over,
     * the end of a clip another follows on its lane handing it to the start of
     * the one following; a press on one laid selects it, and dragging it moves
     * its far side to make it run longer or shorter. While the timeline plays
     * or exports, the transition the picture passes through at the playhead is
     * read again whenever it is asked for, and the Dev window renders the
     * timeline both with and without the clip it lies on — the side it plays
     * each frame set here — so the two can be blended; every clip's own sounds
     * fade in and out with its transitions. The recordings themselves are
     * listed, opened, renamed, copied and deleted from the Recordings tool.
     * Each structural change marks the recording unsaved, moves the revision
     * the recorder tab redraws on and is a step of the open recording's undo
     * history, undone and redone as one, the selection and view kept. Takes save the recording at once.
     * A clip plays a take, another recording nested by name, a song or a notes
     * clip: a saved recording dragged from the Recordings tab is laid as a
     * clip nesting it, refused when it would reach the open recording again at
     * any depth, naming the loop, or when it shows a picture of another world;
     * every clip is bound to what it names whenever a recording opens, and a
     * recording is read again wherever it is nested once it is saved, so
     * editing it changes every place it lies. A nested clip clicked twice
     * opens its recording inside the open one, and Back returns to the one it
     * was opened inside, the way down shown as a breadcrumb. Any number of
     * clips are selected at once — picked alone, or added and taken away with
     * Shift — and move and are deleted together, and Nest Selection moves them
     * into a new recording laid where they were as one clip. A note dragged
     * from the Note Library is laid as a note clip whose note always fills it.
     * A media file any clip of which shows a picture is a video and exports as
     * one, frame by frame; one with none is music and exports as the game's
     * own song and rendered sound; both land in the export directory.
     * An animation clip opens on the timeline in a media file's place, its
     * ARPG as InfoManager holds it being what is edited: its one take holds a
     * track for every bone, laid once from its start, its keys edited on the
     * Keys and Keys Graph tabs like any take's and written back into the clip
     * on the frame they change, so the preview, the Info Panel and the
     * Animation Editor see them at once, and a change made to the clip
     * anywhere else is read back as a step of the same history. Its one clip
     * is never moved, cut or joined by another, nothing is recorded into it,
     * and it is saved, reverted and deleted as content; a drag begun in
     * another window is one step, and taken back whole when it is cancelled.
     * Each layer of the clip — its own tracks and every part a stamp joined —
     * lies on a lane of its own, and a press on one picks that layer.
     */

    // Internal
    private TabManager tabManager;
    private ToolManager toolManager;
    private WorldManager worldManager;
    private ClockManager clockManager;
    private NpcManager npcManager;
    private ProjectileManager projectileManager;
    private VehicleManager vehicleManager;
    private SoundManager soundManager;
    private TransitionManager transitionManager;
    private RecordingFileBranch recordingFileBranch;
    private RecordingTimelineBranch recordingTimelineBranch;
    private RecordingNodeBranch recordingNodeBranch;
    private RecordingCurveBranch recordingCurveBranch;
    private RecordingMixBranch recordingMixBranch;
    private RecordingSoundBranch recordingSoundBranch;
    private RecordingTransitionBranch recordingTransitionBranch;
    private RecordingSourceBranch recordingSourceBranch;
    private RecordingMusicBranch recordingMusicBranch;
    private RecordingRowBranch recordingRowBranch;
    private RecordingRenderBranch recordingRenderBranch;
    private RecordingLayerBranch recordingLayerBranch;
    private RecordingHistoryBranch recordingHistoryBranch;
    private RecordingAnimationBranch recordingAnimationBranch;
    private InstrumentManager instrumentManager;
    private InfoManager infoManager;

    // Directory — the media files beside the player's saves, and what they export to
    private File mediaDirectory;
    private File exportDirectory;

    // Recording
    private RecordingInstance recording;
    private boolean unsaved;

    // Animation — how far the open clip's edits have gone and how far they were written into it, and how many
    // gestures of another window have begun, each one step of its history
    private int documentRevision;
    private int writtenRevision;
    private int gestureCount;

    // Tracks — whether the Tracks tab lists the animations of the character chosen rather than the media files
    private boolean listingAnimations;

    // Nesting — the recordings opened on the way down to the open one, outermost first
    private ObjectArrayList<String> parentNames;

    // Transport
    private RecorderState state;
    private float playhead;
    private int armedChannels;
    private RecordingTakeStruct take;
    private float takeStart;
    private int takeCount;
    private DevContext devContext;
    private ContextPackage soundHost;
    private boolean looping;

    // Music — the lane whose mix the Properties tab shows, and the share of a render last shown
    private int selectedLane;
    private int renderPercent;

    // Suspension
    private boolean suspended;
    private int resumeFrames;

    // Selection — the clip picked last, every clip selected with it, and the clip last clicked and when, so a second
    // click on it opens what it nests
    private int selectedTake;
    private int selectedClip;
    private IntArrayList selectedClips;
    private int clickedClip;
    private long clickedMillis;
    private int selectedNode;
    private int selectedNodeClip;
    private RecordingBodyTrackStruct selectedNodeTrack;
    private RecorderNodeKind selectedNodeKind;
    private float selectedNodeTime;
    private int selectedTransition;

    // History — the clips and keys selected, kept while a step of the history is put back
    private IntArrayList keptClips;
    private IntArrayList keptKeys;

    // Transition — the one the picture passes through at the playhead, read again whenever it is asked for
    private RecordingTransitionStruct transition;

    // Nodes
    private ObjectArrayList<RecordingNodeStruct> nodes;

    // Drag
    private RecordingDragStruct drag;

    // Cut
    private int cutHoverClip;
    private float cutHoverTime;

    // View
    private RecordingViewStruct view;
    private int viewRevision;

    // Pane
    private RecorderPane pane;

    // Keys
    private RecordingTakeStruct keyTake;
    private ObjectArrayList<RecordingCurveTrackStruct> keyTracks;
    private int keyLayer;
    private RecordingCurveTrackStruct selectedTrack;
    private int selectedSlot;
    private int selectedKey;
    private IntArrayList selectedKeys;
    private FloatArrayList draggedKeyTimes;
    private DoubleArrayList draggedKeyValues;
    private double graphMinimum;
    private double graphMaximum;
    private int keysRevision;
    private int curveRevision;

    // Key Clicks — the key of the curve last clicked on the graph and when, so a second click on it frees or joins
    // its handles
    private RecordingCurveStruct clickedCurve;
    private int clickedKey;
    private long clickedKeyMillis;

    // Layers — how the Layers tab lists the recorder for each pane, and how often what it lists changed
    private RecorderLayerView[] layerViews;
    private int layerRevision;

    // Sounds — the sound selected on the Sounds layer, and how often a sound was changed
    private int selectedSound;
    private int soundRevision;

    // Curves
    private ObjectArrayList<RecordingCurveTrackStruct> unlaidTracks;
    private boolean curvesLaid;
    private boolean saveWhenLaid;


    // Export
    private File exportFile;
    private int exportedFrames;
    private int exportFrameCount;
    private RecorderExportResult exportResult;

    // Status
    private int revision;
    private String statusText;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.recordingFileBranch = create(RecordingFileBranch.class);
        this.recordingTimelineBranch = create(RecordingTimelineBranch.class);
        this.recordingNodeBranch = create(RecordingNodeBranch.class);
        this.recordingCurveBranch = create(RecordingCurveBranch.class);
        this.recordingMixBranch = create(RecordingMixBranch.class);
        this.recordingSoundBranch = create(RecordingSoundBranch.class);
        this.recordingTransitionBranch = create(RecordingTransitionBranch.class);
        this.recordingSourceBranch = create(RecordingSourceBranch.class);
        this.recordingMusicBranch = create(RecordingMusicBranch.class);
        this.recordingRowBranch = create(RecordingRowBranch.class);
        this.recordingRenderBranch = create(RecordingRenderBranch.class);
        this.recordingLayerBranch = create(RecordingLayerBranch.class);
        this.recordingHistoryBranch = create(RecordingHistoryBranch.class);
        this.recordingAnimationBranch = create(RecordingAnimationBranch.class);

        // Directory
        this.mediaDirectory = new File(internal.path, EditorSetting.RECORDER_MEDIA_DIRECTORY);
        this.exportDirectory = new File(mediaDirectory, EditorSetting.RECORDER_EXPORT_DIRECTORY);
        recordingSourceBranch.setRecordingDirectory(mediaDirectory);

        // Nesting
        this.parentNames = new ObjectArrayList<>();

        // Transport
        this.state = RecorderState.IDLE;
        this.armedChannels = RecorderChannel.ALL;

        // Music
        this.renderPercent = EngineSetting.INDEX_NOT_FOUND;

        // History
        this.keptClips = new IntArrayList();
        this.keptKeys = new IntArrayList();

        // Selection
        this.selectedClips = new IntArrayList();
        this.clickedClip = EngineSetting.INDEX_NOT_FOUND;
        clearSelection();

        // Transition
        this.transition = new RecordingTransitionStruct();

        // Nodes
        this.nodes = new ObjectArrayList<>();

        // Drag
        this.drag = new RecordingDragStruct();

        // Cut
        this.cutHoverClip = EngineSetting.INDEX_NOT_FOUND;

        // View
        this.view = new RecordingViewStruct();

        // Pane
        this.pane = RecorderPane.KEYS;

        // Keys
        this.keyTracks = new ObjectArrayList<>();
        this.keyLayer = EditorSetting.RECORDER_KEY_LAYER_NONE;
        this.selectedKeys = new IntArrayList();
        this.draggedKeyTimes = new FloatArrayList();
        this.draggedKeyValues = new DoubleArrayList();
        clearCurveSelection();

        // Key Clicks
        this.clickedKey = EngineSetting.INDEX_NOT_FOUND;

        // Layers
        this.layerViews = new RecorderLayerView[RecorderPane.VALUES.length];

        for (int i = 0; i < layerViews.length; i++)
            layerViews[i] = RecorderPane.VALUES[i].getLayerView();

        // Sounds
        clearSoundSelection();

        // Curves
        this.unlaidTracks = new ObjectArrayList<>();

        // Export
        this.exportResult = RecorderExportResult.NONE;

        // Status
        this.statusText = EditorSetting.RECORDER_STATUS_READY;
    }

    @Override
    protected void get() {
        this.tabManager = get(TabManager.class);
        this.toolManager = get(ToolManager.class);
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
        this.npcManager = get(NpcManager.class);
        this.projectileManager = get(ProjectileManager.class);
        this.vehicleManager = get(VehicleManager.class);
        this.soundManager = get(SoundManager.class);
        this.transitionManager = get(TransitionManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::refreshAnimation);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::closeAnimation);
    }

    // Update \\

    @Override
    protected void update() {
        releaseCrashedDevContext();
        updateSuspension();
        advancePlayhead();
        updateView();
        updateKeyTracks();
        layCurves();
        writeAnimation();
        holdWorld();
        holdSound();
        updateRender();
    }

    private void releaseCrashedDevContext() {
        if (devContext != null && devContext.isCrashed())
            stop();
    }

    // An export waits while the game is paused, and for a few frames after, so no paused frame reaches its video; a
    // take and the timeline playing run on, so the pause menu that lets the cursor out never stops them
    private void updateSuspension() {

        boolean wanted = state == RecorderState.EXPORTING && clockManager.isPaused();

        if (wanted)
            this.resumeFrames = EditorSetting.RECORDER_RESUME_FRAMES;
        else if (resumeFrames > 0)
            this.resumeFrames--;

        boolean nowSuspended = state == RecorderState.EXPORTING && (wanted || resumeFrames > 0);

        if (nowSuspended == suspended)
            return;

        this.suspended = nowSuspended;
        setStatus(suspended ? EditorSetting.RECORDER_STATUS_SUSPENDED : resolveRunningStatus());
    }

    private String resolveRunningStatus() {
        return switch (state) {
            case RECORDING -> EditorSetting.RECORDER_STATUS_RECORDING + take.getName();
            case PLAYING -> EditorSetting.RECORDER_STATUS_PLAYING;
            case PAUSED -> EditorSetting.RECORDER_STATUS_PAUSED;
            case EXPORTING -> EditorSetting.RECORDER_STATUS_EXPORTING + exportFile.getName();
            case IDLE -> EditorSetting.RECORDER_STATUS_READY;
        };
    }

    private void advancePlayhead() {

        if (suspended || state != RecorderState.RECORDING && state != RecorderState.PLAYING)
            return;

        playhead += internal.getDeltaTime();

        if (state != RecorderState.PLAYING)
            return;

        float loopStart = resolveLoopStart();
        float loopEnd = resolveLoopEnd();

        if (looping && loopEnd > loopStart) {

            if (playhead >= loopEnd)
                playhead = loopStart;

            return;
        }

        if (playhead >= recording.getDuration()) {
            playhead = recording.getDuration();
            state = RecorderState.PAUSED;
            revision++;
        }
    }

    // The stretch a looping playhead runs round: the range marked, else the whole media file
    private float resolveLoopStart() {

        RecordingExportStruct exportStruct = recording.getExportStruct();

        return exportStruct.hasRangeStart() ? exportStruct.getRangeStart() : 0f;
    }

    private float resolveLoopEnd() {

        RecordingExportStruct exportStruct = recording.getExportStruct();

        return exportStruct.hasRangeEnd() ? exportStruct.getRangeEnd() : recording.getDuration();
    }

    // The view keeps to the timeline as it grows and shrinks, and pages along after a running playhead
    private void updateView() {

        view.resolve(resolveTimelineSpan());

        if (state == RecorderState.RECORDING || state == RecorderState.PLAYING || state == RecorderState.EXPORTING)
            revealPlayhead();
    }

    private void revealPlayhead() {
        if (view.reveal(playhead, resolveTimelineSpan()))
            viewRevision++;
    }

    // The world is held whenever a session plays it rather than recording it live
    private void holdWorld() {

        int played = devContext != null ? getPlayedChannels() : RecorderChannel.NONE;

        npcManager.setHeld(RecorderChannel.CHARACTERS.isIn(played));
        projectileManager.setHeld(RecorderChannel.ITEMS.isIn(played));
        vehicleManager.setHeld(RecorderChannel.VEHICLES.isIn(played));
    }

    // A session's Dev window is the one heard whatever has focus, and its own ambience, music and heartbeat hold
    // silent while the timeline plays the sound channel in their place
    private void holdSound() {

        if (devContext == null) {
            soundManager.unpinHeardWindow();
            return;
        }

        soundManager.pinHeardWindow(devContext.getWindow().getWindowID(),
                RecorderChannel.SOUND.isIn(getPlayedChannels()));
    }

    // Session \\

    // Binds the session to the first Dev window still running, in the world a video was made in; music plays in any,
    // and plays without one through the first Recorder tab that can sound it, though a take always records in one;
    // an animation clip plays in no window, its playhead alone running
    private boolean bindSession(boolean needsDevWindow) {

        if (recording.isAnimation())
            return true;

        if (recording.hasPicture()
                && !recording.getWorldName().equals(worldManager.getActiveWorld().getWorldName())) {
            setStatus(EditorSetting.RECORDER_STATUS_WRONG_WORLD + recording.getWorldName());
            return false;
        }

        ObjectArrayList<TabHandle> openTabs = tabManager.getOpenTabs();
        ContextPackage musicHost = null;

        for (int i = 0; i < openTabs.size(); i++) {

            ContextPackage content = openTabs.get(i).getTabContext().getContentContext();

            if (content instanceof DevContext candidate && !candidate.isCrashed()) {
                this.devContext = candidate;
                this.soundHost = candidate;
                return true;
            }

            if (musicHost == null && content instanceof RecorderSoundHost)
                musicHost = content;
        }

        if (!needsDevWindow && !recording.hasPicture() && musicHost != null) {
            this.soundHost = musicHost;
            return true;
        }

        setStatus(EditorSetting.RECORDER_STATUS_NO_DEV_WINDOW);
        return false;
    }

    // Starts a session if none runs, true once one does; a take and an export need a Dev window, so a session
    // sounding music through a Recorder tab gives way to one bound to a Dev window
    private boolean beginSession(boolean needsDevWindow) {

        if (state.isSession() && (devContext != null || !needsDevWindow))
            return true;

        if (state.isSession())
            endSession();

        return bindSession(needsDevWindow);
    }

    // The window a session runs in leaving ends the session
    public void releaseSessionHost(ContextPackage context) {
        if (soundHost == context)
            stop();
    }

    // Transport \\

    // Records a take of every armed channel, in a new media file when none is open
    public void record() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        if (armedChannels == RecorderChannel.NONE) {
            setStatus(EditorSetting.RECORDER_STATUS_NOTHING_ARMED);
            return;
        }

        if (recording != null && recording.isAnimation()) {
            setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_NOT_RECORDED);
            return;
        }

        if (!ensureRecording())
            return;

        if (!beginSession(true))
            return;

        cancelDrag();
        this.take = new RecordingTakeStruct(resolveTakeName(), armedChannels);
        this.takeStart = playhead;
        this.takeCount++;
        this.state = RecorderState.RECORDING;
        setStatus(EditorSetting.RECORDER_STATUS_RECORDING + take.getName());
    }

    public void play() {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING || !hasClips())
            return;

        if (!beginSession(false))
            return;

        if (playhead >= recording.getDuration())
            playhead = 0f;

        this.state = RecorderState.PLAYING;
        setStatus(EditorSetting.RECORDER_STATUS_PLAYING);
    }

    public void pause() {

        switch (state) {
            case PLAYING -> {
                this.state = RecorderState.PAUSED;
                setStatus(EditorSetting.RECORDER_STATUS_PAUSED);
            }
            case PAUSED -> play();
            case IDLE -> preview();
            default -> {
            }
        }
    }

    // Plays on from the playhead, or pauses what is playing
    public void togglePlayback() {

        if (state == RecorderState.PLAYING)
            pause();
        else
            play();
    }

    // Playing runs round the range marked, or the whole media file, until looping is turned off again
    public void toggleLoop() {
        this.looping = !looping;
        setStatus(looping ? EditorSetting.RECORDER_STATUS_LOOP_ON : EditorSetting.RECORDER_STATUS_LOOP_OFF);
    }

    // Holds the Dev window at the playhead as the timeline stands there
    private void preview() {

        if (!hasClips() || !beginSession(false))
            return;

        this.state = RecorderState.PAUSED;
        setStatus(EditorSetting.RECORDER_STATUS_PAUSED);
    }

    // The one way every session ends; the Dev window returns to just where it was before the session began
    public void stop() {

        if (!state.isSession())
            return;

        if (state == RecorderState.RECORDING)
            finishTake();

        if (state == RecorderState.EXPORTING) {
            this.exportResult = RecorderExportResult.CANCELLED;
            setStatus(EditorSetting.RECORDER_STATUS_EXPORT_CANCELLED);
        } else if (state != RecorderState.RECORDING)
            setStatus(EditorSetting.RECORDER_STATUS_READY);

        endSession();
    }

    private void endSession() {
        this.state = RecorderState.IDLE;
        this.devContext = null;
        this.soundHost = null;
        transition.clear();
        this.suspended = false;
        this.resumeFrames = 0;
        holdWorld();
        holdSound();
    }

    // The take joins the bin and is laid where it was recorded, once the Dev window has handed over all it holds
    private void finishTake() {

        take.setDuration(playhead - takeStart);
        devContext.finishTake();

        if (take.getDuration() < EditorSetting.RECORDER_MIN_TAKE_SECONDS) {
            setStatus(EditorSetting.RECORDER_STATUS_TAKE_TOO_SHORT);
            this.take = null;
            return;
        }

        recording.addTake(take);
        pickClip(recordingTimelineBranch.placeRecordedTake(recording, take, takeStart));
        queueCurves(take);
        this.saveWhenLaid = true;
        this.take = null;
        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_SAVED + recording.getTakes().get(selectedTake).getName());
    }

    // Seek \\

    public void seek(float time) {

        if (recording == null || state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        this.playhead = Math.clamp(time, 0f, recording.getDuration());
        revealPlayhead();

        if (state == RecorderState.IDLE && !recording.isAnimation())
            preview();
    }

    // The moment a share of the way across the view
    public void scrub(float fraction) {
        if (recording != null)
            seek(view.toTime(fraction));
    }

    public void toStart() {
        seek(0f);
    }

    public void toEnd() {
        if (recording != null)
            seek(recording.getDuration());
    }

    // How long the recording runs, and on while a take records past it
    public float resolveLength() {
        return recording != null ? Math.max(recording.getDuration(), playhead) : playhead;
    }

    // The timeline drawn runs the recording's length and a little past it, so a clip can be laid after the last
    public float resolveTimelineSpan() {

        float length = resolveLength();

        return Math.max(
                EditorSetting.RECORDER_MIN_TIMELINE_SECONDS,
                length + Math.max(
                        EditorSetting.RECORDER_TIMELINE_TAIL_SECONDS,
                        length * EditorSetting.RECORDER_TIMELINE_TAIL_SHARE));
    }

    // View \\

    // The view zoomed by the factor about the moment, which stays where it is drawn
    public void zoomView(float time, float factor) {
        if (view.zoom(time, factor, resolveTimelineSpan()))
            viewRevision++;
    }

    // The buttons zoom about the playhead while it is in view, and about the middle of the view while it is not
    public void zoomViewIn() {
        zoomView(resolveZoomAnchor(), 1f / EditorSetting.RECORDER_ZOOM_BUTTON_STEP);
    }

    public void zoomViewOut() {
        zoomView(resolveZoomAnchor(), EditorSetting.RECORDER_ZOOM_BUTTON_STEP);
    }

    private float resolveZoomAnchor() {
        return view.contains(playhead) ? playhead : view.getMiddle();
    }

    public void fitView() {
        view.fit();
        view.resolve(resolveTimelineSpan());
        viewRevision++;
    }

    // The view moved along by a share of its own length, back for a share below zero
    public void scrollView(float share) {
        if (view.scrollTo(view.getStart() + share * view.getSpan(), resolveTimelineSpan()))
            viewRevision++;
    }

    // Arming \\

    // The player follows the camera, so arming the player arms the camera and disarming the camera the player
    public void toggleArm(RecorderChannel channel) {

        if (state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        armedChannels ^= channel.getBit();

        if (channel == RecorderChannel.PLAYER && RecorderChannel.PLAYER.isIn(armedChannels))
            armedChannels |= RecorderChannel.CAMERA.getBit();

        if (channel == RecorderChannel.CAMERA && !RecorderChannel.CAMERA.isIn(armedChannels))
            armedChannels &= ~RecorderChannel.PLAYER.getBit();

        revision++;
    }

    // Recordings \\

    // A new media file opened in place of the open one and saved at once, made in the active world, in four four at
    // RECORDER_DEFAULT_BPM in C major
    public boolean createRecording(String recordingName) {

        if (!isRecordingNameAvailable(recordingName)) {
            setStatus(EditorSetting.RECORDER_STATUS_NAME_TAKEN + recordingName);
            return false;
        }

        closeRecording();

        RecordingInstance created = create(RecordingInstance.class);
        created.constructor(
                recordingName,
                worldManager.getActiveWorld().getWorldName(),
                SongArpgUtility.createTempo(EditorSetting.RECORDER_DEFAULT_BPM),
                EngineSetting.DEFAULT_SONG_BEATS_PER_BAR,
                EngineSetting.DEFAULT_SONG_BEAT_UNIT,
                EngineSetting.DEFAULT_SONG_KEY_ROOT,
                NoteScale.MAJOR,
                new RecordingExportStruct());

        clearParents();
        activateRecording(created);
        markUnsaved();
        saveRecording();
        return true;
    }

    // The media file opened from the list, every recording it was opened inside let go
    public void openRecording(String recordingName) {

        if (isOpen(recordingName))
            return;

        clearParents();
        switchRecording(recordingName);
    }

    // The one way a saved recording is opened in place of the open one, read and bound to all it nests; an older
    // file is saved in the new form at once
    private boolean switchRecording(String recordingName) {

        File file = getRecordingFile(recordingName);

        if (!file.isFile()) {
            setStatus(EditorSetting.RECORDER_STATUS_MISSING + recordingName);
            return false;
        }

        RecordingInstance opened = recordingFileBranch.read(file, recordingName);

        if (opened == null) {
            setStatus(EditorSetting.RECORDER_STATUS_UNREADABLE + recordingName);
            return false;
        }

        boolean upgraded = recordingFileBranch.isUpgraded();

        closeRecording();
        activateRecording(opened);

        if (!upgraded) {
            setStatus(EditorSetting.RECORDER_STATUS_OPENED + recordingName);
            return true;
        }

        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_UPGRADED + recordingName);
        return true;
    }

    // The open recording written to its file, refused while it nests itself at any depth; every recording kept
    // for nesting it is read again, so it is nested as it now stands
    public void saveRecording() {

        if (recording != null && recording.isAnimation()) {
            saveAnimation();
            return;
        }

        if (recording == null || !unsaved)
            return;

        String loop = recordingSourceBranch.findLoopIn(recording);

        if (loop != null) {
            setStatus(EditorSetting.RECORDER_STATUS_LOOP_REFUSED + loop);
            return;
        }

        File file = getRecordingFile(recording.getRecordingName());

        file.getParentFile().mkdirs();

        if (!recordingFileBranch.write(file, recording)) {
            setStatus(EditorSetting.RECORDER_STATUS_SAVE_FAILED + recording.getRecordingName());
            return;
        }

        recordingSourceBranch.forget(recording.getRecordingName());
        this.unsaved = false;
        revision++;
    }

    public void renameRecording(String recordingName) {

        if (recording == null || state.isSession() || refuseAnimation() || !isRecordingNameAvailable(recordingName))
            return;

        saveRecording();

        String previousName = recording.getRecordingName();

        if (!getRecordingFile(previousName).renameTo(getRecordingFile(recordingName))) {
            setStatus(EditorSetting.RECORDER_STATUS_RENAME_FAILED + previousName);
            return;
        }

        recordingSourceBranch.forget(previousName);
        recording.setRecordingName(recordingName);
        setStatus(EditorSetting.RECORDER_STATUS_RENAMED + recordingName);
    }

    // A copy of the open recording saved under the name and opened in its place
    public void duplicateRecording(String recordingName) {

        if (recording == null || state.isSession() || refuseAnimation() || !isRecordingNameAvailable(recordingName))
            return;

        saveRecording();

        String sourceName = recording.getRecordingName();
        RecordingInstance copy = recordingFileBranch.read(getRecordingFile(sourceName), recordingName);

        if (copy == null) {
            setStatus(EditorSetting.RECORDER_STATUS_UNREADABLE + sourceName);
            return;
        }

        closeRecording();
        clearParents();
        activateRecording(copy);
        markUnsaved();
        saveRecording();
        setStatus(EditorSetting.RECORDER_STATUS_DUPLICATED + recordingName);
    }

    public void deleteRecording() {

        if (recording == null || refuseAnimation())
            return;

        stop();

        String recordingName = recording.getRecordingName();
        File file = getRecordingFile(recordingName);

        if (file.exists() && !file.delete()) {
            setStatus(EditorSetting.RECORDER_STATUS_DELETE_FAILED + recordingName);
            return;
        }

        recordingSourceBranch.forget(recordingName);
        dropRecording();
        setStatus(EditorSetting.RECORDER_STATUS_DELETED + recordingName);
    }

    // The open recording let go with nothing open in its place
    private void dropRecording() {
        clearParents();
        this.recording = null;
        this.unsaved = false;
        recordingHistoryBranch.reset(null);
        unlaidTracks.clear();
        cancelDrag();
        clearSelection();
        transition.clear();
        rebuildNodes();
    }

    private boolean isOpen(String recordingName) {
        return recording != null && recording.getRecordingName().equals(recordingName);
    }

    // Every recording the open one was opened inside let go
    private void clearParents() {
        parentNames.clear();
    }

    // Saves and lets go of the open recording, ending any session on it; an animation clip keeps its changes in
    // InfoManager, saved with the content
    private void closeRecording() {

        if (recording == null)
            return;

        stop();

        if (!recording.isAnimation())
            saveRecording();

        this.recording = null;
    }

    // The recording opened with a history of its own, its first clip selected so the Keys pane shows its keys at
    // once
    private void activateRecording(RecordingInstance activated) {

        adoptRecording(activated);
        this.unsaved = false;
        this.playhead = 0f;
        clearSelection();

        if (!activated.getClips().isEmpty())
            pickClip(0);

        fitView();
        rebuildNodes();
        recordingHistoryBranch.reset(activated);
        revision++;
    }

    // The recording made the open one, as opened or as a step of its history brings it back: bound to everything it
    // nests, with every take still without keys queued to be laid on them
    private void adoptRecording(RecordingInstance adopted) {

        recordingSourceBranch.bind(adopted);
        this.recording = adopted;
        cancelDrag();
        transition.clear();
        unlaidTracks.clear();
        layerRevision++;
        this.curvesLaid = false;
        this.saveWhenLaid = false;

        for (int i = 0; i < adopted.getTakes().size(); i++)
            queueCurves(adopted.getTakes().get(i));
    }

    // Animation \\

    // The animation clip opened on the timeline in place of the media file open, which is saved first
    public void openAnimation(String clipName) {

        if (isAnimationOpen(clipName))
            return;

        InfoEntryStruct entry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName, clipName);
        RecordingInstance opened = entry != null ? recordingAnimationBranch.parse(entry.getArpg(), clipName) : null;

        if (opened == null) {
            setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_UNREADABLE + clipName);
            return;
        }

        closeRecording();
        clearParents();
        activateRecording(opened);
        this.writtenRevision = documentRevision + curveRevision;
        setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_OPENED + clipName);
    }

    // A clip deleted while open on the timeline is let go
    private void closeAnimation(InfoEntryStruct entry) {

        if (!isAnimationOpen(entry.getEntryName()))
            return;

        stop();
        dropRecording();
        setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_CLOSED + entry.getEntryName());
    }

    // A change made to the open clip anywhere but here read back in place of what the timeline holds, as a step of
    // its history; a clip that cannot be read now keeps what the timeline holds and says why
    private void refreshAnimation(InfoEntryStruct entry) {

        if (recordingAnimationBranch.isWriting() || !isAnimationOpen(entry.getEntryName()))
            return;

        RecordingInstance refreshed = recordingAnimationBranch.parse(entry.getArpg(), entry.getEntryName());

        if (refreshed == null) {
            setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_UNREADABLE + recordingAnimationBranch.getRefusal());
            return;
        }

        replaceRecording(refreshed, statusText);
        markUnsaved();
        this.writtenRevision = documentRevision + curveRevision;
    }

    // The open clip written back on the frame its keys changed, and its clip laid again to run as long as they reach
    private void writeAnimation() {

        if (recording == null || !recording.isAnimation() || documentRevision + curveRevision == writtenRevision)
            return;

        this.writtenRevision = documentRevision + curveRevision;
        recordingAnimationBranch.write(recording);
        recordingAnimationBranch.fitSpan(recording);
        revision++;
    }

    private void saveAnimation() {

        writeAnimation();

        String clipName = recording.getAnimationName();

        setStatus(infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName)
                ? EditorSetting.RECORDER_STATUS_ANIMATION_SAVED + clipName
                : infoManager.getRefusal());
    }

    // A media file's own actions refused while an animation clip is open, saying where the clip is handled
    private boolean refuseAnimation() {

        if (recording == null || !recording.isAnimation())
            return false;

        setStatus(EditorSetting.RECORDER_STATUS_ANIMATION_CONTENT);
        return true;
    }

    // Every change the gesture of another window makes to the open clip, from now until it ends, is one step
    public void beginGesture() {
        recordingHistoryBranch.beginRow(EditorSetting.RECORDER_GESTURE_ROW_PREFIX + ++gestureCount);
    }

    public void endGesture() {
        recordingHistoryBranch.endRow();
    }

    // The gesture taken back whole, as though it was never made, leaving nothing to redo
    public void cancelGesture() {

        ArpgObjectStruct step = recordingHistoryBranch.cancelRow();

        if (step != null && recording != null)
            restore(step, statusText);
    }

    // The bone track the Keys tab shows picked by its bone, as the Animation Editor's bone selected is: the layer
    // that keys the bone picked on the timeline first when another is
    public void selectBoneLayer(String boneName) {

        if (recording == null || !recording.isAnimation())
            return;

        for (int c = 0; c < recording.getClips().size(); c++)
            if (holdsBone(recording.getClips().get(c).getTake(), boneName)) {

                if (c != selectedClip) {
                    pickClip(c);
                    updateKeyTracks();
                    revision++;
                }

                break;
            }

        for (int i = 0; i < keyTracks.size(); i++)
            if (keyTracks.get(i) instanceof RecordingBoneTrackStruct bone && bone.getBoneName().equals(boneName)) {

                if (keyLayer != i)
                    selectKeyLayer(i);

                return;
            }
    }

    private static boolean holdsBone(RecordingTakeStruct layer, String boneName) {

        for (int i = 0; i < layer.getBoneTracks().size(); i++)
            if (layer.getBoneTracks().get(i).getBoneName().equals(boneName))
                return true;

        return false;
    }

    // The Tracks tab turned to the animations or the media files, as working on either turns it, or by its switch
    public void showAnimationTracks(boolean shown) {

        if (shown == listingAnimations)
            return;

        this.listingAnimations = shown;
        revision++;
    }

    public void toggleAnimationTracks() {
        showAnimationTracks(!listingAnimations);
    }

    public boolean isListingAnimations() {
        return listingAnimations;
    }

    public boolean isAnimationOpen(String clipName) {
        return recording != null && recording.isAnimation() && recording.getAnimationName().equals(clipName);
    }

    // The bone whose track the Keys tab shows, null when it shows no bone
    public String getSelectedBoneName() {
        return selectedTrack instanceof RecordingBoneTrackStruct bone ? bone.getBoneName() : null;
    }

    // Takes \\

    public void selectTake(int takeIndex) {

        if (!isTakeIndex(takeIndex))
            return;

        this.selectedTake = takeIndex;
        revision++;
    }

    // The take picked in the Layers tab's Takes view, -1 after saying none is
    public int requireSelectedTake() {

        if (isTakeIndex(selectedTake))
            return selectedTake;

        setStatus(EditorSetting.RECORDER_STATUS_NO_TAKE);
        return EngineSetting.INDEX_NOT_FOUND;
    }

    public void renameTake(int takeIndex, String takeName) {

        if (!isTakeIndex(takeIndex) || !isLabelValid(takeName))
            return;

        recording.getTakes().get(takeIndex).setName(takeName);
        markUnsaved();
    }

    // The take leaves the bin and every clip of it the timeline
    public void deleteTake(int takeIndex) {

        if (!isTakeIndex(takeIndex) || !canArrangeTimeline())
            return;

        String takeName = recording.getTakes().get(takeIndex).getName();

        recording.removeTake(takeIndex);
        clearSelection();
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_DELETED + takeName);
    }

    // The whole take laid from the playhead, on the lowest clear lane
    public void placeTakeAtPlayhead(int takeIndex) {
        placeTake(takeIndex, playhead, 0);
    }

    public void placeTake(int takeIndex, float start, int lane) {

        if (!isTakeIndex(takeIndex) || !canArrangeTimeline())
            return;

        RecordingTakeStruct placed = recording.getTakes().get(takeIndex);

        pickClip(recordingTimelineBranch.placeTake(recording, placed, start, lane));
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_PLACED + placed.getName());
    }

    // Sounds \\

    // The sound laid from the playhead, on the lowest clear lane
    public void placeSoundAtPlayhead(int soundID) {
        placeSound(soundID, playhead, 0);
    }

    // The sound laid on the timeline as a take holding it alone, the take it was given before reused so the bin holds
    // each sound once
    public void placeSound(int soundID, float start, int lane) {

        if (recording == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_RECORDING_FOR_SOUND);
            return;
        }

        if (!canArrangeTimeline())
            return;

        placeTake(recordingSoundBranch.requireSoundTake(recording, soundManager.getSoundHandleFromSoundID(soundID)),
                start, lane);
    }

    // Clips \\

    public void selectClip(int clipIndex) {

        if (!isClipIndex(clipIndex))
            return;

        if (clipIndex != selectedNodeClip)
            clearNodeSelection();

        if (selectedTransition != EngineSetting.INDEX_NOT_FOUND
                && recordingTransitionBranch.toClipIndex(selectedTransition) != clipIndex)
            clearTransitionSelection();

        focusClip(clipIndex);
        revision++;
    }

    // The clip selected with none of its nodes or transitions, as a click on it away from them selects it, and no
    // other clip
    private void selectClipAlone(int clipIndex) {
        clearNodeSelection();
        clearTransitionSelection();
        pickClip(clipIndex);
        selectClip(clipIndex);
    }

    // A clip clicked with Shift joins the clips selected, or leaves them when it was among them already
    private void toggleClipSelection(int clipIndex) {

        if (!isClipIndex(clipIndex))
            return;

        int at = selectedClips.indexOf(clipIndex);

        clearNodeSelection();
        clearTransitionSelection();

        if (at == EngineSetting.INDEX_NOT_FOUND) {
            selectedClips.add(clipIndex);
            focusClip(clipIndex);
        } else {
            selectedClips.removeInt(at);
            this.selectedClip = selectedClips.isEmpty()
                    ? EngineSetting.INDEX_NOT_FOUND
                    : selectedClips.getInt(selectedClips.size() - 1);
        }

        revision++;
    }

    // The clip selected alone, its take the one the bin shows
    private void pickClip(int clipIndex) {
        selectedClips.clear();
        selectedClips.add(clipIndex);
        focusClip(clipIndex);
    }

    // The clip made the one picked last, kept among those selected, or selected alone when it was not among them
    private void focusClip(int clipIndex) {

        if (!selectedClips.contains(clipIndex)) {
            selectedClips.clear();
            selectedClips.add(clipIndex);
        }

        this.selectedClip = clipIndex;
        this.selectedLane = recording.getClips().get(clipIndex).getLane();
        this.selectedTake = recording.getTakes().indexOf(recording.getClips().get(clipIndex).getTake());
    }

    // A clip clicked twice in quick succession opens the recording it nests; true when it did
    private boolean openOnSecondClick(int clipIndex) {

        long now = internal.getTime();
        boolean second = clipIndex == clickedClip && now - clickedMillis <= EditorSetting.RECORDER_DOUBLE_CLICK_MILLIS;

        this.clickedClip = second ? EngineSetting.INDEX_NOT_FOUND : clipIndex;
        this.clickedMillis = now;

        return second && openNested(clipIndex);
    }

    // The clip's start or end dragged to the moment, as far as its take and the clips beside it allow
    public void trimClipTo(int clipIndex, float time, boolean start) {

        if (!isClipIndex(clipIndex) || !canArrangeTimeline())
            return;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);

        if (start)
            recordingTimelineBranch.trimStartTo(clip, recordingTimelineBranch.resolveTrimStart(recording, clip, time));
        else
            recordingTimelineBranch.trimEndTo(clip, recordingTimelineBranch.resolveTrimEnd(recording, clip, time));

        fitNoteClip(clip);
        focusClip(clipIndex);
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRIMMED + clip.getSourceName());
    }

    // The clip moved to the moment and lane, and every other clip selected with it by as far, each landing on the
    // nearest clear lane to the one it is carried to
    public void moveClip(int clipIndex, float start, int lane) {

        if (!isClipIndex(clipIndex) || !canArrangeTimeline())
            return;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);
        float shift = start - clip.getStart();
        int rise = lane - clip.getLane();

        recordingTimelineBranch.moveClip(recording, clip, start, lane);

        if (selectedClips.contains(clipIndex))
            for (int i = 0; i < selectedClips.size(); i++) {

                RecordingClipStruct carried = recording.getClips().get(selectedClips.getInt(i));

                if (carried != clip)
                    recordingTimelineBranch.moveClip(recording, carried, carried.getStart() + shift,
                            carried.getLane() + rise);
            }

        focusClip(clipIndex);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_MOVED + clip.getSourceName());
    }

    // The selected clip plays the channel or stops playing it; only a channel its source can play can be given
    public void toggleClipChannel(RecorderChannel channel) {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null || !channel.isIn(clip.resolveSourceChannelMask()))
            return;

        clip.setChannelMask(clip.getStoredChannelMask() ^ channel.getBit());
        markUnsaved();
    }

    public void toggleClipMute() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        clip.setMuted(!clip.isMuted());
        markUnsaved();
    }

    // Steps the clip's clock jumps through each ramp in turn, back to a cut after the longest
    // The clip's time ramp stepped to the one before or after it, round from the last to the first
    public void cycleClipTimeRamp(int direction) {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        float[] ramps = EditorSetting.RECORDER_TIME_RAMP_SECONDS;
        int index = 0;

        while (index < ramps.length && ramps[index] != clip.getTimeRampSeconds())
            index++;

        clip.setTimeRampSeconds(ramps[Math.floorMod(index + direction, ramps.length)]);
        markUnsaved();
    }

    // The selected clip cut in two at the playhead
    public void splitClip() {
        if (requireSelectedClip() != null && !cutClip(selectedClip, playhead))
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
    }

    // The one way a clip is cut in two; the piece after the cut is selected, and false when the moment is too near
    // either end of the clip to cut there
    private boolean cutClip(int clipIndex, float time) {

        if (!isClipIndex(clipIndex) || !canArrangeTimeline())
            return false;

        RecordingClipStruct clip = recording.getClips().get(clipIndex);
        int cut = recordingTimelineBranch.splitClip(recording, clip, time);

        if (cut == EngineSetting.INDEX_NOT_FOUND)
            return false;

        fitNoteClip(clip);
        fitNoteClip(recording.getClips().get(cut));
        clearNodeSelection();
        pickClip(cut);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_CUT + clip.getSourceName());
        return true;
    }

    // Cut \\

    // Where the cut tool would cut with the pointer where it is now, nowhere when it is over no clip
    public void hoverCut(float time, int lane, float secondsPerPixel, boolean snapping) {

        int clipIndex = isCutting() && canArrangeTimeline()
                ? recording.findClipAt(time, lane)
                : EngineSetting.INDEX_NOT_FOUND;

        this.cutHoverClip = clipIndex;

        if (clipIndex != EngineSetting.INDEX_NOT_FOUND)
            this.cutHoverTime = recordingTimelineBranch.resolveCut(recording, recording.getClips().get(clipIndex),
                    time, playhead, snapping ? EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel : 0f);
    }

    public void clearCutHover() {
        this.cutHoverClip = EngineSetting.INDEX_NOT_FOUND;
    }

    public void trimClipStart() {
        trimClip(true);
    }

    public void trimClipEnd() {
        trimClip(false);
    }

    private void trimClip(boolean start) {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        boolean trimmed = start
                ? recordingTimelineBranch.trimStart(clip, playhead)
                : recordingTimelineBranch.trimEnd(clip, playhead);

        if (!trimmed) {
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
            return;
        }

        fitNoteClip(clip);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRIMMED + clip.getSourceName());
    }

    // A copy of the selected clip laid from the playhead
    public void stampClip() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        pickClip(recordingTimelineBranch.stampClip(recording, clip, playhead));
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_STAMPED + clip.getSourceName());
    }

    // Every selected clip leaves the timeline as one edit; their takes stay in the bin
    public void removeClip() {

        RecordingClipStruct clip = requireSelectedClip();

        if (clip == null)
            return;

        removeSelectedClips();
        this.playhead = Math.min(playhead, recording.getDuration());
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_CLIP_REMOVED + clip.getSourceName());
    }

    // Every selected clip taken off the timeline, the last laid first so the places of the rest hold
    private void removeSelectedClips() {

        sortSelectedClips();

        for (int i = selectedClips.size() - 1; i >= 0; i--)
            recording.removeClip(selectedClips.getInt(i));

        clearSelection();
    }

    // The selected clips in the order they were laid, so a copy lays them as they were
    private void sortSelectedClips() {
        IntArrays.unstableSort(selectedClips.elements(), 0, selectedClips.size());
    }

    // Nesting \\

    // The media file the clip nests opened in place of the open one, which it is opened inside, so Back returns to
    // it; true when it was opened
    public boolean openNested(int clipIndex) {

        if (!isClipIndex(clipIndex) || state.isSession())
            return false;

        String nestedName = recording.getClips().get(clipIndex).getNestedName();

        if (nestedName == null)
            return false;

        String parentName = recording.getRecordingName();

        saveRecording();

        if (!switchRecording(nestedName))
            return false;

        parentNames.add(parentName);
        revision++;
        return true;
    }

    // The recording the open one was opened inside opened again, nesting it as it now stands
    public void backToParent() {

        if (parentNames.isEmpty() || state.isSession())
            return;

        String parentName = parentNames.pop();

        saveRecording();
        switchRecording(parentName);
        revision++;
    }

    // The selected clips moved into a new recording of the name, laid where they lay, and that recording laid
    // where they were as one clip nesting it: Premiere's Nest, After Effects' Pre-compose
    public void nestSelection(String recordingName) {

        RecordingClipStruct focused = requireSelectedClip();

        if (focused == null)
            return;

        if (!isRecordingNameAvailable(recordingName)) {
            setStatus(EditorSetting.RECORDER_STATUS_NAME_TAKEN + recordingName);
            return;
        }

        float start = Float.MAX_VALUE;
        int lane = recording.getLaneCount();

        for (int i = 0; i < selectedClips.size(); i++) {

            RecordingClipStruct clip = recording.getClips().get(selectedClips.getInt(i));

            start = Math.min(start, clip.getStart());
            lane = Math.min(lane, clip.getLane());
        }

        mediaDirectory.mkdirs();

        if (!recordingFileBranch.write(getRecordingFile(recordingName),
                collectSelection(recordingName, start, lane))) {
            setStatus(EditorSetting.RECORDER_STATUS_SAVE_FAILED + recordingName);
            return;
        }

        removeSelectedClips();
        layRecording(recordingName, start, lane);
        setStatus(EditorSetting.RECORDER_STATUS_NESTED + recordingName);
    }

    // A new recording holding a copy of every selected clip, laid from its own start and lowest lane as they lay
    // against each other, with every take they play in its bin
    private RecordingInstance collectSelection(String recordingName, float start, int lane) {

        RecordingInstance nested = create(RecordingInstance.class);

        nested.constructor(
                recordingName,
                recording.getWorldName(),
                recording.getTempo(),
                recording.getBeatsPerBar(),
                recording.getBeatUnit(),
                recording.getKeyRoot(),
                recording.getKeyScale(),
                new RecordingExportStruct());

        sortSelectedClips();

        for (int i = 0; i < selectedClips.size(); i++) {

            RecordingClipStruct clip = recording.getClips().get(selectedClips.getInt(i));
            RecordingTakeStruct clipTake = clip.getTake();

            if (clipTake != null && !nested.getTakes().contains(clipTake))
                nested.addTake(clipTake);

            nested.addClip(clip.copyTo(clip.getLane() - lane, clip.getStart() - start));
        }

        return nested;
    }

    // Laying a recording \\

    // A saved recording picked up from the Recordings tab, to be laid where it is let go over the lanes, or opened
    // when it is let go where it was pressed
    public void beginRecordingDrag(String recordingName, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || state == RecorderState.RECORDING || state == RecorderState.EXPORTING)
            return;

        beginDrag(RecorderDragKind.RECORDING, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, 0f, 0f);
        drag.carry(recordingName);
    }

    // A note of an instrument picked up from the Note Library, to be laid where it is let go over the lanes as a note
    // clip a beat long; nothing is picked up while no recording is open to lay it on
    public void beginNoteDrag(String instrumentName, int pitch, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !ensureRecording() || !canArrangeTimeline())
            return;

        beginDrag(RecorderDragKind.NOTE, pitch, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, 0f, 0f);
        drag.carry(instrumentName);
    }

    // The saved recording laid inside the open one from the moment, on the lane asked for or the nearest clear one;
    // refused when it would nest the open recording in itself, at any depth, or shows a picture of another world
    public void placeRecording(String recordingName, float start, int lane) {

        if (!canArrangeTimeline())
            return;

        if (layRecording(recordingName, start, lane))
            setStatus(EditorSetting.RECORDER_STATUS_PLACED + recordingName);
    }

    // The one way a nested recording is laid; true when it was, after saying why not when it was not
    private boolean layRecording(String recordingName, float start, int lane) {

        RecordingInstance child = recordingSourceBranch.requireChild(recordingName);

        if (child == null) {
            setStatus(EditorSetting.RECORDER_STATUS_UNREADABLE + recordingName);
            return false;
        }

        String loop = recordingSourceBranch.findLoopLaying(recording, child);

        if (loop != null) {
            setStatus(EditorSetting.RECORDER_STATUS_LOOP_REFUSED + loop);
            return false;
        }

        if (!recordingSourceBranch.isWorldShared(recording, child)) {
            setStatus(EditorSetting.RECORDER_STATUS_OTHER_WORLD + child.getWorldName());
            return false;
        }

        RecordingClipStruct clip = RecordingClipStruct.ofRecording(
                recordingName, 0f, child.getDuration(), lane, start, RecorderChannel.ALL);

        clip.bindChild(child);
        pickClip(recordingTimelineBranch.placeClip(recording, clip));
        markUnsaved();
        return true;
    }

    // The selected clip when the timeline can be edited, null after saying why not
    private RecordingClipStruct requireSelectedClip() {

        if (!canArrangeTimeline())
            return null;

        if (!isClipIndex(selectedClip)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CLIP);
            return null;
        }

        return recording.getClips().get(selectedClip);
    }

    // Nothing on the timeline changes under a take or an export
    // The media file a take or a library's drag lays into: the one open, or a new one made for it
    private boolean ensureRecording() {
        return recording != null || createRecording(resolveDefaultRecordingName());
    }

    private boolean canEditTimeline() {
        return recording != null && state != RecorderState.RECORDING && state != RecorderState.EXPORTING;
    }

    // Clips laid, moved, cut and joined, and everything they carry, are for a media file: an animation clip's one
    // clip only shows its keys
    private boolean canArrangeTimeline() {
        return canEditTimeline() && !recording.isAnimation();
    }

    // Drag \\

    // A take picked up from the Layers tab's Takes view, to be laid where it is let go over the lanes, or picked when
    // it is let go where it was pressed
    public void beginTakeDrag(int takeIndex, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !canArrangeTimeline() || !isTakeIndex(takeIndex))
            return;

        beginDrag(RecorderDragKind.TAKE, takeIndex, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, 0f, 0f);
    }

    // A sound picked up from the sound library in any window, to be laid where it is let go over the lanes; nothing
    // is picked up while no recording is open to lay it on
    public void beginSoundDrag(int soundID, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !ensureRecording() || !canArrangeTimeline())
            return;

        beginDrag(RecorderDragKind.SOUND, soundID, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, 0f, 0f);
    }

    // A transition picked up from the transitions tool in any window, to be laid on the clip edge it is let go
    // over; nothing is picked up while no recording is open to lay it on
    public void beginTransitionDrag(int transitionID, WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || !canArrangeTimeline())
            return;

        beginDrag(RecorderDragKind.TRANSITION, transitionID, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, 0f, 0f);
    }

    // What a press on the lanes takes hold of, read once where it began: a clip's edge to trim it, a node's notch or a
    // gesture to retime or move it, a clip to move it — with Shift, to add it to the clips selected or take it from
    // them should the press not travel — or the empty lanes to scroll the view; the timeline can only be scrolled
    // under a take or an export
    public void pressLanes(
            float time,
            int lane,
            boolean nameRow,
            boolean extending,
            float secondsPerPixel,
            WindowInstance window,
            float pressX,
            float pressY) {

        if (drag.isActive() || recording == null)
            return;

        if (recording.isAnimation()) {
            pressAnimationLayer(window, pressX, pressY, time, lane);
            return;
        }

        int clipIndex = canArrangeTimeline() ? recording.findClipAt(time, lane) : EngineSetting.INDEX_NOT_FOUND;

        if (isCutting() && clipIndex != EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.CUT, clipIndex, EngineSetting.INDEX_NOT_FOUND,
                    window, pressX, pressY, time, 0f);
            landCut(time, EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel);
            return;
        }

        int startEdge = canArrangeTimeline() ? recordingTimelineBranch.findStartEdge(
                recording, time, lane, EditorSetting.RECORDER_EDGE_GRAB_PIXELS * secondsPerPixel)
                : EngineSetting.INDEX_NOT_FOUND;
        int endEdge = canArrangeTimeline() ? recordingTimelineBranch.findEndEdge(
                recording, time, lane, EditorSetting.RECORDER_EDGE_GRAB_PIXELS * secondsPerPixel)
                : EngineSetting.INDEX_NOT_FOUND;

        if (startEdge != EngineSetting.INDEX_NOT_FOUND || endEdge != EngineSetting.INDEX_NOT_FOUND) {
            beginTrimDrag(time, startEdge, endEdge, clipIndex, window, pressX, pressY);
            return;
        }

        int transitionTarget = canArrangeTimeline()
                ? recordingTransitionBranch.findTransitionAt(recording, time, lane, 0f)
                : EngineSetting.INDEX_NOT_FOUND;

        if (transitionTarget != EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.TRANSITION_LENGTH, recordingTransitionBranch.toClipIndex(transitionTarget),
                    EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, time, 0f);
            drag.aim(transitionTarget);
            return;
        }

        if (clipIndex == EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.PAN, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                    window, pressX, pressY, time, 0f);
            return;
        }

        float notchTolerance = EditorSetting.RECORDER_NOTCH_GRAB_PIXELS * secondsPerPixel;
        RecordingTakeStruct clipTake = recording.getClips().get(clipIndex).getTake();
        int nodeIndex = clipTake != null
                ? recordingNodeBranch.findNodeAt(nodes, clipIndex, clipTake.getPlayerTrack(), time, nameRow,
                        notchTolerance)
                : EngineSetting.INDEX_NOT_FOUND;

        if (nodeIndex != EngineSetting.INDEX_NOT_FOUND && isNodeGrabbed(nodes.get(nodeIndex), time, notchTolerance)) {
            RecordingNodeStruct node = nodes.get(nodeIndex);
            beginDrag(RecorderDragKind.NODE, clipIndex, nodeIndex, window, pressX, pressY, time,
                    node.getKind() == RecorderNodeKind.GESTURE ? time - node.getStart() : 0f);
            return;
        }

        beginDrag(RecorderDragKind.CLIP, clipIndex, nodeIndex, window, pressX, pressY, time,
                time - recording.getClips().get(clipIndex).getStart());
        drag.anchorSelection(extending, false, selectedClips.contains(clipIndex));
    }

    // A press on an animation clip's lanes picks the layer under it, its bones then the ones the Layers and Keys tabs
    // show, and moves the playhead there once let go where it was pressed
    private void pressAnimationLayer(WindowInstance window, float pressX, float pressY, float time, int lane) {

        int layerClip = recording.findClipAt(time, lane);

        if (layerClip != EngineSetting.INDEX_NOT_FOUND && layerClip != selectedClip) {
            pickClip(layerClip);
            revision++;
        }

        beginDrag(RecorderDragKind.PAN, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, time, 0f);
    }

    // The pan button pressed over the lanes or the ruler takes hold of the view, whatever lies under the press, and
    // drags it along under the cursor until let go
    public void pressViewScroll(WindowInstance window, float pressX, float pressY) {

        if (drag.isActive() || recording == null)
            return;

        beginDrag(RecorderDragKind.SCROLL, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, 0f, 0f);
    }

    // The edge of the clip under the press when it has one there, else the nearer of the two edges found
    private void beginTrimDrag(
            float time,
            int startEdge,
            int endEdge,
            int clipIndex,
            WindowInstance window,
            float pressX,
            float pressY) {

        boolean start;

        if (clipIndex != EngineSetting.INDEX_NOT_FOUND && (startEdge == clipIndex) != (endEdge == clipIndex))
            start = startEdge == clipIndex;
        else if (startEdge == EngineSetting.INDEX_NOT_FOUND || endEdge == EngineSetting.INDEX_NOT_FOUND)
            start = startEdge != EngineSetting.INDEX_NOT_FOUND;
        else
            start = Math.abs(recording.getClips().get(startEdge).getStart() - time)
                    <= Math.abs(recording.getClips().get(endEdge).getEnd() - time);

        beginDrag(start ? RecorderDragKind.TRIM_START : RecorderDragKind.TRIM_END, start ? startEdge : endEdge,
                EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, time, 0f);
    }

    // A gesture is taken hold of anywhere along it, a movement only at the notch it opens with
    private boolean isNodeGrabbed(RecordingNodeStruct node, float time, float notchTolerance) {
        return node.isMovable() && (node.getKind() == RecorderNodeKind.GESTURE
                || recordingNodeBranch.isOnNotch(node, time, notchTolerance));
    }

    private void beginDrag(
            RecorderDragKind kind,
            int index,
            int nodeIndex,
            WindowInstance window,
            float pressX,
            float pressY,
            float pressTime,
            float grabOffset) {

        drag.begin(kind, index, nodeIndex, window, pressX, pressY, pressTime, grabOffset, view.getStart());
        drag.land(false, pressTime, EngineSetting.INDEX_NOT_FOUND);
        revision++;
    }

    // Where what is carried would land with the cursor where it is now, snapped to the timeline's edges when asked;
    // a scroll moves the view so the moment pressed stays under the cursor
    public void dragTo(
            float x,
            float y,
            boolean overLanes,
            float time,
            int lane,
            float secondsPerPixel,
            boolean snapping) {

        if (!drag.isActive())
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);

        float tolerance = snapping ? EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel : 0f;

        switch (drag.getKind()) {
            case TAKE, SOUND, RECORDING, NOTE, CLIP -> landSpan(overLanes, time, lane, tolerance);
            case TRIM_START, TRIM_END -> landEdge(time, tolerance);
            case CUT -> landCut(time, tolerance);
            case NODE -> landNode(time);
            case PAN, SCROLL -> panView(x, secondsPerPixel);
            case TRANSITION -> landTransition(overLanes, time, lane,
                    EditorSetting.RECORDER_EDGE_GRAB_PIXELS * secondsPerPixel);
            case TRANSITION_LENGTH -> landTransitionLength(time, tolerance);
            case KEY, GRAPH_KEY, HANDLE_IN, HANDLE_OUT, KEYS_PRESS, GRAPH_PRESS, KEYS_BOX, GRAPH_BOX, SOUND_EVENT,
                    NONE -> {
            }
        }
    }

    // A sound from the sound library carried over the Keys pane's rows, to be added to the take shown there at the
    // moment under the cursor
    public void dragSoundOverKeys(float x, float y, float time) {

        if (drag.getKind() != RecorderDragKind.SOUND)
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);
        drag.landOnKeys(time);
    }

    // The span lands on the lane it would really be laid on, which the tab shows before it is let go
    private void landSpan(boolean overLanes, float time, int lane, float tolerance) {

        if (recording == null) {
            drag.land(false, time, lane);
            return;
        }

        RecordingClipStruct ignored = drag.getKind() == RecorderDragKind.CLIP
                ? recording.getClips().get(drag.getIndex())
                : null;
        float length = resolveDragLength();
        float start = Math.max(0f, recordingTimelineBranch.snapSpan(
                recording, Math.max(0f, time - drag.getGrabOffset()), length, playhead, tolerance, ignored));

        drag.land(overLanes, start,
                recordingTimelineBranch.resolveLane(recording, start, start + length, lane, ignored));
    }

    private void landEdge(float time, float tolerance) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());
        float snapped = recordingTimelineBranch.snapTime(recording, time, playhead, tolerance, clip);

        drag.land(true, drag.getKind() == RecorderDragKind.TRIM_START
                ? recordingTimelineBranch.resolveTrimStart(recording, clip, snapped)
                : recordingTimelineBranch.resolveTrimEnd(recording, clip, snapped),
                clip.getLane());
    }

    private void landCut(float time, float tolerance) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());

        drag.land(true, recordingTimelineBranch.resolveCut(recording, clip, time, playhead, tolerance), clip.getLane());
    }

    private void landNode(float time) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());

        drag.land(true,
                recordingNodeBranch.resolveLanding(nodes.get(drag.getNode()), clip, time - drag.getGrabOffset()),
                clip.getLane());
    }

    // A transition lands on the clip edge under the cursor, shown as a line at that edge; over no clip it lands
    // nowhere
    private void landTransition(boolean overLanes, float time, int lane, float tolerance) {

        int target = overLanes && canArrangeTimeline()
                ? recordingTransitionBranch.findDropTarget(recording, time, lane, tolerance)
                : EngineSetting.INDEX_NOT_FOUND;

        drag.aim(target);

        if (target == EngineSetting.INDEX_NOT_FOUND) {
            drag.land(false, time, lane);
            return;
        }

        RecordingClipStruct clip = recording.getClips().get(recordingTransitionBranch.toClipIndex(target));

        drag.land(true, recordingTransitionBranch.resolveEdgeTime(clip, recordingTransitionBranch.toEdge(target)),
                clip.getLane());
    }

    // The far side of a transition dragged to the moment, snapped as an edge is and held within its clip
    private void landTransitionLength(float time, float tolerance) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());
        RecorderClipEdge edge = recordingTransitionBranch.toEdge(drag.getTarget());
        float snapped = recordingTimelineBranch.snapTime(recording, time, playhead, tolerance, clip);

        drag.land(true, recordingTransitionBranch.resolveFarSide(clip, edge,
                recordingTransitionBranch.resolveSecondsTo(clip, edge, snapped)), clip.getLane());
    }

    private void panView(float x, float secondsPerPixel) {
        if (drag.isMoved() && view.scrollTo(
                drag.getViewStart() - (x - drag.getPressX()) * secondsPerPixel, resolveTimelineSpan()))
            viewRevision++;
    }

    // Letting go lays, moves, trims or retimes what was carried where it landed; a press that never travelled
    // selects what it was on instead, or moves the playhead to an empty moment
    public void endDrag() {

        if (!drag.isActive())
            return;

        RecorderDragKind kind = drag.getKind();
        int index = drag.getIndex();
        int nodeIndex = drag.getNode();
        boolean moved = drag.isMoved();
        boolean dropped = moved && drag.isOverLanes();
        boolean droppedOnKeys = drag.isOverKeys();
        float pressTime = drag.getPressTime();
        float landingStart = drag.getLandingStart();
        int landingLane = drag.getLandingLane();
        boolean extending = drag.isExtending();
        boolean subtracting = drag.isSubtracting();
        boolean keySelected = drag.isKeySelected();
        float reachTime = drag.getReachTime();
        double pressValue = drag.getPressValue();
        double reachValue = drag.getReachValue();
        int target = drag.getTarget();
        String carriedName = drag.getName();

        cancelDrag();

        switch (kind) {
            case TAKE -> {
                if (dropped)
                    placeTake(index, landingStart, landingLane);
                else
                    selectTake(index);
            }
            case RECORDING -> {
                if (dropped)
                    placeRecording(carriedName, landingStart, landingLane);
                else if (!moved)
                    openRecording(carriedName);
            }
            case NOTE -> {
                if (dropped)
                    placeNote(carriedName, index, landingStart, landingLane);
            }
            case SOUND -> {
                if (dropped)
                    placeSound(index, landingStart, landingLane);
                else if (moved && droppedOnKeys)
                    addSound(index, landingStart);
            }
            case SOUND_EVENT -> {
                if (moved)
                    moveSound(nodeIndex, landingStart);
            }
            case TRANSITION -> {
                if (moved && target != EngineSetting.INDEX_NOT_FOUND)
                    placeTransition(index, target);
            }
            case TRANSITION_LENGTH -> {
                if (moved)
                    stretchTransition(target, landingStart);
                else
                    selectTransition(target);
            }
            case CLIP -> {
                if (dropped)
                    moveClip(index, landingStart, landingLane);
                else if (extending)
                    toggleClipSelection(index);
                else if (nodeIndex != EngineSetting.INDEX_NOT_FOUND)
                    selectNode(nodeIndex);
                else if (!openOnSecondClick(index))
                    selectClipAlone(index);
            }
            case CUT -> {
                if (!cutClip(index, landingStart))
                    setStatus(EditorSetting.RECORDER_STATUS_CUT_TOO_NEAR);
            }
            case TRIM_START, TRIM_END -> {
                if (moved)
                    trimClipTo(index, landingStart, kind == RecorderDragKind.TRIM_START);
                else
                    selectClipAlone(index);
            }
            case NODE -> {
                if (moved)
                    moveNode(nodeIndex, landingStart);
                else
                    selectNode(nodeIndex);
            }
            case PAN -> {
                if (!moved)
                    seek(pressTime);
            }
            case SCROLL -> {
            }
            case KEY, GRAPH_KEY -> {
                if (moved && !subtracting)
                    finishKeyEdit(selectedKeys.size() == 1
                            ? EditorSetting.RECORDER_STATUS_KEY_MOVED
                            : EditorSetting.RECORDER_STATUS_KEYS_MOVED);
                else if (!moved)
                    pickKey(index, extending, subtracting, keySelected);
            }
            case HANDLE_IN, HANDLE_OUT -> {
                if (moved)
                    finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_MOVED);
                else
                    selectKey(index);
            }
            case KEYS_PRESS, GRAPH_PRESS -> {
                if (!moved && nodeIndex != EngineSetting.INDEX_NOT_FOUND)
                    selectNode(nodeIndex);
                else if (!moved)
                    releaseKeyAt(pressTime);
            }
            case KEYS_BOX, GRAPH_BOX -> {
                if (moved && kind == RecorderDragKind.GRAPH_BOX)
                    boxKeys(pressTime, reachTime, Math.min(pressValue, reachValue),
                            Math.max(pressValue, reachValue), subtracting);
                else if (moved)
                    boxKeys(pressTime, reachTime, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, subtracting);
                else
                    seek(pressTime);
            }
            case NONE -> {
            }
        }
    }

    public void cancelDrag() {

        if (!drag.isActive())
            return;

        drag.clear();
        revision++;
    }

    // How long what is carried runs, so the tab can draw where it would land; nothing for what lands at a moment
    public float resolveDragLength() {
        return switch (drag.getKind()) {
            case TAKE -> recording.getTakes().get(drag.getIndex()).getDuration();
            case SOUND -> soundManager.getSoundHandleFromSoundID(drag.getIndex()).getPlayedLength();
            case RECORDING -> resolveRecordingLength(drag.getName());
            case NOTE -> (float) recording.getTempo().toSeconds(recording.getBeatTicks());
            case CLIP -> recording.getClips().get(drag.getIndex()).getLength();
            case TRIM_START, TRIM_END, CUT, NODE, PAN, SCROLL, KEY, GRAPH_KEY, HANDLE_IN, HANDLE_OUT, KEYS_PRESS,
                    GRAPH_PRESS, KEYS_BOX, GRAPH_BOX, SOUND_EVENT, TRANSITION, TRANSITION_LENGTH, NONE -> 0f;
        };
    }

    // How long the saved recording of the name runs, nothing when it cannot be read
    private float resolveRecordingLength(String recordingName) {

        RecordingInstance child = recordingSourceBranch.requireChild(recordingName);

        return child != null ? child.getDuration() : 0f;
    }

    // Nodes \\

    // Selecting a node selects the clip it lies on, and lets every selected key and any transition go
    public void selectNode(int nodeIndex) {

        if (!isNodeIndex(nodeIndex))
            return;

        RecordingNodeStruct node = nodes.get(nodeIndex);

        selectClip(node.getClipIndex());
        anchorNode(node, node.getTakeStart());
        this.selectedNode = nodeIndex;
        clearTransitionSelection();
        clearKeySelection();
        releaseSound();
        keysRevision++;
    }

    public void nextNodeState() {
        stepNodeState(1);
    }

    public void previousNodeState() {
        stepNodeState(-1);
    }

    // The selected movement does the next or the previous movement state over its run instead
    private void stepNodeState(int step) {

        RecordingNodeStruct node = requireSelectedNode();

        if (node == null || node.getKind() != RecorderNodeKind.MOVEMENT)
            return;

        recordingNodeBranch.stepState(resolveNodeTrack(node), node, step);
        markUnsaved();

        if (isNodeIndex(selectedNode))
            setStatus(EditorSetting.RECORDER_STATUS_NODE_CHANGED + nodes.get(selectedNode).getLabel());
    }

    // A movement removed carries on what came before it; a gesture removed leaves the hands free
    public void removeNode() {

        RecordingNodeStruct node = requireSelectedNode();

        if (node == null)
            return;

        String label = node.getLabel();

        if (node.getKind() == RecorderNodeKind.GESTURE) {
            recordingNodeBranch.removeGesture(resolveNodeTrack(node), node);
            clearNodeSelection();
        } else if (!recordingNodeBranch.removeMovement(resolveNodeTrack(node), node)) {
            setStatus(EditorSetting.RECORDER_STATUS_ONLY_MOVEMENT);
            return;
        }

        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_NODE_REMOVED + label);
    }

    // A movement dragged opens at the moment instead; a gesture dragged plays from it instead
    public void moveNode(int nodeIndex, float time) {

        if (!isNodeIndex(nodeIndex) || !canArrangeTimeline())
            return;

        RecordingNodeStruct node = nodes.get(nodeIndex);

        if (!node.isMovable())
            return;

        RecordingClipStruct clip = recording.getClips().get(node.getClipIndex());
        float takeTime = clip.toChildTime(recordingNodeBranch.resolveLanding(node, clip, time));
        String label = node.getLabel();

        if (node.getKind() == RecorderNodeKind.MOVEMENT)
            recordingNodeBranch.retimeMovement(resolveNodeTrack(node), node, takeTime);
        else
            recordingNodeBranch.moveGesture(resolveNodeTrack(node), node, takeTime);

        focusClip(node.getClipIndex());
        anchorNode(node, takeTime);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_RETIMED + label);
    }

    // The selected node when the timeline can be edited, null after saying why not
    private RecordingNodeStruct requireSelectedNode() {

        if (!canArrangeTimeline())
            return null;

        if (!isNodeIndex(selectedNode)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_NODE);
            return null;
        }

        return nodes.get(selectedNode);
    }

    private RecordingBodyTrackStruct resolveNodeTrack(RecordingNodeStruct node) {
        return node.getTrack();
    }

    // The selection follows a node through rebuilds by its clip, whose it is, its kind and a moment of its run
    private void anchorNode(RecordingNodeStruct node, float takeTime) {
        this.selectedNodeClip = node.getClipIndex();
        this.selectedNodeTrack = node.getTrack();
        this.selectedNodeKind = node.getKind();
        this.selectedNodeTime = takeTime;
    }

    private void clearNodeSelection() {
        this.selectedNode = EngineSetting.INDEX_NOT_FOUND;
        this.selectedNodeClip = EngineSetting.INDEX_NOT_FOUND;
        this.selectedNodeTrack = null;
        this.selectedNodeKind = null;
    }

    // Every clip's nodes read again from the takes, the selected node found again among them
    private void rebuildNodes() {

        recordingNodeBranch.collectNodes(recording, nodes);

        this.selectedNode = selectedNodeKind != null
                ? recordingNodeBranch.findNode(
                        nodes, selectedNodeClip, selectedNodeTrack, selectedNodeKind, selectedNodeTime)
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // Music \\

    // A note clip laid from the moment on the lane: one note of the instrument at the pitch, a beat long, struck at
    // RECORDER_NOTE_VELOCITY; it is stretched and cut on the timeline like any clip, its note always filling it
    public void placeNote(String instrumentName, int pitch, float start, int lane) {

        if (!canArrangeTimeline())
            return;

        int startTick = toTick(start);
        float from = (float) recording.getTempo().toSeconds(startTick);
        float to = (float) recording.getTempo().toSeconds(startTick + recording.getBeatTicks());
        SongClipStruct note = createNoteClip(
                instrumentName,
                pitch,
                EditorSetting.RECORDER_NOTE_VELOCITY,
                recording.getBeatTicks(),
                EngineSetting.DEFAULT_SONG_CLIP_VOLUME,
                EngineSetting.DEFAULT_SONG_CLIP_TRANSPOSE);
        RecordingClipStruct clip = RecordingClipStruct.ofNotes(note, 0f, to - from, lane, from);

        recordingSourceBranch.bindClip(recording, clip);
        pickClip(recordingTimelineBranch.placeClip(recording, clip));
        soundRevision++;
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_NOTE_PLACED + clip.resolveLabelName());
    }

    // A note clip's note made to run its whole length again from its first tick, after its span changed: stretched,
    // it holds the note longer and fades on release where it ends; cut, each piece strikes a note of its own
    private void fitNoteClip(RecordingClipStruct clip) {

        if (!clip.isNoteClip())
            return;

        SongClipStruct note = clip.getSongClip();
        int length = Math.max(1, toTick(clip.getEnd()) - toTick(clip.getStart()));

        clip.setSpan(0f, clip.getLength());
        replaceSongClip(clip, createNoteClip(
                note.getInstrumentName(),
                note.getNotePitch(0),
                note.getNoteVelocity(0),
                length,
                note.getVolume(),
                note.getTranspose()));
    }

    // The one way a note clip's codec clip is made: a notes clip holding one note from its first tick to its end
    private SongClipStruct createNoteClip(
            String instrumentName,
            int pitch,
            int velocity,
            int length,
            float volume,
            int transpose) {

        int[] notes = new int[EngineSetting.SONG_NOTE_STRIDE];

        notes[EngineSetting.SONG_NOTE_START] = 0;
        notes[EngineSetting.SONG_NOTE_LENGTH] = length;
        notes[EngineSetting.SONG_NOTE_PITCH] = pitch;
        notes[EngineSetting.SONG_NOTE_VELOCITY] = velocity;

        return new SongClipStruct(
                SongClipKind.NOTES,
                0,
                0,
                length,
                EngineSetting.DEFAULT_SONG_CLIP_OFFSET,
                EngineSetting.DEFAULT_SONG_CLIP_MUTED,
                volume,
                transpose,
                instrumentName,
                notes,
                EngineSetting.SONG_REFERENCE_NONE,
                EngineSetting.SONG_REFERENCE_NONE);
    }

    // The clip's codec clip replaced whole by an edit — its notes, instrument, volume or transpose — the clip bound
    // again to play it and the timeline's voices stopped so it is heard as it now stands
    public void replaceSongClip(RecordingClipStruct clip, SongClipStruct songClip) {

        if (recording == null || !recording.getClips().contains(clip))
            return;

        clip.setSongClip(songClip);
        recordingSourceBranch.bindClip(recording, clip);
        soundRevision++;
        markUnsaved();
    }

    // The tick of the open recording's tempo map a moment falls on
    public int toTick(float time) {
        return (int) Math.round(recording.getTempo().toTicks(time));
    }

    // Tempo, Meter and Key \\

    // The tempo the open recording starts at set, every later change kept; each clip timed by the tempo map keeps its
    // place in bars and beats, every other keeps its moment
    public void setTempo(float bpm) {

        if (!canArrangeTimeline())
            return;

        SongTempoStruct previous = recording.getTempo();
        int[] ticks = new int[previous.getChangeCount()];
        float[] bpms = new float[ticks.length];

        for (int i = 0; i < ticks.length; i++) {
            ticks[i] = previous.getTick(i);
            bpms[i] = previous.getBpm(i);
        }

        bpms[0] = Math.clamp(Math.round(bpm * EditorSetting.RECORDER_BPM_PRECISION)
                / EditorSetting.RECORDER_BPM_PRECISION, EngineSetting.SONG_BPM_MIN, EngineSetting.SONG_BPM_MAX);
        retime(previous, new SongTempoStruct(ticks, bpms));
    }

    private void retime(SongTempoStruct previous, SongTempoStruct next) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            double startTick = previous.toTicks(clip.getStart());
            float start = (float) next.toSeconds(startTick);

            if (clip.getSongClip() != null) {

                float origin = (float) next.toSeconds(previous.toTicks(clip.getOrigin()));
                float end = (float) next.toSeconds(previous.toTicks(clip.getEnd()));

                clip.setSpan(start - origin, end - origin);
            }

            clip.setStart(start);
        }

        this.playhead = (float) next.toSeconds(previous.toTicks(playhead));
        recording.setTempo(next);
        rebindMusic();
    }

    public void setMeter(int beatsPerBar, int beatUnit) {

        if (!canArrangeTimeline() || beatsPerBar < 1 || beatsPerBar > EngineSetting.SONG_BEATS_PER_BAR_MAX
                || beatUnit < 1 || beatUnit > EngineSetting.SONG_BEAT_UNIT_MAX || Integer.bitCount(beatUnit) != 1)
            return;

        recording.setMeter(beatsPerBar, beatUnit);
        rebindMusic();
    }

    public void setKey(int keyRoot, NoteScale keyScale) {

        if (recording == null)
            return;

        recording.setKey(Math.floorMod(keyRoot, EngineSetting.NOTE_SEMITONES_PER_OCTAVE), keyScale);
        markUnsaved();
    }

    // Every clip bound again after the tempo map or meter moves, and heard again as it now stands
    private void rebindMusic() {
        recordingSourceBranch.bind(recording);
        soundRevision++;
        viewRevision++;
        markUnsaved();
    }

    // Mixer \\

    // The lane whose mix the Properties tab shows
    public void selectLane(int lane) {

        if (recording == null || lane < 0 || lane >= recording.getLaneCount())
            return;

        this.selectedLane = lane;
        revision++;
    }

    public void setLaneName(int lane, String name) {
        if (isLaneIndex(lane)) {
            recording.getLane(lane).setName(name);
            markUnsaved();
        }
    }

    public void setLaneVolume(int lane, float volume) {
        if (isLaneIndex(lane)) {
            recording.getLane(lane).setVolume(volume);
            markUnsaved();
        }
    }

    public void setLanePan(int lane, float pan) {
        if (isLaneIndex(lane)) {
            recording.getLane(lane).setPan(pan);
            markUnsaved();
        }
    }

    public void toggleLaneMute(int lane) {
        if (isLaneIndex(lane)) {
            recording.getLane(lane).setMuted(!recording.getLane(lane).isMuted());
            markUnsaved();
        }
    }

    public void toggleLaneSolo(int lane) {
        if (isLaneIndex(lane)) {
            recording.getLane(lane).setSolo(!recording.getLane(lane).isSolo());
            markUnsaved();
        }
    }

    // The selected lane's name as the rename dialog opens on it
    public String formatLaneNameInput() {
        return recording.getLane(selectedLane).getName();
    }

    public boolean isLaneNameValid(String text) {
        return isLaneIndex(selectedLane);
    }

    public void setSelectedLaneName(String text) {
        setLaneName(selectedLane, text.trim());
    }

    private boolean isLaneIndex(int lane) {
        return recording != null && lane >= 0 && lane < recording.getLaneCount();
    }

    // Export \\

    // The open media file's music written as a Standard MIDI File among the player's songs; the sound clips MIDI
    // cannot hold are counted in the status
    public void exportMidi() {

        if (recording == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_RECORDING);
            return;
        }

        if (refuseAnimation())
            return;

        File file = new File(new File(internal.path, EngineSetting.SONG_USER_DIRECTORY),
                recording.getRecordingName() + EditorSetting.RECORDER_EXTENSION_SEPARATOR
                        + EngineSetting.SONG_USER_FILE_EXTENSION);

        file.getParentFile().mkdirs();

        try {
            int leftOut = SongMidiUtility.write(recordingMusicBranch.toSong(recording, true), file, instrumentManager);

            setStatus(leftOut == 0
                    ? EditorSetting.RECORDER_STATUS_MIDI_EXPORTED + file.getAbsolutePath()
                    : String.format(EditorSetting.RECORDER_STATUS_MIDI_EXPORTED_FORMAT, file.getAbsolutePath(),
                            leftOut));
        } catch (InternalException e) {
            setStatus(EditorSetting.RECORDER_STATUS_MIDI_FAILED + e.getMessage());
        }
    }

    // Music — a media file with no picture — exported as the game's own: its song written by the song codec and its
    // sound rendered offline beside it, under the export directory laid out as the project's assets are, so both are
    // dragged into the project as they lie; the render runs through the thread pipeline, its progress shown in the
    // status line
    public void exportMusic() {

        if (recording == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_RECORDING);
            return;
        }

        if (refuseAnimation())
            return;

        if (recordingRenderBranch.isRunning()) {
            setStatus(EditorSetting.RECORDER_STATUS_RENDER_RUNNING);
            return;
        }

        saveRecording();

        String recordingName = recording.getRecordingName();
        File songFile = ArpgUtility.resolveFile(
                new File(exportDirectory, EditorSetting.RECORDER_EXPORT_SONG_DIRECTORY), recordingName);
        File soundFile = new File(new File(exportDirectory, EditorSetting.RECORDER_EXPORT_SOUND_DIRECTORY),
                recordingName + EditorSetting.RECORDER_EXTENSION_SEPARATOR + EngineSetting.SOUND_FILE_EXTENSION);

        if (!recordingMusicBranch.writeSong(songFile, recording)) {
            setStatus(EditorSetting.RECORDER_STATUS_SAVE_FAILED + songFile.getAbsolutePath());
            return;
        }

        String refusal = recordingRenderBranch.begin(soundFile, recordingMusicBranch.toSong(recording, true));

        setStatus(refusal != null ? refusal : EditorSetting.RECORDER_STATUS_RENDERING_FORMAT.formatted(0));
    }

    // While a render runs its progress is shown a step at a time, and its last word once it ends
    private void updateRender() {

        if (!recordingRenderBranch.isRunning() && !recordingRenderBranch.isFinished())
            return;

        if (recordingRenderBranch.isFinished()) {
            setStatus(recordingRenderBranch.collectResult());
            return;
        }

        int percent = recordingRenderBranch.getPercent();

        if (percent != renderPercent) {
            this.renderPercent = percent;
            setStatus(EditorSetting.RECORDER_STATUS_RENDERING_FORMAT.formatted(percent));
        }
    }

    // Rows \\

    // The rows a Recorder or Timeline tab shows in the Properties tab: the tempo, meter and key; or the
    // lane selected's mix and the clip selected's instrument, volume and transpose
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows, boolean timeline) {
        if (timeline)
            recordingRowBranch.buildTimelineRows(rows);
        else
            recordingRowBranch.buildTransportRows(rows);
    }

    // The rows the Character Outliner shows while a Recorder tab is worked in: the channels armed and the export
    public void buildSettingsRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recordingRowBranch.buildSettingsRows(rows);
    }

    // The rows the Character Outliner shows while the Keys or Keys Graph tab is worked in: the keys selected
    public void buildKeyRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        recordingRowBranch.buildKeyRows(rows);
    }

    // Moves whenever the keys, the curve picked or anything else the recorder shows may have changed
    public int getKeyRowRevision() {
        return revision + keysRevision;
    }

    // Every step of one row in a row is one step of the undo history, so a slider dragged undoes at once
    public void stepRow(String key, int steps) {
        recordingHistoryBranch.beginRow(key);
        recordingRowBranch.step(key, steps);
        recordingHistoryBranch.endRow();
    }

    public void cycleRow(String key, int direction) {
        recordingRowBranch.cycle(key, direction);
    }

    public String getRowValueText(String key) {
        return recordingRowBranch.getValueText(key);
    }

    public boolean isRowValueValid(String key, String text) {
        return recordingRowBranch.isValueValid(key, text);
    }

    public void setRowValue(String key, String text) {
        recordingRowBranch.setValue(key, text);
    }

    // Selection \\

    // What is selected in the pane worked in last removed as one: on the Keys pane the keys, else the sound, else
    // the node; on the Timeline the transition, else the node, else every clip selected, so a clip whose take is a
    // sound alone leaves the timeline rather than losing its sound
    public void deleteSelection() {

        if (pane == RecorderPane.KEYS) {
            deleteKeysPaneSelection();
            return;
        }

        if (isTransitionTarget(selectedTransition)) {
            removeTransition();
            return;
        }

        if (isNodeIndex(selectedNode)) {
            removeNode();
            return;
        }

        removeClip();
    }

    // The keys selected, else the sound, else the node; with none of them, the status says no key is selected
    private void deleteKeysPaneSelection() {

        if (selectedKeys.isEmpty() && isSoundIndex(selectedSound)) {
            removeSound();
            return;
        }

        if (selectedKeys.isEmpty() && isNodeIndex(selectedNode)) {
            removeNode();
            return;
        }

        deleteKeys();
    }

    // History \\

    // True while a session plays the open media file in a window, which is never changed under it; an animation clip
    // plays in no window, so it is undone and redone while it plays
    private boolean isHeldBySession() {
        return state.isSession() && !recording.isAnimation();
    }

    // The open recording put back as it stood before the last edit, refused while a session plays it in a window
    public void undo() {

        if (recording == null || isHeldBySession() || drag.isActive())
            return;

        ArpgObjectStruct step = recordingHistoryBranch.undo();

        if (step == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NOTHING_TO_UNDO);
            return;
        }

        restore(step, EditorSetting.RECORDER_STATUS_UNDONE);
    }

    public void redo() {

        if (recording == null || isHeldBySession() || drag.isActive())
            return;

        ArpgObjectStruct step = recordingHistoryBranch.redo();

        if (step == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NOTHING_TO_REDO);
            return;
        }

        restore(step, EditorSetting.RECORDER_STATUS_REDONE);
    }

    // The recording a step holds made the open one in place of the one open, unsaved, the playhead, the view, the
    // clips, the layer, the curve and the keys selected kept wherever they still fit; a step that cannot be read
    // leaves the recording as it is and the history begun again from it
    private void restore(ArpgObjectStruct step, String status) {

        String recordingName = recording.getRecordingName();
        RecordingInstance restored = recording.isAnimation()
                ? recordingAnimationBranch.parse(step, recordingName)
                : recordingFileBranch.parse(
                        step, recordingName, EditorSetting.RECORDER_HISTORY_ORIGIN_PREFIX + recordingName);

        if (restored == null) {
            recordingHistoryBranch.reset(recording);
            setStatus(EditorSetting.RECORDER_STATUS_UNREADABLE + recordingName);
            return;
        }

        replaceRecording(restored, status);
    }

    // A recording made the open one in place of the one open, as a step of the history or a change made to an open
    // animation clip elsewhere brings it back: unsaved, the playhead, the view, the clips, the layer, the curve and
    // the keys selected kept wherever they still fit
    private void replaceRecording(RecordingInstance restored, String status) {

        int keptClip = selectedClip;
        int keptLayer = keyLayer;
        int keptSlot = selectedSlot;
        int keptKey = selectedKey;
        int keptSound = selectedSound;
        double keptMinimum = graphMinimum;
        double keptMaximum = graphMaximum;

        keptClips.clear();
        keptClips.addAll(selectedClips);
        keptKeys.clear();
        keptKeys.addAll(selectedKeys);

        adoptRecording(restored);
        clearSelection();
        restoreClipSelection(keptClip);
        this.unsaved = true;
        this.playhead = Math.min(playhead, recording.getDuration());
        rebuildNodes();
        updateKeyTracks();
        restoreKeySelection(keptLayer, keptSlot, keptKey, keptSound);
        this.graphMinimum = keptMinimum;
        this.graphMaximum = keptMaximum;
        this.documentRevision++;
        keysRevision++;
        setStatus(status);
    }

    private void restoreClipSelection(int keptClip) {

        for (int i = 0; i < keptClips.size(); i++)
            if (isClipIndex(keptClips.getInt(i)))
                selectedClips.add(keptClips.getInt(i));

        if (isClipIndex(keptClip))
            focusClip(keptClip);
        else if (!selectedClips.isEmpty())
            focusClip(selectedClips.getInt(selectedClips.size() - 1));
    }

    // The layer and curve picked again, and the keys or the sound selected on it that it still holds
    private void restoreKeySelection(int keptLayer, int keptSlot, int keptKey, int keptSound) {

        if (keptLayer == EditorSetting.RECORDER_KEY_LAYER_NONE)
            return;

        selectCurve(keptLayer, keptSlot);

        if (keptLayer == EditorSetting.RECORDER_KEY_TRACK_SOUNDS) {
            selectSound(keptSound);
            return;
        }

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || keyLayer != keptLayer)
            return;

        for (int i = 0; i < keptKeys.size(); i++)
            if (keptKeys.getInt(i) < curve.size())
                addKeyToSelection(keptKeys.getInt(i));

        if (keptKey >= 0 && keptKey < curve.size() && selectedKeys.contains(keptKey))
            this.selectedKey = keptKey;
    }

    // Pane \\

    public void showPane(RecorderPane shown) {
        this.pane = shown;
        revision++;
    }

    // Layers \\

    // The stack the Layers tab lists while a pane is worked in: the take's tracks, or every take in the bin, each
    // pane keeping its own view so the Timeline shows the takes and the Keys the take's layers
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots, RecorderPane shownPane) {
        recordingLayerBranch.buildLayers(roots, getLayerView(shownPane));
    }

    public String getLayerTitle(RecorderPane shownPane) {
        return recordingLayerBranch.resolveTitle(getLayerView(shownPane));
    }

    public void selectLayer(String layerKey) {
        recordingLayerBranch.selectLayer(layerKey);
    }

    // The take a layer's key names in the Takes view, -1 for any other layer
    public int findLayerTake(String layerKey) {
        return recordingLayerBranch.findTake(layerKey);
    }

    public void showLayerView(RecorderPane shownPane, RecorderLayerView shown) {

        if (shown == layerViews[shownPane.ordinal()])
            return;

        layerViews[shownPane.ordinal()] = shown;
        layerRevision++;
    }

    // The tool action the Layers tab's Delete Layer icon runs: a take is deleted from the Takes view, and a track is
    // never deleted
    public String getRemoveLayerKey(RecorderPane shownPane) {
        return getLayerView(shownPane) == RecorderLayerView.TAKES ? EditorSetting.RECORDER_KEY_TAKE_DELETE : "";
    }

    public RecorderLayerView getLayerView(RecorderPane shownPane) {
        return layerViews[shownPane.ordinal()];
    }

    // Moves whenever the takes, the take shown or the layer picked may have changed
    public int getLayerRevision() {
        return revision + layerRevision;
    }

    // Curves \\

    // Every track of the take that recorded anything, queued to have each value it holds laid on keys
    private void queueCurves(RecordingTakeStruct queued) {
        collectCurveTracks(queued, unlaidTracks);
    }

    // One queued track laid on keys a frame, so a long take never stalls the editor; once the last is laid the
    // recording keeps them, saved at once when they were laid for a take just recorded
    private void layCurves() {

        if (unlaidTracks.isEmpty() || !canEditTimeline())
            return;

        RecordingCurveTrackStruct track = unlaidTracks.remove(0);

        if (ensureCurves(track)) {
            this.curvesLaid = true;

            if (keyTracks.contains(track))
                keysRevision++;
        }

        if (!unlaidTracks.isEmpty())
            return;

        if (curvesLaid)
            markLaid();

        if (saveWhenLaid)
            saveRecording();

        this.curvesLaid = false;
        this.saveWhenLaid = false;
    }

    // The tracks of the take that recorded anything, in the order the Keys pane lists them: the camera, the
    // player, every resident, item and vehicle, the weather and the clock, whichever it holds, and an animation
    // clip layer's bones and morphs
    private void collectCurveTracks(RecordingTakeStruct collected, ObjectArrayList<RecordingCurveTrackStruct> tracks) {

        addCurveTrack(collected.getCameraTrack(), tracks);
        addCurveTrack(collected.getPlayerTrack(), tracks);

        for (int i = 0; i < collected.getResidentTracks().size(); i++)
            addCurveTrack(collected.getResidentTracks().get(i), tracks);

        for (int i = 0; i < collected.getProjectileTracks().size(); i++)
            addCurveTrack(collected.getProjectileTracks().get(i), tracks);

        for (int i = 0; i < collected.getVehicleTracks().size(); i++)
            addCurveTrack(collected.getVehicleTracks().get(i), tracks);

        addCurveTrack(collected.getWeatherTrack(), tracks);
        addCurveTrack(collected.getClockTrack(), tracks);

        for (int i = 0; i < collected.getBoneTracks().size(); i++)
            addCurveTrack(collected.getBoneTracks().get(i), tracks);

        addCurveTrack(collected.getMorphTrack(), tracks);
    }

    private void addCurveTrack(RecordingCurveTrackStruct track, ObjectArrayList<RecordingCurveTrackStruct> tracks) {
        if (track != null && (track.isKeyedOnly() || !track.isEmpty()))
            tracks.add(track);
    }

    // Keys \\

    // The tracks of the selected clip's take, listed again whenever another take's clip is selected; a take none of
    // whose tracks is open opens its first, with its first value selected so its graph shows at once, and a take of
    // sounds alone has its first sound within the clip selected so it is changed from the clip, unless a transition
    // on the clip was what selected it
    private void updateKeyTracks() {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingTakeStruct take = clip != null ? clip.getTake() : null;

        if (take == keyTake)
            return;

        this.keyTake = take;
        this.keyLayer = EditorSetting.RECORDER_KEY_LAYER_NONE;
        keyTracks.clear();
        clearCurveSelection();
        clearSoundSelection();
        layerRevision++;
        keysRevision++;

        if (take == null)
            return;

        collectCurveTracks(take, keyTracks);

        if (!keyTracks.isEmpty()) {
            selectKeyLayer(0);
            return;
        }

        if (take.getSoundTrack() == null)
            return;

        showKeyLayer(EditorSetting.RECORDER_KEY_TRACK_SOUNDS);

        if (take.isSoundAlone() && selectedTransition == EngineSetting.INDEX_NOT_FOUND)
            selectFirstSound(EngineSetting.INDEX_NOT_FOUND);
    }

    // The layer the Keys tab shows: a track of the take, each value it holds laid on a curve now if the queue has
    // not reached it yet and its first value selected, or the take's Sounds layer, its first sound heard selected
    public void selectKeyLayer(int layer) {

        if (layer == EditorSetting.RECORDER_KEY_TRACK_SOUNDS) {

            if (getKeySoundTrack() == null)
                return;

            showKeyLayer(layer);
            selectFirstSound(EngineSetting.INDEX_NOT_FOUND);
            return;
        }

        if (!isKeyTrackIndex(layer))
            return;

        if (ensureCurves(keyTracks.get(layer)))
            markLaid();

        selectCurve(layer, 0);
    }

    private void showKeyLayer(int layer) {

        if (layer == keyLayer)
            return;

        this.keyLayer = layer;
        layerRevision++;
        keysRevision++;
    }

    private boolean ensureCurves(RecordingCurveTrackStruct track) {

        if (!canEditTimeline())
            return false;

        boolean laid = false;

        for (int slot = 0; slot < track.getCurveCount(); slot++)
            if (!track.hasCurve(slot) && recordingCurveBranch.ensureCurve(track, slot, resolveFitWorld()))
                laid = true;

        return laid;
    }

    // A curve selected for its keys to be edited, laid now if the queue has not reached it yet; on the Sounds layer
    // the first time the sound is heard within the clip is selected instead
    public void selectCurve(int trackIndex, int slot) {

        if (trackIndex == EditorSetting.RECORDER_KEY_TRACK_SOUNDS) {
            showKeyLayer(trackIndex);
            selectFirstSound(slot);
            return;
        }

        if (!isKeyTrackIndex(trackIndex))
            return;

        RecordingCurveTrackStruct track = keyTracks.get(trackIndex);

        if (slot < 0 || slot >= track.getCurveCount())
            return;

        showKeyLayer(trackIndex);

        if (!track.hasCurve(slot) && canEditTimeline()
                && recordingCurveBranch.ensureCurve(track, slot, resolveFitWorld()))
            markLaid();

        this.selectedTrack = track;
        this.selectedSlot = slot;
        clearKeySelection();
        releaseSound();
        frameGraph();
        keysRevision++;
    }

    // The key selected alone
    public void selectKey(int keyIndex) {

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || keyIndex < 0 || keyIndex >= curve.size())
            return;

        clearKeySelection();
        addKeyToSelection(keyIndex);
        keysRevision++;
    }

    // Every key of the selected curve, the one last picked still the one shown
    public void selectAllKeys() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null || curve.isEmpty())
            return;

        int shownKey = selectedKey;

        selectedKeys.clear();

        for (int i = 0; i < curve.size(); i++)
            selectedKeys.add(i);

        this.selectedKey = shownKey != EngineSetting.INDEX_NOT_FOUND ? shownKey : curve.size() - 1;
        releaseNode();
        releaseSound();
        keysRevision++;
    }

    // A key clicked without moving: alone, or with Shift added to the keys or taken back out if it was among them
    // already, or with Ctrl taken out of them
    private void pickKey(int keyIndex, boolean extending, boolean subtracting, boolean keySelected) {

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || keyIndex < 0 || keyIndex >= curve.size())
            return;

        if (subtracting || extending && keySelected)
            removeKeyFromSelection(keyIndex);
        else if (!extending)
            selectKey(keyIndex);

        keysRevision++;
    }

    // Every key of the selected curve within the moments, and the values when boxed on the graph, added to the keys
    // selected, or with Ctrl taken out of them
    private void boxKeys(float fromTime, float toTime, double lowest, double highest, boolean subtracting) {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (clip == null || curve == null)
            return;

        float from = Math.min(fromTime, toTime);
        float to = Math.max(fromTime, toTime);

        for (int i = 0; i < curve.size(); i++) {

            float time = clip.toTimelineTime(curve.getTime(i));
            double value = curve.getValue(i);

            if (time < from || time > to || value < lowest || value > highest)
                continue;

            if (subtracting)
                removeKeyFromSelection(i);
            else
                addKeyToSelection(i);
        }

        keysRevision++;
    }

    // What a press on a row of the Keys tab takes hold of: a key on a curve's row to retime it with every key
    // selected, a node on a character's movement or gesture row to retime or move it, or a sound on the Sounds
    // layer to retime it; on nothing on a curve's row with Shift or Ctrl it boxes the moments it travels over, and
    // anywhere else it selects what it lies on, or with nothing there moves the playhead
    public void pressKeyRow(
            int trackIndex,
            int row,
            float time,
            float secondsPerPixel,
            boolean extending,
            boolean subtracting,
            WindowInstance window,
            float pressX,
            float pressY) {

        RecordingClipStruct clip = getSelectedClipStruct();

        if (drag.isActive() || clip == null)
            return;

        if (trackIndex == EditorSetting.RECORDER_KEY_TRACK_SOUNDS) {
            pressSoundRow(clip, row, time, secondsPerPixel, window, pressX, pressY);
            return;
        }

        if (!isKeyTrackIndex(trackIndex))
            return;

        RecordingCurveTrackStruct track = keyTracks.get(trackIndex);

        if (row >= 0) {

            if (track != selectedTrack || row != selectedSlot)
                selectCurve(trackIndex, row);

            RecordingCurveStruct curve = getSelectedCurve();
            int key = curve != null && canEditTimeline()
                    ? curve.findKey(clip.toChildTime(time), EditorSetting.RECORDER_KEY_GRAB_PIXELS * secondsPerPixel)
                    : EngineSetting.INDEX_NOT_FOUND;

            if (key != EngineSetting.INDEX_NOT_FOUND) {
                beginKeyDrag(RecorderDragKind.KEY, curve, key, 0.0, extending, subtracting,
                        window, pressX, pressY, time);
                return;
            }

            if (curve != null && canEditTimeline() && (extending || subtracting)) {
                beginBoxDrag(RecorderDragKind.KEYS_BOX, time, 0.0, extending, subtracting, window, pressX, pressY);
                return;
            }
        }

        int nodeIndex = EngineSetting.INDEX_NOT_FOUND;

        if ((row == EditorSetting.RECORDER_KEY_ROW_MOVEMENT || row == EditorSetting.RECORDER_KEY_ROW_GESTURES)
                && track instanceof RecordingBodyTrackStruct body) {

            float notchTolerance = EditorSetting.RECORDER_NOTCH_GRAB_PIXELS * secondsPerPixel;

            nodeIndex = recordingNodeBranch.findNodeAt(nodes, selectedClip, body, time,
                    row == EditorSetting.RECORDER_KEY_ROW_GESTURES, notchTolerance);

            if (nodeIndex != EngineSetting.INDEX_NOT_FOUND && canEditTimeline()
                    && isNodeGrabbed(nodes.get(nodeIndex), time, notchTolerance)) {
                RecordingNodeStruct node = nodes.get(nodeIndex);
                beginDrag(RecorderDragKind.NODE, selectedClip, nodeIndex, window, pressX, pressY, time,
                        node.getKind() == RecorderNodeKind.GESTURE ? time - node.getStart() : 0f);
                return;
            }
        }

        beginDrag(RecorderDragKind.KEYS_PRESS, EngineSetting.INDEX_NOT_FOUND, nodeIndex,
                window, pressX, pressY, time, 0f);
    }

    // What a press on the graph takes hold of: a handle of the key shown, else the key nearest it, each within reach
    // in pixels; on nothing with Shift or Ctrl it boxes keys to add or take away, and anywhere else it scrolls the
    // graph as it travels, or never travelling lets the keys go and moves the playhead
    public void pressGraph(
            float time,
            double value,
            float secondsPerPixel,
            double valuePerPixel,
            boolean extending,
            boolean subtracting,
            WindowInstance window,
            float pressX,
            float pressY) {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (drag.isActive() || clip == null || curve == null)
            return;

        float takeTime = clip.toChildTime(time);

        if (canEditTimeline() && !extending && !subtracting && selectedKey >= 0 && selectedKey < curve.size()
                && curve.getMode(selectedKey) != CurveKeyMode.CONSTANT) {

            float keyTime = curve.getTime(selectedKey);
            double keyValue = curve.getValue(selectedKey);

            if (selectedKey > 0 && isWithinGrab(keyTime + curve.getInTime(selectedKey),
                    keyValue + curve.getInValue(selectedKey), takeTime, value, secondsPerPixel, valuePerPixel)) {
                beginKeyDrag(RecorderDragKind.HANDLE_IN, curve, selectedKey, value, false, false,
                        window, pressX, pressY, time);
                return;
            }

            if (selectedKey < curve.size() - 1 && isWithinGrab(keyTime + curve.getOutTime(selectedKey),
                    keyValue + curve.getOutValue(selectedKey), takeTime, value, secondsPerPixel, valuePerPixel)) {
                beginKeyDrag(RecorderDragKind.HANDLE_OUT, curve, selectedKey, value, false, false,
                        window, pressX, pressY, time);
                return;
            }
        }

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        double nearestPixels = EditorSetting.RECORDER_GRAPH_GRAB_PIXELS;

        for (int i = 0; canEditTimeline() && i < curve.size(); i++) {

            double pixels = Math.hypot(
                    (curve.getTime(i) - takeTime) / secondsPerPixel,
                    (curve.getValue(i) - value) / valuePerPixel);

            if (pixels <= nearestPixels) {
                nearest = i;
                nearestPixels = pixels;
            }
        }

        if (nearest != EngineSetting.INDEX_NOT_FOUND) {

            if (!extending && !subtracting && toggleHandlesOnSecondClick(curve, nearest))
                return;

            beginKeyDrag(RecorderDragKind.GRAPH_KEY, curve, nearest, value, extending, subtracting,
                    window, pressX, pressY, time);
            return;
        }

        if (canEditTimeline() && (extending || subtracting)) {
            beginBoxDrag(RecorderDragKind.GRAPH_BOX, time, value, extending, subtracting, window, pressX, pressY);
            return;
        }

        beginDrag(RecorderDragKind.GRAPH_PRESS, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, time, 0f);
        drag.anchorValue(value);
    }

    // A key clicked twice in quick succession on the graph lets its handles go their own ways, so either side bends
    // alone, or joins them on one line again; true when it did
    private boolean toggleHandlesOnSecondClick(RecordingCurveStruct curve, int keyIndex) {

        long now = internal.getTime();
        boolean second = curve == clickedCurve && keyIndex == clickedKey
                && now - clickedKeyMillis <= EditorSetting.RECORDER_DOUBLE_CLICK_MILLIS;

        this.clickedCurve = second ? null : curve;
        this.clickedKey = second ? EngineSetting.INDEX_NOT_FOUND : keyIndex;
        this.clickedKeyMillis = now;

        if (!second)
            return false;

        boolean freeing = curve.getMode(keyIndex) != CurveKeyMode.FREE;

        if (freeing)
            curve.setMode(keyIndex, CurveKeyMode.FREE);
        else
            curve.joinHandles(keyIndex);

        clearKeySelection();
        addKeyToSelection(keyIndex);
        finishKeyEdit(freeing
                ? EditorSetting.RECORDER_STATUS_HANDLES_FREED
                : EditorSetting.RECORDER_STATUS_HANDLES_JOINED);
        return true;
    }

    private boolean isWithinGrab(
            float pointTime,
            double pointValue,
            float time,
            double value,
            float secondsPerPixel,
            double valuePerPixel) {

        return Math.hypot((pointTime - time) / secondsPerPixel, (pointValue - value) / valuePerPixel)
                <= EditorSetting.RECORDER_GRAPH_GRAB_PIXELS;
    }

    // A key pressed joins the keys selected — alone, or with Shift beside them — unless Ctrl is to take it out, and
    // every key selected is held as it stood so they all move together by how far it travels
    private void beginKeyDrag(
            RecorderDragKind kind,
            RecordingCurveStruct curve,
            int keyIndex,
            double pressValue,
            boolean extending,
            boolean subtracting,
            WindowInstance window,
            float pressX,
            float pressY,
            float time) {

        boolean keySelected = selectedKeys.contains(keyIndex);

        if (!keySelected && !subtracting && !extending)
            clearKeySelection();

        if (!subtracting)
            addKeyToSelection(keyIndex);

        beginDrag(kind, keyIndex, EngineSetting.INDEX_NOT_FOUND, window, pressX, pressY, time, 0f);
        drag.anchorKey(curve.getTime(keyIndex), curve.getValue(keyIndex), pressValue);
        drag.anchorSelection(extending, subtracting, keySelected);
        anchorSelectedKeys(curve);
        keysRevision++;
    }

    // A box begun on nothing, carried to wherever the cursor travels
    private void beginBoxDrag(
            RecorderDragKind kind,
            float time,
            double value,
            boolean extending,
            boolean subtracting,
            WindowInstance window,
            float pressX,
            float pressY) {

        beginDrag(kind, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                window, pressX, pressY, time, 0f);
        drag.anchorValue(value);
        drag.anchorSelection(extending, subtracting, false);
        drag.reach(time, value);
    }

    private void anchorSelectedKeys(RecordingCurveStruct curve) {

        draggedKeyTimes.clear();
        draggedKeyValues.clear();

        for (int i = 0; i < selectedKeys.size(); i++) {
            draggedKeyTimes.add(curve.getTime(selectedKeys.getInt(i)));
            draggedKeyValues.add(curve.getValue(selectedKeys.getInt(i)));
        }
    }

    // A key or handle carried on the Keys pane follows the cursor, its curve changing as it moves, and every other
    // key selected moves with the key carried; a node carried there lands as it does on the lanes, a sound lands
    // where it would begin, a box reaches to
    // the cursor, and a press on nothing scrolls the view, on the graph its values too
    public void dragKeysTo(
            float x,
            float y,
            float time,
            double value,
            float secondsPerPixel,
            double valuePerPixel,
            boolean snapping) {

        if (!drag.isActive())
            return;

        drag.travel(x, y, EditorSetting.RECORDER_DRAG_THRESHOLD_PIXELS);

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (drag.getKind() == RecorderDragKind.NODE) {
            landNode(time);
            return;
        }

        if (drag.getKind() == RecorderDragKind.SOUND_EVENT) {
            landSound(time, secondsPerPixel, snapping);
            return;
        }

        if (drag.getKind().isPress()) {

            if (drag.isMoved())
                panView(x, secondsPerPixel);

            if (drag.isMoved() && drag.getKind() == RecorderDragKind.GRAPH_PRESS)
                panGraph(drag.getPressValue() - value);

            return;
        }

        if (drag.getKind().isBox()) {
            drag.reach(time, value);
            curveRevision++;
            return;
        }

        if (!drag.getKind().isKey() || !drag.isMoved() || drag.isSubtracting() || clip == null || curve == null
                || drag.getIndex() >= curve.size())
            return;

        int keyIndex = drag.getIndex();

        if (drag.getKind() == RecorderDragKind.HANDLE_IN || drag.getKind() == RecorderDragKind.HANDLE_OUT) {

            float handleTime = clip.toChildTime(time);
            double handleValue = value;

            if (snapping) {
                handleTime = snapHandleTime(curve, keyIndex, handleTime, secondsPerPixel);
                handleValue = snapHandleValue(curve, handleValue, valuePerPixel);
            }

            curve.setHandle(keyIndex, drag.getKind() == RecorderDragKind.HANDLE_OUT,
                    handleTime - curve.getTime(keyIndex), handleValue - curve.getValue(keyIndex));
            curveRevision++;
            return;
        }

        float landing = clip.toTimelineTime(drag.getKeyTime()) + time - drag.getPressTime();

        if (snapping)
            landing = recordingTimelineBranch.snapTime(recording, landing, playhead,
                    EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel, null);

        moveSelectedKeys(curve, clip.toChildTime(landing) - drag.getKeyTime(),
                drag.getKind() == RecorderDragKind.GRAPH_KEY ? value - drag.getPressValue() : 0.0);
        curveRevision++;
    }

    // A handle carried with snapping lands on the moment of any other key of its curve within reach
    private float snapHandleTime(RecordingCurveStruct curve, int keyIndex, float time, float secondsPerPixel) {

        float nearest = EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel;
        float snapped = time;

        for (int i = 0; i < curve.size(); i++) {

            float distance = Math.abs(curve.getTime(i) - time);

            if (i != keyIndex && distance <= nearest) {
                nearest = distance;
                snapped = curve.getTime(i);
            }
        }

        return snapped;
    }

    // A handle carried with snapping lands level with the value of any key of its curve within reach, its own key's
    // laying it flat
    private double snapHandleValue(RecordingCurveStruct curve, double value, double valuePerPixel) {

        double nearest = EditorSetting.RECORDER_SNAP_PIXELS * valuePerPixel;
        double snapped = value;

        for (int i = 0; i < curve.size(); i++) {

            double distance = Math.abs(curve.getValue(i) - value);

            if (distance <= nearest) {
                nearest = distance;
                snapped = curve.getValue(i);
            }
        }

        return snapped;
    }

    // Every key selected moved by the same time and value from where it stood, the one furthest along the way first
    // so each still has room beside the next
    // The selected keys moved together, held as one within the take they belong to so none can be carried past its
    // start or its end; keys already lying outside it may be brought back in, never carried further out
    private void moveSelectedKeys(RecordingCurveStruct curve, float shift, double rise) {

        int count = selectedKeys.size();

        if (count == 0)
            return;

        float duration = getSelectedClipStruct().getTake().getDuration();
        float held = Math.clamp(
                shift,
                Math.min(0f, -draggedKeyTimes.getFloat(0)),
                Math.max(0f, duration - draggedKeyTimes.getFloat(count - 1)));

        for (int i = 0; i < count; i++) {

            int slot = held > 0f ? count - 1 - i : i;

            curve.moveKey(selectedKeys.getInt(slot), draggedKeyTimes.getFloat(slot) + held,
                    draggedKeyValues.getDouble(slot) + rise);
        }
    }

    public void addKeyAtPlayhead() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return;

        RecordingClipStruct clip = getSelectedClipStruct();
        float takeTime = Math.clamp(clip.toChildTime(playhead), 0f, clip.getTake().getDuration());

        int keyIndex = curve.addKey(takeTime, curve.evaluate(takeTime), CurveKeyMode.AUTO);

        clearKeySelection();
        addKeyToSelection(keyIndex);
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_ADDED);
    }

    // Every selected key leaves its curve, the last first so the rest keep their places; a curve keeps at least one
    // key, so every value always plays from keys
    public void deleteKeys() {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        int count = selectedKeys.size();

        if (count >= curve.size()) {
            setStatus(EditorSetting.RECORDER_STATUS_LAST_KEY);
            return;
        }

        for (int i = count - 1; i >= 0; i--)
            curve.removeKey(selectedKeys.getInt(i));

        clearKeySelection();
        finishKeyEdit(count == 1
                ? EditorSetting.RECORDER_STATUS_KEY_DELETED
                : EditorSetting.RECORDER_STATUS_KEYS_DELETED);
    }

    // Every selected key's handles stand in the mode after the shown key's, in turn
    public void cycleKeyMode() {
        cycleKeyMode(1);
    }

    public void cycleKeyMode(int direction) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        setKeyMode(CurveKeyMode.VALUES[Math.floorMod(
                curve.getMode(selectedKey).ordinal() + direction, CurveKeyMode.VALUES.length)]);
    }

    // Every selected key's handles stand in the mode
    public void setKeyMode(CurveKeyMode mode) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        for (int i = 0; i < selectedKeys.size(); i++)
            curve.setMode(selectedKeys.getInt(i), mode);

        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_CHANGED);
    }

    // Every selected key moved along by the seconds and up by the rise, together as a drag moves them
    public void shiftKeys(float shift, double rise) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null || shift == 0f && rise == 0.0)
            return;

        anchorSelectedKeys(curve);
        moveSelectedKeys(curve, shift, rise);
        frameGraph();
        finishKeyEdit(EditorSetting.RECORDER_STATUS_KEY_MOVED);
    }

    // The shown key moved to the moment of the timeline, every other selected key with it
    public void setKeyTime(float time) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null)
            return;

        shiftKeys(getSelectedClipStruct().toChildTime(time) - curve.getTime(selectedKey), 0.0);
    }

    public boolean isKeyValueValid(String text) {

        RecordingCurveStruct curve = getSelectedCurve();

        return curve != null && selectedKey >= 0 && selectedKey < curve.size()
                && !Double.isNaN(parseKeyValue(curve, text));
    }

    // The shown key given the typed value, every other selected key rising or falling with it
    public void setKeyValue(String text) {

        RecordingCurveStruct curve = requireSelectedKey();

        if (curve == null || !isKeyValueValid(text))
            return;

        shiftKeys(0f, parseKeyValue(curve, text) - curve.getValue(selectedKey));
    }

    // The value a step of the Outliner's value row moves the keys by, a share of the least span the curve frames
    public double resolveKeyValueStep() {

        RecordingCurveStruct curve = getSelectedCurve();

        return curve != null
                ? curve.getProperty().getKind().getMinimumSpan() * EditorSetting.RECORDER_ROW_KEYS_VALUE_STEP_SHARE
                : 0.0;
    }

    // The shown key's moment on the timeline, or -1 when no key is shown
    public float resolveKeyTime() {

        RecordingCurveStruct curve = getSelectedCurve();
        RecordingClipStruct clip = getSelectedClipStruct();

        return curve != null && clip != null && selectedKey >= 0 && selectedKey < curve.size()
                ? clip.toTimelineTime(curve.getTime(selectedKey))
                : EngineSetting.INDEX_NOT_FOUND;
    }

    private double parseKeyValue(RecordingCurveStruct curve, String text) {
        return RecordingCurveUtility.parseValue(curve.getProperty().getKind(), text, curve.getValue(selectedKey),
                clockManager.getCalendarHandle());
    }

    // The selected key's value written so the value dialog can start from it
    public String formatKeyValueInput() {

        RecordingCurveStruct curve = getSelectedCurve();

        return curve != null && selectedKey >= 0 && selectedKey < curve.size()
                ? RecordingCurveUtility.formatInput(curve.getProperty().getKind(), curve.getValue(selectedKey),
                        clockManager.getCalendarHandle())
                : "";
    }

    public String formatValue(RecorderCurveKind kind, double value) {
        return RecordingCurveUtility.formatValue(kind, value, clockManager.getCalendarHandle());
    }

    // The selected curve laid again from what it recorded, every edit to it undone
    public void refitCurve() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return;

        recordingCurveBranch.refitCurve(selectedTrack, selectedSlot, resolveFitWorld());
        clearKeySelection();
        frameGraph();
        finishKeyEdit(EditorSetting.RECORDER_STATUS_CURVE_REFIT);
    }

    // The graph's values moved along together, so the value pressed on stays under the cursor
    private void panGraph(double shift) {

        if (shift == 0.0)
            return;

        this.graphMinimum += shift;
        this.graphMaximum += shift;
        keysRevision++;
    }

    // The graph's values zoomed by the factor about the value, which stays where it is drawn
    public void zoomGraph(double value, float factor) {

        if (getSelectedCurve() == null)
            return;

        this.graphMinimum = value - (value - graphMinimum) * factor;
        this.graphMaximum = value + (graphMaximum - value) * factor;
        keysRevision++;
    }

    // The view framed about the selected clip and the graph about everything its curve reaches there
    public void frameKeys() {

        RecordingClipStruct clip = getSelectedClipStruct();

        if (clip == null)
            return;

        if (view.frame(clip.getStart(), clip.getEnd(), resolveTimelineSpan()))
            viewRevision++;

        frameGraph();
    }

    // The graph's values framed about everything the selected curve reaches across the part of its clip in view
    public void frameGraph() {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingCurveStruct curve = getSelectedCurve();

        if (clip == null || curve == null || curve.isEmpty())
            return;

        float from = Math.max(view.getStart(), clip.getStart());
        float to = Math.min(view.getEnd(), clip.getEnd());

        if (to <= from) {
            from = clip.getStart();
            to = clip.getEnd();
        }

        double minimum = recordingCurveBranch.resolveMinimum(curve, clip.toChildTime(from), clip.toChildTime(to));
        double maximum = recordingCurveBranch.resolveMaximum(curve, clip.toChildTime(from), clip.toChildTime(to));
        double span = Math.max(maximum - minimum, curve.getProperty().getKind().getMinimumSpan());
        double middle = (minimum + maximum) / 2.0;
        double reach = span / 2.0 * (1.0 + 2.0 * EditorSetting.RECORDER_GRAPH_MARGIN_SHARE);

        this.graphMinimum = middle - reach;
        this.graphMaximum = middle + reach;
        keysRevision++;
    }

    // Every value of the selected clip's take laid on keys again from what it recorded, whatever was done to them,
    // a track a frame as a take just recorded is
    public void refitTake() {

        if (!canEditTimeline() || keyTracks.isEmpty())
            return;

        for (int i = 0; i < keyTracks.size(); i++) {

            RecordingCurveTrackStruct track = keyTracks.get(i);

            for (int slot = 0; slot < track.getCurveCount(); slot++)
                track.setCurve(slot, null);

            unlaidTracks.add(track);
        }

        clearKeySelection();
        keysRevision++;
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TAKE_REFIT + keyTake.getName());
    }

    // Every key edit is saved with the recording and named on the status line
    private void finishKeyEdit(String status) {
        markUnsaved();
        keysRevision++;
        setStatus(status + selectedTrack.getTrackLabel() + EditorSetting.RECORDER_KEY_STATUS_SEPARATOR
                + selectedTrack.getCurveLabel(selectedSlot));
    }

    // The selected curve when keys can be edited, null after saying why not
    private RecordingCurveStruct requireSelectedCurve() {

        if (!canEditTimeline())
            return null;

        RecordingCurveStruct curve = getSelectedCurve();

        if (curve == null || getSelectedClipStruct() == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CURVE);
            return null;
        }

        return curve;
    }

    private RecordingCurveStruct requireSelectedKey() {

        RecordingCurveStruct curve = requireSelectedCurve();

        if (curve == null)
            return null;

        if (selectedKey < 0 || selectedKey >= curve.size()) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_KEY);
            return null;
        }

        return curve;
    }

    // A press on nothing lets the selected keys and sound go and moves the playhead to where it was
    private void releaseKeyAt(float time) {
        clearKeySelection();
        releaseSound();
        keysRevision++;
        seek(time);
    }

    private void clearCurveSelection() {
        this.selectedTrack = null;
        this.selectedSlot = EngineSetting.INDEX_NOT_FOUND;
        clearKeySelection();
    }

    // The keys selected kept in order; the one last added is the key shown, and a key joining lets any node or sound
    // go
    private void addKeyToSelection(int keyIndex) {

        int slot = Arrays.binarySearch(selectedKeys.elements(), 0, selectedKeys.size(), keyIndex);

        if (slot < 0)
            selectedKeys.add(-slot - 1, keyIndex);

        this.selectedKey = keyIndex;
        releaseNode();
        releaseSound();
    }

    // Keys selected let any selected node go, so Delete always knows which of them it removes
    private void releaseNode() {

        if (selectedNodeKind == null)
            return;

        clearNodeSelection();
        revision++;
    }

    // A key taken out of the keys selected; the key shown, if it was the one, becomes the last still selected
    private void removeKeyFromSelection(int keyIndex) {

        selectedKeys.rem(keyIndex);

        if (selectedKey == keyIndex)
            this.selectedKey = selectedKeys.isEmpty()
                    ? EngineSetting.INDEX_NOT_FOUND
                    : selectedKeys.getInt(selectedKeys.size() - 1);
    }

    private void clearKeySelection() {
        this.selectedKey = EngineSetting.INDEX_NOT_FOUND;
        selectedKeys.clear();
    }

    // Positions are read across the world the recording was made in; in another world chunks are counted as they lie
    private WorldHandle resolveFitWorld() {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        return recording != null && recording.getWorldName().equals(activeWorld.getWorldName()) ? activeWorld : null;
    }

    // The selected clip when it plays a take, whose keys and sounds the Keys pane shows; null for none or for a clip
    // of any other source, whose keys are edited where it lies
    private RecordingClipStruct getSelectedClipStruct() {

        RecordingClipStruct clip = isClipIndex(selectedClip) ? recording.getClips().get(selectedClip) : null;

        return clip != null && clip.getTake() != null ? clip : null;
    }

    private boolean isKeyTrackIndex(int trackIndex) {
        return trackIndex >= 0 && trackIndex < keyTracks.size();
    }

    // Sound Layer \\

    // Selecting a sound lets every selected key, any node and any transition go, so Delete always knows what it
    // removes
    public void selectSound(int soundIndex) {

        if (!isSoundIndex(soundIndex) || soundIndex == selectedSound)
            return;

        this.selectedSound = soundIndex;
        clearTransitionSelection();
        clearKeySelection();
        releaseNode();
        keysRevision++;
        revision++;
    }

    // The first sound heard within the selected clip — any sound when no name is given, else a sound of that name
    private void selectFirstSound(int nameIndex) {

        RecordingClipStruct clip = getSelectedClipStruct();
        RecordingSoundTrackStruct track = getKeySoundTrack();

        if (clip == null || track == null)
            return;

        for (int i = 0; i < track.size(); i++)
            if ((nameIndex == EngineSetting.INDEX_NOT_FOUND || track.getNameIndex(i) == nameIndex)
                    && recordingSoundBranch.isHeardIn(clip, track, i)) {
                selectSound(i);
                return;
            }
    }

    // A press on the Sounds layer takes hold of the sound under it — any sound on the layer's own row, a sound of
    // the row's own on a sound's row — selecting it and carrying it to retime it; on nothing it moves the playhead
    private void pressSoundRow(
            RecordingClipStruct clip,
            int row,
            float time,
            float secondsPerPixel,
            WindowInstance window,
            float pressX,
            float pressY) {

        RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();
        int soundIndex = track != null
                ? recordingSoundBranch.findSoundAt(clip, track, row >= 0 ? row : EngineSetting.INDEX_NOT_FOUND, time,
                        EditorSetting.RECORDER_SOUND_GRAB_PIXELS * secondsPerPixel)
                : EngineSetting.INDEX_NOT_FOUND;

        if (soundIndex == EngineSetting.INDEX_NOT_FOUND) {
            beginDrag(RecorderDragKind.KEYS_PRESS, EngineSetting.INDEX_NOT_FOUND, EngineSetting.INDEX_NOT_FOUND,
                    window, pressX, pressY, time, 0f);
            return;
        }

        selectSound(soundIndex);

        if (!canArrangeTimeline())
            return;

        float start = clip.toTimelineTime(track.getTime(soundIndex));

        beginDrag(RecorderDragKind.SOUND_EVENT, selectedClip, soundIndex, window, pressX, pressY, time, time - start);
        drag.land(false, start, clip.getLane());
    }

    // A sound carried lands where it would begin with the cursor where it is now, within its clip, snapped to the
    // timeline's edges when asked; the Keys pane moves its bar there
    private void landSound(float time, float secondsPerPixel, boolean snapping) {

        RecordingClipStruct clip = recording.getClips().get(drag.getIndex());
        float landing = time - drag.getGrabOffset();

        if (snapping)
            landing = recordingTimelineBranch.snapTime(recording, landing, playhead,
                    EditorSetting.RECORDER_SNAP_PIXELS * secondsPerPixel, null);

        drag.land(false, recordingSoundBranch.resolveLanding(clip, landing), clip.getLane());
        curveRevision++;
    }

    // The sound heard from the moment of its clip instead, still selected
    public void moveSound(int soundIndex, float time) {

        RecordingClipStruct clip = getSelectedClipStruct();

        if (!canArrangeTimeline() || clip == null || !isSoundIndex(soundIndex))
            return;

        RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();
        String soundName = track.getSoundName(soundIndex);

        this.selectedSound = track.moveEvent(soundIndex,
                clip.toChildTime(recordingSoundBranch.resolveLanding(clip, time)));
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_MOVED + soundName);
    }

    // A sound from the sound library heard from the moment within the selected clip, the Sounds layer opened to show
    // it selected
    public void addSound(int soundID, float time) {

        if (!canArrangeTimeline())
            return;

        if (soundID == EngineSetting.INDEX_NOT_FOUND) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_LIBRARY_SOUND);
            return;
        }

        RecordingClipStruct clip = getSelectedClipStruct();

        if (clip == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CLIP);
            return;
        }

        RecordingSoundTrackStruct track = clip.getTake().getSoundTrack();

        if (track == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_SOUND_LAYER);
            return;
        }

        SoundHandle soundHandle = soundManager.getSoundHandleFromSoundID(soundID);
        float takeTime = Math.clamp(clip.toChildTime(time), clip.getInPoint(), clip.getOutPoint());

        this.selectedSound = recordingSoundBranch.addSound(track, soundHandle, takeTime);
        showKeyLayer(EditorSetting.RECORDER_KEY_TRACK_SOUNDS);
        clearKeySelection();
        releaseNode();
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_ADDED + soundHandle.getSoundName());
    }

    public void addSoundAtPlayhead(int soundID) {

        if (requirePlayheadInClip())
            addSound(soundID, playhead);
    }

    // A second hearing of the selected sound from the playhead, selected in its place
    public void copySoundToPlayhead() {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null || !requirePlayheadInClip())
            return;

        String soundName = track.getSoundName(selectedSound);

        this.selectedSound = track.copyEvent(selectedSound, getSelectedClipStruct().toChildTime(playhead));
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_ADDED + soundName);
    }

    public void seekSound() {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track != null)
            seek(getSelectedClipStruct().toTimelineTime(track.getTime(selectedSound)));
    }

    // Another sound from the sound library heard in the selected one's place
    public void replaceSound(int soundID) {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null)
            return;

        if (soundID == EngineSetting.INDEX_NOT_FOUND) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_LIBRARY_SOUND);
            return;
        }

        SoundHandle soundHandle = soundManager.getSoundHandleFromSoundID(soundID);

        recordingSoundBranch.replaceSound(track, selectedSound, soundHandle);
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_REPLACED + soundHandle.getSoundName());
    }

    public void removeSound() {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null)
            return;

        String soundName = track.getSoundName(selectedSound);

        track.removeEvent(selectedSound);
        clearSoundSelection();
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_REMOVED + soundName);
    }

    // Every time the take heard the selected sound, removed at once
    public void removeEverySound() {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null)
            return;

        String soundName = track.getSoundName(selectedSound);

        recordingSoundBranch.removeEvery(track, soundName);
        clearSoundSelection();
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUNDS_REMOVED + soundName);
    }

    public boolean isSoundVolumeValid(String text) {
        return isSoundIndex(selectedSound) && !Float.isNaN(recordingSoundBranch.parseAmount(text,
                EditorSetting.RECORDER_SOUND_VOLUME_MIN_PERCENT, EditorSetting.RECORDER_SOUND_VOLUME_MAX_PERCENT));
    }

    // As loud as the percentage typed, reaching the camera louder or quieter by just as much
    public void setSoundVolume(String text) {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null || !isSoundVolumeValid(text))
            return;

        track.setVolume(selectedSound, recordingSoundBranch.parseAmount(text,
                EditorSetting.RECORDER_SOUND_VOLUME_MIN_PERCENT, EditorSetting.RECORDER_SOUND_VOLUME_MAX_PERCENT)
                / EditorSetting.RECORDER_SOUND_PERCENT);
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_CHANGED + track.getSoundName(selectedSound));
    }

    public String formatSoundVolumeInput() {

        RecordingSoundTrackStruct track = getKeySoundTrack();

        return isSoundIndex(selectedSound) ? String.format(EditorSetting.RECORDER_SOUND_PERCENT_FORMAT,
                track.getVolume(selectedSound) * EditorSetting.RECORDER_SOUND_PERCENT) : "";
    }

    public boolean isSoundPitchValid(String text) {
        return isSoundIndex(selectedSound) && !Float.isNaN(recordingSoundBranch.parseAmount(text,
                EditorSetting.RECORDER_SOUND_PITCH_MIN_PERCENT, EditorSetting.RECORDER_SOUND_PITCH_MAX_PERCENT));
    }

    // Played at the percentage typed, a sound that plays through running shorter or longer with it
    public void setSoundPitch(String text) {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null || !isSoundPitchValid(text))
            return;

        recordingSoundBranch.setPitch(track, selectedSound, recordingSoundBranch.parseAmount(text,
                EditorSetting.RECORDER_SOUND_PITCH_MIN_PERCENT, EditorSetting.RECORDER_SOUND_PITCH_MAX_PERCENT)
                / EditorSetting.RECORDER_SOUND_PERCENT);
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_CHANGED + track.getSoundName(selectedSound));
    }

    public String formatSoundPitchInput() {

        RecordingSoundTrackStruct track = getKeySoundTrack();

        return isSoundIndex(selectedSound) ? String.format(EditorSetting.RECORDER_SOUND_PERCENT_FORMAT,
                track.getPitch(selectedSound) * EditorSetting.RECORDER_SOUND_PERCENT) : "";
    }

    public boolean isSoundLengthValid(String text) {
        return isSoundIndex(selectedSound) && !Float.isNaN(recordingSoundBranch.parseAmount(text,
                EditorSetting.RECORDER_SOUND_MIN_LENGTH_SECONDS, Float.MAX_VALUE));
    }

    // Heard for the seconds typed, no longer than it can run unless it loops
    public void setSoundLength(String text) {

        RecordingSoundTrackStruct track = requireSelectedSound();

        if (track == null || !isSoundLengthValid(text))
            return;

        recordingSoundBranch.setLength(track, selectedSound, recordingSoundBranch.parseAmount(
                text, EditorSetting.RECORDER_SOUND_MIN_LENGTH_SECONDS, Float.MAX_VALUE));
        finishSoundEdit(EditorSetting.RECORDER_STATUS_SOUND_CHANGED + track.getSoundName(selectedSound));
    }

    public String formatSoundLengthInput() {

        RecordingSoundTrackStruct track = getKeySoundTrack();

        return isSoundIndex(selectedSound)
                ? String.format(EditorSetting.RECORDER_SOUND_LENGTH_FORMAT, track.getDuration(selectedSound))
                : "";
    }

    // Every sound edit stops what the timeline was sounding and marks it unsaved
    private void finishSoundEdit(String status) {
        soundRevision++;
        markUnsaved();
        keysRevision++;
        setStatus(status);
    }

    // The selected sound's track when sounds can be edited, null after saying why not
    private RecordingSoundTrackStruct requireSelectedSound() {

        if (!canArrangeTimeline())
            return null;

        if (!isSoundIndex(selectedSound)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_SOUND);
            return null;
        }

        return getKeySoundTrack();
    }

    // True when the playhead lies within the selected clip, after saying why not
    private boolean requirePlayheadInClip() {

        RecordingClipStruct clip = getSelectedClipStruct();

        if (clip == null) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_CLIP);
            return false;
        }

        if (playhead < clip.getStart() || playhead > clip.getEnd()) {
            setStatus(EditorSetting.RECORDER_STATUS_OUTSIDE_CLIP);
            return false;
        }

        return true;
    }

    // A key or node selected lets the selected sound go
    private void releaseSound() {

        if (selectedSound == EngineSetting.INDEX_NOT_FOUND)
            return;

        clearSoundSelection();
        revision++;
    }

    private void clearSoundSelection() {
        this.selectedSound = EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean isSoundIndex(int soundIndex) {

        RecordingSoundTrackStruct track = getKeySoundTrack();

        return track != null && soundIndex >= 0 && soundIndex < track.size();
    }

    // Transitions \\

    // The transition the picture passes through at the playhead while the timeline plays or exports; none while a
    // take records or nothing plays
    public RecordingTransitionStruct resolveTransition() {

        if (state == RecorderState.IDLE || state == RecorderState.RECORDING) {
            transition.clear();
            return transition;
        }

        recordingTransitionBranch.resolve(recording, playhead, transition);
        return transition;
    }

    // Whether this frame plays the timeline without the transition's clip or with it
    public void setTransitionWithout(boolean without) {
        transition.setWithoutPlayed(without);
    }

    // The transition as it was last read, without reading it again
    public RecordingTransitionStruct getTransition() {
        return transition;
    }

    // The clip playing the channel at the moment on the side of the transition this frame plays
    // What plays the channel at the moment on the side of the transition this frame plays, walked down through
    // nested recordings into the playing struct given; null when nothing does
    public RecordingPlayingStruct findPlaying(RecorderChannel channel, float time, RecordingPlayingStruct playing) {
        return transition.isWithoutPlayed()
                ? recording.findPlaying(channel, time, transition.getClip(), transition.getCarried(), playing)
                : recording.findPlaying(channel, time, null, null, playing);
    }

    // How loud the clip's own sounds are at the moment, faded across its transitions
    public float resolveClipGain(RecordingClipStruct clip, float time) {
        return recordingTransitionBranch.resolveGain(clip, time);
    }

    // The transition laid on the clip edge, as long as it runs when first laid
    public void placeTransition(int transitionID, int target) {

        if (!canArrangeTimeline() || !isClipIndex(recordingTransitionBranch.toClipIndex(target)))
            return;

        TransitionHandle transitionHandle = transitionManager.getTransitionHandleFromTransitionID(transitionID);

        recordingTransitionBranch.setTransition(
                recording.getClips().get(recordingTransitionBranch.toClipIndex(target)),
                recordingTransitionBranch.toEdge(target),
                transitionHandle,
                transitionHandle.getSeconds());
        selectTransition(target);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRANSITION_PLACED + transitionHandle.getDisplayName());
    }

    // The transitions tool's selection laid on the selected clip's edge, or on the start of the clip following it
    // when the edge is its end
    public void applyTransition(RecorderClipEdge edge) {

        if (requireSelectedClip() == null)
            return;

        placeTransition(transitionManager.getSelectedTransition().getTransitionID(),
                recordingTransitionBranch.resolveEdgeTarget(recording, selectedClip, edge));
    }

    // Selecting a transition selects the clip it lies on, and lets every selected key, node and sound go
    public void selectTransition(int target) {

        if (!isTransitionTarget(target))
            return;

        selectClip(recordingTransitionBranch.toClipIndex(target));
        clearNodeSelection();
        clearKeySelection();
        releaseSound();
        this.selectedTransition = target;
        keysRevision++;
        revision++;
    }

    // The transition's far side dragged to the moment, so it runs from its edge to there
    private void stretchTransition(int target, float farSide) {

        if (!isTransitionTarget(target) || !canArrangeTimeline())
            return;

        RecordingClipStruct clip = recording.getClips().get(recordingTransitionBranch.toClipIndex(target));
        RecorderClipEdge edge = recordingTransitionBranch.toEdge(target);

        recordingTransitionBranch.setSeconds(
                clip, edge, recordingTransitionBranch.resolveSecondsTo(clip, edge, farSide));
        selectTransition(target);
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRANSITION_CHANGED
                + recordingTransitionBranch.resolveHandle(clip, edge).getDisplayName());
    }

    // The transitions tool's selection in the selected transition's place, running as long as it did
    public void replaceTransition() {

        RecordingClipStruct clip = requireSelectedTransition();

        if (clip == null)
            return;

        RecorderClipEdge edge = recordingTransitionBranch.toEdge(selectedTransition);
        TransitionHandle transitionHandle = transitionManager.getSelectedTransition();

        recordingTransitionBranch.setTransition(clip, edge, transitionHandle, clip.getTransitionSeconds(edge));
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRANSITION_CHANGED + transitionHandle.getDisplayName());
    }

    public void removeTransition() {

        RecordingClipStruct clip = requireSelectedTransition();

        if (clip == null)
            return;

        RecorderClipEdge edge = recordingTransitionBranch.toEdge(selectedTransition);
        String displayName = recordingTransitionBranch.resolveHandle(clip, edge).getDisplayName();

        clip.clearTransition(edge);
        clearTransitionSelection();
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRANSITION_REMOVED + displayName);
    }

    public boolean isTransitionLengthValid(String text) {
        return isTransitionTarget(selectedTransition) && !Float.isNaN(recordingTransitionBranch.parseSeconds(text));
    }

    // As long as the seconds typed, within what its clip leaves it
    public void setTransitionLength(String text) {

        RecordingClipStruct clip = requireSelectedTransition();

        if (clip == null || !isTransitionLengthValid(text))
            return;

        RecorderClipEdge edge = recordingTransitionBranch.toEdge(selectedTransition);

        recordingTransitionBranch.setSeconds(clip, edge, recordingTransitionBranch.parseSeconds(text));
        markUnsaved();
        setStatus(EditorSetting.RECORDER_STATUS_TRANSITION_CHANGED
                + recordingTransitionBranch.resolveHandle(clip, edge).getDisplayName());
    }

    public String formatTransitionLengthInput() {
        return isTransitionTarget(selectedTransition)
                ? String.format(EditorSetting.RECORDER_TRANSITION_SECONDS_FORMAT,
                        getSelectedTransitionClip().resolveTransitionSeconds(getSelectedTransitionEdge()))
                : "";
    }

    // The clip holding the selected transition when the timeline can be edited, null after saying why not
    private RecordingClipStruct requireSelectedTransition() {

        if (!canArrangeTimeline())
            return null;

        if (!isTransitionTarget(selectedTransition)) {
            setStatus(EditorSetting.RECORDER_STATUS_NO_TRANSITION);
            return null;
        }

        return getSelectedTransitionClip();
    }

    // True for a clip edge that holds a transition
    private boolean isTransitionTarget(int target) {
        return target != EngineSetting.INDEX_NOT_FOUND
                && isClipIndex(recordingTransitionBranch.toClipIndex(target))
                && recording.getClips().get(recordingTransitionBranch.toClipIndex(target))
                        .hasTransition(recordingTransitionBranch.toEdge(target));
    }

    private void clearTransitionSelection() {
        this.selectedTransition = EngineSetting.INDEX_NOT_FOUND;
    }

    // Export Settings \\

    // Each export setting stepped to the value before or after it, round from the last to the first

    public void cycleResolution(int direction) {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_HEIGHTS, exportStruct.getHeight());
        int next = Math.floorMod(index + direction, EditorSetting.RECORDER_EXPORT_HEIGHTS.length);

        exportStruct.setSize(EditorSetting.RECORDER_EXPORT_WIDTHS[next], EditorSetting.RECORDER_EXPORT_HEIGHTS[next]);
        markUnsaved();
    }

    public void cycleFrameRate(int direction) {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_FRAME_RATES, exportStruct.getFrameRate());

        exportStruct.setFrameRate(EditorSetting.RECORDER_EXPORT_FRAME_RATES[
                Math.floorMod(index + direction, EditorSetting.RECORDER_EXPORT_FRAME_RATES.length)]);
        markUnsaved();
    }

    public void cycleFormat(int direction) {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();

        exportStruct.setFormat(VideoFormat.VALUES[
                Math.floorMod(exportStruct.getFormat().ordinal() + direction, VideoFormat.VALUES.length)]);
        markUnsaved();
    }

    public void cycleMotionBlur(int direction) {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        int index = findIndex(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES, exportStruct.getMotionBlurSamples());

        exportStruct.setMotionBlurSamples(EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES[
                Math.floorMod(index + direction, EditorSetting.RECORDER_EXPORT_MOTION_BLUR_SAMPLES.length)]);
        markUnsaved();
    }

    public void toggleSupersample() {

        if (!canEditExport())
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setSupersampled(!exportStruct.isSupersampled());
        markUnsaved();
    }

    // The range a loop runs round and an export covers begins at the playhead, or at a moment typed
    public void setRangeStart() {
        if (recording != null)
            setRangeStartAt(playhead);
    }

    public void setRangeStartAt(float time) {

        if (!canEditExport())
            return;

        float start = Math.clamp(time, 0f, recording.getDuration());
        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setRangeStart(start);

        if (exportStruct.hasRangeEnd() && exportStruct.getRangeEnd() < start)
            exportStruct.setRangeEnd(EditorSetting.RECORDER_TIME_UNSET);

        markUnsaved();
    }

    public void setRangeEnd() {
        if (recording != null)
            setRangeEndAt(playhead);
    }

    public void setRangeEndAt(float time) {

        if (!canEditExport())
            return;

        float end = Math.clamp(time, 0f, recording.getDuration());
        RecordingExportStruct exportStruct = recording.getExportStruct();
        exportStruct.setRangeEnd(end);

        if (exportStruct.hasRangeStart() && exportStruct.getRangeStart() > end)
            exportStruct.setRangeStart(EditorSetting.RECORDER_TIME_UNSET);

        markUnsaved();
    }

    private boolean canEditExport() {
        return recording != null && state != RecorderState.EXPORTING;
    }

    private int findIndex(int[] values, int value) {

        for (int i = 0; i < values.length; i++)
            if (values[i] == value)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Export \\

    // Renders the export range into a new video beside the recordings, frame by frame in the Dev window; a refusal
    // says why in the status
    public void export() {

        if (state == RecorderState.EXPORTING || refuseAnimation())
            return;

        this.exportResult = RecorderExportResult.REFUSED;

        if (state == RecorderState.RECORDING) {
            setStatus(EditorSetting.RECORDER_STATUS_EXPORT_WHILE_RECORDING);
            return;
        }

        if (clockManager.isPaused()) {
            setStatus(EditorSetting.RECORDER_STATUS_EXPORT_WHILE_PAUSED);
            return;
        }

        if (!hasClips() || !beginSession(true))
            return;

        RecordingExportStruct exportStruct = recording.getExportStruct();

        cancelDrag();
        saveRecording();
        exportDirectory.mkdirs();

        float duration = recording.getDuration();
        float span = exportStruct.resolveEnd(duration) - exportStruct.resolveStart(duration);

        this.exportFile = resolveVideoFile(exportStruct.getFormat());
        this.exportedFrames = 0;
        this.exportFrameCount = Math.max(1, Math.round(span * exportStruct.getFrameRate()));
        this.playhead = exportStruct.resolveStart(duration);
        this.state = RecorderState.EXPORTING;
        this.exportResult = RecorderExportResult.EXPORTING;
        setStatus(EditorSetting.RECORDER_STATUS_EXPORTING + exportFile.getName());
    }

    // The sounds of the export range decoded for its mix, true when it holds any to be heard
    public boolean prepareExportMix(int sampleRate) {

        RecordingExportStruct exportStruct = recording.getExportStruct();
        float duration = recording.getDuration();

        return recordingMixBranch.prepare(
                recording, exportStruct.resolveStart(duration), exportStruct.resolveEnd(duration), sampleRate);
    }

    // Every sound the timeline holds mixed over the frames of sound from the moment, both sides of each frame
    // side by side
    public void mixExportAudio(float time, short[] samples, int frameCount) {
        recordingMixBranch.mix(recording, time, samples, frameCount);
    }

    public void releaseExportMix() {
        recordingMixBranch.clear();
    }

    public void reportExportProgress(int exportedFrames) {
        this.exportedFrames = exportedFrames;
    }

    // The export's last word — the session ends either way, and a failed video is never left behind
    public void finishExport(boolean succeeded, String message) {

        if (state != RecorderState.EXPORTING)
            return;

        endSession();
        this.exportResult = succeeded ? RecorderExportResult.EXPORTED : RecorderExportResult.FAILED;

        if (!succeeded && exportFile.exists())
            exportFile.delete();

        setStatus(succeeded
                ? EditorSetting.RECORDER_STATUS_EXPORTED + exportFile.getAbsolutePath()
                : EditorSetting.RECORDER_STATUS_EXPORT_FAILED + message);
    }

    // The next free video named after the recording
    private File resolveVideoFile(VideoFormat format) {

        int number = EditorSetting.RECORDER_FIRST_NAME_NUMBER;
        File file;

        do {
            file = new File(exportDirectory, recording.getRecordingName()
                    + EditorSetting.RECORDER_VIDEO_NAME_SEPARATOR + number
                    + EditorSetting.RECORDER_EXTENSION_SEPARATOR + format.getExtension());
            number++;
        } while (file.exists());

        return file;
    }

    // Names \\

    private String resolveDefaultRecordingName() {

        int number = EditorSetting.RECORDER_FIRST_NAME_NUMBER;

        while (!isRecordingNameAvailable(EditorSetting.RECORDER_DEFAULT_NAME_PREFIX + number))
            number++;

        return EditorSetting.RECORDER_DEFAULT_NAME_PREFIX + number;
    }

    private String resolveTakeName() {

        int number = recording.getTakes().size() + 1;

        while (recording.hasTakeName(EditorSetting.RECORDER_TAKE_NAME_PREFIX + number))
            number++;

        return EditorSetting.RECORDER_TAKE_NAME_PREFIX + number;
    }

    // Whether a new media file may take the name
    public boolean isRecordingNameAvailable(String recordingName) {
        return FileUtility.isValidFileName(recordingName, EditorSetting.RECORDER_NAME_MAX_LENGTH)
                && !getRecordingFile(recordingName).exists();
    }

    public boolean isLabelValid(String label) {
        return !label.isBlank() && label.length() <= EditorSetting.RECORDER_LABEL_MAX_LENGTH;
    }

    // Every media file in the media directory beside the player's saves, video and music alike, most recently
    // changed first
    public ObjectArrayList<String> getRecordingNames() {

        ObjectArrayList<File> recordingFiles = new ObjectArrayList<>();
        File[] files = mediaDirectory.listFiles();

        if (files != null)
            for (File file : files)
                if (ArpgUtility.isArpgFile(file))
                    recordingFiles.add(file);

        recordingFiles.sort((first, second) -> Long.compare(second.lastModified(), first.lastModified()));

        ObjectArrayList<String> recordingNames = new ObjectArrayList<>(recordingFiles.size());

        for (int i = 0; i < recordingFiles.size(); i++)
            recordingNames.add(FileUtility.getFileName(recordingFiles.get(i)));

        return recordingNames;
    }

    private File getRecordingFile(String recordingName) {
        return ArpgUtility.resolveFile(mediaDirectory, recordingName);
    }

    // Utility \\

    private boolean hasClips() {

        if (recording != null && !recording.getClips().isEmpty())
            return true;

        setStatus(EditorSetting.RECORDER_STATUS_NOTHING_RECORDED);
        return false;
    }

    private boolean isTakeIndex(int takeIndex) {
        return recording != null && takeIndex >= 0 && takeIndex < recording.getTakes().size();
    }

    private boolean isClipIndex(int clipIndex) {
        return recording != null && clipIndex >= 0 && clipIndex < recording.getClips().size();
    }

    private boolean isNodeIndex(int nodeIndex) {
        return recording != null && nodeIndex >= 0 && nodeIndex < nodes.size();
    }

    private void clearSelection() {
        this.selectedTake = EngineSetting.INDEX_NOT_FOUND;
        this.selectedClip = EngineSetting.INDEX_NOT_FOUND;
        selectedClips.clear();
        clearNodeSelection();
        clearTransitionSelection();
    }

    // Every edit moves the nodes with it and is a step of the undo history
    private void markUnsaved() {
        this.unsaved = true;
        this.documentRevision++;
        rebuildNodes();
        revision++;
        recordingHistoryBranch.commit(recording);
    }

    // Curves laid from what a take recorded are kept as an edit is, but are no step of their own: a step that brings
    // back the recording without them has them laid the same way again
    private void markLaid() {
        this.unsaved = true;
        rebuildNodes();
        revision++;
        recordingHistoryBranch.rebase(recording);
    }

    private void setStatus(String statusText) {
        this.statusText = statusText;
        revision++;
    }

    // Accessible \\

    public boolean isSessionFor(DevContext context) {
        return devContext == context && state.isSession();
    }

    // True for the window whose sound system carries the session's sound: its Dev window, or for music without one
    // the Recorder tab it plays through
    public boolean isSoundHost(ContextPackage context) {
        return soundHost == context && state.isSession();
    }

    public boolean isLooping() {
        return looping;
    }

    // True while the game is paused under a running session, and for the few frames after
    public boolean isSuspended() {
        return suspended;
    }

    // True while the timeline's sounds are heard — playing or recording, and not suspended
    public boolean isSoundPlayed() {
        return (state == RecorderState.PLAYING || state == RecorderState.RECORDING) && !suspended;
    }

    // The channels a session plays from the timeline — every one the recording's kind plays but what a take records
    // live
    public int getPlayedChannels() {
        return state.isSession() ? RecorderChannel.ALL & ~getLiveChannels() : RecorderChannel.NONE;
    }

    public int getLiveChannels() {
        return isTaking() ? take.getChannelMask() : RecorderChannel.NONE;
    }

    // True while a take records the world live
    public boolean isTaking() {
        return state == RecorderState.RECORDING;
    }

    public RecorderState getState() {
        return state;
    }

    public float getPlayhead() {
        return playhead;
    }

    public void setExportPlayhead(float playhead) {
        if (state == RecorderState.EXPORTING)
            this.playhead = playhead;
    }

    public int getArmedChannels() {
        return armedChannels;
    }

    public boolean hasRecording() {
        return recording != null;
    }

    public RecordingInstance getRecording() {
        return recording;
    }

    public boolean isUnsaved() {
        return recording != null && recording.isAnimation()
                ? infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATIONS, recording.getAnimationName())
                : unsaved;
    }

    public RecordingTakeStruct getTake() {
        return take;
    }

    // The take's own time at the playhead
    public float getTakeTime() {
        return playhead - takeStart;
    }

    public int getTakeCount() {
        return takeCount;
    }

    public int getSelectedTake() {
        return selectedTake;
    }

    public int getSelectedClip() {
        return selectedClip;
    }

    // True when the clip is among those selected
    public boolean isClipSelected(int clipIndex) {
        return selectedClips.contains(clipIndex);
    }

    // The recordings the open one was opened inside, outermost first
    public ObjectArrayList<String> getParentNames() {
        return parentNames;
    }

    // How many lanes the open recording's timeline shows
    public int getLaneCount() {
        return recording != null ? recording.getShownLaneCount() : EditorSetting.RECORDER_SHOWN_LANES;
    }

    public int getSelectedLane() {
        return selectedLane;
    }

    public int getSelectedNode() {
        return selectedNode;
    }

    // The clip holding the selected transition, null when none is selected
    public RecordingClipStruct getSelectedTransitionClip() {
        return isTransitionTarget(selectedTransition)
                ? recording.getClips().get(recordingTransitionBranch.toClipIndex(selectedTransition))
                : null;
    }

    public RecorderClipEdge getSelectedTransitionEdge() {
        return isTransitionTarget(selectedTransition) ? recordingTransitionBranch.toEdge(selectedTransition) : null;
    }

    // The transition the clip's edge plays, the default standing in for one that no longer exists
    public TransitionHandle resolveTransitionHandle(RecordingClipStruct clip, RecorderClipEdge edge) {
        return recordingTransitionBranch.resolveHandle(clip, edge);
    }

    public ObjectArrayList<RecordingNodeStruct> getNodes() {
        return nodes;
    }

    public RecordingDragStruct getDrag() {
        return drag;
    }

    // True while the Cut tool is chosen for the timeline; a press on a clip then cuts it instead of taking hold of it
    public boolean isCutting() {
        return toolManager.getTool(getToolCanvas()) == EditorTool.CUT;
    }

    // What the Timeline's tools work on: the open media file's clips
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.RECORDER;
    }

    // True while the open media file is a video, so it exports as one; false for music, and while none is open
    public boolean hasPicture() {
        return recording != null && recording.hasPicture();
    }

    // The clip the cut tool is over, -1 for none or while it is not chosen
    public int getCutHoverClip() {
        return isCutting() ? cutHoverClip : EngineSetting.INDEX_NOT_FOUND;
    }

    public float getCutHoverTime() {
        return cutHoverTime;
    }

    public RecordingViewStruct getView() {
        return view;
    }

    public RecorderPane getPane() {
        return pane;
    }

    public ObjectArrayList<RecordingCurveTrackStruct> getKeyTracks() {
        return keyTracks;
    }

    // The track of the take the Keys tab shows, RECORDER_KEY_TRACK_SOUNDS for its Sounds layer, or
    // RECORDER_KEY_LAYER_NONE
    public int getKeyLayer() {
        return keyLayer;
    }

    public RecordingCurveTrackStruct getSelectedTrack() {
        return selectedTrack;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public RecordingCurveStruct getSelectedCurve() {
        return selectedTrack != null ? selectedTrack.getCurve(selectedSlot) : null;
    }

    public int getSelectedKey() {
        return selectedKey;
    }

    // The indices of every key selected on the selected curve, in order
    public IntArrayList getSelectedKeys() {
        return selectedKeys;
    }

    // The sounds of the take the Keys pane shows, null when it holds no sound channel
    public RecordingSoundTrackStruct getKeySoundTrack() {
        return keyTake != null ? keyTake.getSoundTrack() : null;
    }

    public int getSelectedSound() {
        return selectedSound;
    }

    public boolean isSoundHeardIn(RecordingClipStruct clip, int soundIndex) {
        return recordingSoundBranch.isHeardIn(clip, getKeySoundTrack(), soundIndex);
    }

    public int countSounds(int nameIndex) {
        return recordingSoundBranch.countSounds(getKeySoundTrack(), nameIndex);
    }

    // Moves whenever a sound is changed, so whatever the timeline sounds is started again as it now stands
    public int getSoundRevision() {
        return soundRevision;
    }

    public RecordingClipStruct getKeyClip() {
        return getSelectedClipStruct();
    }

    public double getGraphMinimum() {
        return graphMinimum;
    }

    public double getGraphMaximum() {
        return graphMaximum;
    }

    public int getKeysRevision() {
        return keysRevision;
    }

    public int getCurveRevision() {
        return curveRevision;
    }

    public int getViewRevision() {
        return viewRevision;
    }

    public File getExportFile() {
        return exportFile;
    }

    public int getExportedFrames() {
        return exportedFrames;
    }

    public int getExportFrameCount() {
        return exportFrameCount;
    }

    public RecorderExportResult getExportResult() {
        return exportResult;
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {
        return statusText;
    }
}
