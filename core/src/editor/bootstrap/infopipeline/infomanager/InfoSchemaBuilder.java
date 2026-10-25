package editor.bootstrap.infopipeline.infomanager;

import java.io.File;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoLocalStepStruct;
import editor.bootstrap.infopipeline.infoschema.InfoReferenceStruct;
import editor.bootstrap.infopipeline.infoschema.InfoRuleStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaData;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.bootstrap.infopipeline.util.InfoLocalStep;
import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import editor.bootstrap.infopipeline.util.InfoResourceNaming;
import editor.runtime.EditorSetting;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgNullStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
     * which is unique already; only a primitive inside an array element may
     * be marked distinct. A string may declare one reference, a map a schema
     * reference for its keys, a number its bounds, a list its minimum count,
     * and any field the sibling that excuses it; every sibling a rule names is
     * checked to exist with the right type, and a local reference's path
     * against the fields it walks, so every rule the editor enforces is
     * itself well formed. Required names list the entries the game itself
     * cannot start without.
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
        boolean referencedByName = parseReferenceForm(arpg, entriesKey != null, schemaName);
        ObjectArrayList<String> requiredNames = parseStrings(
                arpg, "required_names", "Info schema '" + schemaName + "'");

        InfoFieldStruct rootField = new InfoFieldStruct(
                null,
                InfoFieldType.OBJECT,
                true,
                false,
                false,
                null,
                new ObjectArrayList<>(),
                null,
                null,
                createOpenRule(),
                parseFields(ArpgUtility.validateArray(arpg, "fields"), true, false, schemaName),
                null,
                0);

        if (nameField != null && !isRequiredString(rootField.findField(nameField)))
            throwException("Info schema '" + schemaName + "' must declare its name field '" + nameField
                    + "' as a required string.");

        if (nameField != null && rootField.findField(nameField).isUnique())
            throwException("Info schema '" + schemaName + "' marks its name field '" + nameField
                    + "' unique; entry names are unique already.");

        verifyLocalReferences(rootField, rootField, schemaName);

        ObjectArrayList<String> referencedSchemaNames = new ObjectArrayList<>();
        ObjectArrayList<String> referencedDirectories = new ObjectArrayList<>();
        collectDependencies(rootField, referencedSchemaNames, referencedDirectories);

        InfoSchemaData schemaData = new InfoSchemaData(
                schemaName,
                tabName,
                order,
                directory,
                entriesKey,
                nameField,
                defaultFile,
                referencedByName,
                requiredNames,
                rootField,
                collectUniqueKeys(rootField),
                referencedSchemaNames,
                referencedDirectories);

        InfoSchemaHandle schemaHandle = create(InfoSchemaHandle.class);
        schemaHandle.constructor(schemaData);

        return schemaHandle;
    }

    private boolean parseReferenceForm(ArpgObjectStruct arpg, boolean arrayLayout, String schemaName) {

        String referenceForm = ArpgUtility.getString(
                arpg, "reference_form", EditorSetting.INFO_REFERENCE_FORM_PATH);

        if (!referenceForm.equals(EditorSetting.INFO_REFERENCE_FORM_PATH)
                && !referenceForm.equals(EditorSetting.INFO_REFERENCE_FORM_NAME))
            throwException("Info schema '" + schemaName + "' has unknown reference form '" + referenceForm
                    + "'; use '" + EditorSetting.INFO_REFERENCE_FORM_PATH + "' or '"
                    + EditorSetting.INFO_REFERENCE_FORM_NAME + "'.");

        boolean referencedByName = referenceForm.equals(EditorSetting.INFO_REFERENCE_FORM_NAME);

        if (referencedByName && !arrayLayout)
            throwException("Info schema '" + schemaName + "' keeps one entry per file, so its entries are "
                    + "referenced by their file and cannot use the reference form '"
                    + EditorSetting.INFO_REFERENCE_FORM_NAME + "'.");

        return referencedByName;
    }

    // Parse \\

    private ObjectArrayList<InfoFieldStruct> parseFields(
            ArpgArrayStruct fieldsArpg,
            boolean topLevel,
            boolean inArray,
            String schemaName) {

        ObjectArrayList<InfoFieldStruct> fields = new ObjectArrayList<>(fieldsArpg.size());

        for (int i = 0; i < fieldsArpg.size(); i++) {

            InfoFieldStruct field = parseField(fieldsArpg.get(i).getAsObject(), true, topLevel, inArray, schemaName);

            for (int j = 0; j < fields.size(); j++)
                if (fields.get(j).getKey().equals(field.getKey()))
                    throwException("Info schema '" + schemaName + "' declares field '" + field.getKey()
                            + "' twice in one object.");

            fields.add(field);
        }

        verifySiblings(fields, schemaName);
        return fields;
    }

    private InfoFieldStruct parseField(
            ArpgObjectStruct fieldArpg,
            boolean keyed,
            boolean topLevel,
            boolean inArray,
            String schemaName) {

        String key = keyed ? ArpgUtility.validateString(fieldArpg, "key") : null;
        String typeName = ArpgUtility.validateString(fieldArpg, "type");
        InfoFieldType type = InfoFieldType.fromSchemaName(typeName);

        if (type == null)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has unknown type '"
                    + typeName + "'.");

        boolean required = ArpgUtility.getBoolean(fieldArpg, "required", false);
        boolean unique = ArpgUtility.getBoolean(fieldArpg, "unique", false);
        boolean distinct = ArpgUtility.getBoolean(fieldArpg, "distinct", false);
        ObjectArrayList<String> values = type == InfoFieldType.ENUM
                ? parseValues(fieldArpg, key, schemaName)
                : new ObjectArrayList<>();
        InfoReferenceStruct reference = parseReference(fieldArpg, type, key, schemaName);
        InfoReferenceStruct keyReference = parseKeyReference(fieldArpg, type, key, schemaName);
        InfoRuleStruct rule = parseRule(fieldArpg, type, key, schemaName);
        ObjectArrayList<InfoFieldStruct> fields = type == InfoFieldType.OBJECT
                ? parseFields(ArpgUtility.validateArray(fieldArpg, "fields"), false, inArray, schemaName)
                : new ObjectArrayList<>();
        InfoFieldStruct element = type == InfoFieldType.ARRAY || type == InfoFieldType.MAP
                ? parseField(
                        ArpgUtility.validateObject(fieldArpg, "element"),
                        false,
                        false,
                        type == InfoFieldType.ARRAY || inArray,
                        schemaName)
                : null;
        int length = type == InfoFieldType.ARRAY ? ArpgUtility.getInt(fieldArpg, "length", 0) : 0;
        ArpgElementStruct defaultValue = type.isGroup() ? null : parseDefault(fieldArpg, type, values, key, schemaName);

        if (length < 0)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a negative length.");

        if (unique && (!topLevel || type.isGroup() || type == InfoFieldType.RAW))
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' is marked unique, but only a top-level primitive field can be.");

        if (distinct && (!inArray || type.isGroup() || type == InfoFieldType.RAW))
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' is marked distinct, but only a primitive field inside an array element can be.");

        if (defaultValue != null && defaultValue.isValue() && defaultValue.getAsValue().isNumber()
                && !rule.isWithinBounds(defaultValue.getAsDouble()))
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' has a default outside its own bounds.");

        return new InfoFieldStruct(
                key, type, required, unique, distinct, defaultValue, values, reference, keyReference, rule,
                fields, element, length);
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

    // Rules \\

    private InfoRuleStruct parseRule(ArpgObjectStruct fieldArpg, InfoFieldType type, String key, String schemaName) {

        if ((fieldArpg.has("min") && fieldArpg.has("above")) || (fieldArpg.has("max") && fieldArpg.has("below")))
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' declares an inclusive and an exclusive bound on the same side.");

        boolean minExclusive = fieldArpg.has("above");
        boolean maxExclusive = fieldArpg.has("below");
        double min = parseBound(
                fieldArpg, minExclusive ? "above" : "min", Double.NEGATIVE_INFINITY, type, key, schemaName);
        double max = parseBound(
                fieldArpg, maxExclusive ? "below" : "max", Double.POSITIVE_INFINITY, type, key, schemaName);
        String atLeastField = ArpgUtility.getString(fieldArpg, "at_least_field", null);
        int minCount = ArpgUtility.getInt(fieldArpg, "min_count", 0);
        int maxCount = ArpgUtility.getInt(fieldArpg, "max_count", Integer.MAX_VALUE);
        String requiredUnless = ArpgUtility.getString(fieldArpg, "required_unless", null);
        String conflictsWith = ArpgUtility.getString(fieldArpg, "conflicts_with", null);
        ArpgObjectStruct requiredWhenArpg = fieldArpg.has("required_when")
                ? ArpgUtility.validateObject(fieldArpg, "required_when")
                : null;
        String requiredWhenKey = requiredWhenArpg != null ? ArpgUtility.validateString(requiredWhenArpg, "key") : null;
        ObjectArrayList<String> requiredWhenValues = requiredWhenArpg != null
                ? parseStrings(requiredWhenArpg, "values", "Info schema '" + schemaName + "' field '" + key + "'")
                : new ObjectArrayList<>();

        if (min > max || (min == max && (minExclusive || maxExclusive)))
            throwException("Info schema '" + schemaName + "' field '" + key + "' has bounds no value can meet.");

        if (atLeastField != null && type != InfoFieldType.INT && type != InfoFieldType.FLOAT)
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' must be at least another field, but only int and float fields compare.");

        boolean counted = minCount > 0 || maxCount != Integer.MAX_VALUE;

        if (minCount < 0 || maxCount < minCount)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has counts no list can meet.");

        if (counted && type != InfoFieldType.ARRAY && type != InfoFieldType.MAP)
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' has a count, but only arrays and maps hold a count.");

        if (requiredWhenArpg != null && requiredWhenValues.isEmpty())
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' is required when a sibling holds a value, but lists no values.");

        return new InfoRuleStruct(
                min, max, minExclusive, maxExclusive, atLeastField, minCount, maxCount,
                requiredUnless, requiredWhenKey, requiredWhenValues, conflictsWith);
    }

    private InfoRuleStruct createOpenRule() {
        return new InfoRuleStruct(
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, false, null, 0, Integer.MAX_VALUE,
                null, null, new ObjectArrayList<>(), null);
    }

    // Every sibling a field's rule or keyed reference names present in its object, with the type the rule reads when
    // it reads one
    private void verifySiblings(ObjectArrayList<InfoFieldStruct> fields, String schemaName) {

        for (int i = 0; i < fields.size(); i++) {

            InfoFieldStruct field = fields.get(i);
            InfoRuleStruct rule = field.getRule();

            if (rule.hasAtLeastField())
                verifySibling(fields, field, rule.getAtLeastField(), schemaName,
                        InfoFieldType.INT, InfoFieldType.FLOAT);

            if (rule.hasRequiredUnless())
                verifySibling(fields, field, rule.getRequiredUnless(), schemaName, null, null);

            if (rule.hasRequiredWhen())
                verifySibling(fields, field, rule.getRequiredWhenKey(), schemaName,
                        InfoFieldType.ENUM, InfoFieldType.STRING);

            if (rule.hasConflict())
                verifySibling(fields, field, rule.getConflictsWith(), schemaName, null, null);

            if (field.hasReference() && field.getReference().isKeyed())
                verifySibling(fields, field, field.getReference().getKeyField(), schemaName,
                        InfoFieldType.ENUM, InfoFieldType.STRING);
        }
    }

    private void verifySibling(
            ObjectArrayList<InfoFieldStruct> fields,
            InfoFieldStruct field,
            String siblingKey,
            String schemaName,
            InfoFieldType firstType,
            InfoFieldType secondType) {

        for (int i = 0; i < fields.size(); i++) {

            InfoFieldType type = fields.get(i).getType();

            if (fields.get(i).getKey().equals(siblingKey)
                    && (firstType == null || type == firstType || type == secondType))
                return;
        }

        throwException("Info schema '" + schemaName + "' field '" + field.getKey() + "' names sibling '"
                + siblingKey + "', which its object does not declare"
                + (firstType != null ? " as " + firstType.getSchemaName() : "") + ".");
    }

    private ObjectArrayList<String> parseStrings(ArpgObjectStruct arpg, String key, String owner) {

        ObjectArrayList<String> strings = new ObjectArrayList<>();

        if (!arpg.has(key))
            return strings;

        ArpgArrayStruct stringsArpg = ArpgUtility.validateArray(arpg, key);

        for (int i = 0; i < stringsArpg.size(); i++) {

            if (!stringsArpg.get(i).isValue() || !stringsArpg.get(i).getAsValue().isString())
                throwException(owner + " lists a value under '" + key + "' that is not a string.");

            strings.add(stringsArpg.get(i).getAsString());
        }

        return strings;
    }

    private double parseBound(
            ArpgObjectStruct fieldArpg,
            String boundKey,
            double unbounded,
            InfoFieldType type,
            String key,
            String schemaName) {

        if (!fieldArpg.has(boundKey))
            return unbounded;

        if (type != InfoFieldType.INT && type != InfoFieldType.FLOAT)
            throwException("Info schema '" + schemaName + "' field '" + key + "' declares a bound '" + boundKey
                    + "', but only int and float fields have bounds.");

        if (!ArpgUtility.hasNumber(fieldArpg, boundKey))
            throwException("Info schema '" + schemaName + "' field '" + key + "' has a " + boundKey
                    + " that is not a number.");

        return fieldArpg.get(boundKey).getAsDouble();
    }

    // References \\

    private InfoReferenceStruct parseReference(
            ArpgObjectStruct fieldArpg,
            InfoFieldType type,
            String key,
            String schemaName) {

        int declared = (fieldArpg.has("reference") ? 1 : 0)
                + (fieldArpg.has("resource") ? 1 : 0)
                + (fieldArpg.has("local") ? 1 : 0);

        if (declared == 0) {

            if (fieldArpg.has("allow"))
                throwException("Info schema '" + schemaName + "' field '" + key
                        + "' lists allowed values but declares no reference.");

            return null;
        }

        if (declared > 1)
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' declares more than one of reference, resource and local.");

        if (type != InfoFieldType.STRING)
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' declares a reference, but only a string field can name something.");

        ObjectArrayList<String> allowedValues = parseStrings(
                fieldArpg, "allow", "Info schema '" + schemaName + "' field '" + key + "'");

        if (fieldArpg.has("reference"))
            return parseSchemaReference(fieldArpg.get("reference"), allowedValues, key, schemaName);

        if (fieldArpg.has("resource"))
            return parseResourceReference(fieldArpg, allowedValues, key, schemaName);

        return new InfoReferenceStruct(
                InfoReferenceKind.LOCAL, null, null, null, null, null,
                parseLocalSteps(ArpgUtility.validateString(fieldArpg, "local"), key, schemaName),
                allowedValues);
    }

    private InfoReferenceStruct parseKeyReference(
            ArpgObjectStruct fieldArpg,
            InfoFieldType type,
            String key,
            String schemaName) {

        if (!fieldArpg.has("key_reference"))
            return null;

        if (type != InfoFieldType.MAP)
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' declares a key reference, but only a map has keys that name something.");

        return new InfoReferenceStruct(
                InfoReferenceKind.SCHEMA, ArpgUtility.validateString(fieldArpg, "key_reference"),
                null, null, null, null, null, new ObjectArrayList<>());
    }

    private InfoReferenceStruct parseSchemaReference(
            ArpgElementStruct referenceArpg,
            ObjectArrayList<String> allowedValues,
            String key,
            String schemaName) {

        if (referenceArpg.isValue() && referenceArpg.getAsValue().isString())
            return new InfoReferenceStruct(
                    InfoReferenceKind.SCHEMA, referenceArpg.getAsString(), null, null, null, null, null,
                    allowedValues);

        if (!referenceArpg.isObject())
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' has a reference that is neither a schema name nor an object.");

        ArpgObjectStruct keyedArpg = referenceArpg.getAsObject();
        String keyField = ArpgUtility.validateString(keyedArpg, "by");
        ArpgObjectStruct schemasArpg = ArpgUtility.validateObject(keyedArpg, "schemas");
        Object2ObjectOpenHashMap<String, String> keyValue2SchemaName = new Object2ObjectOpenHashMap<>();

        for (String keyValue : schemasArpg.keySet())
            keyValue2SchemaName.put(keyValue, schemasArpg.get(keyValue).getAsString());

        if (keyValue2SchemaName.isEmpty())
            throwException("Info schema '" + schemaName + "' field '" + key
                    + "' has a keyed reference that lists no schemas.");

        return new InfoReferenceStruct(
                InfoReferenceKind.SCHEMA, null, keyField, keyValue2SchemaName, null, null, null, allowedValues);
    }

    private InfoReferenceStruct parseResourceReference(
            ArpgObjectStruct fieldArpg,
            ObjectArrayList<String> allowedValues,
            String key,
            String schemaName) {

        ArpgObjectStruct resourceArpg = ArpgUtility.validateObject(fieldArpg, "resource");
        String directory = ArpgUtility.validateString(resourceArpg, "directory");
        String namingName = ArpgUtility.validateString(resourceArpg, "naming");
        InfoResourceNaming naming = InfoResourceNaming.fromSchemaName(namingName);

        if (naming == null)
            throwException("Info schema '" + schemaName + "' field '" + key + "' has unknown resource naming '"
                    + namingName + "'.");

        return new InfoReferenceStruct(
                InfoReferenceKind.RESOURCE, null, null, null, directory, naming, null, allowedValues);
    }

    // Local \\

    private ObjectArrayList<InfoLocalStepStruct> parseLocalSteps(String path, String key, String schemaName) {

        ObjectArrayList<String> segments = splitLocalPath(path);
        ObjectArrayList<InfoLocalStepStruct> steps = new ObjectArrayList<>(segments.size());

        for (int i = 0; i < segments.size(); i++) {

            InfoLocalStepStruct step = parseLocalStep(segments.get(i));
            boolean last = i == segments.size() - 1;

            if (step.getKey().isEmpty())
                throwException("Info schema '" + schemaName + "' field '" + key + "' has a local path '" + path
                        + "' with an empty step.");

            if (step.getStep() == InfoLocalStep.MAP_KEYS && !last)
                throwException("Info schema '" + schemaName + "' field '" + key + "' has a local path '" + path
                        + "' that reads map keys before its end.");

            if (last && step.getStep() != InfoLocalStep.FIELD && step.getStep() != InfoLocalStep.MAP_KEYS)
                throwException("Info schema '" + schemaName + "' field '" + key + "' has a local path '" + path
                        + "' that ends on an array instead of a value or map keys.");

            steps.add(step);
        }

        return steps;
    }

    // A local path cut at every separator outside a step's brackets, so a same-element step keeps its own
    private ObjectArrayList<String> splitLocalPath(String path) {

        ObjectArrayList<String> segments = new ObjectArrayList<>();
        char separator = EditorSetting.INFO_LOCAL_SEPARATOR.charAt(0);
        char open = EditorSetting.INFO_LOCAL_ANY_ELEMENT.charAt(0);
        char close = EditorSetting.INFO_LOCAL_ANY_ELEMENT.charAt(1);
        int depth = 0;
        int start = 0;

        for (int i = 0; i < path.length(); i++) {

            char character = path.charAt(i);

            if (character == open)
                depth++;
            else if (character == close)
                depth--;
            else if (character == separator && depth == 0) {
                segments.add(path.substring(start, i));
                start = i + 1;
            }
        }

        segments.add(path.substring(start));
        return segments;
    }

    private InfoLocalStepStruct parseLocalStep(String segment) {

        if (segment.endsWith(EditorSetting.INFO_LOCAL_SAME_ELEMENT))
            return new InfoLocalStepStruct(
                    segment.substring(0, segment.length() - EditorSetting.INFO_LOCAL_SAME_ELEMENT.length()),
                    InfoLocalStep.SAME_ELEMENT);

        if (segment.endsWith(EditorSetting.INFO_LOCAL_ANY_ELEMENT))
            return new InfoLocalStepStruct(
                    segment.substring(0, segment.length() - EditorSetting.INFO_LOCAL_ANY_ELEMENT.length()),
                    InfoLocalStep.ANY_ELEMENT);

        if (segment.endsWith(EditorSetting.INFO_LOCAL_MAP_KEYS))
            return new InfoLocalStepStruct(
                    segment.substring(0, segment.length() - EditorSetting.INFO_LOCAL_MAP_KEYS.length()),
                    InfoLocalStep.MAP_KEYS);

        return new InfoLocalStepStruct(segment, InfoLocalStep.FIELD);
    }

    // Every local reference's path walked through the schema's own fields, so a path naming nothing fails at boot
    private void verifyLocalReferences(InfoFieldStruct field, InfoFieldStruct rootField, String schemaName) {

        if (field.hasReference() && field.getReference().getKind() == InfoReferenceKind.LOCAL)
            verifyLocalPath(field, rootField, schemaName);

        for (int i = 0; i < field.getFields().size(); i++)
            verifyLocalReferences(field.getFields().get(i), rootField, schemaName);

        if (field.getElement() != null)
            verifyLocalReferences(field.getElement(), rootField, schemaName);
    }

    private void verifyLocalPath(InfoFieldStruct field, InfoFieldStruct rootField, String schemaName) {

        ObjectArrayList<InfoLocalStepStruct> steps = field.getReference().getLocalSteps();
        InfoFieldStruct current = rootField;

        for (int i = 0; i < steps.size(); i++) {

            InfoLocalStepStruct step = steps.get(i);
            InfoFieldStruct stepField = current != null && current.getType() == InfoFieldType.OBJECT
                    ? current.findField(step.getKey())
                    : null;
            InfoFieldType expected = switch (step.getStep()) {
                case FIELD -> i == steps.size() - 1 ? InfoFieldType.STRING : InfoFieldType.OBJECT;
                case ANY_ELEMENT, SAME_ELEMENT -> InfoFieldType.ARRAY;
                case MAP_KEYS -> InfoFieldType.MAP;
            };

            if (stepField == null || stepField.getType() != expected)
                throwException("Info schema '" + schemaName + "' field '" + field.getKey()
                        + "' has a local path whose step '" + step.getKey() + "' is not "
                        + expected.getSchemaName() + " field.");

            current = step.getStep() == InfoLocalStep.ANY_ELEMENT || step.getStep() == InfoLocalStep.SAME_ELEMENT
                    ? stepField.getElement()
                    : stepField;
        }
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

    private void collectDependencies(
            InfoFieldStruct field,
            ObjectArrayList<String> referencedSchemaNames,
            ObjectArrayList<String> referencedDirectories) {

        if (field.hasReference())
            addDependencies(field.getReference(), referencedSchemaNames, referencedDirectories);

        if (field.hasKeyReference())
            addDependencies(field.getKeyReference(), referencedSchemaNames, referencedDirectories);

        for (int i = 0; i < field.getFields().size(); i++)
            collectDependencies(field.getFields().get(i), referencedSchemaNames, referencedDirectories);

        if (field.getElement() != null)
            collectDependencies(field.getElement(), referencedSchemaNames, referencedDirectories);
    }

    private void addDependencies(
            InfoReferenceStruct reference,
            ObjectArrayList<String> referencedSchemaNames,
            ObjectArrayList<String> referencedDirectories) {

        switch (reference.getKind()) {
            case SCHEMA -> {

                if (!reference.isKeyed())
                    addOnce(referencedSchemaNames, reference.getSchemaName());
                else
                    for (String referencedSchemaName : reference.getKeyValue2SchemaName().values())
                        addOnce(referencedSchemaNames, referencedSchemaName);
            }
            case RESOURCE -> addOnce(referencedDirectories, reference.getDirectory());
            case LOCAL -> {
            }
        }
    }

    private void addOnce(ObjectArrayList<String> names, String name) {

        if (!names.contains(name))
            names.add(name);
    }

    private boolean isRequiredString(InfoFieldStruct field) {
        return field != null && field.isRequired() && field.getType() == InfoFieldType.STRING;
    }
}
