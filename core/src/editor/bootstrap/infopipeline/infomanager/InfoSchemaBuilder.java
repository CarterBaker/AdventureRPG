package editor.bootstrap.infopipeline.infomanager;

import java.io.File;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaData;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgNullStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoSchemaBuilder extends BuilderPackage {

    /*
     * Parses one schema ARPG file into an InfoSchemaHandle. The entry itself
     * is an object field holding the schema's "fields"; enums need values,
     * objects need fields, and arrays and maps need an element. A primitive
     * without a "default" starts from the natural empty value of its type,
     * and every default is checked against its type so a malformed schema
     * fails at boot. An array layout must declare its name field. Only a
     * top-level primitive may be marked unique, and never the name field,
     * which is unique already.
     */

    // Build \\

    InfoSchemaHandle build(File file, String schemaName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        String tabName = ArpgUtility.validateString(arpg, "tab");
        int order = ArpgUtility.getInt(arpg, "order", 0);
        String directory = ArpgUtility.validateString(arpg, "directory");
        String entriesKey = ArpgUtility.getString(arpg, "entries", null);
        String nameField = entriesKey != null ? ArpgUtility.validateString(arpg, "name_field") : null;
        String defaultFile = entriesKey != null ? ArpgUtility.validateString(arpg, "default_file") : null;

        InfoFieldStruct rootField = new InfoFieldStruct(
                null,
                InfoFieldType.OBJECT,
                true,
                false,
                null,
                new ObjectArrayList<>(),
                parseFields(ArpgUtility.validateArray(arpg, "fields"), true, schemaName),
                null,
                0);

        if (nameField != null && !isRequiredString(rootField.findField(nameField)))
            throwException("Info schema '" + schemaName + "' must declare its name field '" + nameField
                    + "' as a required string.");

        if (nameField != null && rootField.findField(nameField).isUnique())
            throwException("Info schema '" + schemaName + "' marks its name field '" + nameField
                    + "' unique; entry names are unique already.");

        InfoSchemaData schemaData = new InfoSchemaData(
                schemaName,
                tabName,
                order,
                directory,
                entriesKey,
                nameField,
                defaultFile,
                rootField,
                collectUniqueKeys(rootField));

        InfoSchemaHandle schemaHandle = create(InfoSchemaHandle.class);
        schemaHandle.constructor(schemaData);

        return schemaHandle;
    }

    // Parse \\

    private ObjectArrayList<InfoFieldStruct> parseFields(
            ArpgArrayStruct fieldsArpg,
            boolean topLevel,
            String schemaName) {

        ObjectArrayList<InfoFieldStruct> fields = new ObjectArrayList<>(fieldsArpg.size());

        for (int i = 0; i < fieldsArpg.size(); i++) {

            InfoFieldStruct field = parseField(fieldsArpg.get(i).getAsObject(), true, topLevel, schemaName);

            for (int j = 0; j < fields.size(); j++)
                if (fields.get(j).getKey().equals(field.getKey()))
                    throwException("Info schema '" + schemaName + "' declares field '" + field.getKey()
                            + "' twice in one object.");

            fields.add(field);
        }

        return fields;
    }

    private InfoFieldStruct parseField(
            ArpgObjectStruct fieldArpg,
            boolean keyed,
            boolean topLevel,
            String schemaName) {

        String key = keyed ? ArpgUtility.validateString(fieldArpg, "key") : null;
        String typeName = ArpgUtility.validateString(fieldArpg, "type");
        InfoFieldType type = InfoFieldType.fromSchemaName(typeName);

        if (type == null)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has unknown type '"
                    + typeName + "'.");

        boolean required = ArpgUtility.getBoolean(fieldArpg, "required", false);
        boolean unique = ArpgUtility.getBoolean(fieldArpg, "unique", false);
        ObjectArrayList<String> values = type == InfoFieldType.ENUM
                ? parseValues(fieldArpg, key, schemaName)
                : new ObjectArrayList<>();
        ObjectArrayList<InfoFieldStruct> fields = type == InfoFieldType.OBJECT
                ? parseFields(ArpgUtility.validateArray(fieldArpg, "fields"), false, schemaName)
                : new ObjectArrayList<>();
        InfoFieldStruct element = type == InfoFieldType.ARRAY || type == InfoFieldType.MAP
                ? parseField(ArpgUtility.validateObject(fieldArpg, "element"), false, false, schemaName)
                : null;
        int length = type == InfoFieldType.ARRAY ? ArpgUtility.getInt(fieldArpg, "length", 0) : 0;
        ArpgElementStruct defaultValue = type.isGroup() ? null : parseDefault(fieldArpg, type, values, key, schemaName);

        if (length < 0)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a negative length.");

        if (unique && (!topLevel || type.isGroup() || type == InfoFieldType.RAW))
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' is marked unique, but only a top-level primitive field can be.");

        return new InfoFieldStruct(key, type, required, unique, defaultValue, values, fields, element, length);
    }

    private ObjectArrayList<String> parseValues(ArpgObjectStruct fieldArpg, String key, String schemaName) {

        ArpgArrayStruct valuesArpg = ArpgUtility.validateArray(fieldArpg, "values");
        ObjectArrayList<String> values = new ObjectArrayList<>(valuesArpg.size());

        for (int i = 0; i < valuesArpg.size(); i++)
            values.add(valuesArpg.get(i).getAsString());

        if (values.isEmpty())
            throwException("Info schema '" + schemaName + "' enum field '" + key + "' lists no values.");

        return values;
    }

    private ArpgElementStruct parseDefault(
            ArpgObjectStruct fieldArpg,
            InfoFieldType type,
            ObjectArrayList<String> values,
            String key,
            String schemaName) {

        ArpgElementStruct defaultValue = fieldArpg.has("default")
                ? fieldArpg.get("default").deepCopy()
                : createEmptyValue(type, values);

        if (!type.accepts(defaultValue))
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a default that is not a "
                    + type.getSchemaName() + ".");

        if (type == InfoFieldType.ENUM && !values.contains(defaultValue.getAsString()))
            throwException("Info schema '" + schemaName + "' enum field '" + key + "' defaults to '"
                    + defaultValue.getAsString() + "', which is not one of its values.");

        return defaultValue;
    }

    private ArpgElementStruct createEmptyValue(InfoFieldType type, ObjectArrayList<String> values) {

        return switch (type) {
            case STRING -> new ArpgValueStruct("");
            case INT -> new ArpgValueStruct(0);
            case FLOAT -> new ArpgValueStruct(0.0);
            case BOOLEAN -> new ArpgValueStruct(false);
            case ENUM -> new ArpgValueStruct(values.get(0));
            default -> ArpgNullStruct.INSTANCE;
        };
    }

    // Utility \\

    private ObjectArrayList<String> collectUniqueKeys(InfoFieldStruct rootField) {

        ObjectArrayList<InfoFieldStruct> fields = rootField.getFields();
        ObjectArrayList<String> uniqueKeys = new ObjectArrayList<>();

        for (int i = 0; i < fields.size(); i++)
            if (fields.get(i).isUnique())
                uniqueKeys.add(fields.get(i).getKey());

        uniqueKeys.trim();
        return uniqueKeys;
    }

    private boolean isRequiredString(InfoFieldStruct field) {
        return field != null && field.isRequired() && field.getType() == InfoFieldType.STRING;
    }
}
