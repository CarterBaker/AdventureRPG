package application.bootstrap.worldpipeline.worlditem;

import application.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemPaletteHandle extends HandlePackage {

    private ObjectArrayList<WorldItemStruct> items;

    // Constructor \\

    public void constructor() {
        this.items = new ObjectArrayList<>();
    }

    // Management \\

    public void addItem(WorldItemStruct item) {
        items.add(item);
    }

    public void removeItem(WorldItemStruct item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    // Accessible \\

    public ObjectArrayList<WorldItemStruct> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}