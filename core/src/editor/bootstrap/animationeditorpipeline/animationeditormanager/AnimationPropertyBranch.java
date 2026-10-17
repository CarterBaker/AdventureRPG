package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animation.AnimationClipUtility;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.animationeditorpipeline.util.AnimationEditorChannel;
import editor.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationPropertyBranch extends BranchPackage {

    /*
     * The one place every value in the animation editor's panels is read,
     * stepped, typed, cycled or acted on, by the key its row carries. A bone's
     * pose at the playhead reads straight from its key when it has one there,
     * and otherwise is sampled from the preview clip exactly as the game
     * samples it. Changing a value where the bone has no key keys it there
     * first, holding the pose it showed. Mirroring swaps every left bone
     * with its right and flips the pose across the body's middle.
     */

    // Internal
    private AnimationEditorManager animationEditorManager;
    private AnimationEditBranch animationEditBranch;

    // Scratch
    private Vector3 positionScratch;
    private Vector3 rotationScratch;
    private Vector3 scaleScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.positionScratch = new Vector3();
        this.rotationScratch = new Vector3();
        this.scaleScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.animationEditorManager = get(AnimationEditorManager.class);
        this.animationEditBranch = get(AnimationEditBranch.class);
    }

    // Selection \\

    void select(AnimationDocumentInstance document, String key) {

        if (CharacterViewUtility.getKeyName(key).equals(EditorSetting.ANIMATION_EDITOR_KEY_BONE))
            animationEditorManager.selectBone(CharacterViewUtility.getKeyIndex(key));
    }

    // Values \\

    String getValueText(AnimationDocumentInstance document, String key) {

        String keyName = CharacterViewUtility.getKeyName(key);
        AnimationEditorChannel channel = AnimationEditorChannel.fromField(keyName);

        if (channel != null)
            return document.hasRig() ? CharacterViewUtility.formatNumber(getChannelValue(document, key)) : "";

        return switch (keyName) {
            case EditorSetting.ANIMATION_EDITOR_KEY_CHARACTER -> animationEditorManager.getCharacterName() != null
                    ? animationEditorManager.getCharacterName()
                    : EditorSetting.CHARACTER_VIEW_NONE;
            case EditorSetting.ANIMATION_EDITOR_KEY_LOOP -> isLooping()
                    ? EditorSetting.CHARACTER_VIEW_YES
                    : EditorSetting.CHARACTER_VIEW_NO;
            default -> "";
        };
    }

    boolean isValueValid(AnimationDocumentInstance document, String key, String text) {
        return AnimationEditorChannel.fromField(CharacterViewUtility.getKeyName(key)) != null
                && document.hasRig()
                && CharacterViewUtility.isNumber(text);
    }

    void setValue(AnimationDocumentInstance document, String key, String text) {

        animationEditorManager.pause();
        writeComponent(document, key, CharacterViewUtility.parseNumber(text));
    }

    void step(AnimationDocumentInstance document, String key, int direction) {

        AnimationEditorChannel channel = AnimationEditorChannel.fromField(CharacterViewUtility.getKeyName(key));

        if (channel == null || !document.hasRig())
            return;

        animationEditorManager.pause();
        writeComponent(document, key, getChannelValue(document, key) + direction * channel.getStep());
    }

    // One axis of one channel of the selected bone at the playhead
    private float getChannelValue(AnimationDocumentInstance document, String key) {

        AnimationEditorChannel channel = AnimationEditorChannel.fromField(CharacterViewUtility.getKeyName(key));
        float[] pose = samplePose(document, animationEditorManager.getSelectedBoneIndex());

        return pose[channel.toPoseIndex(CharacterViewUtility.getKeyIndex(key))];
    }

    private void writeComponent(AnimationDocumentInstance document, String key, float value) {

        AnimationEditorChannel channel = AnimationEditorChannel.fromField(CharacterViewUtility.getKeyName(key));
        int boneIndex = animationEditorManager.getSelectedBoneIndex();
        float written = channel == AnimationEditorChannel.SCALE
                ? Math.max(EditorSetting.ANIMATION_EDITOR_SCALE_MIN, value)
                : value;

        animationEditBranch.setKeyComponent(
                document,
                document.getRigHandle().getBone(boneIndex).getName(),
                animationEditorManager.getTime(),
                samplePose(document, boneIndex),
                channel,
                CharacterViewUtility.getKeyIndex(key),
                written);
    }

    // Pose \\

    // A bone's rotation, position and scale at the playhead, read from its key there or sampled between keys
    float[] samplePose(AnimationDocumentInstance document, int boneIndex) {

        float[] pose = new float[AnimationEditorChannel.getPoseLength()];
        ArpgObjectStruct key = findKeyAt(document, boneIndex);

        if (key != null) {
            readKey(key, pose);
            return pose;
        }

        if (!document.hasPreviewClip()) {
            writeDefaults(pose);
            return pose;
        }

        AnimationClipHandle clip = document.getPreviewClip();
        float phase = CharacterViewUtility.clamp(animationEditorManager.getTime() / clip.getDuration(), 0f, 1f);

        AnimationClipUtility.sampleClip(clip, phase, boneIndex, positionScratch, rotationScratch, scaleScratch);
        writeVector(pose, AnimationEditorChannel.ROTATION, rotationScratch);
        writeVector(pose, AnimationEditorChannel.POSITION, positionScratch);
        writeVector(pose, AnimationEditorChannel.SCALE, scaleScratch);

        return pose;
    }

    private void readKey(ArpgObjectStruct key, float[] pose) {

        for (AnimationEditorChannel channel : AnimationEditorChannel.VALUES)
            for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
                pose[channel.toPoseIndex(axis)] = CharacterViewUtility.getComponent(
                        key, channel.getField(), axis, channel.getDefaultValue());
    }

    private void writeDefaults(float[] pose) {

        for (AnimationEditorChannel channel : AnimationEditorChannel.VALUES)
            for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
                pose[channel.toPoseIndex(axis)] = channel.getDefaultValue();
    }

    private void writeVector(float[] pose, AnimationEditorChannel channel, Vector3 vector) {
        pose[channel.toPoseIndex(0)] = vector.x;
        pose[channel.toPoseIndex(1)] = vector.y;
        pose[channel.toPoseIndex(2)] = vector.z;
    }

    // The same pose seen in a mirror down the body's middle: turns about the up-down axis and the facing axis
    // reverse, and sideways moves reverse
    private float[] mirrorPose(float[] pose) {

        float[] mirrored = pose.clone();
        int yaw = AnimationEditorChannel.ROTATION.toPoseIndex(1);
        int roll = AnimationEditorChannel.ROTATION.toPoseIndex(2);
        int sideways = AnimationEditorChannel.POSITION.toPoseIndex(0);

        mirrored[yaw] = -pose[yaw];
        mirrored[roll] = -pose[roll];
        mirrored[sideways] = -pose[sideways];

        return mirrored;
    }

    private boolean isRestPose(float[] pose) {

        for (AnimationEditorChannel channel : AnimationEditorChannel.VALUES)
            for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
                if (CharacterViewUtility.round(pose[channel.toPoseIndex(axis)]) != channel.getDefaultValue())
                    return false;

        return true;
    }

    // Choices \\

    void cycle(AnimationDocumentInstance document, String key, int direction) {

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_EDITOR_KEY_CHARACTER -> animationEditorManager.cycleCharacter(direction);
            case EditorSetting.ANIMATION_EDITOR_KEY_LOOP -> animationEditBranch.setLoop(document, !isLooping());
            default -> {
            }
        }
    }

    private boolean isLooping() {
        return ArpgUtility.getBoolean(
                animationEditorManager.getClipArpg(),
                EditorSetting.ANIMATION_EDITOR_FIELD_LOOP,
                false);
    }

    // Actions \\

    void act(AnimationDocumentInstance document, String key) {

        if (!document.hasRig())
            return;

        animationEditorManager.pause();

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_EDITOR_KEY_ADD_KEY -> keyBones(document, collectSelectedBone(document));
            case EditorSetting.ANIMATION_EDITOR_KEY_DELETE_KEY -> deleteKey(document);
            case EditorSetting.ANIMATION_EDITOR_KEY_PREVIOUS_KEY -> jumpToKey(document, -1);
            case EditorSetting.ANIMATION_EDITOR_KEY_NEXT_KEY -> jumpToKey(document, 1);
            case EditorSetting.ANIMATION_EDITOR_KEY_KEY_EARLIER -> moveKey(document, -1);
            case EditorSetting.ANIMATION_EDITOR_KEY_KEY_LATER -> moveKey(document, 1);
            case EditorSetting.ANIMATION_EDITOR_KEY_KEY_POSE -> keyBones(document, collectKeyedBones(document));
            case EditorSetting.ANIMATION_EDITOR_KEY_COPY_POSE -> copyPose(document);
            case EditorSetting.ANIMATION_EDITOR_KEY_PASTE_POSE -> pastePose(document);
            case EditorSetting.ANIMATION_EDITOR_KEY_MIRROR_POSE -> mirrorPose(document);
            default -> {
            }
        }
    }

    private void keyBones(AnimationDocumentInstance document, ObjectArrayList<String> boneNames) {

        RigHandle rigHandle = document.getRigHandle();
        ObjectArrayList<float[]> poses = new ObjectArrayList<>(boneNames.size());

        for (int i = 0; i < boneNames.size(); i++)
            poses.add(samplePose(document, rigHandle.getBoneIndex(boneNames.get(i))));

        animationEditBranch.writeKeys(document, boneNames, animationEditorManager.getTime(), poses);
    }

    private void deleteKey(AnimationDocumentInstance document) {

        int boneIndex = animationEditorManager.getSelectedBoneIndex();
        String boneName = document.getRigHandle().getBone(boneIndex).getName();
        int keyIndex = findKeyIndexAt(document, boneIndex);

        if (keyIndex != EngineSetting.INDEX_NOT_FOUND)
            animationEditBranch.deleteKey(document, boneName, keyIndex);
    }

    // Moves the playhead to the selected bone's nearest key before or after it
    private void jumpToKey(AnimationDocumentInstance document, int direction) {

        ArpgArrayStruct track = findBoneTrack(document, animationEditorManager.getSelectedBoneIndex());

        if (track == null)
            return;

        float time = animationEditorManager.getTime();
        float target = Float.NaN;

        for (int i = 0; i < track.size(); i++) {

            float keyTime = animationEditBranch.getKeyTime(track, i);
            boolean beyond = direction < 0
                    ? keyTime < time - EditorSetting.ANIMATION_EDITOR_KEY_EPSILON
                    : keyTime > time + EditorSetting.ANIMATION_EDITOR_KEY_EPSILON;

            if (beyond && (Float.isNaN(target) || (direction < 0 ? keyTime > target : keyTime < target)))
                target = keyTime;
        }

        if (!Float.isNaN(target))
            animationEditorManager.setTime(target);
    }

    // Moves the selected bone's key at the playhead one frame, never past its neighbours or before the start
    private void moveKey(AnimationDocumentInstance document, int direction) {

        int boneIndex = animationEditorManager.getSelectedBoneIndex();
        int keyIndex = findKeyIndexAt(document, boneIndex);
        ArpgArrayStruct track = findBoneTrack(document, boneIndex);

        if (keyIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        float frame = 1f / EditorSetting.ANIMATION_EDITOR_FRAME_RATE;
        float epsilon = EditorSetting.ANIMATION_EDITOR_KEY_EPSILON;
        float time = CharacterViewUtility.round(animationEditBranch.getKeyTime(track, keyIndex) + direction * frame);
        boolean afterPrevious = keyIndex == 0
                || time > animationEditBranch.getKeyTime(track, keyIndex - 1) + epsilon;
        boolean beforeNext = keyIndex == track.size() - 1
                || time < animationEditBranch.getKeyTime(track, keyIndex + 1) - epsilon;

        if (time < 0f || !afterPrevious || !beforeNext)
            return;

        animationEditBranch.setKeyTime(document, document.getRigHandle().getBone(boneIndex).getName(), keyIndex, time);
        animationEditorManager.setTime(time);
    }

    private void copyPose(AnimationDocumentInstance document) {

        RigHandle rigHandle = document.getRigHandle();
        ObjectArrayList<float[]> poses = new ObjectArrayList<>(rigHandle.getBoneCount());

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            poses.add(samplePose(document, i));

        animationEditorManager.setClipboard(rigHandle, poses);
        animationEditorManager.setStatusMessage(EditorSetting.ANIMATION_EDITOR_MESSAGE_POSE_COPIED);
    }

    // Keys every bone the copied pose moves, and every bone already keyed, to the copied pose
    private void pastePose(AnimationDocumentInstance document) {

        RigHandle rigHandle = document.getRigHandle();

        if (!animationEditorManager.hasClipboard(rigHandle))
            return;

        ObjectArrayList<float[]> clipboard = animationEditorManager.getClipboardPoses();
        ObjectArrayList<String> boneNames = new ObjectArrayList<>();
        ObjectArrayList<float[]> poses = new ObjectArrayList<>();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            String boneName = rigHandle.getBone(i).getName();

            if (!isRestPose(clipboard.get(i)) || isKeyed(boneName)) {
                boneNames.add(boneName);
                poses.add(clipboard.get(i));
            }
        }

        animationEditBranch.writeKeys(document, boneNames, animationEditorManager.getTime(), poses);
    }

    // Keys every bone that it or its mirror partner has keyed to its partner's pose, flipped
    private void mirrorPose(AnimationDocumentInstance document) {

        RigHandle rigHandle = document.getRigHandle();
        ObjectArrayList<float[]> sampled = new ObjectArrayList<>(rigHandle.getBoneCount());
        ObjectArrayList<String> boneNames = new ObjectArrayList<>();
        ObjectArrayList<float[]> poses = new ObjectArrayList<>();

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            sampled.add(samplePose(document, i));

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            String boneName = rigHandle.getBone(i).getName();
            int partner = resolveMirrorBone(rigHandle, i);

            if (!isKeyed(boneName) && !isKeyed(rigHandle.getBone(partner).getName()))
                continue;

            boneNames.add(boneName);
            poses.add(mirrorPose(sampled.get(partner)));
        }

        animationEditBranch.writeKeys(document, boneNames, animationEditorManager.getTime(), poses);
    }

    // A left bone's partner is the right bone of the same name, and the other way round; a middle bone is its own
    private int resolveMirrorBone(RigHandle rigHandle, int boneIndex) {

        String boneName = rigHandle.getBone(boneIndex).getName();
        String partnerName = swapSuffix(
                boneName,
                EditorSetting.ANIMATION_EDITOR_LEFT_SUFFIX,
                EditorSetting.ANIMATION_EDITOR_RIGHT_SUFFIX);

        if (partnerName == null)
            partnerName = swapSuffix(
                    boneName,
                    EditorSetting.ANIMATION_EDITOR_RIGHT_SUFFIX,
                    EditorSetting.ANIMATION_EDITOR_LEFT_SUFFIX);

        return partnerName != null && rigHandle.hasBone(partnerName) ? rigHandle.getBoneIndex(partnerName) : boneIndex;
    }

    private String swapSuffix(String boneName, String suffix, String replacement) {
        return boneName.endsWith(suffix)
                ? boneName.substring(0, boneName.length() - suffix.length()) + replacement
                : null;
    }

    // Timeline \\

    void collectKeyTimes(AnimationDocumentInstance document, FloatArrayList selectedTimes, FloatArrayList otherTimes) {

        selectedTimes.clear();
        otherTimes.clear();

        ArpgObjectStruct tracks = CharacterViewUtility.findObject(
                animationEditorManager.getClipArpg(),
                EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);

        if (tracks == null || !document.hasRig())
            return;

        String selectedBone = document.getRigHandle().getBone(animationEditorManager.getSelectedBoneIndex()).getName();
        ArpgArrayStruct selectedTrack = animationEditBranch.findTrack(
                animationEditorManager.getClipArpg(),
                selectedBone);

        if (selectedTrack != null)
            for (int i = 0; i < selectedTrack.size(); i++)
                selectedTimes.add(animationEditBranch.getKeyTime(selectedTrack, i));

        for (String boneName : tracks.keySet()) {

            ArpgArrayStruct track = animationEditBranch.findTrack(animationEditorManager.getClipArpg(), boneName);

            if (track == null || boneName.equals(selectedBone))
                continue;

            for (int i = 0; i < track.size(); i++) {

                float keyTime = animationEditBranch.getKeyTime(track, i);

                if (!containsTime(selectedTimes, keyTime) && !containsTime(otherTimes, keyTime))
                    otherTimes.add(keyTime);
            }
        }
    }

    private boolean containsTime(FloatArrayList times, float time) {

        for (int i = 0; i < times.size(); i++)
            if (Math.abs(times.getFloat(i) - time) <= EditorSetting.ANIMATION_EDITOR_KEY_EPSILON)
                return true;

        return false;
    }

    // Keys \\

    private ObjectArrayList<String> collectSelectedBone(AnimationDocumentInstance document) {

        ObjectArrayList<String> boneNames = new ObjectArrayList<>(1);
        boneNames.add(document.getRigHandle().getBone(animationEditorManager.getSelectedBoneIndex()).getName());
        return boneNames;
    }

    // Every bone with a track, in rig order, and the selected bone with them
    private ObjectArrayList<String> collectKeyedBones(AnimationDocumentInstance document) {

        RigHandle rigHandle = document.getRigHandle();
        ObjectArrayList<String> boneNames = new ObjectArrayList<>();

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            String boneName = rigHandle.getBone(i).getName();

            if (isKeyed(boneName) || i == animationEditorManager.getSelectedBoneIndex())
                boneNames.add(boneName);
        }

        return boneNames;
    }

    boolean isKeyed(String boneName) {
        return animationEditBranch.findTrack(animationEditorManager.getClipArpg(), boneName) != null;
    }

    int getKeyCount(String boneName) {

        ArpgArrayStruct track = animationEditBranch.findTrack(animationEditorManager.getClipArpg(), boneName);
        return track != null ? track.size() : 0;
    }

    int findKeyIndexAt(AnimationDocumentInstance document, int boneIndex) {
        return animationEditBranch.findKeyIndex(
                animationEditBranch.findTrack(
                        animationEditorManager.getClipArpg(),
                        document.getRigHandle().getBone(boneIndex).getName()),
                animationEditorManager.getTime());
    }

    private ArpgObjectStruct findKeyAt(AnimationDocumentInstance document, int boneIndex) {

        int keyIndex = findKeyIndexAt(document, boneIndex);

        return keyIndex != EngineSetting.INDEX_NOT_FOUND
                ? findBoneTrack(document, boneIndex).get(keyIndex).getAsObject()
                : null;
    }

    private ArpgArrayStruct findBoneTrack(AnimationDocumentInstance document, int boneIndex) {
        return animationEditBranch.findTrack(
                animationEditorManager.getClipArpg(),
                document.getRigHandle().getBone(boneIndex).getName());
    }
}
