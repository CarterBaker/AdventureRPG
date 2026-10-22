package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import java.util.function.Consumer;

import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.animationeditorpipeline.util.AnimationEditorChannel;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationEditBranch extends BranchPackage {

    /*
     * Performs every change the animation editor makes to a clip, each
     * through InfoManager so the Info Panel, the preview and every tab see
     * it at once. A bone's track stays in strictly increasing time order: a
     * key written where the bone has none is inserted in its place, holding
     * the pose it is given. A key writes its rotation always, and position
     * and scale only once they leave their defaults, as authored clips do;
     * a track left with no key is removed. Changes that touch several bones
     * go through as one edit.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Clip \\

    private void edit(AnimationDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ANIMATIONS,
                document.getClipName(),
                document.getClipName(),
                editor);
    }

    void setLoop(AnimationDocumentInstance document, boolean loop) {
        edit(document, clip -> clip.addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_LOOP, loop));
    }

    // Keys \\

    // One axis of one channel of a bone's key at a time, keying the bone there with the pose first if it has none
    void setKeyComponent(
            AnimationDocumentInstance document,
            String boneName,
            float time,
            float[] pose,
            AnimationEditorChannel channel,
            int axis,
            float value) {

        edit(document, clip -> CharacterViewUtility.setObjectComponent(
                requireKey(clip, boneName, time, pose),
                channel.getField(),
                axis,
                value,
                channel.getDefaultValue()));
    }

    // Whole keys for several bones at one time, each holding its own pose
    void writeKeys(
            AnimationDocumentInstance document,
            ObjectArrayList<String> boneNames,
            float time,
            ObjectArrayList<float[]> poses) {

        edit(document, clip -> {
            for (int i = 0; i < boneNames.size(); i++)
                writePose(requireKey(clip, boneNames.get(i), time, poses.get(i)), poses.get(i));
        });
    }

    void deleteKey(AnimationDocumentInstance document, String boneName, int keyIndex) {
        edit(document, clip -> {

            ArpgObjectStruct tracks = clip.getAsObject(EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);
            ArpgArrayStruct track = tracks.getAsArray(boneName);

            track.remove(keyIndex);

            if (track.isEmpty())
                tracks.remove(boneName);
        });
    }

    void setKeyTime(AnimationDocumentInstance document, String boneName, int keyIndex, float time) {
        edit(document, clip -> findTrack(clip, boneName)
                .get(keyIndex)
                .getAsObject()
                .addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_TIME, CharacterViewUtility.round(time)));
    }

    private ArpgObjectStruct requireKey(ArpgObjectStruct clip, String boneName, float time, float[] pose) {

        ArpgArrayStruct track = CharacterViewUtility.requireArray(
                CharacterViewUtility.requireObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS),
                boneName);
        int keyIndex = findKeyIndex(track, time);

        if (keyIndex != EngineSetting.INDEX_NOT_FOUND)
            return track.get(keyIndex).getAsObject();

        ArpgObjectStruct key = new ArpgObjectStruct();
        key.addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_TIME, CharacterViewUtility.round(time));
        writePose(key, pose);
        insertKey(track, key, time);

        return key;
    }

    // Shifts every later key up one place and sets the new key in the gap, keeping the track in time order
    private void insertKey(ArpgArrayStruct track, ArpgObjectStruct key, float time) {

        int insertIndex = 0;

        while (insertIndex < track.size() && getKeyTime(track, insertIndex) < time)
            insertIndex++;

        track.add(key);

        for (int i = track.size() - 1; i > insertIndex; i--)
            track.set(i, track.get(i - 1));

        track.set(insertIndex, key);
    }

    private void writePose(ArpgObjectStruct key, float[] pose) {

        for (AnimationEditorChannel channel : AnimationEditorChannel.VALUES)
            if (channel == AnimationEditorChannel.ROTATION || key.has(channel.getField()) || !isDefault(pose, channel))
                for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
                    CharacterViewUtility.setObjectComponent(
                            key,
                            channel.getField(),
                            axis,
                            pose[channel.toPoseIndex(axis)],
                            channel.getDefaultValue());
    }

    private boolean isDefault(float[] pose, AnimationEditorChannel channel) {

        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
            if (CharacterViewUtility.round(pose[channel.toPoseIndex(axis)]) != channel.getDefaultValue())
                return false;

        return true;
    }

    // Reads \\

    ArpgArrayStruct findTrack(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct tracks = CharacterViewUtility.findObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);

        return tracks != null && ArpgUtility.hasArray(tracks, boneName) ? tracks.getAsArray(boneName) : null;
    }

    int findKeyIndex(ArpgArrayStruct track, float time) {

        if (track == null)
            return EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < track.size(); i++)
            if (Math.abs(getKeyTime(track, i) - time) <= EditorSetting.ANIMATION_EDITOR_KEY_EPSILON)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    float getKeyTime(ArpgArrayStruct track, int keyIndex) {
        return ArpgUtility.getFloat(track.get(keyIndex).getAsObject(), EditorSetting.ANIMATION_EDITOR_FIELD_TIME, 0f);
    }
}
