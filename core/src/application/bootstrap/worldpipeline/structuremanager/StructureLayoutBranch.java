package application.bootstrap.worldpipeline.structuremanager;

import java.util.SplittableRandom;

import application.bootstrap.worldpipeline.structure.RoadData;
import application.bootstrap.worldpipeline.structure.RoadPathStruct;
import application.bootstrap.worldpipeline.structure.StructureCacheStruct;
import application.bootstrap.worldpipeline.structure.StructureElevationType;
import application.bootstrap.worldpipeline.structure.StructureGeometryUtility;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureLayoutData;
import application.bootstrap.worldpipeline.structure.StructurePlacementStruct;
import application.bootstrap.worldpipeline.structure.StructurePlanStruct;
import application.bootstrap.worldpipeline.structure.StructureTemplateData;
import application.bootstrap.worldpipeline.structure.StructureTemplatePieceStruct;
import application.bootstrap.worldpipeline.structure.StructureType;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldgenerationmanager.TerrainSampleStruct;
import application.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StructureLayoutBranch extends BranchPackage {

    /*
     * Resolves a placement into the concrete streets and templates it
     * writes, memoized per placement.
     *
     * A plain STRUCTURE is one template, rotated and dropped so its anchor
     * lands on the placement point.
     *
     * SETTLEMENT and DUNGEON share one generator. Streets are axis-aligned
     * block lines: the main street runs through the placement point along
     * the placement's rotation, and branches leave existing streets at
     * right angles, level by level, each rejected if it would leave the
     * layout radius or run too close to a street other than its parent.
     * On the surface each street's height follows the terrain, smoothed and
     * grade-limited, pinned where it leaves its parent so junctions meet
     * flush; underground every corridor stays on the layout's floor level.
     * An underground layout with an entrance then cuts a passage from the
     * outer end of its main street up to the surface. Street sides are
     * walked next and lined with buildings whose fronts face the street and
     * whose floors sit at street level — landmarks first, walking outward
     * from the centre along the main street so they cluster there — and
     * finally fill structures are dropped into whatever open ground is left.
     * Every random choice comes from a generator seeded by the placement's
     * identity, so the same layout is produced on every thread, every time.
     */

    // Internal
    private StructureManager structureManager;
    private RoadPlanBranch roadPlanBranch;
    private WorldGenerationManager worldGenerationManager;

    private StructureCacheStruct<StructurePlanStruct> planCache;

    // Base \\

    @Override
    protected void create() {
        this.planCache = new StructureCacheStruct<>(EngineSetting.STRUCTURE_PLAN_CACHE_LIMIT);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
        this.roadPlanBranch = get(RoadPlanBranch.class);
        this.worldGenerationManager = get(WorldGenerationManager.class);
    }

    void clear() {
        planCache.clear();
    }

    // Plan \\

    StructurePlanStruct getPlan(WorldHandle worldHandle, StructurePlacementStruct placement) {
        return planCache.get(placement.getPlacementID(), key -> placement.getStructureHandle().getStructureType()
                .isLayout()
                        ? planLayout(worldHandle, placement)
                        : planTemplate(placement));
    }

    // Template \\

    private StructurePlanStruct planTemplate(StructurePlacementStruct placement) {

        StructurePlanStruct plan = new StructurePlanStruct(placement);
        StructureTemplateData template = placement.getStructureHandle().getTemplateData();
        int rotation = placement.getRotation();

        int anchorX = StructureTemplatePieceStruct.footprintXOf(
                template, rotation, template.getAnchorX(), template.getAnchorZ());
        int anchorZ = StructureTemplatePieceStruct.footprintZOf(
                template, rotation, template.getAnchorX(), template.getAnchorZ());

        long pointX = (long) Math.floor(placement.getX());
        long pointZ = (long) Math.floor(placement.getZ());

        plan.addPiece(new StructureTemplatePieceStruct(
                template, pointX - anchorX, pointZ - anchorZ,
                placement.getY() - template.getAnchorY(), rotation));

        // The road meets the template two blocks out from the middle of its front face.
        int frontX = StructureTemplatePieceStruct.frontX(rotation);
        int frontZ = StructureTemplatePieceStruct.frontZ(rotation);
        int footprintX = StructureTemplatePieceStruct.footprintX(template, rotation);
        int footprintZ = StructureTemplatePieceStruct.footprintZ(template, rotation);

        double connectX = frontX == 0 ? footprintX / 2.0 - anchorX : (frontX > 0 ? footprintX - anchorX + 1 : -anchorX - 2);
        double connectZ = frontZ == 0 ? footprintZ / 2.0 - anchorZ : (frontZ > 0 ? footprintZ - anchorZ + 1 : -anchorZ - 2);

        plan.setConnection(connectX, connectZ, placement.getY());

        return plan;
    }

    // Layout \\

    private static final int[] AXIS_X = { 0, 1, 0, -1 };
    private static final int[] AXIS_Z = { -1, 0, 1, 0 };
    private static final int STREET_GAP_BLOCKS = 8;

    private static final class Street {

        final long startX;
        final long startZ;
        final int dirX;
        final int dirZ;
        final int length;
        final int parent;
        final int level;

        RoadData road;
        int[] heights;

        Street(long startX, long startZ, int dirX, int dirZ, int length, int parent, int level) {
            this.startX = startX;
            this.startZ = startZ;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.length = length;
            this.parent = parent;
            this.level = level;
        }

        long columnX(int t) {
            return startX + (long) dirX * t;
        }

        long columnZ(int t) {
            return startZ + (long) dirZ * t;
        }

        int heightAt(int t) {
            return heights[Math.max(0, Math.min(length, t))];
        }
    }

    private StructurePlanStruct planLayout(WorldHandle worldHandle, StructurePlacementStruct placement) {

        StructureHandle handle = placement.getStructureHandle();
        StructureLayoutData layout = handle.getLayoutData();
        StructurePlanStruct plan = new StructurePlanStruct(placement);

        SplittableRandom random = new SplittableRandom(StructureGeometryUtility.hash(
                worldHandle.getSeed() ^ EngineSetting.STRUCTURE_LAYOUT_SEED, placement.getPlacementID(), 0, 0));

        TerrainSampleStruct sample = new TerrainSampleStruct();
        boolean underground = handle.getElevationType() == StructureElevationType.UNDERGROUND;

        RoadData streetRoad = structureManager.getStructureHandleOfType(
                layout.getStreetRoadName(), StructureType.ROAD, handle.getStructureName()).getRoadData();

        long centerX = (long) Math.floor(placement.getX());
        long centerZ = (long) Math.floor(placement.getZ());

        ObjectArrayList<Street> streets = new ObjectArrayList<>();

        buildStreetNetwork(layout, placement, random, centerX, centerZ, streetRoad, streets);
        resolveStreetHeights(worldHandle, placement, underground, sample, streets);

        Street entrance = null;

        if (underground && layout.hasEntrance())
            entrance = buildEntrance(worldHandle, handle, layout, placement, sample, streets.get(0));

        for (int i = 0; i < streets.size(); i++)
            plan.addStreet(toPath(worldHandle, streets.get(i), sample));

        if (entrance != null)
            plan.addStreet(toPath(worldHandle, entrance, sample));

        ObjectArrayList<long[]> occupied = new ObjectArrayList<>();

        for (int i = 0; i < streets.size(); i++)
            occupied.add(streetBand(streets.get(i)));

        if (entrance != null)
            occupied.add(streetBand(entrance));

        int streetBandCount = occupied.size();

        lineStreets(handle, layout, random, centerX, centerZ, streets, occupied, streetBandCount, plan);
        placeFill(worldHandle, handle, layout, placement, random, underground, sample, centerX, centerZ, occupied,
                streetBandCount, plan);

        // The world road meets the layout where people would walk in.
        Street gateStreet = entrance != null ? entrance : streets.get(0);
        int gateIndex = entrance != null ? entrance.length : 0;
        int outwardX = entrance != null ? entrance.dirX : -streets.get(0).dirX;
        int outwardZ = entrance != null ? entrance.dirZ : -streets.get(0).dirZ;

        plan.setConnection(
                gateStreet.columnX(gateIndex) + outwardX * 2 - centerX,
                gateStreet.columnZ(gateIndex) + outwardZ * 2 - centerZ,
                gateStreet.heightAt(gateIndex));

        return plan;
    }

    // Streets \\

    private void buildStreetNetwork(
            StructureLayoutData layout,
            StructurePlacementStruct placement,
            SplittableRandom random,
            long centerX, long centerZ,
            RoadData streetRoad,
            ObjectArrayList<Street> streets) {

        int rotation = placement.getRotation();
        int dirX = AXIS_X[rotation];
        int dirZ = AXIS_Z[rotation];

        int mainLength = rangeRoll(random, layout.getMainStreetMinLength(), layout.getMainStreetMaxLength());
        mainLength = Math.min(mainLength, layout.getRadiusBlocks() * 2 - 4);

        Street main = new Street(
                centerX - (long) dirX * (mainLength / 2), centerZ - (long) dirZ * (mainLength / 2),
                dirX, dirZ, mainLength, -1, 0);
        main.road = streetRoad;
        streets.add(main);

        int clearance = (int) Math.ceil(streetRoad.getHalfWidthBlocks()) * 2 + STREET_GAP_BLOCKS;

        for (int level = 1; level <= layout.getBranchDepth(); level++) {

            int parentCount = streets.size();

            for (int parentIndex = 0; parentIndex < parentCount; parentIndex++) {

                Street parent = streets.get(parentIndex);

                if (parent.level != level - 1)
                    continue;

                int count = rangeRoll(random, layout.getBranchMinCount(), layout.getBranchMaxCount()) / level;

                for (int b = 0; b < count; b++) {

                    int margin = clearance / 2 + 2;

                    if (parent.length <= margin * 2)
                        break;

                    int t = margin + random.nextInt(parent.length - margin * 2 + 1);
                    int side = random.nextBoolean() ? 1 : -1;
                    int branchDirX = -parent.dirZ * side;
                    int branchDirZ = parent.dirX * side;
                    int length = rangeRoll(random, layout.getBranchMinLength(), layout.getBranchMaxLength());

                    Street branch = new Street(
                            parent.columnX(t), parent.columnZ(t), branchDirX, branchDirZ, length, parentIndex, level);
                    branch.road = streetRoad;

                    if (fitsNetwork(branch, streets, centerX, centerZ, layout.getRadiusBlocks(), clearance))
                        streets.add(branch);
                }
            }
        }
    }

    private boolean fitsNetwork(
            Street candidate,
            ObjectArrayList<Street> streets,
            long centerX, long centerZ,
            int radius,
            int clearance) {

        long endX = candidate.columnX(candidate.length);
        long endZ = candidate.columnZ(candidate.length);

        if (Math.hypot(endX - centerX, endZ - centerZ) > radius - clearance / 2.0)
            return false;

        // Measured from just past the junction, so a street crossing near the junction still blocks it.
        double fromX = candidate.columnX(clearance / 2);
        double fromZ = candidate.columnZ(clearance / 2);

        for (int i = 0; i < streets.size(); i++) {

            if (i == candidate.parent)
                continue;

            Street other = streets.get(i);

            double distance = StructureGeometryUtility.segmentSegmentDistance(
                    fromX, fromZ, endX, endZ,
                    other.startX, other.startZ, other.columnX(other.length), other.columnZ(other.length));

            if (distance < clearance)
                return false;
        }

        return true;
    }

    private void resolveStreetHeights(
            WorldHandle worldHandle,
            StructurePlacementStruct placement,
            boolean underground,
            TerrainSampleStruct sample,
            ObjectArrayList<Street> streets) {

        for (int s = 0; s < streets.size(); s++) {

            Street street = streets.get(s);
            int count = street.length + 1;
            street.heights = new int[count];

            if (underground) {
                java.util.Arrays.fill(street.heights, placement.getY());
                continue;
            }

            float[] target = new float[count];
            boolean[] pinned = new boolean[count];

            for (int t = 0; t < count; t++)
                target[t] = worldGenerationManager.sampleSurfaceHeightBlocks(
                        worldHandle, street.columnX(t), street.columnZ(t), sample);

            if (street.parent < 0) {
                int middle = street.length / 2;
                target[middle] = placement.getY();
                pinned[middle] = true;
            } else {
                Street parent = streets.get(street.parent);
                int junction = (int) (Math.abs(street.startX - parent.startX) + Math.abs(street.startZ - parent.startZ));
                target[0] = parent.heightAt(junction);
                pinned[0] = true;
            }

            street.heights = roadPlanBranch.solveHeights(target, pinned, street.road);
        }
    }

    /*
     * A passage out of the far end of the main street, continuing in the
     * same direction, long enough to climb from the layout floor to the
     * surface at the entrance road's grade.
     */
    private Street buildEntrance(
            WorldHandle worldHandle,
            StructureHandle handle,
            StructureLayoutData layout,
            StructurePlacementStruct placement,
            TerrainSampleStruct sample,
            Street main) {

        RoadData road = structureManager.getStructureHandleOfType(
                layout.getEntranceRoadName(), StructureType.ROAD, handle.getStructureName()).getRoadData();

        int climb = Math.max(1, placement.getDepthBlocks());
        int length = Math.min(layout.getEntranceMaxLengthBlocks(), (int) Math.ceil(climb / road.getMaxGrade()) + 8);

        Street entrance = new Street(main.startX, main.startZ, -main.dirX, -main.dirZ, length, 0, 0);
        entrance.road = road;

        int count = length + 1;
        float[] target = new float[count];
        boolean[] pinned = new boolean[count];

        for (int t = 0; t < count; t++)
            target[t] = worldGenerationManager.sampleSurfaceHeightBlocks(
                    worldHandle, entrance.columnX(t), entrance.columnZ(t), sample);

        target[0] = main.heightAt(0);
        pinned[0] = true;
        pinned[count - 1] = true;

        entrance.heights = roadPlanBranch.solveHeights(target, pinned, road);

        return entrance;
    }

    private RoadPathStruct toPath(WorldHandle worldHandle, Street street, TerrainSampleStruct sample) {

        DoubleArrayList xs = new DoubleArrayList(street.length + 1);
        DoubleArrayList zs = new DoubleArrayList(street.length + 1);

        for (int t = 0; t <= street.length; t++) {
            xs.add(street.columnX(t) + 0.5);
            zs.add(street.columnZ(t) + 0.5);
        }

        return roadPlanBranch.buildPath(worldHandle, street.road, xs, zs, street.heights, sample);
    }

    // Block rectangle { minX, minZ, maxX, maxZ } a street's surface covers.
    private long[] streetBand(Street street) {

        long half = (long) Math.floor(street.road.getHalfWidthBlocks());
        long endX = street.columnX(street.length);
        long endZ = street.columnZ(street.length);

        return new long[] {
                Math.min(street.startX, endX) - half, Math.min(street.startZ, endZ) - half,
                Math.max(street.startX, endX) + half, Math.max(street.startZ, endZ) + half };
    }

    // Lots \\

    private void lineStreets(
            StructureHandle handle,
            StructureLayoutData layout,
            SplittableRandom random,
            long centerX, long centerZ,
            ObjectArrayList<Street> streets,
            ObjectArrayList<long[]> occupied,
            int streetBandCount,
            StructurePlanStruct plan) {

        ObjectArrayList<String> landmarks = new ObjectArrayList<>(layout.getLandmarkNames());

        if (layout.getBuildingNames().isEmpty() && landmarks.isEmpty())
            return;

        for (int s = 0; s < streets.size(); s++) {

            Street street = streets.get(s);

            for (int side = -1; side <= 1; side += 2) {

                if (street.parent < 0) {
                    int middle = street.length / 2;
                    walkSide(handle, layout, random, centerX, centerZ, street, side, middle, street.length, 1,
                            landmarks, occupied, streetBandCount, plan);
                    walkSide(handle, layout, random, centerX, centerZ, street, side, middle - 1, 0, -1,
                            landmarks, occupied, streetBandCount, plan);
                } else {
                    walkSide(handle, layout, random, centerX, centerZ, street, side, 0, street.length, 1,
                            landmarks, occupied, streetBandCount, plan);
                }
            }
        }
    }

    /*
     * Walks one side of one street from `from` toward `to` in `step`
     * direction, placing a building at each cursor that fits and moving on
     * by its width plus the lot spacing; where nothing fits, it moves on a
     * block and tries again.
     */
    private void walkSide(
            StructureHandle handle,
            StructureLayoutData layout,
            SplittableRandom random,
            long centerX, long centerZ,
            Street street,
            int side,
            int from, int to, int step,
            ObjectArrayList<String> landmarks,
            ObjectArrayList<long[]> occupied,
            int streetBandCount,
            StructurePlanStruct plan) {

        int normalX = -street.dirZ * side;
        int normalZ = street.dirX * side;
        int rotation = StructureTemplatePieceStruct.rotationFacing(-normalX, -normalZ);
        int frontOffset = (int) Math.floor(street.road.getHalfWidthBlocks()) + 1 + layout.getLotSetbackBlocks();

        int cursor = from;

        while ((to - cursor) * step >= 0) {

            boolean usingLandmark = !landmarks.isEmpty();
            String name = usingLandmark
                    ? landmarks.get(0)
                    : pickWeighted(random, layout.getBuildingNames(), layout.getBuildingWeights());

            int advance = name == null ? -1 : tryLot(handle, layout, centerX, centerZ, street, cursor, step,
                    normalX, normalZ, rotation, frontOffset, name, occupied, streetBandCount, plan);

            if (advance < 0 && usingLandmark && !layout.getBuildingNames().isEmpty()) {
                name = pickWeighted(random, layout.getBuildingNames(), layout.getBuildingWeights());
                usingLandmark = false;
                advance = tryLot(handle, layout, centerX, centerZ, street, cursor, step,
                        normalX, normalZ, rotation, frontOffset, name, occupied, streetBandCount, plan);
            }

            if (advance < 0) {
                cursor += step;
                continue;
            }

            if (usingLandmark)
                landmarks.remove(0);

            cursor += step * (advance + layout.getLotSpacingBlocks());
        }
    }

    // Places one building at a cursor if it fits; returns its width along the street, or -1.
    private int tryLot(
            StructureHandle handle,
            StructureLayoutData layout,
            long centerX, long centerZ,
            Street street,
            int cursor, int step,
            int normalX, int normalZ,
            int rotation,
            int frontOffset,
            String buildingName,
            ObjectArrayList<long[]> occupied,
            int streetBandCount,
            StructurePlanStruct plan) {

        StructureTemplateData template = resolveChildTemplate(handle, buildingName);

        int footprintX = StructureTemplatePieceStruct.footprintX(template, rotation);
        int footprintZ = StructureTemplatePieceStruct.footprintZ(template, rotation);
        int along = street.dirX != 0 ? footprintX : footprintZ;
        int depth = street.dirX != 0 ? footprintZ : footprintX;

        int farCursor = cursor + step * (along - 1);

        if (farCursor < 0 || farCursor > street.length)
            return -1;

        long nearX = street.columnX(cursor) + (long) normalX * frontOffset;
        long nearZ = street.columnZ(cursor) + (long) normalZ * frontOffset;
        long farX = street.columnX(farCursor) + (long) normalX * (frontOffset + depth - 1);
        long farZ = street.columnZ(farCursor) + (long) normalZ * (frontOffset + depth - 1);

        long[] rect = { Math.min(nearX, farX), Math.min(nearZ, farZ), Math.max(nearX, farX), Math.max(nearZ, farZ) };

        if (!fitsLayout(rect, centerX, centerZ, layout, occupied, streetBandCount))
            return -1;

        int floorY = street.heightAt(cursor + step * (along / 2));

        plan.addPiece(new StructureTemplatePieceStruct(
                template, rect[0], rect[1], floorY - template.getAnchorY(), rotation));
        occupied.add(rect);

        return along;
    }

    // Fill \\

    private void placeFill(
            WorldHandle worldHandle,
            StructureHandle handle,
            StructureLayoutData layout,
            StructurePlacementStruct placement,
            SplittableRandom random,
            boolean underground,
            TerrainSampleStruct sample,
            long centerX, long centerZ,
            ObjectArrayList<long[]> occupied,
            int streetBandCount,
            StructurePlanStruct plan) {

        for (int attempt = 0; attempt < layout.getFillAttempts(); attempt++) {

            String name = pickWeighted(random, layout.getFillNames(), layout.getFillWeights());

            if (name == null)
                return;

            StructureTemplateData template = resolveChildTemplate(handle, name);
            int rotation = random.nextInt(4);
            int footprintX = StructureTemplatePieceStruct.footprintX(template, rotation);
            int footprintZ = StructureTemplatePieceStruct.footprintZ(template, rotation);

            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * layout.getRadiusBlocks();

            long minX = centerX + Math.round(Math.cos(angle) * distance) - footprintX / 2;
            long minZ = centerZ + Math.round(Math.sin(angle) * distance) - footprintZ / 2;
            long[] rect = { minX, minZ, minX + footprintX - 1, minZ + footprintZ - 1 };

            if (!fitsLayout(rect, centerX, centerZ, layout, occupied, streetBandCount))
                continue;

            int floorY = underground
                    ? placement.getY()
                    : worldGenerationManager.sampleSurfaceHeightBlocks(
                            worldHandle, minX + footprintX / 2, minZ + footprintZ / 2, sample);

            plan.addPiece(new StructureTemplatePieceStruct(
                    template, minX, minZ, floorY - template.getAnchorY(), rotation));
            occupied.add(rect);
        }
    }

    // Validation \\

    private boolean fitsLayout(
            long[] rect,
            long centerX, long centerZ,
            StructureLayoutData layout,
            ObjectArrayList<long[]> occupied,
            int streetBandCount) {

        double radius = layout.getRadiusBlocks();

        if (Math.hypot(rect[0] - centerX, rect[1] - centerZ) > radius
                || Math.hypot(rect[2] - centerX, rect[1] - centerZ) > radius
                || Math.hypot(rect[0] - centerX, rect[3] - centerZ) > radius
                || Math.hypot(rect[2] - centerX, rect[3] - centerZ) > radius)
            return false;

        int spacing = layout.getLotSpacingBlocks();

        for (int i = 0; i < occupied.size(); i++) {

            long[] other = occupied.get(i);
            long pad = i < streetBandCount ? 0 : spacing;

            if (rect[0] <= other[2] + pad && rect[2] >= other[0] - pad
                    && rect[1] <= other[3] + pad && rect[3] >= other[1] - pad)
                return false;
        }

        return true;
    }

    private StructureTemplateData resolveChildTemplate(StructureHandle parent, String childName) {
        return structureManager.getStructureHandleOfType(
                childName, StructureType.STRUCTURE, parent.getStructureName()).getTemplateData();
    }

    // Random \\

    private static int rangeRoll(SplittableRandom random, int min, int max) {
        return max <= min ? min : min + random.nextInt(max - min + 1);
    }

    private static String pickWeighted(SplittableRandom random, ObjectArrayList<String> names, FloatArrayList weights) {

        if (names.isEmpty())
            return null;

        float total = 0f;

        for (int i = 0; i < weights.size(); i++)
            total += weights.getFloat(i);

        float roll = (float) random.nextDouble() * total;

        for (int i = 0; i < names.size(); i++) {
            roll -= weights.getFloat(i);
            if (roll < 0f)
                return names.get(i);
        }

        return names.get(names.size() - 1);
    }
}
