package application.bootstrap.entitypipeline.animationtree;

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
     * holds its bone, so no jolt flips a lock over; the bones are then turned
     * about their joints onto the simulated points. A chain
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

    // Frame — how the body stands turned and sized in the world, and back
    private final Matrix4 frame;
    private final Matrix4 frameInverse;

    // Scratch
    private final Vector3 jointScratch;
    private final Vector3 fromScratch;
    private final Vector3 toScratch;
    private final Matrix4 turnScratch;
    private final Matrix4 matrixScratch;

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

    // Where the pose just composed holds every joint, in the body's frame
    public void capture(Matrix4[] worldMatrices) {

        for (int c = 0; c < posed.length; c++) {

            if (!chainActive[c])
                continue;

            RigSpringChainStruct chain = rigHandle.getSpringChain(c);

            for (int j = 0; j < chain.getBoneCount(); j++)
                writeFramed(worldMatrices[chain.getBone(j)], posed[c], j * EngineSetting.RIGGED_MESH_POSITION_FLOATS);
        }
    }

    private void writeFramed(Matrix4 world, float[] out, int i) {

        float[] m = frame.val;
        float x = world.val[12];
        float y = world.val[13];
        float z = world.val[14];

        out[i] = m[0] * x + m[4] * y + m[8] * z + m[12];
        out[i + 1] = m[1] * x + m[5] * y + m[9] * z + m[13];
        out[i + 2] = m[2] * x + m[6] * y + m[10] * z + m[14];
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
                    i += EngineSetting.RIGGED_MESH_POSITION_FLOATS)
                stepJoint(p, q, now, then, i, t, keep, hold, pull, fall);
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
