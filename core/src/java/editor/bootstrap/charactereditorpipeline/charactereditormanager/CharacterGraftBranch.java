package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterLoopUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class CharacterGraftBranch extends BranchPackage {

    /*
     * Joins a body part to the character as one edit of its body and rig,
     * the way a modular kit's parts are merged: the part's bones are grafted
     * onto the rig first, each placed in the bind pose where the placement
     * carries it, its top bones under the bone it joins, all renamed for the
     * side they join and numbered past every name the rig holds, with the
     * part's layers; the rig applies live. Then the body opens where the part
     * joins, at the open loop it was aimed at or through a hole cut from the
     * faces within its plug's radius, and the part's vertices and faces come
     * in carried by the placement, weighed onto the grafted bones, with its
     * masks, shape keys, textures, zones, UVs, pins and seams; a mirrored part
     * winds its faces the other way so they still face out. The part's plug
     * loop meets the body's: loops of as many vertices lying close are welded
     * vertex to vertex, blending positions, weights, masks and shape keys;
     * any others are bridged with a strip of faces walked along both rims by
     * length, quads where the rims keep pace and triangles held as quads with
     * one corner doubled where they do not, as edge-loop bridging does. The
     * strip takes the body's texture and UVs, and the edge where the part's
     * own UV island begins is marked as a seam. A root part replaces the whole
     * body. A twin across the body joins in the same edit. Undo takes back
     * the rig and the mesh together. The part's animations then join the
     * rig's clips of their names, through CharacterClipBranch.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterWeightBranch characterWeightBranch;
    private CharacterClipBranch characterClipBranch;

    // Rig — the number every grafted name takes, the names the rig holds, each grafted bone's frame in the bind pose,
    // each stamp bone's bone in the rig, and the layers made for the part
    private int number;
    private ObjectOpenHashSet<String> takenNames;
    private ObjectArrayList<Matrix4> graftWorlds;
    private IntArrayList stampBoneRemap;
    private ObjectArrayList<ArpgObjectStruct> graftedParts;

    // Hole — the faces cut, each directed edge's face, and the flood's queue
    private BooleanArrayList faceFlags;
    private Long2IntOpenHashMap edgeFaces;
    private IntArrayList faceQueue;

    // Loops — the body's rim in its faces' winding, the part's against its own, and how far along each rim lies
    private LongOpenHashSet directedEdges;
    private IntArrayList loopNext;
    private ObjectArrayList<IntArrayList> loops;
    private IntArrayList hostLoop;
    private IntArrayList stampLoop;
    private FloatArrayList hostLengths;
    private FloatArrayList stampLengths;

    // Weld — each vertex's stand-in once welded, and the pair blended
    private IntArrayList vertexReplace;
    private IntArrayList weldVertices;
    private FloatArrayList weldShares;

    // Scratch
    private Vector3 pointScratch;
    private Vector3 centreScratch;
    private Vector3 normalScratch;
    private Vector3 eulerScratch;
    private Vector3 referenceScratch;
    private Matrix4 inverseScratch;
    private Matrix4 localScratch;
    private Matrix4 rotationScratch;

    // Base \\

    @Override
    protected void create() {

        // Rig
        this.takenNames = new ObjectOpenHashSet<>();
        this.graftWorlds = new ObjectArrayList<>();
        this.stampBoneRemap = new IntArrayList();
        this.graftedParts = new ObjectArrayList<>();

        // Hole
        this.faceFlags = new BooleanArrayList();
        this.edgeFaces = new Long2IntOpenHashMap();
        this.edgeFaces.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.faceQueue = new IntArrayList();

        // Loops
        this.directedEdges = new LongOpenHashSet();
        this.loopNext = new IntArrayList();
        this.loops = new ObjectArrayList<>();
        this.hostLoop = new IntArrayList();
        this.stampLoop = new IntArrayList();
        this.hostLengths = new FloatArrayList();
        this.stampLengths = new FloatArrayList();

        // Weld
        this.vertexReplace = new IntArrayList();
        this.weldVertices = new IntArrayList();
        this.weldShares = new FloatArrayList();

        // Scratch
        this.pointScratch = new Vector3();
        this.centreScratch = new Vector3();
        this.normalScratch = new Vector3();
        this.eulerScratch = new Vector3();
        this.referenceScratch = new Vector3();
        this.inverseScratch = new Matrix4();
        this.localScratch = new Matrix4();
        this.rotationScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
        this.characterClipBranch = get(CharacterClipBranch.class);
    }

    // Graft \\

    // Joins a part, and its twin across the body when one is given, as one edit of the body and its rig; false, with
    // the status saying why, when it could not join
    boolean graft(
            CharacterMeshDocumentInstance document,
            CharacterPartPlacementStruct placement,
            CharacterPartPlacementStruct twin) {

        ArpgObjectStruct rigBefore = characterRigBranch.copyRig();

        if (rigBefore == null)
            return false;

        StampHandle stampHandle = placement.getStampHandle();
        ArpgObjectStruct rigAfter = rigBefore.deepCopy();

        this.number = resolveNumber(rigAfter, stampHandle, placement, twin);
        graftedParts.clear();
        graftRig(rigAfter, stampHandle, placement, false);

        if (twin != null)
            graftRig(rigAfter, stampHandle, twin, true);

        characterHistoryBranch.begin(document, rigBefore, rigAfter);
        characterRigBranch.replaceRig(rigAfter);

        if (characterRigBranch.getRefusal() != null)
            return refuse(document, rigBefore, characterRigBranch.getRefusal());

        String refusal = graftMesh(document, placement);

        if (refusal == null && twin != null)
            refusal = graftMesh(document, twin);

        if (refusal != null)
            return refuse(document, rigBefore, refusal);

        characterSculptBranch.removeOrphans(document);

        if (!characterMeshBranch.finishEdit(document)) {
            characterRigBranch.replaceRig(rigBefore);
            return false;
        }

        int animated = characterClipBranch.join(stampHandle, placement, boneName -> rename(boneName, placement));

        if (twin != null)
            characterClipBranch.join(stampHandle, twin, boneName -> rename(boneName, twin));

        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_STAMP_MESSAGE_PLACED
                + stampHandle.getDisplayName()
                + (animated > 0 ? EditorSetting.CHARACTER_STAMP_MESSAGE_ANIMATED + animated : ""));

        return true;
    }

    // The body and rig put back as they stood, and the reason said
    private boolean refuse(CharacterMeshDocumentInstance document, ArpgObjectStruct rigBefore, String refusal) {

        characterHistoryBranch.cancel(document);
        characterRigBranch.replaceRig(rigBefore);
        characterMeshBranch.upload(document);
        characterEditorManager.setStatusMessage(refusal);

        return false;
    }

    // Names \\

    // The first number, none at all before the rest, at which every bone the part and its twin bring is a name the
    // rig does not hold
    private int resolveNumber(
            ArpgObjectStruct rigArpg,
            StampHandle stampHandle,
            CharacterPartPlacementStruct placement,
            CharacterPartPlacementStruct twin) {

        ArpgArrayStruct bones = ArpgUtility.validateArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_BONES);

        takenNames.clear();

        for (int i = 0; i < bones.size(); i++)
            takenNames.add(ArpgUtility.getString(bones.get(i).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_NAME,
                    ""));

        int candidate = 0;

        while (!isFree(stampHandle, placement, candidate) || twin != null && !isFree(stampHandle, twin, candidate))
            candidate = candidate == 0 ? EditorSetting.CHARACTER_STAMP_FIRST_NUMBER : candidate + 1;

        return candidate;
    }

    private boolean isFree(StampHandle stampHandle, CharacterPartPlacementStruct placement, int candidate) {

        ArpgArrayStruct stampBones = stampHandle.getBonesArpg();

        for (int i = 0; i < stampBones.size(); i++)
            if (takenNames.contains(rename(readName(stampBones, i), placement, candidate)))
                return false;

        return true;
    }

    // A stamp bone's name as it joins: its side swapped where the part is mirrored, or the side it joins on for a bone
    // of no side, numbered before the side when the number is not none
    private String rename(String boneName, CharacterPartPlacementStruct placement, int candidate) {

        String side = CharacterMeshUtility.toSideSuffix(boneName);
        String base = boneName.substring(0, boneName.length() - side.length());
        String joinedSide = side.isEmpty()
                ? placement.getSide()
                : placement.isMirrored() ? CharacterMeshUtility.toMirrorBoneName(side) : side;

        return candidate == 0
                ? base + joinedSide
                : base + EditorSetting.CHARACTER_STAMP_NUMBER_SEPARATOR + candidate + joinedSide;
    }

    private String rename(String boneName, CharacterPartPlacementStruct placement) {
        return rename(boneName, placement, number);
    }

    private static String readName(ArpgArrayStruct bones, int index) {
        return ArpgUtility.getString(bones.get(index).getAsObject(), EditorSetting.CHARACTER_RIG_FIELD_NAME, "");
    }

    // Rig \\

    // Every bone of the stamp added to the rig's entry where the placement carries it, then the part's layers; a twin
    // adds its bones to the layers the part made
    private void graftRig(
            ArpgObjectStruct rigArpg,
            StampHandle stampHandle,
            CharacterPartPlacementStruct placement,
            boolean twinPass) {

        ArpgArrayStruct bones = ArpgUtility.validateArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_BONES);
        ArpgArrayStruct stampBones = stampHandle.getBonesArpg();
        RigHandle stampRig = stampHandle.getRigHandle();
        Matrix4 hostWorld = characterRigBranch.getWorldMatrix(
                Math.max(0, characterRigBranch.findBoneIndex(placement.getHostBoneName())));

        while (graftWorlds.size() < stampBones.size())
            graftWorlds.add(new Matrix4());

        for (int i = 0; i < stampBones.size(); i++) {

            ArpgObjectStruct source = stampBones.get(i).getAsObject();
            String boneName = readName(stampBones, i);
            String parentName = ArpgUtility.getString(source, EditorSetting.CHARACTER_RIG_FIELD_PARENT, "");
            int parent = findStampBone(stampBones, parentName);
            Matrix4 world = graftWorlds.get(i);

            resolveGraftWorld(stampRig.getBindWorldMatrix(stampRig.getBoneIndex(boneName)), placement, world);
            inverseScratch.set(parent == EngineSetting.INDEX_NOT_FOUND ? hostWorld : graftWorlds.get(parent))
                    .inverse();
            localScratch.set(inverseScratch).multiply(world);
            bones.add(writeBone(source, boneName, parent == EngineSetting.INDEX_NOT_FOUND
                    ? placement.getHostBoneName()
                    : rename(parentName, placement), placement));
        }

        graftParts(rigArpg, stampHandle, placement, twinPass);
    }

    // A stamp bone's frame in the bind pose: its turn reflected where the part is mirrored, then carried by the
    // placement's turn, its joint carried by the whole placement
    private void resolveGraftWorld(Matrix4 stampWorld, CharacterPartPlacementStruct placement, Matrix4 out) {

        CharacterPoseUtility.extractRotation(stampWorld, rotationScratch);

        if (placement.isMirrored())
            CharacterPoseUtility.reflectX(rotationScratch, rotationScratch);

        out.set(placement.getRotation()).multiply(rotationScratch);
        CharacterPoseUtility.transformPoint(
                placement.getTransform(), stampWorld.val[12], stampWorld.val[13], stampWorld.val[14], pointScratch);
        out.val[12] = pointScratch.x;
        out.val[13] = pointScratch.y;
        out.val[14] = pointScratch.z;
    }

    // The stamp's bone entry as it joins: renamed, under its parent, placed and turned by the local frame just
    // resolved and sized by the placement's scale, every other field kept
    private ArpgObjectStruct writeBone(
            ArpgObjectStruct source,
            String boneName,
            String parentName,
            CharacterPartPlacementStruct placement) {

        ArpgObjectStruct boneArpg = source.deepCopy();
        String rotationKey = EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION;
        String sizeKey = EditorSetting.CHARACTER_EDITOR_FIELD_SIZE;
        float scale = placement.getScale();

        referenceScratch.set(
                CharacterViewUtility.getComponent(source, rotationKey, 0, 0f),
                CharacterViewUtility.getComponent(source, rotationKey, 1, 0f),
                CharacterViewUtility.getComponent(source, rotationKey, 2, 0f));
        CharacterPoseUtility.extractRotation(localScratch, rotationScratch);
        CharacterPoseUtility.toEuler(rotationScratch, referenceScratch, eulerScratch);

        boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, rename(boneName, placement));
        boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, parentName);
        boneArpg.add(EditorSetting.CHARACTER_EDITOR_FIELD_POSITION,
                CharacterViewUtility.toVector(localScratch.val[12], localScratch.val[13], localScratch.val[14]));
        boneArpg.add(rotationKey, CharacterViewUtility.toVector(eulerScratch.x, eulerScratch.y, eulerScratch.z));
        boneArpg.add(sizeKey, CharacterViewUtility.toVector(
                CharacterViewUtility.getComponent(source, sizeKey, 0, EngineSetting.DEFAULT_BONE_SIZE) * scale,
                CharacterViewUtility.getComponent(source, sizeKey, 1, EngineSetting.DEFAULT_BONE_SIZE) * scale,
                CharacterViewUtility.getComponent(source, sizeKey, 2, EngineSetting.DEFAULT_BONE_SIZE) * scale));

        return boneArpg;
    }

    // Each of the stamp's layers added under a name the rig does not hold, its bones renamed; a twin's bones join the
    // layers the part made, so a pair of twins is one sided layer
    private void graftParts(
            ArpgObjectStruct rigArpg,
            StampHandle stampHandle,
            CharacterPartPlacementStruct placement,
            boolean twinPass) {

        ArpgArrayStruct stampParts = stampHandle.getPartsArpg();

        if (stampParts.size() == 0)
            return;

        if (!ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_PARTS))
            rigArpg.add(EditorSetting.CHARACTER_RIG_FIELD_PARTS, new ArpgArrayStruct());

        ArpgArrayStruct parts = rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_PARTS);

        for (int p = 0; p < stampParts.size(); p++) {

            ArpgObjectStruct source = stampParts.get(p).getAsObject();
            ArpgArrayStruct sourceBones = ArpgUtility.validateArray(source, EditorSetting.CHARACTER_RIG_FIELD_BONES);
            ArpgObjectStruct partArpg;

            if (twinPass)
                partArpg = graftedParts.get(p);
            else {
                partArpg = source.deepCopy();
                partArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_NAME, resolvePartName(parts,
                        ArpgUtility.getString(source, EditorSetting.CHARACTER_RIG_FIELD_NAME, "")));
                partArpg.add(EditorSetting.CHARACTER_RIG_FIELD_BONES, new ArpgArrayStruct());
                parts.add(partArpg);
                graftedParts.add(partArpg);
            }

            ArpgArrayStruct partBones = partArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_BONES);

            for (int b = 0; b < sourceBones.size(); b++)
                partBones.add(rename(sourceBones.get(b).getAsString(), placement));
        }
    }

    // A layer's name with the part's number where the rig holds it already, counting on past any taken
    private String resolvePartName(ArpgArrayStruct parts, String partName) {

        String candidate = partName;
        int next = Math.max(number, EditorSetting.CHARACTER_STAMP_FIRST_NUMBER);

        while (hasPart(parts, candidate))
            candidate = partName + EditorSetting.CHARACTER_STAMP_NUMBER_SEPARATOR + next++;

        return candidate;
    }

    private boolean hasPart(ArpgArrayStruct parts, String partName) {

        for (int i = 0; i < parts.size(); i++)
            if (partName.equals(ArpgUtility.getString(parts.get(i).getAsObject(),
                    EditorSetting.CHARACTER_RIG_FIELD_NAME, "")))
                return true;

        return false;
    }

    private static int findStampBone(ArpgArrayStruct stampBones, String boneName) {

        if (boneName.isEmpty())
            return EngineSetting.INDEX_NOT_FOUND;

        for (int i = 0; i < stampBones.size(); i++)
            if (readName(stampBones, i).equals(boneName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Mesh \\

    // The part joined to the body where the placement says; the reason it could not be, null when it was
    private String graftMesh(CharacterMeshDocumentInstance document, CharacterPartPlacementStruct placement) {

        RiggedMeshStruct mesh = document.getMesh();
        StampHandle stampHandle = placement.getStampHandle();

        resolveStampBoneRemap(mesh.getRigHandle(), stampHandle, placement);

        if (placement.isRoot()) {

            int rootBase = mesh.getVertexCount();

            flagFaces(mesh, 0, true);
            mesh.removeFaces(faceFlags);
            appendStamp(mesh, stampHandle.getMesh(), placement);
            resetReplace(mesh);
            appendSeams(mesh, stampHandle.getMesh(), rootBase);
            return null;
        }

        if (!placement.isOnLoop() && !cutHole(mesh, placement))
            return EditorSetting.CHARACTER_STAMP_MESSAGE_NO_HOLE;

        int hostCount = CharacterLoopUtility.collectLoops(mesh, null, directedEdges, loopNext, loops);
        int host = CharacterLoopUtility.findNearestLoop(mesh, loops, hostCount, placement.getPoint(), centreScratch);

        if (host == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_STAMP_MESSAGE_NO_HOLE;

        hostLoop.clear();
        hostLoop.addAll(loops.get(host));

        int faceBase = mesh.getFaceCount();
        int vertexBase = mesh.getVertexCount();

        appendStamp(mesh, stampHandle.getMesh(), placement);
        flagFaces(mesh, faceBase, true);

        Vector3 plug = stampHandle.getPlug().getPosition();

        CharacterPoseUtility.transformPoint(placement.getTransform(), plug.x, plug.y, plug.z, pointScratch);

        int stampCount = CharacterLoopUtility.collectLoops(mesh, faceFlags, directedEdges, loopNext, loops);
        int stamp = CharacterLoopUtility.findNearestLoop(mesh, loops, stampCount, pointScratch, centreScratch);

        if (stamp == EngineSetting.INDEX_NOT_FOUND)
            return EditorSetting.CHARACTER_STAMP_MESSAGE_NO_LOOP;

        IntArrayList found = loops.get(stamp);

        stampLoop.clear();

        for (int i = found.size() - 1; i >= 0; i--)
            stampLoop.add(found.getInt(i));

        resetReplace(mesh);
        join(mesh, placement, faceBase);
        appendSeams(mesh, stampHandle.getMesh(), vertexBase);

        return null;
    }

    // Each of the stamp rig's bones as the body's rig now holds it: the host placeholder the bone joined, every other
    // by the name it joined under
    private void resolveStampBoneRemap(
            RigHandle rigHandle,
            StampHandle stampHandle,
            CharacterPartPlacementStruct placement) {

        RigHandle stampRig = stampHandle.getRigHandle();

        stampBoneRemap.clear();

        for (int i = 0; i < stampRig.getBoneCount(); i++) {

            String boneName = i == 0
                    ? placement.getHostBoneName()
                    : rename(stampRig.getBone(i).getName(), placement);

            stampBoneRemap.add(rigHandle.hasBone(boneName) ? rigHandle.getBoneIndex(boneName) : 0);
        }
    }

    // Every face from one on flagged as given, those before it the other way
    private void flagFaces(RiggedMeshStruct mesh, int from, boolean flagged) {

        faceFlags.clear();
        faceFlags.size(mesh.getFaceCount());

        for (int f = 0; f < mesh.getFaceCount(); f++)
            faceFlags.set(f, f >= from == flagged);
    }

    // Hole \\

    // The faces facing out where the part joins, flooded across edges from the surface face nearest the point while
    // within the hole's radius of it, any face left with only cut faces about it and any card standing in the hole,
    // removed; false where no face faces it
    private boolean cutHole(RiggedMeshStruct mesh, CharacterPartPlacementStruct placement) {

        Vector3 point = placement.getPoint();
        Vector3 normal = placement.getNormal();
        float reach = placement.getRadius() * EditorSetting.CHARACTER_STAMP_HOLE_SHARE;
        int start = EngineSetting.INDEX_NOT_FOUND;
        float nearest = Float.POSITIVE_INFINITY;

        edgeFaces.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (a != b)
                    edgeFaces.put(CharacterLoopUtility.toDirectedKey(a, b), f);
            }

            float distance = measureFace(mesh, f, point);

            if (distance < nearest && normalScratch.dot(normal) > 0f && CharacterLoopUtility.isSurface(mesh, f)) {
                nearest = distance;
                start = f;
            }
        }

        if (start == EngineSetting.INDEX_NOT_FOUND)
            return false;

        flagFaces(mesh, mesh.getFaceCount(), true);
        faceFlags.set(start, true);
        faceQueue.clear();
        faceQueue.add(start);

        for (int q = 0; q < faceQueue.size(); q++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int neighbour = findNeighbour(mesh, faceQueue.getInt(q), c);

                if (neighbour == EngineSetting.INDEX_NOT_FOUND || faceFlags.getBoolean(neighbour))
                    continue;

                if (measureFace(mesh, neighbour, point) <= reach && normalScratch.dot(normal) > 0f) {
                    faceFlags.set(neighbour, true);
                    faceQueue.add(neighbour);
                }
            }

        closeHole(mesh);
        flagCards(mesh, point, reach);
        mesh.removeFaces(faceFlags);

        return true;
    }

    // Every card (hair or fur) standing within the hole's reach, cut with the surface it grew from
    private void flagCards(RiggedMeshStruct mesh, Vector3 point, float reach) {

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (!CharacterLoopUtility.isSurface(mesh, f) && measureFace(mesh, f, point) <= reach)
                faceFlags.set(f, true);
    }

    // Any face whose every neighbour is cut is cut with them, until none is left inside the hole
    private void closeHole(RiggedMeshStruct mesh) {

        boolean changed = true;

        while (changed) {

            changed = false;

            for (int f = 0; f < mesh.getFaceCount(); f++) {

                if (faceFlags.getBoolean(f))
                    continue;

                boolean enclosed = true;

                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT && enclosed; c++) {

                    if (mesh.getFaceVertex(f, c) == mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT))
                        continue;

                    int neighbour = findNeighbour(mesh, f, c);

                    enclosed = neighbour != EngineSetting.INDEX_NOT_FOUND && faceFlags.getBoolean(neighbour);
                }

                if (enclosed) {
                    faceFlags.set(f, true);
                    changed = true;
                }
            }
        }
    }

    // The face across a face's edge from one corner to the next, INDEX_NOT_FOUND at an open edge
    private int findNeighbour(RiggedMeshStruct mesh, int faceIndex, int corner) {

        int a = mesh.getFaceVertex(faceIndex, corner);
        int b = mesh.getFaceVertex(faceIndex, (corner + 1) % EngineSetting.QUAD_VERTEX_COUNT);

        return a == b ? EngineSetting.INDEX_NOT_FOUND : edgeFaces.get(CharacterLoopUtility.toDirectedKey(b, a));
    }

    // How far a face's centre lies from a point, its Newell normal left in the normal scratch
    private float measureFace(RiggedMeshStruct mesh, int faceIndex, Vector3 point) {

        centreScratch.set(0f, 0f, 0f);
        normalScratch.set(0f, 0f, 0f);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int a = mesh.getFaceVertex(faceIndex, c);
            int b = mesh.getFaceVertex(faceIndex, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

            centreScratch.add(mesh.getX(a), mesh.getY(a), mesh.getZ(a));
            normalScratch.add(
                    (mesh.getY(a) - mesh.getY(b)) * (mesh.getZ(a) + mesh.getZ(b)),
                    (mesh.getZ(a) - mesh.getZ(b)) * (mesh.getX(a) + mesh.getX(b)),
                    (mesh.getX(a) - mesh.getX(b)) * (mesh.getY(a) + mesh.getY(b)));
        }

        centreScratch.divide(EngineSetting.QUAD_VERTEX_COUNT);

        float dx = centreScratch.x - point.x;
        float dy = centreScratch.y - point.y;
        float dz = centreScratch.z - point.z;

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Append \\

    // Every vertex, face and shape key of the stamp brought into the body, carried by the placement
    private void appendStamp(
            RiggedMeshStruct mesh,
            RiggedMeshStruct stampMesh,
            CharacterPartPlacementStruct placement) {

        int vertexBase = mesh.getVertexCount();
        float[] m = placement.getTransform().val;

        for (int v = 0; v < stampMesh.getVertexCount(); v++) {

            CharacterPoseUtility.transformPoint(
                    placement.getTransform(), stampMesh.getX(v), stampMesh.getY(v), stampMesh.getZ(v), pointScratch);

            int copy = mesh.addVertex(0f, 0f, 0f);

            CharacterMeshUtility.setRoundedPosition(mesh, copy, pointScratch.x, pointScratch.y, pointScratch.z);

            for (int n = 0; n < stampMesh.getInfluenceCount(v); n++) {

                int bone = stampBoneRemap.getInt(stampMesh.getInfluenceBone(v, n));
                int existing = mesh.findInfluence(copy, bone);
                float weight = stampMesh.getInfluenceWeight(v, n);

                if (existing == EngineSetting.INDEX_NOT_FOUND)
                    mesh.addInfluence(copy, bone, weight);
                else
                    mesh.setInfluenceWeight(copy, existing, mesh.getInfluenceWeight(copy, existing) + weight);
            }

            for (int ch = 0; ch < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; ch++)
                mesh.setMask(copy, ch, stampMesh.getMask(v, ch));
        }

        int[] order = placement.isMirrored()
                ? EditorSetting.CHARACTER_STAMP_MIRRORED_CORNERS
                : EditorSetting.CHARACTER_STAMP_CORNERS;

        for (int f = 0; f < stampMesh.getFaceCount(); f++)
            appendFace(mesh, stampMesh, f, vertexBase, order);

        for (int s = 0; s < stampMesh.getMorphCount(); s++) {

            int morph = mesh.resolveMorph(stampMesh.getMorphName(s));

            for (int e = 0; e < stampMesh.getMorphEntryCount(s); e++) {

                float dx = stampMesh.getMorphDelta(s, e, 0);
                float dy = stampMesh.getMorphDelta(s, e, 1);
                float dz = stampMesh.getMorphDelta(s, e, 2);

                mesh.setMorphDelta(
                        morph,
                        vertexBase + stampMesh.getMorphVertex(s, e),
                        m[0] * dx + m[4] * dy + m[8] * dz,
                        m[1] * dx + m[5] * dy + m[9] * dz,
                        m[2] * dx + m[6] * dy + m[10] * dz);
            }
        }
    }

    // One stamp face with its corners in the order given, so a mirrored part still faces out, its UVs and pins
    // following its corners
    private void appendFace(
            RiggedMeshStruct mesh,
            RiggedMeshStruct stampMesh,
            int faceIndex,
            int vertexBase,
            int[] order) {

        int copy = mesh.addFace(
                vertexBase + stampMesh.getFaceVertex(faceIndex, order[0]),
                vertexBase + stampMesh.getFaceVertex(faceIndex, order[1]),
                vertexBase + stampMesh.getFaceVertex(faceIndex, order[2]),
                vertexBase + stampMesh.getFaceVertex(faceIndex, order[3]),
                mesh.resolveTexture(stampMesh.getTextureName(stampMesh.getFaceTexture(faceIndex))));
        int pins = stampMesh.getFacePins(faceIndex);
        int joinedPins = 0;

        mesh.setFaceZone(copy, stampMesh.getFaceZone(faceIndex));

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            if (stampMesh.hasAuthoredUVs(faceIndex))
                mesh.setFaceUV(
                        copy,
                        c,
                        stampMesh.getFaceU(faceIndex, order[c]),
                        stampMesh.getFaceV(faceIndex, order[c]));

            if ((pins >> order[c] & 1) != 0)
                joinedPins |= 1 << c;
        }

        mesh.setFacePins(copy, joinedPins);
    }

    // Every seam of the stamp marked on the vertices it came in as, or those they were welded onto
    private void appendSeams(RiggedMeshStruct mesh, RiggedMeshStruct stampMesh, int vertexBase) {

        long[] seams = stampMesh.getSortedSeams();

        for (int i = 0; i < seams.length; i++)
            mesh.setSeam(
                    vertexReplace.getInt(vertexBase + RiggedMeshStruct.getSeamVertex(seams[i], 0)),
                    vertexReplace.getInt(vertexBase + RiggedMeshStruct.getSeamVertex(seams[i], 1)),
                    true);
    }

    // Every vertex its own stand-in until welded
    private void resetReplace(RiggedMeshStruct mesh) {

        vertexReplace.clear();

        for (int v = 0; v < mesh.getVertexCount(); v++)
            vertexReplace.add(v);
    }

    // Join \\

    // The part's plug loop met to the body's rim: welded when both hold as many vertices lying close, bridged
    // otherwise, the strip starting from the pair of vertices that lie nearest
    private void join(RiggedMeshStruct mesh, CharacterPartPlacementStruct placement, int faceBase) {

        int hostCount = hostLoop.size();
        int stampCount = stampLoop.size();

        edgeFaces.clear();

        for (int f = 0; f < faceBase; f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (a != b)
                    edgeFaces.put(CharacterLoopUtility.toDirectedKey(a, b), f);
            }

        if (hostCount != stampCount) {
            bridge(mesh, findNearestStampVertex(mesh, hostLoop.getInt(0)), false);
            return;
        }

        int offset = 0;
        float best = Float.POSITIVE_INFINITY;

        for (int k = 0; k < stampCount; k++) {

            float total = 0f;

            for (int i = 0; i < hostCount && total < best; i++)
                total += CharacterMeshUtility.distanceSquared(
                        mesh, hostLoop.getInt(i), stampLoop.getInt((i + k) % stampCount));

            if (total < best) {
                best = total;
                offset = k;
            }
        }

        float weldReach = placement.getRadius() * EditorSetting.CHARACTER_STAMP_WELD_SHARE;

        if (Math.sqrt(best / hostCount) <= weldReach) {
            weld(mesh, offset, faceBase);
            return;
        }

        bridge(mesh, offset, true);
    }

    // Each rim vertex of the part merged into its partner on the body's rim, at their midpoint, their weights,
    // masks and shape keys blended, the part's faces turned onto the body's vertices, and the rim marked as a seam
    private void weld(RiggedMeshStruct mesh, int offset, int faceBase) {

        int count = hostLoop.size();

        for (int i = 0; i < count; i++) {

            int a = hostLoop.getInt(i);
            int b = stampLoop.getInt((i + offset) % count);

            CharacterMeshUtility.setRoundedPosition(
                    mesh,
                    a,
                    (mesh.getX(a) + mesh.getX(b)) * 0.5f,
                    (mesh.getY(a) + mesh.getY(b)) * 0.5f,
                    (mesh.getZ(a) + mesh.getZ(b)) * 0.5f);

            for (int ch = 0; ch < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; ch++)
                mesh.setMask(a, ch, (mesh.getMask(a, ch) + mesh.getMask(b, ch)) * 0.5f);

            blendMorphs(mesh, a, b);
            weldVertices.clear();
            weldVertices.add(a);
            weldVertices.add(b);
            weldShares.clear();
            weldShares.add(0.5f);
            weldShares.add(0.5f);
            characterWeightBranch.blend(mesh, weldVertices, weldShares, a);
            vertexReplace.set(b, a);
        }

        for (int f = faceBase; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceVertex(f, c, vertexReplace.getInt(mesh.getFaceVertex(f, c)));

        for (int i = 0; i < count; i++)
            mesh.setSeam(hostLoop.getInt(i), hostLoop.getInt((i + 1) % count), true);
    }

    // Every shape key moving either of two welded vertices moves the first by the mean of the two
    private void blendMorphs(RiggedMeshStruct mesh, int a, int b) {

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            int entryA = mesh.findMorphEntry(m, a);
            int entryB = mesh.findMorphEntry(m, b);

            if (entryA == EngineSetting.INDEX_NOT_FOUND && entryB == EngineSetting.INDEX_NOT_FOUND)
                continue;

            mesh.setMorphDelta(
                    m,
                    a,
                    (readDelta(mesh, m, entryA, 0) + readDelta(mesh, m, entryB, 0)) * 0.5f,
                    (readDelta(mesh, m, entryA, 1) + readDelta(mesh, m, entryB, 1)) * 0.5f,
                    (readDelta(mesh, m, entryA, 2) + readDelta(mesh, m, entryB, 2)) * 0.5f);
        }
    }

    private static float readDelta(RiggedMeshStruct mesh, int morph, int entry, int axis) {
        return entry == EngineSetting.INDEX_NOT_FOUND ? 0f : mesh.getMorphDelta(morph, entry, axis);
    }

    // A strip of faces between the two rims, walked along both by the share of each rim's length passed: a quad
    // where both rims step on together, a triangle, held as a quad with one corner doubled, where one steps alone;
    // even rims always step together. The rim of the part is marked as a seam, where its own UV island begins.
    private void bridge(RiggedMeshStruct mesh, int offset, boolean even) {

        int hostCount = hostLoop.size();
        int stampCount = stampLoop.size();

        measureRim(mesh, hostLoop, 0, hostLengths);
        measureRim(mesh, stampLoop, offset, stampLengths);

        float hostTotal = Math.max(hostLengths.getFloat(hostCount), EditorSetting.CHARACTER_VIEW_RAY_EPSILON);
        float stampTotal = Math.max(stampLengths.getFloat(stampCount), EditorSetting.CHARACTER_VIEW_RAY_EPSILON);
        int i = 0;
        int j = 0;

        while (i < hostCount || j < stampCount) {

            int a0 = hostLoop.getInt(i % hostCount);
            int a1 = hostLoop.getInt((i + 1) % hostCount);
            int s0 = stampLoop.getInt((j + offset) % stampCount);
            int s1 = stampLoop.getInt((j + 1 + offset) % stampCount);
            int edge = Math.min(i, hostCount - 1);
            int hostFace = edgeFaces.get(CharacterLoopUtility.toDirectedKey(
                    hostLoop.getInt(edge), hostLoop.getInt((edge + 1) % hostCount)));
            float hostNext = i < hostCount ? hostLengths.getFloat(i + 1) / hostTotal : Float.POSITIVE_INFINITY;
            float stampNext = j < stampCount ? stampLengths.getFloat(j + 1) / stampTotal : Float.POSITIVE_INFINITY;
            float hostStep = i < hostCount ? hostNext - hostLengths.getFloat(i) / hostTotal : 0f;
            float stampStep = j < stampCount ? stampNext - stampLengths.getFloat(j) / stampTotal : 0f;
            boolean together = i < hostCount && j < stampCount
                    && (even || Math.abs(hostNext - stampNext)
                    <= Math.min(hostStep, stampStep) * EditorSetting.CHARACTER_STAMP_PACE_SHARE);

            if (together) {
                addBridgeFace(mesh, hostFace, a1, a0, s0, s1, a1, a0, a0, a1);
                i++;
                j++;
            } else if (j >= stampCount || i < hostCount && hostNext <= stampNext) {
                addBridgeFace(mesh, hostFace, a1, a0, s0, s0, a1, a0, a0, a0);
                i++;
            } else {
                addBridgeFace(mesh, hostFace, a0, a0, s0, s1, a0, a0, a0, a0);
                j++;
            }
        }

        for (int k = 0; k < stampCount; k++)
            mesh.setSeam(stampLoop.getInt(k), stampLoop.getInt((k + 1) % stampCount), true);
    }

    // How far along a rim each of its vertices lies from the one it starts at, the whole length last
    private void measureRim(RiggedMeshStruct mesh, IntArrayList rim, int offset, FloatArrayList lengths) {

        int count = rim.size();
        float total = 0f;

        lengths.clear();
        lengths.add(0f);

        for (int i = 0; i < count; i++) {
            total += (float) Math.sqrt(CharacterMeshUtility.distanceSquared(
                    mesh, rim.getInt((i + offset) % count), rim.getInt((i + 1 + offset) % count)));
            lengths.add(total);
        }
    }

    // A face of the strip textured, zoned and laid out in UV as the body's face beside it is, each corner taking the
    // UV that face gives the body vertex named for it
    private void addBridgeFace(
            RiggedMeshStruct mesh,
            int hostFace,
            int v0,
            int v1,
            int v2,
            int v3,
            int uv0,
            int uv1,
            int uv2,
            int uv3) {

        int face = mesh.addFace(v0, v1, v2, v3, hostFace != EngineSetting.INDEX_NOT_FOUND
                ? mesh.getFaceTexture(hostFace)
                : 0);

        if (hostFace == EngineSetting.INDEX_NOT_FOUND)
            return;

        mesh.setFaceZone(face, mesh.getFaceZone(hostFace));

        if (!mesh.hasAuthoredUVs(hostFace))
            return;

        copyCornerUV(mesh, hostFace, uv0, face, 0);
        copyCornerUV(mesh, hostFace, uv1, face, 1);
        copyCornerUV(mesh, hostFace, uv2, face, 2);
        copyCornerUV(mesh, hostFace, uv3, face, 3);
    }

    private static void copyCornerUV(RiggedMeshStruct mesh, int fromFace, int vertexIndex, int toFace, int corner) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (mesh.getFaceVertex(fromFace, c) == vertexIndex) {
                mesh.setFaceUV(toFace, corner, mesh.getFaceU(fromFace, c), mesh.getFaceV(fromFace, c));
                return;
            }
    }

    // Utility \\

    // The place on the part's rim of the vertex lying nearest one of the body's
    private int findNearestStampVertex(RiggedMeshStruct mesh, int vertexIndex) {

        int nearest = 0;
        float best = Float.POSITIVE_INFINITY;

        for (int k = 0; k < stampLoop.size(); k++) {

            float distance = CharacterMeshUtility.distanceSquared(mesh, vertexIndex, stampLoop.getInt(k));

            if (distance < best) {
                best = distance;
                nearest = k;
            }
        }

        return nearest;
    }
}
