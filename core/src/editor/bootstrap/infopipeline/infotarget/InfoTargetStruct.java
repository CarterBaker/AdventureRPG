package editor.bootstrap.infopipeline.infotarget;

import com.google.gson.JsonElement;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.StructPackage;

public class InfoTargetStruct extends StructPackage {

    /*
     * What a row path resolves to inside an entry: the object or array that
     * holds the value, the key or index it sits under, the schema field that
     * describes it, and the value itself, if present. The field and its
     * container are null where the schema says nothing, and the value is null
     * for an optional field not yet added.
     */

    // Container
    private final JsonElement container;
    private final InfoFieldStruct containerField;

    // Slot
    private final String key;
    private final int index;

    // Value
    private final InfoFieldStruct field;
    private final JsonElement value;

    // Constructor \\

    public InfoTargetStruct(
            JsonElement container,
            InfoFieldStruct containerField,
            String key,
            int index,
            InfoFieldStruct field,
            JsonElement value) {

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
            return InfoFieldType.JSON;

        if (value != null && !field.getType().accepts(value))
            return InfoFieldType.JSON;

        return field.getType();
    }

    public void write(JsonElement newValue) {

        if (container.isJsonArray())
            container.getAsJsonArray().set(index, newValue);
        else
            container.getAsJsonObject().add(key, newValue);
    }

    public void remove() {

        if (container.isJsonArray())
            container.getAsJsonArray().remove(index);
        else
            container.getAsJsonObject().remove(key);
    }

    // Accessible \\

    public JsonElement getContainer() {
        return container;
    }

    public boolean isInArray() {
        return container.isJsonArray();
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

    public JsonElement getValue() {
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }
}
