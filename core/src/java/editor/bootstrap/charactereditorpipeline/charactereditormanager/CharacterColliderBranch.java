package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.rig.RigColliderShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterColliderBranch extends BranchPackage {

    /*
     * Owns the colliders of the rig the Rig mode edits: the spheres and
     * capsules its bones carry for spring chains to rest against, read into
     * a draft beside CharacterRigBranch's bones whenever the rig's entry
     * changes, with the one selected among the selected bone's. A new
     * collider is a sphere halfway to its bone's first child, every spring
     * chain resting against it; a capsule runs from its bone's joint out
     * past the sphere's centre as far again, and a sphere taken back stands
     * at the capsule's middle. While mirroring is on, every collider on a
     * sided bone is added, removed, reshaped and moved with its twin on the
     * twin bone, mirrored across the body. Colliders are not structure, so
     * every edit goes through CharacterRigBranch's one edit path and is
     * simulated at once.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;

    // Draft — one entry per collider of the rig as it stands in its entry, storage only ever growing
    private ObjectArrayList<String> colliderNames;
    private IntArrayList colliderBones;
    private BooleanArrayList colliderCapsules;
    private FloatArrayList colliderRadii;
    private ObjectArrayList<Vector3> colliderStarts;
    private ObjectArrayList<Vector3> colliderEnds;
    private int colliderCount;
    private int selectedCollider;

    // Scratch
    private Vector3 pointScratch;

    // Base \\

    @Override
    protected void create() {

        // Draft
        this.colliderNames = new ObjectArrayList<>();
        this.colliderBones = new IntArrayList();
        this.colliderCapsules = new BooleanArrayList();
        this.colliderRadii = new FloatArrayList();
        this.colliderStarts = new ObjectArrayList<>();
        this.colliderEnds = new ObjectArrayList<>();
        this.selectedCollider = EngineSetting.INDEX_NOT_FOUND;

        // Scratch
        this.pointScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
    }

    // Draft \\

    // Every collider of the rig's entry read into the draft, the selection kept on the selected bone
    void reload(ArpgObjectStruct rigArpg) {

        ArpgArrayStruct colliders = rigArpg != null
                && ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS)
                        ? rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS)
                        : null;

        this.colliderCount = colliders != null ? colliders.size() : 0;

        for (int i = 0; i < colliderCount; i++)
            readCollider(i, colliders.get(i).getAsObject());

        if (selectedCollider >= colliderCount
                || (selectedCollider != EngineSetting.INDEX_NOT_FOUND
                        && colliderBones.getInt(selectedCollider) != characterRigBranch.getSelectedBone()))
            this.selectedCollider = findFirstCollider(characterRigBranch.getSelectedBone());
    }

    private void readCollider(int colliderIndex, ArpgObjectStruct colliderArpg) {

        while (colliderNames.size() <= colliderIndex) {
            colliderNames.add("");
            colliderBones.add(EngineSetting.INDEX_NOT_FOUND);
            colliderCapsules.add(false);
            colliderRadii.add(0f);
            colliderStarts.add(new Vector3());
            colliderEnds.add(new Vector3());
        }

        boolean capsule = isCapsule(colliderArpg);

        colliderNames.set(colliderIndex,
                ArpgUtility.getString(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));
        colliderBones.set(colliderIndex, characterRigBranch.findBoneIndex(
                ArpgUtility.getString(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_BONE, "")));
        colliderCapsules.set(colliderIndex, capsule);
        colliderRadii.set(colliderIndex, ArpgUtility.getFloat(
                colliderArpg,
                EditorSetting.CHARACTER_RIG_FIELD_RADIUS,
                EditorSetting.CHARACTER_RIG_COLLIDER_RADIUS_MIN));
        readPoint(colliderArpg, capsule ? EditorSetting.CHARACTER_RIG_FIELD_START
                : EditorSetting.CHARACTER_RIG_FIELD_OFFSET, colliderStarts.get(colliderIndex));
        readPoint(colliderArpg, capsule ? EditorSetting.CHARACTER_RIG_FIELD_END
                : EditorSetting.CHARACTER_RIG_FIELD_OFFSET, colliderEnds.get(colliderIndex));
    }

    private void readPoint(ArpgObjectStruct colliderArpg, String key, Vector3 out) {
        out.set(
                CharacterViewUtility.getComponent(colliderArpg, key, 0, 0f),
                CharacterViewUtility.getComponent(colliderArpg, key, 1, 0f),
                CharacterViewUtility.getComponent(colliderArpg, key, 2, 0f));
    }

    private boolean isCapsule(ArpgObjectStruct colliderArpg) {
        return ArpgUtility.getEnum(
                colliderArpg,
                EditorSetting.CHARACTER_RIG_FIELD_SHAPE,
                RigColliderShape.class,
                RigColliderShape.SPHERE) == RigColliderShape.CAPSULE;
    }

    // Structure \\

    // A sphere on a bone and, while mirroring, one on its twin across the body; every spring chain rests on them
    void addCollider(int boneIndex) {

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND)
            return;

        ObjectArrayList<ArpgObjectStruct> added = new ObjectArrayList<>();
        String name = resolveColliderName(boneIndex);
        String twinName = CharacterMeshUtility.toMirrorBoneName(name);
        int twin = characterRigBranch.findTwinBone(boneIndex);

        added.add(buildCollider(name, boneIndex));

        if (characterEditorManager.isMirror() && twin != EngineSetting.INDEX_NOT_FOUND
                && findCollider(twinName) == EngineSetting.INDEX_NOT_FOUND)
            added.add(buildCollider(twinName, twin));

        if (colliderCount + added.size() > EngineSetting.RIG_COLLIDER_MAX) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_COLLIDER_FULL);
            return;
        }

        int selected = colliderCount;

        characterRigBranch.editRig(rig -> {

            ArpgArrayStruct colliders = CharacterViewUtility.requireArray(
                    rig, EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);
            ArpgArrayStruct chains = ArpgUtility.hasArray(rig, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS)
                    ? rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS)
                    : null;

            for (int i = 0; i < added.size(); i++) {

                colliders.add(added.get(i));

                for (int c = 0; chains != null && c < chains.size(); c++)
                    CharacterViewUtility.requireArray(
                            chains.get(c).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS)
                            .add(ArpgUtility.getString(added.get(i), EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));
            }
        });
        selectCollider(selected);
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_COLLIDER_ADDED + name);
    }

    // The selected collider and, while mirroring, its twin removed, and every spring chain let go of them
    void removeCollider() {

        if (selectedCollider == EngineSetting.INDEX_NOT_FOUND) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_COLLIDER_NONE);
            return;
        }

        String name = colliderNames.get(selectedCollider);
        int twin = findTwinCollider(selectedCollider);
        String twinName = twin != EngineSetting.INDEX_NOT_FOUND ? colliderNames.get(twin) : name;

        characterRigBranch.editRig(rig -> {

            ArpgArrayStruct colliders = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

            for (int i = colliders.size() - 1; i >= 0; i--)
                if (namesCollider(colliders.get(i).getAsObject(), name, twinName))
                    colliders.remove(i);

            if (!ArpgUtility.hasArray(rig, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS))
                return;

            ArpgArrayStruct chains = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS);

            for (int c = 0; c < chains.size(); c++) {

                ArpgObjectStruct chainArpg = chains.get(c).getAsObject();

                if (!ArpgUtility.hasArray(chainArpg, EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS))
                    continue;

                ArpgArrayStruct named = chainArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

                for (int i = named.size() - 1; i >= 0; i--)
                    if (named.get(i).getAsString().equals(name) || named.get(i).getAsString().equals(twinName))
                        named.remove(i);
            }
        });
        this.selectedCollider = findFirstCollider(characterRigBranch.getSelectedBone());
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_COLLIDER_REMOVED + name);
    }

    private boolean namesCollider(ArpgObjectStruct colliderArpg, String name, String twinName) {

        String colliderName = ArpgUtility.getString(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, "");
        return colliderName.equals(name) || colliderName.equals(twinName);
    }

    // A sphere halfway from a bone's joint to its first child, as wide as half the bone's thinner side
    private ArpgObjectStruct buildCollider(String name, int boneIndex) {

        ArpgObjectStruct colliderArpg = new ArpgObjectStruct();
        Vector3 size = characterRigBranch.getSize(boneIndex);
        int child = characterRigBranch.findFirstChild(boneIndex);

        if (child != EngineSetting.INDEX_NOT_FOUND)
            pointScratch.set(characterRigBranch.getPosition(child))
                    .multiply(EditorSetting.CHARACTER_RIG_COLLIDER_CENTRE_SHARE);
        else
            pointScratch.set(0f, 0f, 0f);

        colliderArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, name);
        colliderArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_BONE, characterRigBranch.getBoneName(boneIndex));
        colliderArpg.addProperty(
                EditorSetting.CHARACTER_RIG_FIELD_SHAPE, ArpgUtility.toEnumName(RigColliderShape.SPHERE));
        colliderArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_RADIUS, CharacterViewUtility.round(Math.max(
                EditorSetting.CHARACTER_RIG_COLLIDER_RADIUS_MIN,
                Math.min(size.x, size.z) * EditorSetting.CHARACTER_RIG_COLLIDER_WIDTH_SHARE)));
        writePoint(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_OFFSET, pointScratch, false);

        return colliderArpg;
    }

    // The first free name of a collider on a bone: the bone's name without its side, the suffix, a number from two
    // when taken, then the side, so a collider and its twin's names mirror as their bones' do
    private String resolveColliderName(int boneIndex) {

        String boneName = characterRigBranch.getBoneName(boneIndex);
        String side = CharacterMeshUtility.toSideSuffix(boneName);
        String stem = boneName.substring(0, boneName.length() - side.length())
                + EditorSetting.CHARACTER_RIG_COLLIDER_SUFFIX;

        for (int number = 1;; number++) {

            String name = stem + (number > 1 ? String.valueOf(number) : "") + side;

            if (findCollider(name) == EngineSetting.INDEX_NOT_FOUND)
                return name;
        }
    }

    // Values \\

    // One value of a collider by its place in the collider's rows: the radius, then each end's three axes
    float getValue(int colliderIndex, int field) {

        if (field == EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_RADIUS)
            return colliderRadii.getFloat(colliderIndex);

        Vector3 point = field < EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_END
                ? colliderStarts.get(colliderIndex)
                : colliderEnds.get(colliderIndex);
        int axis = toAxis(field);

        return axis == 0 ? point.x : axis == 1 ? point.y : point.z;
    }

    // One value of a collider written, a radius held to its range, and its twin's mirrored while mirroring
    void setValue(int colliderIndex, int field, float value) {

        float written = field == EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_RADIUS
                ? CharacterViewUtility.round(CharacterViewUtility.clamp(
                        value, EditorSetting.CHARACTER_RIG_COLLIDER_RADIUS_MIN, EngineSetting.RIG_COLLIDER_RADIUS_MAX))
                : value;
        int twin = findTwinCollider(colliderIndex);

        characterRigBranch.editRig(rig -> {

            ArpgArrayStruct colliders = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

            writeValue(colliders.get(colliderIndex).getAsObject(), field, written, false);

            if (twin != EngineSetting.INDEX_NOT_FOUND)
                writeValue(colliders.get(twin).getAsObject(), field, written, true);
        });
    }

    private void writeValue(ArpgObjectStruct colliderArpg, int field, float value, boolean mirrored) {

        if (field == EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_RADIUS) {
            colliderArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_RADIUS, value);
            return;
        }

        int axis = toAxis(field);
        String key = !isCapsule(colliderArpg) ? EditorSetting.CHARACTER_RIG_FIELD_OFFSET
                : field < EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_END ? EditorSetting.CHARACTER_RIG_FIELD_START
                        : EditorSetting.CHARACTER_RIG_FIELD_END;

        CharacterViewUtility.setObjectComponent(colliderArpg, key, axis, mirrored && axis == 0 ? -value : value, 0f);
    }

    private int toAxis(int field) {
        return (field - EditorSetting.CHARACTER_RIG_COLLIDER_FIELD_START)
                % EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length;
    }

    // A collider turned from a sphere into a capsule, its bone's joint out past the centre as far again, or back
    // into a sphere at the capsule's middle; its twin turned with it
    void cycleShape(int colliderIndex) {

        int twin = findTwinCollider(colliderIndex);

        characterRigBranch.editRig(rig -> {

            ArpgArrayStruct colliders = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

            toggleShape(colliders.get(colliderIndex).getAsObject(), colliderIndex);

            if (twin != EngineSetting.INDEX_NOT_FOUND)
                toggleShape(colliders.get(twin).getAsObject(), twin);
        });
    }

    private void toggleShape(ArpgObjectStruct colliderArpg, int colliderIndex) {

        Vector3 start = colliderStarts.get(colliderIndex);
        Vector3 end = colliderEnds.get(colliderIndex);

        if (colliderCapsules.getBoolean(colliderIndex)) {
            pointScratch.set(start).add(end).multiply(EditorSetting.CHARACTER_RIG_COLLIDER_CENTRE_SHARE);
            colliderArpg.remove(EditorSetting.CHARACTER_RIG_FIELD_START);
            colliderArpg.remove(EditorSetting.CHARACTER_RIG_FIELD_END);
            colliderArpg.addProperty(
                    EditorSetting.CHARACTER_RIG_FIELD_SHAPE, ArpgUtility.toEnumName(RigColliderShape.SPHERE));
            writePoint(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_OFFSET, pointScratch, false);
            return;
        }

        colliderArpg.remove(EditorSetting.CHARACTER_RIG_FIELD_OFFSET);
        colliderArpg.addProperty(
                EditorSetting.CHARACTER_RIG_FIELD_SHAPE, ArpgUtility.toEnumName(RigColliderShape.CAPSULE));
        writePoint(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_START, pointScratch.set(0f, 0f, 0f), false);
        writePoint(colliderArpg, EditorSetting.CHARACTER_RIG_FIELD_END,
                pointScratch.set(start).multiply(1f / EditorSetting.CHARACTER_RIG_COLLIDER_CENTRE_SHARE), false);
    }

    private void writePoint(ArpgObjectStruct colliderArpg, String key, Vector3 point, boolean mirrored) {
        CharacterViewUtility.setObjectComponent(colliderArpg, key, 0, mirrored ? -point.x : point.x, 0f);
        CharacterViewUtility.setObjectComponent(colliderArpg, key, 1, point.y, 0f);
        CharacterViewUtility.setObjectComponent(colliderArpg, key, 2, point.z, 0f);
    }

    // Chains \\

    // How many of the rig's spring chains rest against a collider
    int countChains(int colliderIndex) {

        ArpgArrayStruct chains = characterRigBranch.getSpringsArpg();
        int count = 0;

        for (int c = 0; chains != null && c < chains.size(); c++)
            if (isChainCollider(chains.get(c).getAsObject(), colliderNames.get(colliderIndex)))
                count++;

        return count;
    }

    int countAllChains() {

        ArpgArrayStruct chains = characterRigBranch.getSpringsArpg();
        return chains != null ? chains.size() : 0;
    }

    // Every spring chain made to rest against a collider and its twin, or none
    void setChains(int colliderIndex, boolean all) {

        int twin = findTwinCollider(colliderIndex);
        String name = colliderNames.get(colliderIndex);
        String twinName = twin != EngineSetting.INDEX_NOT_FOUND ? colliderNames.get(twin) : name;

        characterRigBranch.editRig(rig -> {

            if (!ArpgUtility.hasArray(rig, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS))
                return;

            ArpgArrayStruct chains = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS);

            for (int c = 0; c < chains.size(); c++) {
                writeChainCollider(chains.get(c).getAsObject(), name, all);
                writeChainCollider(chains.get(c).getAsObject(), twinName, all);
            }
        });
    }

    boolean isSpringCollider(int chainIndex, int colliderIndex) {

        ArpgArrayStruct chains = characterRigBranch.getSpringsArpg();

        return chains != null && chainIndex < chains.size()
                && isChainCollider(chains.get(chainIndex).getAsObject(), colliderNames.get(colliderIndex));
    }

    // A spring chain made to rest against a collider, or let go of it
    void toggleSpringCollider(int chainIndex, int colliderIndex) {

        boolean rests = !isSpringCollider(chainIndex, colliderIndex);
        String name = colliderNames.get(colliderIndex);

        characterRigBranch.editRig(rig -> writeChainCollider(
                rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS).get(chainIndex).getAsObject(),
                name,
                rests));
    }

    private boolean isChainCollider(ArpgObjectStruct chainArpg, String name) {

        if (!ArpgUtility.hasArray(chainArpg, EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS))
            return false;

        ArpgArrayStruct named = chainArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

        for (int i = 0; i < named.size(); i++)
            if (named.get(i).getAsString().equals(name))
                return true;

        return false;
    }

    private void writeChainCollider(ArpgObjectStruct chainArpg, String name, boolean rests) {

        if (rests == isChainCollider(chainArpg, name))
            return;

        ArpgArrayStruct named = CharacterViewUtility.requireArray(
                chainArpg, EditorSetting.CHARACTER_RIG_FIELD_COLLIDERS);

        if (rests) {
            named.add(name);
            return;
        }

        for (int i = named.size() - 1; i >= 0; i--)
            if (named.get(i).getAsString().equals(name))
                named.remove(i);
    }

    // Every collider the rig carries, named so a new spring chain rests against them all
    ArpgArrayStruct buildColliderNames() {

        ArpgArrayStruct names = new ArpgArrayStruct();

        for (int i = 0; i < colliderCount; i++)
            names.add(colliderNames.get(i));

        return names;
    }

    // Selection \\

    void selectCollider(int colliderIndex) {
        this.selectedCollider = colliderIndex;
        characterEditorManager.notifyChanged();
    }

    // The selected bone's first collider, the selection a newly picked bone starts with
    void followBone(int boneIndex) {
        this.selectedCollider = findFirstCollider(boneIndex);
    }

    private int findFirstCollider(int boneIndex) {

        for (int i = 0; i < colliderCount; i++)
            if (colliderBones.getInt(i) == boneIndex)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private int findCollider(String name) {

        for (int i = 0; i < colliderCount; i++)
            if (colliderNames.get(i).equals(name))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // A collider's twin on the twin bone, named as its mirror, found only while mirroring
    private int findTwinCollider(int colliderIndex) {

        if (!characterEditorManager.isMirror())
            return EngineSetting.INDEX_NOT_FOUND;

        String name = colliderNames.get(colliderIndex);
        String twinName = CharacterMeshUtility.toMirrorBoneName(name);
        int twin = twinName.equals(name) ? EngineSetting.INDEX_NOT_FOUND : findCollider(twinName);

        return twin != EngineSetting.INDEX_NOT_FOUND
                && colliderBones.getInt(twin) == characterRigBranch.findTwinBone(colliderBones.getInt(colliderIndex))
                        ? twin
                        : EngineSetting.INDEX_NOT_FOUND;
    }

    // Measure \\

    // One end of a collider where the rig as edited places it: a sphere's centre, or a capsule's start or end
    void resolvePoint(int colliderIndex, boolean end, Vector3 out) {

        Vector3 local = end ? colliderEnds.get(colliderIndex) : colliderStarts.get(colliderIndex);
        int bone = colliderBones.getInt(colliderIndex);

        if (bone == EngineSetting.INDEX_NOT_FOUND)
            out.set(local);
        else
            CharacterPoseUtility.transformPoint(
                    characterRigBranch.getWorldMatrix(bone), local.x, local.y, local.z, out);
    }

    // Accessible \\

    int getColliderCount() {
        return colliderCount;
    }

    int getSelectedCollider() {
        return selectedCollider;
    }

    String getColliderName(int colliderIndex) {
        return colliderNames.get(colliderIndex);
    }

    int getColliderBone(int colliderIndex) {
        return colliderBones.getInt(colliderIndex);
    }

    boolean isCapsule(int colliderIndex) {
        return colliderCapsules.getBoolean(colliderIndex);
    }

    float getRadius(int colliderIndex) {
        return colliderRadii.getFloat(colliderIndex);
    }
}
