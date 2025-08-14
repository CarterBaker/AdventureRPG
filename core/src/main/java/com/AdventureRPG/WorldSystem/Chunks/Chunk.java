package com.AdventureRPG.WorldSystem.Chunks;

import com.AdventureRPG.Util.Coordinate2Int;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class Chunk {

    // Chunk
    public final long coordinate;
    public final int coordinateX, coordinateY;

    // Neighbors
    public final long north;
    public final long south;
    public final long east;
    public final long west;

    // Data
    private int[][][] biomes;
    private int[][][] blocks;

    // Position
    public long position;
    public int positionX, positionY;

    // Model Instance
    public ModelInstance modelInstance;

    // Base \\

    public Chunk(WorldSystem worldSystem, long coordinate) {

        // Chunk
        this.coordinate = coordinate;
        this.coordinateX = Coordinate2Int.unpackX(coordinate);
        this.coordinateY = Coordinate2Int.unpackY(coordinate);

        // Neighbors
        this.north = Coordinate2Int.add(coordinate, Direction2Int.NORTH.packed);
        worldSystem.wrapAroundWorld(north);
        this.south = Coordinate2Int.add(coordinate, Direction2Int.SOUTH.packed);
        worldSystem.wrapAroundWorld(south);
        this.east = Coordinate2Int.add(coordinate, Direction2Int.EAST.packed);
        worldSystem.wrapAroundWorld(east);
        this.west = Coordinate2Int.add(coordinate, Direction2Int.WEST.packed);
        worldSystem.wrapAroundWorld(west);
    }

    // Data \\

    public void generate(int[][][] biomes, int[][][] blocks) {
        this.biomes = biomes;
        this.blocks = blocks;
    }

    // Position \\

    public void moveTo(long position) {

        this.position = position;
        this.positionX = Coordinate2Int.unpackX(position);
        this.positionY = Coordinate2Int.unpackY(position);
    }

    // Model Instance \\

    public boolean tryBuild(Chunk[] neighbors) {
        return false;
    }

    // Utility \\

    public void dispose() {

    }
}
