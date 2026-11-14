package application.bootstrap.worldpipeline.roadmanager;

import java.io.File;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.road.RoadBridgeStruct;
import application.bootstrap.worldpipeline.road.RoadData;
import application.bootstrap.worldpipeline.road.RoadHandle;
import application.bootstrap.worldpipeline.road.RoadTunnelStruct;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

class RoadBuilder extends BuilderPackage {

    /*
     * Parses one road ARPG file into a RoadData and wraps it in a RoadHandle.
     * A road names its "width_blocks" and its "surface", a list of blocks
     * each with an optional "weight", and may name a "shoulder" block and
     * width that edge it into the land, the "base_block" filled under it
     * where it runs above the ground, the "clearance_blocks" kept clear over
     * it, the steepest "max_grade" it climbs per block, whether its slopes
     * are "smooth_slopes" laid in half steps, and an optional "bridge" and
     * "tunnel". Every block and value is resolved and validated here, so a
     * malformed road fails at boot.
     */

    // Internal
    private RoadManager roadManager;
    private BlockManager blockManager;

    // Base \\

    @Override
    protected void get() {
        this.roadManager = get(RoadManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Build \\

    RoadHandle build(File file, String roadName) {

        short roadID = roadManager.registerRoadName(roadName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        float widthBlocks = ArpgUtility.validateFloat(arpg, "width_blocks");

        if (widthBlocks <= 0f || widthBlocks > EngineSetting.ROAD_MAX_WIDTH_BLOCKS)
            throwException("Road \"" + roadName + "\" \"width_blocks\" " + widthBlocks
                    + " — a road is wider than 0 and no wider than " + EngineSetting.ROAD_MAX_WIDTH_BLOCKS
                    + " blocks.");

        ArpgArrayStruct surfaceArpg = ArpgUtility.validateArray(arpg, "surface");
        short[] surfaceBlockIDs = new short[surfaceArpg.size()];
        FloatArrayList surfaceWeights = new FloatArrayList(surfaceArpg.size());
        boolean smoothSlopes = ArpgUtility.getBoolean(arpg, "smooth_slopes", true);

        for (int i = 0; i < surfaceArpg.size(); i++) {

            ArpgObjectStruct entry = surfaceArpg.get(i).getAsObject();
            BlockHandle blockHandle = resolveBlock(entry, "block", roadName);

            if (smoothSlopes && blockHandle.getGeometry() != DynamicGeometryType.FULL)
                throwException("Road \"" + roadName + "\" surface block \"" + blockHandle.getBlockName()
                        + "\" must be a FULL-geometry block to lay smooth slopes in half steps.");

            surfaceBlockIDs[i] = blockHandle.getBlockID();
            surfaceWeights.add(ArpgUtility.getFloat(entry, "weight", 1f));
        }

        float[] surfaceCumulativeWeights = WeightedTableUtility.buildCumulative(surfaceWeights, roadName);
        BlockHandle surfaceBlock = blockManager.getBlockHandleFromBlockID(surfaceBlockIDs[0]);

        boolean hasShoulder = ArpgUtility.hasObject(arpg, "shoulder");
        ArpgObjectStruct shoulderArpg = hasShoulder ? arpg.getAsObject("shoulder") : new ArpgObjectStruct();
        short shoulderBlockID = hasShoulder
                ? resolveBlock(shoulderArpg, "block", roadName).getBlockID()
                : surfaceBlockIDs[0];
        float shoulderWidthBlocks = hasShoulder ? ArpgUtility.validateFloat(shoulderArpg, "width_blocks") : 0f;

        short baseBlockID = arpg.has("base_block")
                ? resolveBlock(arpg, "base_block", roadName).getBlockID()
                : surfaceBlockIDs[0];

        int clearanceBlocks = ArpgUtility.getInt(arpg, "clearance_blocks", EngineSetting.DEFAULT_ROAD_CLEARANCE_BLOCKS);
        float maxGrade = ArpgUtility.getFloat(arpg, "max_grade", EngineSetting.DEFAULT_ROAD_MAX_GRADE);

        if (shoulderWidthBlocks < 0f || clearanceBlocks < 1)
            throwException("Road \"" + roadName + "\" needs a shoulder of at least 0 blocks and a clearance of at "
                    + "least 1 block.");

        if (maxGrade <= 0f || maxGrade > 1f)
            throwException("Road \"" + roadName + "\" \"max_grade\" " + maxGrade
                    + " — a road climbs more than 0 and at most 1 block per block.");

        RoadBridgeStruct bridge = parseBridge(arpg, roadName);
        RoadTunnelStruct tunnel = parseTunnel(arpg, roadName);

        RoadData roadData = new RoadData(
                roadName,
                roadID,
                RegistryUtility.toNameSeed(roadName),
                widthBlocks,
                surfaceBlockIDs,
                surfaceCumulativeWeights,
                smoothSlopes,
                shoulderBlockID,
                shoulderWidthBlocks,
                baseBlockID,
                clearanceBlocks,
                maxGrade,
                bridge,
                tunnel,
                surfaceBlock.getMapColorForFace(Direction3Vector.UP),
                surfaceBlock.getMapColorForFace(Direction3Vector.NORTH),
                bridge != null
                        ? blockManager.getBlockHandleFromBlockID(bridge.getDeckBlockID())
                                .getMapColorForFace(Direction3Vector.UP)
                        : surfaceBlock.getMapColorForFace(Direction3Vector.UP));

        RoadHandle roadHandle = create(RoadHandle.class);
        roadHandle.constructor(roadData);

        return roadHandle;
    }

    // Crossings \\

    private RoadBridgeStruct parseBridge(ArpgObjectStruct arpg, String roadName) {

        if (!ArpgUtility.hasObject(arpg, "bridge"))
            return null;

        ArpgObjectStruct bridgeArpg = arpg.getAsObject("bridge");
        int pillarSpacingBlocks = ArpgUtility.getInt(
                bridgeArpg, "pillar_spacing_blocks", EngineSetting.DEFAULT_ROAD_PILLAR_SPACING_BLOCKS);
        int minHeightBlocks = ArpgUtility.getInt(
                bridgeArpg, "min_height_blocks", EngineSetting.DEFAULT_ROAD_BRIDGE_MIN_HEIGHT_BLOCKS);

        if (pillarSpacingBlocks < 1 || minHeightBlocks < 1)
            throwException("Road \"" + roadName + "\" bridge needs a pillar spacing and a minimum height of at "
                    + "least 1 block.");

        return new RoadBridgeStruct(
                resolveBlock(bridgeArpg, "deck_block", roadName).getBlockID(),
                resolveBlock(bridgeArpg, "rail_block", roadName).getBlockID(),
                resolveBlock(bridgeArpg, "pillar_block", roadName).getBlockID(),
                pillarSpacingBlocks,
                minHeightBlocks);
    }

    private RoadTunnelStruct parseTunnel(ArpgObjectStruct arpg, String roadName) {

        if (!ArpgUtility.hasObject(arpg, "tunnel"))
            return null;

        ArpgObjectStruct tunnelArpg = arpg.getAsObject("tunnel");
        int heightBlocks = ArpgUtility.getInt(
                tunnelArpg, "height_blocks", EngineSetting.DEFAULT_ROAD_TUNNEL_HEIGHT_BLOCKS);
        int minDepthBlocks = ArpgUtility.getInt(
                tunnelArpg, "min_depth_blocks", EngineSetting.DEFAULT_ROAD_TUNNEL_MIN_DEPTH_BLOCKS);

        if (heightBlocks < EngineSetting.ROAD_TUNNEL_MIN_HEIGHT_BLOCKS || minDepthBlocks <= heightBlocks)
            throwException("Road \"" + roadName + "\" tunnel must be at least "
                    + EngineSetting.ROAD_TUNNEL_MIN_HEIGHT_BLOCKS
                    + " blocks high and need more ground over the road than its height before it bores.");

        return new RoadTunnelStruct(
                resolveBlock(tunnelArpg, "lining_block", roadName).getBlockID(),
                heightBlocks,
                minDepthBlocks);
    }

    // Blocks \\

    private BlockHandle resolveBlock(ArpgObjectStruct arpg, String key, String roadName) {
        return blockManager.getSolidBlockHandleFromBlockName(ArpgUtility.validateString(arpg, key), roadName);
    }
}
