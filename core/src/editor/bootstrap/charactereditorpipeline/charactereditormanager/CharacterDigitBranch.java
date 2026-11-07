package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterDigitStruct;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterPoseUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterDigitBranch extends BranchPackage {

    /*
     * Finds the digits of a hand or a foot in its body mesh, the way an
     * auto-rigger does, for the rig tools to lay bone chains on and the weight
     * tools to weigh onto them. The vertices a bone and the bones below it
     * carry are swept from the farthest out back toward the bone's joint,
     * joined face by face as they are reached: each digit is born at its tip
     * and ends where it meets another, and two long branches meeting at a web
     * are both digits, the oldest branch thereafter the palm. A digit is laid
     * on three joints from a little past its web to its tip; five are named
     * thumb (the one meeting the palm nearest the joint), then index, middle,
     * ring and pinky by their distance from it, any other count by number;
     * the side is the rig tools' to add. A
     * region with no digits apart, a foot whose toes are one block, takes one
     * toe at the ball, a set share of the way along its length. Run only when
     * asked, never per frame.
     */

    // Search — main thread only
    private RiggedMeshStruct mesh;
    private boolean[] held;
    private float[] depth;
    private int[] parent;
    private float[] birth;
    private boolean[] palm;
    private boolean[] reached;
    private int[][] neighbours;
    private ObjectArrayList<IntArrayList> found;
    private float[] foundBaseDepths;

    // Detect \\

    // The digits below a bone of a body mesh, found among its own chain's vertices
    void detect(RiggedMeshStruct mesh, int rootBone, ObjectArrayList<CharacterDigitStruct> digits) {

        this.mesh = mesh;
        digits.clear();

        Vector3 joint = new Vector3();
        CharacterPoseUtility.getTranslation(mesh.getRigHandle().getBindWorldMatrix(rootBone), joint);

        resolveHeld(mesh.getRigHandle(), rootBone);
        Vector3 axis = resolveAxis(joint);

        if (axis == null) {
            forget();
            return;
        }

        resolveDepths(joint, axis);
        sweep();

        if (found.isEmpty())
            addBall(joint, digits);
        else
            addDigits(axis, digits);

        forget();
    }

    // The vertices the bone and every bone below it carry at least the pick share of
    private void resolveHeld(RigHandle rigHandle, int rootBone) {

        boolean[] chain = new boolean[rigHandle.getBoneCount()];

        for (int i = 0; i < chain.length; i++) {

            int boneParent = rigHandle.getBone(i).getParentIndex();
            chain[i] = i == rootBone || (boneParent != EngineSetting.INDEX_NOT_FOUND && chain[boneParent]);
        }

        this.held = new boolean[mesh.getVertexCount()];

        for (int v = 0; v < held.length; v++) {

            float share = 0f;

            for (int i = 0; i < mesh.getInfluenceCount(v); i++)
                if (chain[mesh.getInfluenceBone(v, i)])
                    share += mesh.getInfluenceWeight(v, i);

            held[v] = share >= EditorSetting.CHARACTER_REGION_PICK_SHARE;
        }
    }

    // From the joint toward the middle of the held vertices, null when it holds none
    private Vector3 resolveAxis(Vector3 joint) {

        Vector3 centre = new Vector3();
        int count = 0;

        for (int v = 0; v < held.length; v++)
            if (held[v]) {
                centre.add(mesh.getX(v), mesh.getY(v), mesh.getZ(v));
                count++;
            }

        if (count == 0)
            return null;

        centre.divide(count).subtract(joint);
        float length = centre.length();

        return length > 0f ? centre.divide(length) : null;
    }

    private void resolveDepths(Vector3 joint, Vector3 axis) {

        this.depth = new float[held.length];

        for (int v = 0; v < held.length; v++)
            depth[v] = (mesh.getX(v) - joint.x) * axis.x
                    + (mesh.getY(v) - joint.y) * axis.y
                    + (mesh.getZ(v) - joint.z) * axis.z;
    }

    // Sweep \\

    // Every held vertex reached from the farthest in, joined to those reached beside it, branches recorded as they end
    private void sweep() {

        buildNeighbours();

        int vertexCount = held.length;
        IntArrayList order = new IntArrayList();

        for (int v = 0; v < vertexCount; v++)
            if (held[v])
                order.add(v);

        int[] sorted = order.toIntArray();
        IntArrays.quickSort(sorted, (a, b) -> Float.compare(depth[b], depth[a]));

        this.parent = new int[vertexCount];
        this.birth = new float[vertexCount];
        this.palm = new boolean[vertexCount];
        this.reached = new boolean[vertexCount];
        this.found = new ObjectArrayList<>();

        FloatArrayList baseDepths = new FloatArrayList();

        for (int vertex : sorted) {

            parent[vertex] = vertex;
            birth[vertex] = depth[vertex];
            reached[vertex] = true;

            for (int neighbour : neighbours[vertex])
                if (reached[neighbour])
                    join(find(neighbour), find(vertex), depth[vertex], baseDepths);
        }

        this.foundBaseDepths = baseDepths.toFloatArray();
    }

    // Two branches meeting: the younger ends, and when both are long enough both are digits and the elder is palm
    private void join(int a, int b, float meeting, FloatArrayList baseDepths) {

        if (a == b)
            return;

        int elder = birth[a] >= birth[b] ? a : b;
        int younger = elder == a ? b : a;
        boolean youngerLong = birth[younger] - meeting >= EditorSetting.CHARACTER_RIG_DIGIT_MIN_LENGTH;
        boolean elderLong = birth[elder] - meeting >= EditorSetting.CHARACTER_RIG_DIGIT_MIN_LENGTH;

        if (youngerLong && !palm[younger])
            record(younger, meeting, baseDepths);

        if (youngerLong && elderLong && !palm[elder])
            record(elder, meeting, baseDepths);

        parent[younger] = elder;
        palm[elder] = palm[elder] || palm[younger] || youngerLong;
    }

    private void record(int root, float meeting, FloatArrayList baseDepths) {

        IntArrayList members = new IntArrayList();

        for (int v = 0; v < held.length; v++)
            if (reached[v] && find(v) == root)
                members.add(v);

        found.add(members);
        baseDepths.add(meeting);
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

    // Each held vertex's neighbours across the faces whose corners are all held
    private void buildNeighbours() {

        IntArrayList[] lists = new IntArrayList[held.length];

        for (int f = 0; f < mesh.getFaceCount(); f++) {

            if (!isHeldFace(f))
                continue;

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int a = mesh.getFaceVertex(f, c);
                int b = mesh.getFaceVertex(f, (c + 1) % EngineSetting.QUAD_VERTEX_COUNT);

                if (a == b)
                    continue;

                link(lists, a, b);
                link(lists, b, a);
            }
        }

        this.neighbours = new int[held.length][];

        for (int v = 0; v < held.length; v++)
            neighbours[v] = lists[v] != null ? lists[v].toIntArray() : IntArrays.EMPTY_ARRAY;
    }

    private boolean isHeldFace(int faceIndex) {

        for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++)
            if (!held[mesh.getFaceVertex(faceIndex, c)])
                return false;

        return true;
    }

    private void link(IntArrayList[] lists, int from, int to) {

        if (lists[from] == null)
            lists[from] = new IntArrayList();

        if (!lists[from].contains(to))
            lists[from].add(to);
    }

    // Digits \\

    private void addDigits(Vector3 axis, ObjectArrayList<CharacterDigitStruct> digits) {

        ObjectArrayList<CharacterDigitStruct> laid = new ObjectArrayList<>();
        float[] baseDepths = new float[found.size()];

        for (int i = 0; i < found.size(); i++) {
            laid.add(layDigit(found.get(i), foundBaseDepths[i], axis));
            baseDepths[i] = foundBaseDepths[i];
        }

        int[] order = nameOrder(laid, baseDepths);

        for (int rank = 0; rank < order.length; rank++) {

            CharacterDigitStruct digit = laid.get(order[rank]);

            digits.add(new CharacterDigitStruct(
                    resolveDigitName(rank, order.length),
                    digit.getJoints(),
                    digit.getTip(),
                    digit.getThickness(),
                    digit.getVertices()));
        }
    }

    // A digit's chain from a little past its web to its tip along the line its vertices lie on
    private CharacterDigitStruct layDigit(IntArrayList members, float baseDepth, Vector3 rootAxis) {

        Vector3 centre = centroid(members);
        Vector3 line = principalAxis(members, centre);

        if (line.dot(rootAxis) < 0f)
            line.multiply(-1f);

        float near = Float.MAX_VALUE;
        float far = -Float.MAX_VALUE;
        float spread = 0f;

        for (int i = 0; i < members.size(); i++) {

            float along = project(members.getInt(i), centre, line);

            near = Math.min(near, along);
            far = Math.max(far, along);
        }

        Vector3 root = new Vector3(line).multiply(near - EditorSetting.CHARACTER_RIG_DIGIT_ROOT_REACH).add(centre);
        Vector3 tip = new Vector3(line).multiply(far).add(centre);
        float length = far - near + EditorSetting.CHARACTER_RIG_DIGIT_ROOT_REACH;
        ObjectArrayList<Vector3> joints = new ObjectArrayList<>();

        for (float share : EditorSetting.CHARACTER_RIG_DIGIT_SHARES)
            joints.add(new Vector3(line).multiply(length * share).add(root));

        for (int i = 0; i < members.size(); i++)
            spread += distanceFromLine(members.getInt(i), centre, line);

        return new CharacterDigitStruct(
                null,
                joints,
                tip,
                spread / members.size() * EditorSetting.CHARACTER_RIG_DIGIT_THICKNESS_SHARE,
                members);
    }

    // Five digits run thumb first, the one meeting the palm nearest the joint, then by distance from it
    private int[] nameOrder(ObjectArrayList<CharacterDigitStruct> laid, float[] baseDepths) {

        int count = laid.size();
        int first = 0;

        for (int i = 1; i < count; i++)
            if (baseDepths[i] < baseDepths[first])
                first = i;

        Vector3 thumbRoot = laid.get(first).getJoint(0);
        int[] order = new int[count];
        float[] distance = new float[count];

        for (int i = 0; i < count; i++) {
            order[i] = i;
            distance[i] = i == first ? -1f : new Vector3(laid.get(i).getJoint(0)).subtract(thumbRoot).length();
        }

        IntArrays.quickSort(order, (a, b) -> Float.compare(distance[a], distance[b]));
        return order;
    }

    private String resolveDigitName(int rank, int count) {

        String[] names = EditorSetting.CHARACTER_RIG_DIGIT_NAMES;

        return count == names.length
                ? names[rank]
                : EditorSetting.CHARACTER_RIG_DIGIT_GENERIC + (rank + 1);
    }

    // Ball \\

    // One toe at the ball: the share of the way along the region's length, centred in its cross-section there
    private void addBall(Vector3 joint, ObjectArrayList<CharacterDigitStruct> digits) {

        IntArrayList members = new IntArrayList();

        for (int v = 0; v < held.length; v++)
            if (held[v])
                members.add(v);

        Vector3 centre = centroid(members);
        Vector3 line = principalAxis(members, centre);

        if (new Vector3(centre).subtract(joint).dot(line) < 0f)
            line.multiply(-1f);

        float back = Float.MAX_VALUE;
        float front = -Float.MAX_VALUE;

        for (int i = 0; i < members.size(); i++) {

            float along = project(members.getInt(i), centre, line);

            back = Math.min(back, along);
            front = Math.max(front, along);
        }

        float ball = back + (front - back) * EditorSetting.CHARACTER_RIG_BALL_SHARE;
        Vector3 ballCentre = new Vector3();
        IntArrayList past = new IntArrayList();
        int slab = 0;
        float spread = 0f;

        for (int i = 0; i < members.size(); i++) {

            int v = members.getInt(i);
            float along = project(v, centre, line);

            if (along > ball)
                past.add(v);

            if (Math.abs(along - ball) <= EditorSetting.CHARACTER_RIG_BALL_SLAB) {
                ballCentre.add(mesh.getX(v), mesh.getY(v), mesh.getZ(v));
                slab++;
            }
        }

        if (slab == 0)
            return;

        ballCentre.divide(slab);

        for (int i = 0; i < past.size(); i++)
            spread += distanceFromLine(past.getInt(i), ballCentre, line);

        ObjectArrayList<Vector3> joints = new ObjectArrayList<>();
        joints.add(ballCentre);

        digits.add(new CharacterDigitStruct(
                EditorSetting.CHARACTER_RIG_TOE_NAME,
                joints,
                new Vector3(line).multiply(front - ball).add(ballCentre),
                past.isEmpty() ? 0f : spread / past.size() * EditorSetting.CHARACTER_RIG_DIGIT_THICKNESS_SHARE,
                past));
    }

    // Geometry \\

    private Vector3 centroid(IntArrayList members) {

        Vector3 centre = new Vector3();

        for (int i = 0; i < members.size(); i++) {
            int v = members.getInt(i);
            centre.add(mesh.getX(v), mesh.getY(v), mesh.getZ(v));
        }

        return centre.divide(members.size());
    }

    // The line the vertices spread farthest along, by power iteration on their covariance
    private Vector3 principalAxis(IntArrayList members, Vector3 centre) {

        float xx = 0f;
        float xy = 0f;
        float xz = 0f;
        float yy = 0f;
        float yz = 0f;
        float zz = 0f;

        for (int i = 0; i < members.size(); i++) {

            int v = members.getInt(i);
            float x = mesh.getX(v) - centre.x;
            float y = mesh.getY(v) - centre.y;
            float z = mesh.getZ(v) - centre.z;

            xx += x * x;
            xy += x * y;
            xz += x * z;
            yy += y * y;
            yz += y * z;
            zz += z * z;
        }

        Vector3 axis = new Vector3(1f, 1f, 1f);

        for (int i = 0; i < EditorSetting.CHARACTER_RIG_AXIS_ITERATIONS; i++) {

            float x = xx * axis.x + xy * axis.y + xz * axis.z;
            float y = xy * axis.x + yy * axis.y + yz * axis.z;
            float z = xz * axis.x + yz * axis.y + zz * axis.z;
            float length = (float) Math.sqrt(x * x + y * y + z * z);

            if (length <= 0f)
                break;

            axis.set(x / length, y / length, z / length);
        }

        return axis;
    }

    private float project(int vertexIndex, Vector3 centre, Vector3 line) {
        return (mesh.getX(vertexIndex) - centre.x) * line.x
                + (mesh.getY(vertexIndex) - centre.y) * line.y
                + (mesh.getZ(vertexIndex) - centre.z) * line.z;
    }

    private float distanceFromLine(int vertexIndex, Vector3 centre, Vector3 line) {

        float along = project(vertexIndex, centre, line);
        float x = mesh.getX(vertexIndex) - centre.x - line.x * along;
        float y = mesh.getY(vertexIndex) - centre.y - line.y * along;
        float z = mesh.getZ(vertexIndex) - centre.z - line.z * along;

        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private void forget() {
        this.mesh = null;
        this.held = null;
        this.depth = null;
        this.parent = null;
        this.birth = null;
        this.palm = null;
        this.reached = null;
        this.neighbours = null;
        this.found = null;
        this.foundBaseDepths = null;
    }
}
