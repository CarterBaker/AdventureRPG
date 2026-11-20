package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigIkChainStruct;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.recorderpipeline.recording.RecorderState;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationEditorManager extends ManagerPackage {

    /*
     * Owns the animation editor's shared state — the open clips, the active
     * one, the selected bone, the character it plays on (the player until
     * another is chosen) and the copied pose. Clips are ARPG entries owned by
     * InfoManager: selecting one there, or in the Tracks tab, opens it here
     * and on the recorder's timeline, every edit goes back through it, and
     * every change to an open clip, made here, on the Keys tabs or in the
     * Info Panel, rebuilds its preview live. The timeline is the recorder's:
     * its playhead is this editor's, stopped on whole frames, its transport
     * plays the clip, its history undoes every edit of it, and the bone its
     * Keys tab shows is the bone selected here and the other way round.
     * While a clip with a rig plays on a character, the viewport is a tool
     * canvas whose tools pose the selected bone, keying it at the playhead
     * as a drag goes, a whole drag one step. Edits, previews, panel values
     * and panel rows each live in their own branch.
     */

    // Internal
    private InfoManager infoManager;
    private EntityManager entityManager;
    private ToolManager toolManager;
    private RecorderManager recorderManager;
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

        // Active
        this.characterName = EngineSetting.DEFAULT_PLAYER_RACE;
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.entityManager = get(EntityManager.class);
        this.toolManager = get(ToolManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSelectionListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::openEntry);
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::refreshEntry);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_ANIMATIONS, this::closeEntry);
    }

    // Update \\

    // The bone the recorder's Keys tab shows is the bone selected here
    @Override
    protected void update() {

        if (activeDocument == null || !activeDocument.hasRig() || posing || !isOnTimeline())
            return;

        String boneName = recorderManager.getSelectedBoneName();
        RigHandle rigHandle = activeDocument.getRigHandle();

        if (boneName == null || !rigHandle.hasBone(boneName))
            return;

        int boneIndex = rigHandle.getBoneIndex(boneName);

        if (boneIndex == selectedBoneIndex)
            return;

        this.selectedBoneIndex = boneIndex;
        notifyChanged();
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
        this.selectedBoneIndex = 0;
        this.statusMessage = null;

        if (document != null) {
            resolveSelection();
            claimTimeline();
        }

        notifyChanged();
    }

    // The clip picked to be opened here and on the timeline, through InfoManager so every window follows
    public void openClip(String clipName) {
        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName, clipName);
    }

    // The active clip made the one the recorder's timeline holds, so its keys, playhead and history are this
    // editor's; false while no clip is active or it cannot be opened there
    private boolean claimTimeline() {

        if (activeDocument == null)
            return false;

        recorderManager.openAnimation(activeDocument.getClipName());
        return isOnTimeline();
    }

    private boolean isOnTimeline() {
        return activeDocument != null && recorderManager.isAnimationOpen(activeDocument.getClipName());
    }

    // Keeps the selected bone and the character on the rig the clip names now
    private void resolveSelection() {

        RigHandle rigHandle = activeDocument.getRigHandle();

        if (rigHandle == null || selectedBoneIndex >= rigHandle.getBoneCount())
            this.selectedBoneIndex = 0;

        ObjectArrayList<String> characterNames = getCharacterNames();

        if (characterNames.contains(characterName))
            return;

        if (characterNames.contains(EngineSetting.DEFAULT_PLAYER_RACE))
            this.characterName = EngineSetting.DEFAULT_PLAYER_RACE;
        else
            this.characterName = characterNames.isEmpty() ? null : characterNames.get(0);
    }

    public void createClip(String localName) {

        if (!isClipNameAvailable(localName))
            return;

        RigHandle characterRig = getCharacterRig();
        String rigName = activeDocument != null
                ? ArpgUtility.getString(
                        getClipArpg(),
                        EditorSetting.ANIMATION_EDITOR_FIELD_RIG,
                        EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG)
                : characterRig != null ? characterRig.getRigName() : EditorSetting.ANIMATION_EDITOR_DEFAULT_RIG;

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
        if (claimTimeline())
            recorderManager.togglePlayback();
    }

    // Stops on the frame the playhead had reached
    void pause() {
        if (isPlaying())
            recorderManager.pause();
    }

    public void toStart() {
        setTime(0f);
    }

    public void toEnd() {
        setTime(getTimelineSpan());
    }

    public void stepFrame(int direction) {
        setTime(getTime() + direction / EditorSetting.ANIMATION_EDITOR_FRAME_RATE);
    }

    // Snaps the timeline's playhead to the nearest whole frame within the clip
    void setTime(float time) {

        if (!claimTimeline())
            return;

        float frameRate = EditorSetting.ANIMATION_EDITOR_FRAME_RATE;
        float snapped = Math.round(CharacterViewUtility.clamp(time, 0f, getTimelineSpan()) * frameRate) / frameRate;

        pause();
        recorderManager.seek(CharacterViewUtility.round(snapped));
        notifyChanged();
    }

    // Character \\

    public ObjectArrayList<String> getCharacterNames() {

        RigHandle rigHandle = activeDocument != null ? activeDocument.getRigHandle() : null;

        return rigHandle != null ? entityManager.getTemplateNamesForRig(rigHandle) : new ObjectArrayList<>();
    }

    // The rig the character chosen is posed on, whose clips the Tracks tab lists; null with none chosen
    public RigHandle getCharacterRig() {
        return characterName != null && entityManager.hasTemplate(characterName)
                ? entityManager.getEntityHandleFromTemplateName(characterName).getEntityData().getRaceData()
                        .getRigHandle()
                : null;
    }

    // The clips posed on the rig of the character chosen, by name, as the Tracks tab lists them
    public void collectCharacterClips(ObjectArrayList<String> clipNames) {

        clipNames.clear();

        RigHandle rigHandle = getCharacterRig();

        if (rigHandle == null)
            return;

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(EditorSetting.INFO_SCHEMA_ANIMATIONS);

        for (int i = 0; i < entries.size(); i++)
            if (rigHandle.getRigName().equals(ArpgUtility.getString(
                    entries.get(i).getArpg(), EditorSetting.ANIMATION_EDITOR_FIELD_RIG, "")))
                clipNames.add(entries.get(i).getEntryName());

        clipNames.sort(String.CASE_INSENSITIVE_ORDER);
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

        if (claimTimeline())
            animationPropertyBranch.step(activeDocument, key, direction);
    }

    public void cycle(String key, int direction) {

        if (claimTimeline())
            animationPropertyBranch.cycle(activeDocument, key, direction);
    }

    public void act(String key) {

        if (claimTimeline())
            animationPropertyBranch.act(activeDocument, key);
    }

    public String getValueText(String key) {
        return activeDocument != null ? animationPropertyBranch.getValueText(activeDocument, key) : "";
    }

    public boolean isValueValid(String key, String text) {
        return activeDocument != null && animationPropertyBranch.isValueValid(activeDocument, key, text);
    }

    public void setValue(String key, String text) {

        if (isValueValid(key, text) && claimTimeline())
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

    public String getBoneName(int boneIndex) {
        return activeDocument.getRigHandle().getBone(boneIndex).getName();
    }

    // Whether a clip keys a bone of its rig: every bone but those a spring chain simulates
    public boolean isKeyedBone(int boneIndex) {
        return !activeDocument.getRigHandle().isSpringBone(boneIndex);
    }

    // Selection \\

    public void selectBone(int boneIndex) {

        if (activeDocument == null || !activeDocument.hasRig() || posing || boneIndex == selectedBoneIndex
                || boneIndex < 0 || boneIndex >= activeDocument.getRigHandle().getBoneCount()
                || !isKeyedBone(boneIndex))
            return;

        this.selectedBoneIndex = boneIndex;

        if (isOnTimeline())
            recorderManager.selectBoneLayer(getBoneName(boneIndex));

        notifyChanged();
    }

    // Pose \\

    // Opens a drag posing the selected bone at the playhead from how it stands there; false with nothing to pose
    public boolean beginPose() {

        if (getToolCanvas() == null || posing || !claimTimeline())
            return false;

        pause();
        recorderManager.beginGesture();

        this.poseStart = animationPropertyBranch.samplePose(activeDocument, selectedBoneIndex);
        this.posing = true;

        return true;
    }

    // Opens a drag posing an IK handle's root and middle bones at the playhead from how they stand there
    public boolean beginChainPose(RigIkChainStruct chain) {

        if (getToolCanvas() == null || posing || !claimTimeline())
            return false;

        pause();
        recorderManager.beginGesture();

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
        recorderManager.endGesture();
    }

    public void cancelPose() {

        if (!posing)
            return;

        this.posing = false;
        recorderManager.cancelGesture();
    }

    public boolean isPosing() {
        return posing;
    }

    // The selected bone's rotation, position and scale at the playhead when the drag began
    public float[] getPoseStart() {
        return poseStart;
    }

    // History \\

    // The recorder's history undoes and redoes every edit of the clip, made here or anywhere else
    public void undo() {
        if (!posing && claimTimeline())
            recorderManager.undo();
    }

    public void redo() {
        if (!posing && claimTimeline())
            recorderManager.redo();
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

    // The timeline's playhead while it holds the active clip
    public float getTime() {
        return isOnTimeline() ? Math.min(recorderManager.getPlayhead(), getTimelineSpan()) : 0f;
    }

    public boolean isPlaying() {
        return isOnTimeline() && recorderManager.getState() == RecorderState.PLAYING;
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
                .append(isPlaying()
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
