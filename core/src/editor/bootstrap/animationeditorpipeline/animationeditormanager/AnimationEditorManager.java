package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigIkChainStruct;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationEditorManager extends ManagerPackage {

    /*
     * Owns the animation editor's shared state — the open clips, the active
     * one, the selected bone, the character it plays on, the playhead and the
     * copied pose. Clips are ARPG entries owned by InfoManager: selecting one
     * there opens it here, every edit goes back through it, and every change
     * to an open clip, made here or in the Info Panel, rebuilds its preview
     * live. The playhead stops on whole frames; playback runs it freely and
     * loops or holds at the end as the clip does. While a clip with a rig
     * plays on a character, the viewport is a tool canvas whose tools pose
     * the selected bone, keying it at the playhead as a drag goes and
     * undoing a whole drag at once; every edit of the clip has its undo.
     * Edits, previews, panel values and panel rows each live in their own
     * branch.
     */

    // Internal
    private InfoManager infoManager;
    private EntityManager entityManager;
    private ToolManager toolManager;
    private AnimationEditBranch animationEditBranch;
    private AnimationPreviewBranch animationPreviewBranch;
    private AnimationPropertyBranch animationPropertyBranch;
    private AnimationRowBranch animationRowBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, AnimationDocumentInstance> clipName2AnimationDocument;

    // Active
    private AnimationDocumentInstance activeDocument;
    private int selectedBoneIndex;
    private String characterName;

    // Playback
    private float time;
    private boolean playing;

    // Clipboard
    private ObjectArrayList<float[]> clipboardPoses;
    private RigHandle clipboardRig;

    // Pose
    private float[] poseStart;
    private float[][] chainPoseStarts;
    private boolean posing;
    private ObjectArrayList<String> poseBoneNames;
    private ObjectArrayList<float[]> posePoses;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.animationEditBranch = create(AnimationEditBranch.class);
        this.animationPreviewBranch = create(AnimationPreviewBranch.class);
        this.animationPropertyBranch = create(AnimationPropertyBranch.class);
        this.animationRowBranch = create(AnimationRowBranch.class);

        // Palette
        this.clipName2AnimationDocument = new Object2ObjectOpenHashMap<>();

        // Clipboard
        this.clipboardPoses = new ObjectArrayList<>();

        // Pose
        this.poseBoneNames = new ObjectArrayList<>();
        this.posePoses = new ObjectArrayList<>();
        this.chainPoseStarts = new float[2][];
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.entityManager = get(EntityManager.class);
        this.toolManager = get(ToolManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::closeEntry);
    }

    // Update \\

    @Override
    protected void update() {

        if (!playing || activeDocument == null)
            return;

        float duration = activeDocument.getDuration();

        if (duration <= 0f) {
            pause();
            return;
        }

        time += internal.getDeltaTime();

        if (time <= duration)
            return;

        if (activeDocument.getPreviewClip().isLooping())
            time %= duration;
        else {
            time = duration;
            pause();
        }
    }

    // Clips \\

    private void openEntry(InfoEntryStruct entry) {

        AnimationDocumentInstance document = clipName2AnimationDocument.get(entry.getEntryName());

        if (document == null) {
            document = create(AnimationDocumentInstance.class);
            document.constructor(entry.getEntryName());
            clipName2AnimationDocument.put(entry.getEntryName(), document);
            animationPreviewBranch.rebuild(document, entry.getArpg());
        }

        if (document != activeDocument)
            activate(document);
    }

    private void refreshEntry(InfoEntryStruct entry) {

        AnimationDocumentInstance document = clipName2AnimationDocument.get(entry.getEntryName());

        if (document == null)
            return;

        animationPreviewBranch.rebuild(document, entry.getArpg());

        if (document == activeDocument)
            resolveSelection();

        notifyChanged();
    }

    private void closeEntry(InfoEntryStruct entry) {

        AnimationDocumentInstance document = clipName2AnimationDocument.remove(entry.getEntryName());

        if (document != null && document == activeDocument)
            activate(null);
    }

    private void activate(AnimationDocumentInstance document) {

        this.activeDocument = document;
        this.time = 0f;
        this.playing = false;
        this.selectedBoneIndex = 0;
        this.statusMessage = null;

        if (document != null)
            resolveSelection();

        notifyChanged();
    }

    // Keeps the selected bone and the character on the rig the clip names now
    private void resolveSelection() {

        RigHandle rigHandle = activeDocument.getRigHandle();

        if (rigHandle == null || selectedBoneIndex >= rigHandle.getBoneCount())
            this.selectedBoneIndex = 0;

        ObjectArrayList<String> characterNames = getCharacterNames();

        if (!characterNames.contains(characterName))
            this.characterName = characterNames.isEmpty() ? null : characterNames.get(0);
    }

    public void createClip(String localName) {

        if (!isClipNameAvailable(localName))
            return;

        String rigName = activeDocument != null
                ? ArpgUtility.getString(
                        getClipArpg(),
                        EditorSetting.ANIMATION_EDITOR_FIELD_RIG,
                        EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG)
                : EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG;

        infoManager.createDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, toSiblingName(localName), root -> {
            root.addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_RIG, rigName);
            root.addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_LOOP, false);
            root.add(EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS, new ArpgObjectStruct());
        });
        setStatusMessage(EditorSetting.ANIMATION_EDITOR_MESSAGE_CREATED + localName);
    }

    public void saveActiveClip() {

        if (activeDocument == null)
            return;

        setStatusMessage(infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, activeDocument.getClipName())
                ? EditorSetting.ANIMATION_EDITOR_MESSAGE_SAVED + activeDocument.getClipName()
                : infoManager.getRefusal());
    }

    public void revertActiveClip() {

        if (activeDocument == null)
            return;

        cancelPose();
        activeDocument.getHistory().clear();
        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, activeDocument.getClipName());
        setStatusMessage(EditorSetting.ANIMATION_EDITOR_MESSAGE_REVERTED + activeDocument.getClipName());
    }

    public void deleteActiveClip() {

        if (activeDocument == null)
            return;

        String clipName = activeDocument.getClipName();

        setStatusMessage(infoManager.deleteDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName)
                ? EditorSetting.ANIMATION_EDITOR_MESSAGE_DELETED + clipName
                : infoManager.getRefusal());
    }

    public boolean isClipNameAvailable(String localName) {
        return FileUtility.isValidFileName(localName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, toSiblingName(localName));
    }

    public boolean isActiveClipName(String localName) {
        return activeDocument != null && toLocalName(activeDocument.getClipName()).equals(localName);
    }

    public String getActiveLocalName() {
        return activeDocument != null ? toLocalName(activeDocument.getClipName()) : "";
    }

    // A new clip beside the active one, in the same folder, or at the top when none is open
    private String toSiblingName(String localName) {

        if (activeDocument == null)
            return localName;

        String clipName = activeDocument.getClipName();
        int separator = clipName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR);

        return separator == EngineSetting.INDEX_NOT_FOUND
                ? localName
                : clipName.substring(0, separator + EditorSetting.INFO_FOLDER_SEPARATOR.length()) + localName;
    }

    private String toLocalName(String clipName) {
        return clipName.substring(clipName.lastIndexOf(EditorSetting.INFO_FOLDER_SEPARATOR) + 1);
    }

    // Playback \\

    public void togglePlayback() {

        if (activeDocument == null || activeDocument.getDuration() <= 0f)
            return;

        if (playing) {
            pause();
            return;
        }

        if (time >= activeDocument.getDuration())
            this.time = 0f;

        this.playing = true;
        notifyChanged();
    }

    // Stops on the frame the playhead had reached
    void pause() {

        this.playing = false;
        setTime(time);
    }

    public void toStart() {

        this.playing = false;
        setTime(0f);
    }

    public void toEnd() {

        this.playing = false;
        setTime(getTimelineSpan());
    }

    public void stepFrame(int direction) {

        this.playing = false;
        setTime(time + direction / EditorSetting.ANIMATION_EDITOR_FRAME_RATE);
    }

    public void scrub(float fraction) {

        this.playing = false;
        setTime(CharacterViewUtility.clamp(fraction, 0f, 1f) * getTimelineSpan());
    }

    // Snaps the playhead to the nearest whole frame within the timeline
    void setTime(float time) {

        float frameRate = EditorSetting.ANIMATION_EDITOR_FRAME_RATE;
        float snapped = Math.round(CharacterViewUtility.clamp(time, 0f, getTimelineSpan()) * frameRate) / frameRate;

        this.time = CharacterViewUtility.round(snapped);
        notifyChanged();
    }

    // Character \\

    public ObjectArrayList<String> getCharacterNames() {

        RigHandle rigHandle = activeDocument != null ? activeDocument.getRigHandle() : null;

        return rigHandle != null ? entityManager.getTemplateNamesForRig(rigHandle) : new ObjectArrayList<>();
    }

    void cycleCharacter(int direction) {

        ObjectArrayList<String> characterNames = getCharacterNames();

        if (characterNames.isEmpty())
            return;

        int current = Math.max(0, characterNames.indexOf(characterName));

        this.characterName = characterNames.get(Math.floorMod(current + direction, characterNames.size()));
        notifyChanged();
    }

    // Panel \\

    public void select(String key) {

        if (activeDocument != null)
            animationPropertyBranch.select(activeDocument, key);
    }

    public void step(String key, int direction) {

        if (activeDocument != null)
            animationPropertyBranch.step(activeDocument, key, direction);
    }

    public void cycle(String key, int direction) {

        if (activeDocument != null)
            animationPropertyBranch.cycle(activeDocument, key, direction);
    }

    public void act(String key) {

        if (activeDocument != null)
            animationPropertyBranch.act(activeDocument, key);
    }

    public String getValueText(String key) {
        return activeDocument != null ? animationPropertyBranch.getValueText(activeDocument, key) : "";
    }

    public boolean isValueValid(String key, String text) {
        return activeDocument != null && animationPropertyBranch.isValueValid(activeDocument, key, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text))
            animationPropertyBranch.setValue(activeDocument, key, text);
    }

    public void buildListRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            animationRowBranch.buildListRows(activeDocument, rows);
    }

    public void buildPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        if (activeDocument != null)
            animationRowBranch.buildPropertyRows(activeDocument, rows);
    }

    // Times of every key on the timeline: the selected bone's, and every other bone's apart from those
    public void collectKeyTimes(FloatArrayList selectedTimes, FloatArrayList otherTimes) {

        if (activeDocument != null)
            animationPropertyBranch.collectKeyTimes(activeDocument, selectedTimes, otherTimes);
    }

    public void collectBoneKeyTimes(int boneIndex, FloatArrayList times) {

        times.clear();

        if (activeDocument != null)
            animationPropertyBranch.collectBoneKeyTimes(activeDocument, boneIndex, times);
    }

    // The bones of the clip's rig a timeline lists, none while the clip names no rig the engine knows
    public int getBoneCount() {
        return activeDocument != null && activeDocument.hasRig() ? activeDocument.getRigHandle().getBoneCount() : 0;
    }

    public String getBoneName(int boneIndex) {
        return activeDocument.getRigHandle().getBone(boneIndex).getName();
    }

    // Selection \\

    public void selectBone(int boneIndex) {

        if (activeDocument == null || !activeDocument.hasRig() || posing || boneIndex == selectedBoneIndex
                || boneIndex < 0 || boneIndex >= activeDocument.getRigHandle().getBoneCount())
            return;

        this.selectedBoneIndex = boneIndex;
        notifyChanged();
    }

    // Pose \\

    // Opens a drag posing the selected bone at the playhead from how it stands there; false with nothing to pose
    public boolean beginPose() {

        if (getToolCanvas() == null || posing)
            return false;

        pause();
        animationEditBranch.beginGesture(activeDocument);

        this.poseStart = animationPropertyBranch.samplePose(activeDocument, selectedBoneIndex);
        this.posing = true;

        return true;
    }

    // Opens a drag posing an IK handle's root and middle bones at the playhead from how they stand there
    public boolean beginChainPose(RigIkChainStruct chain) {

        if (getToolCanvas() == null || posing)
            return false;

        pause();
        animationEditBranch.beginGesture(activeDocument);

        chainPoseStarts[0] = animationPropertyBranch.samplePose(activeDocument, chain.getRootBone());
        chainPoseStarts[1] = animationPropertyBranch.samplePose(activeDocument, chain.getMiddleBone());
        this.posing = true;

        return true;
    }

    // An IK handle's root and middle bones keyed together at the playhead, one pose each
    public void writeChainPose(RigIkChainStruct chain, float[] rootPose, float[] middlePose) {

        if (!posing)
            return;

        poseBoneNames.clear();
        poseBoneNames.add(activeDocument.getRigHandle().getBone(chain.getRootBone()).getName());
        poseBoneNames.add(activeDocument.getRigHandle().getBone(chain.getMiddleBone()).getName());
        posePoses.clear();
        posePoses.add(rootPose);
        posePoses.add(middlePose);
        animationEditBranch.writeKeys(activeDocument, poseBoneNames, getTime(), posePoses);
    }

    // A handle's root (0) or middle (1) bone's pose at the playhead when its drag began
    public float[] getChainPoseStart(int link) {
        return chainPoseStarts[link];
    }

    // The selected bone keyed at the playhead holding a pose of every channel
    public void writePose(float[] pose) {

        if (!posing)
            return;

        poseBoneNames.clear();
        poseBoneNames.add(activeDocument.getRigHandle().getBone(selectedBoneIndex).getName());
        posePoses.clear();
        posePoses.add(pose);
        animationEditBranch.writeKeys(activeDocument, poseBoneNames, getTime(), posePoses);
    }

    public void endPose() {

        if (!posing)
            return;

        this.posing = false;
        animationEditBranch.endGesture(activeDocument);
    }

    public void cancelPose() {

        if (!posing)
            return;

        this.posing = false;
        animationEditBranch.cancelGesture(activeDocument);
    }

    public boolean isPosing() {
        return posing;
    }

    // The selected bone's rotation, position and scale at the playhead when the drag began
    public float[] getPoseStart() {
        return poseStart;
    }

    // History \\

    public void undo() {

        if (activeDocument == null || posing)
            return;

        setStatusMessage(animationEditBranch.undo(activeDocument)
                ? EditorSetting.ANIMATION_EDITOR_MESSAGE_UNDONE
                : EditorSetting.ANIMATION_EDITOR_MESSAGE_NOTHING_TO_UNDO);
    }

    public void redo() {

        if (activeDocument == null || posing)
            return;

        setStatusMessage(animationEditBranch.redo(activeDocument)
                ? EditorSetting.ANIMATION_EDITOR_MESSAGE_REDONE
                : EditorSetting.ANIMATION_EDITOR_MESSAGE_NOTHING_TO_REDO);
    }

    // Clipboard \\

    void setClipboard(RigHandle rigHandle, ObjectArrayList<float[]> poses) {

        this.clipboardRig = rigHandle;
        clipboardPoses.clear();
        clipboardPoses.addAll(poses);
        notifyChanged();
    }

    boolean hasClipboard(RigHandle rigHandle) {
        return clipboardRig != null && clipboardRig == rigHandle;
    }

    ObjectArrayList<float[]> getClipboardPoses() {
        return clipboardPoses;
    }

    // Status \\

    void notifyChanged() {
        revision++;
    }

    void setStatusMessage(String statusMessage) {

        this.statusMessage = statusMessage;
        notifyChanged();
    }

    // Accessible \\

    public AnimationDocumentInstance getActiveDocument() {
        return activeDocument;
    }

    public boolean hasActiveDocument() {
        return activeDocument != null;
    }

    // The active clip's live ARPG, as InfoManager holds it
    ArpgObjectStruct getClipArpg() {

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_ANIMATIONS,
                activeDocument.getClipName(),
                activeDocument.getClipName());

        return entry != null ? entry.getArpg() : null;
    }

    public int getSelectedBoneIndex() {
        return selectedBoneIndex;
    }

    public String getCharacterName() {
        return characterName;
    }

    public float getTime() {
        return Math.min(time, getTimelineSpan());
    }

    public boolean isPlaying() {
        return playing;
    }

    // The clip's length, or a floor that leaves room to key a clip that has none yet
    public float getTimelineSpan() {

        float duration = activeDocument != null ? activeDocument.getDuration() : 0f;
        return Math.max(duration, EditorSetting.ANIMATION_EDITOR_TIMELINE_MIN_SPAN);
    }

    // What the viewport's tools work on: the pose while a clip with a rig plays on a character, nothing otherwise
    public ToolCanvas getToolCanvas() {
        return activeDocument != null && activeDocument.hasRig() && characterName != null
                ? ToolCanvas.ANIMATION_POSE
                : null;
    }

    public EditorTool getPoseTool() {
        return toolManager.getTool(ToolCanvas.ANIMATION_POSE);
    }

    public int getRevision() {
        return revision;
    }

    public String getStatusText() {

        StringBuilder status = new StringBuilder();

        if (activeDocument == null)
            status.append(EditorSetting.ANIMATION_EDITOR_STATUS_NO_CLIP);
        else
            appendDocumentStatus(status);

        if (statusMessage != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(statusMessage);

        return status.toString();
    }

    private void appendDocumentStatus(StringBuilder status) {

        status.append(activeDocument.getClipName());

        if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATIONS, activeDocument.getClipName()))
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                .append(CharacterViewUtility.formatNumber(getTime()))
                .append(EditorSetting.ANIMATION_EDITOR_TIME_SEPARATOR)
                .append(CharacterViewUtility.formatNumber(activeDocument.getDuration()))
                .append(EditorSetting.ANIMATION_EDITOR_TIME_SUFFIX);
        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                .append(playing
                        ? EditorSetting.ANIMATION_EDITOR_STATUS_PLAYING
                        : EditorSetting.ANIMATION_EDITOR_STATUS_PAUSED);

        if (!activeDocument.hasRig())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.ANIMATION_EDITOR_STATUS_NO_RIG);
        else if (characterName == null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.ANIMATION_EDITOR_STATUS_NO_CHARACTER);

        else
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.ANIMATION_EDITOR_STATUS_POSE_HINT);

        if (activeDocument.hasPreviewError())
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_VIEW_MESSAGE_PREVIEW_REFUSED)
                    .append(activeDocument.getPreviewError());
    }
}
