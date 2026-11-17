package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.function.UnaryOperator;

import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class CharacterClipBranch extends BranchPackage {

    /*
     * Joins the motion a body part brings into the clips of the rig it joins,
     * the way a modular kit's parts carry their own animation layers. Each
     * clip the part names that the rig plays gains one part of its own,
     * named after the part's top bone as it joined, holding the part's tracks
     * on its bones as they were renamed: the side's own tracks where the part
     * brings them for the side it joins, otherwise its tracks for either
     * side, mirrored across the body when the part was. The clip plays it as
     * one with the rest and the Animation Editor edits it apart. A joined
     * clip is built again live at once, and again whenever the rig's bones
     * change, so a part taken back by Undo stops playing; it is written and
     * reverted with the character.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private InfoManager infoManager;
    private AnimationManager animationManager;

    // Joined — the clips a part has joined since they were last written or reverted
    private ObjectOpenHashSet<String> joinedClips;
    private ObjectArrayList<String> clipScratch;

    // Base \\

    @Override
    protected void create() {

        // Joined
        this.joinedClips = new ObjectOpenHashSet<>();
        this.clipScratch = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.infoManager = get(InfoManager.class);
        this.animationManager = get(AnimationManager.class);
    }

    // Join \\

    // Every animation the part brings joined to the rig's clip of its name; how many clips it joined
    int join(StampHandle stampHandle, CharacterPartPlacementStruct placement, UnaryOperator<String> rename) {

        ArpgObjectStruct animationsArpg = stampHandle.getAnimationsArpg();
        String rigName = characterEditorManager.getActiveRigName();

        if (animationsArpg.size() == 0 || rigName == null)
            return 0;

        String partName = rename.apply(readTopBone(stampHandle));
        String sideGroup = resolveSideGroup(CharacterMeshUtility.toSideSuffix(partName));
        int joined = 0;

        for (String clipName : animationsArpg.keySet()) {

            InfoEntryStruct entry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName, clipName);

            if (entry == null || !rigName.equals(
                    ArpgUtility.getString(entry.getArpg(), EditorSetting.ANIMATION_EDITOR_FIELD_RIG, "")))
                continue;

            ArpgObjectStruct partArpg = new ArpgObjectStruct();

            partArpg.add(
                    EditorSetting.ANIMATION_EDITOR_FIELD_TRACKS,
                    resolveTracks(animationsArpg.getAsObject(clipName), sideGroup, placement.isMirrored(), rename));
            infoManager.editEntry(
                    EditorSetting.INFO_SCHEMA_ANIMATIONS,
                    clipName,
                    clipName,
                    clip -> CharacterViewUtility
                            .requireObject(clip, EditorSetting.ANIMATION_EDITOR_FIELD_PARTS)
                            .add(partName, partArpg));

            joinedClips.add(clipName);
            apply(clipName);
            joined++;
        }

        return joined;
    }

    // The group a part joining on a side takes its own tracks from, empty for a part on the centre
    private static String resolveSideGroup(String side) {

        if (side.equals(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT))
            return EditorSetting.STAMP_FIELD_LEFT;

        return side.equals(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT) ? EditorSetting.STAMP_FIELD_RIGHT : "";
    }

    // The part's tracks for one clip on the bones they joined as: the side's own where the part brings them, else its
    // tracks for either side, reflected across the body for a mirrored part
    private ArpgObjectStruct resolveTracks(
            ArpgObjectStruct animationArpg,
            String sideGroup,
            boolean mirrored,
            UnaryOperator<String> rename) {

        boolean sided = !sideGroup.isEmpty() && ArpgUtility.hasObject(animationArpg, sideGroup);
        ArpgObjectStruct sourceArpg = sided
                ? animationArpg.getAsObject(sideGroup)
                : CharacterViewUtility.findObject(animationArpg, EditorSetting.STAMP_FIELD_TRACKS);
        ArpgObjectStruct tracksArpg = new ArpgObjectStruct();

        if (sourceArpg == null)
            return tracksArpg;

        for (String boneName : sourceArpg.keySet()) {

            ArpgArrayStruct track = sourceArpg.getAsArray(boneName).deepCopy();

            if (mirrored && !sided)
                for (int i = 0; i < track.size(); i++)
                    mirrorKey(track.get(i).getAsObject());

            tracksArpg.add(rename.apply(boneName), track);
        }

        return tracksArpg;
    }

    // A key reflected across the body's centre plane: a turn keeps its x and turns the other way about y and z, and a
    // move crosses over in x
    private static void mirrorKey(ArpgObjectStruct keyArpg) {

        ArpgObjectStruct rotation = CharacterViewUtility.findObject(
                keyArpg,
                EditorSetting.ANIMATION_EDITOR_FIELD_ROTATION);
        ArpgObjectStruct position = CharacterViewUtility.findObject(
                keyArpg,
                EditorSetting.ANIMATION_EDITOR_FIELD_POSITION);

        if (rotation != null) {
            negate(rotation, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[1]);
            negate(rotation, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[2]);
        }

        if (position != null)
            negate(position, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[0]);
    }

    private static void negate(ArpgObjectStruct vectorArpg, String axis) {

        if (vectorArpg.has(axis))
            vectorArpg.addProperty(axis, -ArpgUtility.getFloat(vectorArpg, axis, 0f));
    }

    // The part's first bone, the one every other hangs below
    private static String readTopBone(StampHandle stampHandle) {
        return ArpgUtility.getString(
                stampHandle.getBonesArpg().get(0).getAsObject(),
                EditorSetting.CHARACTER_RIG_FIELD_NAME,
                "");
    }

    // Live \\

    // Every joined clip built again on the rig as it now stands, so a part whose bones were taken back stops playing
    void refresh() {

        for (String clipName : joinedClips)
            apply(clipName);
    }

    private void apply(String clipName) {

        InfoEntryStruct entry = infoManager.findEntry(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName, clipName);

        if (entry == null)
            return;

        try {
            animationManager.updateClip(clipName, entry.getArpg());
        } catch (InternalException e) {
            errorLog(e.getMessage());
        }
    }

    // Files \\

    // Every joined clip written with the character; false, with the refusal said, when one could not be
    boolean save() {

        clipScratch.clear();
        clipScratch.addAll(joinedClips);

        for (int i = 0; i < clipScratch.size(); i++) {

            String clipName = clipScratch.get(i);

            if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName)
                    && !infoManager.saveDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName))
                return false;

            joinedClips.remove(clipName);
        }

        return true;
    }

    boolean isDirty() {

        for (String clipName : joinedClips)
            if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName))
                return true;

        return false;
    }

    // Every joined clip put back as its file holds it, and played that way again
    void revert() {

        for (String clipName : joinedClips) {

            if (infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName))
                infoManager.revertDocument(EditorSetting.INFO_SCHEMA_ANIMATIONS, clipName);

            apply(clipName);
        }

        joinedClips.clear();
    }
}
