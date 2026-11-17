package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterLoopUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.stamppipeline.stamp.StampFurStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterPoseUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class CharacterFurBranch extends BranchPackage {

    /*
     * Lays a fur stamp's strand card over the body, as a groom scatters
     * cards over a scalp. Roots are scattered over the surface faces (never
     * over cards already laid) facing the fur
     * within its patch, one for every square of the spacing a face's area
     * holds, at places drawn from a hash of the face and the card, so the
     * same aim lays the same fur. Each card stands on its root along the
     * surface's normal and is combed along the surface toward the comb, down
     * and back on a body facing +z, or the other way when the Comb is
     * flipped, turned about the normal by the tool's Twist (the other way
     * for a mirrored twin) and by its own jitter, and sized by the Scale and
     * its own jitter. The card's two bends are modelled in the stamp's card;
     * the character shader sways it in the wind as it sways hair, by how far
     * along its strand each corner stands. A laid card takes the weights and
     * shape keys of the face it roots on, blended at its root, so it rides
     * the body as it moves and reshapes, and its faces keep the card's
     * texture, zone and UVs. Laying is one edit of the body.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterHistoryBranch characterHistoryBranch;
    private CharacterMeshBranch characterMeshBranch;
    private CharacterWeightBranch characterWeightBranch;
    private ToolManager toolManager;

    // Root — the face's corners a card roots between and the share each holds of it
    private IntArrayList rootCorners;
    private FloatArrayList rootShares;

    // Scratch
    private Vector3 centreScratch;
    private Vector3 normalScratch;
    private Vector3 rootScratch;
    private Vector3 combScratch;
    private Vector3 sideScratch;
    private Vector3 pointScratch;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.rootCorners = new IntArrayList();
        this.rootShares = new FloatArrayList();

        // Scratch
        this.centreScratch = new Vector3();
        this.normalScratch = new Vector3();
        this.rootScratch = new Vector3();
        this.combScratch = new Vector3();
        this.sideScratch = new Vector3();
        this.pointScratch = new Vector3();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterHistoryBranch = get(CharacterHistoryBranch.class);
        this.characterMeshBranch = get(CharacterMeshBranch.class);
        this.characterWeightBranch = get(CharacterWeightBranch.class);
        this.toolManager = get(ToolManager.class);
    }

    // Cards \\

    // Every card a fur placement lays, each with its frame in the bind pose and the place on its face it roots at
    void resolveCards(RiggedMeshStruct mesh, CharacterPartPlacementStruct placement) {

        StampFurStruct fur = placement.getStampHandle().getFur();
        float scale = placement.getScale();
        float radius = fur.getRadius() * scale;
        float spacing = fur.getSpacing() * scale;
        Vector3 point = placement.getPoint();

        placement.clearFur();

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!CharacterLoopUtility.isSurface(mesh, f))
                continue;

            float area = measureFace(mesh, f);

            if (area <= 0f || normalScratch.dot(placement.getNormal()) <= 0f
                    || CharacterMeshUtility.distanceSquared(centreScratch, point) > radius * radius)
                continue;

            float expected = area / (spacing * spacing);
            int count = (int) expected + (hash(f, EngineSetting.INDEX_NOT_FOUND) < expected - (int) expected ? 1 : 0);

            for (int k = 0; k < count; k++)
                addCard(mesh, f, k, fur, placement, radius);
        }
    }

    // One card rooted at its hashed place on a face, kept only where it lies within the patch
    private void addCard(
            RiggedMeshStruct mesh,
            int faceIndex,
            int card,
            StampFurStruct fur,
            CharacterPartPlacementStruct placement,
            float radius) {

        int channel = card * EditorSetting.CHARACTER_FUR_HASH_CHANNELS;
        float u = hash(faceIndex, channel + EditorSetting.CHARACTER_FUR_CHANNEL_U);
        float v = hash(faceIndex, channel + EditorSetting.CHARACTER_FUR_CHANNEL_V);

        resolveRoot(mesh, faceIndex, u, v, rootScratch);

        if (CharacterMeshUtility.distanceSquared(rootScratch, placement.getPoint()) > radius * radius)
            return;

        float jitter = fur.getJitter();
        float size = placement.getScale()
                * (1f + jitter * (hash(faceIndex, channel + EditorSetting.CHARACTER_FUR_CHANNEL_LENGTH) * 2f - 1f));
        float turn = (float) Math.toRadians(
                toolManager.getOption(ToolCanvas.CHARACTER_SCULPT, ToolOption.TURN)
                        + EditorSetting.CHARACTER_FUR_TURN_JITTER_DEGREES * jitter
                                * (hash(faceIndex, channel + EditorSetting.CHARACTER_FUR_CHANNEL_TURN) * 2f - 1f));

        writeFrame(placement.addFurCard(faceIndex, u, v), placement.isMirrored() ? -turn : turn, size);
    }

    // A card's frame: it stands along the face's normal, combs along the surface toward the comb turned about the
    // normal, crosses the two, and sits on its root
    private void writeFrame(Matrix4 frame, float turn, float size) {

        float[] comb = EditorSetting.CHARACTER_FUR_COMB;
        float sign = toolManager.getOption(ToolCanvas.CHARACTER_SCULPT, ToolOption.COMB)
                == EditorSetting.CHARACTER_FUR_COMB_FLIPPED ? -1f : 1f;

        projectComb(comb, sign);

        if (combScratch.length() <= EditorSetting.CHARACTER_VIEW_RAY_EPSILON)
            projectComb(EditorSetting.CHARACTER_FUR_COMB_FALLBACK, sign);

        combScratch.normalize();
        sideScratch.set(
                normalScratch.y * combScratch.z - normalScratch.z * combScratch.y,
                normalScratch.z * combScratch.x - normalScratch.x * combScratch.z,
                normalScratch.x * combScratch.y - normalScratch.y * combScratch.x);

        float cos = (float) Math.cos(turn);
        float sin = (float) Math.sin(turn);

        combScratch.set(
                combScratch.x * cos + sideScratch.x * sin,
                combScratch.y * cos + sideScratch.y * sin,
                combScratch.z * cos + sideScratch.z * sin);
        sideScratch.set(
                normalScratch.y * combScratch.z - normalScratch.z * combScratch.y,
                normalScratch.z * combScratch.x - normalScratch.x * combScratch.z,
                normalScratch.x * combScratch.y - normalScratch.y * combScratch.x);

        frame.set(
                sideScratch.x * size, normalScratch.x * size, combScratch.x * size, rootScratch.x,
                sideScratch.y * size, normalScratch.y * size, combScratch.y * size, rootScratch.y,
                sideScratch.z * size, normalScratch.z * size, combScratch.z * size, rootScratch.z,
                0f, 0f, 0f, 1f);
    }

    // The comb, flipped by the sign given, with the part along the face's normal taken out
    private void projectComb(float[] comb, float sign) {

        combScratch.set(comb[0] * sign, comb[1] * sign, comb[2] * sign);

        float along = combScratch.dot(normalScratch);

        combScratch.subtract(normalScratch.x * along, normalScratch.y * along, normalScratch.z * along);
    }

    // Lay \\

    // The cards of a fur placement, and its twin's when one is given, laid on the body as one edit; false, with the
    // status saying why, when none could be
    boolean lay(
            CharacterMeshDocumentInstance document,
            CharacterPartPlacementStruct placement,
            CharacterPartPlacementStruct twin) {

        characterHistoryBranch.begin(document);

        int laid = appendCards(document.getMesh(), placement);

        if (twin != null)
            laid += appendCards(document.getMesh(), twin);

        if (laid == 0) {
            characterHistoryBranch.cancel(document);
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_FUR_MESSAGE_NONE);
            return false;
        }

        if (!characterMeshBranch.finishEdit(document))
            return false;

        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_FUR_MESSAGE_LAID + laid);

        return true;
    }

    // Every card of a placement brought into the body: the stamp's card carried by its frame, weighed and shaped as
    // its root is, its faces as the card's
    private int appendCards(RiggedMeshStruct mesh, CharacterPartPlacementStruct placement) {

        RiggedMeshStruct card = placement.getStampHandle().getMesh();

        for (int c = 0; c < placement.getFurCount(); c++) {

            int vertexBase = mesh.getVertexCount();

            for (int v = 0; v < card.getVertexCount(); v++) {

                CharacterPoseUtility.transformPoint(
                        placement.getFurFrame(c), card.getX(v), card.getY(v), card.getZ(v), pointScratch);

                int copy = mesh.addVertex(0f, 0f, 0f);

                CharacterMeshUtility.setRoundedPosition(mesh, copy, pointScratch.x, pointScratch.y, pointScratch.z);
            }

            resolveShares(mesh, placement.getFurFace(c), placement.getFurU(c), placement.getFurV(c));
            characterWeightBranch.blend(mesh, rootCorners, rootShares, vertexBase);

            for (int v = 1; v < card.getVertexCount(); v++)
                mesh.copyInfluences(vertexBase, vertexBase + v);

            followMorphs(mesh, vertexBase, card.getVertexCount());

            for (int f = 0; f < card.getFaceCount(); f++)
                appendFace(mesh, card, f, vertexBase);
        }

        return placement.getFurCount();
    }

    // Every shape key moves a card as far as it moves the card's root, so the fur rides a reshaped body
    private void followMorphs(RiggedMeshStruct mesh, int vertexBase, int vertexCount) {

        for (int m = 0; m < mesh.getMorphCount(); m++) {

            float dx = 0f;
            float dy = 0f;
            float dz = 0f;

            for (int i = 0; i < rootCorners.size(); i++) {

                int entry = mesh.findMorphEntry(m, rootCorners.getInt(i));

                if (entry == EngineSetting.INDEX_NOT_FOUND)
                    continue;

                float share = rootShares.getFloat(i);

                dx += mesh.getMorphDelta(m, entry, 0) * share;
                dy += mesh.getMorphDelta(m, entry, 1) * share;
                dz += mesh.getMorphDelta(m, entry, 2) * share;
            }

            if (Math.abs(dx) <= EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                    && Math.abs(dy) <= EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON
                    && Math.abs(dz) <= EngineSetting.RIGGED_MESH_MORPH_DELTA_EPSILON)
                continue;

            for (int v = 0; v < vertexCount; v++)
                mesh.setMorphDelta(m, vertexBase + v, dx, dy, dz);
        }
    }

    private void appendFace(RiggedMeshStruct mesh, RiggedMeshStruct card, int faceIndex, int vertexBase) {

        int copy = mesh.addFace(
                vertexBase + card.getFaceVertex(faceIndex, 0),
                vertexBase + card.getFaceVertex(faceIndex, 1),
                vertexBase + card.getFaceVertex(faceIndex, 2),
                vertexBase + card.getFaceVertex(faceIndex, 3),
                mesh.resolveTexture(card.getTextureName(card.getFaceTexture(faceIndex))));

        mesh.setFaceZone(copy, card.getFaceZone(faceIndex));
        mesh.setFacePins(copy, card.getFacePins(faceIndex));

        if (!card.hasAuthoredUVs(faceIndex))
            return;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            mesh.setFaceUV(copy, c, card.getFaceU(faceIndex, c), card.getFaceV(faceIndex, c));
    }

    // Root \\

    // A place on a face, bilinear across its corners in their order
    private void resolveRoot(RiggedMeshStruct mesh, int faceIndex, float u, float v, Vector3 out) {

        resolveShares(mesh, faceIndex, u, v);
        out.set(0f, 0f, 0f);

        for (int i = 0; i < rootCorners.size(); i++) {

            int corner = rootCorners.getInt(i);
            float share = rootShares.getFloat(i);

            out.add(mesh.getX(corner) * share, mesh.getY(corner) * share, mesh.getZ(corner) * share);
        }
    }

    // The share each of a face's corners holds of a place on it: (1 - u)(1 - v), u(1 - v), uv and (1 - u)v
    private void resolveShares(RiggedMeshStruct mesh, int faceIndex, float u, float v) {

        rootCorners.clear();
        rootShares.clear();

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            float alongU = c == 1 || c == 2 ? u : 1f - u;
            float alongV = c >= 2 ? v : 1f - v;

            rootCorners.add(mesh.getFaceVertex(faceIndex, c));
            rootShares.add(alongU * alongV);
        }
    }

    // A face's area, its centre and its unit Newell normal left in the scratch
    private float measureFace(RiggedMeshStruct mesh, int faceIndex) {

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

        float length = normalScratch.length();

        if (length <= 0f)
            return 0f;

        normalScratch.divide(length);

        return length * 0.5f;
    }

    // Utility \\

    // A number from 0 to 1 drawn from a face and a channel, the same every time
    private static float hash(int faceIndex, int channel) {
        return BiomeFieldUtility.hash01(
                BiomeFieldUtility.hashCell(EditorSetting.CHARACTER_FUR_SALT, faceIndex, channel));
    }
}
