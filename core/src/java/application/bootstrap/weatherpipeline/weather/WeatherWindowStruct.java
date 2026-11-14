package application.bootstrap.weatherpipeline.weather;

import engine.root.StructPackage;

public class WeatherWindowStruct extends StructPackage {

    /*
     * The square of weather-image cells one grid currently reads, centred on
     * the cell above its reference chunk. originCellX/Z is the noise-space
     * cell at window index (0, 0), before wrapping. mapOriginX/ZBlocks is the
     * distance in blocks from that cell's corner to the grid's reference chunk
     * corner — the offset the shader adds to a position relative to the
     * reference chunk to find where it falls on the window. Reused scratch,
     * rewritten by WeatherPatternManager.resolveWindow().
     */

    private int originCellX;
    private int originCellZ;
    private float mapOriginXBlocks;
    private float mapOriginZBlocks;

    public void set(int originCellX, int originCellZ, float mapOriginXBlocks, float mapOriginZBlocks) {
        this.originCellX = originCellX;
        this.originCellZ = originCellZ;
        this.mapOriginXBlocks = mapOriginXBlocks;
        this.mapOriginZBlocks = mapOriginZBlocks;
    }

    public int getOriginCellX() {
        return originCellX;
    }

    public int getOriginCellZ() {
        return originCellZ;
    }

    public float getMapOriginXBlocks() {
        return mapOriginXBlocks;
    }

    public float getMapOriginZBlocks() {
        return mapOriginZBlocks;
    }
}
