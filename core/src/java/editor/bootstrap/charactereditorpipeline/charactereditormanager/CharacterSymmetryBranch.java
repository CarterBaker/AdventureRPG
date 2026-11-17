package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterSymmetryBranch extends BranchPackage {

    /*
     * Symmetrize, as Blender's and Maya's do: the mesh is cut along the centre plane, the side given up is removed
     * and the side kept is mirrored across x and welded on the plane, so every vertex has an exact twin and every
     * face a mirrored twin, whatever the two sides held before. A face crossing the plane along two edges is cut
     * where they cross it; one crossing it any other way first has its corners on the side it holds fewer of laid on
     * the plane. Twins take their side's weights with each sided bone swapped for its twin (by name, or by its
     * mirrored joint for an unsided chain such as hair), so the rig and every clip stay as they are; sided shape keys
     * swap with their twins and the rest mirror; masks, seams, pins, textures and zones carry over; vertices on the
     * plane take the even blend of their own weights and deltas and their mirror's. UV islands stay whole: one
     * crossing the plane is made symmetric about its own centre line, and one on the kept side is mirrored into the
     * place its twin held on the side removed (the layout that fits it best, separate or shared), so painted maps
     * keep their layout. One undo step.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterSculptBranch characterSculptBranch;
    private CharacterTopologyBranch characterTopologyBranch;
    private CharacterWeightBranch characterWeightBranch;
    private CharacterUVBranch characterUVBranch;

    // Sides
    private float sign;
    private int originalVertexCount;
    private int twinStart;
    private IntArrayList originalTwins;
    private IntArrayList twins;
    private int[] twinBones;
    private BooleanArrayList unmatchedBones;
    private int unmatchedCount;

    // Cut
    private Long2IntOpenHashMap edge2Cut;
    private int cutCount;
    private int laidCount;

    // Faces
    private BooleanArrayList faceFlags;
    private IntArrayList faceIslands;
    private IntArrayList faceTwins;
    private IntArrayList faceScratch;

    // Islands
    private BooleanArrayList islandKept;
    private BooleanArrayList islandRemoved;
    private int overlapCount;
    private float axisNU;
    private float axisNV;
    private float axisOffset;
    private float axisGlide;
    private float shiftU;
    private float shiftV;

    // Removed Corners — every authored corner on the side removed, chained per vertex: its face, island, texture and UV
    private IntArrayList vertex2FirstCorner;
    private IntArrayList cornerNext;
    private IntArrayList cornerFaces;
    private IntArrayList cornerIslands;
    private IntArrayList cornerTextures;
    private FloatArrayList cornerUVs;

    // Fit — each kept corner's UV beside its old twin's UV in the island its twin answers in
    private Int2IntOpenHashMap island2Votes;
    private FloatArrayList pairUVs;
    private FloatArrayList fitV;

    // Morphs
    private Int2IntOpenHashMap vertex2EntryA;
    private Int2IntOpenHashMap vertex2EntryB;
    private IntArrayList centreVertices;
    private IntOpenHashSet centreSet;

    // Base \\

    @Override
    protected void create() {

        // Sides
        this.originalTwins = new IntArrayList();
        this.twins = new IntArrayList();
        this.twinBones = new int[0];
        this.unmatchedBones = new BooleanArrayList();

        // Cut
        this.edge2Cut = new Long2IntOpenHashMap();
        this.edge2Cut.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        // Faces
        this.faceFlags = new BooleanArrayList();
        this.faceIslands = new IntArrayList();
        this.faceTwins = new IntArrayList();
        this.faceScratch = new IntArrayList();

        // Islands
        this.islandKept = new BooleanArrayList();
        this.islandRemoved = new BooleanArrayList();

        // Removed Corners
        this.vertex2FirstCorner = new IntArrayList();
        this.cornerNext = new IntArrayList();
        this.cornerFaces = new IntArrayList();
        this.cornerIslands = new IntArrayList();
        this.cornerTextures = new IntArrayList();
        this.cornerUVs = new FloatArrayList();

        // Fit
        this.island2Votes = new Int2IntOpenHashMap();
        this.pairUVs = new FloatArrayList();
        this.fitV = new FloatArrayList();

        // Morphs
        this.vertex2EntryA = new Int2IntOpenHashMap();
        this.vertex2EntryA.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.vertex2EntryB = new Int2IntOpenHashMap();
        this.vertex2EntryB.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.centreVertices = new IntArrayList();
        this.centreSet = new IntOpenHashSet();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterSculptBranch = get(CharacterSculptBranch.class);
        this.characterTopologyBranch = get(CharacterTopologyBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
        this.characterUVBranch = get(CharacterUVBranch.class);
    }

    // Symmetrize \\

    // The side of x named mirrored onto the other as one edit, refused while a shape key is sculpted
    void symmetrize(CharacterMeshDocumentInstance document, boolean positiveToNegative) {

        if (document.isEditingShape()) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_TOPOLOGY_SHAPE);
            return;
        }

        RiggedMeshStruct mesh = document.getMesh();

        this.sign = positiveToNegative ? 1f : -1f;
        this.originalVertexCount = mesh.getVertexCount();
        this.cutCount = 0;
        this.laidCount = 0;
        this.overlapCount = 0;
        this.unmatchedCount = 0;

        originalTwins.clear();
        originalTwins.addAll(characterSelectionBranch.resolveMirror(document));
        characterHistoryBranch.begin(document);

        layOnCentre(mesh);
        resolveIslands(mesh);
        recordRemovedCorners(mesh);
        cutAcross(mesh);
        removeSide(mesh);

        int faceCount = mesh.getFaceCount();

        addTwinVertices(mesh);
        mirrorMorphs(mesh);
        addTwinFaces(mesh, faceCount);
        mirrorSeams(mesh);
        mirrorUVs(mesh, faceCount);
        characterSculptBranch.removeOrphans(document);

        if (characterMeshBranch.finishEdit(document))
            characterEditorManager.setStatusMessage(describe(document));
    }

    // Sides \\

    private int classify(RiggedMeshStruct mesh, int vertexIndex) {

        float x = mesh.getX(vertexIndex) * sign;

        if (x > EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE)
            return EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT;

        if (x < -EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE)
            return EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED;

        return EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE;
    }

    // Every vertex within the centre tolerance laid exactly on the plane
    private void layOnCentre(RiggedMeshStruct mesh) {
        for (int i = 0; i < mesh.getVertexCount(); i++)
            if (classify(mesh, i) == EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE && mesh.getX(i) != 0f)
                mesh.setPosition(i, 0f, mesh.getY(i), mesh.getZ(i));
    }

    private int countSide(RiggedMeshStruct mesh, int faceIndex, int side) {

        int count = 0;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (classify(mesh, mesh.getFaceVertex(faceIndex, c)) == side)
                count++;

        return count;
    }

    // Islands \\

    // Each face's UV island before the cut, and whether each island holds faces on the side kept and the side removed
    private void resolveIslands(RiggedMeshStruct mesh) {

        int faceCount = mesh.getFaceCount();
        int[] islands = new int[faceCount];

        fillFaces(faceCount);

        int islandCount = characterUVBranch.resolveIslands(mesh, faceScratch, true, islands);

        faceIslands.clear();
        faceIslands.addElements(0, islands);
        islandKept.clear();
        islandKept.size(islandCount);
        islandRemoved.clear();
        islandRemoved.size(islandCount);

        for (int f = 0; f < faceCount; f++) {

            int island = islands[f];

            if (countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT) > 0)
                islandKept.set(island, true);

            if (countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED) > 0)
                islandRemoved.set(island, true);
        }
    }

    private void fillFaces(int faceCount) {

        faceScratch.clear();

        for (int f = 0; f < faceCount; f++)
            faceScratch.add(f);
    }

    // Every authored corner on the side removed, with its face and the island, texture and UV that face gives it
    private void recordRemovedCorners(RiggedMeshStruct mesh) {

        vertex2FirstCorner.clear();
        vertex2FirstCorner.size(originalVertexCount);
        cornerNext.clear();
        cornerFaces.clear();
        cornerIslands.clear();
        cornerTextures.clear();
        cornerUVs.clear();

        for (int i = 0; i < originalVertexCount; i++)
            vertex2FirstCorner.set(i, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!mesh.hasAuthoredUVs(f))
                continue;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertex = mesh.getFaceVertex(f, c);

                if (classify(mesh, vertex) != EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED)
                    continue;

                cornerNext.add(vertex2FirstCorner.getInt(vertex));
                vertex2FirstCorner.set(vertex, cornerIslands.size());
                cornerFaces.add(f);
                cornerIslands.add(faceIslands.getInt(f));
                cornerTextures.add(mesh.getFaceTexture(f));
                cornerUVs.add(mesh.getFaceU(f, c));
                cornerUVs.add(mesh.getFaceV(f, c));
            }
        }
    }

    // Cut \\

    // Every face holding corners on both sides cut or laid until none does; each pass leaves fewer corners off the
    // plane in crossing faces, so it ends
    private void cutAcross(RiggedMeshStruct mesh) {

        edge2Cut.clear();

        boolean crossing = true;

        while (crossing) {

            crossing = false;

            for (int f = 0; f < mesh.getFaceCount(); f++) {

                int kept = countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT);
                int removed = countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED);

                if (kept == 0 || removed == 0)
                    continue;

                int lead = findCutLead(mesh, f);

                if (lead != EngineSetting.INDEX_NOT_FOUND)
                    cutFace(mesh, f, lead);
                else
                    layMinority(mesh, f, kept, removed);

                crossing = true;
            }
        }
    }

    // The corner leading two kept corners that are followed by two removed ones, so the face crosses along two edges
    private int findCutLead(RiggedMeshStruct mesh, int faceIndex) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (sideAt(mesh, faceIndex, c) == EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT
                    && sideAt(mesh, faceIndex, c + 1) == EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT
                    && sideAt(mesh, faceIndex, c + 2) == EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED
                    && sideAt(mesh, faceIndex, c + 3) == EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED)
                return c;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private int sideAt(RiggedMeshStruct mesh, int faceIndex, int corner) {
        return classify(mesh, mesh.getFaceVertex(faceIndex, corner % EngineSetting.QUAD_VERTEX_COUNT));
    }

    // The face's kept half: its two removed corners replaced by the points where its crossing edges meet the plane
    private void cutFace(RiggedMeshStruct mesh, int faceIndex, int lead) {

        int c1 = (lead + 1) % EngineSetting.QUAD_VERTEX_COUNT;
        int c2 = (lead + 2) % EngineSetting.QUAD_VERTEX_COUNT;
        int c3 = (lead + 3) % EngineSetting.QUAD_VERTEX_COUNT;
        int v0 = mesh.getFaceVertex(faceIndex, lead);
        int v1 = mesh.getFaceVertex(faceIndex, c1);
        int v2 = mesh.getFaceVertex(faceIndex, c2);
        int v3 = mesh.getFaceVertex(faceIndex, c3);
        float share1 = resolveCrossShare(mesh, v1, v2);
        float share0 = resolveCrossShare(mesh, v0, v3);
        boolean seam1 = mesh.isSeam(v1, v2);
        boolean seam0 = mesh.isSeam(v0, v3);
        int cut1 = resolveCut(mesh, v1, v2, share1);
        int cut0 = resolveCut(mesh, v0, v3, share0);

        if (mesh.hasAuthoredUVs(faceIndex)) {

            float u2 = lerp(mesh.getFaceU(faceIndex, c1), mesh.getFaceU(faceIndex, c2), share1);
            float uv2 = lerp(mesh.getFaceV(faceIndex, c1), mesh.getFaceV(faceIndex, c2), share1);
            float u3 = lerp(mesh.getFaceU(faceIndex, lead), mesh.getFaceU(faceIndex, c3), share0);
            float uv3 = lerp(mesh.getFaceV(faceIndex, lead), mesh.getFaceV(faceIndex, c3), share0);

            mesh.setFaceUV(faceIndex, c2, u2, uv2);
            mesh.setFaceUV(faceIndex, c3, u3, uv3);
        }

        if (seam1)
            mesh.setSeam(v1, cut1, true);

        if (seam0)
            mesh.setSeam(v0, cut0, true);

        mesh.setFaceVertex(faceIndex, c2, cut1);
        mesh.setFaceVertex(faceIndex, c3, cut0);
        mesh.setCornerPinned(faceIndex, c2, false);
        mesh.setCornerPinned(faceIndex, c3, false);
    }

    // How far along an edge from its kept end to its removed end the plane lies
    private float resolveCrossShare(RiggedMeshStruct mesh, int kept, int removed) {

        float from = mesh.getX(kept);

        return from / (from - mesh.getX(removed));
    }

    // The vertex where an edge meets the plane, made once and shared by both faces beside the edge
    private int resolveCut(RiggedMeshStruct mesh, int kept, int removed, float share) {

        long edgeKey = RiggedMeshStruct.toEdgeKey(kept, removed);
        int cut = edge2Cut.get(edgeKey);

        if (cut != EngineSetting.INDEX_NOT_FOUND)
            return cut;

        cut = characterTopologyBranch.addBetween(mesh, kept, removed, share);
        mesh.setPosition(cut, 0f, mesh.getY(cut), mesh.getZ(cut));
        edge2Cut.put(edgeKey, cut);
        cutCount++;

        return cut;
    }

    // A face crossing the plane other than along two edges: the corners on the side it holds fewer of laid on the
    // plane, the side nearer it on a tie
    private void layMinority(RiggedMeshStruct mesh, int faceIndex, int kept, int removed) {

        int side = kept < removed
                ? EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT
                : removed < kept ? EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED : resolveNearerSide(mesh, faceIndex);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertex = mesh.getFaceVertex(faceIndex, c);

            if (classify(mesh, vertex) != side)
                continue;

            mesh.setPosition(vertex, 0f, mesh.getY(vertex), mesh.getZ(vertex));
            laidCount++;
        }
    }

    private int resolveNearerSide(RiggedMeshStruct mesh, int faceIndex) {

        float kept = 0f;
        float removed = 0f;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int vertex = mesh.getFaceVertex(faceIndex, c);
            int side = classify(mesh, vertex);

            if (side == EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT)
                kept += Math.abs(mesh.getX(vertex));
            else if (side == EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED)
                removed += Math.abs(mesh.getX(vertex));
        }

        return kept <= removed
                ? EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT
                : EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED;
    }

    // Every face on the side removed taken out, the islands of the faces left kept in step
    private void removeSide(RiggedMeshStruct mesh) {

        int faceCount = mesh.getFaceCount();
        int kept = 0;

        faceFlags.clear();
        faceFlags.size(faceCount);

        for (int f = 0; f < faceCount; f++) {

            boolean removed = countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED) > 0;

            faceFlags.set(f, removed);

            if (!removed)
                faceIslands.set(kept++, faceIslands.getInt(f));
        }

        faceIslands.size(kept);
        mesh.removeFaces(faceFlags);
    }

    // Vertices \\

    // A twin across the plane for every vertex on the side kept, its weights on the twin bones; every vertex on the
    // plane is its own twin, its weights shared evenly with their mirror
    private void addTwinVertices(RiggedMeshStruct mesh) {

        int vertexCount = mesh.getVertexCount();
        int[] mirrorBones = resolveTwinBones(mesh.getRigHandle());

        this.twinStart = vertexCount;
        twins.clear();
        twins.size(vertexCount);

        for (int i = 0; i < vertexCount; i++) {

            int side = classify(mesh, i);

            if (side == EditorSetting.CHARACTER_SYMMETRY_SIDE_REMOVED) {
                twins.set(i, EngineSetting.INDEX_NOT_FOUND);
                continue;
            }

            if (side == EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE) {
                twins.set(i, i);
                characterWeightBranch.symmetrizeWeights(mesh, i, mirrorBones);
                continue;
            }

            int twin = mesh.duplicateVertex(i);

            noteUnmatched(mesh, i);
            mesh.setPosition(twin, -mesh.getX(i), mesh.getY(i), mesh.getZ(i));
            characterWeightBranch.copyMirrored(mesh, i, twin, mirrorBones);
            twins.set(i, twin);
        }
    }

    // Counts each bone off the plane a kept vertex weighs onto that has no twin, so its mirrored side stays on it
    private void noteUnmatched(RiggedMeshStruct mesh, int vertexIndex) {
        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++) {

            int bone = mesh.getInfluenceBone(vertexIndex, i);

            if (twinBones[bone] == bone && !unmatchedBones.getBoolean(bone)
                    && Math.abs(mesh.getRigHandle().getBindWorldMatrix(bone).getM03())
                            > EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE) {
                unmatchedBones.set(bone, true);
                unmatchedCount++;
            }
        }
    }

    // Each bone's twin by name, or for an unsided bone off the plane the unsided bone whose joint lies nearest its
    // mirrored joint within reach, as a hair chain has its twin chain; otherwise itself
    private int[] resolveTwinBones(RigHandle rigHandle) {

        int[] named = characterWeightBranch.resolveMirrorBones(rigHandle);
        int boneCount = named.length;
        float reach = EditorSetting.CHARACTER_SYMMETRY_BONE_REACH * EditorSetting.CHARACTER_SYMMETRY_BONE_REACH;

        if (twinBones.length < boneCount)
            this.twinBones = new int[boneCount];

        System.arraycopy(named, 0, twinBones, 0, boneCount);
        unmatchedBones.clear();
        unmatchedBones.size(boneCount);

        for (int i = 0; i < boneCount; i++) {

            float x = rigHandle.getBindWorldMatrix(i).getM03();

            if (named[i] != i || Math.abs(x) <= EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE)
                continue;

            float y = rigHandle.getBindWorldMatrix(i).getM13();
            float z = rigHandle.getBindWorldMatrix(i).getM23();
            float nearest = reach;

            for (int j = 0; j < boneCount; j++) {

                if (j == i || named[j] != j)
                    continue;

                float dx = rigHandle.getBindWorldMatrix(j).getM03() + x;
                float dy = rigHandle.getBindWorldMatrix(j).getM13() - y;
                float dz = rigHandle.getBindWorldMatrix(j).getM23() - z;
                float distance = dx * dx + dy * dy + dz * dz;

                if (distance <= nearest) {
                    nearest = distance;
                    twinBones[i] = j;
                }
            }
        }

        return twinBones;
    }

    // Morphs \\

    // Every shape key made symmetric: a vertex on the plane takes the even blend of its delta and its mirror's, and
    // a twin takes its vertex's delta mirrored, from the key's twin when the key is sided
    private void mirrorMorphs(RiggedMeshStruct mesh) {

        int morphCount = mesh.getMorphCount();

        for (int m = 0; m < morphCount; m++) {

            int twin = resolveTwinMorph(mesh, m);

            if (twin >= m)
                blendCentre(mesh, m, twin);
        }

        for (int m = 0; m < morphCount; m++) {

            int source = resolveTwinMorph(mesh, m);
            int entryCount = mesh.getMorphEntryCount(source);

            for (int e = 0; e < entryCount; e++) {

                int vertex = mesh.getMorphVertex(source, e);

                if (vertex >= twinStart || classify(mesh, vertex) != EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT)
                    continue;

                mesh.addMorphDelta(
                        m,
                        twins.getInt(vertex),
                        -mesh.getMorphDelta(source, e, 0),
                        mesh.getMorphDelta(source, e, 1),
                        mesh.getMorphDelta(source, e, 2));
            }
        }
    }

    private int resolveTwinMorph(RiggedMeshStruct mesh, int morphIndex) {

        int twin = mesh.getMorphIndex(CharacterMeshUtility.toMirrorBoneName(mesh.getMorphName(morphIndex)));

        return twin != EngineSetting.INDEX_NOT_FOUND ? twin : morphIndex;
    }

    // The deltas two twin keys (or one unsided key twice) give the vertices on the plane, blended with their mirror
    private void blendCentre(RiggedMeshStruct mesh, int morphA, int morphB) {

        centreVertices.clear();
        centreSet.clear();
        collectCentre(mesh, morphA, vertex2EntryA);
        collectCentre(mesh, morphB, vertex2EntryB);

        float half = EditorSetting.CHARACTER_SYMMETRY_HALF;

        for (int i = 0; i < centreVertices.size(); i++) {

            int vertex = centreVertices.getInt(i);
            int entryA = vertex2EntryA.get(vertex);
            int entryB = vertex2EntryB.get(vertex);
            float ax = readDelta(mesh, morphA, entryA, 0);
            float ay = readDelta(mesh, morphA, entryA, 1);
            float az = readDelta(mesh, morphA, entryA, 2);
            float bx = readDelta(mesh, morphB, entryB, 0);
            float by = readDelta(mesh, morphB, entryB, 1);
            float bz = readDelta(mesh, morphB, entryB, 2);
            float x = (ax - bx) * half;
            float y = (ay + by) * half;
            float z = (az + bz) * half;

            writeDelta(mesh, morphA, vertex, entryA, x, y, z);

            if (morphB != morphA)
                writeDelta(mesh, morphB, vertex, entryB, -x, y, z);
        }
    }

    private void collectCentre(RiggedMeshStruct mesh, int morphIndex, Int2IntOpenHashMap vertex2Entry) {

        vertex2Entry.clear();

        for (int e = 0; e < mesh.getMorphEntryCount(morphIndex); e++) {

            int vertex = mesh.getMorphVertex(morphIndex, e);

            if (vertex >= twinStart || classify(mesh, vertex) != EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE)
                continue;

            vertex2Entry.put(vertex, e);

            if (centreSet.add(vertex))
                centreVertices.add(vertex);
        }
    }

    private float readDelta(RiggedMeshStruct mesh, int morphIndex, int entry, int axis) {
        return entry != EngineSetting.INDEX_NOT_FOUND ? mesh.getMorphDelta(morphIndex, entry, axis) : 0f;
    }

    private void writeDelta(RiggedMeshStruct mesh, int morphIndex, int vertex, int entry, float x, float y, float z) {
        if (entry != EngineSetting.INDEX_NOT_FOUND)
            mesh.setMorphEntryDelta(morphIndex, entry, x, y, z);
        else
            mesh.addMorphDelta(morphIndex, vertex, x, y, z);
    }

    // Faces \\

    // A twin for every face left off the plane over the twins of its corners, wound back so it faces outward
    private void addTwinFaces(RiggedMeshStruct mesh, int faceCount) {

        faceTwins.clear();
        faceTwins.size(faceCount);

        for (int f = 0; f < faceCount; f++) {

            if (countSide(mesh, f, EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE) == EngineSetting.QUAD_VERTEX_COUNT) {
                faceTwins.set(f, EngineSetting.INDEX_NOT_FOUND);
                continue;
            }

            int twin = mesh.addFace(
                    twinCorner(mesh, f, 0),
                    twinCorner(mesh, f, 1),
                    twinCorner(mesh, f, 2),
                    twinCorner(mesh, f, 3),
                    mesh.getFaceTexture(f));

            mesh.setFaceZone(twin, mesh.getFaceZone(f));

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setCornerPinned(twin, c, mesh.isCornerPinned(f, toSourceCorner(c)));

            faceTwins.set(f, twin);
        }
    }

    private int twinCorner(RiggedMeshStruct mesh, int faceIndex, int corner) {
        return twins.getInt(mesh.getFaceVertex(faceIndex, toSourceCorner(corner)));
    }

    // The corner of the source face a twin's corner stands over: the first stays, the rest run back
    private int toSourceCorner(int corner) {
        return (EngineSetting.QUAD_VERTEX_COUNT - corner) % EngineSetting.QUAD_VERTEX_COUNT;
    }

    // Every seam on the side kept cut on its twin edge too
    private void mirrorSeams(RiggedMeshStruct mesh) {

        long[] seams = mesh.getSortedSeams();

        for (int i = 0; i < seams.length; i++) {

            int a = RiggedMeshStruct.getSeamVertex(seams[i], 0);
            int b = RiggedMeshStruct.getSeamVertex(seams[i], 1);

            if (a >= twinStart || b >= twinStart)
                continue;

            int twinA = twins.getInt(a);
            int twinB = twins.getInt(b);

            if (twinA != EngineSetting.INDEX_NOT_FOUND && twinB != EngineSetting.INDEX_NOT_FOUND
                    && (twinA != a || twinB != b))
                mesh.setSeam(twinA, twinB, true);
        }
    }

    // UVs \\

    // Each island's twin faces given their UVs where the island's old twin stood: an island that crossed the plane
    // made symmetric across its own centre line, any other mirrored into the place of the island its old twins
    // stood in, or in place when nothing on the side removed answers it
    private void mirrorUVs(RiggedMeshStruct mesh, int faceCount) {

        fillFaces(faceCount);

        ObjectArrayList<IntArrayList> islands = characterUVBranch.groupIslands(
                faceScratch,
                faceIslands.elements(),
                islandKept.size());

        for (int i = 0; i < islands.size(); i++) {

            IntArrayList faces = islands.get(i);

            if (faces.isEmpty())
                continue;

            boolean crossing = islandKept.getBoolean(i) && islandRemoved.getBoolean(i);

            collectPairs(mesh, faces, crossing ? i : resolveTwinIsland(mesh, faces));

            if (crossing && !pairUVs.isEmpty())
                reflectAcrossAxis(mesh, faces);
            else
                placeMirrored(mesh, faces);
        }
    }

    // The island the old twin faces of the island's faces stood in most often, INDEX_NOT_FOUND when none did
    private int resolveTwinIsland(RiggedMeshStruct mesh, IntArrayList faces) {

        island2Votes.clear();

        int best = EngineSetting.INDEX_NOT_FOUND;
        int bestVotes = 0;

        for (int i = 0; i < faces.size(); i++) {

            int record = resolveTwinRecord(mesh, faces.getInt(i));

            if (record == EngineSetting.INDEX_NOT_FOUND)
                continue;

            int island = cornerIslands.getInt(record);
            int votes = island2Votes.addTo(island, 1) + 1;

            if (votes > bestVotes || votes == bestVotes && island < best) {
                best = island;
                bestVotes = votes;
            }
        }

        return best;
    }

    // A removed corner of the face's old twin, the face on the side removed standing on the old twins of all its
    // kept corners in its texture; INDEX_NOT_FOUND when it had none
    private int resolveTwinRecord(RiggedMeshStruct mesh, int faceIndex) {

        int lead = EngineSetting.INDEX_NOT_FOUND;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT && lead == EngineSetting.INDEX_NOT_FOUND; c++)
            if (resolveFirstCorner(mesh, faceIndex, c) != EngineSetting.INDEX_NOT_FOUND)
                lead = c;

        if (lead == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        for (int r = resolveFirstCorner(mesh, faceIndex, lead); r != EngineSetting.INDEX_NOT_FOUND;
                r = cornerNext.getInt(r))
            if (cornerTextures.getInt(r) == mesh.getFaceTexture(faceIndex)
                    && standsOnTwins(mesh, faceIndex, cornerFaces.getInt(r)))
                return r;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean standsOnTwins(RiggedMeshStruct mesh, int faceIndex, int oldFace) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (resolveFirstCorner(mesh, faceIndex, c) != EngineSetting.INDEX_NOT_FOUND
                    && findRecord(mesh, faceIndex, c, oldFace) == EngineSetting.INDEX_NOT_FOUND)
                return false;

        return true;
    }

    // The removed corner a kept corner's old twin has in an old face, INDEX_NOT_FOUND when it has none there
    private int findRecord(RiggedMeshStruct mesh, int faceIndex, int corner, int oldFace) {

        for (int r = resolveFirstCorner(mesh, faceIndex, corner); r != EngineSetting.INDEX_NOT_FOUND;
                r = cornerNext.getInt(r))
            if (cornerFaces.getInt(r) == oldFace)
                return r;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The first removed corner of a kept corner's old twin, INDEX_NOT_FOUND when it had none
    private int resolveFirstCorner(RiggedMeshStruct mesh, int faceIndex, int corner) {

        if (!mesh.hasAuthoredUVs(faceIndex))
            return EngineSetting.INDEX_NOT_FOUND;

        int vertex = mesh.getFaceVertex(faceIndex, corner);

        if (vertex >= originalVertexCount
                || classify(mesh, vertex) != EditorSetting.CHARACTER_SYMMETRY_SIDE_KEPT)
            return EngineSetting.INDEX_NOT_FOUND;

        int old = originalTwins.getInt(vertex);

        return old != EngineSetting.INDEX_NOT_FOUND && old < originalVertexCount
                ? vertex2FirstCorner.getInt(old)
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // Each kept corner's UV beside the UV its old twin had in the face's old twin, where that stood in the island
    // named; four floats a pair
    private void collectPairs(RiggedMeshStruct mesh, IntArrayList faces, int island) {

        pairUVs.clear();

        if (island == EngineSetting.INDEX_NOT_FOUND)
            return;

        for (int i = 0; i < faces.size(); i++) {

            int face = faces.getInt(i);
            int record = resolveTwinRecord(mesh, face);

            if (record == EngineSetting.INDEX_NOT_FOUND || cornerIslands.getInt(record) != island)
                continue;

            int oldFace = cornerFaces.getInt(record);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int corner = findRecord(mesh, face, c, oldFace);

                if (corner == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                pairUVs.add(mesh.getFaceU(face, c));
                pairUVs.add(mesh.getFaceV(face, c));
                pairUVs.add(cornerUVs.getFloat(corner * 2));
                pairUVs.add(cornerUVs.getFloat(corner * 2 + 1));
            }
        }
    }

    // The glide reflection carrying the island's kept corners onto their old twins: its line's normal the direction
    // their offsets vary along (u when they do not vary), its place the median of their midpoints along it, and with
    // glide its slide the median of their offsets along the line
    private void fitReflection(boolean glide) {

        int count = pairUVs.size() / 4;
        float meanU = 0f;
        float meanV = 0f;

        for (int p = 0; p < pairUVs.size(); p += 4) {
            meanU += pairUVs.getFloat(p + 2) - pairUVs.getFloat(p);
            meanV += pairUVs.getFloat(p + 3) - pairUVs.getFloat(p + 1);
        }

        meanU /= count;
        meanV /= count;

        float uu = 0f;
        float vv = 0f;
        float uv = 0f;

        for (int p = 0; p < pairUVs.size(); p += 4) {

            float du = pairUVs.getFloat(p + 2) - pairUVs.getFloat(p) - meanU;
            float dv = pairUVs.getFloat(p + 3) - pairUVs.getFloat(p + 1) - meanV;

            uu += du * du;
            vv += dv * dv;
            uv += du * dv;
        }

        float spread = EditorSetting.CHARACTER_SYMMETRY_AXIS_SPREAD_MIN;
        float angle = uu + vv > spread * spread * count
                ? (float) Math.atan2(2f * uv, uu - vv) * EditorSetting.CHARACTER_SYMMETRY_HALF
                : 0f;

        this.axisNU = (float) Math.cos(angle);
        this.axisNV = (float) Math.sin(angle);

        fitV.clear();

        for (int p = 0; p < pairUVs.size(); p += 4)
            fitV.add(((pairUVs.getFloat(p) + pairUVs.getFloat(p + 2)) * axisNU
                    + (pairUVs.getFloat(p + 1) + pairUVs.getFloat(p + 3)) * axisNV)
                    * EditorSetting.CHARACTER_SYMMETRY_HALF);

        this.axisOffset = median(fitV);
        this.axisGlide = 0f;

        if (!glide)
            return;

        fitV.clear();

        for (int p = 0; p < pairUVs.size(); p += 4)
            fitV.add((pairUVs.getFloat(p + 3) - pairUVs.getFloat(p + 1)) * axisNU
                    - (pairUVs.getFloat(p + 2) - pairUVs.getFloat(p)) * axisNV);

        this.axisGlide = median(fitV);
    }

    private float acrossAxis(float u, float v) {
        return u * axisNU + v * axisNV - axisOffset;
    }

    private float reflectU(float u, float v) {
        return u - 2f * acrossAxis(u, v) * axisNU - axisGlide * axisNV;
    }

    private float reflectV(float u, float v) {
        return v - 2f * acrossAxis(u, v) * axisNV + axisGlide * axisNU;
    }

    // An island that crossed the plane made symmetric across its own centre line, the kept corners on the plane that
    // lie within a weld of the line laid on it first so both halves meet
    private void reflectAcrossAxis(RiggedMeshStruct mesh, IntArrayList faces) {

        fitReflection(false);

        for (int i = 0; i < faces.size(); i++) {

            int face = faces.getInt(i);

            if (!mesh.hasAuthoredUVs(face))
                continue;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                float u = mesh.getFaceU(face, c);
                float v = mesh.getFaceV(face, c);
                float across = acrossAxis(u, v);

                if (classify(mesh, mesh.getFaceVertex(face, c)) == EditorSetting.CHARACTER_SYMMETRY_SIDE_CENTRE
                        && Math.abs(across) <= EditorSetting.CHARACTER_SYMMETRY_AXIS_WELD)
                    mesh.setFaceUV(face, c, u - across * axisNU, v - across * axisNV);
            }
        }

        layTwinUVs(mesh, faces, true);
    }

    // The island's twin laid on its old twins' UVs: reflected for an island of its own, or moved for a layout that
    // shared one, whichever fits them best; mirrored in u within its own span when no old twin answers it
    private void placeMirrored(RiggedMeshStruct mesh, IntArrayList faces) {

        if (pairUVs.isEmpty()) {

            this.axisNU = 1f;
            this.axisNV = 0f;
            this.axisOffset = resolveSpanU(mesh, faces) * EditorSetting.CHARACTER_SYMMETRY_HALF;
            this.axisGlide = 0f;
            overlapCount++;
            layTwinUVs(mesh, faces, true);
            return;
        }

        fitReflection(true);
        fitShift();
        layTwinUVs(mesh, faces, resolveDeviation(true) <= resolveDeviation(false));
    }

    // The move carrying the island's kept corners onto their old twins, as a shared layout lays them
    private void fitShift() {

        fitV.clear();

        for (int p = 0; p < pairUVs.size(); p += 4)
            fitV.add(pairUVs.getFloat(p + 2) - pairUVs.getFloat(p));

        this.shiftU = median(fitV);

        fitV.clear();

        for (int p = 0; p < pairUVs.size(); p += 4)
            fitV.add(pairUVs.getFloat(p + 3) - pairUVs.getFloat(p + 1));

        this.shiftV = median(fitV);
    }

    // How far the reflection, or the move, leaves the island's kept corners from their old twins in all
    private float resolveDeviation(boolean reflect) {

        float total = 0f;

        for (int p = 0; p < pairUVs.size(); p += 4) {

            float u = pairUVs.getFloat(p);
            float v = pairUVs.getFloat(p + 1);

            total += Math.abs((reflect ? reflectU(u, v) : u + shiftU) - pairUVs.getFloat(p + 2))
                    + Math.abs((reflect ? reflectV(u, v) : v + shiftV) - pairUVs.getFloat(p + 3));
        }

        return total;
    }

    // Each twin face's corners its source's carried by the reflection, or by the move
    private void layTwinUVs(RiggedMeshStruct mesh, IntArrayList faces, boolean reflect) {
        for (int i = 0; i < faces.size(); i++) {

            int face = faces.getInt(i);
            int twin = faceTwins.getInt(face);

            if (twin == EngineSetting.INDEX_NOT_FOUND || !mesh.hasAuthoredUVs(face))
                continue;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int source = toSourceCorner(c);
                float u = mesh.getFaceU(face, source);
                float v = mesh.getFaceV(face, source);

                mesh.setFaceUV(
                        twin,
                        c,
                        reflect ? reflectU(u, v) : u + shiftU,
                        reflect ? reflectV(u, v) : v + shiftV);
            }
        }
    }

    // The shift that mirrors an island in u within its own span
    private float resolveSpanU(RiggedMeshStruct mesh, IntArrayList faces) {

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        for (int i = 0; i < faces.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                float u = mesh.getFaceU(faces.getInt(i), c);

                min = Math.min(min, u);
                max = Math.max(max, u);
            }

        return min + max;
    }

    private float median(FloatArrayList values) {

        FloatArrays.quickSort(values.elements(), 0, values.size());
        return values.getFloat(values.size() / 2);
    }

    // Utility \\

    private float lerp(float from, float to, float share) {
        return from + (to - from) * share;
    }

    private String describe(CharacterMeshDocumentInstance document) {

        StringBuilder message = new StringBuilder(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRIZED)
                .append(document.getMeshName())
                .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_SEPARATOR)
                .append(document.getMesh().getFaceCount())
                .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_FACES)
                .append(cutCount)
                .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_CUT)
                .append(laidCount)
                .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_LAID);

        if (overlapCount > 0)
            message.append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_OVERLAP_START)
                    .append(overlapCount)
                    .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_OVERLAP);

        if (unmatchedCount > 0)
            message.append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_OVERLAP_START)
                    .append(unmatchedCount)
                    .append(EditorSetting.CHARACTER_EDITOR_MESSAGE_SYMMETRY_UNMATCHED);

        return message.toString();
    }
}
