package application.bootstrap.geometrypipeline.mesh;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RiggedMeshStruct extends StructPackage {

    /*
     * An editable rigged quad mesh, the one form every character mesh is
     * parsed into, built from and written back out of. A vertex is a position
     * with up to MAX_BONE_INFLUENCES weighted bones, shared by every quad
     * corner standing on it with the same influences; a face is four vertices
     * in winding order, a texture from the mesh's palette and, when authored,
     * its own corner UVs and the zone it is made of. Each vertex also carries
     * its mask channels, and named morph targets move vertices by sparse
     * deltas, so one mesh holds every face shape and expression a character
     * slides between. MeshBuilder builds the game's meshes from it and the
     * character editor changes it in place.
     */

    // Rig
    private final RigHandle rigHandle;

    // Textures
    private final ObjectArrayList<String> textureNames;

    // Vertices — three position floats and MAX_BONE_INFLUENCES influence slots each, filled from the first
    private final FloatArrayList positions;
    private final IntArrayList influenceBones;
    private final FloatArrayList influenceWeights;
    private final FloatArrayList masks;

    // Faces — four vertices, a texture and eight corner UV floats each
    private final IntArrayList faceVertices;
    private final IntArrayList faceTextures;
    private final FloatArrayList faceUVs;
    private final BooleanArrayList faceAuthoredUVs;
    private final IntArrayList faceZones;

    // Morphs — per target its name, the vertices it moves and three delta floats per vertex
    private final ObjectArrayList<String> morphNames;
    private final ObjectArrayList<IntArrayList> morphVertices;
    private final ObjectArrayList<FloatArrayList> morphDeltas;

    // Constructor \\

    public RiggedMeshStruct(RigHandle rigHandle) {

        // Rig
        this.rigHandle = rigHandle;

        // Textures
        this.textureNames = new ObjectArrayList<>();

        // Vertices
        this.positions = new FloatArrayList();
        this.influenceBones = new IntArrayList();
        this.influenceWeights = new FloatArrayList();
        this.masks = new FloatArrayList();

        // Faces
        this.faceVertices = new IntArrayList();
        this.faceTextures = new IntArrayList();
        this.faceUVs = new FloatArrayList();
        this.faceAuthoredUVs = new BooleanArrayList();
        this.faceZones = new IntArrayList();

        // Morphs
        this.morphNames = new ObjectArrayList<>();
        this.morphVertices = new ObjectArrayList<>();
        this.morphDeltas = new ObjectArrayList<>();
    }

    public RiggedMeshStruct(RiggedMeshStruct source) {
        this(source.rigHandle);
        set(source);
    }

    // Copy \\

    // Takes every texture, vertex and face of a mesh on the same rig, keeping this mesh's storage
    public void set(RiggedMeshStruct source) {

        if (source.rigHandle != rigHandle)
            throwException("A rigged mesh can only take the content of a mesh on the same rig.");

        textureNames.clear();
        textureNames.addAll(source.textureNames);
        positions.clear();
        positions.addAll(source.positions);
        influenceBones.clear();
        influenceBones.addAll(source.influenceBones);
        influenceWeights.clear();
        influenceWeights.addAll(source.influenceWeights);
        masks.clear();
        masks.addAll(source.masks);
        faceVertices.clear();
        faceVertices.addAll(source.faceVertices);
        faceTextures.clear();
        faceTextures.addAll(source.faceTextures);
        faceUVs.clear();
        faceUVs.addAll(source.faceUVs);
        faceAuthoredUVs.clear();
        faceAuthoredUVs.addAll(source.faceAuthoredUVs);
        faceZones.clear();
        faceZones.addAll(source.faceZones);
        setMorphs(source);
    }

    // Takes every morph target of a mesh, reusing this mesh's lists where it already has them
    private void setMorphs(RiggedMeshStruct source) {

        morphNames.clear();
        morphNames.addAll(source.morphNames);

        while (morphVertices.size() < morphNames.size()) {
            morphVertices.add(new IntArrayList());
            morphDeltas.add(new FloatArrayList());
        }

        morphVertices.size(morphNames.size());
        morphDeltas.size(morphNames.size());

        for (int m = 0; m < morphNames.size(); m++) {
            morphVertices.get(m).clear();
            morphVertices.get(m).addAll(source.morphVertices.get(m));
            morphDeltas.get(m).clear();
            morphDeltas.get(m).addAll(source.morphDeltas.get(m));
        }
    }

    // Textures \\

    // The palette index of a texture, added to the palette the first time it is named
    public int resolveTexture(String textureName) {

        int textureIndex = textureNames.indexOf(textureName);

        if (textureIndex != EngineSetting.INDEX_NOT_FOUND)
            return textureIndex;

        textureNames.add(textureName);
        return textureNames.size() - 1;
    }

    public int getTextureCount() {
        return textureNames.size();
    }

    public String getTextureName(int textureIndex) {
        return textureNames.get(textureIndex);
    }

    // Vertices \\

    public int addVertex(float x, float y, float z) {

        positions.add(x);
        positions.add(y);
        positions.add(z);

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {
            influenceBones.add(EngineSetting.INDEX_NOT_FOUND);
            influenceWeights.add(0f);
        }

        for (int i = 0; i < EngineSetting.RIGGED_MESH_MASK_FLOATS; i++)
            masks.add(0f);

        return getVertexCount() - 1;
    }

    // A new vertex at another's position with its influences
    public int duplicateVertex(int vertexIndex) {

        int copy = addVertex(getX(vertexIndex), getY(vertexIndex), getZ(vertexIndex));
        copyInfluences(vertexIndex, copy);
        copyMasks(vertexIndex, copy);

        return copy;
    }

    public void setPosition(int vertexIndex, float x, float y, float z) {

        int base = vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        positions.set(base, x);
        positions.set(base + 1, y);
        positions.set(base + 2, z);
    }

    public int getVertexCount() {
        return positions.size() / EngineSetting.RIGGED_MESH_POSITION_FLOATS;
    }

    public float getX(int vertexIndex) {
        return positions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
    }

    public float getY(int vertexIndex) {
        return positions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 1);
    }

    public float getZ(int vertexIndex) {
        return positions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + 2);
    }

    public float getAxis(int vertexIndex, int axis) {
        return positions.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_POSITION_FLOATS + axis);
    }

    // Removes every vertex flagged, renumbering the rest in order; remap receives each old vertex's new index, or
    // INDEX_NOT_FOUND for one removed. No face may still use a vertex removed.
    public void removeVertices(BooleanArrayList removed, IntArrayList remap) {

        int vertexCount = getVertexCount();
        int kept = 0;

        remap.clear();

        for (int i = 0; i < vertexCount; i++) {

            if (removed.getBoolean(i)) {
                remap.add(EngineSetting.INDEX_NOT_FOUND);
                continue;
            }

            if (kept != i)
                moveVertex(i, kept);

            remap.add(kept++);
        }

        positions.size(kept * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        influenceBones.size(kept * EngineSetting.MAX_BONE_INFLUENCES);
        influenceWeights.size(kept * EngineSetting.MAX_BONE_INFLUENCES);
        masks.size(kept * EngineSetting.RIGGED_MESH_MASK_FLOATS);
        remapMorphs(remap);

        for (int i = 0; i < faceVertices.size(); i++) {

            int vertexIndex = remap.getInt(faceVertices.getInt(i));

            if (vertexIndex == EngineSetting.INDEX_NOT_FOUND)
                throwException("A rigged mesh vertex cannot be removed while a face still uses it.");

            faceVertices.set(i, vertexIndex);
        }
    }

    // Drops the vertex added last, before any face stands on it
    public void removeLastVertex() {

        int vertexCount = getVertexCount() - 1;

        positions.size(vertexCount * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        influenceBones.size(vertexCount * EngineSetting.MAX_BONE_INFLUENCES);
        influenceWeights.size(vertexCount * EngineSetting.MAX_BONE_INFLUENCES);
        masks.size(vertexCount * EngineSetting.RIGGED_MESH_MASK_FLOATS);
    }

    private void moveVertex(int from, int to) {

        setPosition(to, getX(from), getY(from), getZ(from));
        copyInfluences(from, to);
        copyMasks(from, to);
    }

    // Masks \

    public float getMask(int vertexIndex, int channel) {
        return masks.getFloat(vertexIndex * EngineSetting.RIGGED_MESH_MASK_FLOATS + channel);
    }

    public void setMask(int vertexIndex, int channel, float value) {
        masks.set(vertexIndex * EngineSetting.RIGGED_MESH_MASK_FLOATS + channel, value);
    }

    public void copyMasks(int fromVertex, int toVertex) {

        int from = fromVertex * EngineSetting.RIGGED_MESH_MASK_FLOATS;
        int to = toVertex * EngineSetting.RIGGED_MESH_MASK_FLOATS;

        for (int i = 0; i < EngineSetting.RIGGED_MESH_MASK_FLOATS; i++)
            masks.set(to + i, masks.getFloat(from + i));
    }

    // Morphs \

    // The index of a morph target, added empty the first time it is named
    public int resolveMorph(String morphName) {

        int morphIndex = getMorphIndex(morphName);

        if (morphIndex != EngineSetting.INDEX_NOT_FOUND)
            return morphIndex;

        morphNames.add(morphName);
        morphVertices.add(new IntArrayList());
        morphDeltas.add(new FloatArrayList());

        return morphNames.size() - 1;
    }

    public int getMorphIndex(String morphName) {
        return morphNames.indexOf(morphName);
    }

    public int getMorphCount() {
        return morphNames.size();
    }

    public String getMorphName(int morphIndex) {
        return morphNames.get(morphIndex);
    }

    public int getMorphEntryCount(int morphIndex) {
        return morphVertices.get(morphIndex).size();
    }

    public int getMorphVertex(int morphIndex, int entry) {
        return morphVertices.get(morphIndex).getInt(entry);
    }

    public float getMorphDelta(int morphIndex, int entry, int axis) {
        return morphDeltas.get(morphIndex).getFloat(entry * EngineSetting.RIGGED_MESH_POSITION_FLOATS + axis);
    }

    // The entry a morph holds for a vertex, INDEX_NOT_FOUND when it does not move it
    public int findMorphEntry(int morphIndex, int vertexIndex) {
        return morphVertices.get(morphIndex).indexOf(vertexIndex);
    }

    // Sets how far a morph moves a vertex, adding the vertex to the morph the first time it moves
    public void setMorphDelta(int morphIndex, int vertexIndex, float dx, float dy, float dz) {

        int entry = findMorphEntry(morphIndex, vertexIndex);
        FloatArrayList deltas = morphDeltas.get(morphIndex);

        if (entry == EngineSetting.INDEX_NOT_FOUND) {
            morphVertices.get(morphIndex).add(vertexIndex);
            deltas.add(dx);
            deltas.add(dy);
            deltas.add(dz);
            return;
        }

        int base = entry * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        deltas.set(base, dx);
        deltas.set(base + 1, dy);
        deltas.set(base + 2, dz);
    }

    // Empties a morph, keeping its name and index
    public void clearMorph(int morphIndex) {
        morphVertices.get(morphIndex).clear();
        morphDeltas.get(morphIndex).clear();
    }

    // Renumbers every morph's vertices after a removal, dropping the entries of vertices removed
    private void remapMorphs(IntArrayList remap) {

        for (int m = 0; m < morphNames.size(); m++) {

            IntArrayList vertices = morphVertices.get(m);
            FloatArrayList deltas = morphDeltas.get(m);
            int kept = 0;

            for (int e = 0; e < vertices.size(); e++) {

                int vertexIndex = remap.getInt(vertices.getInt(e));

                if (vertexIndex == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                vertices.set(kept, vertexIndex);

                for (int a = 0; a < EngineSetting.RIGGED_MESH_POSITION_FLOATS; a++)
                    deltas.set(kept * EngineSetting.RIGGED_MESH_POSITION_FLOATS + a,
                            deltas.getFloat(e * EngineSetting.RIGGED_MESH_POSITION_FLOATS + a));

                kept++;
            }

            vertices.size(kept);
            deltas.size(kept * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        }
    }

    // Influences \\

    public int getInfluenceCount(int vertexIndex) {

        int base = vertexIndex * EngineSetting.MAX_BONE_INFLUENCES;
        int count = 0;

        while (count < EngineSetting.MAX_BONE_INFLUENCES
                && influenceBones.getInt(base + count) != EngineSetting.INDEX_NOT_FOUND)
            count++;

        return count;
    }

    public int getInfluenceBone(int vertexIndex, int influence) {
        return influenceBones.getInt(vertexIndex * EngineSetting.MAX_BONE_INFLUENCES + influence);
    }

    public float getInfluenceWeight(int vertexIndex, int influence) {
        return influenceWeights.getFloat(vertexIndex * EngineSetting.MAX_BONE_INFLUENCES + influence);
    }

    public int findInfluence(int vertexIndex, int boneIndex) {

        int count = getInfluenceCount(vertexIndex);

        for (int i = 0; i < count; i++)
            if (getInfluenceBone(vertexIndex, i) == boneIndex)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The weight a bone has on a vertex, zero when it has none
    public float getBoneWeight(int vertexIndex, int boneIndex) {

        int influence = findInfluence(vertexIndex, boneIndex);
        return influence == EngineSetting.INDEX_NOT_FOUND ? 0f : getInfluenceWeight(vertexIndex, influence);
    }

    public void addInfluence(int vertexIndex, int boneIndex, float weight) {

        int count = getInfluenceCount(vertexIndex);

        if (count == EngineSetting.MAX_BONE_INFLUENCES)
            throwException("A rigged mesh vertex holds at most " + EngineSetting.MAX_BONE_INFLUENCES
                    + " bone influences.");

        int slot = vertexIndex * EngineSetting.MAX_BONE_INFLUENCES + count;

        influenceBones.set(slot, boneIndex);
        influenceWeights.set(slot, weight);
    }

    public void setInfluenceWeight(int vertexIndex, int influence, float weight) {
        influenceWeights.set(vertexIndex * EngineSetting.MAX_BONE_INFLUENCES + influence, weight);
    }

    public void clearInfluences(int vertexIndex) {

        int base = vertexIndex * EngineSetting.MAX_BONE_INFLUENCES;

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {
            influenceBones.set(base + i, EngineSetting.INDEX_NOT_FOUND);
            influenceWeights.set(base + i, 0f);
        }
    }

    public void copyInfluences(int fromVertex, int toVertex) {

        int from = fromVertex * EngineSetting.MAX_BONE_INFLUENCES;
        int to = toVertex * EngineSetting.MAX_BONE_INFLUENCES;

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++) {
            influenceBones.set(to + i, influenceBones.getInt(from + i));
            influenceWeights.set(to + i, influenceWeights.getFloat(from + i));
        }
    }

    // Whether two vertices hold the same bones with the same weights, in the same order
    public boolean hasSameInfluences(int vertexA, int vertexB) {

        int a = vertexA * EngineSetting.MAX_BONE_INFLUENCES;
        int b = vertexB * EngineSetting.MAX_BONE_INFLUENCES;

        for (int i = 0; i < EngineSetting.MAX_BONE_INFLUENCES; i++)
            if (influenceBones.getInt(a + i) != influenceBones.getInt(b + i)
                    || influenceWeights.getFloat(a + i) != influenceWeights.getFloat(b + i))
                return false;

        return true;
    }

    // Faces \\

    // A face over four vertices in winding order, spanning its texture's whole tile until UVs are authored for it
    public int addFace(int v0, int v1, int v2, int v3, int textureIndex) {

        faceVertices.add(v0);
        faceVertices.add(v1);
        faceVertices.add(v2);
        faceVertices.add(v3);
        faceTextures.add(textureIndex);
        faceUVs.addElements(faceUVs.size(), EngineSetting.MESH_QUAD_DEFAULT_UVS);
        faceAuthoredUVs.add(false);
        faceZones.add(MeshZone.SKIN.ordinal());

        return getFaceCount() - 1;
    }

    // A new face over the same vertices with the same texture and UVs
    public int duplicateFace(int faceIndex) {

        int copy = addFace(
                getFaceVertex(faceIndex, 0),
                getFaceVertex(faceIndex, 1),
                getFaceVertex(faceIndex, 2),
                getFaceVertex(faceIndex, 3),
                getFaceTexture(faceIndex));

        copyFaceUVs(faceIndex, copy);
        setFaceZone(copy, getFaceZone(faceIndex));
        return copy;
    }

    public int getFaceCount() {
        return faceTextures.size();
    }

    public int getFaceVertex(int faceIndex, int corner) {
        return faceVertices.getInt(faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner);
    }

    public void setFaceVertex(int faceIndex, int corner, int vertexIndex) {
        faceVertices.set(faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner, vertexIndex);
    }

    public int getFaceTexture(int faceIndex) {
        return faceTextures.getInt(faceIndex);
    }

    public void setFaceTexture(int faceIndex, int textureIndex) {
        faceTextures.set(faceIndex, textureIndex);
    }

    public MeshZone getFaceZone(int faceIndex) {
        return MeshZone.VALUES[faceZones.getInt(faceIndex)];
    }

    public void setFaceZone(int faceIndex, MeshZone zone) {
        faceZones.set(faceIndex, zone.ordinal());
    }

    public boolean hasAuthoredUVs(int faceIndex) {
        return faceAuthoredUVs.getBoolean(faceIndex);
    }

    public float getFaceU(int faceIndex, int corner) {
        return faceUVs.getFloat(toUVBase(faceIndex, corner));
    }

    public float getFaceV(int faceIndex, int corner) {
        return faceUVs.getFloat(toUVBase(faceIndex, corner) + 1);
    }

    // Authors one corner's UV, marking the face's UVs as its own
    public void setFaceUV(int faceIndex, int corner, float u, float v) {

        int base = toUVBase(faceIndex, corner);

        faceUVs.set(base, u);
        faceUVs.set(base + 1, v);
        faceAuthoredUVs.set(faceIndex, true);
    }

    public void copyFaceUVs(int fromFace, int toFace) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int from = toUVBase(fromFace, c);
            int to = toUVBase(toFace, c);

            faceUVs.set(to, faceUVs.getFloat(from));
            faceUVs.set(to + 1, faceUVs.getFloat(from + 1));
        }

        faceAuthoredUVs.set(toFace, faceAuthoredUVs.getBoolean(fromFace));
    }

    // Removes every face flagged, keeping the rest in order
    public void removeFaces(BooleanArrayList removed) {

        int faceCount = getFaceCount();
        int kept = 0;

        for (int i = 0; i < faceCount; i++) {

            if (removed.getBoolean(i))
                continue;

            if (kept != i)
                moveFace(i, kept);

            kept++;
        }

        faceVertices.size(kept * EngineSetting.QUAD_VERTEX_COUNT);
        faceTextures.size(kept);
        faceUVs.size(kept * EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.RIGGED_MESH_UV_FLOATS);
        faceAuthoredUVs.size(kept);
        faceZones.size(kept);
    }

    private void moveFace(int from, int to) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            setFaceVertex(to, c, getFaceVertex(from, c));

        faceTextures.set(to, faceTextures.getInt(from));
        faceZones.set(to, faceZones.getInt(from));
        copyFaceUVs(from, to);
    }

    // Whether a face stands on a vertex at any of its corners
    public boolean usesVertex(int faceIndex, int vertexIndex) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (getFaceVertex(faceIndex, c) == vertexIndex)
                return true;

        return false;
    }

    // Whether all four corners of a face hold the same influences, so it can name its bones once for the quad
    public boolean hasUniformInfluences(int faceIndex) {

        int first = getFaceVertex(faceIndex, 0);

        for (int c = 1; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (!hasSameInfluences(first, getFaceVertex(faceIndex, c)))
                return false;

        return true;
    }

    private int toUVBase(int faceIndex, int corner) {
        return (faceIndex * EngineSetting.QUAD_VERTEX_COUNT + corner) * EngineSetting.RIGGED_MESH_UV_FLOATS;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public boolean isEmpty() {
        return getFaceCount() == 0;
    }
}
