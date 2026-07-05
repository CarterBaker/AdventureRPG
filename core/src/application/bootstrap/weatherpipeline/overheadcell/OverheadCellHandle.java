package application.bootstrap.weatherpipeline.overheadcell;

import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class OverheadCellHandle extends HandlePackage {

    /*
     * One streamed overhead weather cell. Identity is its wrapped grid
     * index — stable for the lifetime this cell is streamed in, and always
     * sufficient on its own to re-derive this cell's canonical chunk-space
     * center (see getCenterChunkX()/getCenterChunkY()), so no derived
     * position is ever stored redundantly alongside the index it comes
     * from — there is nothing here that could ever disagree with itself.
     *
     * currentWeather/currentCloud are resolved exactly once, at
     * construction, by OverheadManager.buildCell() — never re-rolled on
     * later frames. A physical cloud drifting overhead keeps its own
     * identity as it moves; it only changes when something explicit
     * decides it should (a biome/season boundary crossing, handled by a
     * later transition step), never because the same coordinate happened
     * to resample slightly different noise on a later frame.
     */

    // Identity — wrapped grid index, stable storage key
    private int cellGridX;
    private int cellGridY;

    // Weather — persistent, not re-rolled per frame
    private WeatherHandle currentWeather;
    private CloudChanceStruct currentCloud;

    // Constructor \\

    public void constructor(
            int cellGridX,
            int cellGridY,
            WeatherHandle currentWeather,
            CloudChanceStruct currentCloud) {

        // Identity
        this.cellGridX = cellGridX;
        this.cellGridY = cellGridY;

        // Weather
        this.currentWeather = currentWeather;
        this.currentCloud = currentCloud;
    }

    // Accessible \\

    public int getCellGridX() {
        return cellGridX;
    }

    public int getCellGridY() {
        return cellGridY;
    }

    public int getCenterChunkX() {
        return cellGridX * EngineSetting.OVERHEAD_CELL_SIZE + EngineSetting.OVERHEAD_CELL_SIZE / 2;
    }

    public int getCenterChunkY() {
        return cellGridY * EngineSetting.OVERHEAD_CELL_SIZE + EngineSetting.OVERHEAD_CELL_SIZE / 2;
    }

    public WeatherHandle getCurrentWeather() {
        return currentWeather;
    }

    public CloudChanceStruct getCurrentCloud() {
        return currentCloud;
    }
}