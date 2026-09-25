package editor.bootstrap.infopipeline.infomanager;

import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infotarget.InfoTargetStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class InfoEditBranch extends BranchPackage {

    /*
     * Performs every change to an entry's JSON. A row path is resolved against
     * the entry and its schema into an InfoTargetStruct; typed text is parsed
     * by the type the value actually holds, so anything that does not match
     * its schema is shown and edited as raw JSON. New values are built from the schema:
     * objects with their required fields, fixed-length arrays filled, and
     * primitives from their default. The same removal rule decides both what
     * the panel offers and what is allowed.
     */

    // Internal
    private Pattern pathPattern;

    // Base \\

    @Override
    protected void create() {
        this.pathPattern = Pattern.compile(Pattern.quote(EditorSetting.INFO_PATH_SEPARATOR));
    }

    // Resolve \\

    InfoTargetStruct resolve(JsonObject entryJson, InfoFieldStruct rootField, String path) {

        String[] segments = pathPattern.split(path);
        JsonElement container = entryJson;
        InfoFieldStruct containerField = rootField;

        for (int i = 0; i < segments.length; i++) {

            if (!container.isJsonArray() && !container.isJsonObject())
                return null;

            String segment = segments[i];
            String key = null;
            int index = EngineSetting.INDEX_NOT_FOUND;
            InfoFieldStruct field;
            JsonElement value;

            if (container.isJsonArray()) {

                JsonArray array = container.getAsJsonArray();
                index = parseIndex(segment);

                if (index < 0 || index >= array.size())
                    return null;

                field = containerField != null && containerField.getType() == InfoFieldType.ARRAY
                        ? containerField.getElement()
                        : null;
                value = array.get(index);
            } else {

                key = segment;
                field = findChildField(containerField, key);
                value = container.getAsJsonObject().get(key);
            }

            if (i == segments.length - 1)
                return new InfoTargetStruct(container, containerField, key, index, field, value);

            if (value == null)
                return null;

            containerField = field != null && field.getType().accepts(value) ? field : null;
            container = value;
        }

        return null;
    }

    private InfoFieldStruct findChildField(InfoFieldStruct containerField, String key) {

        if (containerField == null)
            return null;

        return switch (containerField.getType()) {
            case OBJECT -> containerField.findField(key);
            case MAP -> containerField.getElement();
            default -> null;
        };
    }

    private int parseIndex(String segment) {

        try {
            return Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return EngineSetting.INDEX_NOT_FOUND;
        }
    }

    // Parse \\

    JsonElement parse(InfoTargetStruct target, String text) {

        return switch (target.resolveType()) {
            case STRING -> new JsonPrimitive(text);
            case INT -> parseInt(text);
            case FLOAT -> parseFloat(text);
            case BOOLEAN -> parseBoolean(text);
            case ENUM -> parseEnum(target.getField(), text);
            case JSON -> parseJson(text);
            default -> null;
        };
    }

    private JsonElement parseInt(String text) {

        try {
            return new JsonPrimitive(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JsonElement parseFloat(String text) {

        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? new JsonPrimitive(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JsonElement parseBoolean(String text) {

        String trimmed = text.trim();

        if (trimmed.equalsIgnoreCase(Boolean.TRUE.toString()))
            return new JsonPrimitive(true);

        if (trimmed.equalsIgnoreCase(Boolean.FALSE.toString()))
            return new JsonPrimitive(false);

        return null;
    }

    private JsonElement parseEnum(InfoFieldStruct field, String text) {

        int valueIndex = findEnumIndex(field, text.trim());

        return valueIndex != EngineSetting.INDEX_NOT_FOUND
                ? new JsonPrimitive(field.getValues().get(valueIndex))
                : null;
    }

    private JsonElement parseJson(String text) {

        try {
            return JsonParser.parseString(text);
        } catch (JsonParseException e) {
            return null;
        }
    }

    // Values \\

    void toggle(InfoTargetStruct target) {
        target.write(new JsonPrimitive(!target.getValue().getAsBoolean()));
    }

    void cycle(InfoTargetStruct target) {

        InfoFieldStruct field = target.getField();
        int current = findEnumIndex(field, target.getValue().getAsString());
        int next = (current + 1) % field.getValues().size();

        target.write(new JsonPrimitive(field.getValues().get(next)));
    }

    private int findEnumIndex(InfoFieldStruct field, String value) {

        for (int i = 0; i < field.getValues().size(); i++)
            if (field.getValues().get(i).equalsIgnoreCase(value))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    String formatValue(JsonElement value, InfoFieldType type) {
        return type != InfoFieldType.JSON && value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    // Structure \\

    JsonElement createDefault(InfoFieldStruct field) {

        return switch (field.getType()) {
            case OBJECT -> createDefaultObject(field);
            case ARRAY -> createDefaultArray(field);
            case MAP -> new JsonObject();
            default -> field.getDefaultValue().deepCopy();
        };
    }

    private JsonObject createDefaultObject(InfoFieldStruct field) {

        JsonObject object = new JsonObject();

        for (int i = 0; i < field.getFields().size(); i++) {

            InfoFieldStruct child = field.getFields().get(i);

            if (child.isRequired())
                object.add(child.getKey(), createDefault(child));
        }

        return object;
    }

    private JsonArray createDefaultArray(InfoFieldStruct field) {

        JsonArray array = new JsonArray();

        for (int i = 0; i < field.getLength(); i++)
            array.add(createDefault(field.getElement()));

        return array;
    }

    void addField(InfoTargetStruct target) {
        target.write(createDefault(target.getField()));
    }

    void addElement(InfoTargetStruct target) {

        JsonArray array = target.getValue().getAsJsonArray();
        InfoFieldStruct elementField = target.resolveType() == InfoFieldType.ARRAY
                ? target.getField().getElement()
                : null;

        if (elementField != null)
            array.add(createDefault(elementField));
        else
            array.add(array.isEmpty() ? JsonNull.INSTANCE : array.get(array.size() - 1).deepCopy());
    }

    void addMapEntry(InfoTargetStruct target, String key) {

        InfoFieldStruct elementField = target.getField().getElement();
        target.getValue().getAsJsonObject().add(key, createDefault(elementField));
    }

    boolean isMapKeyAvailable(InfoTargetStruct target, String key) {
        return !key.isEmpty()
                && !key.contains(EditorSetting.INFO_PATH_SEPARATOR)
                && !target.getValue().getAsJsonObject().has(key);
    }

    // Rules \\

    boolean isAddable(InfoTargetStruct target) {

        InfoFieldType type = target.resolveType();

        return type == InfoFieldType.MAP
                || (type == InfoFieldType.ARRAY && !target.getField().isFixedLength());
    }

    boolean isRemovable(InfoTargetStruct target) {
        return isRemovable(target.getContainerField(), target.getField(), target.getContainer().isJsonArray());
    }

    boolean isRemovable(InfoFieldStruct containerField, InfoFieldStruct field, boolean inArray) {

        if (inArray)
            return containerField == null || !containerField.isFixedLength();

        if (containerField != null && containerField.getType() == InfoFieldType.MAP)
            return true;

        return field == null || !field.isRequired();
    }
}
