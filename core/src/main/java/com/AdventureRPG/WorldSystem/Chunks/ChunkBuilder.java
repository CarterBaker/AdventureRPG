package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.PackedCoordinate3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Blocks.Type;

public class ChunkBuilder {

    // Game Manager
    private final WorldSystem worldSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;

    // Settings
    private final int WORLD_HEIGHT;

    public ChunkBuilder(WorldSystem worldSystem) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;

        // Settings
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;
    }

    public void build(Chunk chunk, int subChunkIndex) {

        SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

        for (int axisIndex = 0; axisIndex < Axis.values().length; axisIndex++) {

            Axis axis = Axis.VALUES[axisIndex];

            for (int index = 0; index < packedCoordinate3Int.chunkSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                int aX = packedCoordinate3Int.unpackX(xyz);
                int aY = packedCoordinate3Int.unpackY(xyz);
                int aZ = packedCoordinate3Int.unpackZ(xyz);

                int blockID = subChunk.getBlock(aX, aY, aZ);
                Type type = worldSystem.getBlockType(blockID);

                if (type == Type.NULL)
                    return;

                int biomeID = subChunk.getBiome(aX, aY, aZ);

                for (Direction3Int direction : axis.getDirections()) {

                    if (compareBlocks(
                            chunk,
                            subChunkIndex,
                            aX, aY, aZ,
                            axis, direction,
                            type)) {
                        assembleFace();
                    }
                }
            }
        }
    }

    private void assembleFace() {

    }

    private boolean compareBlocks(
            Chunk chunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Axis axis, Direction3Int direction,
            Type type) {

        int bX = packedCoordinate3Int.addAndWrapAxis(direction.x, aX);
        int bY = packedCoordinate3Int.addAndWrapAxis(direction.y, aY);
        int bZ = packedCoordinate3Int.addAndWrapAxis(direction.z, aZ);

        SubChunk comparativeSubChunk = getComparativeSubChunk(
                chunk,
                subChunkIndex,
                bX, bY, bZ,
                direction);

        int blockID = comparativeSubChunk.getBlock(bX, bY, bZ);
        Type comparativeType = worldSystem.getBlockType(blockID);

        return comparativeType != type;
    }

    private SubChunk getComparativeSubChunk(
            Chunk chunk,
            int subChunkIndex,
            int bX, int bY, int bZ,
            Direction3Int direction) {

        boolean isOverEdge = packedCoordinate3Int.isOverEdge(bX, bY, bZ, direction);

        if (!isOverEdge)
            return chunk.getSubChunk(subChunkIndex);

        if (direction == Direction3Int.UP || direction == Direction3Int.DOWN) {

            int outputSubChunk = subChunkIndex + direction.y;

            if (outputSubChunk > 0 && outputSubChunk < WORLD_HEIGHT)
                return chunk.getSubChunk(outputSubChunk);
            else
                return null;
        } else {

            Direction2Int direction2Int = direction.direction2Int;
            Chunk neighborChunk = chunk.getNeighborChunk(direction2Int);
            SubChunk outputSubChunk = neighborChunk.getSubChunk(subChunkIndex);

            return outputSubChunk;
        }
    }

    // Utility \\

    private enum Axis {
        X(Direction3Int.EAST, Direction3Int.WEST),
        Y(Direction3Int.UP, Direction3Int.DOWN),
        Z(Direction3Int.NORTH, Direction3Int.SOUTH);

        private final Direction3Int[] directions;

        Axis(Direction3Int pos, Direction3Int neg) {
            this.directions = new Direction3Int[] { pos, neg };
        }

        public Direction3Int[] getDirections() {
            return directions;
        }

        public static final Axis[] VALUES = { X, Y, Z };
    }
}
