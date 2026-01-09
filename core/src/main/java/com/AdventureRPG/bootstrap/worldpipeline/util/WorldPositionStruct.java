package com.AdventureRPG.bootstrap.worldpipeline.util;

import com.AdventureRPG.core.engine.StructPackage;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;

public class WorldPositionStruct extends StructPackage {

    // Internal
    private Vector3 position = new Vector3();
    private long chunkCoordinate = Coordinate2Int.pack(0, 0);

    // Accessible \\

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 position) {
        this.position.set(position);
    }

    public long getChunkCoordinate() {
        return chunkCoordinate;
    }

    public void setChunkCoordinate(long chunkCoordinate) {
        this.chunkCoordinate = chunkCoordinate;
    }
}
