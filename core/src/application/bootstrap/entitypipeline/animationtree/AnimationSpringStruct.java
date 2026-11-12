package application.bootstrap.entitypipeline.animationtree;

import application.bootstrap.geometrypipeline.rig.RigColliderStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rig.RigMathUtility;
import application.bootstrap.geometrypipeline.rig.RigSpringChainStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class AnimationSpringStruct extends StructPackage {

    /*
     * The live simulation of every spring chain one posed body carries. Each
     * joint after a chain's first is a point kept in the body's frame — the
     * world's axes about where the body stands, turned and sized with it —
     * so it keeps its own momentum as the body turns, runs and stops. Each
     * substep a joint is pulled toward where the pose holds it from the
     * joint before it, its swing against that joint damped, drawn down by
     * gravity and held back by the air's drag, then kept its bone's length
     * from the joint before it and within a cone about the way the pose
     * holds its bone, so no jolt flips a lock over, then pushed out of every
     * collider the chain names — the bone between it and the joint before
     * it a capsule of the chain's radius, each collider placed where the
     * pose just composed carries its bone — and held its length again; the
     * bones are then turned about their joints onto the simulated points. A
     * chain
     * starts afresh at the pose when it is first stepped, switched back on,
     * or its body is moved farther than a step could carry it; an update
     * longer than the springs simulate carries them rigidly with the pose for
     * the time past, so a hitch never flings them. Every array is sized once
     * for the rig, so stepping never allocates.
     */

    // Rig
    private final RigHandle rigHandle;

    // Chains — whether each runs, and whether it starts again at the pose
    private final boolean[] chainActive;
    private final boolean[] chainReset;

    // Joints — per chain, three floats per joint in the body's frame: where it is, where it stood a substep ago,
    // where the pose holds it now and where the pose held it at the last step
    private final float[][] positions;
    private final float[][] previous;
    private final float[][] posed;
    private final float[][] lastPosed;

    // Colliders — per rig collider, both ends and the radius in the body's frame where the pose last held them
    private final float[] colliderEnds;
    private final float[] colliderRadii;

    // Frame — how the body stands turned and sized in the world, and back
    private final Matrix4 frame;
    private final Matrix4 frameInverse;

    // Scratch
    private final Vector3 jointScratch;
    private final Vector3 fromScratch;
    private final Vector3 toScratch;
    private final Matrix4 turnScratch;
    private final Matrix4 matrixScratch;
    private float closestBone;
    private float closestCollider;

    // Constructor \\

    public AnimationSpringStruct(RigHandle rigHandle) {

        int chainCount = rigHandle.getSpringChainCount();

        // Rig
        this.rigHandle = rigHandle;

        // Chains
        this.chainActive = new boolean[chainCount];
        this.chainReset = new boolean[chainCount];

        // Joints
        this.positions = new float[chainCount][];
        this.previous = new float[chainCount][];
        this.posed = new float[chainCount][];
        this.lastPosed = new float[chainCount][];

        for (int c = 0; c < chainCount; c++) {

            int floats = rigHandle.getSpringChain(c).getBoneCount() * EngineSetting.RIGGED_MESH_POSITION_FLOATS;

            positions[c] = new float[floats];
            previous[c] = new float[floats];
            posed[c] = new float[floats];
            lastPosed[c] = new float[floats];
            chainActive[c] = true;
            chainReset[c] = true;
        }

        // Colliders
        this.colliderEnds = new float[EngineSetting.RIG_COLLIDER_MAX * EngineSetting.RIG_COLLIDER_END_FLOATS];
        this.colliderRadii = new float[EngineSetting.RIG_COLLIDER_MAX];

        // Frame
        this.frame = new Matrix4(1f);
        this.frameInverse = new Matrix4(1f);

        // Scratch
        this.jointScratch = new Vector3();
        this.fromScratch = new Vector3();
        this.toScratch = new Vector3();
        this.turnScratch = new Matrix4();
        this.matrixScratch = new Matrix4();
    }

    // Chains \\

    public boolean hasChains() {
        return chainActive.length > 0;
    }

    // Whether a chain runs; one switched back on starts again at the pose
    public void setActive(int chainIndex, boolean active) {

        if (active && !chainActive[chainIndex])
            chainReset[chainIndex] = true;

        chainActive[chainIndex] = active;
    }

    public void reset() {

        for (int c = 0; c < chainReset.length; c++)
            chainReset[c] = true;
    }

    // Frame \\

    // How the body now stands turned and sized in the world, and how far it travelled since the last frame given:
    // every simulated point stays where it was in the world while the body moves out from under it
    public void setFrame(Matrix4 bodyFrame, float travelX, float travelY, float travelZ) {

        frame.set(bodyFrame);
        frameInverse.set(bodyFrame).inverse();

        if (travelX * travelX + travelY * travelY + travelZ * travelZ
                > EngineSetting.RIG_SPRING_RESET_DISTANCE * EngineSetting.RIG_SPRING_RESET_DISTANCE) {
            reset();
            return;
        }

        for (int c = 0; c < positions.length; c++)
            for (int i = 0; i < positions[c].length; i += EngineSetting.RIGGED_MESH_POSITION_FLOATS) {
                shift(positions[c], i, travelX, travelY, travelZ);
                shift(previous[c], i, travelX, travelY, travelZ);
                shift(lastPosed[c], i, travelX, travelY, travelZ);
            }
    }

    private static void shift(float[] points, int i, float x, float y, float z) {
        points[i] -= x;
        points[i + 1] -= y;
        points[i + 2] -= z;
    }

    // Capture \\

    // Where the pose just composed holds every joint and every collider, in the body's frame
    public void capture(Matrix4[] worldMatrices) {

        for (int k = 0; k < rigHandle.getColliderCount(); k++)
            captureCollider(rigHandle.getCollider(k), worldMatrices, k);

        for (int c = 0; c < posed.length; c++) {

            if (!chainActive[c])
                continue;

            RigSpringChainStruct chain = rigHandle.getSpringChain(c);

            for (int j = 0; j < chain.getBoneCount(); j++)
                writeFramed(worldMatrices[chain.getBone(j)], posed[c], j * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        }
    }

    private void writeFramed(Matrix4 world, float[] out, int i) {
        writeFramedPoint(world.val[12], world.val[13], world.val[14], out, i);
    }

    private void writeFramedPoint(float x, float y, float z, float[] out, int i) {

        float[] m = frame.val;

        out[i] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out[i + 1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out[i + 2] = m[2] * x + m[6] * y + m[10] * z + m[14];
    }

    // A collider's ends carried by its bone as posed into the body's frame, its radius sized with the bone and body
    private void captureCollider(RigColliderStruct collider, Matrix4[] worldMatrices, int colliderIndex) {

        float[] w = worldMatrices[collider.getBoneIndex()].val;
        float[] m = frame.val;
        int i = colliderIndex * EngineSetting.RIG_COLLIDER_END_FLOATS;

        writeCarried(w, collider.getStart().x, collider.getStart().y, collider.getStart().z, i);
        writeCarried(w, collider.getEnd().x, collider.getEnd().y, collider.getEnd().z,
                i + EngineSetting.RIGGED_MESH_POSITION_FLOATS);

        float ax = m[0] * w[0] + m[4] * w[1] + m[8] * w[2];
        float ay = m[1] * w[0] + m[5] * w[1] + m[9] * w[2];
        float az = m[2] * w[0] + m[6] * w[1] + m[10] * w[2];

        colliderRadii[colliderIndex] = collider.getRadius() * (float) Math.sqrt(ax * ax + ay * ay + az * az);
    }

    private void writeCarried(float[] w, float x, float y, float z, int i) {
        writeFramedPoint(
                w[0] * x + w[4] * y + w[8] * z + w[12],
                w[1] * x + w[5] * y + w[9] * z + w[13],
                w[2] * x + w[6] * y + w[10] * z + w[14],
                colliderEnds,
                i);
    }

    // Step \\

    // Advances every running chain by the time given in equal substeps, one starting afresh taking the pose; time
    // past the most simulated is ridden with the pose
    public void step(float deltaTime) {

        float time = Math.min(deltaTime, EngineSetting.RIG_SPRING_MAX_DELTA);

        if (time <= 0f)
            return;

        float ridden = 1f - time / deltaTime;

        int substeps = Math.clamp(
                (int) Math.ceil(time / EngineSetting.RIG_SPRING_STEP), 1, EngineSetting.RIG_SPRING_MAX_STEPS);
        float substep = time / substeps;

        for (int c = 0; c < positions.length; c++) {

            if (!chainActive[c])
                continue;

            if (chainReset[c]) {
                settle(c);
                continue;
            }

            if (ridden > 0f)
                ride(c, ridden);

            stepChain(rigHandle.getSpringChain(c), c, substeps, substep);
            System.arraycopy(posed[c], 0, lastPosed[c], 0, posed[c].length);
        }
    }

    private void settle(int chainIndex) {

        System.arraycopy(posed[chainIndex], 0, positions[chainIndex], 0, posed[chainIndex].length);
        System.arraycopy(posed[chainIndex], 0, previous[chainIndex], 0, posed[chainIndex].length);
        System.arraycopy(posed[chainIndex], 0, lastPosed[chainIndex], 0, posed[chainIndex].length);
        chainReset[chainIndex] = false;
    }

    // A share of the way the pose moved since the last step taken by the chain rigidly, as if simulated with it
    private void ride(int chainIndex, float share) {

        float[] p = positions[chainIndex];
        float[] q = previous[chainIndex];
        float[] now = posed[chainIndex];
        float[] then = lastPosed[chainIndex];

        for (int k = 0; k < p.length; k++) {

            float carried = (now[k] - then[k]) * share;

            then[k] += carried;
            p[k] += carried;
            q[k] += carried;
        }
    }

    // Each substep moves the pose a share of the way from where it stood to where it stands now; the first joint
    // rides the pose, every one after it is simulated, root to tip
    private void stepChain(RigSpringChainStruct chain, int chainIndex, int substeps, float substep) {

        float[] p = positions[chainIndex];
        float[] q = previous[chainIndex];
        float[] now = posed[chainIndex];
        float[] then = lastPosed[chainIndex];
        float keep = (float) Math.exp(-chain.getDamping() * substep);
        float hold = (float) Math.exp(-chain.getDrag() * substep);
        float pull = chain.getStiffness() * substep * substep;
        float fall = chain.getGravity() * substep * substep;
        float share = 1f / substeps;

        for (int s = 1; s <= substeps; s++) {

            float t = s * share;

            for (int axis = 0; axis < EngineSetting.RIGGED_MESH_POSITION_FLOATS; axis++) {
                q[axis] = p[axis];
                p[axis] = then[axis] + (now[axis] - then[axis]) * t;
            }

            for (int i = EngineSetting.RIGGED_MESH_POSITION_FLOATS; i < p.length;
                    i += EngineSetting.RIGGED_MESH_POSITION_FLOATS) {

                stepJoint(p, q, now, then, i, t, keep, hold, pull, fall);

                if (collide(chain, p, i))
                    holdLength(p, now, i, i - EngineSetting.RIGGED_MESH_POSITION_FLOATS);
            }
        }
    }

    private void stepJoint(
            float[] p,
            float[] q,
            float[] now,
            float[] then,
            int i,
            float t,
            float keep,
            float hold,
            float pull,
            float fall) {

        int parent = i - EngineSetting.RIGGED_MESH_POSITION_FLOATS;

        for (int axis = 0; axis < EngineSetting.RIGGED_MESH_POSITION_FLOATS; axis++) {

            int k = i + axis;
            float carried = p[parent + axis] - q[parent + axis];
            float posedHere = then[k] + (now[k] - then[k]) * t;
            float posedParent = then[parent + axis] + (now[parent + axis] - then[parent + axis]) * t;
            float target = posedHere + p[parent + axis] - posedParent;
            float velocity = (carried + (p[k] - q[k] - carried) * keep) * hold;

            float moved = p[k] + velocity;

            q[k] = p[k];
            p[k] = moved + (target - moved) * pull;
        }

        p[i + 1] -= fall;
        holdLength(p, now, i, parent);
    }

    // A joint kept the length the pose gives its bone from the joint before it, swung no farther from the way the
    // pose holds that bone than the swing cone allows
    private void holdLength(float[] p, float[] now, int i, int parent) {

        float rx = now[i] - now[parent];
        float ry = now[i + 1] - now[parent + 1];
        float rz = now[i + 2] - now[parent + 2];
        float rest = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        float dx = p[i] - p[parent];
        float dy = p[i + 1] - p[parent + 1];
        float dz = p[i + 2] - p[parent + 2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (length <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON || rest <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON)
            return;

        rx /= rest;
        ry /= rest;
        rz /= rest;
        dx /= length;
        dy /= length;
        dz /= length;

        float cos = dx * rx + dy * ry + dz * rz;

        if (cos < EngineSetting.RIG_SPRING_SWING_COSINE) {

            float ox = dx - rx * cos;
            float oy = dy - ry * cos;
            float oz = dz - rz * cos;
            float off = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);

            if (off > EngineSetting.RIG_SPRING_DIRECTION_EPSILON) {

                float cone = EngineSetting.RIG_SPRING_SWING_COSINE;
                float side = (float) Math.sqrt(1f - cone * cone) / off;

                dx = rx * cone + ox * side;
                dy = ry * cone + oy * side;
                dz = rz * cone + oz * side;
            }
        }

        p[i] = p[parent] + dx * rest;
        p[i + 1] = p[parent + 1] + dy * rest;
        p[i + 2] = p[parent + 2] + dz * rest;
    }

    // Collision \\

    // A joint pushed out of every collider its chain names, the bone from the joint before it swept by the chain's
    // radius; true when it moved
    private boolean collide(RigSpringChainStruct chain, float[] p, int i) {

        int parent = i - EngineSetting.RIGGED_MESH_POSITION_FLOATS;
        boolean pushed = false;

        for (int k = 0; k < chain.getColliderCount(); k++) {

            int collider = chain.getCollider(k);
            int start = collider * EngineSetting.RIG_COLLIDER_END_FLOATS;
            int end = start + EngineSetting.RIGGED_MESH_POSITION_FLOATS;
            float reach = colliderRadii[collider] + chain.getRadius();

            resolveClosest(p, parent, i, colliderEnds, start, end);

            if (closestBone < EngineSetting.RIG_COLLIDER_SEGMENT_SHARE_MIN)
                continue;

            float dx = p[parent] + (p[i] - p[parent]) * closestBone
                    - colliderEnds[start] - (colliderEnds[end] - colliderEnds[start]) * closestCollider;
            float dy = p[parent + 1] + (p[i + 1] - p[parent + 1]) * closestBone
                    - colliderEnds[start + 1] - (colliderEnds[end + 1] - colliderEnds[start + 1]) * closestCollider;
            float dz = p[parent + 2] + (p[i + 2] - p[parent + 2]) * closestBone
                    - colliderEnds[start + 2] - (colliderEnds[end + 2] - colliderEnds[start + 2]) * closestCollider;
            float distanceSquared = dx * dx + dy * dy + dz * dz;

            if (distanceSquared >= reach * reach)
                continue;

            float distance = (float) Math.sqrt(distanceSquared);

            if (distance <= EngineSetting.RIG_SPRING_DIRECTION_EPSILON)
                continue;

            float push = (reach - distance) / (distance * closestBone);

            p[i] += dx * push;
            p[i + 1] += dy * push;
            p[i + 2] += dz * push;
            pushed = true;
        }

        return pushed;
    }

    // Where a bone's segment and a collider's come closest, as shares of the way along each, a sphere's segment a
    // point
    private void resolveClosest(float[] p, int a0, int a1, float[] c, int b0, int b1) {

        float d1x = p[a1] - p[a0];
        float d1y = p[a1 + 1] - p[a0 + 1];
        float d1z = p[a1 + 2] - p[a0 + 2];
        float d2x = c[b1] - c[b0];
        float d2y = c[b1 + 1] - c[b0 + 1];
        float d2z = c[b1 + 2] - c[b0 + 2];
        float rx = p[a0] - c[b0];
        float ry = p[a0 + 1] - c[b0 + 1];
        float rz = p[a0 + 2] - c[b0 + 2];
        float a = d1x * d1x + d1y * d1y + d1z * d1z;
        float e = d2x * d2x + d2y * d2y + d2z * d2z;
        float f = d2x * rx + d2y * ry + d2z * rz;
        float epsilon = EngineSetting.RIG_SPRING_DIRECTION_EPSILON;

        if (a <= epsilon) {
            closestBone = 0f;
            closestCollider = e <= epsilon ? 0f : Math.clamp(f / e, 0f, 1f);
            return;
        }

        float cross = d1x * rx + d1y * ry + d1z * rz;

        if (e <= epsilon) {
            closestCollider = 0f;
            closestBone = Math.clamp(-cross / a, 0f, 1f);
            return;
        }

        float b = d1x * d2x + d1y * d2y + d1z * d2z;
        float denominator = a * e - b * b;

        closestBone = denominator > epsilon ? Math.clamp((b * f - cross * e) / denominator, 0f, 1f) : 0f;
        closestCollider = (b * closestBone + f) / e;

        if (closestCollider < 0f) {
            closestCollider = 0f;
            closestBone = Math.clamp(-cross / a, 0f, 1f);
        } else if (closestCollider > 1f) {
            closestCollider = 1f;
            closestBone = Math.clamp((b - cross) / a, 0f, 1f);
        }
    }

    // Apply \\

    // Every running chain's bones turned about their joints, root to tip, onto the points simulated for them, each
    // carrying every bone below it; a chain not yet stepped keeps the pose
    public void apply(Matrix4[] worldMatrices) {

        for (int c = 0; c < positions.length; c++) {

            if (!chainActive[c] || chainReset[c])
                continue;

            RigSpringChainStruct chain = rigHandle.getSpringChain(c);

            for (int j = 0; j < chain.getBoneCount() - 1; j++)
                turnBone(chain, worldMatrices, j, positions[c], (j + 1) * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        }
    }

    private void turnBone(RigSpringChainStruct chain, Matrix4[] worldMatrices, int joint, float[] p, int i) {

        Matrix4 world = worldMatrices[chain.getBone(joint)];
        Matrix4 child = worldMatrices[chain.getBone(joint + 1)];
        float[] m = frameInverse.val;

        jointScratch.set(world.val[12], world.val[13], world.val[14]);
        fromScratch.set(
                child.val[12] - jointScratch.x,
                child.val[13] - jointScratch.y,
                child.val[14] - jointScratch.z);
        toScratch.set(
                m[0] * p[i] + m[4] * p[i + 1] + m[8] * p[i + 2] + m[12] - jointScratch.x,
                m[1] * p[i] + m[5] * p[i + 1] + m[9] * p[i + 2] + m[13] - jointScratch.y,
                m[2] * p[i] + m[6] * p[i + 1] + m[10] * p[i + 2] + m[14] - jointScratch.z);

        RigMathUtility.setPivotTurn(turnScratch, jointScratch, fromScratch, toScratch);

        for (int bone : chain.getCarried(joint)) {
            matrixScratch.set(turnScratch).multiply(worldMatrices[bone]);
            worldMatrices[bone].set(matrixScratch);
        }
    }
}
