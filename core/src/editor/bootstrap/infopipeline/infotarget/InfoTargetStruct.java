package editor.bootstrap.infopipeline.infotarget;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.StructPackage;
import engine.util.arpg.ArpgElementStruct;

public class InfoTargetStruct extends StructPackage {

    /*
     * What a row path resolves to inside an entry: the object or array that
     * holds the value, the key or index it sits under, the schema field that
     * describes it, and the value itself, if present. The field and its
     * container are null where the schema says nothing, and the value is null
     * for an optional field not yet added.
     */

    // Container
    private final ArpgElementStruct container;
    private final InfoFieldStruct containerField;

    // Slot
    private final String key;
    private final int index;

    // Value
    private final InfoFieldStruct field;
    private final ArpgElementStruct value;

    // Constructor \\

    public InfoTargetStruct(
            ArpgElementStruct container,
            InfoFieldStruct containerField,
            String key,
            int index,
            InfoFieldStruct field,
            ArpgElementStruct value) {

        // Container
        this.container = container;
        this.containerField = containerField;

        // Slot
        this.key = key;
        this.index = index;

        // Value
        this.field = field;
        this.value = value;
    }

    // Value \\

    public InfoFieldType resolveType() {

        if (field == null)
            return InfoFieldType.RAW;

        if (value != null && !field.getType().accepts(value))
            return InfoFieldType.RAW;

        return field.getType();
    }

    public void write(ArpgElementStruct newValue) {

        if (container.isArray())
            container.getAsArray().set(index, newValue);
        else
            container.getAsObject().add(key, newValue);
    }

    public void remove() {

        if (container.isArray())
            container.getAsArray().remove(index);
        else
            container.getAsObject().remove(key);
    }

    // Accessible \\

    public ArpgElementStruct getContainer() {
        return container;
    }

    public boolean isInArray() {
        return container.isArray();
    }

    public InfoFieldStruct getContainerField() {
        return containerField;
    }

    public String getKey() {
        return key;
    }

    public int getIndex() {
        return index;
    }

    public InfoFieldStruct getField() {
        return field;
    }

    public ArpgElementStruct getValue() {
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }
}
