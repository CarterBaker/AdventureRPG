package editor.bootstrap.infopipeline.infomanager;

import java.util.regex.Pattern;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoRuleStruct;
import editor.bootstrap.infopipeline.infotarget.InfoTargetStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgNullStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

class InfoEditBranch extends BranchPackage {

    /*
     * Performs every change to an entry's ARPG tree. A row path is resolved
     * against the entry and its schema into an InfoTargetStruct; typed text is
     * parsed by the type the value actually holds, so anything that does not
     * match its schema is shown and edited as raw ARPG text. New values are
     * built from the schema: objects with their required fields, fixed-length
     * arrays filled, and primitives from their default. A number steps by
     * one whole or by a small decimal, kept inside the range its schema
     * declares, and a number declaring both ends of its range is also set at a
     * share of it. A group standing in an array or a map is duplicated beside
     * itself. The same removal and duplication rules decide both what the
     * panel offers and what is allowed.
     */

    // Internal
    private Pattern pathPattern;

    // Base \\

    @Override
    protected void create() {
        this.pathPattern = Pattern.compile(Pattern.quote(EditorSetting.INFO_PATH_SEPARATOR));
    }

    // Resolve \\

    InfoTargetStruct resolve(ArpgObjectStruct entryArpg, InfoFieldStruct rootField, String path) {

        String[] segments = pathPattern.split(path);
        ArpgElementStruct container = entryArpg;
        InfoFieldStruct containerField = rootField;

        for (int i = 0; i < segments.length; i++) {

            if (!container.isArray() && !container.isObject())
                return null;

            String segment = segments[i];
            String key = null;
            int index = EngineSetting.INDEX_NOT_FOUND;
            InfoFieldStruct field;
            ArpgElementStruct value;

            if (container.isArray()) {

                ArpgArrayStruct array = container.getAsArray();
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
                value = container.getAsObject().get(key);
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

    ArpgElementStruct parse(InfoTargetStruct target, String text) {

        return switch (target.resolveType()) {
            case STRING -> new ArpgValueStruct(text);
            case INT -> parseInt(text);
            case FLOAT -> parseFloat(text);
            case BOOLEAN -> parseBoolean(text);
            case ENUM -> parseEnum(target.getField(), text);
            case RAW -> ArpgUtility.tryParseText(text);
            default -> null;
        };
    }

    private ArpgElementStruct parseInt(String text) {

        try {
            return new ArpgValueStruct(Long.parseLong(text.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ArpgElementStruct parseFloat(String text) {

        try {
            double value = Double.parseDouble(text.trim());
            return Double.isFinite(value) ? new ArpgValueStruct(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ArpgElementStruct parseBoolean(String text) {

        String trimmed = text.trim();

        if (trimmed.equalsIgnoreCase(Boolean.TRUE.toString()))
            return new ArpgValueStruct(true);

        if (trimmed.equalsIgnoreCase(Boolean.FALSE.toString()))
            return new ArpgValueStruct(false);

        return null;
    }

    private ArpgElementStruct parseEnum(InfoFieldStruct field, String text) {

        int valueIndex = findEnumIndex(field, text.trim());

        return valueIndex != EngineSetting.INDEX_NOT_FOUND
                ? new ArpgValueStruct(field.getValues().get(valueIndex))
                : null;
    }

    // Values \\

    private int findEnumIndex(InfoFieldStruct field, String value) {

        for (int i = 0; i < field.getValues().size(); i++)
            if (field.getValues().get(i).equalsIgnoreCase(value))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Numbers \\

    // A number moved a count of its steps; false when the move would leave its range
    boolean step(InfoTargetStruct target, int steps) {

        double stepSize = target.resolveType() == InfoFieldType.INT ? 1.0 : EditorSetting.INFO_FLOAT_STEP;
        return writeNumber(target, target.getValue().getAsDouble() + steps * stepSize);
    }

    // A number declaring both ends of its range set at a share of it
    boolean setFraction(InfoTargetStruct target, float fraction) {

        InfoRuleStruct rule = target.getField().getRule();
        return writeNumber(target, rule.getMin() + Math.clamp(fraction, 0f, 1f) * (rule.getMax() - rule.getMin()));
    }

    // Where a number lies across its declared range, NaN for a number that declares no two ends
    float toFraction(InfoFieldStruct field, ArpgElementStruct value) {

        if (!hasRange(field) || !value.isValue() || !value.getAsValue().isNumber())
            return Float.NaN;

        InfoRuleStruct rule = field.getRule();
        return (float) Math.clamp((value.getAsDouble() - rule.getMin()) / (rule.getMax() - rule.getMin()), 0.0, 1.0);
    }

    boolean hasRange(InfoFieldStruct field) {

        if (field == null || field.getRule() == null)
            return false;

        InfoRuleStruct rule = field.getRule();
        return rule.hasMin() && rule.hasMax() && rule.getMax() > rule.getMin();
    }

    private boolean writeNumber(InfoTargetStruct target, double value) {

        InfoRuleStruct rule = target.getField().getRule();
        double held = rule != null ? holdInRange(rule, value) : value;
        double scale = EditorSetting.INFO_FLOAT_SCALE;
        ArpgValueStruct number = target.resolveType() == InfoFieldType.INT
                ? new ArpgValueStruct(Math.round(held))
                : new ArpgValueStruct(Math.round(held * scale) / scale);

        if (rule != null && !rule.isWithinBounds(number.getAsDouble()))
            return false;

        target.write(number);
        return true;
    }

    private double holdInRange(InfoRuleStruct rule, double value) {

        double held = rule.hasMin() ? Math.max(value, rule.getMin()) : value;
        return rule.hasMax() ? Math.min(held, rule.getMax()) : held;
    }

    String formatValue(ArpgElementStruct value, InfoFieldType type) {
        return type != InfoFieldType.RAW && value.isValue() ? value.getAsString() : value.toString();
    }

    // Structure \\

    ArpgElementStruct createDefault(InfoFieldStruct field) {

        return switch (field.getType()) {
            case OBJECT -> createDefaultObject(field);
            case ARRAY -> createDefaultArray(field);
            case MAP -> new ArpgObjectStruct();
            default -> field.getDefaultValue().deepCopy();
        };
    }

    private ArpgObjectStruct createDefaultObject(InfoFieldStruct field) {

        ArpgObjectStruct object = new ArpgObjectStruct();

        for (int i = 0; i < field.getFields().size(); i++) {

            InfoFieldStruct child = field.getFields().get(i);

            if (child.isRequired())
                object.add(child.getKey(), createDefault(child));
        }

        return object;
    }

    private ArpgArrayStruct createDefaultArray(InfoFieldStruct field) {

        ArpgArrayStruct array = new ArpgArrayStruct();

        for (int i = 0; i < field.getLength(); i++)
            array.add(createDefault(field.getElement()));

        return array;
    }

    void addField(InfoTargetStruct target) {
        target.write(createDefault(target.getField()));
    }

    void addElement(InfoTargetStruct target) {

        ArpgArrayStruct array = target.getValue().getAsArray();
        InfoFieldStruct elementField = target.resolveType() == InfoFieldType.ARRAY
                ? target.getField().getElement()
                : null;

        if (elementField != null)
            array.add(createDefault(elementField));
        else
            array.add(array.isEmpty() ? ArpgNullStruct.INSTANCE : array.get(array.size() - 1).deepCopy());
    }

    void addMapEntry(InfoTargetStruct target, String key) {

        InfoFieldStruct elementField = target.getField().getElement();
        target.getValue().getAsObject().add(key, createDefault(elementField));
    }

    boolean isMapKeyAvailable(InfoTargetStruct target, String key) {
        return !key.isEmpty()
                && !key.contains(EditorSetting.INFO_PATH_SEPARATOR)
                && !target.getValue().getAsObject().has(key);
    }

    // A copy of an array's element placed just after it
    void duplicateElement(InfoTargetStruct target) {
        target.getContainer().getAsArray().add(target.getIndex() + 1, target.getValue().deepCopy());
    }

    // A copy of a map's entry under a new key
    void duplicateMapEntry(InfoTargetStruct target, String key) {
        target.getContainer().getAsObject().add(key, target.getValue().deepCopy());
    }

    boolean isDuplicateKeyAvailable(InfoTargetStruct target, String key) {
        return !key.isEmpty()
                && !key.contains(EditorSetting.INFO_PATH_SEPARATOR)
                && !target.getContainer().getAsObject().has(key);
    }

    // Rules \\

    boolean isAddable(InfoTargetStruct target) {

        InfoFieldType type = target.resolveType();

        return type == InfoFieldType.MAP
                || (type == InfoFieldType.ARRAY && !target.getField().isFixedLength());
    }

    boolean isRemovable(InfoTargetStruct target) {
        return isRemovable(target.getContainerField(), target.getField(), target.getContainer().isArray());
    }

    boolean isDuplicable(InfoTargetStruct target) {
        return isDuplicable(target.getContainerField(), target.getContainer().isArray());
    }

    // An element of a list that may grow, or an entry of a map
    boolean isDuplicable(InfoFieldStruct containerField, boolean inArray) {

        if (inArray)
            return containerField == null || !containerField.isFixedLength();

        return containerField != null && containerField.getType() == InfoFieldType.MAP;
    }

    boolean isMapEntry(InfoTargetStruct target) {
        return !target.isInArray()
                && target.getContainerField() != null
                && target.getContainerField().getType() == InfoFieldType.MAP;
    }

    boolean isRemovable(InfoFieldStruct containerField, InfoFieldStruct field, boolean inArray) {

        if (inArray)
            return containerField == null || !containerField.isFixedLength();

        if (containerField != null && containerField.getType() == InfoFieldType.MAP)
            return true;

        return field == null || !field.isRequired();
    }
}
