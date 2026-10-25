package editor.bootstrap.infopipeline.infoschema;

import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.StructPackage;
import engine.util.arpg.ArpgElementStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InfoFieldStruct extends StructPackage {

    /*
     * One field a schema allows. Object fields list their children, arrays and
     * maps describe their element, and enums their values. A field left out of
     * its object is optional unless marked required. The default is the value
     * a newly added primitive starts from; groups start from their structure.
     * A fixed length pins an array's size, so its elements can be edited but
     * never added or removed. A unique field must differ from every other
     * entry of its schema, so a copy has to be given a new value for it, and a
     * distinct field from the same field of every other element of its array.
     * A reference says what a string must name, a key reference what every
     * key of a map must name, and its rule what else its value must pass.
     */

    // Identity
    private final String key;
    private final InfoFieldType type;
    private final boolean required;
    private final boolean unique;
    private final boolean distinct;

    // Value
    private final ArpgElementStruct defaultValue;
    private final ObjectArrayList<String> values;

    // Rules
    private final InfoReferenceStruct reference;
    private final InfoReferenceStruct keyReference;
    private final InfoRuleStruct rule;

    // Structure
    private final ObjectArrayList<InfoFieldStruct> fields;
    private final InfoFieldStruct element;
    private final int length;

    // Constructor \\

    public InfoFieldStruct(
            String key,
            InfoFieldType type,
            boolean required,
            boolean unique,
            boolean distinct,
            ArpgElementStruct defaultValue,
            ObjectArrayList<String> values,
            InfoReferenceStruct reference,
            InfoReferenceStruct keyReference,
            InfoRuleStruct rule,
            ObjectArrayList<InfoFieldStruct> fields,
            InfoFieldStruct element,
            int length) {

        // Identity
        this.key = key;
        this.type = type;
        this.required = required;
        this.unique = unique;
        this.distinct = distinct;

        // Value
        this.defaultValue = defaultValue;
        this.values = values;

        // Rules
        this.reference = reference;
        this.keyReference = keyReference;
        this.rule = rule;

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

    // Rules \

    public boolean hasReference() {
        return reference != null;
    }

    public boolean hasKeyReference() {
        return keyReference != null;
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

    public boolean isUnique() {
        return unique;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public ArpgElementStruct getDefaultValue() {
        return defaultValue;
    }

    public ObjectArrayList<String> getValues() {
        return values;
    }

    public InfoReferenceStruct getReference() {
        return reference;
    }

    public InfoReferenceStruct getKeyReference() {
        return keyReference;
    }

    public InfoRuleStruct getRule() {
        return rule;
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
