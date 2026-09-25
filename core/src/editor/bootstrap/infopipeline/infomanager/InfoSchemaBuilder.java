package editor.bootstrap.infopipeline.infomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaData;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.BuilderPackage;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoSchemaBuilder extends BuilderPackage {

    /*
     * Parses one schema JSON file into an InfoSchemaHandle. The entry itself
     * is an object field holding the schema's "fields"; enums need values,
     * objects need fields, and arrays and maps need an element. A primitive
     * without a "default" starts from the natural empty value of its type,
     * and every default is checked against its type so a malformed schema
     * fails at boot. An array layout must declare its name field.
     */

    // Build \\

    InfoSchemaHandle build(File file, String schemaName) {

        JsonObject json = JsonUtility.loadJsonObject(file);

        String tabName = JsonUtility.validateString(json, "tab");
        int order = JsonUtility.getInt(json, "order", 0);
        String directory = JsonUtility.validateString(json, "directory");
        String entriesKey = JsonUtility.getString(json, "entries", null);
        String nameField = entriesKey != null ? JsonUtility.validateString(json, "name_field") : null;
        String defaultFile = entriesKey != null ? JsonUtility.validateString(json, "default_file") : null;

        InfoFieldStruct rootField = new InfoFieldStruct(
                null,
                InfoFieldType.OBJECT,
                true,
                null,
                new ObjectArrayList<>(),
                parseFields(JsonUtility.validateArray(json, "fields"), schemaName),
                null,
                0);

        if (nameField != null && !isRequiredString(rootField.findField(nameField)))
            throwException("Info schema '" + schemaName + "' must declare its name field '" + nameField
                    + "' as a required string.");

        InfoSchemaData schemaData = new InfoSchemaData(
                schemaName,
                tabName,
                order,
                directory,
                entriesKey,
                nameField,
                defaultFile,
                rootField);

        InfoSchemaHandle schemaHandle = create(InfoSchemaHandle.class);
        schemaHandle.constructor(schemaData);

        return schemaHandle;
    }

    // Parse \\

    private ObjectArrayList<InfoFieldStruct> parseFields(JsonArray fieldsJson, String schemaName) {

        ObjectArrayList<InfoFieldStruct> fields = new ObjectArrayList<>(fieldsJson.size());

        for (int i = 0; i < fieldsJson.size(); i++) {

            InfoFieldStruct field = parseField(fieldsJson.get(i).getAsJsonObject(), true, schemaName);

            for (int j = 0; j < fields.size(); j++)
                if (fields.get(j).getKey().equals(field.getKey()))
                    throwException("Info schema '" + schemaName + "' declares field '" + field.getKey()
                            + "' twice in one object.");

            fields.add(field);
        }

        return fields;
    }

    private InfoFieldStruct parseField(JsonObject fieldJson, boolean keyed, String schemaName) {

        String key = keyed ? JsonUtility.validateString(fieldJson, "key") : null;
        String typeName = JsonUtility.validateString(fieldJson, "type");
        InfoFieldType type = InfoFieldType.fromSchemaName(typeName);

        if (type == null)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has unknown type '"
                    + typeName + "'.");

        boolean required = JsonUtility.getBoolean(fieldJson, "required", false);
        ObjectArrayList<String> values = type == InfoFieldType.ENUM
                ? parseValues(fieldJson, key, schemaName)
                : new ObjectArrayList<>();
        ObjectArrayList<InfoFieldStruct> fields = type == InfoFieldType.OBJECT
                ? parseFields(JsonUtility.validateArray(fieldJson, "fields"), schemaName)
                : new ObjectArrayList<>();
        InfoFieldStruct element = type == InfoFieldType.ARRAY || type == InfoFieldType.MAP
                ? parseField(JsonUtility.validateObject(fieldJson, "element"), false, schemaName)
                : null;
        int length = type == InfoFieldType.ARRAY ? JsonUtility.getInt(fieldJson, "length", 0) : 0;
        JsonElement defaultValue = type.isGroup() ? null : parseDefault(fieldJson, type, values, key, schemaName);

        if (length < 0)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a negative length.");

        return new InfoFieldStruct(key, type, required, defaultValue, values, fields, element, length);
    }

    private ObjectArrayList<String> parseValues(JsonObject fieldJson, String key, String schemaName) {

        JsonArray valuesJson = JsonUtility.validateArray(fieldJson, "values");
        ObjectArrayList<String> values = new ObjectArrayList<>(valuesJson.size());

        for (int i = 0; i < valuesJson.size(); i++)
            values.add(valuesJson.get(i).getAsString());

        if (values.isEmpty())
            throwException("Info schema '" + schemaName + "' enum field '" + key + "' lists no values.");

        return values;
    }

    private JsonElement parseDefault(
            JsonObject fieldJson,
            InfoFieldType type,
            ObjectArrayList<String> values,
            String key,
            String schemaName) {

        JsonElement defaultValue = fieldJson.has("default")
                ? fieldJson.get("default").deepCopy()
                : createEmptyValue(type, values);

        if (!type.accepts(defaultValue))
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a default that is not a "
                    + type.getSchemaName() + ".");

        if (type == InfoFieldType.ENUM && !values.contains(defaultValue.getAsString()))
            throwException("Info schema '" + schemaName + "' enum field '" + key + "' defaults to '"
                    + defaultValue.getAsString() + "', which is not one of its values.");

        return defaultValue;
    }

    private JsonElement createEmptyValue(InfoFieldType type, ObjectArrayList<String> values) {

        return switch (type) {
            case STRING -> new JsonPrimitive("");
            case INT -> new JsonPrimitive(0);
            case FLOAT -> new JsonPrimitive(0.0);
            case BOOLEAN -> new JsonPrimitive(false);
            case ENUM -> new JsonPrimitive(values.get(0));
            default -> JsonNull.INSTANCE;
        };
    }

    // Utility \\

    private boolean isRequiredString(InfoFieldStruct field) {
        return field != null && field.isRequired() && field.getType() == InfoFieldType.STRING;
    }
}
