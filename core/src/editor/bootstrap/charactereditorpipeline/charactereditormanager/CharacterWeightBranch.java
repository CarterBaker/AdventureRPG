package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CharacterWeightBranch extends BranchPackage {

    /*
     * Performs every change to the bones a character mesh's vertices follow.
     * Setting one bone's weight scales the others to fill the rest, so a
     * vertex's weights always sum to one; a vertex keeps at most
     * MAX_BONE_INFLUENCES, the lightest dropped first, and never loses its
     * last bone. Weights are rounded to the editor's precision with the
     * remainder given to the heaviest. With mirror on, every vertex changed
     * hands its weights, left and right bones swapped, to its mirror. Brush
     * strokes change weights live and become one undo step when they end.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;

    // Palette
    private Object2ObjectOpenHashMap<RigHandle, int[]> rig2MirrorBones;

    // Scratch
    private int[] boneScratch;
    private float[] weightScratch;
    private float[] boneTotals;
    private IntArrayList touchedBones;
    private IntArrayList blendBones;
    private FloatArrayList blendWeights;
    private BooleanArrayList changedVertices;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.rig2MirrorBones = new Object2ObjectOpenHashMap<>();

        // Scratch
        this.boneScratch = new int[EngineSetting.MAX_BONE_INFLUENCES + 1];
        this.weightScratch = new float[EngineSetting.MAX_BONE_INFLUENCES + 1];
        this.boneTotals = new float[0];
        this.touchedBones = new IntArrayList();
        this.blendBones = new IntArrayList();
        this.blendWeights = new FloatArrayList();
        this.changedVertices = new BooleanArrayList();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
    }

    // Selection Edits \\

    void assign(CharacterMeshDocumentInstance document, int boneIndex, float weight) {

        beginChange(document);

        for (int i = 0; i < document.getSelection().size(); i++)
            if (document.isSelected(i))
                setBoneWeight(document.getMesh(), i, boneIndex, weight);

        finishChange(document);
    }

    void add(CharacterMeshDocumentInstance document, int boneIndex, float delta) {

        RiggedMeshStruct mesh = document.getMesh();

        beginChange(document);

        for (int i = 0; i < document.getSelection().size(); i++)
            if (document.isSelected(i))
                setBoneWeight(mesh, i, boneIndex, mesh.getBoneWeight(i, boneIndex) + delta);

        finishChange(document);
    }

    void normalize(CharacterMeshDocumentInstance document) {

        beginChange(document);

        for (int i = 0; i < document.getSelection().size(); i++)
            if (document.isSelected(i))
                writeVertex(document.getMesh(), i, gather(document.getMesh(), i));

        finishChange(document);
    }

    void smooth(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        beginChange(document);
        blendBones.clear();
        blendWeights.clear();

        for (int i = 0; i < mesh.getVertexCount(); i++) {
            resolveBlend(mesh, i, document.isSelected(i) ? 1f : 0f);
            changedVertices.set(i, document.isSelected(i));
        }

        applyBlend(mesh);
        finishChange(document);
    }

    // Copies weights from one side of the mesh onto the other, left and right bones swapped
    void mirror(CharacterMeshDocumentInstance document, boolean positiveToNegative) {

        RiggedMeshStruct mesh = document.getMesh();
        IntArrayList mirrorVertices = characterSelectionBranch.resolveMirror(document);
        int[] mirrorBones = resolveMirrorBones(mesh.getRigHandle());

        characterHistoryBranch.begin(document);

        for (int i = 0; i < mesh.getVertexCount(); i++) {

            int mirror = mirrorVertices.getInt(i);
            float x = mesh.getX(i);

            if (mirror != EngineSetting.INDEX_NOT_FOUND && mirror != i && (positiveToNegative ? x > 0f : x < 0f))
                copyMirrored(mesh, i, mirror, mirrorBones);
        }

        characterMeshBranch.finishEdit(document);
    }

    // Brush \\

    void beginStroke(CharacterMeshDocumentInstance document) {
        characterHistoryBranch.begin(document);
    }

    // Draws a bone's weight on each vertex toward a target by the strength the brush gives it there
    void paint(
            CharacterMeshDocumentInstance document,
            int boneIndex,
            IntArrayList vertices,
            FloatArrayList strengths,
            float target) {

        RiggedMeshStruct mesh = document.getMesh();

        resetChanged(mesh);

        for (int i = 0; i < vertices.size(); i++) {

            int vertexIndex = vertices.getInt(i);
            float weight = mesh.getBoneWeight(vertexIndex, boneIndex);

            setBoneWeight(mesh, vertexIndex, boneIndex, weight + (target - weight) * strengths.getFloat(i));
            changedVertices.set(vertexIndex, true);
        }

        mirrorChanged(document);
        characterMeshBranch.preview(document);
    }

    // Blends each vertex's weights toward the average of its own and its neighbours' by the brush's strength there
    void blur(CharacterMeshDocumentInstance document, IntArrayList vertices, FloatArrayList strengths) {

        RiggedMeshStruct mesh = document.getMesh();

        resetChanged(mesh);
        blendBones.clear();
        blendWeights.clear();

        for (int i = 0; i < mesh.getVertexCount(); i++)
            resolveBlend(mesh, i, 0f);

        for (int i = 0; i < vertices.size(); i++) {

            int vertexIndex = vertices.getInt(i);

            blendTowardNeighbours(mesh, vertexIndex, strengths.getFloat(i));
            changedVertices.set(vertexIndex, true);
        }

        applyBlend(mesh);
        mirrorChanged(document);
        characterMeshBranch.preview(document);
    }

    void endStroke(CharacterMeshDocumentInstance document) {

        if (document.hasOpenStep())
            characterMeshBranch.finishEdit(document);
    }

    // Weights \\

    // Sets one bone's weight on a vertex, scaling its others to fill the rest; false when nothing could change
    boolean setBoneWeight(RiggedMeshStruct mesh, int vertexIndex, int boneIndex, float weight) {

        float target = CharacterViewUtility.clamp(weight, 0f, 1f);
        int count = gather(mesh, vertexIndex);
        int slot = findSlot(boneIndex, count);
        float others = 0f;

        for (int i = 0; i < count; i++)
            if (i != slot)
                others += weightScratch[i];

        if (others <= EditorSetting.CHARACTER_MESH_WEIGHT_EPSILON) {

            if (target <= EditorSetting.CHARACTER_MESH_WEIGHT_EPSILON)
                return false;

            boneScratch[0] = boneIndex;
            weightScratch[0] = 1f;
            writeVertex(mesh, vertexIndex, 1);
            return true;
        }

        if (slot == EngineSetting.INDEX_NOT_FOUND) {

            if (target <= EditorSetting.CHARACTER_MESH_WEIGHT_EPSILON)
                return false;

            slot = count++;
            boneScratch[slot] = boneIndex;
        }

        float scale = (1f - target) / others;

        for (int i = 0; i < count; i++)
            weightScratch[i] = i == slot ? target : weightScratch[i] * scale;

        writeVertex(mesh, vertexIndex, count);
        return true;
    }

    // A vertex's influences into the scratch, returning how many
    private int gather(RiggedMeshStruct mesh, int vertexIndex) {

        int count = mesh.getInfluenceCount(vertexIndex);

        for (int i = 0; i < count; i++) {
            boneScratch[i] = mesh.getInfluenceBone(vertexIndex, i);
            weightScratch[i] = mesh.getInfluenceWeight(vertexIndex, i);
        }

        return count;
    }

    private int findSlot(int boneIndex, int count) {

        for (int i = 0; i < count; i++)
            if (boneScratch[i] == boneIndex)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Writes the scratch back as the vertex's influences: weightless bones dropped, the lightest dropped past the
    // limit, the rest scaled to sum to one, rounded, and the rounding's remainder given to the heaviest
    private void writeVertex(RiggedMeshStruct mesh, int vertexIndex, int count) {

        count = dropLight(count);

        float sum = 0f;

        for (int i = 0; i < count; i++)
            sum += weightScratch[i];

        if (count == 0 || sum <= 0f)
            return;

        float rounded = 0f;
        int heaviest = 0;

        for (int i = 0; i < count; i++) {

            weightScratch[i] = CharacterViewUtility.round(weightScratch[i] / sum);
            rounded += weightScratch[i];

            if (weightScratch[i] > weightScratch[heaviest])
                heaviest = i;
        }

        weightScratch[heaviest] = CharacterViewUtility.round(weightScratch[heaviest] + 1f - rounded);
        mesh.clearInfluences(vertexIndex);

        for (int i = 0; i < count; i++)
            if (weightScratch[i] > 0f)
                mesh.addInfluence(vertexIndex, boneScratch[i], weightScratch[i]);
    }

    private int dropLight(int count) {

        for (int i = count - 1; i >= 0; i--)
            if (weightScratch[i] <= EditorSetting.CHARACTER_MESH_WEIGHT_EPSILON && count > 1)
                count = removeSlot(i, count);

        while (count > EngineSetting.MAX_BONE_INFLUENCES)
            count = removeSlot(findLightest(count), count);

        return count;
    }

    private int findLightest(int count) {

        int lightest = 0;

        for (int i = 1; i < count; i++)
            if (weightScratch[i] < weightScratch[lightest])
                lightest = i;

        return lightest;
    }

    private int removeSlot(int slot, int count) {

        for (int i = slot; i < count - 1; i++) {
            boneScratch[i] = boneScratch[i + 1];
            weightScratch[i] = weightScratch[i + 1];
        }

        return count - 1;
    }

    // Blend \\

    // Records a vertex's weights blended toward the average of its own and its neighbours' by an amount
    private void resolveBlend(RiggedMeshStruct mesh, int vertexIndex, float amount) {

        if (amount <= 0f) {
            recordBlend(mesh, vertexIndex);
            return;
        }

        blendTowardNeighbours(mesh, vertexIndex, amount);
    }

    private void recordBlend(RiggedMeshStruct mesh, int vertexIndex) {

        int count = mesh.getInfluenceCount(vertexIndex);

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {
            blendBones.add(i < count ? mesh.getInfluenceBone(vertexIndex, i) : EngineSetting.INDEX_NOT_FOUND);
            blendWeights.add(i < count ? mesh.getInfluenceWeight(vertexIndex, i) : 0f);
        }
    }

    // Overwrites a vertex's recorded blend, or records it when its turn comes, with its weights moved toward the
    // average of every vertex it shares a face with and itself
    private void blendTowardNeighbours(RiggedMeshStruct mesh, int vertexIndex, float amount) {

        int boneCount = mesh.getRigHandle().getBoneCount();

        if (boneTotals.length < boneCount)
            this.boneTotals = new float[boneCount];

        touchedBones.clear();
        int neighbours = accumulateNeighbours(mesh, vertexIndex);

        for (int i = 0; i < touchedBones.size(); i++) {

            int bone = touchedBones.getInt(i);
            float average = boneTotals[bone] / neighbours;

            boneTotals[bone] = mesh.getBoneWeight(vertexIndex, bone) * (1f - amount) + average * amount;
        }

        int count = 0;

        for (int i = 0; i < touchedBones.size(); i++) {

            int bone = touchedBones.getInt(i);

            if (count <= EngineSetting.MAX_BONE_INFLUENCES) {
                boneScratch[count] = bone;
                weightScratch[count] = boneTotals[bone];
                count++;
            } else {
                int lightest = findLightest(count);

                if (boneTotals[bone] > weightScratch[lightest]) {
                    boneScratch[lightest] = bone;
                    weightScratch[lightest] = boneTotals[bone];
                }
            }

            boneTotals[bone] = 0f;
        }

        count = dropLight(count);

        float sum = 0f;

        for (int i = 0; i < count; i++)
            sum += weightScratch[i];

        int base = vertexIndex * EngineSetting.MAX_BONE_INFLUENCES;

        while (blendBones.size() < base + EngineSetting.MAX_BONE_INFLUENCES) {
            blendBones.add(EngineSetting.INDEX_NOT_FOUND);
            blendWeights.add(0f);
        }

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {
            blendBones.set(base + i, i < count ? boneScratch[i] : EngineSetting.INDEX_NOT_FOUND);
            blendWeights.set(base + i, i < count && sum > 0f ? weightScratch[i] / sum : 0f);
        }
    }

    // Sums the weights of a vertex and every vertex it shares a face with into the bone totals, counting them
    private int accumulateNeighbours(RiggedMeshStruct mesh, int vertexIndex) {

        int neighbours = 1;

        accumulate(mesh, vertexIndex);

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!mesh.usesVertex(f, vertexIndex))
                continue;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int other = mesh.getFaceVertex(f, c);

                if (other == vertexIndex)
                    continue;

                accumulate(mesh, other);
                neighbours++;
            }
        }

        return neighbours;
    }

    private void accumulate(RiggedMeshStruct mesh, int vertexIndex) {

        for (int i = 0; i < mesh.getInfluenceCount(vertexIndex); i++) {

            int bone = mesh.getInfluenceBone(vertexIndex, i);

            if (boneTotals[bone] == 0f && !touchedBones.contains(bone))
                touchedBones.add(bone);

            boneTotals[bone] += mesh.getInfluenceWeight(vertexIndex, i);
        }
    }

    // Writes the recorded blend of every changed vertex back, so no vertex's new weights feed a neighbour's blend in
    // the same pass
    private void applyBlend(RiggedMeshStruct mesh) {

        for (int v = 0; v < mesh.getVertexCount(); v++) {

            if (!changedVertices.getBoolean(v))
                continue;

            int base = v * EngineSetting.MAX_BONE_INFLUENCES;
            int count = 0;

            for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {

                int bone = blendBones.getInt(base + i);

                if (bone == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                boneScratch[count] = bone;
                weightScratch[count] = blendWeights.getFloat(base + i);
                count++;
            }

            if (count > 0)
                writeVertex(mesh, v, count);
        }
    }

    // Averages several vertices' weights onto one of them, keeping the heaviest bones
    void average(RiggedMeshStruct mesh, IntArrayList vertices, int targetVertex) {

        int boneCount = mesh.getRigHandle().getBoneCount();

        if (boneTotals.length < boneCount)
            this.boneTotals = new float[boneCount];

        touchedBones.clear();

        for (int i = 0; i < vertices.size(); i++)
            accumulate(mesh, vertices.getInt(i));

        int count = 0;

        for (int i = 0; i < touchedBones.size(); i++) {

            int bone = touchedBones.getInt(i);

            if (count <= EngineSetting.MAX_BONE_INFLUENCES) {
                boneScratch[count] = bone;
                weightScratch[count] = boneTotals[bone];
                count++;
            } else {
                int lightest = findLightest(count);

                if (boneTotals[bone] > weightScratch[lightest]) {
                    boneScratch[lightest] = bone;
                    weightScratch[lightest] = boneTotals[bone];
                }
            }

            boneTotals[bone] = 0f;
        }

        writeVertex(mesh, targetVertex, count);
    }

    // Mirror \\

    // A vertex's weights onto another, each left bone swapped for its right twin and each right for its left
    void copyMirrored(RiggedMeshStruct mesh, int fromVertex, int toVertex, int[] mirrorBones) {

        int count = mesh.getInfluenceCount(fromVertex);

        for (int i = 0; i < count; i++) {
            boneScratch[i] = mirrorBones[mesh.getInfluenceBone(fromVertex, i)];
            weightScratch[i] = mesh.getInfluenceWeight(fromVertex, i);
        }

        mesh.clearInfluences(toVertex);

        for (int i = 0; i < count; i++)
            mesh.addInfluence(toVertex, boneScratch[i], weightScratch[i]);
    }

    // Each bone's twin across the rig's centre by name, or itself
    int[] resolveMirrorBones(RigHandle rigHandle) {

        int[] mirrorBones = rig2MirrorBones.get(rigHandle);

        if (mirrorBones != null)
            return mirrorBones;

        mirrorBones = new int[rigHandle.getBoneCount()];

        for (int i = 0; i < mirrorBones.length; i++) {

            String mirrorName = CharacterMeshUtility.toMirrorBoneName(rigHandle.getBone(i).getName());
            mirrorBones[i] = rigHandle.hasBone(mirrorName) ? rigHandle.getBoneIndex(mirrorName) : i;
        }

        rig2MirrorBones.put(rigHandle, mirrorBones);
        return mirrorBones;
    }

    int resolveMirrorBone(RigHandle rigHandle, int boneIndex) {
        return resolveMirrorBones(rigHandle)[boneIndex];
    }

    // Change \\

    private void beginChange(CharacterMeshDocumentInstance document) {
        characterHistoryBranch.begin(document);
        resetChanged(document.getMesh());
    }

    private void finishChange(CharacterMeshDocumentInstance document) {

        for (int i = 0; i < document.getSelection().size(); i++)
            if (document.isSelected(i))
                changedVertices.set(i, true);

        mirrorChanged(document);
        characterMeshBranch.finishEdit(document);
    }

    private void resetChanged(RiggedMeshStruct mesh) {

        changedVertices.clear();
        changedVertices.size(mesh.getVertexCount());
    }

    // Hands every changed vertex's weights to its mirror, when mirror is on and the mirror did not change itself
    private void mirrorChanged(CharacterMeshDocumentInstance document) {

        if (!characterEditorManager.isMirror())
            return;

        RiggedMeshStruct mesh = document.getMesh();
        IntArrayList mirrorVertices = characterSelectionBranch.resolveMirror(document);
        int[] mirrorBones = resolveMirrorBones(mesh.getRigHandle());

        for (int i = 0; i < changedVertices.size(); i++) {

            int mirror = mirrorVertices.getInt(i);

            if (changedVertices.getBoolean(i) && mirror != EngineSetting.INDEX_NOT_FOUND && mirror != i
                    && !changedVertices.getBoolean(mirror))
                copyMirrored(mesh, i, mirror, mirrorBones);
        }
    }
}
