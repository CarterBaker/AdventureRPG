package program.bootstrap.itempipeline.backpack;

import program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import program.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BackpackInstance extends InstancePackage {

    /*
     * Per-entity runtime inventory container. Holds an ordered list of item
     * definition handles representing the contents of a backpack slot. No
     * manager owns this — it lives directly on InventoryHandle.
     */

    // Internal
    private ObjectArrayList<ItemDefinitionHandle> items;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.items = new ObjectArrayList<>();
    }

    // Accessible \\

    public void addItem(ItemDefinitionHandle item) {
        items.add(item);
    }

    public void removeItem(ItemDefinitionHandle item) {
        items.remove(item);
    }

    public ObjectArrayList<ItemDefinitionHandle> getItems() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }
}