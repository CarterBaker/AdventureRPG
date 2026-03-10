package com.internal.bootstrap.itempipeline.backpack;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BackpackInstance extends InstancePackage {

    private ObjectArrayList<ItemDefinitionHandle> items;

    @Override
    protected void create() {
        this.items = new ObjectArrayList<>();
    }

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