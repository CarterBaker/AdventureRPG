package application.bootstrap.worldpipeline.river;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RiverGatherStruct extends StructPackage {

    /*
     * The reaches that can touch one chunk or one point, each with the run of
     * its segments close enough to matter there and where the span's corner
     * lies in the reach's own unwrapped frame, gathered once and measured
     * against by every column, which finds itself in each reach by its offset
     * from that corner alone. Owned by a worker and cleared for each gather,
     * so nothing is allocated once its lists have grown.
     */

    // Span — world blocks of its corner
    private double fromX;
    private double fromZ;

    // Reaches
    private final ObjectArrayList<RiverPathStruct> paths = new ObjectArrayList<>();
    private final IntArrayList firstSegments = new IntArrayList();
    private final IntArrayList lastSegments = new IntArrayList();
    private final DoubleArrayList localFromX = new DoubleArrayList();
    private final DoubleArrayList localFromZ = new DoubleArrayList();

    // Management \\

    public void clear(double fromX, double fromZ) {
        this.fromX = fromX;
        this.fromZ = fromZ;
        paths.clear();
        firstSegments.clear();
        lastSegments.clear();
        localFromX.clear();
        localFromZ.clear();
    }

    public void add(RiverPathStruct path, int firstSegment, int lastSegment, double pathFromX, double pathFromZ) {
        paths.add(path);
        firstSegments.add(firstSegment);
        lastSegments.add(lastSegment);
        localFromX.add(pathFromX);
        localFromZ.add(pathFromZ);
    }

    // Accessible \\

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public int size() {
        return paths.size();
    }

    public RiverPathStruct getPath(int index) {
        return paths.get(index);
    }

    public int getFirstSegment(int index) {
        return firstSegments.getInt(index);
    }

    public int getLastSegment(int index) {
        return lastSegments.getInt(index);
    }

    // A world position, given in the frame the span's corner was, as an offset in a reach's own frame
    public double toLocalX(int index, double worldX) {
        return localFromX.getDouble(index) + (worldX - fromX);
    }

    public double toLocalZ(int index, double worldZ) {
        return localFromZ.getDouble(index) + (worldZ - fromZ);
    }
}
