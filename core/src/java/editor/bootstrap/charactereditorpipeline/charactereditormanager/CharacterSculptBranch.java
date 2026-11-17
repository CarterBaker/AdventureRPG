package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorShape;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorTransform;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

class CharacterSculptBranch extends BranchPackage {

    /*
     * Performs every change to the geometry of a character mesh. A
     * transform holds the selection's start and moves, turns or scales it
     * from there each time it is updated; held on a posed mesh it works on
     * the skinned cage as the view shows it and writes each vertex back to
     * the bind pose through the inverse of its blended skinning matrix (as
     * Maya and Blender edit a skinned mesh on its cage), each vertex as far as it is
     * selected, so a soft brush's selection fades out at its edge, easing
     * unselected vertices within the proportional radius along by their
     * distance; with mirror on, a vertex whose mirror moves too follows it
     * from the negative side, an unmoved mirror is placed opposite, and a
     * vertex on the centre plane stays on it. Topology edits — extrude,
     * duplicate, delete, make face, merge, subdivide, symmetrize and new
     * parts — each become one undo step, and with mirror on the first three
     * take the selection's mirror along.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterSelectionBranch characterSelectionBranch;
    private CharacterWeightBranch characterWeightBranch;

    // Transform
    private CharacterMeshDocumentInstance transformDocument;
    private IntArrayList transformVertices;
    private FloatArrayList transformStarts;
    private FloatArrayList transformFactors;
    private IntArrayList transformMirrors;
    private BooleanArrayList transformFollowers;
    private IntArrayList vertex2Slot;
    private Vector3 transformPivot;
    private FloatArrayList transformInverses;
    private Matrix4[] transformSkinning;

    // Scratch
    private Vector3 pointScratch;
    private Vector3 normalScratch;
    private Vector3 minScratch;
    private Vector3 maxScratch;
    private IntArrayList vertexRemap;
    private IntArrayList vertexList;
    private IntArrayList twinList;
    private BooleanArrayList faceFlags;
    private BooleanArrayList vertexFlags;
    private BooleanArrayList selectionScratch;
    private FloatArrayList selectionWeightScratch;
    private BooleanArrayList hiddenScratch;
    private Long2IntOpenHashMap edge2Value;
    private float[] cornerScratch;
    private Matrix4 blendScratch;

    // Base \\

    @Override
    protected void create() {

        // Transform
        this.transformVertices = new IntArrayList();
        this.transformStarts = new FloatArrayList();
        this.transformFactors = new FloatArrayList();
        this.transformMirrors = new IntArrayList();
        this.transformFollowers = new BooleanArrayList();
        this.vertex2Slot = new IntArrayList();
        this.transformPivot = new Vector3();
        this.transformInverses = new FloatArrayList();

        // Scratch
        this.pointScratch = new Vector3();
        this.normalScratch = new Vector3();
        this.minScratch = new Vector3();
        this.maxScratch = new Vector3();
        this.vertexRemap = new IntArrayList();
        this.vertexList = new IntArrayList();
        this.twinList = new IntArrayList();
        this.faceFlags = new BooleanArrayList();
        this.vertexFlags = new BooleanArrayList();
        this.selectionScratch = new BooleanArrayList();
        this.selectionWeightScratch = new FloatArrayList();
        this.hiddenScratch = new BooleanArrayList();
        this.edge2Value = new Long2IntOpenHashMap();
        this.edge2Value.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.cornerScratch = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.RIGGED_MESH_POSITION_FLOATS];
        this.blendScratch = new Matrix4();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterSelectionBranch = get(CharacterSelectionBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
    }

    // Transform \\

    // Holds the selection's start for a transform, on the cage the skinning matrices pose when given and in the bind
    // pose otherwise; false when nothing is selected or a transform is already held
    boolean beginTransform(CharacterMeshDocumentInstance document, Matrix4[] skinningMatrices) {

        if (transformDocument != null || !characterSelectionBranch.resolveCenter(document, transformPivot))
            return false;

        RiggedMeshStruct mesh = document.getMesh();
        int vertexCount = mesh.getVertexCount();

        this.transformSkinning = skinningMatrices;
        characterHistoryBranch.begin(document);
        transformVertices.clear();
        transformStarts.clear();
        transformFactors.clear();
        transformMirrors.clear();
        transformFollowers.clear();
        transformInverses.clear();
        fill(vertex2Slot, vertexCount, EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < vertexCount; i++)
            if (document.isSelected(i))
                holdVertex(mesh, i, document.getSelectionWeight(i));

        if (skinningMatrices != null)
            resolvePosedPivot();

        if (characterEditorManager.isProportional())
            holdProportional(document);

        resolveTransformMirrors(document);
        this.transformDocument = document;

        return true;
    }

    // A vertex held where it starts, posed by its blended skinning matrix when the transform works on the posed cage,
    // its inverse kept to write the moved point back to the bind pose
    private void holdVertex(RiggedMeshStruct mesh, int vertexIndex, float factor) {

        vertex2Slot.set(vertexIndex, transformVertices.size());
        transformVertices.add(vertexIndex);
        transformFactors.add(factor);

        if (transformSkinning == null) {
            transformStarts.add(mesh.getX(vertexIndex));
            transformStarts.add(mesh.getY(vertexIndex));
            transformStarts.add(mesh.getZ(vertexIndex));
            return;
        }

        resolveBlend(mesh, vertexIndex, blendScratch);
        CharacterPoseUtility.transformPoint(
                blendScratch, mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex), pointScratch);
        transformStarts.add(pointScratch.x);
        transformStarts.add(pointScratch.y);
        transformStarts.add(pointScratch.z);
        blendScratch.inverse();

        for (int i = 0; i < blendScratch.val.length; i++)
            transformInverses.add(blendScratch.val[i]);
    }

    // B = Σ wᵢ Mᵢ, a vertex's skinning matrices blended by its weights: affine under linear blend skinning, so its
    // inverse carries a posed point back to the bind pose exactly
    private void resolveBlend(RiggedMeshStruct mesh, int vertexIndex, Matrix4 out) {

        int influenceCount = mesh.getInfluenceCount(vertexIndex);

        if (influenceCount == 0) {
            out.set(1f);
            return;
        }

        Arrays.fill(out.val, 0f);

        for (int i = 0; i < influenceCount; i++) {

            float[] m = transformSkinning[mesh.getInfluenceBone(vertexIndex, i)].val;
            float w = mesh.getInfluenceWeight(vertexIndex, i);

            for (int k = 0; k < out.val.length; k++)
                out.val[k] += m[k] * w;
        }

        out.val[3] = 0f;
        out.val[7] = 0f;
        out.val[11] = 0f;
        out.val[15] = 1f;
    }

    // The pivot the posed selection turns and scales about: its weighted centre on the posed cage
    private void resolvePosedPivot() {

        float total = 0f;

        transformPivot.set(0f, 0f, 0f);

        for (int s = 0; s < transformVertices.size(); s++) {

            int base = s * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
            float weight = transformFactors.getFloat(s);

            transformPivot.add(
                    transformStarts.getFloat(base) * weight,
                    transformStarts.getFloat(base + 1) * weight,
                    transformStarts.getFloat(base + 2) * weight);
            total += weight;
        }

        if (total > 0f)
            transformPivot.divide(total);
    }

    // Where a vertex stands on the cage the transform works on
    private void resolveStart(RiggedMeshStruct mesh, int vertexIndex, Vector3 out) {

        if (transformSkinning == null) {
            out.set(mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex));
            return;
        }

        resolveBlend(mesh, vertexIndex, blendScratch);
        CharacterPoseUtility.transformPoint(
                blendScratch, mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex), out);
    }

    // Unselected vertices within the radius of the nearest selected one, eased by their distance to it
    private void holdProportional(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        float radius = characterEditorManager.getProportionalRadius();
        int selectedCount = transformVertices.size();

        for (int i = 0; i < mesh.getVertexCount(); i++) {

            if (document.isSelected(i))
                continue;

            float nearest = Float.MAX_VALUE;

            resolveStart(mesh, i, normalScratch);

            for (int s = 0; s < selectedCount; s++) {

                int base = s * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
                float dx = normalScratch.x - transformStarts.getFloat(base);
                float dy = normalScratch.y - transformStarts.getFloat(base + 1);
                float dz = normalScratch.z - transformStarts.getFloat(base + 2);

                nearest = Math.min(nearest, dx * dx + dy * dy + dz * dz);
            }

            float factor = CharacterMeshUtility.falloff((float) Math.sqrt(nearest), radius)
                    * characterEditorManager.getRegionShare(document, characterEditorManager.getRegion(), i);

            if (factor > 0f)
                holdVertex(mesh, i, factor);
        }
    }

    // A held vertex on the negative side whose mirror is held too follows it rather than moving itself
    private void resolveTransformMirrors(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        IntArrayList mirrorVertices = characterEditorManager.isMirror()
                ? characterSelectionBranch.resolveMirror(document)
                : null;

        for (int s = 0; s < transformVertices.size(); s++) {

            int vertexIndex = transformVertices.getInt(s);
            int mirror = mirrorVertices != null ? mirrorVertices.getInt(vertexIndex) : EngineSetting.INDEX_NOT_FOUND;

            transformMirrors.add(mirror);
            transformFollowers.add(mirror != EngineSetting.INDEX_NOT_FOUND
                    && mirror != vertexIndex
                    && vertex2Slot.getInt(mirror) != EngineSetting.INDEX_NOT_FOUND
                    && mesh.getX(vertexIndex) < 0f);
        }
    }

    // Moves every held vertex from its start: Grab by the vector, Rotate about the vector as an axis by the amount in
    // radians, Scale by the vector's factors on each axis
    void updateTransform(CharacterEditorTransform transform, Vector3 vector, float amount) {

        if (transformDocument == null)
            return;

        RiggedMeshStruct mesh = transformDocument.getMesh();

        for (int s = 0; s < transformVertices.size(); s++)
            if (!transformFollowers.getBoolean(s))
                placeHeldVertex(mesh, s, transform, vector, amount);

        for (int s = 0; s < transformVertices.size(); s++)
            placeMirror(mesh, s);

        characterMeshBranch.preview(transformDocument);
    }

    private void placeHeldVertex(
            RiggedMeshStruct mesh,
            int slot,
            CharacterEditorTransform transform,
            Vector3 vector,
            float amount) {

        int base = slot * EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        float x = transformStarts.getFloat(base);
        float y = transformStarts.getFloat(base + 1);
        float z = transformStarts.getFloat(base + 2);

        switch (transform) {
            case GRAB -> pointScratch.set(x + vector.x, y + vector.y, z + vector.z);
            case ROTATE -> CharacterMeshUtility.rotate(x, y, z, transformPivot, vector, amount, pointScratch);
            case SCALE -> pointScratch.set(
                    transformPivot.x + (x - transformPivot.x) * vector.x,
                    transformPivot.y + (y - transformPivot.y) * vector.y,
                    transformPivot.z + (z - transformPivot.z) * vector.z);
        }

        float factor = transformFactors.getFloat(slot);

        pointScratch.set(
                x + (pointScratch.x - x) * factor,
                y + (pointScratch.y - y) * factor,
                z + (pointScratch.z - z) * factor);

        if (transformSkinning != null)
            toBindPose(slot, pointScratch);

        CharacterMeshUtility.setRoundedPosition(
                mesh, transformVertices.getInt(slot), pointScratch.x, pointScratch.y, pointScratch.z);
    }

    // A point on the posed cage carried back to the bind pose through its vertex's inverse blended matrix
    private void toBindPose(int slot, Vector3 point) {

        int base = slot * blendScratch.val.length;

        for (int i = 0; i < blendScratch.val.length; i++)
            blendScratch.val[i] = transformInverses.getFloat(base + i);

        CharacterPoseUtility.transformPoint(blendScratch, point.x, point.y, point.z, point);
    }

    // Keeps a held vertex's mirror opposite it, and a vertex on the centre plane on it
    private void placeMirror(RiggedMeshStruct mesh, int slot) {

        int vertexIndex = transformVertices.getInt(slot);
        int mirror = transformMirrors.getInt(slot);

        if (mirror == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (mirror == vertexIndex) {
            mesh.setPosition(vertexIndex, 0f, mesh.getY(vertexIndex), mesh.getZ(vertexIndex));
            return;
        }

        if (transformFollowers.getBoolean(slot))
            mesh.setPosition(vertexIndex, -mesh.getX(mirror), mesh.getY(mirror), mesh.getZ(mirror));
        else if (vertex2Slot.getInt(mirror) == EngineSetting.INDEX_NOT_FOUND)
            mesh.setPosition(mirror, -mesh.getX(vertexIndex), mesh.getY(vertexIndex), mesh.getZ(vertexIndex));
    }

    void confirmTransform() {

        CharacterMeshDocumentInstance document = transformDocument;

        if (document == null)
            return;

        this.transformDocument = null;
        characterMeshBranch.finishEdit(document);
    }

    void cancelTransform() {

        CharacterMeshDocumentInstance document = transformDocument;

        if (document == null)
            return;

        this.transformDocument = null;
        characterHistoryBranch.cancel(document);
        characterMeshBranch.upload(document);
        characterEditorManager.notifyChanged();
    }

    boolean isTransforming() {
        return transformDocument != null;
    }

    Vector3 getTransformPivot() {
        return transformPivot;
    }

    // Numeric \\

    // Moves the selection so its centre stands at a value on one axis
    void moveSelection(CharacterMeshDocumentInstance document, int axis, float value) {

        if (!characterSelectionBranch.resolveCenter(document, pointScratch))
            return;

        float delta = value - getAxis(pointScratch, axis);

        normalScratch.set(axis == 0 ? delta : 0f, axis == 1 ? delta : 0f, axis == 2 ? delta : 0f);
        runTransform(document, CharacterEditorTransform.GRAB, normalScratch, 0f);
    }

    // Scales the selection about its centre so it spans a size on one axis
    void resizeSelection(CharacterMeshDocumentInstance document, int axis, float size) {

        if (!characterSelectionBranch.resolveBounds(document, minScratch, maxScratch))
            return;

        float span = getAxis(maxScratch, axis) - getAxis(minScratch, axis);

        if (span <= EditorSetting.CHARACTER_MESH_SPAN_EPSILON || size <= 0f) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_FLAT_SELECTION);
            return;
        }

        float factor = size / span;

        normalScratch.set(axis == 0 ? factor : 1f, axis == 1 ? factor : 1f, axis == 2 ? factor : 1f);
        runTransform(document, CharacterEditorTransform.SCALE, normalScratch, 0f);
    }

    void rotateSelection(CharacterMeshDocumentInstance document, int axis, float degrees) {

        normalScratch.set(axis == 0 ? 1f : 0f, axis == 1 ? 1f : 0f, axis == 2 ? 1f : 0f);
        runTransform(document, CharacterEditorTransform.ROTATE, normalScratch, (float) Math.toRadians(degrees));
    }

    private void runTransform(
            CharacterMeshDocumentInstance document,
            CharacterEditorTransform transform,
            Vector3 vector,
            float amount) {

        if (!beginTransform(document, null))
            return;

        updateTransform(transform, vector, amount);
        confirmTransform();
    }

    // Extrude \\

    // Extrudes the selected faces as a region, or the selected edges when no face is wholly selected, selecting what
    // was pulled out; the normal receives the region's average facing, zero for edges. False when nothing extrudes.
    boolean extrude(CharacterMeshDocumentInstance document, Vector3 normal) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        characterHistoryBranch.begin(document);
        normal.set(0f, 0f, 0f);
        fill(vertexRemap, mesh.getVertexCount(), EngineSetting.INDEX_NOT_FOUND);

        boolean extruded = flagSelectedFaces(document) > 0
                ? extrudeFaces(document, normal)
                : extrudeEdges(document);

        if (!extruded) {
            characterHistoryBranch.cancel(document);
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NOTHING_TO_EXTRUDE);
            return false;
        }

        selectRemapped(document);
        removeOrphans(document);

        return characterMeshBranch.finishEdit(document);
    }

    private boolean extrudeFaces(CharacterMeshDocumentInstance document, Vector3 normal) {

        RiggedMeshStruct mesh = document.getMesh();
        int faceCount = mesh.getFaceCount();

        edge2Value.clear();

        for (int f = 0; f < faceCount; f++)
            if (faceFlags.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int a = mesh.getFaceVertex(f, c);
                    int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                    if (a != b)
                        countEdge(toEdgeKey(a, b));

                    if (vertexRemap.getInt(a) == EngineSetting.INDEX_NOT_FOUND)
                        vertexRemap.set(a, mesh.duplicateVertex(a));
                }

        for (int f = 0; f < faceCount; f++)
            if (faceFlags.getBoolean(f)) {

                addFaceNormal(mesh, f, normal);

                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                    int a = mesh.getFaceVertex(f, c);
                    int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                    if (a != b && edge2Value.get(toEdgeKey(a, b)) == 1)
                        mesh.addFace(a, b, vertexRemap.getInt(b), vertexRemap.getInt(a), mesh.getFaceTexture(f));
                }

                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    mesh.setFaceVertex(f, c, vertexRemap.getInt(mesh.getFaceVertex(f, c)));
            }

        if (characterEditorManager.isMirror() && touchesCentre(mesh, faceCount))
            normal.x = 0f;

        float length = normal.length();

        if (length > 0f)
            normal.divide(length);

        return true;
    }

    // Each edge whose two ends are selected, pulled out once into a new quad
    private boolean extrudeEdges(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int faceCount = mesh.getFaceCount();
        boolean extruded = false;

        edge2Value.clear();

        for (int f = 0; f < faceCount; f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);
                long edgeKey = toEdgeKey(a, b);

                if (a == b || !document.isSelected(a) || !document.isSelected(b) || edge2Value.containsKey(edgeKey))
                    continue;

                edge2Value.put(edgeKey, f);
                remapDuplicate(mesh, a);
                remapDuplicate(mesh, b);
                mesh.addFace(a, b, vertexRemap.getInt(b), vertexRemap.getInt(a), mesh.getFaceTexture(f));
                extruded = true;
            }

        return extruded;
    }

    // One key for an edge whichever way round its ends are named
    private long toEdgeKey(int a, int b) {
        return (long) Math.min(a, b) << Integer.SIZE | Math.max(a, b);
    }

    // How many selected faces hold an edge; one that only a single face holds borders the region
    private void countEdge(long edgeKey) {

        int count = edge2Value.get(edgeKey);
        edge2Value.put(edgeKey, count == EngineSetting.INDEX_NOT_FOUND ? 1 : count + 1);
    }

    // Whether a flagged face reaches the centre plane, so the region and its mirror are one region facing along it
    private boolean touchesCentre(RiggedMeshStruct mesh, int faceCount) {

        for (int f = 0; f < faceCount; f++)
            if (faceFlags.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    if (Math.abs(mesh.getX(mesh.getFaceVertex(f, c))) <= EditorSetting.CHARACTER_MESH_CENTER_TOLERANCE)
                        return true;

        return false;
    }

    private void addFaceNormal(RiggedMeshStruct mesh, int faceIndex, Vector3 normal) {

        if (characterEditorManager.isMirror() && resolveFaceCenterX(mesh, faceIndex) < 0f)
            return;

        resolveFaceNormal(mesh, faceIndex, pointScratch);
        normal.add(pointScratch);
    }

    // Duplicate \\

    // Copies the selected faces onto new vertices of their own, selecting the copy
    boolean duplicate(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        int faceCount = mesh.getFaceCount();

        if (flagSelectedFaces(document) == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return false;
        }

        characterHistoryBranch.begin(document);
        fill(vertexRemap, mesh.getVertexCount(), EngineSetting.INDEX_NOT_FOUND);

        for (int f = 0; f < faceCount; f++) {

            if (!faceFlags.getBoolean(f))
                continue;

            int copy = mesh.duplicateFace(f);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                mesh.setFaceVertex(copy, c, remapDuplicate(mesh, mesh.getFaceVertex(f, c)));
        }

        selectRemapped(document);
        return characterMeshBranch.finishEdit(document);
    }

    private int remapDuplicate(RiggedMeshStruct mesh, int vertexIndex) {

        if (vertexRemap.getInt(vertexIndex) == EngineSetting.INDEX_NOT_FOUND)
            vertexRemap.set(vertexIndex, mesh.duplicateVertex(vertexIndex));

        return vertexRemap.getInt(vertexIndex);
    }

    // Delete \\

    // Removes the selected vertices and every face standing on any of them
    void deleteVertices(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterSelectionBranch.getSelectedCount(document) == 0)
            return;

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        characterHistoryBranch.begin(document);
        faceFlags.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            faceFlags.add(touchesSelection(document, f));

        mesh.removeFaces(faceFlags);
        removeOrphans(document);
        characterMeshBranch.finishEdit(document);
    }

    // Removes the wholly selected faces and any vertex they leave standing on nothing
    void deleteFaces(CharacterMeshDocumentInstance document) {

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        if (flagSelectedFaces(document) == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return;
        }

        characterHistoryBranch.begin(document);
        document.getMesh().removeFaces(faceFlags);
        removeOrphans(document);
        characterMeshBranch.finishEdit(document);
    }

    // Make Face \\

    // A quad over four selected vertices, or a triangle over three, its corners ordered around their centre; with
    // mirror on, the twin over their twins as well, wound back (a quad's corners 0, 3, 2, 1, a triangle's 0, 2, 1, 1)
    void makeFace(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectSelected(document, vertexList);

        if (vertexList.size() < EngineSetting.QUAD_VERTEX_COUNT - 1
                || vertexList.size() > EngineSetting.QUAD_VERTEX_COUNT) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_FACE_VERTICES);
            return;
        }

        if (!resolvePolygonNormal(mesh, vertexList, normalScratch)) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_FACE_FLAT);
            return;
        }

        characterSelectionBranch.resolveCenter(document, pointScratch);
        orderAround(mesh, vertexList, pointScratch, normalScratch);
        characterHistoryBranch.begin(document);

        int last = vertexList.getInt(vertexList.size() - 1);
        int texture = resolveNeighbourTexture(mesh, vertexList);

        mesh.addFace(vertexList.getInt(0), vertexList.getInt(1), vertexList.getInt(2), last, texture);

        if (characterEditorManager.isMirror() && collectTwins(document, vertexList) == 0)
            mesh.addFace(
                    twinList.getInt(0),
                    twinList.getInt(twinList.size() - 1),
                    twinList.getInt(twinList.size() - 2),
                    twinList.getInt(1),
                    texture);

        characterMeshBranch.finishEdit(document);
    }

    // The twin of every vertex listed, in order, into the twin list; how many of the twins are among the vertices
    // listed (zero for a set apart from its mirror), or INDEX_NOT_FOUND when one has no twin
    private int collectTwins(CharacterMeshDocumentInstance document, IntArrayList vertices) {

        IntArrayList mirrorVertices = characterSelectionBranch.resolveMirror(document);
        int shared = 0;

        twinList.clear();

        for (int i = 0; i < vertices.size(); i++) {

            int twin = mirrorVertices.getInt(vertices.getInt(i));

            if (twin == EngineSetting.INDEX_NOT_FOUND)
                return EngineSetting.INDEX_NOT_FOUND;

            if (vertices.contains(twin))
                shared++;

            twinList.add(twin);
        }

        return shared;
    }

    // The largest cross of the corners' spokes from the first; false when they lie on one line
    private boolean resolvePolygonNormal(RiggedMeshStruct mesh, IntArrayList vertices, Vector3 normal) {

        int origin = vertices.getInt(0);
        float best = 0f;

        for (int a = 1; a < vertices.size(); a++)
            for (int b = a + 1; b < vertices.size(); b++) {

                int va = vertices.getInt(a);
                int vb = vertices.getInt(b);
                float ax = mesh.getX(va) - mesh.getX(origin);
                float ay = mesh.getY(va) - mesh.getY(origin);
                float az = mesh.getZ(va) - mesh.getZ(origin);
                float bx = mesh.getX(vb) - mesh.getX(origin);
                float by = mesh.getY(vb) - mesh.getY(origin);
                float bz = mesh.getZ(vb) - mesh.getZ(origin);
                float nx = ay * bz - az * by;
                float ny = az * bx - ax * bz;
                float nz = ax * by - ay * bx;
                float length = nx * nx + ny * ny + nz * nz;

                if (length > best) {
                    best = length;
                    normal.set(nx, ny, nz);
                }
            }

        if (best <= EditorSetting.CHARACTER_MESH_SPAN_EPSILON * EditorSetting.CHARACTER_MESH_SPAN_EPSILON)
            return false;

        normal.normalize();
        return true;
    }

    // Sorts the vertices by their angle about the normal around the centre
    private void orderAround(RiggedMeshStruct mesh, IntArrayList vertices, Vector3 center, Vector3 normal) {

        int first = vertices.getInt(0);
        float ux = mesh.getX(first) - center.x;
        float uy = mesh.getY(first) - center.y;
        float uz = mesh.getZ(first) - center.z;
        float wx = normal.y * uz - normal.z * uy;
        float wy = normal.z * ux - normal.x * uz;
        float wz = normal.x * uy - normal.y * ux;

        for (int i = 1; i < vertices.size(); i++)
            for (int j = i; j > 0 && resolveAngle(mesh, vertices.getInt(j), center, ux, uy, uz, wx, wy, wz)
                    < resolveAngle(mesh, vertices.getInt(j - 1), center, ux, uy, uz, wx, wy, wz); j--) {

                int swap = vertices.getInt(j);
                vertices.set(j, vertices.getInt(j - 1));
                vertices.set(j - 1, swap);
            }
    }

    private float resolveAngle(
            RiggedMeshStruct mesh,
            int vertexIndex,
            Vector3 center,
            float ux,
            float uy,
            float uz,
            float wx,
            float wy,
            float wz) {

        float dx = mesh.getX(vertexIndex) - center.x;
        float dy = mesh.getY(vertexIndex) - center.y;
        float dz = mesh.getZ(vertexIndex) - center.z;

        return (float) Math.atan2(dx * wx + dy * wy + dz * wz, dx * ux + dy * uy + dz * uz);
    }

    // The texture of a face already standing on any of the vertices, or the mesh's first
    private int resolveNeighbourTexture(RiggedMeshStruct mesh, IntArrayList vertices) {

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int i = 0; i < vertices.size(); i++)
                if (mesh.usesVertex(f, vertices.getInt(i)))
                    return mesh.getFaceTexture(f);

        return 0;
    }

    // Merge \\

    // Joins the selected vertices into one at their centre carrying their averaged weights; faces left with fewer
    // than three corners go. With mirror on, their twins join at the mirrored point with the mirrored weights, or,
    // where the two sets meet, both join as one on the centre plane
    void merge(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        collectSelected(document, vertexList);

        if (vertexList.size() < 2) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_MERGE_VERTICES);
            return;
        }

        int target = vertexList.getInt(0);
        int shared = characterEditorManager.isMirror()
                ? collectTwins(document, vertexList)
                : EngineSetting.INDEX_NOT_FOUND;
        int[] mirrorBones = characterWeightBranch.resolveMirrorBones(mesh.getRigHandle());

        characterHistoryBranch.begin(document);
        characterSelectionBranch.resolveCenter(document, pointScratch);
        fill(vertexRemap, mesh.getVertexCount(), EngineSetting.INDEX_NOT_FOUND);

        if (shared > 0) {

            for (int i = 0; i < twinList.size(); i++)
                if (!vertexList.contains(twinList.getInt(i)))
                    vertexList.add(twinList.getInt(i));

            mergeInto(mesh, vertexList, target, 0f, pointScratch.y, pointScratch.z);
            characterWeightBranch.symmetrizeWeights(mesh, target, mirrorBones);
        } else {

            mergeInto(mesh, vertexList, target, pointScratch.x, pointScratch.y, pointScratch.z);

            if (shared == 0) {
                mergeInto(mesh, twinList, twinList.getInt(0), -pointScratch.x, pointScratch.y, pointScratch.z);
                characterWeightBranch.copyMirrored(mesh, target, twinList.getInt(0), mirrorBones);
            }
        }

        faceFlags.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int merged = vertexRemap.getInt(mesh.getFaceVertex(f, c));

                if (merged != EngineSetting.INDEX_NOT_FOUND)
                    mesh.setFaceVertex(f, c, merged);
            }

            faceFlags.add(countDistinctCorners(mesh, f) < EngineSetting.QUAD_VERTEX_COUNT - 1);
        }

        mesh.removeFaces(faceFlags);

        for (int i = 0; i < document.getSelection().size(); i++)
            document.getSelection().set(i, vertexRemap.getInt(i) == i);

        document.getSelectionWeights().set(target, 1f);

        if (shared == 0)
            document.getSelectionWeights().set(twinList.getInt(0), 1f);

        document.setActiveVertex(target);
        removeOrphans(document);
        characterMeshBranch.finishEdit(document);
    }

    // The vertices listed merged into one at a point, with their averaged weights; each vertex is mapped to it
    private void mergeInto(RiggedMeshStruct mesh, IntArrayList vertices, int target, float x, float y, float z) {

        characterWeightBranch.average(mesh, vertices, target);
        CharacterMeshUtility.setRoundedPosition(mesh, target, x, y, z);

        for (int i = 0; i < vertices.size(); i++)
            vertexRemap.set(vertices.getInt(i), target);
    }

    private int countDistinctCorners(RiggedMeshStruct mesh, int faceIndex) {

        int distinct = 0;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            boolean repeated = false;

            for (int p = 0; p < c; p++)
                repeated |= mesh.getFaceVertex(faceIndex, p) == mesh.getFaceVertex(faceIndex, c);

            if (!repeated)
                distinct++;
        }

        return distinct;
    }

    // Subdivide \\

    // Splits each selected face into four through its edges' midpoints and its centre, sharing every midpoint with the
    // selected face across the edge; new vertices carry averaged weights and the faces their UVs interpolated
    void subdivide(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        int faceCount = mesh.getFaceCount();

        if (flagSelectedFaces(document) == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return;
        }

        characterHistoryBranch.begin(document);
        edge2Value.clear();

        for (int f = 0; f < faceCount; f++)
            if (faceFlags.getBoolean(f))
                subdivideFace(mesh, f);

        document.syncSelection();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (f >= faceCount || faceFlags.getBoolean(f))
                for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                    document.getSelection().set(mesh.getFaceVertex(f, c), true);

        characterMeshBranch.finishEdit(document);
    }

    private void subdivideFace(RiggedMeshStruct mesh, int faceIndex) {

        int v0 = mesh.getFaceVertex(faceIndex, 0);
        int v1 = mesh.getFaceVertex(faceIndex, 1);
        int v2 = mesh.getFaceVertex(faceIndex, 2);
        int v3 = mesh.getFaceVertex(faceIndex, 3);
        int m01 = resolveMidpoint(mesh, v0, v1);
        int m12 = resolveMidpoint(mesh, v1, v2);
        int m23 = resolveMidpoint(mesh, v2, v3);
        int m30 = resolveMidpoint(mesh, v3, v0);
        int center = resolveCenterVertex(mesh, faceIndex);
        int texture = mesh.getFaceTexture(faceIndex);

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {
            cornerScratch[c * 2] = mesh.getFaceU(faceIndex, c);
            cornerScratch[c * 2 + 1] = mesh.getFaceV(faceIndex, c);
        }

        int f1 = mesh.addFace(m01, v1, m12, center, texture);
        int f2 = mesh.addFace(center, m12, v2, m23, texture);
        int f3 = mesh.addFace(m30, center, m23, v3, texture);

        mesh.setFaceVertex(faceIndex, 1, m01);
        mesh.setFaceVertex(faceIndex, 2, center);
        mesh.setFaceVertex(faceIndex, 3, m30);

        writeSubUVs(mesh, faceIndex, 0f, 0f, 0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f);
        writeSubUVs(mesh, f1, 0.5f, 0f, 1f, 0f, 1f, 0.5f, 0.5f, 0.5f);
        writeSubUVs(mesh, f2, 0.5f, 0.5f, 1f, 0.5f, 1f, 1f, 0.5f, 1f);
        writeSubUVs(mesh, f3, 0f, 0.5f, 0.5f, 0.5f, 0.5f, 1f, 0f, 1f);
    }

    // A sub-face's corner UVs, each interpolated across the parent's corners at a place given in the parent's own
    // corner space — from corner 0 along corner 1, and from there toward corners 3 and 2
    private void writeSubUVs(
            RiggedMeshStruct mesh,
            int faceIndex,
            float s0,
            float t0,
            float s1,
            float t1,
            float s2,
            float t2,
            float s3,
            float t3) {

        writeSubUV(mesh, faceIndex, 0, s0, t0);
        writeSubUV(mesh, faceIndex, 1, s1, t1);
        writeSubUV(mesh, faceIndex, 2, s2, t2);
        writeSubUV(mesh, faceIndex, 3, s3, t3);
    }

    private void writeSubUV(RiggedMeshStruct mesh, int faceIndex, int corner, float s, float t) {

        float u = (cornerScratch[0] * (1f - s) + cornerScratch[2] * s) * (1f - t)
                + (cornerScratch[6] * (1f - s) + cornerScratch[4] * s) * t;
        float v = (cornerScratch[1] * (1f - s) + cornerScratch[3] * s) * (1f - t)
                + (cornerScratch[7] * (1f - s) + cornerScratch[5] * s) * t;

        mesh.setFaceUV(faceIndex, corner, CharacterViewUtility.round(u), CharacterViewUtility.round(v));
    }

    private int resolveMidpoint(RiggedMeshStruct mesh, int a, int b) {

        if (a == b)
            return a;

        long edgeKey = toEdgeKey(a, b);
        int midpoint = edge2Value.get(edgeKey);

        if (midpoint != EngineSetting.INDEX_NOT_FOUND)
            return midpoint;

        midpoint = mesh.addVertex(0f, 0f, 0f);
        CharacterMeshUtility.setRoundedPosition(
                mesh,
                midpoint,
                (mesh.getX(a) + mesh.getX(b)) * 0.5f,
                (mesh.getY(a) + mesh.getY(b)) * 0.5f,
                (mesh.getZ(a) + mesh.getZ(b)) * 0.5f);

        vertexList.clear();
        vertexList.add(a);
        vertexList.add(b);
        characterWeightBranch.average(mesh, vertexList, midpoint);
        edge2Value.put(edgeKey, midpoint);

        return midpoint;
    }

    // A new vertex at a face's centre, summed in double so a face and its mirror (corners the other way round) find
    // the same point, with the corners' averaged weights
    private int resolveCenterVertex(RiggedMeshStruct mesh, int faceIndex) {

        int center = mesh.addVertex(0f, 0f, 0f);
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        vertexList.clear();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int corner = mesh.getFaceVertex(faceIndex, c);

            x += mesh.getX(corner);
            y += mesh.getY(corner);
            z += mesh.getZ(corner);
            vertexList.add(corner);
        }

        double share = 1.0 / EngineSetting.QUAD_VERTEX_COUNT;

        CharacterMeshUtility.setRoundedPosition(
                mesh,
                center,
                (float) (x * share),
                (float) (y * share),
                (float) (z * share));
        characterWeightBranch.average(mesh, vertexList, center);

        return center;
    }

    // Parts \\

    // A new shape at a bone's joint in the bind pose, weighted wholly to it, and with mirror on its twin at the
    // mirrored bone's; the new vertices become the selection
    void addShape(
            CharacterMeshDocumentInstance document,
            CharacterEditorShape shape,
            float size,
            int boneIndex,
            String textureName) {

        RiggedMeshStruct mesh = document.getMesh();
        RigHandle rigHandle = mesh.getRigHandle();
        int firstVertex = mesh.getVertexCount();

        characterHistoryBranch.begin(document);

        int textureIndex = mesh.resolveTexture(textureName);
        addShapeAtJoint(mesh, shape, size, boneIndex, textureIndex);

        int mirrorBone = characterWeightBranch.resolveMirrorBone(rigHandle, boneIndex);

        if (characterEditorManager.isMirror() && mirrorBone != boneIndex)
            addShapeAtJoint(mesh, shape, size, mirrorBone, textureIndex);

        document.syncSelection();

        for (int i = 0; i < document.getSelection().size(); i++)
            document.getSelection().set(i, i >= firstVertex);

        characterMeshBranch.finishEdit(document);
    }

    private void addShapeAtJoint(
            RiggedMeshStruct mesh,
            CharacterEditorShape shape,
            float size,
            int boneIndex,
            int textureIndex) {

        Matrix4 joint = mesh.getRigHandle().getBindWorldMatrix(boneIndex);

        pointScratch.set(joint.getM03(), joint.getM13(), joint.getM23());
        CharacterMeshUtility.addShape(mesh, shape, pointScratch, size, boneIndex, textureIndex);
    }

    // Textures \\

    void setFaceTexture(CharacterMeshDocumentInstance document, String textureName) {

        RiggedMeshStruct mesh = document.getMesh();

        if (characterEditorManager.isMirror())
            characterSelectionBranch.extendMirror(document);

        if (flagSelectedFaces(document) == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_EDITOR_MESSAGE_NO_FACES);
            return;
        }

        characterHistoryBranch.begin(document);

        int textureIndex = mesh.resolveTexture(textureName);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (faceFlags.getBoolean(f))
                mesh.setFaceTexture(f, textureIndex);

        characterMeshBranch.finishEdit(document);
    }

    // The texture of the first wholly selected face, or null when none is
    String findSelectedFaceTexture(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();

        for (int f = 0; f < mesh.getFaceCount(); f++)
            if (CharacterMeshUtility.isFaceSelected(mesh, f, document.getSelection()))
                return mesh.getTextureName(mesh.getFaceTexture(f));

        return null;
    }

    // Faces \\

    // Flags each face whose every corner is selected, returning how many are
    private int flagSelectedFaces(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        int count = 0;

        faceFlags.clear();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            boolean selected = CharacterMeshUtility.isFaceSelected(mesh, f, document.getSelection());

            faceFlags.add(selected);

            if (selected)
                count++;
        }

        return count;
    }

    private boolean touchesSelection(CharacterMeshDocumentInstance document, int faceIndex) {

        RiggedMeshStruct mesh = document.getMesh();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (document.isSelected(mesh.getFaceVertex(faceIndex, c)))
                return true;

        return false;
    }

    // Each face's diagonals crossed
    private void resolveFaceNormal(RiggedMeshStruct mesh, int faceIndex, Vector3 normal) {

        int c0 = mesh.getFaceVertex(faceIndex, 0);
        int c1 = mesh.getFaceVertex(faceIndex, 1);
        int c2 = mesh.getFaceVertex(faceIndex, 2);
        int c3 = mesh.getFaceVertex(faceIndex, 3);
        float ax = mesh.getX(c2) - mesh.getX(c0);
        float ay = mesh.getY(c2) - mesh.getY(c0);
        float az = mesh.getZ(c2) - mesh.getZ(c0);
        float bx = mesh.getX(c3) - mesh.getX(c1);
        float by = mesh.getY(c3) - mesh.getY(c1);
        float bz = mesh.getZ(c3) - mesh.getZ(c1);

        normal.set(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx);
    }

    private float resolveFaceCenterX(RiggedMeshStruct mesh, int faceIndex) {

        float x = 0f;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            x += mesh.getX(mesh.getFaceVertex(faceIndex, c));

        return x;
    }

    // Vertices \\

    // Every vertex the remap gave a new copy of becomes the selection, and nothing else
    private void selectRemapped(CharacterMeshDocumentInstance document) {

        BooleanArrayList selection = document.getSelection();

        document.syncSelection();

        for (int i = 0; i < selection.size(); i++)
            selection.set(i, false);

        for (int i = 0; i < vertexRemap.size(); i++)
            if (vertexRemap.getInt(i) != EngineSetting.INDEX_NOT_FOUND)
                selection.set(vertexRemap.getInt(i), true);
    }

    // Removes every vertex no face stands on, carrying the selection, its weights and the active vertex over to the
    // new numbering
    void removeOrphans(CharacterMeshDocumentInstance document) {

        RiggedMeshStruct mesh = document.getMesh();
        BooleanArrayList selection = document.getSelection();
        FloatArrayList weights = document.getSelectionWeights();
        BooleanArrayList hidden = document.getHidden();
        int vertexCount = mesh.getVertexCount();

        vertexFlags.clear();
        vertexFlags.size(vertexCount);

        for (int i = 0; i < vertexCount; i++)
            vertexFlags.set(i, true);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
                vertexFlags.set(mesh.getFaceVertex(f, c), false);

        document.syncSelection();
        mesh.removeVertices(vertexFlags, vertexRemap);
        selectionScratch.clear();
        selectionScratch.size(mesh.getVertexCount());
        selectionWeightScratch.clear();
        selectionWeightScratch.size(mesh.getVertexCount());
        hiddenScratch.clear();
        hiddenScratch.size(mesh.getVertexCount());

        for (int i = 0; i < vertexCount; i++)
            if (vertexRemap.getInt(i) != EngineSetting.INDEX_NOT_FOUND) {
                selectionScratch.set(vertexRemap.getInt(i), selection.getBoolean(i));
                selectionWeightScratch.set(vertexRemap.getInt(i), weights.getFloat(i));
                hiddenScratch.set(vertexRemap.getInt(i), hidden.getBoolean(i));
            }

        int activeVertex = document.getActiveVertex();

        document.setActiveVertex(activeVertex != EngineSetting.INDEX_NOT_FOUND
                ? vertexRemap.getInt(activeVertex)
                : EngineSetting.INDEX_NOT_FOUND);
        selection.clear();
        selection.addAll(selectionScratch);
        weights.clear();
        weights.addAll(selectionWeightScratch);
        hidden.clear();
        hidden.addAll(hiddenScratch);
        document.markHiddenRemapped();
    }

    private void collectSelected(CharacterMeshDocumentInstance document, IntArrayList vertices) {

        vertices.clear();

        for (int i = 0; i < document.getSelection().size(); i++)
            if (document.isSelected(i))
                vertices.add(i);
    }

    // Utility \\

    private void fill(IntArrayList list, int size, int value) {

        list.clear();
        list.size(size);

        for (int i = 0; i < size; i++)
            list.set(i, value);
    }

    private float getAxis(Vector3 vector, int axis) {
        return axis == 0 ? vector.x : axis == 1 ? vector.y : vector.z;
    }
}
