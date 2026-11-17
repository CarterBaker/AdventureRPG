package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPartPlacementStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorSide;
import editor.bootstrap.charactereditorpipeline.util.CharacterLoopUtility;
import editor.bootstrap.charactereditorpipeline.util.CharacterMeshUtility;
import editor.bootstrap.stamppipeline.stamp.StampHandle;
import editor.bootstrap.stamppipeline.stamp.StampSocketStruct;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.bootstrap.toolpipeline.util.ToolOption;
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
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPlacementBranch extends BranchPackage {

    /*
     * Works out where a body part chosen in the Stamps tab would join the
     * character's body as the Stamp tab aims it: at an open loop already in
     * the body, whose rim the part is welded or bridged to, or at the surface
     * pointed at, where a hole the size of the part's plug is cut. Either
     * must lie near an attach point, a joint of the rig or an open loop's
     * centre. The plug is turned to face into the body, twisted about that
     * axis by the tool's Twist, tilted as the pointer turns it once set down,
     * and scaled by the tool's Scale; a part standing on the other side of
     * the body from the side its bones were cut on is mirrored, so a left arm
     * joins the right shoulder still facing forward, its bones named for the
     * side it joins. A root part, which has no plug, starts the body over at
     * the rig's root. Fur is laid over a patch about the point aimed at,
     * anywhere on the body, its cards found by CharacterFurBranch. A
     * placement's twin is the same part mirrored across the body. The body's open loops are found once per edit.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private CharacterRigBranch characterRigBranch;
    private CharacterFurBranch characterFurBranch;
    private ToolManager toolManager;
    private MeshManager meshManager;

    // Attach Points — the joints of the bones a part may join near, large enough for the rig's height, and that
    // height, both found again with the loops or whenever the editor's state moves
    private IntArrayList attachJoints;
    private float rigHeight;
    private int syncedManagerRevision;

    // Loops — the body's open loops as last found, each loop's centre, outward normal, radius and heaviest bone
    private CharacterMeshDocumentInstance loopDocument;
    private int loopRevision;
    private int loopCount;
    private ObjectArrayList<IntArrayList> loops;
    private FloatArrayList loopCentres;
    private FloatArrayList loopNormals;
    private FloatArrayList loopRadii;
    private IntArrayList loopBones;
    private LongOpenHashSet directedEdges;
    private IntArrayList loopNext;
    private float[] boneTotals;

    // Scratch
    private Vector3 centreScratch;
    private Vector3 normalScratch;
    private Vector3 jointScratch;
    private Vector3 plugNormal;
    private Vector3 target;
    private Vector3 axis;
    private Matrix4 align;
    private Matrix4 twist;

    // Base \\

    @Override
    protected void create() {

        // Attach Points
        this.attachJoints = new IntArrayList();
        this.syncedManagerRevision = EngineSetting.INDEX_NOT_FOUND;

        // Loops
        this.loopRevision = EngineSetting.INDEX_NOT_FOUND;
        this.loops = new ObjectArrayList<>();
        this.loopCentres = new FloatArrayList();
        this.loopNormals = new FloatArrayList();
        this.loopRadii = new FloatArrayList();
        this.loopBones = new IntArrayList();
        this.directedEdges = new LongOpenHashSet();
        this.loopNext = new IntArrayList();
        this.boneTotals = new float[0];

        // Scratch
        this.centreScratch = new Vector3();
        this.normalScratch = new Vector3();
        this.jointScratch = new Vector3();
        this.plugNormal = new Vector3();
        this.target = new Vector3();
        this.axis = new Vector3();
        this.align = new Matrix4();
        this.twist = new Matrix4();
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.characterRigBranch = get(CharacterRigBranch.class);
        this.characterFurBranch = get(CharacterFurBranch.class);
        this.toolManager = get(ToolManager.class);
        this.meshManager = get(MeshManager.class);
    }

    // Loops \\

    // The body's open loops and the joints a part may join near found again whenever the body was edited or the
    // editor's state, the rig among it, moved since they were last found
    void sync(CharacterMeshDocumentInstance document) {

        if (document == loopDocument && document.getRevision() == loopRevision
                && characterEditorManager.getRevision() == syncedManagerRevision)
            return;

        RiggedMeshStruct mesh = document.getMesh();

        this.syncedManagerRevision = characterEditorManager.getRevision();
        syncJoints(mesh);

        this.loopDocument = document;
        this.loopRevision = document.getRevision();
        this.loopCount = CharacterLoopUtility.collectLoops(mesh, null, directedEdges, loopNext, loops);

        if (boneTotals.length < mesh.getRigHandle().getBoneCount())
            this.boneTotals = new float[mesh.getRigHandle().getBoneCount()];

        loopCentres.clear();
        loopNormals.clear();
        loopRadii.clear();
        loopBones.clear();

        for (int i = 0; i < loopCount; i++) {

            IntArrayList loop = loops.get(i);

            CharacterLoopUtility.resolveCentre(mesh, loop, centreScratch);
            CharacterLoopUtility.resolveNormal(mesh, loop, normalScratch);
            loopCentres.add(centreScratch.x);
            loopCentres.add(centreScratch.y);
            loopCentres.add(centreScratch.z);
            loopNormals.add(-normalScratch.x);
            loopNormals.add(-normalScratch.y);
            loopNormals.add(-normalScratch.z);
            loopRadii.add(CharacterLoopUtility.resolveRadius(mesh, loop, centreScratch));
            loopBones.add(CharacterLoopUtility.resolveHeaviestBone(mesh, loop, boneTotals));
        }
    }

    // Attach Points \\

    // The rig's height, then the joint of every bone that carries some of the body and whose largest side reaches its
    // share of that height, so neither the fine bones of a face or a hand nor the bones of worn hair crowd the body
    private void syncJoints(RiggedMeshStruct mesh) {

        if (boneTotals.length < characterRigBranch.getBoneCount())
            this.boneTotals = new float[characterRigBranch.getBoneCount()];

        Arrays.fill(boneTotals, 0f);

        for (int v = 0; v < mesh.getVertexCount(); v++)
            for (int n = 0; n < mesh.getInfluenceCount(v); n++)
                if (mesh.getInfluenceBone(v, n) < boneTotals.length)
                    boneTotals[mesh.getInfluenceBone(v, n)] += mesh.getInfluenceWeight(v, n);

        float low = Float.POSITIVE_INFINITY;
        float high = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < characterRigBranch.getBoneCount(); i++) {

            CharacterPoseUtility.getTranslation(characterRigBranch.getWorldMatrix(i), jointScratch);
            low = Math.min(low, jointScratch.y);
            high = Math.max(high, jointScratch.y);
        }

        this.rigHeight = high > low ? high - low : 1f;
        attachJoints.clear();

        for (int i = 0; i < characterRigBranch.getBoneCount(); i++) {

            Vector3 size = characterRigBranch.getSize(i);
            float largest = Math.max(size.x, Math.max(size.y, size.z));

            if (boneTotals[i] > 0f && largest >= rigHeight * EditorSetting.CHARACTER_STAMP_JOINT_SIZE_SHARE)
                attachJoints.add(i);
        }
    }

    // Every joint a part may join near, then every open loop's centre
    int getAttachPointCount() {
        return attachJoints.size() + loopCount;
    }

    void getAttachPoint(int attachPoint, Vector3 out) {

        if (attachPoint < attachJoints.size()) {
            Matrix4 world = characterRigBranch.getWorldMatrix(attachJoints.getInt(attachPoint));

            CharacterPoseUtility.getTranslation(world, out);
            return;
        }

        int loop = (attachPoint - attachJoints.size()) * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        out.set(loopCentres.getFloat(loop), loopCentres.getFloat(loop + 1), loopCentres.getFloat(loop + 2));
    }

    // Whether an attach point is an open loop's centre, which a part snaps to, rather than a joint
    boolean isLoopPoint(int attachPoint) {
        return attachPoint >= attachJoints.size() && attachPoint < getAttachPointCount();
    }

    // The attach point nearest a point, INDEX_NOT_FOUND when there is none
    private int findNearestAttachPoint(Vector3 point) {

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        float best = Float.POSITIVE_INFINITY;

        for (int i = 0; i < getAttachPointCount(); i++) {

            getAttachPoint(i, jointScratch);

            float distance = CharacterMeshUtility.distanceSquared(jointScratch, point);

            if (distance < best) {
                best = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    // The open loop whose centre lies nearest a point within a reach, INDEX_NOT_FOUND for none
    private int findLoopNear(Vector3 point, float reach) {

        int nearest = EngineSetting.INDEX_NOT_FOUND;
        float best = reach * reach;

        for (int i = 0; i < loopCount; i++) {

            getAttachPoint(attachJoints.size() + i, jointScratch);

            float distance = CharacterMeshUtility.distanceSquared(jointScratch, point);

            if (distance <= best) {
                best = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    // Placement \\

    // A placement at an open loop of the body, taken by its attach point, or at a face of the body where the ray met
    // it; a root part, which has no plug, is placed at the rig's root whatever is aimed at
    void resolve(
            CharacterMeshDocumentInstance document,
            StampHandle stampHandle,
            int attachPoint,
            int faceIndex,
            Vector3 point,
            Vector3 normal,
            Matrix4 tilt,
            CharacterPartPlacementStruct out) {

        sync(document);
        out.begin(stampHandle, toolManager.getOption(ToolCanvas.CHARACTER_SCULPT, ToolOption.SCALE));

        if (stampHandle.hasFur()) {
            resolveFur(document.getMesh(), point, normal, out);
            return;
        }

        if (!stampHandle.hasPlug()) {
            resolveRoot(stampHandle, out);
            return;
        }

        RiggedMeshStruct mesh = document.getMesh();
        int hostBone;

        if (isLoopPoint(attachPoint)) {

            int loop = attachPoint - attachJoints.size();
            int at = loop * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

            out.setTarget(
                    loopCentres.getFloat(at),
                    loopCentres.getFloat(at + 1),
                    loopCentres.getFloat(at + 2),
                    loopNormals.getFloat(at),
                    loopNormals.getFloat(at + 1),
                    loopNormals.getFloat(at + 2),
                    loopRadii.getFloat(loop),
                    true);
            hostBone = loopBones.getInt(loop);
        } else {
            out.setTarget(
                    point.x, point.y, point.z,
                    normal.x, normal.y, normal.z,
                    stampHandle.getPlug().getRadius() * out.getScale(),
                    false);
            hostBone = resolveFaceBone(mesh, faceIndex);
        }

        int nearest = findNearestAttachPoint(out.getPoint());

        if (hostBone == EngineSetting.INDEX_NOT_FOUND)
            hostBone = 0;

        out.setHost(
                mesh.getRigHandle().getBone(hostBone).getName(),
                resolveSide(out.getPoint().x),
                out.isOnLoop() ? attachPoint : nearest);

        float reach = rigHeight * EditorSetting.CHARACTER_STAMP_ATTACH_REACH_SHARE;

        if (!out.isOnLoop() && (nearest == EngineSetting.INDEX_NOT_FOUND
                || resolveDistance(nearest, out.getPoint()) > reach))
            out.refuse(EditorSetting.CHARACTER_STAMP_MESSAGE_FAR);

        refuseOverLimit(mesh.getFaceCount() + stampHandle.getMesh().getFaceCount(), out);

        String stampSide = resolveStampSide(stampHandle);

        out.setMirrored(!stampSide.isEmpty() && !out.getSide().isEmpty() && !stampSide.equals(out.getSide()));
        compose(stampHandle.getPlug(), tilt, out);
    }

    // A root part placed to start a blank model's body, where no body stands to aim at
    void resolveStart(StampHandle stampHandle, CharacterPartPlacementStruct out) {

        out.begin(stampHandle, toolManager.getOption(ToolCanvas.CHARACTER_SCULPT, ToolOption.SCALE));
        resolveRoot(stampHandle, out);
    }

    // A root part with its first bone at the rig's root joint, upright and unturned, starting the body over
    private void resolveRoot(StampHandle stampHandle, CharacterPartPlacementStruct out) {

        float scale = out.getScale();
        ArpgArrayStruct bones = stampHandle.getBonesArpg();

        CharacterPoseUtility.getTranslation(characterRigBranch.getWorldMatrix(0), jointScratch);
        centreScratch.set(0f, 0f, 0f);

        if (bones.size() > 0) {

            ArpgObjectStruct first = bones.get(0).getAsObject();
            String key = EditorSetting.CHARACTER_EDITOR_FIELD_POSITION;

            centreScratch.set(
                    CharacterViewUtility.getComponent(first, key, 0, 0f),
                    CharacterViewUtility.getComponent(first, key, 1, 0f),
                    CharacterViewUtility.getComponent(first, key, 2, 0f));
        }

        out.setRoot(true);
        refuseOverLimit(stampHandle.getMesh().getFaceCount(), out);
        out.setTarget(jointScratch.x, jointScratch.y, jointScratch.z, 0f, 1f, 0f, 0f, false);
        out.setHost(characterRigBranch.getBoneName(0), "", 0);
        out.getRotation().set(1f);

        Matrix4 transform = out.getTransform();

        transform.set(scale);
        transform.val[15] = 1f;
        transform.val[12] = jointScratch.x - centreScratch.x * scale;
        transform.val[13] = jointScratch.y - centreScratch.y * scale;
        transform.val[14] = jointScratch.z - centreScratch.z * scale;
    }

    // The same part placed mirrored across the body: its point, normal and turn reflected in x, its side swapped,
    // joining the open loop there when one lies within its radius, a hole cut otherwise
    void resolveTwin(CharacterMeshDocumentInstance document, CharacterPartPlacementStruct placement,
            CharacterPartPlacementStruct out) {

        sync(document);
        out.set(placement);

        Vector3 point = placement.getPoint();
        Vector3 normal = placement.getNormal();
        RiggedMeshStruct mesh = document.getMesh();
        String hostName = CharacterMeshUtility.toMirrorBoneName(placement.getHostBoneName());

        target.set(-point.x, point.y, point.z);

        int loop = findLoopNear(target, Math.max(placement.getRadius(), EditorSetting.CHARACTER_STAMP_RADIUS_MIN));

        out.setTarget(
                target.x,
                target.y,
                target.z,
                -normal.x,
                normal.y,
                normal.z,
                placement.getRadius(),
                loop != EngineSetting.INDEX_NOT_FOUND);
        out.setHost(
                mesh.getRigHandle().hasBone(hostName) ? hostName : placement.getHostBoneName(),
                CharacterMeshUtility.toMirrorBoneName(placement.getSide()),
                placement.getAttachPoint());
        out.setMirrored(!placement.isMirrored());

        if (placement.getStampHandle().hasFur()) {
            characterFurBranch.resolveCards(mesh, out);
            refuseFur(mesh, out, placement.getFurCount());
            return;
        }

        refuseOverLimit(mesh.getFaceCount() + placement.getStampHandle().getMesh().getFaceCount() * 2, out);
        CharacterPoseUtility.reflectX(placement.getRotation(), out.getRotation());

        float[] m = out.getTransform().val;

        for (int column = 0; column < 4; column++)
            m[column * 4] = -m[column * 4];
    }

    // Fur laid over the patch about the point aimed at, its side the point's, no bone hosting it
    private void resolveFur(RiggedMeshStruct mesh, Vector3 point, Vector3 normal, CharacterPartPlacementStruct out) {

        out.setTarget(
                point.x,
                point.y,
                point.z,
                normal.x,
                normal.y,
                normal.z,
                out.getStampHandle().getFur().getRadius() * out.getScale(),
                false);
        out.setHost("", resolveSide(point.x), EngineSetting.INDEX_NOT_FOUND);
        characterFurBranch.resolveCards(mesh, out);
        refuseFur(mesh, out, 0);
    }

    // Fur refused where it lays no card, or where its cards, with any its twin lays, would pass the mesh's limit
    private void refuseFur(RiggedMeshStruct mesh, CharacterPartPlacementStruct out, int twinCards) {

        if (out.getFurCount() == 0)
            out.refuse(EditorSetting.CHARACTER_FUR_MESSAGE_NONE);

        int cardFaces = out.getStampHandle().getMesh().getFaceCount();

        refuseOverLimit(mesh.getFaceCount() + (out.getFurCount() + twinCards) * cardFaces, out);
    }

    // A placement refused when the body would hold more faces than the mesh's limit; the joining itself is held to
    // the limit exactly, with the hole cut and the bridge laid
    private void refuseOverLimit(int faces, CharacterPartPlacementStruct out) {

        if (!meshManager.fitsRiggedMeshLimit(faces))
            out.refuse(EditorSetting.CHARACTER_EDITOR_MESSAGE_MESH_LIMIT);
    }

    // Compose \\

    // The stamp's space carried into the bind pose: mirrored where it is, scaled, its plug turned to face into the
    // body, twisted about that axis, tilted, and set on the point
    private void compose(StampSocketStruct plug, Matrix4 tilt, CharacterPartPlacementStruct out) {

        Vector3 normal = out.getNormal();
        float sign = out.isMirrored() ? -1f : 1f;

        plugNormal.set(plug.getNormal()).multiply(sign, 1f, 1f);
        target.set(-normal.x, -normal.y, -normal.z);
        resolveAlignment(plugNormal, target, align);
        CharacterPoseUtility.toAxisRotation(target, (float) Math.toRadians(
                toolManager.getOption(ToolCanvas.CHARACTER_SCULPT, ToolOption.TURN)), twist);

        Matrix4 rotation = out.getRotation();

        rotation.set(tilt).multiply(twist).multiply(align);

        float scale = out.getScale();
        float[] r = rotation.val;
        float[] m = out.getTransform().val;
        Vector3 point = out.getPoint();

        for (int column = 0; column < 3; column++) {

            float columnScale = scale * (column == 0 ? sign : 1f);

            m[column * 4] = r[column * 4] * columnScale;
            m[column * 4 + 1] = r[column * 4 + 1] * columnScale;
            m[column * 4 + 2] = r[column * 4 + 2] * columnScale;
            m[column * 4 + 3] = 0f;
        }

        m[12] = point.x;
        m[13] = point.y;
        m[14] = point.z;
        m[15] = 1f;
    }

    // The shortest turn carrying one unit direction onto another, about the axis square to both; half a turn about
    // any axis square to the first when they point apart
    private void resolveAlignment(Vector3 from, Vector3 to, Matrix4 out) {

        float cosine = Math.max(-1f, Math.min(1f, from.dot(to)));

        axis.set(
                from.y * to.z - from.z * to.y,
                from.z * to.x - from.x * to.z,
                from.x * to.y - from.y * to.x);

        if (axis.length() > EditorSetting.CHARACTER_VIEW_RAY_EPSILON) {
            CharacterPoseUtility.toAxisRotation(axis.normalize(), (float) Math.acos(cosine), out);
            return;
        }

        if (cosine > 0f) {
            out.set(1f);
            return;
        }

        axis.set(Math.abs(from.y) < EditorSetting.CHARACTER_STAMP_PARALLEL_COSINE ? 0f : 1f,
                Math.abs(from.y) < EditorSetting.CHARACTER_STAMP_PARALLEL_COSINE ? 1f : 0f, 0f);
        axis.set(
                from.y * axis.z - from.z * axis.y,
                from.z * axis.x - from.x * axis.z,
                from.x * axis.y - from.y * axis.x).normalize();
        CharacterPoseUtility.toAxisRotation(axis, (float) Math.PI, out);
    }

    // Sides \\

    // The side a point stands on, by its bones' suffix, none within the centre's reach of the mirror plane
    private String resolveSide(float x) {

        if (Math.abs(x) <= rigHeight * EditorSetting.CHARACTER_STAMP_CENTRE_SHARE)
            return "";

        return CharacterEditorSide.LEFT.holdsX(x)
                ? CharacterEditorSide.LEFT.getSuffix()
                : CharacterEditorSide.RIGHT.getSuffix();
    }

    // The side a stamp was cut on, its first sided top bone's; none for a part of the centre, whose own twins (a
    // head's eyes) hang below a top bone of no side
    private String resolveStampSide(StampHandle stampHandle) {

        ArpgArrayStruct bones = stampHandle.getBonesArpg();

        for (int i = 0; i < bones.size(); i++) {

            ArpgObjectStruct boneArpg = bones.get(i).getAsObject();
            String side = CharacterMeshUtility.toSideSuffix(
                    ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_NAME, ""));

            if (!side.isEmpty() && ArpgUtility.getString(boneArpg, EditorSetting.CHARACTER_RIG_FIELD_PARENT, "")
                    .isEmpty())
                return side;
        }

        return "";
    }

    // Utility \\

    // The bone weighing most over a face's corners
    private int resolveFaceBone(RiggedMeshStruct mesh, int faceIndex) {

        if (faceIndex == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        if (boneTotals.length < mesh.getRigHandle().getBoneCount())
            this.boneTotals = new float[mesh.getRigHandle().getBoneCount()];

        Arrays.fill(boneTotals, 0f);

        int heaviest = EngineSetting.INDEX_NOT_FOUND;

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

            int v = mesh.getFaceVertex(faceIndex, c);

            for (int n = 0; n < mesh.getInfluenceCount(v); n++) {

                int bone = mesh.getInfluenceBone(v, n);

                boneTotals[bone] += mesh.getInfluenceWeight(v, n);

                if (heaviest == EngineSetting.INDEX_NOT_FOUND || boneTotals[bone] > boneTotals[heaviest])
                    heaviest = bone;
            }
        }

        return heaviest;
    }

    private float resolveDistance(int attachPoint, Vector3 point) {
        getAttachPoint(attachPoint, jointScratch);
        return (float) Math.sqrt(CharacterMeshUtility.distanceSquared(jointScratch, point));
    }
}
