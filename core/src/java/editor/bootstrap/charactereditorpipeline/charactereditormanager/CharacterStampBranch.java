package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.stamppipeline.util.StampCategory;
import editor.bootstrap.stamppipeline.util.StampRigUtility;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterStampBranch extends BranchPackage {

    /*
     * Cuts the faces selected on the open part into a stamp, a body part
     * carrying its own bones, weights, morphs, masks, UVs, seams and layers,
     * the way a modular creature kit's parts are cut from a finished body.
     * Its bones are every bone whose influence it holds the most of, with the
     * bones between them; the bone they all hang from is its host, and a
     * weight on a bone mostly outside the part rests on the nearest of its
     * own bones above it, or on the host. The open loops the cut leaves are found edge by
     * edge: the loop weighing most on the host is the plug the part joins a
     * host by, every other a socket named after the bone it rides, each with
     * its centre, the way it faces out of the part and its radius. A part
     * with no host is a root part, placed alone. The part is laid in its own
     * space, the plug's centre (or a root part's middle) at the origin, its
     * top bones placed and turned there as the rig holds them, and written as
     * a new Stamps file beside the others.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private InfoManager infoManager;
    private MeshManager meshManager;
    private RigManager rigManager;

    // Cut — the faces taken, each source vertex's place in the part, and the source vertices kept in order
    private BooleanArrayList faceFlags;
    private IntArrayList vertexRemap;
    private IntArrayList keptVertices;

    // Bones — each rig bone's weight over the part and over the whole mesh, and its place in the part's rig, the host
    // first
    private float[] boneTotals;
    private float[] meshTotals;
    private IntArrayList boneRemap;
    private IntArrayList stampBones;

    // Loops — the cut's open edges, each vertex's two neighbours along them, and the loops walked
    private Long2IntOpenHashMap partEdges;
    private Long2IntOpenHashMap meshEdges;
    private IntArrayList loopNext;
    private IntArrayList loopOther;
    private ObjectArrayList<IntArrayList> loops;

    // Scratch
    private Vector3 origin;
    private Vector3 centroid;
    private Vector3 loopCenter;
    private Vector3 loopNormal;
    private Vector3 jointScratch;
    private Vector3 eulerScratch;
    private Matrix4 rotationScratch;

    // Base \\

    @Override
    protected void create() {

        // Cut
        this.faceFlags = new BooleanArrayList();
        this.vertexRemap = new IntArrayList();
        this.keptVertices = new IntArrayList();

        // Bones
        this.boneTotals = new float[0];
        this.meshTotals = new float[0];
        this.boneRemap = new IntArrayList();
        this.stampBones = new IntArrayList();

        // Loops
        this.partEdges = new Long2IntOpenHashMap();
        this.meshEdges = new Long2IntOpenHashMap();
        this.loopNext = new IntArrayList();
        this.loopOther = new IntArrayList();
        this.loops = new ObjectArrayList<>();

        // Scratch
        this.origin = new Vector3();
        this.centroid = new Vector3();
        this.loopCenter = new Vector3();
        this.loopNormal = new Vector3();
        this.jointScratch = new Vector3();
        this.eulerScratch = new Vector3();
        this.rotationScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.infoManager = get(InfoManager.class);
        this.meshManager = get(MeshManager.class);
        this.rigManager = get(RigManager.class);
    }

    // Save \\

    // A name no stamp has yet, while the open part has whole faces selected to cut
    public boolean isStampNameAvailable(String stampName) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        return document != null
                && FileUtility.isValidFileName(stampName, EditorSetting.NAME_INPUT_MAX_LENGTH)
                && !infoManager.hasEntry(EditorSetting.INFO_SCHEMA_STAMPS, stampName, stampName)
                && flagSelectedFaces(document) > 0;
    }

    // The selected faces of the open part cut into a new stamp, written beside the others and shown for editing
    public void saveSelection(String stampName) {

        CharacterMeshDocumentInstance document = characterEditorManager.getActiveMeshDocument();

        if (document == null || flagSelectedFaces(document) == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return;
        }

        RigHandle rigHandle = document.getMesh().getRigHandle();
        InfoEntryStruct rigEntry = infoManager.findEntry(
                EditorSetting.INFO_SCHEMA_RIGS, rigHandle.getRigName(), rigHandle.getRigName());

        if (rigEntry == null)
            return;

        ArpgObjectStruct stampArpg = cut(document.getMesh(), faceFlags, rigEntry.getArpg(), stampName);

        infoManager.createDocument(
                EditorSetting.INFO_SCHEMA_STAMPS,
                stampName,
                root -> CharacterViewUtility.replaceObject(root, stampArpg));

        if (infoManager.saveDocument(EditorSetting.INFO_SCHEMA_STAMPS, stampName))
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_STAMP_MESSAGE_SAVED + stampName);
    }

    private int flagSelectedFaces(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int count = 0;

        faceFlags.clear();
        faceFlags.size(mesh.getFaceCount());

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (CharacterMeshUtility.isFaceSelected(mesh, f, document.getSelection())) {
                faceFlags.set(f, true);
                count++;
            }

        return count;
    }

    // Cut \\

    // The flagged faces of a mesh as a stamp's ARPG object, every field its file holds
    ArpgObjectStruct cut(RiggedMeshStruct mesh, BooleanArrayList faces, ArpgObjectStruct rigArpg, String stampName) {

        RigHandle rigHandle = mesh.getRigHandle();

        collectVertices(mesh, faces);
        collectBones(mesh, rigHandle);
        collectLoops(mesh, faces);

        int host = resolveHost(rigHandle);

        resolveStampBones(rigHandle, host);

        int plugLoop = host != EngineSetting.INDEX_NOT_FOUND ? resolvePlugLoop(mesh) : EngineSetting.INDEX_NOT_FOUND;

        resolveOrigin(mesh, plugLoop);

        ArpgArrayStruct bonesArpg = writeBones(rigHandle, rigArpg, host);
        RigHandle stampRig = rigManager.buildDetachedRig(
                EditorSetting.STAMP_RIG_PREFIX + stampName, StampRigUtility.toRigArpg(bonesArpg, stampName));
        RiggedMeshStruct stampMesh = copyMesh(mesh, faces, stampRig);
        ArpgObjectStruct stampArpg = new ArpgObjectStruct();

        stampArpg.addProperty(EditorSetting.STAMP_FIELD_DISPLAY_NAME, stampName);
        stampArpg.addProperty(EditorSetting.STAMP_FIELD_CATEGORY, ArpgUtility.toEnumName(resolveCategory(rigHandle)));

        if (plugLoop != EngineSetting.INDEX_NOT_FOUND)
            stampArpg.add(EditorSetting.STAMP_FIELD_PLUG, writeLoop(mesh, plugLoop, null, null));

        stampArpg.add(EditorSetting.STAMP_FIELD_SOCKETS, writeSockets(mesh, rigHandle, plugLoop));
        stampArpg.add(EditorSetting.STAMP_FIELD_BONES, bonesArpg);
        stampArpg.add(EditorSetting.STAMP_FIELD_PARTS, writeParts(rigArpg, rigHandle));

        return meshManager.toRiggedMeshArpg(stampMesh, stampArpg);
    }

    // Every vertex the flagged faces stand on, numbered in the order they first appear
    private void collectVertices(RiggedMeshStruct mesh, BooleanArrayList faces) {

        vertexRemap.clear();
        vertexRemap.size(mesh.getVertexCount());
        keptVertices.clear();

        for (int v = 0; v < mesh.getVertexCount(); v++)
            vertexRemap.set(v, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faces.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int vertexIndex = mesh.getFaceVertex(f, c);

                    if (vertexRemap.getInt(vertexIndex) != EngineSetting.INDEX_NOT_FOUND)
                        continue;

                    vertexRemap.set(vertexIndex, keptVertices.size());
                    keptVertices.add(vertexIndex);
                }
    }

    // Every bone's weight over the part's vertices and over the whole mesh's
    private void collectBones(RiggedMeshStruct mesh, RigHandle rigHandle) {

        if (boneTotals.length < rigHandle.getBoneCount()) {
            this.boneTotals = new float[rigHandle.getBoneCount()];
            this.meshTotals = new float[rigHandle.getBoneCount()];
        }

        for (int b = 0; b < boneTotals.length; b++) {
            boneTotals[b] = 0f;
            meshTotals[b] = 0f;
        }

        for (int v = 0; v < mesh.getVertexCount(); v++)
            for (int n = 0; n < mesh.getInfluenceCount(v); n++) {

                int bone = mesh.getInfluenceBone(v, n);
                float weight = mesh.getInfluenceWeight(v, n);

                meshTotals[bone] += weight;

                if (vertexRemap.getInt(v) != EngineSetting.INDEX_NOT_FOUND)
                    boneTotals[bone] += weight;
            }
    }

    // The bone every one of the part's bones hangs from: the host its top bone joins when that bone is the part's
    // own, or the bone itself when it is not, as when two limbs are cut together; INDEX_NOT_FOUND for a root part,
    // whose top bone has no parent
    private int resolveHost(RigHandle rigHandle) {

        int common = EngineSetting.INDEX_NOT_FOUND;

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            if (isCarried(b))
                common = common == EngineSetting.INDEX_NOT_FOUND ? b : findCommonAncestor(rigHandle, common, b);

        if (common == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        return isCarried(common) ? rigHandle.getBone(common).getParentIndex() : common;
    }

    // Whether the part holds the bone share of a bone's whole influence, so the bone is the part's own
    private boolean isCarried(int boneIndex) {
        return boneTotals[boneIndex] > 0f
                && boneTotals[boneIndex] >= meshTotals[boneIndex] * EditorSetting.CHARACTER_STAMP_BONE_SHARE;
    }

    private int findCommonAncestor(RigHandle rigHandle, int a, int b) {

        for (int x = a; x != EngineSetting.INDEX_NOT_FOUND; x = rigHandle.getBone(x).getParentIndex())
            for (int y = b; y != EngineSetting.INDEX_NOT_FOUND; y = rigHandle.getBone(y).getParentIndex())
                if (x == y)
                    return x;

        return 0;
    }

    // Every bone below the host that is the part's own and every bone between it and the host, in rig order, each
    // numbered in the part's rig after the host placeholder; a bone mostly outside the part hands its weight to its
    // nearest ancestor among them, and the host and every bone above it rest on the placeholder
    private void resolveStampBones(RigHandle rigHandle, int host) {

        stampBones.clear();
        boneRemap.clear();
        boneRemap.size(rigHandle.getBoneCount());

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            boneRemap.set(b, 0);

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            if (b != host && isCarried(b) && isBelow(rigHandle, b, host))
                for (int x = b; x != host && x != EngineSetting.INDEX_NOT_FOUND;
                        x = rigHandle.getBone(x).getParentIndex())
                    boneRemap.set(x, 1);

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            if (boneRemap.getInt(b) == 1) {
                stampBones.add(b);
                boneRemap.set(b, stampBones.size());
            }

        for (int b = 0; b < rigHandle.getBoneCount(); b++)
            if (boneRemap.getInt(b) == 0)
                boneRemap.set(b, resolveCarrier(rigHandle, b, host));
    }

    // Whether a bone hangs below the host, every bone hanging below none for a root part
    private boolean isBelow(RigHandle rigHandle, int boneIndex, int host) {

        if (host == EngineSetting.INDEX_NOT_FOUND)
            return true;

        for (int x = rigHandle.getBone(boneIndex).getParentIndex(); x != EngineSetting.INDEX_NOT_FOUND;
                x = rigHandle.getBone(x).getParentIndex())
            if (x == host)
                return true;

        return false;
    }

    // The part's bone a bone outside it hands its weight to: its nearest ancestor among the part's bones, the host
    // placeholder for one reaching the host or the top first
    private int resolveCarrier(RigHandle rigHandle, int boneIndex, int host) {

        for (int x = boneIndex; x != host && x != EngineSetting.INDEX_NOT_FOUND;
                x = rigHandle.getBone(x).getParentIndex())
            if (boneRemap.getInt(x) > 0 && stampBones.contains(x))
                return boneRemap.getInt(x);

        return 0;
    }

    // Loops \\

    // The open loops the cut leaves: edges the part's faces use once where the mesh's faces use them twice, walked
    // vertex to vertex
    private void collectLoops(RiggedMeshStruct mesh, BooleanArrayList faces) {

        partEdges.clear();
        meshEdges.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                long edgeKey = RiggedMeshStruct.toEdgeKey(
                        mesh.getFaceVertex(f, c),
                        mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT));

                meshEdges.addTo(edgeKey, 1);

                if (faces.getBoolean(f))
                    partEdges.addTo(edgeKey, 1);
            }

        loopNext.clear();
        loopNext.size(mesh.getVertexCount());
        loopOther.clear();
        loopOther.size(mesh.getVertexCount());

        for (int v = 0; v < mesh.getVertexCount(); v++) {
            loopNext.set(v, EngineSetting.INDEX_NOT_FOUND);
            loopOther.set(v, EngineSetting.INDEX_NOT_FOUND);
        }

        for (Long2IntOpenHashMap.Entry entry : partEdges.long2IntEntrySet())
            if (entry.getIntValue() == 1 && meshEdges.get(entry.getLongKey()) == 2)
                linkLoopEdge(
                        RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 0),
                        RiggedMeshStruct.getSeamVertex(entry.getLongKey(), 1));

        loops.clear();

        for (int v = 0; v < mesh.getVertexCount(); v++)
            if (loopNext.getInt(v) != EngineSetting.INDEX_NOT_FOUND)
                walkLoop(v);
    }

    private void linkLoopEdge(int a, int b) {
        linkLoopVertex(a, b);
        linkLoopVertex(b, a);
    }

    private void linkLoopVertex(int vertexIndex, int neighbour) {

        if (loopNext.getInt(vertexIndex) == EngineSetting.INDEX_NOT_FOUND)
            loopNext.set(vertexIndex, neighbour);
        else
            loopOther.set(vertexIndex, neighbour);
    }

    // One loop walked from a vertex until it closes or ends, each vertex taken off the open edges as it is passed
    private void walkLoop(int start) {

        IntArrayList loop = new IntArrayList();
        int previous = EngineSetting.INDEX_NOT_FOUND;
        int current = start;

        while (current != EngineSetting.INDEX_NOT_FOUND && loopNext.getInt(current) != EngineSetting.INDEX_NOT_FOUND) {

            loop.add(current);

            int next = loopNext.getInt(current) != previous ? loopNext.getInt(current) : loopOther.getInt(current);

            loopNext.set(current, EngineSetting.INDEX_NOT_FOUND);
            loopOther.set(current, EngineSetting.INDEX_NOT_FOUND);
            previous = current;
            current = next;
        }

        if (loop.size() >= EngineSetting.QUAD_VERTEX_COUNT - 1)
            loops.add(loop);
    }

    // The loop that weighs most on the host, or none where no loop touches it
    private int resolvePlugLoop(RiggedMeshStruct mesh) {

        int plug = EngineSetting.INDEX_NOT_FOUND;
        float heaviest = 0f;

        for (int l = 0; l < loops.size(); l++) {

            float hostWeight = 0f;
            IntArrayList loop = loops.get(l);

            for (int i = 0; i < loop.size(); i++) {

                int vertexIndex = loop.getInt(i);

                for (int n = 0; n < mesh.getInfluenceCount(vertexIndex); n++)
                    if (boneRemap.getInt(mesh.getInfluenceBone(vertexIndex, n)) == 0)
                        hostWeight += mesh.getInfluenceWeight(vertexIndex, n);
            }

            if (hostWeight / loop.size() > heaviest) {
                heaviest = hostWeight / loop.size();
                plug = l;
            }
        }

        return plug;
    }

    // The part's own origin: its plug's centre, or for a part joining no host by a loop the middle of its bounds
    private void resolveOrigin(RiggedMeshStruct mesh, int plugLoop) {

        resolveCentroid(mesh);

        if (plugLoop != EngineSetting.INDEX_NOT_FOUND) {
            resolveLoop(mesh, loops.get(plugLoop));
            origin.set(loopCenter);
            return;
        }

        origin.set(centroid);
    }

    private void resolveCentroid(RiggedMeshStruct mesh) {

        centroid.set(0f, 0f, 0f);

        for (int i = 0; i < keptVertices.size(); i++) {

            int vertexIndex = keptVertices.getInt(i);

            centroid.add(mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex));
        }

        if (!keptVertices.isEmpty())
            centroid.multiply(1f / keptVertices.size());
    }

    // A loop's centre, the way it faces out of the part by Newell's method, its mean radius returned
    private float resolveLoop(RiggedMeshStruct mesh, IntArrayList loop) {

        loopCenter.set(0f, 0f, 0f);
        loopNormal.set(0f, 0f, 0f);

        for (int i = 0; i < loop.size(); i++) {

            int a = loop.getInt(i);
            int b = loop.getInt((i + 1) % loop.size());

            loopCenter.add(mesh.getX(a), mesh.getY(a), mesh.getZ(a));
            loopNormal.add(
                    (mesh.getY(a) - mesh.getY(b)) * (mesh.getZ(a) + mesh.getZ(b)),
                    (mesh.getZ(a) - mesh.getZ(b)) * (mesh.getX(a) + mesh.getX(b)),
                    (mesh.getX(a) - mesh.getX(b)) * (mesh.getY(a) + mesh.getY(b)));
        }

        loopCenter.multiply(1f / loop.size());

        float outward = (loopCenter.x - centroid.x) * loopNormal.x
                + (loopCenter.y - centroid.y) * loopNormal.y
                + (loopCenter.z - centroid.z) * loopNormal.z;

        if (loopNormal.length() <= 0f)
            loopNormal.set(loopCenter).subtract(centroid);

        if (outward < 0f)
            loopNormal.multiply(-1f);

        loopNormal.normalize();

        float radius = 0f;

        for (int i = 0; i < loop.size(); i++) {

            int vertexIndex = loop.getInt(i);

            jointScratch.set(mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex))
                    .subtract(loopCenter);
            radius += jointScratch.length();
        }

        return radius / loop.size();
    }

    // A loop as a plug or socket entry, its centre in the part's space; a socket names itself and its bone
    private ArpgObjectStruct writeLoop(RiggedMeshStruct mesh, int loopIndex, String socketName, String boneName) {

        float radius = resolveLoop(mesh, loops.get(loopIndex));
        ArpgObjectStruct loopArpg = new ArpgObjectStruct();

        if (socketName != null) {
            loopArpg.addProperty(EditorSetting.STAMP_FIELD_NAME, socketName);
            loopArpg.addProperty(EditorSetting.STAMP_FIELD_BONE, boneName);
        }

        loopArpg.add(EditorSetting.STAMP_FIELD_POSITION, toVector(
                loopCenter.x - origin.x, loopCenter.y - origin.y, loopCenter.z - origin.z));
        loopArpg.add(EditorSetting.STAMP_FIELD_NORMAL, toVector(loopNormal.x, loopNormal.y, loopNormal.z));
        loopArpg.addProperty(
                EditorSetting.STAMP_FIELD_RADIUS,
                CharacterViewUtility.round(Math.max(radius, EditorSetting.CHARACTER_STAMP_RADIUS_MIN)));

        return loopArpg;
    }

    // Every loop but the plug as a socket on the part's bone weighing most over it, named after the bone the cut took
    // away beyond it
    private ArpgArrayStruct writeSockets(RiggedMeshStruct mesh, RigHandle rigHandle, int plugLoop) {

        ArpgArrayStruct socketsArpg = new ArpgArrayStruct();
        ObjectArrayList<String> names = new ObjectArrayList<>();

        for (int l = 0; l < loops.size(); l++) {

            if (l == plugLoop)
                continue;

            int bone = resolveLoopBone(mesh, loops.get(l));

            if (bone == EngineSetting.INDEX_NOT_FOUND)
                continue;

            String boneName = rigHandle.getBone(bone).getName();
            String baseName = rigHandle.getBone(resolveCutBone(mesh, loops.get(l), bone)).getName();
            String socketName = baseName;

            for (int n = 1; names.contains(socketName); n++)
                socketName = baseName + EditorSetting.CHARACTER_STAMP_SOCKET_SEPARATOR + n;

            names.add(socketName);
            socketsArpg.add(writeLoop(mesh, l, socketName, boneName));
        }

        return socketsArpg;
    }

    // The bone outside the part carrying most of a loop's weight, the bone the cut took away beyond it, or the part's
    // own bone given where none outside reaches the loop
    private int resolveCutBone(RiggedMeshStruct mesh, IntArrayList loop, int partBone) {

        int heaviest = partBone;
        float most = 0f;

        for (int b = 0; b < boneTotals.length && b < mesh.getRigHandle().getBoneCount(); b++) {

            if (stampBones.contains(b))
                continue;

            float weight = 0f;

            for (int i = 0; i < loop.size(); i++)
                weight += mesh.getBoneWeight(loop.getInt(i), b);

            if (weight > most) {
                most = weight;
                heaviest = b;
            }
        }

        return heaviest;
    }

    // The part's bone carrying most of a loop's weight, INDEX_NOT_FOUND where the loop rests on the host alone
    private int resolveLoopBone(RiggedMeshStruct mesh, IntArrayList loop) {

        int heaviest = EngineSetting.INDEX_NOT_FOUND;
        float most = 0f;

        for (int s = 0; s < stampBones.size(); s++) {

            int bone = stampBones.getInt(s);
            float weight = 0f;

            for (int i = 0; i < loop.size(); i++)
                weight += mesh.getBoneWeight(loop.getInt(i), bone);

            if (weight > most) {
                most = weight;
                heaviest = bone;
            }
        }

        return heaviest;
    }

    // Bones \\

    // Each of the part's bones as its rig entry holds it; a top bone hangs from the host, placed and turned in the
    // part's space as the rig's bind pose holds it
    private ArpgArrayStruct writeBones(RigHandle rigHandle, ArpgObjectStruct rigArpg, int host) {

        ArpgArrayStruct rigBones = ArpgUtility.validateArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_BONES);
        ArpgArrayStruct bonesArpg = new ArpgArrayStruct();

        for (int s = 0; s < stampBones.size(); s++) {

            int bone = stampBones.getInt(s);
            String boneName = rigHandle.getBone(bone).getName();
            ArpgObjectStruct boneArpg = findBoneArpg(rigBones, boneName).deepCopy();
            int parent = rigHandle.getBone(bone).getParentIndex();

            if (parent == host || parent == EngineSetting.INDEX_NOT_FOUND) {

                Matrix4 world = rigHandle.getBindWorldMatrix(bone);

                CharacterPoseUtility.getTranslation(world, jointScratch);
                CharacterPoseUtility.extractRotation(world, rotationScratch);
                CharacterPoseUtility.toEuler(rotationScratch, eulerScratch.set(0f, 0f, 0f), eulerScratch);

                boneArpg.addProperty(EditorSetting.CHARACTER_RIG_FIELD_PARENT, "");
                boneArpg.add(EditorSetting.CHARACTER_EDITOR_FIELD_POSITION, toVector(
                        jointScratch.x - origin.x, jointScratch.y - origin.y, jointScratch.z - origin.z));
                boneArpg.add(EditorSetting.CHARACTER_EDITOR_FIELD_ROTATION, toVector(
                        eulerScratch.x, eulerScratch.y, eulerScratch.z));
            }

            bonesArpg.add(boneArpg);
        }

        return bonesArpg;
    }

    private ArpgObjectStruct findBoneArpg(ArpgArrayStruct rigBones, String boneName) {

        for (int i = 0; i < rigBones.size(); i++) {

            ArpgObjectStruct boneArpg = rigBones.get(i).getAsObject();

            if (boneName.equals(ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, "")))
                return boneArpg;
        }

        return throwException("Rig entry holds no bone named \"" + boneName + "\"");
    }

    // Every rig layer whose bones the part holds all of
    private ArpgArrayStruct writeParts(ArpgObjectStruct rigArpg, RigHandle rigHandle) {

        ArpgArrayStruct partsArpg = new ArpgArrayStruct();

        if (!ArpgUtility.hasArray(rigArpg, EditorSetting.CHARACTER_RIG_FIELD_PARTS))
            return partsArpg;

        ArpgArrayStruct rigParts = rigArpg.getAsArray(EditorSetting.CHARACTER_RIG_FIELD_PARTS);

        for (int p = 0; p < rigParts.size(); p++) {

            ArpgObjectStruct partArpg = rigParts.get(p).getAsObject();
            ArpgArrayStruct partBones = ArpgUtility.validateArray(partArpg, EditorSetting.CHARACTER_RIG_FIELD_BONES);
            boolean held = partBones.size() > 0;

            for (int b = 0; b < partBones.size() && held; b++) {

                String boneName = partBones.get(b).getAsString();

                held = rigHandle.hasBone(boneName) && stampBones.contains(rigHandle.getBoneIndex(boneName));
            }

            if (held)
                partsArpg.add(partArpg.deepCopy());
        }

        return partsArpg;
    }

    // The first kind whose name the part's top bone carries, the part's kind otherwise left for its author
    private StampCategory resolveCategory(RigHandle rigHandle) {

        if (stampBones.isEmpty())
            return StampCategory.OTHER;

        String topName = rigHandle.getBone(stampBones.getInt(0)).getName();

        for (StampCategory category : StampCategory.VALUES)
            if (topName.contains(ArpgUtility.toEnumName(category)))
                return category;

        return StampCategory.OTHER;
    }

    // Mesh \\

    // The flagged faces with every vertex they stand on, in the part's space and on its rig: positions, weights,
    // masks, shape keys, textures, zones, UVs, pins and seams
    private RiggedMeshStruct copyMesh(RiggedMeshStruct mesh, BooleanArrayList faces, RigHandle stampRig) {

        RiggedMeshStruct stampMesh = new RiggedMeshStruct(stampRig);

        for (int i = 0; i < keptVertices.size(); i++)
            copyVertex(mesh, keptVertices.getInt(i), stampMesh);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faces.getBoolean(f))
                copyFace(mesh, f, stampMesh);

        copySeams(mesh, stampMesh);
        copyMorphs(mesh, stampMesh);

        return stampMesh;
    }

    private void copyVertex(RiggedMeshStruct mesh, int vertexIndex, RiggedMeshStruct stampMesh) {

        int copy = stampMesh.addVertex(
                mesh.getX(vertexIndex) - origin.x,
                mesh.getY(vertexIndex) - origin.y,
                mesh.getZ(vertexIndex) - origin.z);

        for (int n = 0; n < mesh.getInfluenceCount(vertexIndex); n++) {

            int bone = boneRemap.getInt(mesh.getInfluenceBone(vertexIndex, n));
            int existing = stampMesh.findInfluence(copy, bone);
            float weight = mesh.getInfluenceWeight(vertexIndex, n);

            if (existing == EngineSetting.INDEX_NOT_FOUND)
                stampMesh.addInfluence(copy, bone, weight);
            else
                stampMesh.setInfluenceWeight(copy, existing, stampMesh.getInfluenceWeight(copy, existing) + weight);
        }

        for (int ch = 0; ch < EngineSetting.RIGGED_MESH_MASK_CHANNELS.length; ch++)
            stampMesh.setMask(copy, ch, mesh.getMask(vertexIndex, ch));
    }

    private void copyFace(RiggedMeshStruct mesh, int faceIndex, RiggedMeshStruct stampMesh) {

        int copy = stampMesh.addFace(
                vertexRemap.getInt(mesh.getFaceVertex(faceIndex, 0)),
                vertexRemap.getInt(mesh.getFaceVertex(faceIndex, 1)),
                vertexRemap.getInt(mesh.getFaceVertex(faceIndex, 2)),
                vertexRemap.getInt(mesh.getFaceVertex(faceIndex, 3)),
                stampMesh.resolveTexture(mesh.getTextureName(mesh.getFaceTexture(faceIndex))));

        stampMesh.setFaceZone(copy, mesh.getFaceZone(faceIndex));

        if (mesh.hasAuthoredUVs(faceIndex))
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                stampMesh.setFaceUV(copy, c, mesh.getFaceU(faceIndex, c), mesh.getFaceV(faceIndex, c));

        stampMesh.setFacePins(copy, mesh.getFacePins(faceIndex));
    }

    private void copySeams(RiggedMeshStruct mesh, RiggedMeshStruct stampMesh) {

        long[] seams = mesh.getSortedSeams();

        for (int i = 0; i < seams.length; i++) {

            int a = vertexRemap.getInt(RiggedMeshStruct.getSeamVertex(seams[i], 0));
            int b = vertexRemap.getInt(RiggedMeshStruct.getSeamVertex(seams[i], 1));

            if (a != EngineSetting.INDEX_NOT_FOUND && b != EngineSetting.INDEX_NOT_FOUND)
                stampMesh.setSeam(a, b, true);
        }
    }

    // Every shape key carrying a delta on a vertex the part keeps, by its name
    private void copyMorphs(RiggedMeshStruct mesh, RiggedMeshStruct stampMesh) {

        for (int m = 0; m < mesh.getMorphCount(); m++)
            for (int e = 0; e < mesh.getMorphEntryCount(m); e++) {

                int copy = vertexRemap.getInt(mesh.getMorphVertex(m, e));

                if (copy == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                stampMesh.setMorphDelta(
                        stampMesh.resolveMorph(mesh.getMorphName(m)),
                        copy,
                        mesh.getMorphDelta(m, e, 0),
                        mesh.getMorphDelta(m, e, 1),
                        mesh.getMorphDelta(m, e, 2));
            }
    }

    // Utility \\

    // A vector as rig entries hold one, an object of its axes, each rounded to the editor's precision
    private ArpgObjectStruct toVector(float x, float y, float z) {

        ArpgObjectStruct vector = new ArpgObjectStruct();

        vector.addProperty(EditorSetting.CHARACTER_VIEW_AXIS_NAMES[0], CharacterViewUtility.round(x));
        vector.addProperty(EditorSetting.CHARACTER_VIEW_AXIS_NAMES[1], CharacterViewUtility.round(y));
        vector.addProperty(EditorSetting.CHARACTER_VIEW_AXIS_NAMES[2], CharacterViewUtility.round(z));

        return vector;
    }
}
