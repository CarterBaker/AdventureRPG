package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecture.ArchitectureWallStruct;
import application.bootstrap.worldpipeline.layout.LayoutDraftStruct;
import application.bootstrap.worldpipeline.layout.LayoutLotStruct;
import application.bootstrap.worldpipeline.layout.LayoutRandomStruct;
import application.bootstrap.worldpipeline.layout.LayoutRoadKind;
import application.bootstrap.worldpipeline.layout.LayoutWallStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadPathStruct;
import application.bootstrap.worldpipeline.road.RoadQueryStruct;
import application.bootstrap.worldpipeline.road.RoadSpanType;
import application.bootstrap.worldpipeline.roadmanager.RoadManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementRoleStruct;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.util.RoadPathUtility;
import application.bootstrap.worldpipeline.util.StructurePlacementUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class SettlementPlanBranch extends BranchPackage {

    /*
     * Async — plans one settlement from its site, on whatever thread first
     * needs it. Its centre role stands in the middle, a keep, castle or well,
     * ringed by a round street just clear of it. Streets leave the ring
     * outward, bending as they go, and lanes branch off them to either side;
     * every street is planned against the land, so it rolls with the ground
     * and bridges or bores where it must. A walled settlement raises its
     * curtain wall around its heart, gated wherever a street passes out,
     * with a gatehouse over each main street and towers along it. A few
     * buildings are bridged over its streets. Its lots then line the ring,
     * the streets and the lanes, each facing the street across its setback,
     * its leading roles first and its fill role after, and trails wander out
     * from the far ends of its streets to outposts or dead ends. Every draw
     * comes from the site's seed, so the same site always plans the same
     * settlement.
     */

    // Internal
    private RoadManager roadManager;
    private LayoutManager layoutManager;
    private SettlementTrailBranch settlementTrailBranch;
    private SettlementPlanAsyncContainer planContainer;

    // Base \\

    @Override
    protected void create() {
        this.planContainer = create(SettlementPlanAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
        this.layoutManager = get(LayoutManager.class);
        this.settlementTrailBranch = get(SettlementTrailBranch.class);
    }

    // Plan \\

    SettlementPlanStruct plan(WorldHandle worldHandle, SettlementSiteStruct site) {

        SettlementPlanAsyncContainer scratch = planContainer.getInstance();

        try {

            ArchitectureHandle architectureHandle = site.getArchitectureHandle();
            LayoutRandomStruct random = new LayoutRandomStruct(site.getSeed());
            double centerX = site.getCenterX() + EngineSetting.BLOCK_CENTER_OFFSET;
            double centerZ = site.getCenterZ() + EngineSetting.BLOCK_CENTER_OFFSET;
            LayoutDraftStruct draft = new LayoutDraftStruct(worldHandle, centerX, centerZ);
            float centerY = resolveCenterY(worldHandle, centerX, centerZ);

            double ringRadius = placeCenter(draft, site, random, centerX, centerZ, centerY);
            RoadPathStruct ring = layRing(draft, scratch, architectureHandle, random, centerX, centerZ, ringRadius,
                    centerY);

            laySpokes(draft, scratch, site, random, ring, centerX, centerZ);

            if (site.getSettlementHandle().hasWall() && architectureHandle.hasWall())
                raiseWall(draft, scratch, site, random, centerX, centerZ);

            placeOverpasses(draft, scratch, site, random);
            fillLots(draft, scratch, site, random, centerX, centerZ);
            layTrails(draft, scratch, site, random);

            return new SettlementPlanStruct(
                    site,
                    layoutManager.seal(draft),
                    scratch.exitX.toDoubleArray(),
                    scratch.exitZ.toDoubleArray(),
                    scratch.exitY.toFloatArray(),
                    scratch.exitHeadingX.toDoubleArray(),
                    scratch.exitHeadingZ.toDoubleArray());
        } finally {
            scratch.reset();
        }
    }

    // The ground at the centre, or the sea's level where a hand placed one over water
    private float resolveCenterY(WorldHandle worldHandle, double centerX, double centerZ) {

        int ground = layoutManager.probeGround(worldHandle, centerX, centerZ);

        return ground == EngineSetting.LAYOUT_GROUND_FLOODED ? EngineSetting.TERRAIN_SEA_LEVEL_BLOCKS : ground;
    }

    // Centre \\

    // The centre role stood in the middle, returning the radius its ring street runs at just clear of it
    private double placeCenter(
            LayoutDraftStruct draft,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            double centerX,
            double centerZ,
            float centerY) {

        ArchitectureHandle architectureHandle = site.getArchitectureHandle();
        RoadHandle street = architectureHandle.getRoad(LayoutRoadKind.STREET);
        StructureHandle center = settlementTrailBranch.drawRole(
                architectureHandle, site.getSettlementHandle().getCenterRole(), random);
        double radius = EngineSetting.SETTLEMENT_RING_MIN_RADIUS_BLOCKS;

        if (center == null)
            return radius;

        LayoutLotStruct lot = layoutManager.placeLotAt(
                draft, center, random.nextInt(0, EngineSetting.STRUCTURE_QUARTER_TURN_COUNT - 1),
                (long) Math.floor(centerX), (long) Math.floor(centerZ), centerY);

        if (lot == null)
            return radius;

        double reachX = Math.max(Math.abs(lot.getMinX() - centerX), Math.abs(lot.getMaxX() - centerX));
        double reachZ = Math.max(Math.abs(lot.getMinZ() - centerZ), Math.abs(lot.getMaxZ() - centerZ));
        double clear = Math.sqrt(reachX * reachX + reachZ * reachZ) + settlementTrailBranch.resolveClearance(street);

        return Math.min(EngineSetting.SETTLEMENT_RING_MAX_RADIUS_BLOCKS, Math.max(radius, clear));
    }

    private RoadPathStruct layRing(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            ArchitectureHandle architectureHandle,
            LayoutRandomStruct random,
            double centerX,
            double centerZ,
            double ringRadius,
            float centerY) {

        RoadPathUtility.buildRing(
                centerX, centerZ, ringRadius, EngineSetting.SETTLEMENT_RING_WOBBLE, random.nextSeed(),
                scratch.pointX, scratch.pointZ);

        RoadPathStruct ring = roadManager.planPath(
                draft.getWorldHandle(), architectureHandle.getRoad(LayoutRoadKind.STREET),
                scratch.pointX, scratch.pointZ, centerY, centerY);

        settlementTrailBranch.addRoad(draft, ring);
        scratch.streets.add(ring);

        return ring;
    }

    // Streets \\

    private void laySpokes(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            RoadPathStruct ring,
            double centerX,
            double centerZ) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        int count = random.nextInt(settlementHandle.getSpokes().getMin(), settlementHandle.getSpokes().getMax());
        double baseAngle = random.next() * Math.PI * 2.0;

        for (int spoke = 0; spoke < count; spoke++) {

            double angle = baseAngle + Math.PI * 2.0 * spoke / count
                    + random.nextSigned() * EngineSetting.SETTLEMENT_SPOKE_JITTER * Math.PI / count;

            laySpoke(draft, scratch, site, random, ring, centerX, centerZ, Math.cos(angle), Math.sin(angle));
        }
    }

    private void laySpoke(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            RoadPathStruct ring,
            double centerX,
            double centerZ,
            double headingX,
            double headingZ) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        RoadHandle street = site.getArchitectureHandle().getRoad(LayoutRoadKind.STREET);
        int start = findRingPoint(ring, centerX, centerZ, headingX, headingZ);
        double startX = ring.getPointX(start);
        double startZ = ring.getPointZ(start);
        int length = random.nextInt(
                settlementHandle.getSpokeLengthBlocks().getMin(), settlementHandle.getSpokeLengthBlocks().getMax());

        RoadPathUtility.buildCurve(
                startX, startZ, startX + headingX * length, startZ + headingZ * length,
                settlementHandle.getBend(), random.nextSeed(), scratch.pointX, scratch.pointZ);

        RoadPathStruct spoke = roadManager.planPath(
                draft.getWorldHandle(), street, scratch.pointX, scratch.pointZ,
                ring.getPointY(start), EngineSetting.ROAD_HEIGHT_FREE);

        settlementTrailBranch.addRoad(draft, spoke);
        scratch.streets.add(spoke);
        scratch.spokes.add(spoke);

        addExit(scratch, spoke);
        layBranches(draft, scratch, site, random, spoke);
    }

    // The ring point lying furthest along a heading from the centre
    private int findRingPoint(RoadPathStruct ring, double centerX, double centerZ, double headingX, double headingZ) {

        int best = 0;
        double bestAlignment = -Double.MAX_VALUE;

        for (int i = 0; i < ring.getPointCount(); i++) {

            double alignment = (ring.getPointX(i) - centerX) * headingX + (ring.getPointZ(i) - centerZ) * headingZ;

            if (alignment <= bestAlignment)
                continue;

            bestAlignment = alignment;
            best = i;
        }

        return best;
    }

    // The far end of a street, with the heading it leaves along, where roads and trails set out from
    private void addExit(SettlementPlanAsyncContainer scratch, RoadPathStruct spoke) {

        RoadQueryStruct query = scratch.query;

        RoadPathUtility.sampleAlong(spoke, spoke.getLength(), query);

        scratch.exitX.add(query.getCenterX());
        scratch.exitZ.add(query.getCenterZ());
        scratch.exitY.add(query.getSurfaceY());
        scratch.exitHeadingX.add(query.getHeadingX());
        scratch.exitHeadingZ.add(query.getHeadingZ());
    }

    // Lanes leaving a street to either side, tilted a little from square
    private void layBranches(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            RoadPathStruct spoke) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        RoadHandle street = site.getArchitectureHandle().getRoad(LayoutRoadKind.STREET);
        RoadQueryStruct query = scratch.query;
        int count = random.nextInt(settlementHandle.getBranches().getMin(), settlementHandle.getBranches().getMax());

        for (int branch = 0; branch < count; branch++) {

            RoadPathUtility.sampleAlong(spoke, spoke.getLength() * random.nextRange(
                    EngineSetting.SETTLEMENT_BRANCH_START_FRACTION, EngineSetting.SETTLEMENT_BRANCH_END_FRACTION),
                    query);

            int side = random.next() < 0.5f ? -1 : 1;
            double tilt = random.nextSigned() * EngineSetting.SETTLEMENT_BRANCH_TILT_RADIANS;
            double headingX = RoadPathUtility.turnHeadingX(-query.getHeadingZ() * side, query.getHeadingX() * side,
                    tilt);
            double headingZ = RoadPathUtility.turnHeadingZ(-query.getHeadingZ() * side, query.getHeadingX() * side,
                    tilt);
            int length = random.nextInt(
                    settlementHandle.getBranchLengthBlocks().getMin(),
                    settlementHandle.getBranchLengthBlocks().getMax());
            double startX = query.getCenterX();
            double startZ = query.getCenterZ();
            float startY = query.getSurfaceY();

            if (length <= 0)
                continue;

            RoadPathUtility.buildCurve(
                    startX, startZ, startX + headingX * length, startZ + headingZ * length,
                    settlementHandle.getBend(), random.nextSeed(), scratch.pointX, scratch.pointZ);

            RoadPathStruct lane = roadManager.planPath(
                    draft.getWorldHandle(), street, scratch.pointX, scratch.pointZ,
                    startY, EngineSetting.ROAD_HEIGHT_FREE);

            settlementTrailBranch.addRoad(draft, lane);
            scratch.streets.add(lane);
        }
    }

    // Wall \\

    private void raiseWall(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            double centerX,
            double centerZ) {

        ArchitectureHandle architectureHandle = site.getArchitectureHandle();
        ArchitectureWallStruct wall = architectureHandle.getWall();

        RoadPathUtility.buildRing(
                centerX, centerZ, site.getSettlementHandle().getWallRadiusBlocks(),
                EngineSetting.SETTLEMENT_WALL_WOBBLE, random.nextSeed(), scratch.pointX, scratch.pointZ);

        RoadPathStruct centerline = roadManager.planPath(
                draft.getWorldHandle(), architectureHandle.getRoad(LayoutRoadKind.STREET),
                scratch.pointX, scratch.pointZ, EngineSetting.ROAD_HEIGHT_FREE, EngineSetting.ROAD_HEIGHT_FREE);

        cutGates(draft, scratch, site, random, centerline, wall);
        raiseTowers(draft, scratch, site, random, centerline, centerX, centerZ);

        draft.addWall(new LayoutWallStruct(
                centerline, wall, scratch.gateStarts.toFloatArray(), scratch.gateEnds.toFloatArray()));
        draft.addObstacle(centerline, wall.getHalfThicknessBlocks() + EngineSetting.LAYOUT_LOT_ROAD_GAP_BLOCKS);
    }

    // A gate wherever a street passes the wall, and a gatehouse over each main street there
    private void cutGates(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            RoadPathStruct centerline,
            ArchitectureWallStruct wall) {

        RoadHandle street = site.getArchitectureHandle().getRoad(LayoutRoadKind.STREET);
        RoadQueryStruct query = scratch.query;
        double crossing = street.getHalfWidthBlocks() + wall.getHalfThicknessBlocks();
        float gateHalf = (float) settlementTrailBranch.resolveClearance(street)
                + EngineSetting.SETTLEMENT_GATE_MARGIN_BLOCKS;
        float length = centerline.getLength();

        for (int s = 1; s < scratch.streets.size(); s++) {

            RoadPathStruct candidate = scratch.streets.get(s);
            int nearest = findNearestPoint(candidate, centerline, query);

            if (RoadPathUtility.locate(centerline, candidate.getPointX(nearest), candidate.getPointZ(nearest), query)
                    > crossing)
                continue;

            float along = query.getAlong();

            addGate(scratch, along - gateHalf, along + gateHalf, length);

            if (!scratch.spokes.contains(candidate))
                continue;

            StructureHandle gatehouse = settlementTrailBranch.drawRole(
                    site.getArchitectureHandle(), EngineSetting.SETTLEMENT_ROLE_GATEHOUSE, random);

            if (gatehouse == null)
                continue;

            RoadPathUtility.locate(candidate, candidate.getPointX(nearest), candidate.getPointZ(nearest), query);
            layoutManager.placeLotSpanning(
                    draft, gatehouse, query.getCenterX(), query.getCenterZ(), query.getSurfaceY(),
                    query.getHeadingX(), query.getHeadingZ());
        }
    }

    // The street point standing nearest the wall's centreline
    private int findNearestPoint(RoadPathStruct street, RoadPathStruct centerline, RoadQueryStruct query) {

        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;

        for (int i = 0; i < street.getPointCount(); i++) {

            double distance = RoadPathUtility.locate(centerline, street.getPointX(i), street.getPointZ(i), query);

            if (distance >= nearestDistance)
                continue;

            nearestDistance = distance;
            nearest = i;
        }

        return nearest;
    }

    // A stretch of the closed wall left open, carried over its seam where it runs past either end
    private void addGate(SettlementPlanAsyncContainer scratch, float start, float end, float length) {

        scratch.gateStarts.add(start);
        scratch.gateEnds.add(end);

        if (start < 0f) {
            scratch.gateStarts.add(length + start);
            scratch.gateEnds.add(length);
        }

        if (end > length) {
            scratch.gateStarts.add(0f);
            scratch.gateEnds.add(end - length);
        }
    }

    private boolean nearGate(SettlementPlanAsyncContainer scratch, float along, float clearance) {

        for (int i = 0; i < scratch.gateStarts.size(); i++)
            if (along >= scratch.gateStarts.getFloat(i) - clearance
                    && along <= scratch.gateEnds.getFloat(i) + clearance)
                return true;

        return false;
    }

    // Towers stood along the wall, facing out, every so many blocks clear of its gates
    private void raiseTowers(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            RoadPathStruct centerline,
            double centerX,
            double centerZ) {

        int spacing = site.getSettlementHandle().getTowerSpacingBlocks();
        RoadQueryStruct query = scratch.query;

        for (float along = spacing * 0.5f; along < centerline.getLength(); along += spacing) {

            if (nearGate(scratch, along, EngineSetting.SETTLEMENT_TOWER_GATE_CLEARANCE_BLOCKS))
                continue;

            StructureHandle tower = settlementTrailBranch.drawRole(
                    site.getArchitectureHandle(), EngineSetting.SETTLEMENT_ROLE_WALL_TOWER, random);

            if (tower == null)
                return;

            RoadPathUtility.sampleAlong(centerline, along, query);

            layoutManager.placeLotAt(
                    draft, tower,
                    StructurePlacementUtility.resolveQuarterTurns(
                            tower.getFront(), query.getCenterX() - centerX, query.getCenterZ() - centerZ),
                    Math.round(query.getCenterX()), Math.round(query.getCenterZ()), query.getSurfaceY());
        }
    }

    // Overpasses \\

    // Buildings bridged over the main streets, a little way out from the ring
    private void placeOverpasses(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        RoadQueryStruct query = scratch.query;
        int count = random.nextInt(
                settlementHandle.getOverpasses().getMin(), settlementHandle.getOverpasses().getMax());

        for (int i = 0; i < count && !scratch.spokes.isEmpty(); i++) {

            RoadPathStruct spoke = scratch.spokes.get(random.nextInt(0, scratch.spokes.size() - 1));
            StructureHandle overpass = settlementTrailBranch.drawRole(
                    site.getArchitectureHandle(), EngineSetting.SETTLEMENT_ROLE_OVERPASS, random);

            RoadPathUtility.sampleAlong(spoke, spoke.getLength() * random.nextRange(
                    EngineSetting.SETTLEMENT_OVERPASS_START_FRACTION, EngineSetting.SETTLEMENT_OVERPASS_END_FRACTION),
                    query);

            if (overpass == null || query.getSpan() != RoadSpanType.GROUND)
                continue;

            layoutManager.placeLotSpanning(
                    draft, overpass, query.getCenterX(), query.getCenterZ(), query.getSurfaceY(),
                    query.getHeadingX(), query.getHeadingZ());
        }
    }

    // Lots \\

    // Every street lined with lots in turn, its leading roles first and its fill role after
    private void fillLots(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random,
            double centerX,
            double centerZ) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        ArchitectureHandle architectureHandle = site.getArchitectureHandle();
        RoadHandle street = architectureHandle.getRoad(LayoutRoadKind.STREET);
        double frontDistance = settlementTrailBranch.resolveClearance(street) + settlementHandle.getSetbackBlocks();
        double radiusSq = (double) settlementHandle.getRadiusBlocks() * settlementHandle.getRadiusBlocks();
        RoadQueryStruct query = scratch.query;

        queueRoles(scratch, settlementHandle, random);

        int queued = 0;
        int attempts = 0;

        for (int s = 0; s < scratch.streets.size() && queued < scratch.roleQueue.size(); s++) {

            RoadPathStruct path = scratch.streets.get(s);
            float end = path.getLength() - EngineSetting.SETTLEMENT_LOT_EDGE_BLOCKS;

            for (float along = EngineSetting.SETTLEMENT_LOT_EDGE_BLOCKS; along < end
                    && queued < scratch.roleQueue.size(); along += EngineSetting.SETTLEMENT_LOT_STEP_BLOCKS) {

                RoadPathUtility.sampleAlong(path, along, query);

                double deltaX = query.getCenterX() - centerX;
                double deltaZ = query.getCenterZ() - centerZ;

                if (query.getSpan() != RoadSpanType.GROUND || deltaX * deltaX + deltaZ * deltaZ > radiusSq)
                    continue;

                for (int side = -1; side <= 1 && queued < scratch.roleQueue.size(); side += 2) {

                    StructureHandle structure = settlementTrailBranch.drawRole(
                            architectureHandle, scratch.roleQueue.get(queued), random);

                    boolean placed = structure != null && layoutManager.placeLotFacing(
                            draft, structure, query.getCenterX(), query.getCenterZ(), query.getSurfaceY(),
                            query.getHeadingX(), query.getHeadingZ(), side, frontDistance) != null;

                    if (placed || structure == null || ++attempts >= EngineSetting.SETTLEMENT_ROLE_ATTEMPTS) {
                        queued++;
                        attempts = 0;
                    }
                }
            }
        }
    }

    // The leading roles each as many times as drawn, then the fill role up to the lot limit
    private void queueRoles(
            SettlementPlanAsyncContainer scratch,
            SettlementHandle settlementHandle,
            LayoutRandomStruct random) {

        ObjectArrayList<SettlementRoleStruct> roles = settlementHandle.getRoles();

        for (int i = 0; i < roles.size(); i++) {

            SettlementRoleStruct role = roles.get(i);
            int count = random.nextInt(role.getCount().getMin(), role.getCount().getMax());

            for (int j = 0; j < count; j++)
                scratch.roleQueue.add(role.getRole());
        }

        while (scratch.roleQueue.size() < settlementHandle.getMaxLots())
            scratch.roleQueue.add(settlementHandle.getFillRole());
    }

    // Trails \\

    // Trails wandering out from the far ends of streets, turned a little from them
    private void layTrails(
            LayoutDraftStruct draft,
            SettlementPlanAsyncContainer scratch,
            SettlementSiteStruct site,
            LayoutRandomStruct random) {

        SettlementHandle settlementHandle = site.getSettlementHandle();
        int count = random.nextInt(settlementHandle.getOutposts().getMin(), settlementHandle.getOutposts().getMax());

        for (int i = 0; i < count && !scratch.exitX.isEmpty(); i++) {

            int exit = random.nextInt(0, scratch.exitX.size() - 1);
            double turn = random.nextSigned() * EngineSetting.SETTLEMENT_TRAIL_MAX_TURN_RADIANS;
            double headingX = scratch.exitHeadingX.getDouble(exit);
            double headingZ = scratch.exitHeadingZ.getDouble(exit);

            settlementTrailBranch.layTrail(
                    draft, scratch, site.getArchitectureHandle(), random,
                    scratch.exitX.getDouble(exit), scratch.exitZ.getDouble(exit), scratch.exitY.getFloat(exit),
                    RoadPathUtility.turnHeadingX(headingX, headingZ, turn),
                    RoadPathUtility.turnHeadingZ(headingX, headingZ, turn),
                    random.nextInt(settlementHandle.getTrailLengthBlocks().getMin(),
                            settlementHandle.getTrailLengthBlocks().getMax()),
                    settlementHandle.getOutpostChance());
        }
    }
}
