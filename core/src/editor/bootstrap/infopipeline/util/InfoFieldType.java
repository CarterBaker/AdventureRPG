package editor.bootstrap.infopipeline.util;

import engine.util.arpg.ArpgElementStruct;

public enum InfoFieldType {

    /*
     * The kind of value a schema field holds. Primitive kinds are edited in
     * place; OBJECT, ARRAY, and MAP are groups whose children are listed
     * beneath them. RAW accepts any value and is edited as raw ARPG text,
     * which is also how any value that does not match its schema is shown.
     */

    STRING("string"),
    INT("int"),
    FLOAT("float"),
    BOOLEAN("boolean"),
    ENUM("enum"),
    OBJECT("object"),
    ARRAY("array"),
    MAP("map"),
    RAW("raw");

    // Internal
    private final String schemaName;

    // Constructor \\

    InfoFieldType(String schemaName) {
        this.schemaName = schemaName;
    }

    // Utility \\

    public static InfoFieldType fromSchemaName(String schemaName) {

        for (InfoFieldType type : values())
            if (type.schemaName.equals(schemaName))
                return type;

        return null;
    }

    public boolean accepts(ArpgElementStruct value) {

        return switch (this) {
            case STRING, ENUM -> value.isValue() && value.getAsValue().isString();
            case INT, FLOAT -> value.isValue() && value.getAsValue().isNumber();
            case BOOLEAN -> value.isValue() && value.getAsValue().isBoolean();
            case OBJECT, MAP -> value.isObject();
            case ARRAY -> value.isArray();
            case RAW -> true;
        };
    }

    public boolean isGroup() {
        return this == OBJECT || this == ARRAY || this == MAP;
    }

    // Accessible \\

    public String getSchemaName() {
        return schemaName;
    }
}
