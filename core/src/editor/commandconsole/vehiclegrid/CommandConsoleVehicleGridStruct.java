package editor.commandconsole.vehiclegrid;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleVehicleGridStruct extends StructPackage {

    /*
     * One vehicle grid in the command console's tree: the scrolling grid
     * element, the command every tile in it runs, how many columns its rows
     * are laid out in, and each tile with the vehicle it hands to the command.
     */

    // Grid
    private final ElementInstance gridElement;
    private final CommandHandle commandHandle;
    private int columns;

    // Tiles
    private final ObjectArrayList<ElementInstance> tileElements;
    private final ObjectArrayList<VehicleHandle> tileVehicles;

    // Constructor \\

    public CommandConsoleVehicleGridStruct(ElementInstance gridElement, CommandHandle commandHandle) {

        // Grid
        this.gridElement = gridElement;
        this.commandHandle = commandHandle;
        this.columns = EngineSetting.INDEX_NOT_FOUND;

        // Tiles
        this.tileElements = new ObjectArrayList<>();
        this.tileVehicles = new ObjectArrayList<>();
    }

    // Management \\

    public void addTile(ElementInstance tileElement, VehicleHandle vehicle) {
        tileElements.add(tileElement);
        tileVehicles.add(vehicle);
    }

    public void clearTiles() {
        tileElements.clear();
        tileVehicles.clear();
    }

    // Accessible \\

    public ElementInstance getGridElement() {
        return gridElement;
    }

    public CommandHandle getCommandHandle() {
        return commandHandle;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public int getTileCount() {
        return tileElements.size();
    }

    public int indexOfTile(ElementInstance tileElement) {
        return tileElements.indexOf(tileElement);
    }

    public ElementInstance getTileElement(int index) {
        return tileElements.get(index);
    }

    public VehicleHandle getTileVehicle(int index) {
        return tileVehicles.get(index);
    }
}
