package application.bootstrap.vehiclepipeline.vehiclemanager;

import java.util.Arrays;

import application.bootstrap.vehiclepipeline.vehicle.VehicleColumnStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHullStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehicleLumpStruct;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VehicleHullBuilder extends BuilderPackage {

    /*
     * Works out a hull from its watertight parts. Their boxes are laid onto a
     * grid of dry cells, every cell one touches a wall; then each horizontal
     * layer is flooded in from its edge through open cells, and whatever the
     * sea cannot reach, the walls and everything they ring, is dry. Dry cells
     * gather into upright columns, each sampling the sea once, and each column
     * into lumps the sea lifts; the hull's mass is the sea those lumps displace
     * at the design draft, and its centre of mass sits over the centroid of
     * that displacement at the height the data sets, so it floats level at its
     * draft. Inertia follows from the dry volume's length and
     * beam. Contact points are the bottom of every column and the hull's ends
     * every few layers, and the waterline mask samples the narrowest
     * half-beam around the design waterline at stations along the length,
     * out to the hull's outside less one dry cell so it never reaches past
     * the planking yet keeps the sea off the inside of every wall.
     */

    // Settings
    private int resolution;
    private int cellSize;

    // Grid
    private int originX;
    private int originY;
    private int originZ;
    private int cellsX;
    private int cellsY;
    private int cellsZ;
    private boolean[] walls;
    private boolean[] dry;

    // Build \\

    @Override
    protected void create() {

        // Settings
        this.resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        this.cellSize = EngineSetting.VEHICLE_DRY_CELL_SUB_VOXELS;
    }

    VehicleHullStruct build(
            String vehicleName,
            ObjectArrayList<VehiclePartStruct> parts,
            float draft,
            float centerOfMassHeight) {

        layOutGrid(vehicleName, parts);
        layWalls(parts);
        floodLayers();

        IntArrayList columnOrigins = new IntArrayList();
        ObjectArrayList<VehicleColumnStruct> columns = gatherColumns(columnOrigins);
        ObjectArrayList<VehicleLumpStruct> lumps = gatherLumps(columnOrigins);
        Vector3 centerOfMass = new Vector3();
        float mass = resolveMass(vehicleName, lumps, draft, centerOfMassHeight, centerOfMass);
        float[] maskHalfBeams = new float[EngineSetting.OCEAN_HULL_STATIONS];
        float[] maskExtent = resolveMask(draft, maskHalfBeams);

        return new VehicleHullStruct(
                mass,
                resolveInertia(mass),
                centerOfMass,
                draft,
                dry,
                originX,
                originY,
                originZ,
                cellsX,
                cellsY,
                cellsZ,
                sumVolume(lumps),
                columns,
                lumps,
                gatherContactPoints(columns),
                maskExtent[0],
                maskExtent[1],
                maskExtent[2],
                maskExtent[3],
                maskExtent[4],
                maskHalfBeams);
    }

    // Grid \\

    // Cells around every watertight box, with one open cell of margin on every side the sea floods in from
    private void layOutGrid(String vehicleName, ObjectArrayList<VehiclePartStruct> parts) {

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            if (!part.getRole().isWatertight() || part.isEmpty())
                continue;

            minX = Math.min(minX, part.getMinX());
            minY = Math.min(minY, part.getMinY());
            minZ = Math.min(minZ, part.getMinZ());
            maxX = Math.max(maxX, part.getMaxX());
            maxY = Math.max(maxY, part.getMaxY());
            maxZ = Math.max(maxZ, part.getMaxZ());
        }

        if (minX == Integer.MAX_VALUE)
            throwException("Vehicle '" + vehicleName + "' has no watertight hull part to float on.");

        this.originX = Math.floorDiv(minX, cellSize) * cellSize - cellSize;
        this.originY = Math.floorDiv(minY, cellSize) * cellSize;
        this.originZ = Math.floorDiv(minZ, cellSize) * cellSize - cellSize;
        this.cellsX = Math.floorDiv(maxX - 1 - originX, cellSize) + 2;
        this.cellsY = Math.floorDiv(maxY - 1 - originY, cellSize) + 1;
        this.cellsZ = Math.floorDiv(maxZ - 1 - originZ, cellSize) + 2;
        this.walls = new boolean[cellsX * cellsY * cellsZ];
        this.dry = new boolean[cellsX * cellsY * cellsZ];
    }

    private void layWalls(ObjectArrayList<VehiclePartStruct> parts) {

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            if (!part.getRole().isWatertight())
                continue;

            for (int box = 0; box < part.getBoxCount(); box++)
                layWall(
                        part.getBoxBound(box, EngineSetting.BOX_MIN_X),
                        part.getBoxBound(box, EngineSetting.BOX_MIN_Y),
                        part.getBoxBound(box, EngineSetting.BOX_MIN_Z),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_X),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_Y),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_Z));
        }
    }

    private void layWall(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        int lastX = Math.floorDiv(maxX - 1 - originX, cellSize);
        int lastY = Math.floorDiv(maxY - 1 - originY, cellSize);
        int lastZ = Math.floorDiv(maxZ - 1 - originZ, cellSize);

        for (int z = Math.floorDiv(minZ - originZ, cellSize); z <= lastZ; z++)
            for (int y = Math.floorDiv(minY - originY, cellSize); y <= lastY; y++)
                for (int x = Math.floorDiv(minX - originX, cellSize); x <= lastX; x++)
                    walls[toCellIndex(x, y, z)] = true;
    }

    // Each layer flooded in from its edge through open cells; what the sea never reaches is dry
    private void floodLayers() {

        boolean[] outside = new boolean[cellsX * cellsZ];
        int[] queue = new int[cellsX * cellsZ];

        for (int y = 0; y < cellsY; y++) {

            Arrays.fill(outside, false);
            int head = 0;
            int tail = 0;

            for (int z = 0; z < cellsZ; z++)
                for (int x = 0; x < cellsX; x++) {

                    boolean border = x == 0 || z == 0 || x == cellsX - 1 || z == cellsZ - 1;

                    if (!border || walls[toCellIndex(x, y, z)])
                        continue;

                    outside[x + z * cellsX] = true;
                    queue[tail++] = x + z * cellsX;
                }

            while (head < tail) {

                int column = queue[head++];
                int x = column % cellsX;
                int z = column / cellsX;

                tail = floodInto(x + 1, y, z, outside, queue, tail);
                tail = floodInto(x - 1, y, z, outside, queue, tail);
                tail = floodInto(x, y, z + 1, outside, queue, tail);
                tail = floodInto(x, y, z - 1, outside, queue, tail);
            }

            for (int z = 0; z < cellsZ; z++)
                for (int x = 0; x < cellsX; x++)
                    dry[toCellIndex(x, y, z)] = !outside[x + z * cellsX];
        }
    }

    private int floodInto(int x, int y, int z, boolean[] outside, int[] queue, int tail) {

        if (x < 0 || z < 0 || x >= cellsX || z >= cellsZ)
            return tail;

        int column = x + z * cellsX;

        if (outside[column] || walls[toCellIndex(x, y, z)])
            return tail;

        outside[column] = true;
        queue[tail] = column;

        return tail + 1;
    }

    // Columns \\

    // Every column holding a dry cell, its first cell column written to the origins given, two ints each
    private ObjectArrayList<VehicleColumnStruct> gatherColumns(IntArrayList columnOrigins) {

        int span = Math.max(1, EngineSetting.VEHICLE_COLUMN_SUB_VOXELS / cellSize);
        int columnsX = (cellsX + span - 1) / span;
        int columnsZ = (cellsZ + span - 1) / span;
        boolean[] filled = new boolean[columnsX * columnsZ];
        float[] extents = new float[columnsX * columnsZ * EngineSetting.VEHICLE_COLUMN_EXTENT_FLOATS];

        for (int columnZ = 0; columnZ < columnsZ; columnZ++)
            for (int columnX = 0; columnX < columnsX; columnX++)
                filled[columnX + columnZ * columnsX] = measureColumn(
                        columnX * span,
                        columnZ * span,
                        span,
                        extents,
                        (columnX + columnZ * columnsX) * EngineSetting.VEHICLE_COLUMN_EXTENT_FLOATS);

        ObjectArrayList<VehicleColumnStruct> columns = new ObjectArrayList<>();

        for (int columnZ = 0; columnZ < columnsZ; columnZ++)
            for (int columnX = 0; columnX < columnsX; columnX++) {

                int column = columnX + columnZ * columnsX;

                if (!filled[column])
                    continue;

                int base = column * EngineSetting.VEHICLE_COLUMN_EXTENT_FLOATS;
                boolean edge = !isFilledColumn(filled, columnsX, columnsZ, columnX + 1, columnZ)
                        || !isFilledColumn(filled, columnsX, columnsZ, columnX - 1, columnZ)
                        || !isFilledColumn(filled, columnsX, columnsZ, columnX, columnZ + 1)
                        || !isFilledColumn(filled, columnsX, columnsZ, columnX, columnZ - 1);

                columns.add(new VehicleColumnStruct(
                        extents[base],
                        extents[base + 1],
                        extents[base + 2],
                        extents[base + 3],
                        extents[base + 4],
                        edge));
                columnOrigins.add(columnX * span);
                columnOrigins.add(columnZ * span);
            }

        return columns;
    }

    // Lumps \\

    // Each column cut into lumps a column wide and as tall, every lump the centroid and volume of its dry cells
    private ObjectArrayList<VehicleLumpStruct> gatherLumps(IntArrayList columnOrigins) {

        int span = Math.max(1, EngineSetting.VEHICLE_COLUMN_SUB_VOXELS / cellSize);
        float cellBlocks = (float) cellSize / resolution;
        ObjectArrayList<VehicleLumpStruct> lumps = new ObjectArrayList<>();

        for (int column = 0; column < columnOrigins.size() / 2; column++) {

            int firstX = columnOrigins.getInt(column * 2);
            int firstZ = columnOrigins.getInt(column * 2 + 1);

            for (int firstY = 0; firstY < cellsY; firstY += span) {

                float sumX = 0f;
                float sumY = 0f;
                float sumZ = 0f;
                int count = 0;

                for (int z = firstZ; z < Math.min(firstZ + span, cellsZ); z++)
                    for (int y = firstY; y < Math.min(firstY + span, cellsY); y++)
                        for (int x = firstX; x < Math.min(firstX + span, cellsX); x++) {

                            if (!dry[toCellIndex(x, y, z)])
                                continue;

                            sumX += toBlocks(originX, x);
                            sumY += toBlocks(originY, y);
                            sumZ += toBlocks(originZ, z);
                            count++;
                        }

                if (count == 0)
                    continue;

                lumps.add(new VehicleLumpStruct(
                        sumX / count + cellBlocks * 0.5f,
                        sumY / count + cellBlocks * 0.5f,
                        sumZ / count + cellBlocks * 0.5f,
                        count * cellBlocks * cellBlocks * cellBlocks,
                        column));
            }
        }

        return lumps;
    }

    // Writes a column's centre x and z, floor, top and volume in blocks — false when it holds no dry cell
    private boolean measureColumn(int firstX, int firstZ, int span, float[] extents, int base) {

        float cellBlocks = (float) cellSize / resolution;
        float sumX = 0f;
        float sumZ = 0f;
        int count = 0;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;

        for (int z = firstZ; z < Math.min(firstZ + span, cellsZ); z++)
            for (int x = firstX; x < Math.min(firstX + span, cellsX); x++)
                for (int y = 0; y < cellsY; y++) {

                    if (!dry[toCellIndex(x, y, z)])
                        continue;

                    sumX += toBlocks(originX, x) + cellBlocks * 0.5f;
                    sumZ += toBlocks(originZ, z) + cellBlocks * 0.5f;
                    lowest = Math.min(lowest, y);
                    highest = Math.max(highest, y);
                    count++;
                }

        if (count == 0)
            return false;

        extents[base] = sumX / count;
        extents[base + 1] = sumZ / count;
        extents[base + 2] = toBlocks(originY, lowest);
        extents[base + 3] = toBlocks(originY, highest + 1);
        extents[base + 4] = count * cellBlocks * cellBlocks * cellBlocks;

        return true;
    }

    private boolean isFilledColumn(boolean[] filled, int columnsX, int columnsZ, int columnX, int columnZ) {
        return columnX >= 0 && columnZ >= 0 && columnX < columnsX && columnZ < columnsZ
                && filled[columnX + columnZ * columnsX];
    }

    private float sumVolume(ObjectArrayList<VehicleLumpStruct> lumps) {

        float volume = 0f;

        for (int i = 0; i < lumps.size(); i++)
            volume += lumps.get(i).getVolume();

        return volume;
    }

    // Mass \\

    // The sea the lumps displace upright at the draft, each by the share of its height below the waterline, the
    // centre of mass set over the centroid of that displacement at the height given
    private float resolveMass(
            String vehicleName,
            ObjectArrayList<VehicleLumpStruct> lumps,
            float draft,
            float centerOfMassHeight,
            Vector3 centerOfMass) {

        float lumpHeight = (float) EngineSetting.VEHICLE_COLUMN_SUB_VOXELS / resolution;
        float displaced = 0f;
        float momentX = 0f;
        float momentZ = 0f;

        for (int i = 0; i < lumps.size(); i++) {

            VehicleLumpStruct lump = lumps.get(i);
            float submerged = Math.max(0f, Math.min(1f, (draft - lump.getY()) / lumpHeight + 0.5f));
            float volume = lump.getVolume() * submerged;

            displaced += volume;
            momentX += volume * lump.getX();
            momentZ += volume * lump.getZ();
        }

        if (displaced <= 0f)
            throwException("Vehicle '" + vehicleName + "' sets its draft at " + draft
                    + " blocks, below the bottom of its hull.");

        centerOfMass.set(momentX / displaced, centerOfMassHeight, momentZ / displaced);

        return displaced * EngineSetting.VEHICLE_WATER_DENSITY;
    }

    // Roll about the length, yaw about the vertical and pitch about the beam, each from its radius of gyration
    private Vector3 resolveInertia(float mass) {

        float cellBlocks = (float) cellSize / resolution;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int z = 0; z < cellsZ; z++)
            for (int y = 0; y < cellsY; y++)
                for (int x = 0; x < cellsX; x++) {

                    if (!dry[toCellIndex(x, y, z)])
                        continue;

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }

        float length = (maxX - minX + 1) * cellBlocks;
        float beam = (maxZ - minZ + 1) * cellBlocks;
        float roll = EngineSetting.VEHICLE_ROLL_GYRATION_RATIO * beam;
        float yaw = EngineSetting.VEHICLE_YAW_GYRATION_RATIO * length;
        float pitch = EngineSetting.VEHICLE_PITCH_GYRATION_RATIO * length;

        return new Vector3(mass * roll * roll, mass * yaw * yaw, mass * pitch * pitch);
    }

    // Contact \\

    // The bottom of the hull under every column, then the hull's two ends every few layers
    private FloatArrayList gatherContactPoints(ObjectArrayList<VehicleColumnStruct> columns) {

        FloatArrayList points = new FloatArrayList();
        float cellBlocks = (float) cellSize / resolution;

        for (int i = 0; i < columns.size(); i++) {

            VehicleColumnStruct column = columns.get(i);

            points.add(column.getX());
            points.add(column.getBottom());
            points.add(column.getZ());
        }

        for (int y = 0; y < cellsY; y += EngineSetting.VEHICLE_CONTACT_LAYER_STEP) {

            int foremost = Integer.MIN_VALUE;
            int aftmost = Integer.MAX_VALUE;
            int foreZ = 0;
            int aftZ = 0;

            for (int z = 0; z < cellsZ; z++)
                for (int x = 0; x < cellsX; x++) {

                    if (!walls[toCellIndex(x, y, z)])
                        continue;

                    if (x > foremost) {
                        foremost = x;
                        foreZ = z;
                    }

                    if (x < aftmost) {
                        aftmost = x;
                        aftZ = z;
                    }
                }

            if (foremost == Integer.MIN_VALUE)
                continue;

            float height = toBlocks(originY, y) + cellBlocks * 0.5f;

            points.add(toBlocks(originX, foremost + 1));
            points.add(height);
            points.add(toBlocks(originZ, foreZ) + cellBlocks * 0.5f);

            points.add(toBlocks(originX, aftmost));
            points.add(height);
            points.add(toBlocks(originZ, aftZ) + cellBlocks * 0.5f);
        }

        return points;
    }

    // Mask \\

    // Writes the narrowest inner half-beam at each station, returning the start, length, floor, top and centre line
    private float[] resolveMask(float draft, float[] halfBeams) {

        float cellBlocks = (float) cellSize / resolution;
        int waterline = Math.max(0, Math.min(cellsY - 1,
                Math.floorDiv(Math.round(draft * resolution) - originY, cellSize)));
        int firstLayer = Math.max(0, waterline - EngineSetting.VEHICLE_HULL_MASK_BAND_CELLS);
        int lastLayer = Math.min(cellsY - 1, waterline + EngineSetting.VEHICLE_HULL_MASK_BAND_CELLS);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int bottom = Integer.MAX_VALUE;
        int top = Integer.MIN_VALUE;

        for (int z = 0; z < cellsZ; z++)
            for (int y = 0; y < cellsY; y++)
                for (int x = 0; x < cellsX; x++) {

                    int index = toCellIndex(x, y, z);

                    if (!dry[index])
                        continue;

                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                    bottom = Math.min(bottom, y);

                    if (walls[index])
                        continue;

                    top = Math.max(top, y);

                    if (y < firstLayer || y > lastLayer)
                        continue;

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }

        float centerZ = (toBlocks(originZ, minZ) + toBlocks(originZ, maxZ + 1)) * 0.5f;

        if (minX == Integer.MAX_VALUE)
            return new float[] { 0f, 0f, 0f, 0f, centerZ };

        int stations = halfBeams.length;
        int spanCells = maxX - minX + 1;

        for (int station = 0; station < stations; station++) {

            int firstX = minX + station * spanCells / stations;
            int lastX = Math.max(firstX, minX + (station + 1) * spanCells / stations - 1);

            halfBeams[station] = resolveStationHalfBeam(firstX, lastX, firstLayer, lastLayer, centerZ);
        }

        return new float[] {
                toBlocks(originX, minX),
                spanCells * cellBlocks,
                toBlocks(originY, bottom),
                toBlocks(originY, top + 1),
                centerZ };
    }

    // The least room either side of the centre line out to the hull's outside, short by the dry cell its planking
    // may only partly fill, over every cell column and layer of a station
    private float resolveStationHalfBeam(int firstX, int lastX, int firstLayer, int lastLayer, float centerZ) {

        float cellBlocks = (float) cellSize / resolution;
        float halfBeam = Float.MAX_VALUE;

        for (int x = firstX; x <= lastX; x++)
            for (int y = firstLayer; y <= lastLayer; y++) {

                int outerMin = Integer.MAX_VALUE;
                int outerMax = Integer.MIN_VALUE;

                for (int z = 0; z < cellsZ; z++) {

                    if (!dry[toCellIndex(x, y, z)])
                        continue;

                    outerMin = Math.min(outerMin, z);
                    outerMax = Math.max(outerMax, z);
                }

                if (outerMin == Integer.MAX_VALUE)
                    return 0f;

                halfBeam = Math.min(halfBeam, Math.min(
                        centerZ - toBlocks(originZ, outerMin),
                        toBlocks(originZ, outerMax + 1) - centerZ) - cellBlocks);
            }

        return Math.max(0f, halfBeam);
    }

    // Utility \\

    private float toBlocks(int origin, int cell) {
        return (float) (origin + cell * cellSize) / resolution;
    }

    private int toCellIndex(int x, int y, int z) {
        return x + cellsX * (y + cellsY * z);
    }
}
