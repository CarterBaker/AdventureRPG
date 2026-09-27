package editor.commandconsole.itemgrid;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleItemGridStruct extends StructPackage {

    /*
     * One item grid in the command console's tree: the scrolling grid
     * element, the command every tile in it runs, how many columns its rows
     * are laid out in, and each tile with the area its icon is drawn in and
     * the item it hands to the command.
     */

    // Grid
    private final ElementInstance gridElement;
    private final CommandHandle commandHandle;
    private int columns;

    // Tiles
    private final ObjectArrayList<ElementInstance> tileElements;
    private final ObjectArrayList<ElementInstance> iconElements;
    private final ObjectArrayList<ItemDefinitionHandle> tileItems;

    // Constructor \\

    public CommandConsoleItemGridStruct(ElementInstance gridElement, CommandHandle commandHandle) {

        // Grid
        this.gridElement = gridElement;
        this.commandHandle = commandHandle;
        this.columns = EngineSetting.INDEX_NOT_FOUND;

        // Tiles
        this.tileElements = new ObjectArrayList<>();
        this.iconElements = new ObjectArrayList<>();
        this.tileItems = new ObjectArrayList<>();
    }

    // Management \\

    public void addTile(ElementInstance tileElement, ElementInstance iconElement, ItemDefinitionHandle item) {
        tileElements.add(tileElement);
        iconElements.add(iconElement);
        tileItems.add(item);
    }

    public void clearTiles() {
        tileElements.clear();
        iconElements.clear();
        tileItems.clear();
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

    public ElementInstance getIconElement(int index) {
        return iconElements.get(index);
    }

    public ItemDefinitionHandle getTileItem(int index) {
        return tileItems.get(index);
    }
}
