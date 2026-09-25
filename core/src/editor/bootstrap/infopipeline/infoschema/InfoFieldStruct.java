package editor.bootstrap.infopipeline.infoschema;

import com.google.gson.JsonElement;

import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoFieldStruct extends StructPackage {

    /*
     * One field a schema allows. Object fields list their children, arrays and
     * maps describe their element, and enums their values. A field left out of
     * its object is optional unless marked required. The default is the value
     * a newly added primitive starts from; groups start from their structure.
     * A fixed length pins an array's size, so its elements can be edited but
     * never added or removed.
     */

    // Identity
    private final String key;
    private final InfoFieldType type;
    private final boolean required;

    // Value
    private final JsonElement defaultValue;
    private final ObjectArrayList<String> values;

    // Structure
    private final ObjectArrayList<InfoFieldStruct> fields;
    private final InfoFieldStruct element;
    private final int length;

    // Constructor \\

    public InfoFieldStruct(
            String key,
            InfoFieldType type,
            boolean required,
            JsonElement defaultValue,
            ObjectArrayList<String> values,
            ObjectArrayList<InfoFieldStruct> fields,
            InfoFieldStruct element,
            int length) {

        // Identity
        this.key = key;
        this.type = type;
        this.required = required;

        // Value
        this.defaultValue = defaultValue;
        this.values = values;

        // Structure
        this.fields = fields;
        this.element = element;
        this.length = length;
    }

    // Structure \\

    public InfoFieldStruct findField(String fieldKey) {

        for (int i = 0; i < fields.size(); i++)
            if (fields.get(i).getKey().equals(fieldKey))
                return fields.get(i);

        return null;
    }

    public boolean isFixedLength() {
        return length > 0;
    }

    // Accessible \\

    public String getKey() {
        return key;
    }

    public InfoFieldType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public JsonElement getDefaultValue() {
        return defaultValue;
    }

    public ObjectArrayList<String> getValues() {
        return values;
    }

    public ObjectArrayList<InfoFieldStruct> getFields() {
        return fields;
    }

    public InfoFieldStruct getElement() {
        return element;
    }

    public int getLength() {
        return length;
    }
}
