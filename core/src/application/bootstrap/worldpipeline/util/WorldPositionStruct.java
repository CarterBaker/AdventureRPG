package application.bootstrap.worldpipeline.util;

import application.core.engine.StructPackage;
import application.core.util.mathematics.extras.Coordinate2Long;
import application.core.util.mathematics.vectors.Vector3;

public class WorldPositionStruct extends StructPackage {

    // Internal
    private Vector3 position = new Vector3();
    private long chunkCoordinate = Coordinate2Long.pack(0, 0);

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
