package application.runtime.particle;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ParticlePoolStruct extends StructPackage {

    /*
     * A fixed pool of live particles, packed. Each holds its place in absolute blocks as doubles, so it stands still in
     * the world however far the camera wanders, and a run of floats its kind lays out for its own state. A particle
     * spawned takes the next free slot and one retired hands its slot to the last, so the live ones always fill the
     * front of the arrays and a walk over them touches nothing else. Nothing is allocated after construction.
     */

    // Capacity
    private final int capacity;
    private final int floatsPerParticle;
    private int count;

    // Place — absolute blocks
    private final double[] positions;

    // State — the kind's own floats
    private final float[] values;

    // Constructor \\

    public ParticlePoolStruct(int capacity, int floatsPerParticle) {

        // Capacity
        this.capacity = capacity;
        this.floatsPerParticle = floatsPerParticle;

        // Place
        this.positions = new double[capacity * 3];

        // State
        this.values = new float[capacity * floatsPerParticle];
    }

    // Management \\

    // The slot of a particle spawned at a place, every float of its state zero, or INDEX_NOT_FOUND while the pool is
    // full
    public int spawn(double x, double y, double z) {

        if (count >= capacity)
            return EngineSetting.INDEX_NOT_FOUND;

        int particle = count++;
        int valueStart = particle * floatsPerParticle;

        setPosition(particle, x, y, z);

        for (int i = 0; i < floatsPerParticle; i++)
            values[valueStart + i] = 0f;

        return particle;
    }

    // A particle let go, the last one taking its slot, so a walk retiring as it goes steps back from the end
    public void retire(int particle) {

        int last = --count;

        if (particle == last)
            return;

        System.arraycopy(positions, last * 3, positions, particle * 3, 3);
        System.arraycopy(values, last * floatsPerParticle, values, particle * floatsPerParticle, floatsPerParticle);
    }

    public void clear() {
        count = 0;
    }

    // Place \\

    public void setPosition(int particle, double x, double y, double z) {

        int start = particle * 3;

        positions[start] = x;
        positions[start + 1] = y;
        positions[start + 2] = z;
    }

    public void move(int particle, double dx, double dy, double dz) {

        int start = particle * 3;

        positions[start] += dx;
        positions[start + 1] += dy;
        positions[start + 2] += dz;
    }

    public void setY(int particle, double y) {
        positions[particle * 3 + 1] = y;
    }

    // Accessible \\

    public int getCount() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public double getX(int particle) {
        return positions[particle * 3];
    }

    public double getY(int particle) {
        return positions[particle * 3 + 1];
    }

    public double getZ(int particle) {
        return positions[particle * 3 + 2];
    }

    public float getValue(int particle, int slot) {
        return values[particle * floatsPerParticle + slot];
    }

    public void setValue(int particle, int slot, float value) {
        values[particle * floatsPerParticle + slot] = value;
    }

    public void addValue(int particle, int slot, float amount) {
        values[particle * floatsPerParticle + slot] += amount;
    }
}
