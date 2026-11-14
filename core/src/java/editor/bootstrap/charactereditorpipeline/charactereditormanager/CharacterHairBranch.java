package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshZone;
import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigSpringChainStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterHairCardStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterHairLockStruct;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterPoseUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterHairBranch extends BranchPackage {

    /*
     * Finds the cards and the locks of a hair mesh from its raw UVs, the way a
     * groom is rigged for spring bones, for the rig tools to lay chains on and
     * the weight tools to weigh onto them. A card is a run of hair faces
     * joined corner to corner whose v runs from root to tip; its line is read
     * row by row, each row's vertices averaged where they stand at one v. A
     * card at least the lock length long is long hair: cards rooted together
     * and hanging alike, matched point for point along their length, are one
     * lock, whose chain follows the line they share in as many segments as
     * its length takes. Shorter hair is left to the shader. A card weighs
     * onto the hair chain whose joints lie nearest its own line, read along
     * both by distance from the root, so a shorter style rides the upper
     * joints of the chains a longer one laid. Run only when asked, never per
     * frame.
     */

    // Search — main thread only
    private int[] parent;
    private Vector3 sampleScratch;
    private Vector3 chainScratch;

    // Base \\

    @Override
    protected void create() {

        // Search
        this.sampleScratch = new Vector3();
        this.chainScratch = new Vector3();
    }

    // Cards \\

    // Every card of a hair mesh: its hair faces joined where they share a corner, kept where their v spans root to tip
    void detectCards(RiggedMeshStruct mesh, ObjectArrayList<CharacterHairCardStruct> cards) {

        cards.clear();
        this.parent = new int[mesh.getVertexCount()];

        for (int v = 0; v < parent.length; v++)
            parent[v] = v;

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (isCardFace(mesh, f))
                for (int c = 1; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    union(mesh.getFaceVertex(f, 0), mesh.getFaceVertex(f, c));

        int[] group2Card = new int[parent.length];
        ObjectArrayList<IntArrayList> groups = new ObjectArrayList<>();

        Arrays.fill(group2Card, EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!isCardFace(mesh, f))
                continue;

            int root = find(mesh.getFaceVertex(f, 0));

            if (group2Card[root] == EngineSetting.INDEX_NOT_FOUND) {
                group2Card[root] = groups.size();
                groups.add(new IntArrayList());
            }

            groups.get(group2Card[root]).add(f);
        }

        for (int g = 0; g < groups.size(); g++) {

            CharacterHairCardStruct card = buildCard(mesh, groups.get(g));

            if (card != null)
                cards.add(card);
        }

        this.parent = null;
    }

    private boolean isCardFace(RiggedMeshStruct mesh, int faceIndex) {
        return mesh.getFaceZone(faceIndex) == MeshZone.HAIR && mesh.hasAuthoredUVs(faceIndex);
    }

    // A card's rows root to tip, each the middle of its vertices, and every vertex with the v it stands at; null for a
    // run of faces whose v never spans root to tip, a scalp cap
    private CharacterHairCardStruct buildCard(RiggedMeshStruct mesh, IntArrayList faces) {

        int cornerCount = faces.size() * EngineSetting.QUAD_VERTEX_COUNT;
        int[] cornerVertices = new int[cornerCount];
        float[] cornerVs = new float[cornerCount];
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;

        for (int i = 0; i < faces.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int corner = i * EngineSetting.QUAD_VERTEX_COUNT + c;

                cornerVertices[corner] = mesh.getFaceVertex(faces.getInt(i), c);
                cornerVs[corner] = mesh.getFaceV(faces.getInt(i), c);
                low = Math.min(low, cornerVs[corner]);
                high = Math.max(high, cornerVs[corner]);
            }

        if (high - low < EditorSetting.CHARACTER_HAIR_CARD_MIN_SPAN)
            return null;

        int[] order = new int[cornerCount];

        for (int i = 0; i < cornerCount; i++)
            order[i] = i;

        IntArrays.quickSort(order, (a, b) -> Float.compare(cornerVs[a], cornerVs[b]));

        ObjectArrayList<Vector3> points = new ObjectArrayList<>();
        FloatArrayList rowVs = new FloatArrayList();
        IntArrayList vertices = new IntArrayList();
        FloatArrayList vertexVs = new FloatArrayList();
        IntArrayList row = new IntArrayList();
        float rowV = cornerVs[order[0]];

        for (int i = 0; i <= cornerCount; i++) {

            if (i == cornerCount || cornerVs[order[i]] - rowV > EditorSetting.CHARACTER_HAIR_ROW_GAP) {

                points.add(centroid(mesh, row));
                rowVs.add(rowV);
                row.clear();

                if (i == cornerCount)
                    break;

                rowV = cornerVs[order[i]];
            }

            int vertex = cornerVertices[order[i]];

            if (!row.contains(vertex))
                row.add(vertex);

            int known = vertices.indexOf(vertex);

            if (known == EngineSetting.INDEX_NOT_FOUND) {
                vertices.add(vertex);
                vertexVs.add(cornerVs[order[i]]);
            } else {
                vertexVs.set(known, Math.max(vertexVs.getFloat(known), cornerVs[order[i]]));
            }
        }

        return new CharacterHairCardStruct(points, rowVs, vertices, vertexVs);
    }

    private Vector3 centroid(RiggedMeshStruct mesh, IntArrayList members) {

        Vector3 centre = new Vector3();

        for (int i = 0; i < members.size(); i++) {
            int v = members.getInt(i);
            centre.add(mesh.getX(v), mesh.getY(v), mesh.getZ(v));
        }

        return centre.divide(members.size());
    }

    private void union(int a, int b) {
        parent[find(a)] = find(b);
    }

    private int find(int vertex) {

        int root = vertex;

        while (parent[root] != root)
            root = parent[root];

        while (parent[vertex] != root) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }

        return root;
    }

    // Locks \\

    // The locks of long hair in a hair mesh, longest first: cards long enough joined to the lock whose first card
    // they lie nearest point for point, within the lock radius
    void detectLocks(RiggedMeshStruct mesh, ObjectArrayList<CharacterHairLockStruct> locks) {

        locks.clear();

        ObjectArrayList<CharacterHairCardStruct> cards = new ObjectArrayList<>();
        ObjectArrayList<CharacterHairCardStruct> seeds = new ObjectArrayList<>();
        ObjectArrayList<ObjectArrayList<CharacterHairCardStruct>> groups = new ObjectArrayList<>();

        detectCards(mesh, cards);
        cards.removeIf(card -> card.getLength() < EditorSetting.CHARACTER_HAIR_LOCK_MIN_LENGTH);
        cards.sort((a, b) -> Float.compare(b.getLength(), a.getLength()));

        for (int c = 0; c < cards.size(); c++) {

            CharacterHairCardStruct card = cards.get(c);
            int nearest = EngineSetting.INDEX_NOT_FOUND;
            float best = EditorSetting.CHARACTER_HAIR_LOCK_RADIUS;

            for (int g = 0; g < seeds.size(); g++) {

                float gap = measureGap(card, seeds.get(g));

                if (gap <= best) {
                    best = gap;
                    nearest = g;
                }
            }

            if (nearest == EngineSetting.INDEX_NOT_FOUND) {
                seeds.add(card);
                groups.add(new ObjectArrayList<>());
                nearest = groups.size() - 1;
            }

            groups.get(nearest).add(card);
        }

        for (int g = 0; g < groups.size(); g++)
            locks.add(buildLock(groups.get(g)));
    }

    // How far apart two cards hang on average, matched at the same share of their lengths
    private float measureGap(CharacterHairCardStruct a, CharacterHairCardStruct b) {

        int samples = EditorSetting.CHARACTER_HAIR_LOCK_SAMPLES;
        float gap = 0f;

        for (int s = 0; s < samples; s++) {

            float share = s / (samples - 1f);

            a.sampleAt(a.getLength() * share, sampleScratch);
            b.sampleAt(b.getLength() * share, chainScratch);
            gap += distance(sampleScratch, chainScratch);
        }

        return gap / samples;
    }

    // A lock's joints along the line its cards share, one per segment of its length and its tip, and its thickness
    private CharacterHairLockStruct buildLock(ObjectArrayList<CharacterHairCardStruct> cards) {

        float length = 0f;

        for (int i = 0; i < cards.size(); i++)
            length += cards.get(i).getLength();

        length /= cards.size();

        int segments = Math.clamp(
                Math.round(length / EditorSetting.CHARACTER_HAIR_LOCK_SEGMENT),
                EditorSetting.CHARACTER_HAIR_LOCK_MIN_SEGMENTS,
                EditorSetting.CHARACTER_HAIR_LOCK_MAX_SEGMENTS);
        ObjectArrayList<Vector3> joints = new ObjectArrayList<>();
        float spread = 0f;

        for (int j = 0; j <= segments; j++) {

            float share = j / (float) segments;
            Vector3 joint = new Vector3();

            for (int i = 0; i < cards.size(); i++)
                joint.add(cards.get(i).sampleAt(cards.get(i).getLength() * share, sampleScratch));

            joints.add(joint.divide(cards.size()));

            for (int i = 0; i < cards.size(); i++)
                spread += distance(cards.get(i).sampleAt(cards.get(i).getLength() * share, sampleScratch), joint);
        }

        float thickness = spread / (cards.size() * (segments + 1)) * EditorSetting.CHARACTER_HAIR_LOCK_THICKNESS_SHARE;

        return new CharacterHairLockStruct(joints, Math.max(thickness, EditorSetting.CHARACTER_RIG_SIZE_MIN));
    }

    // Anchor \\

    // The bone a hair mesh grows on: the one the roots of its cards weigh most onto, INDEX_NOT_FOUND without cards
    int resolveAnchor(RiggedMeshStruct mesh, ObjectArrayList<CharacterHairCardStruct> cards) {

        float[] totals = new float[mesh.getRigHandle().getBoneCount()];
        int anchor = EngineSetting.INDEX_NOT_FOUND;

        for (int c = 0; c < cards.size(); c++) {

            CharacterHairCardStruct card = cards.get(c);

            for (int i = 0; i < card.getVertices().size(); i++) {

                if (card.reachAt(card.getVertexV(i)) > EditorSetting.CHARACTER_HAIR_WEIGHT_ROOT)
                    continue;

                int vertex = card.getVertex(i);

                for (int k = 0; k < mesh.getInfluenceCount(vertex); k++)
                    totals[mesh.getInfluenceBone(vertex, k)] += mesh.getInfluenceWeight(vertex, k);
            }
        }

        for (int b = 0; b < totals.length; b++)
            if (totals[b] > 0f && (anchor == EngineSetting.INDEX_NOT_FOUND || totals[b] > totals[anchor]))
                anchor = b;

        return anchor;
    }

    // Chains \\

    // The hair chain of a loaded rig whose joints lie nearest a card's line, both read by distance from their roots
    // over the length they share; INDEX_NOT_FOUND when no chain's root lies within the weight reach of the card's
    int findChain(RigHandle rigHandle, CharacterHairCardStruct card, FloatArrayList reaches) {

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        float best = Float.MAX_VALUE;

        for (int c = 0; c < rigHandle.getSpringChainCount(); c++) {

            RigSpringChainStruct chain = rigHandle.getSpringChain(c);

            if (!isHairChain(rigHandle, chain))
                continue;

            resolveReaches(rigHandle, chain, reaches);
            sampleChain(rigHandle, chain, reaches, 0f, chainScratch);

            if (distance(card.getRoot(), chainScratch) > EditorSetting.CHARACTER_HAIR_WEIGHT_REACH)
                continue;

            float gap = measureChainGap(rigHandle, chain, reaches, card);

            if (gap < best) {
                best = gap;
                nearest = c;
            }
        }

        return nearest;
    }

    private boolean isHairChain(RigHandle rigHandle, RigSpringChainStruct chain) {
        return rigHandle.getBone(chain.getBone(0)).getName().startsWith(EditorSetting.CHARACTER_HAIR_LOCK_PREFIX);
    }

    private float measureChainGap(
            RigHandle rigHandle,
            RigSpringChainStruct chain,
            FloatArrayList reaches,
            CharacterHairCardStruct card) {

        int samples = EditorSetting.CHARACTER_HAIR_LOCK_SAMPLES;
        float shared = Math.min(card.getLength(), reaches.getFloat(reaches.size() - 1));
        float gap = 0f;

        for (int s = 0; s < samples; s++) {

            float reach = shared * s / (samples - 1f);

            card.sampleAt(reach, sampleScratch);
            sampleChain(rigHandle, chain, reaches, reach, chainScratch);
            gap += distance(sampleScratch, chainScratch);
        }

        return gap / samples;
    }

    // How far along a chain each of its joints stands from its root in the bind pose
    void resolveReaches(RigHandle rigHandle, RigSpringChainStruct chain, FloatArrayList reaches) {

        reaches.clear();

        float reach = 0f;

        for (int j = 0; j < chain.getBoneCount(); j++) {

            CharacterPoseUtility.getTranslation(rigHandle.getBindWorldMatrix(chain.getBone(j)), sampleScratch);

            if (j > 0)
                reach += distance(sampleScratch, chainScratch);

            reaches.add(reach);
            chainScratch.set(sampleScratch);
        }
    }

    // The point a distance along a chain's joints in the bind pose, held to its ends
    private Vector3 sampleChain(
            RigHandle rigHandle,
            RigSpringChainStruct chain,
            FloatArrayList reaches,
            float reach,
            Vector3 out) {

        int last = chain.getBoneCount() - 1;
        int segment = 0;

        while (segment < last - 1 && reach > reaches.getFloat(segment + 1))
            segment++;

        float span = reaches.getFloat(segment + 1) - reaches.getFloat(segment);
        float t = span > 0f ? Math.clamp((reach - reaches.getFloat(segment)) / span, 0f, 1f) : 0f;
        float ax = rigHandle.getBindWorldMatrix(chain.getBone(segment)).val[12];
        float ay = rigHandle.getBindWorldMatrix(chain.getBone(segment)).val[13];
        float az = rigHandle.getBindWorldMatrix(chain.getBone(segment)).val[14];
        float bx = rigHandle.getBindWorldMatrix(chain.getBone(segment + 1)).val[12];
        float by = rigHandle.getBindWorldMatrix(chain.getBone(segment + 1)).val[13];
        float bz = rigHandle.getBindWorldMatrix(chain.getBone(segment + 1)).val[14];

        return out.set(ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t);
    }

    // Geometry \\

    private float distance(Vector3 a, Vector3 b) {

        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dz = b.z - a.z;

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
