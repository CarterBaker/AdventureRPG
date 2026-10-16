package application.bootstrap.shipwrightpipeline.shipwright;

import java.util.Arrays;

import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ShipOctantGridStruct extends StructPackage {

    /*
     * A ship's octant geometry while it is compiled: every octant of its model
     * grid holds the piece it is built as, or nothing. Pieces are laid,
     * overwritten and cut here, then each is merged into the fewest boxes its
     * octants allow — runs along the length first, then across the breadth,
     * then up — and written into its vehicle part in sub-voxels, so the
     * greedy mesher downstream starts from large flat faces. Kept by its
     * owner and resized in place, so recompiling a ship allocates only when it
     * outgrows every ship compiled before it.
     */

    // Size — octants
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    // Cells — the piece in each octant, x fastest, then z, then y
    private byte[] cells;
    private boolean[] merged;

    // Constructor \\

    public ShipOctantGridStruct() {
        this.cells = new byte[0];
        this.merged = new boolean[0];
    }

    // Clears the grid to a size, keeping its storage whenever it is large enough
    public void reset(int sizeX, int sizeY, int sizeZ) {

        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;

        int count = sizeX * sizeY * sizeZ;

        if (cells.length < count) {
            this.cells = new byte[count];
            this.merged = new boolean[count];
            return;
        }

        Arrays.fill(cells, 0, count, (byte) 0);
    }

    // Cells \\

    public boolean contains(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sizeX && y < sizeY && z < sizeZ;
    }

    public boolean isEmpty(int x, int y, int z) {
        return !contains(x, y, z) || cells[toIndex(x, y, z)] == 0;
    }

    // The piece in an octant, null when it is empty or off the grid
    public ShipPiece get(int x, int y, int z) {

        if (!contains(x, y, z))
            return null;

        byte cell = cells[toIndex(x, y, z)];

        return cell == 0 ? null : ShipPiece.fromCell(cell);
    }

    public void set(int x, int y, int z, ShipPiece piece) {

        if (contains(x, y, z))
            cells[toIndex(x, y, z)] = piece.toCell();
    }

    // Lays a piece only where nothing is built yet
    public void setIfEmpty(int x, int y, int z, ShipPiece piece) {

        if (contains(x, y, z) && cells[toIndex(x, y, z)] == 0)
            cells[toIndex(x, y, z)] = piece.toCell();
    }

    public void clear(int x, int y, int z) {

        if (contains(x, y, z))
            cells[toIndex(x, y, z)] = 0;
    }

    private int toIndex(int x, int y, int z) {
        return x + sizeX * (z + sizeZ * y);
    }

    // Merge \\

    // Every octant of a piece merged into boxes and laid into its part in sub-voxels; true when it had any
    public boolean mergeInto(ShipPiece piece, VehiclePartStruct part) {

        int count = sizeX * sizeY * sizeZ;
        int scale = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        byte target = piece.toCell();
        boolean any = false;

        Arrays.fill(merged, 0, count, false);

        for (int y = 0; y < sizeY; y++)
            for (int z = 0; z < sizeZ; z++)
                for (int x = 0; x < sizeX; x++) {

                    int index = toIndex(x, y, z);

                    if (cells[index] != target || merged[index])
                        continue;

                    int endX = x + 1;

                    while (endX < sizeX && isOpen(endX, y, z, target))
                        endX++;

                    int endZ = z + 1;

                    while (endZ < sizeZ && isOpenRow(x, endX, y, endZ, target))
                        endZ++;

                    int endY = y + 1;

                    while (endY < sizeY && isOpenLayer(x, endX, endY, z, endZ, target))
                        endY++;

                    markMerged(x, endX, y, endY, z, endZ);
                    part.addBox(x * scale, y * scale, z * scale, endX * scale, endY * scale, endZ * scale);
                    any = true;
                }

        return any;
    }

    private boolean isOpen(int x, int y, int z, byte target) {

        int index = toIndex(x, y, z);

        return cells[index] == target && !merged[index];
    }

    private boolean isOpenRow(int startX, int endX, int y, int z, byte target) {

        for (int x = startX; x < endX; x++)
            if (!isOpen(x, y, z, target))
                return false;

        return true;
    }

    private boolean isOpenLayer(int startX, int endX, int y, int startZ, int endZ, byte target) {

        for (int z = startZ; z < endZ; z++)
            if (!isOpenRow(startX, endX, y, z, target))
                return false;

        return true;
    }

    private void markMerged(int startX, int endX, int startY, int endY, int startZ, int endZ) {

        for (int y = startY; y < endY; y++)
            for (int z = startZ; z < endZ; z++)
                for (int x = startX; x < endX; x++)
                    merged[toIndex(x, y, z)] = true;
    }

    // Accessible \\

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }
}
