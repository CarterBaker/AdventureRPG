package application.bootstrap.worldpipeline.tree;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.extras.Coordinate3Long;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class TreeNodeStruct extends StructPackage {

    /*
     * The wood of one tree shape as the blocks it passes through, the way
     * Dynamic Trees grows a tree: each node is one block, offset from the
     * block the root stands in, with the radius its wood has there in
     * sub-voxels, the faces it joins its neighbours through, and the shape
     * segment it was laid out from. A node no thicker than a block is a cube
     * at the block's centre with an arm out to each joined face, as thick as
     * the thinner of the two; a thicker node is one box as wide as its radius
     * that stops flush at a face it shares with another thick node. A node a
     * cut runs through is clipped flat along one axis. Laid out once per
     * shape, then only read, so any thread may share it.
     */

    // Nodes
    private final IntArrayList x;
    private final IntArrayList y;
    private final IntArrayList z;
    private final ShortArrayList radius;
    private final ByteArrayList connections;
    private final IntArrayList segment;

    // Clip — sub-voxels from the node block's corner, INDEX_NOT_FOUND axis for none
    private final ByteArrayList clipAxis;
    private final ShortArrayList clipLow;
    private final ShortArrayList clipHigh;

    // Lookup
    private final Long2IntOpenHashMap block2Node;

    // Reach — the thickest radius any node has
    private int maxRadius;

    // Constructor \\

    public TreeNodeStruct() {

        // Nodes
        this.x = new IntArrayList();
        this.y = new IntArrayList();
        this.z = new IntArrayList();
        this.radius = new ShortArrayList();
        this.connections = new ByteArrayList();
        this.segment = new IntArrayList();

        // Clip
        this.clipAxis = new ByteArrayList();
        this.clipLow = new ShortArrayList();
        this.clipHigh = new ShortArrayList();

        // Lookup
        this.block2Node = new Long2IntOpenHashMap();
        this.block2Node.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
    }

    // Layout \\

    // The node in a block, laid out with a radius from a segment — a block wood already passes through keeps its
    // segment and takes the thicker radius
    public int addNode(int blockX, int blockY, int blockZ, int nodeRadius, int nodeSegment) {

        int node = findNode(blockX, blockY, blockZ);

        maxRadius = Math.max(maxRadius, nodeRadius);

        if (node != EngineSetting.INDEX_NOT_FOUND) {
            radius.set(node, (short) Math.max(radius.getShort(node), nodeRadius));
            return node;
        }

        x.add(blockX);
        y.add(blockY);
        z.add(blockZ);
        radius.add((short) nodeRadius);
        connections.add((byte) 0);
        segment.add(nodeSegment);
        clipAxis.add((byte) EngineSetting.INDEX_NOT_FOUND);
        clipLow.add(Short.MIN_VALUE);
        clipHigh.add(Short.MAX_VALUE);

        node = x.size() - 1;
        block2Node.put(Coordinate3Long.pack(blockX, blockY, blockZ), node);

        return node;
    }

    // Joins two face-adjacent nodes through the face they share
    public void connect(int from, int to) {

        int axis = x.getInt(to) != x.getInt(from)
                ? EngineSetting.AXIS_X
                : y.getInt(to) != y.getInt(from) ? EngineSetting.AXIS_Y : EngineSetting.AXIS_Z;
        boolean positive = getCoordinate(to, axis) > getCoordinate(from, axis);

        connections.set(from, (byte) (connections.getByte(from) | toFaceBit(axis, positive)));
        connections.set(to, (byte) (connections.getByte(to) | toFaceBit(axis, !positive)));
    }

    // Clips a node to the sub-voxels between two planes on one axis, narrowing any clip it already has on that axis
    public void clip(int node, int axis, int low, int high) {

        int current = clipAxis.getByte(node);

        if (current != EngineSetting.INDEX_NOT_FOUND && current != axis)
            return;

        clipAxis.set(node, (byte) axis);
        clipLow.set(node, (short) Math.max(clipLow.getShort(node), low));
        clipHigh.set(node, (short) Math.min(clipHigh.getShort(node), high));
    }

    // Lookup \\

    // The node in a block offset from the root block, INDEX_NOT_FOUND when the wood does not pass through it
    public int findNode(int blockX, int blockY, int blockZ) {
        return block2Node.get(Coordinate3Long.pack(blockX, blockY, blockZ));
    }

    // Connections \\

    // The face bit for one side of an axis
    public static int toFaceBit(int axis, boolean positive) {
        return 1 << (axis * 2 + (positive ? 1 : 0));
    }

    public boolean isConnected(int node, int axis, boolean positive) {
        return (connections.getByte(node) & toFaceBit(axis, positive)) != 0;
    }

    // The node joined through one face, INDEX_NOT_FOUND when that face is not joined
    public int getNeighbor(int node, int axis, boolean positive) {

        if (!isConnected(node, axis, positive))
            return EngineSetting.INDEX_NOT_FOUND;

        int step = positive ? 1 : -1;

        return findNode(
                x.getInt(node) + (axis == EngineSetting.AXIS_X ? step : 0),
                y.getInt(node) + (axis == EngineSetting.AXIS_Y ? step : 0),
                z.getInt(node) + (axis == EngineSetting.AXIS_Z ? step : 0));
    }

    // Accessible \\

    public int getCount() {
        return x.size();
    }

    public boolean isEmpty() {
        return x.isEmpty();
    }

    public int getX(int node) {
        return x.getInt(node);
    }

    public int getY(int node) {
        return y.getInt(node);
    }

    public int getZ(int node) {
        return z.getInt(node);
    }

    public int getCoordinate(int node, int axis) {

        if (axis == EngineSetting.AXIS_X)
            return x.getInt(node);

        return axis == EngineSetting.AXIS_Y ? y.getInt(node) : z.getInt(node);
    }

    public int getRadius(int node) {
        return radius.getShort(node);
    }

    public int getMaxRadius() {
        return maxRadius;
    }

    public int getSegment(int node) {
        return segment.getInt(node);
    }

    public int getClipAxis(int node) {
        return clipAxis.getByte(node);
    }

    public int getClipLow(int node) {
        return clipLow.getShort(node);
    }

    public int getClipHigh(int node) {
        return clipHigh.getShort(node);
    }
}
