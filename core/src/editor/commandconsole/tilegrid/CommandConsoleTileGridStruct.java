package editor.commandconsole.tilegrid;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleTileGridStruct<T> extends StructPackage {

    /*
     * One category's grid of tiles in the command console's tree: the grid
     * element, the command every tile in it runs, how many columns its rows
     * are laid out in, each entry it lists with the argument it hands to the
     * command and the name its tile shows, and the tile and icon area laid
     * out for each entry, in the same order.
     */

    // Grid
    private final ElementInstance gridElement;
    private final CommandHandle commandHandle;
    private int columns;

    // Entries
    private final ObjectArrayList<T> entries;
    private final ObjectArrayList<String> entryArguments;
    private final ObjectArrayList<String> entryLabels;

    // Tiles
    private final ObjectArrayList<ElementInstance> tileElements;
    private final ObjectArrayList<ElementInstance> iconElements;

    // Constructor \\

    public CommandConsoleTileGridStruct(ElementInstance gridElement, CommandHandle commandHandle) {

        // Grid
        this.gridElement = gridElement;
        this.commandHandle = commandHandle;
        this.columns = EngineSetting.INDEX_NOT_FOUND;

        // Entries
        this.entries = new ObjectArrayList<>();
        this.entryArguments = new ObjectArrayList<>();
        this.entryLabels = new ObjectArrayList<>();

        // Tiles
        this.tileElements = new ObjectArrayList<>();
        this.iconElements = new ObjectArrayList<>();
    }

    // Management \\

    public void addEntry(T entry, String argument, String label) {
        entries.add(entry);
        entryArguments.add(argument);
        entryLabels.add(label);
    }

    public void addTile(ElementInstance tileElement, ElementInstance iconElement) {
        tileElements.add(tileElement);
        iconElements.add(iconElement);
    }

    public void clearTiles() {
        tileElements.clear();
        iconElements.clear();
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

    public int getEntryCount() {
        return entries.size();
    }

    public T getEntry(int index) {
        return entries.get(index);
    }

    public String getEntryArgument(int index) {
        return entryArguments.get(index);
    }

    public String getEntryLabel(int index) {
        return entryLabels.get(index);
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

    public ElementInstance getIconElement(int index) {
        return iconElements.get(index);
    }
}
