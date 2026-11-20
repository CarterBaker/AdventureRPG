package editor.bootstrap.animationeditorpipeline.animationeditormanager;

import java.util.function.Consumer;

import application.bootstrap.animationpipeline.animation.AnimationChannel;
import application.bootstrap.animationpipeline.util.AnimationCurveArpgUtility;
import editor.bootstrap.animationeditorpipeline.animationdocument.AnimationDocumentInstance;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.curves.CurveEnds;
import engine.util.mathematics.curves.CurveKeyMode;
import engine.util.mathematics.curves.CurveStruct;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationEditBranch extends BranchPackage {

    /*
     * Performs every change the animation editor makes to a clip, each
     * through InfoManager so the Info Panel, the recorder's timeline and
     * keys, the preview and every tab see it at once; the recorder holds the
     * clip's one undo history, and takes each change as a step of it. A
     * bone's track keys each axis of each channel on a curve of its own
     * (AnimationCurveArpgUtility). Keying a bone at a moment keys the pose it
     * is given on every axis it already keys or that leaves its default, as
     * a DCC tool keys a bone's channels, and at least its first rotation axis
     * so the bone stays keyed; a bone's keys are the moments any of its
     * curves holds a key, so deleting or moving one acts on every curve
     * keyed there. A curve left with no key is dropped, and a track left with
     * none is removed. A bone a joined part keys is edited in that part's
     * tracks, so each part's motion stays its own within the one clip.
     * Changes that touch several bones go through as one edit, made whole on
     * a copy of the clip and then put in its place, so a clip that cannot be
     * read is refused, saying why, and never left half changed.
     */

    // Internal
    private InfoManager infoManager;
    private AnimationEditorManager animationEditorManager;

    // Scratch
    private FloatArrayList keyTimes;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.keyTimes = new FloatArrayList();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.animationEditorManager = get(AnimationEditorManager.class);
    }

    // Clip \\

    private void edit(AnimationDocumentInstance document, Consumer<ArpgObjectStruct> editor) {

        ArpgObjectStruct clip = findClip(document);

        if (clip == null)
            return;

        ArpgObjectStruct edited = clip.deepCopy().getAsObject();

        try {
            editor.accept(edited);
        } catch (InternalException e) {
            animationEditorManager.setStatusMessage(EditorSetting.ANIMATION_EDITOR_MESSAGE_REFUSED + e.getMessage());
            return;
        }

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ANIMATIONS,
                document.getClipName(),
                document.getClipName(),
                target -> CharacterViewUtility.replaceObject(target, edited));
    }

    // The clip's live ARPG, as InfoManager holds it
    ArpgObjectStruct findClip(AnimationDocumentInstance document) {

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_ANIMATIONS,
                document.getClipName(),
                document.getClipName());

        return entry != null ? entry.getArpg() : null;
    }

    void setLoop(AnimationDocumentInstance document, boolean loop) {
        edit(document, clip -> clip.addProperty(EditorSetting.ANIMATION_EDITOR_FIELD_LOOP, loop));
    }

    // Keys \\

    // One axis of one channel of a bone at a moment, keying the bone there with the pose first if it has no key
    void setKeyComponent(
            AnimationDocumentInstance document,
            String boneName,
            float time,
            float[] pose,
            AnimationChannel channel,
            int axis,
            float value) {

        edit(document, clip -> editTrack(clip, boneName, curves -> {

            if (!hasKeyAt(curves, time))
                keyPose(clip, curves, time, pose);

            keyValue(clip, curves, channel.toSlot(axis), time, value);
        }));
    }

    // Whole keys for several bones at one moment, each holding its own pose
    void writeKeys(
            AnimationDocumentInstance document,
            ObjectArrayList<String> boneNames,
            float time,
            ObjectArrayList<float[]> poses) {

        edit(document, clip -> {
            for (int i = 0; i < boneNames.size(); i++) {

                float[] pose = poses.get(i);

                editTrack(clip, boneNames.get(i), curves -> keyPose(clip, curves, time, pose));
            }
        });
    }

    // Every key a bone holds at the moment, on every curve
    void deleteKey(AnimationDocumentInstance document, String boneName, float time) {
        edit(document, clip -> editTrack(clip, boneName, curves -> {
            for (int slot = 0; slot < curves.length; slot++) {

                int key = findCurveKey(curves[slot], time);

                if (key != EngineSetting.INDEX_NOT_FOUND)
                    curves[slot].removeKey(key);
            }
        }));
    }

    // Every key a bone holds at one moment moved to another, on every curve
    void setKeyTime(AnimationDocumentInstance document, String boneName, float fromTime, float toTime) {
        edit(document, clip -> editTrack(clip, boneName, curves -> {
            for (int slot = 0; slot < curves.length; slot++) {

                int key = findCurveKey(curves[slot], fromTime);

                if (key != EngineSetting.INDEX_NOT_FOUND)
                    curves[slot].moveKey(key, CharacterViewUtility.round(toTime), curves[slot].getValue(key));
            }
        }));
    }

    // The pose keyed on every axis the bone already keys or that leaves its default, and its first rotation axis
    // when it would otherwise key none
    private void keyPose(ArpgObjectStruct clip, CurveStruct[] curves, float time, float[] pose) {

        boolean keyed = false;

        for (int slot = 0; slot < curves.length; slot++)
            if (curves[slot] != null
                    || CharacterViewUtility.round(pose[slot]) != AnimationChannel.fromSlot(slot).getDefaultValue()) {
                keyValue(clip, curves, slot, time, pose[slot]);
                keyed = true;
            }

        if (!keyed)
            keyValue(clip, curves, AnimationChannel.ROTATION.toSlot(0), time, pose[0]);
    }

    private void keyValue(ArpgObjectStruct clip, CurveStruct[] curves, int slot, float time, float value) {

        if (curves[slot] == null)
            curves[slot] = new CurveStruct(resolveEnds(clip));

        curves[slot].addKey(CharacterViewUtility.round(time), CharacterViewUtility.round(value), CurveKeyMode.AUTO);
    }

    // Tracks \\

    // A bone's curves read from where it is keyed, changed, and written back there, or the track removed when no
    // key is left on it
    private void editTrack(ArpgObjectStruct clip, String boneName, Consumer<CurveStruct[]> editor) {

        ArpgObjectStruct tracks = resolveBoneTracks(clip, boneName);
        CurveStruct[] curves = readCurves(clip, tracks, boneName);

        editor.accept(curves);

        for (int slot = 0; slot < curves.length; slot++)
            if (curves[slot] != null && curves[slot].isEmpty())
                curves[slot] = null;

        ArpgObjectStruct trackArpg = AnimationCurveArpgUtility.writeTrack(curves);

        if (trackArpg.isEmpty())
            tracks.remove(boneName);
        else
            tracks.add(boneName, trackArpg);
    }

    private CurveStruct[] readCurves(ArpgObjectStruct clip, ArpgObjectStruct tracks, String boneName) {
        return tracks != null && ArpgUtility.hasObject(tracks, boneName)
                ? AnimationCurveArpgUtility.readTrack(
                        tracks.getAsObject(boneName), resolveEnds(clip), boneName, EditorSetting.INFO_SCHEMA_ANIMATIONS)
                : new CurveStruct[AnimationChannel.SLOT_COUNT];
    }

    // The tracks a bone is keyed in: those of the joined part that keys it, so an edit stays with its part, else the
    // clip's own, made when it has none
    private ArpgObjectStruct resolveBoneTracks(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct partTracks = findPartTracks(clip, boneName);

        return partTracks != null
                ? partTracks
                : CharacterViewUtility.requireObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);
    }

    private ArpgObjectStruct findBoneTracks(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct partTracks = findPartTracks(clip, boneName);

        return partTracks != null
                ? partTracks
                : CharacterViewUtility.findObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);
    }

    private ArpgObjectStruct findPartTracks(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct parts = CharacterViewUtility.findObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_PARTS);

        if (parts == null)
            return null;

        for (String partName : parts.keySet()) {

            ArpgObjectStruct tracks = CharacterViewUtility.findObject(
                    parts.getAsObject(partName),
                    EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS);

            if (tracks != null && ArpgUtility.hasObject(tracks, boneName))
                return tracks;
        }

        return null;
    }

    private static CurveEnds resolveEnds(ArpgObjectStruct clip) {
        return AnimationCurveArpgUtility.resolveEnds(
                ArpgUtility.getBoolean(clip, EditorSetting.ANIMATION_EDITOR_FIELD_LOOP, false));
    }

    // Reads \\

    // A bone's curves as the clip keys them, null when it keys the bone nowhere or its track cannot be read, which
    // the preview's status already names
    CurveStruct[] findCurves(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct tracks = clip != null ? findBoneTracks(clip, boneName) : null;

        if (tracks == null || !ArpgUtility.hasObject(tracks, boneName))
            return null;

        try {
            return readCurves(clip, tracks, boneName);
        } catch (InternalException e) {
            return null;
        }
    }

    boolean isKeyed(ArpgObjectStruct clip, String boneName) {

        ArpgObjectStruct tracks = clip != null ? findBoneTracks(clip, boneName) : null;

        return tracks != null && ArpgUtility.hasObject(tracks, boneName);
    }

    // Every moment any of a bone's curves holds a key, in time order, into the branch's own list
    FloatArrayList collectKeyTimes(ArpgObjectStruct clip, String boneName) {

        keyTimes.clear();

        CurveStruct[] curves = findCurves(clip, boneName);

        if (curves == null)
            return keyTimes;

        for (int slot = 0; slot < curves.length; slot++)
            for (int key = 0; curves[slot] != null && key < curves[slot].size(); key++)
                addKeyTime(curves[slot].getTime(key));

        keyTimes.sort(null);
        return keyTimes;
    }

    private void addKeyTime(float time) {

        if (findKeyIndex(keyTimes, time) == EngineSetting.INDEX_NOT_FOUND)
            keyTimes.add(time);
    }

    private boolean hasKeyAt(CurveStruct[] curves, float time) {

        for (int slot = 0; slot < curves.length; slot++)
            if (findCurveKey(curves[slot], time) != EngineSetting.INDEX_NOT_FOUND)
                return true;

        return false;
    }

    private int findCurveKey(CurveStruct curve, float time) {
        return curve != null
                ? curve.findKey(time, EditorSetting.ANIMATION_EDITOR_KEY_EPSILON)
                : EngineSetting.INDEX_NOT_FOUND;
    }

    int findKeyIndex(FloatArrayList times, float time) {

        for (int i = 0; i < times.size(); i++)
            if (Math.abs(times.getFloat(i) - time) <= EditorSetting.ANIMATION_EDITOR_KEY_EPSILON)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }
}
