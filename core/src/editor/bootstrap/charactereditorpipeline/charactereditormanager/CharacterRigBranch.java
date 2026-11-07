package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.function.Consumer;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterDigitStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterHairCardStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterHairLockStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorRegion;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.history.CharacterViewHistoryStruct;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterRigBranch extends BranchPackage {

    /*
     * Owns the rig the active character is built on as the Rig mode edits
     * it: its ARPG entry, owned by InfoManager, read into a draft of every
     * bone's name, parent and bind pose with its joint placed in the world,
     * the bone selected, and the rig's undo history. Every change goes
     * through InfoManager, so the Info Panel and every tab see it, and is
     * applied to the registered rig in place while the rig keeps its bones —
     * moving a joint, turning or sizing a bone shows at once on every
     * character, clip and pose built on it. Adding a bone, naming one,
     * moving it under another or mirroring one across the body changes the
     * bones themselves, which everything loaded is laid out by, so those
     * are written to the rig and take hold the next time the editor starts;
     * only bones added since it started can be renamed or removed, since
     * nothing loaded can name them yet. A bone moved under another keeps its
     * joint where it stood, and the bones stay parent before child. Spring
     * chains are rig data beside the bones: the hair's are laid on the locks
     * found in its cards, any other on a bone and the single children below
     * it, and their stiffness, damping, gravity and drag apply at once to
     * every character simulating them.
     */

    // Internal
    private InfoManager infoManager;
    private RigManager rigManager;
    private CharacterEditorManager characterEditorManager;
    private CharacterDigitBranch characterDigitBranch;
    private CharacterHairBranch characterHairBranch;

    // Rig
    private RigHandle rigHandle;
    private boolean structureChanged;
    private String refusal;
    private CharacterViewHistoryStruct history;

    // Draft — one entry per bone of the rig as it stands in its entry, storage only ever growing
    private ObjectArrayList<String> boneNames;
    private IntArrayList parents;
    private ObjectArrayList<Vector3> positions;
    private ObjectArrayList<Vector3> rotations;
    private ObjectArrayList<Vector3> sizes;
    private ObjectArrayList<Matrix4> worldMatrices;
    private int boneCount;
    private int selectedBone;

    // Gesture
    private Vector3 gesturePosition;
    private Vector3 gestureSize;
    private Matrix4 gestureParentInverse;

    // Scratch
    private Vector3 unitScale;
    private Vector3 vectorScratch;
    private Matrix4 localScratch;
    private Matrix4 matrixScratchA;
    private Matrix4 matrixScratchB;
    private Matrix4 matrixScratchC;
    private BooleanArrayList subtree;
    private IntArrayList orderScratch;

    // Base \\

    @Override
    protected void create() {

        // Rig
        this.history = new CharacterViewHistoryStruct();

        // Draft
        this.boneNames = new ObjectArrayList<>();
        this.parents = new IntArrayList();
        this.positions = new ObjectArrayList<>();
        this.rotations = new ObjectArrayList<>();
        this.sizes = new ObjectArrayList<>();
        this.worldMatrices = new ObjectArrayList<>();
        this.selectedBone = EngineSetting.INDEX_NOT_FOUND;

        // Gesture
        this.gesturePosition = new Vector3();
        this.gestureSize = new Vector3();
        this.gestureParentInverse = new Matrix4();

        // Scratch
        this.unitScale = new Vector3(1f, 1f, 1f);
        this.vectorScratch = new Vector3();
        this.localScratch = new Matrix4();
        this.matrixScratchA = new Matrix4();
        this.matrixScratchB = new Matrix4();
        this.matrixScratchC = new Matrix4();
        this.subtree = new BooleanArrayList();
        this.orderScratch = new IntArrayList();
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.rigManager = get(RigManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterDigitBranch = get(CharacterDigitBranch.class);
        this.characterHairBranch = get(CharacterHairBranch.class);
    }

    // Rig \\

    // Follows the rig the active character is built on, reading it afresh whenever that rig changes
    void sync(RigHandle activeRig) {

        if (activeRig == rigHandle)
            return;

        this.rigHandle = activeRig;
        this.selectedBone = activeRig != null ? 0 : EngineSetting.INDEX_NOT_FOUND;
        this.structureChanged = false;
        this.refusal = null;
        history.clear();
        reload();
    }

    // A change to a rig entry, made here or in the Info Panel: the draft read again and the bind pose applied
    void refreshEntry(InfoEntryStruct entry) {

        if (rigHandle == null || !entry.getEntryName().equals(rigHandle.getRigName()))
            return;

        reload();
        apply(entry.getArpg());
        characterEditorManager.notifyChanged();
    }

    private void apply(ArpgObjectStruct rigArpg) {

        try {
            this.structureChanged = !rigManager.updateRig(rigHandle, rigArpg);
            this.refusal = null;
        } catch (InternalException e) {
            errorLog(e.getMessage());
            this.refusal = e.getMessage();
        }
    }

    private ArpgObjectStruct getRigArpg() {

        if (rigHandle == null)
            return null;

        InfoEntryStruct entry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RIGS,
                rigHandle.getRigName(),
                rigHandle.getRigName());

        return entry != null ? entry.getArpg() : null;
    }

    private ArpgArrayStruct getBonesArpg() {

        ArpgObjectStruct rigArpg = getRigArpg();

        return rigArpg != null && ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_BONES)
                ? rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES)
                : null;
    }

    // Draft \\

    // Every bone of the entry read into the draft, each joint placed by its parent's, the selection kept in range
    private void reload() {

        ArpgArrayStruct bones = getBonesArpg();

        this.boneCount = bones != null ? bones.size() : 0;

        for (int i = 0; i < boneCount; i++)
            readBone(i, bones.get(i).getAsObject());

        if (selectedBone >= boneCount)
            this.selectedBone = boneCount > 0 ? 0 : EngineSetting.INDEX_NOT_FOUND;
    }

    private void readBone(int boneIndex, ArpgObjectStruct boneArpg) {

        while (worldMatrices.size() <= boneIndex) {
            boneNames.add("");
            parents.add(EngineSetting.INDEX_NOT_FOUND);
            positions.add(new Vector3());
            rotations.add(new Vector3());
            sizes.add(new Vector3());
            worldMatrices.add(new Matrix4());
        }

        boneNames.set(boneIndex, ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));
        parents.set(boneIndex, findBefore(
                ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_PARENT, ""),
                boneIndex));
        readVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, 0f, positions.get(boneIndex));
        readVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, 0f, rotations.get(boneIndex));
        readVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, EngineSetting.DEFAULT_BONE_SIZE,
                sizes.get(boneIndex));

        RigMathUtility.composeLocal(
                positions.get(boneIndex),
                rotations.get(boneIndex),
                unitScale,
                localScratch,
                matrixScratchA,
                matrixScratchB);

        int parent = parents.getInt(boneIndex);
        Matrix4 world = worldMatrices.get(boneIndex);

        if (parent == EngineSetting.INDEX_NOT_FOUND)
            world.set(localScratch);
        else
            world.set(worldMatrices.get(parent)).multiply(localScratch);
    }

    private void readVector(ArpgObjectStruct boneArpg, String key, float defaultValue, Vector3 out) {
        out.set(
                CharacterViewUtility.getComponent(boneArpg, key, 0, defaultValue),
                CharacterViewUtility.getComponent(boneArpg, key, 1, defaultValue),
                CharacterViewUtility.getComponent(boneArpg, key, 2, defaultValue));
    }

    // A bone named before another in the draft, as a parent must be
    private int findBefore(String boneName, int limit) {

        for (int i = 0; i < limit; i++)
            if (boneNames.get(i).equals(boneName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private int findBone(String boneName) {
        return findBefore(boneName, boneCount);
    }

    // Edits \\

    // One change to the rig's bones, recorded for undo first unless a gesture already holds the rig as it stood
    private void edit(Consumer<ArpgArrayStruct> editor) {
        editRig(rig -> editor.accept(rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES)));
    }

    // One change to the rig as a whole, recorded for undo first
    private void editRig(Consumer<ArpgObjectStruct> editor) {

        ArpgObjectStruct rigArpg = getRigArpg();

        if (rigArpg == null)
            return;

        history.record(rigArpg);
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_RIGS,
                rigHandle.getRigName(),
                rigHandle.getRigName(),
                editor);
    }

    private void replace(ArpgObjectStruct snapshot) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_RIGS,
                rigHandle.getRigName(),
                rigHandle.getRigName(),
                rig -> CharacterViewUtility.replaceObject(rig, snapshot));
    }

    // One axis of a bone's position, rotation or size
    void setComponent(int boneIndex, String field, int axis, float value) {

        float defaultValue = field.equals(EditorSetting.CHARACTER_EDITOR_FIELD_SIZE)
                ? EngineSetting.DEFAULT_BONE_SIZE
                : 0f;
        float written = field.equals(EditorSetting.CHARACTER_EDITOR_FIELD_SIZE)
                ? Math.max(EditorSetting.CHARACTER_RIG_SIZE_MIN, value)
                : value;

        edit(bones -> CharacterViewUtility.setObjectComponent(
                bones.get(boneIndex).getAsObject(), field, axis, written, defaultValue));
    }

    private void writeVector(ArpgObjectStruct boneArpg, String field, Vector3 value) {
        for (int axis = 0; axis < EditorSetting.CHARACTER_VIEW_AXIS_NAMES.length; axis++)
            CharacterViewUtility.setObjectComponent(boneArpg, field, axis, getAxis(value, axis), 0f);
    }

    // Gestures \\

    // Opens one undo step for a drag on the selected bone, holding where it started
    boolean beginGesture() {

        ArpgObjectStruct rigArpg = getRigArpg();

        if (rigArpg == null || selectedBone == EngineSetting.INDEX_NOT_FOUND)
            return false;

        history.begin(rigArpg);
        gesturePosition.set(positions.get(selectedBone));
        gestureSize.set(sizes.get(selectedBone));
        resolveParentRotation(selectedBone, gestureParentInverse);
        gestureParentInverse.inverse();

        return true;
    }

    // The selected joint carried by a move in the world from where the drag began, children following
    void moveGesture(Vector3 worldMove) {

        int boneIndex = selectedBone;

        CharacterPoseUtility.transformDirection(gestureParentInverse, worldMove.x, worldMove.y, worldMove.z,
                vectorScratch);
        vectorScratch.add(gesturePosition);
        edit(bones -> writeVector(bones.get(boneIndex).getAsObject(), EditorSetting.CHARACTER_EDITOR_FIELD_POSITION,
                vectorScratch));
    }

    // The selected bone's size grown or shrunk from what it was when the drag began
    void scaleGesture(float factor) {

        int boneIndex = selectedBone;

        vectorScratch.set(gestureSize).multiply(factor);
        vectorScratch.set(
                Math.max(EditorSetting.CHARACTER_RIG_SIZE_MIN, vectorScratch.x),
                Math.max(EditorSetting.CHARACTER_RIG_SIZE_MIN, vectorScratch.y),
                Math.max(EditorSetting.CHARACTER_RIG_SIZE_MIN, vectorScratch.z));
        edit(bones -> writeVector(bones.get(boneIndex).getAsObject(), EditorSetting.CHARACTER_EDITOR_FIELD_SIZE,
                vectorScratch));
    }

    void endGesture() {

        ArpgObjectStruct rigArpg = getRigArpg();

        if (rigArpg != null)
            history.commit(rigArpg);
    }

    void cancelGesture() {

        ArpgObjectStruct snapshot = history.cancel();

        if (snapshot != null && rigHandle != null)
            replace(snapshot);
    }

    boolean isGesturing() {
        return history.isOpen();
    }

    // Structure \\

    // A new child of the selected bone at a point in the world, or a step above it, named after the parent
    void addBone(Vector3 worldPoint) {

        if (selectedBone == EngineSetting.INDEX_NOT_FOUND)
            return;

        int parent = selectedBone;
        String boneName = resolveNewName(boneNames.get(parent));
        Vector3 position = new Vector3(0f, EditorSetting.CHARACTER_RIG_NEW_BONE_OFFSET, 0f);
        float size = EditorSetting.CHARACTER_RIG_NEW_BONE_SIZE;

        if (worldPoint != null) {
            matrixScratchA.set(worldMatrices.get(parent)).inverse();
            CharacterPoseUtility.transformPoint(matrixScratchA, worldPoint.x, worldPoint.y, worldPoint.z, position);
        }

        ArpgObjectStruct boneArpg = new ArpgObjectStruct();

        boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, boneName);
        boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, boneNames.get(parent));
        writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, position);
        writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, vectorScratch.set(size, size, size));

        edit(bones -> bones.add(boneArpg));
        this.selectedBone = findBone(boneName);
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_ADDED + boneName);
    }

    // A chain of bones on every digit found below each of a region's bones in a body mesh, the digits already rigged
    // left as they are, all in one edit
    void addDigits(RiggedMeshStruct mesh, CharacterEditorRegion region) {

        ObjectArrayList<ArpgObjectStruct> added = new ObjectArrayList<>();
        ObjectArrayList<CharacterDigitStruct> digits = new ObjectArrayList<>();

        for (String rootName : region.getRootBones()) {

            int root = findBone(rootName);

            if (root == EngineSetting.INDEX_NOT_FOUND || !mesh.getRigHandle().hasBone(rootName))
                continue;

            characterDigitBranch.detect(mesh, mesh.getRigHandle().getBoneIndex(rootName), digits);
            matrixScratchA.set(worldMatrices.get(root)).inverse();

            String side = CharacterMeshUtility.toSideSuffix(rootName);

            for (int d = 0; d < digits.size(); d++)
                layChain(digits.get(d), rootName, side, added);
        }

        if (added.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_DIGITS);
            return;
        }

        edit(bones -> {
            for (int i = 0; i < added.size(); i++)
                bones.add(added.get(i));
        });
        characterEditorManager.setStatusMessage(added.size() + EditorSetting.CHARACTER_RIG_MESSAGE_DIGITS);
    }

    // One digit's bones, each placed from the last in the root's frame, sized to the segment it covers
    private void layChain(
            CharacterDigitStruct digit,
            String rootName,
            String side,
            ObjectArrayList<ArpgObjectStruct> added) {

        int jointCount = digit.getJointCount();

        if (findBone(CharacterMeshUtility.toDigitBoneName(digit.getName(), 0, jointCount, side))
                != EngineSetting.INDEX_NOT_FOUND)
            return;

        Vector3 previous = new Vector3();
        Vector3 local = new Vector3();
        String parentName = rootName;
        float thickness = Math.max(digit.getThickness(), EditorSetting.CHARACTER_RIG_SIZE_MIN);

        for (int j = 0; j < jointCount; j++) {

            Vector3 joint = digit.getJoint(j);
            Vector3 end = digit.getSegmentEnd(j);
            String boneName = CharacterMeshUtility.toDigitBoneName(digit.getName(), j, jointCount, side);
            float length = new Vector3(end).subtract(joint).length();

            CharacterPoseUtility.transformPoint(matrixScratchA, joint.x, joint.y, joint.z, local);

            ArpgObjectStruct boneArpg = new ArpgObjectStruct();

            boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, boneName);
            boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, parentName);
            writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, new Vector3(local).subtract(previous));
            writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, vectorScratch.set(
                    thickness, Math.max(length, EditorSetting.CHARACTER_RIG_SIZE_MIN), thickness));
            added.add(boneArpg);

            previous.set(local);
            parentName = boneName;
        }
    }

    // Hair \\

    // A spring chain on every lock of long hair found in a hair mesh's cards, hung from the bone the hair grows on;
    // a lock rooted within the lock radius of a hair chain already rigged takes that chain and is left as it is
    void addHairLocks(RiggedMeshStruct mesh) {

        ObjectArrayList<CharacterHairCardStruct> cards = new ObjectArrayList<>();
        ObjectArrayList<CharacterHairLockStruct> locks = new ObjectArrayList<>();

        characterHairBranch.detectCards(mesh, cards);
        characterHairBranch.detectLocks(mesh, locks);

        int anchorIndex = characterHairBranch.resolveAnchor(mesh, cards);
        int anchor = anchorIndex != EngineSetting.INDEX_NOT_FOUND
                ? findBone(mesh.getRigHandle().getBone(anchorIndex).getName())
                : EngineSetting.INDEX_NOT_FOUND;

        if (anchor == EngineSetting.INDEX_NOT_FOUND || locks.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_HAIR);
            return;
        }

        ObjectArrayList<Vector3> roots = new ObjectArrayList<>();
        ObjectArrayList<ArpgObjectStruct> addedBones = new ObjectArrayList<>();
        ObjectArrayList<ArpgObjectStruct> addedChains = new ObjectArrayList<>();
        int number = 1;

        collectHairRoots(roots);
        matrixScratchA.set(worldMatrices.get(anchor)).inverse();

        for (int l = 0; l < locks.size(); l++) {

            CharacterHairLockStruct lock = locks.get(l);

            if (isNearAny(lock.getRoot(), roots))
                continue;

            number = resolveLockNumber(number, addedBones);
            layLock(lock, number, boneNames.get(anchor), addedBones, addedChains);
            roots.add(lock.getRoot());
        }

        if (addedChains.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_HAIR);
            return;
        }

        editRig(rig -> {

            ArpgArrayStruct bones = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES);
            ArpgArrayStruct chains = CharacterViewUtility.requireArray(rig, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS);

            for (int i = 0; i < addedBones.size(); i++)
                bones.add(addedBones.get(i));

            for (int i = 0; i < addedChains.size(); i++)
                chains.add(addedChains.get(i));
        });
        characterEditorManager.setStatusMessage(addedChains.size() + EditorSetting.CHARACTER_RIG_MESSAGE_HAIR);
    }

    // Where every hair chain already rigged takes root, its first joint in the draft
    private void collectHairRoots(ObjectArrayList<Vector3> roots) {

        ArpgArrayStruct chains = getSpringsArpg();

        for (int c = 0; chains != null && c < chains.size(); c++) {

            int root = findBone(readSpringBone(chains.get(c).getAsObject(), 0));

            if (root == EngineSetting.INDEX_NOT_FOUND
                    || !boneNames.get(root).startsWith(EditorSetting.CHARACTER_HAIR_LOCK_PREFIX))
                continue;

            Vector3 point = new Vector3();

            CharacterPoseUtility.getTranslation(worldMatrices.get(root), point);
            roots.add(point);
        }
    }

    private boolean isNearAny(Vector3 point, ObjectArrayList<Vector3> roots) {

        for (int i = 0; i < roots.size(); i++)
            if (new Vector3(point).subtract(roots.get(i)).length() <= EditorSetting.CHARACTER_HAIR_LOCK_RADIUS)
                return true;

        return false;
    }

    // The first lock number from the one given whose first bone neither the rig nor this edit names yet
    private int resolveLockNumber(int from, ObjectArrayList<ArpgObjectStruct> addedBones) {

        for (int number = from;; number++) {

            String boneName = toLockBoneName(number, 0);

            if (findBone(boneName) == EngineSetting.INDEX_NOT_FOUND && findBoneArpg(addedBones, boneName) == null)
                return number;
        }
    }

    private String toLockBoneName(int number, int joint) {
        return toLockName(number) + EditorSetting.CHARACTER_RIG_DIGIT_SEPARATOR + (joint + 1);
    }

    private String toLockName(int number) {
        return EditorSetting.CHARACTER_HAIR_LOCK_PREFIX + number;
    }

    private ArpgObjectStruct findBoneArpg(ObjectArrayList<ArpgObjectStruct> bones, String boneName) {

        for (int i = 0; i < bones.size(); i++)
            if (boneName.equals(ArpgUtility.getString(bones.get(i), EditorSetting.CHARACTER_RIG_FIELD_NAME, "")))
                return bones.get(i);

        return null;
    }

    // One lock's bones, each placed from the last in the anchor's frame and sized to the segment it covers, and the
    // spring chain they make with the settings a new chain starts with
    private void layLock(
            CharacterHairLockStruct lock,
            int number,
            String anchorName,
            ObjectArrayList<ArpgObjectStruct> addedBones,
            ObjectArrayList<ArpgObjectStruct> addedChains) {

        Vector3 previous = new Vector3();
        Vector3 local = new Vector3();
        ArpgArrayStruct chainBones = new ArpgArrayStruct();
        String parentName = anchorName;
        float thickness = lock.getThickness();

        for (int j = 0; j < lock.getJointCount(); j++) {

            Vector3 joint = lock.getJoint(j);
            String boneName = toLockBoneName(number, j);
            float length = j + 1 < lock.getJointCount()
                    ? new Vector3(lock.getJoint(j + 1)).subtract(joint).length()
                    : thickness;

            CharacterPoseUtility.transformPoint(matrixScratchA, joint.x, joint.y, joint.z, local);

            ArpgObjectStruct boneArpg = new ArpgObjectStruct();

            boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, boneName);
            boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, parentName);
            writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, new Vector3(local).subtract(previous));
            writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, vectorScratch.set(
                    thickness, Math.max(length, EditorSetting.CHARACTER_RIG_SIZE_MIN), thickness));
            addedBones.add(boneArpg);
            chainBones.add(boneName);

            previous.set(local);
            parentName = boneName;
        }

        addedChains.add(buildSpringChain(toLockName(number), chainBones));
    }

    // Springs \\

    // A spring chain from a bone down the single children below it, and one from its twin across the body; none
    // where the bone has no child, is the root or already belongs to a chain or an IK handle
    void addSpringChain(int boneIndex) {

        ObjectArrayList<ArpgObjectStruct> added = new ObjectArrayList<>();

        laySpringChain(boneIndex, added);
        laySpringChain(findTwin(boneIndex), added);

        if (added.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_SPRING_NONE);
            return;
        }

        editRig(rig -> {

            ArpgArrayStruct chains = CharacterViewUtility.requireArray(rig, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS);

            for (int i = 0; i < added.size(); i++)
                chains.add(added.get(i));
        });
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_SPRING_ADDED
                + ArpgUtility.getString(added.get(0), EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));
    }

    // The spring chains holding a bone and its twin removed, their bones kept
    void removeSpringChain(int boneIndex) {

        int chain = findSpringChain(boneIndex);
        int twinChain = findSpringChain(findTwin(boneIndex));

        if (chain == EngineSetting.INDEX_NOT_FOUND && twinChain == EngineSetting.INDEX_NOT_FOUND) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_SPRING_FREE);
            return;
        }

        String name = getSpringName(chain != EngineSetting.INDEX_NOT_FOUND ? chain : twinChain);

        editRig(rig -> {

            ArpgArrayStruct chains = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS);

            for (int i = chains.size() - 1; i >= 0; i--)
                if (i == chain || i == twinChain)
                    chains.remove(i);
        });
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_SPRING_REMOVED + name);
    }

    private void laySpringChain(int boneIndex, ObjectArrayList<ArpgObjectStruct> added) {

        if (boneIndex <= 0 || isChained(boneIndex, added))
            return;

        ArpgArrayStruct chainBones = new ArpgArrayStruct();

        for (int bone = boneIndex; bone != EngineSetting.INDEX_NOT_FOUND; bone = findOnlyChild(bone)) {

            if (isChained(bone, added))
                return;

            chainBones.add(boneNames.get(bone));
        }

        if (chainBones.size() < EngineSetting.RIG_SPRING_MIN_BONES)
            return;

        added.add(buildSpringChain(boneNames.get(boneIndex) + EditorSetting.CHARACTER_RIG_SPRING_SUFFIX, chainBones));
    }

    // Whether a bone already belongs to a spring chain, one about to be added, or an IK handle
    private boolean isChained(int boneIndex, ObjectArrayList<ArpgObjectStruct> added) {

        if (findSpringChain(boneIndex) != EngineSetting.INDEX_NOT_FOUND || isIkBone(boneIndex))
            return true;

        for (int i = 0; i < added.size(); i++)
            if (namesBone(added.get(i), boneNames.get(boneIndex)))
                return true;

        return false;
    }

    private boolean isIkBone(int boneIndex) {

        ArpgObjectStruct rigArpg = getRigArpg();

        if (rigArpg == null || !ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_IK))
            return false;

        ArpgArrayStruct chains = rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_IK);

        for (int i = 0; i < chains.size(); i++)
            if (namesBone(chains.get(i).getAsObject(), boneNames.get(boneIndex)))
                return true;

        return false;
    }

    // A bone's one child, or none when it has none or several
    private int findOnlyChild(int boneIndex) {

        int child = EngineSetting.INDEX_NOT_FOUND;

        for (int i = boneIndex + 1; i < boneCount; i++)
            if (parents.getInt(i) == boneIndex) {

                if (child != EngineSetting.INDEX_NOT_FOUND)
                    return EngineSetting.INDEX_NOT_FOUND;

                child = i;
            }

        return child;
    }

    private ArpgObjectStruct buildSpringChain(String name, ArpgArrayStruct chainBones) {

        ArpgObjectStruct chainArpg = new ArpgObjectStruct();

        chainArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, name);
        chainArpg.add(EditorSetting.CHARACTER_RIG_FIELD_BONES, chainBones);

        for (int i = 0; i < EditorSetting.CHARACTER_RIG_SPRING_FIELDS.length; i++)
            chainArpg.addProperty(
                    EditorSetting.CHARACTER_RIG_SPRING_FIELDS[i],
                    EditorSetting.CHARACTER_RIG_SPRING_DEFAULTS[i]);

        return chainArpg;
    }

    // The spring chain whose bones name a bone, by its place in the rig's springs; INDEX_NOT_FOUND when none does
    int findSpringChain(int boneIndex) {

        ArpgArrayStruct chains = getSpringsArpg();

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND || chains == null)
            return EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < chains.size(); i++)
            if (namesBone(chains.get(i).getAsObject(), boneNames.get(boneIndex)))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean namesBone(ArpgObjectStruct chainArpg, String boneName) {

        ArpgArrayStruct bones = chainArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES);

        for (int i = 0; i < bones.size(); i++)
            if (bones.get(i).getAsString().equals(boneName))
                return true;

        return false;
    }

    private String readSpringBone(ArpgObjectStruct chainArpg, int joint) {
        return chainArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES).get(joint).getAsString();
    }

    private ArpgArrayStruct getSpringsArpg() {

        ArpgObjectStruct rigArpg = getRigArpg();

        return rigArpg != null && ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_SPRINGS)
                ? rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS)
                : null;
    }

    String getSpringName(int chainIndex) {
        return ArpgUtility.getString(
                getSpringsArpg().get(chainIndex).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_NAME, "");
    }

    // One setting of a spring chain, by its place in CHARACTER_RIG_SPRING_FIELDS
    float getSpringValue(int chainIndex, int field) {
        return ArpgUtility.getFloat(
                getSpringsArpg().get(chainIndex).getAsObject(),
                EditorSetting.CHARACTER_RIG_SPRING_FIELDS[field],
                EditorSetting.CHARACTER_RIG_SPRING_DEFAULTS[field]);
    }

    // One setting of a spring chain written, held to its range, and simulated so at once
    void setSpringValue(int chainIndex, int field, float value) {

        float written = CharacterViewUtility.round(
                CharacterViewUtility.clamp(value, 0f, EditorSetting.CHARACTER_RIG_SPRING_MAXIMA[field]));

        editRig(rig -> rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_SPRINGS).get(chainIndex).getAsObject()
                .addProperty(EditorSetting.CHARACTER_RIG_SPRING_FIELDS[field], written));
    }

    // IK \\

    // An IK handle ending at a bone, its root two bones up, and one ending at its twin across the body; none where
    // a bone already ends one or has no two bones above it
    void addIkChain(int boneIndex) {

        ObjectArrayList<ArpgObjectStruct> added = new ObjectArrayList<>();

        layIkChain(boneIndex, added);
        layIkChain(findTwin(boneIndex), added);

        if (added.isEmpty()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_IK_NONE);
            return;
        }

        editRig(rig -> {

            ArpgArrayStruct chains = CharacterViewUtility.requireArray(rig, EditorSetting.CHARACTER_RIG_FIELD_IK);

            for (int i = 0; i < added.size(); i++)
                chains.add(added.get(i));
        });
        characterEditorManager.setStatusMessage(added.size() + EditorSetting.CHARACTER_RIG_MESSAGE_IK_ADDED);
    }

    // The IK handles ending at a bone and at its twin removed
    void removeIkChain(int boneIndex) {

        int found = countIkChains(boneIndex) + countIkChains(findTwin(boneIndex));

        if (found == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_IK_NONE);
            return;
        }

        String endName = boneNames.get(boneIndex);
        int twin = findTwin(boneIndex);
        String twinName = twin != EngineSetting.INDEX_NOT_FOUND ? boneNames.get(twin) : endName;

        editRig(rig -> {

            ArpgArrayStruct chains = rig.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_IK);

            for (int i = chains.size() - 1; i >= 0; i--) {

                String end = readIkEnd(chains.get(i).getAsObject());

                if (end.equals(endName) || end.equals(twinName))
                    chains.remove(i);
            }
        });
        characterEditorManager.setStatusMessage(found + EditorSetting.CHARACTER_RIG_MESSAGE_IK_REMOVED);
    }

    private void layIkChain(int end, ObjectArrayList<ArpgObjectStruct> added) {

        if (end == EngineSetting.INDEX_NOT_FOUND || countIkChains(end) > 0)
            return;

        int middle = parents.getInt(end);
        int root = middle != EngineSetting.INDEX_NOT_FOUND ? parents.getInt(middle) : EngineSetting.INDEX_NOT_FOUND;

        if (root == EngineSetting.INDEX_NOT_FOUND)
            return;

        for (int i = 0; i < added.size(); i++)
            if (readIkEnd(added.get(i)).equals(boneNames.get(end)))
                return;

        ArpgObjectStruct chainArpg = new ArpgObjectStruct();
        ArpgArrayStruct bones = new ArpgArrayStruct();
        Vector3 pole = resolvePole(root, middle, end);
        float scale = EditorSetting.CHARACTER_RIG_IK_POLE_SCALE;

        bones.add(boneNames.get(root));
        bones.add(boneNames.get(middle));
        bones.add(boneNames.get(end));
        chainArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME,
                boneNames.get(end) + EditorSetting.CHARACTER_RIG_IK_SUFFIX);
        chainArpg.add(EditorSetting.CHARACTER_RIG_FIELD_BONES, bones);
        writeVector(chainArpg, EditorSetting.CHARACTER_RIG_FIELD_POLE, pole.set(
                Math.round(pole.x * scale) / scale,
                Math.round(pole.y * scale) / scale,
                Math.round(pole.z * scale) / scale));
        added.add(chainArpg);
    }

    // The way a chain's middle joint stands off the line from its root to its end, or where it stands straight, out
    // across the chain from the front, a finger's knuckle away from the palm, mirrored on the right
    private Vector3 resolvePole(int root, int middle, int end) {

        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        Vector3 c = new Vector3();

        CharacterPoseUtility.getTranslation(worldMatrices.get(root), a);
        CharacterPoseUtility.getTranslation(worldMatrices.get(middle), b);
        CharacterPoseUtility.getTranslation(worldMatrices.get(end), c);

        Vector3 line = new Vector3(c).subtract(a);
        float lengthSquared = line.lengthSquared();
        float along = lengthSquared > 0f ? new Vector3(b).subtract(a).dot(line) / lengthSquared : 0f;
        Vector3 off = new Vector3(b).subtract(new Vector3(line).multiply(along).add(a));

        if (off.length() >= EditorSetting.CHARACTER_RIG_IK_POLE_MIN)
            return off.divide(off.length());

        Vector3 axis = lengthSquared > 0f ? line.divide((float) Math.sqrt(lengthSquared)) : line.set(0f, -1f, 0f);
        Vector3 pole = new Vector3(-axis.y, axis.x, 0f);

        String side = CharacterMeshUtility.toSideSuffix(boneNames.get(end));

        if (side.equals(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT))
            pole.x = -pole.x;

        float length = pole.length();
        return length > 0f ? pole.divide(length) : pole.set(0f, 0f, 1f);
    }

    private int countIkChains(int end) {

        ArpgObjectStruct rigArpg = getRigArpg();

        if (end == EngineSetting.INDEX_NOT_FOUND || rigArpg == null
                || !ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_IK))
            return 0;

        ArpgArrayStruct chains = rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_IK);
        int count = 0;

        for (int i = 0; i < chains.size(); i++)
            if (readIkEnd(chains.get(i).getAsObject()).equals(boneNames.get(end)))
                count++;

        return count;
    }

    private String readIkEnd(ArpgObjectStruct chainArpg) {

        ArpgArrayStruct bones = chainArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES);
        return bones.get(bones.size() - 1).getAsString();
    }

    private int findTwin(int boneIndex) {

        if (boneIndex == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        int twin = findBone(CharacterMeshUtility.toMirrorBoneName(boneNames.get(boneIndex)));
        return twin != boneIndex ? twin : EngineSetting.INDEX_NOT_FOUND;
    }

    // The first free name of the editor's new bones, named as the region worked in names its own, carrying the
    // parent's side so the bone mirrors with it
    private String resolveNewName(String parentName) {

        String side = resolveSide(parentName);
        CharacterEditorRegion region = characterEditorManager.getRegion();
        String stem = region.hasBonePrefix() ? region.getBonePrefix() : EditorSetting.CHARACTER_RIG_NEW_BONE_NAME;

        for (int number = 1;; number++) {

            String boneName = stem + number + side;

            if (findBone(boneName) == EngineSetting.INDEX_NOT_FOUND)
                return boneName;
        }
    }

    private String resolveSide(String boneName) {

        if (boneName.endsWith(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT))
            return EditorSetting.CHARACTER_MESH_BONE_SUFFIX_LEFT;

        if (boneName.endsWith(EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT))
            return EditorSetting.CHARACTER_MESH_BONE_SUFFIX_RIGHT;

        return "";
    }

    // A bone added since the editor started and every bone below it removed
    void removeBone(int boneIndex) {

        if (!canRename(boneIndex)) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_LOADED_BONE);
            return;
        }

        String boneName = boneNames.get(boneIndex);
        int parent = parents.getInt(boneIndex);

        markSubtree(boneIndex);
        edit(bones -> {
            for (int i = boneCount - 1; i >= 0; i--)
                if (subtree.getBoolean(i))
                    bones.remove(i);
        });
        this.selectedBone = parent;
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_REMOVED + boneName);
    }

    boolean isBoneNameValid(int boneIndex, String boneName) {
        return canRename(boneIndex)
                && FileUtility.isValidFileName(boneName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && (boneName.equals(boneNames.get(boneIndex)) || findBone(boneName) == EngineSetting.INDEX_NOT_FOUND);
    }

    // A bone added since the editor started given a new name, its children following it
    void renameBone(int boneIndex, String boneName) {

        if (!isBoneNameValid(boneIndex, boneName))
            return;

        String oldName = boneNames.get(boneIndex);

        edit(bones -> {
            for (int i = 0; i < bones.size(); i++) {

                ArpgObjectStruct boneArpg = bones.get(i).getAsObject();

                if (i == boneIndex)
                    boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, boneName);
                else if (oldName.equals(ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_PARENT, "")))
                    boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, boneName);
            }
        });
    }

    // Only a bone nothing loaded can name yet takes a new name or goes; the root never does
    boolean canRename(int boneIndex) {
        return boneIndex > 0 && boneIndex < boneCount && rigHandle != null
                && !rigHandle.hasBone(boneNames.get(boneIndex));
    }

    // Parent \\

    // The bone moved under the next or previous bone that is neither itself nor below it, its joint kept in place
    void cycleParent(int boneIndex, int direction) {

        if (boneIndex <= 0 || boneIndex >= boneCount)
            return;

        markSubtree(boneIndex);

        int parent = parents.getInt(boneIndex);
        int candidate = parent;

        do {
            candidate = Math.floorMod(candidate + direction, boneCount);
        } while (subtree.getBoolean(candidate) && candidate != parent);

        if (candidate == parent)
            return;

        reparent(boneIndex, candidate);
    }

    private void reparent(int boneIndex, int parent) {

        String boneName = boneNames.get(boneIndex);
        String parentName = boneNames.get(parent);
        Vector3 position = new Vector3();
        Vector3 rotation = new Vector3();

        matrixScratchA.set(worldMatrices.get(parent)).inverse();
        matrixScratchC.set(matrixScratchA).multiply(worldMatrices.get(boneIndex));
        CharacterPoseUtility.getTranslation(matrixScratchC, position);
        CharacterPoseUtility.extractRotation(matrixScratchC, matrixScratchB);
        CharacterPoseUtility.toEuler(matrixScratchB, rotations.get(boneIndex), rotation);

        edit(bones -> {

            ArpgObjectStruct boneArpg = bones.get(boneIndex).getAsObject();

            boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, parentName);
            writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, position);

            if (boneArpg.has(EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION) || rotation.lengthSquared() > 0f)
                writeVector(boneArpg, EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, rotation);

            reorder(bones);
        });

        this.selectedBone = findBone(boneName);
    }

    // The bones laid out again parent before child, each keeping its place among those free to stand there; a bone
    // naming a parent the rig lacks keeps its turn at the end, for the builder to refuse with its reason
    private void reorder(ArpgArrayStruct bones) {

        ObjectArrayList<ArpgObjectStruct> ordered = new ObjectArrayList<>();
        ObjectArrayList<String> placed = new ObjectArrayList<>();

        boolean progressed = true;

        orderScratch.clear();

        while (progressed && ordered.size() < bones.size())
            progressed = placeFree(bones, ordered, placed);

        for (int i = 0; i < bones.size(); i++)
            if (!orderScratch.contains(i))
                ordered.add(bones.get(i).getAsObject());

        for (int i = 0; i < ordered.size(); i++)
            bones.set(i, ordered.get(i));
    }

    // One pass placing every bone whose parent is already placed; false when the pass placed none
    private boolean placeFree(
            ArpgArrayStruct bones,
            ObjectArrayList<ArpgObjectStruct> ordered,
            ObjectArrayList<String> placed) {

        boolean progressed = false;

        for (int i = 0; i < bones.size(); i++) {

            ArpgObjectStruct boneArpg = bones.get(i).getAsObject();
            String parent = ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_PARENT, "");

            if (!orderScratch.contains(i) && (parent.isEmpty() || placed.contains(parent))) {
                orderScratch.add(i);
                ordered.add(boneArpg);
                placed.add(ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));
                progressed = true;
            }
        }

        return progressed;
    }

    // Mirror \\

    // The selected bone and every bone below it given a twin across the body, made where none exists yet
    void mirrorBone(int boneIndex) {

        if (boneIndex <= 0 || boneIndex >= boneCount)
            return;

        markSubtree(boneIndex);

        for (int i = 0; i < boneCount; i++)
            if (subtree.getBoolean(i) && resolveSide(boneNames.get(i)).isEmpty()) {
                characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_RIG_MESSAGE_NO_SIDE);
                return;
            }

        edit(bones -> {
            for (int i = 0; i < boneCount; i++)
                if (subtree.getBoolean(i))
                    mirrorInto(bones, i);
        });
        characterEditorManager.setStatusMessage(
                EditorSetting.CHARACTER_RIG_MESSAGE_MIRRORED + boneNames.get(boneIndex));
    }

    // Every bone on one side handed to its existing twin on the other, mirrored across the body
    void symmetrize(String fromSide) {

        edit(bones -> {
            for (int i = 0; i < boneCount; i++)
                if (boneNames.get(i).endsWith(fromSide)
                        && findBone(CharacterMeshUtility.toMirrorBoneName(boneNames.get(i)))
                                != EngineSetting.INDEX_NOT_FOUND)
                    mirrorInto(bones, i);
        });
    }

    // A bone's bind pose written onto its twin across x, the twin made under the parent's twin where it is missing
    private void mirrorInto(ArpgArrayStruct bones, int boneIndex) {

        String twinName = CharacterMeshUtility.toMirrorBoneName(boneNames.get(boneIndex));
        ArpgObjectStruct twin = findBoneArpg(bones, twinName);

        if (twin == null) {

            String parentName = boneNames.get(parents.getInt(boneIndex));
            String twinParent = CharacterMeshUtility.toMirrorBoneName(parentName);

            twin = new ArpgObjectStruct();
            twin.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, twinName);
            twin.addProperty(
                    EditorSetting.CHARACTER_RIG_FIELD_PARENT,
                    findBoneArpg(bones, twinParent) != null ? twinParent : parentName);
            bones.add(twin);
        }

        Vector3 position = positions.get(boneIndex);
        Vector3 rotation = rotations.get(boneIndex);

        writeVector(twin, EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, vectorScratch.set(-position.x, position.y,
                position.z));

        if (rotation.lengthSquared() > 0f || twin.has(EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION))
            writeVector(twin, EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, vectorScratch.set(rotation.x,
                    -rotation.y, -rotation.z));

        writeVector(twin, EditorSetting.CHARACTER_EDITOR_FIELD_SIZE, sizes.get(boneIndex));
    }

    private ArpgObjectStruct findBoneArpg(ArpgArrayStruct bones, String boneName) {

        for (int i = 0; i < bones.size(); i++) {

            ArpgObjectStruct boneArpg = bones.get(i).getAsObject();

            if (boneName.equals(ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, "")))
                return boneArpg;
        }

        return null;
    }

    // Marks a bone and every bone below it, which in parent-before-child order all come after it
    private void markSubtree(int boneIndex) {

        subtree.clear();
        subtree.size(boneCount);
        subtree.set(boneIndex, true);

        for (int i = boneIndex + 1; i < boneCount; i++) {

            int parent = parents.getInt(i);

            if (parent != EngineSetting.INDEX_NOT_FOUND && subtree.getBoolean(parent))
                subtree.set(i, true);
        }
    }

    // History \\

    boolean undo() {

        ArpgObjectStruct rigArpg = getRigArpg();

        return rigArpg != null && restore(history.undo(rigArpg));
    }

    boolean redo() {

        ArpgObjectStruct rigArpg = getRigArpg();

        return rigArpg != null && restore(history.redo(rigArpg));
    }

    private boolean restore(ArpgObjectStruct snapshot) {

        if (snapshot == null)
            return false;

        replace(snapshot);
        return true;
    }

    // File \\

    boolean isDirty() {
        return rigHandle != null
                && infoManager.isDocumentDirty(EditorSetting.INFO_SCHEMA_RIGS, rigHandle.getRigName());
    }

    // The rig written when it holds unsaved changes; false, with the refusal said, when it could not be
    boolean save() {

        if (!isDirty())
            return true;

        if (infoManager.saveDocument(EditorSetting.INFO_SCHEMA_RIGS, rigHandle.getRigName()))
            return true;

        characterEditorManager.setStatusMessage(infoManager.getRefusal());
        return false;
    }

    void revert() {

        if (!isDirty())
            return;

        history.clear();
        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_RIGS, rigHandle.getRigName());
    }

    // Selection \\

    void selectBone(int boneIndex) {

        if (boneIndex >= 0 && boneIndex < boneCount) {
            this.selectedBone = boneIndex;
            characterEditorManager.notifyChanged();
        }
    }

    // Measure \\

    // A bone's parent's turn in the world, or none for the root
    private void resolveParentRotation(int boneIndex, Matrix4 out) {

        int parent = parents.getInt(boneIndex);

        if (parent == EngineSetting.INDEX_NOT_FOUND)
            out.set(1f);
        else
            CharacterPoseUtility.extractRotation(worldMatrices.get(parent), out);
    }

    private float getAxis(Vector3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }

    // Status \\

    void appendStatus(StringBuilder status) {

        if (rigHandle == null)
            return;

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR).append(rigHandle.getRigName());

        if (isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);

        if (refusal != null)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_RIG_MESSAGE_REFUSED)
                    .append(refusal);
        else if (structureChanged)
            status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                    .append(EditorSetting.CHARACTER_RIG_MESSAGE_RESTART);
    }

    // Accessible \\

    boolean hasRig() {
        return rigHandle != null && boneCount > 0;
    }

    String getRigName() {
        return rigHandle != null ? rigHandle.getRigName() : "";
    }

    int getBoneCount() {
        return boneCount;
    }

    int getSelectedBone() {
        return selectedBone;
    }

    String getBoneName(int boneIndex) {
        return boneNames.get(boneIndex);
    }

    int getParent(int boneIndex) {
        return parents.getInt(boneIndex);
    }

    Vector3 getPosition(int boneIndex) {
        return positions.get(boneIndex);
    }

    Vector3 getRotation(int boneIndex) {
        return rotations.get(boneIndex);
    }

    Vector3 getSize(int boneIndex) {
        return sizes.get(boneIndex);
    }

    Matrix4 getWorldMatrix(int boneIndex) {
        return worldMatrices.get(boneIndex);
    }

    // True for a bone the loaded content is laid out by, false for one added since the editor started
    boolean isLoaded(int boneIndex) {
        return rigHandle != null && rigHandle.hasBone(boneNames.get(boneIndex));
    }
}
