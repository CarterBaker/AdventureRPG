package editor.bootstrap.infopipeline.util;

import com.google.gson.JsonElement;

public enum InfoFieldType {

    /*
     * The kind of value a schema field holds. Primitive kinds are edited in
     * place; OBJECT, ARRAY, and MAP are groups whose children are listed
     * beneath them. JSON accepts any value and is edited as raw JSON text,
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
    JSON("json");

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

    public boolean accepts(JsonElement value) {

        return switch (this) {
            case STRING, ENUM -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
            case INT, FLOAT -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
            case BOOLEAN -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
            case OBJECT, MAP -> value.isJsonObject();
            case ARRAY -> value.isJsonArray();
            case JSON -> true;
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
