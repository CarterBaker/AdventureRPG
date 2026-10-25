package editor.bootstrap.infopipeline.infomanager;

import editor.bootstrap.infopipeline.infodocument.InfoDocumentInstance;
import editor.bootstrap.infopipeline.infoissue.InfoIssueStruct;
import editor.bootstrap.infopipeline.infolink.InfoLinkStruct;
import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoLocalStepStruct;
import editor.bootstrap.infopipeline.infoschema.InfoReferenceStruct;
import editor.bootstrap.infopipeline.infoschema.InfoRuleStruct;
import editor.bootstrap.infopipeline.infoschema.InfoSchemaHandle;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.bootstrap.infopipeline.util.InfoIssueSeverity;
import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InfoValidationBranch extends BranchPackage {

    /*
     * The one judge of whether the game can load a content file. A file is
     * walked against its schema exactly as its rows are listed, and every
     * value the game would refuse becomes an issue at its row path: a
     * required field left out, a key the game never reads, a value of the
     * wrong type, a fraction where a whole number belongs, an enum value or
     * number out of bounds or below the sibling it must reach, a list of the
     * wrong length or too few values, a field its condition requires, a value
     * repeated where it must be distinct or unique, an entry without a name
     * or with one another entry holds, an entry the game cannot start without
     * taken away, and a name that names nothing — an entry no schema holds, a
     * file no resource directory holds, or a value nothing in its own entry
     * holds. A key the game never reads is noted as ignored rather than as an
     * error: it cannot break the game, so it is kept as it is and never stops
     * a save. The same walk lists every name a file depends on, so saving and
     * deleting can check what the game would find on disk.
     */

    // Internal
    private InfoReferenceBranch infoReferenceBranch;

    // Walk — main thread only
    private ObjectArrayList<String> segments;
    private ObjectOpenHashSet<String> localValues;
    private ObjectOpenHashSet<String> distinctValues;
    private ObjectOpenHashSet<String> entryNames;
    private InfoSchemaHandle schema;
    private String definitionName;
    private String entryName;
    private ArpgObjectStruct entryArpg;
    private ObjectArrayList<InfoIssueStruct> issues;
    private ObjectArrayList<InfoLinkStruct> links;

    // Base \\

    @Override
    protected void create() {

        // Walk
        this.segments = new ObjectArrayList<>();
        this.localValues = new ObjectOpenHashSet<>();
        this.distinctValues = new ObjectOpenHashSet<>();
        this.entryNames = new ObjectOpenHashSet<>();
    }

    @Override
    protected void get() {
        this.infoReferenceBranch = get(InfoReferenceBranch.class);
    }

    // Validate \\

    ObjectArrayList<InfoIssueStruct> validate(InfoDocumentInstance document) {

        ObjectArrayList<InfoIssueStruct> found = new ObjectArrayList<>();

        this.schema = document.getSchema();
        this.issues = found;

        if (schema.isArrayLayout())
            validateEntries(document);
        else
            validateEntry(document.getDefinitionName(), document.getRoot());

        validateRequiredNames(document);
        endWalk();
        found.trim();
        return found;
    }

    private void validateEntries(InfoDocumentInstance document) {

        ArpgObjectStruct root = document.getRoot();

        this.entryName = null;
        this.entryArpg = null;

        for (String key : root.keySet())
            if (!key.equals(schema.getEntriesKey()))
                issues.add(new InfoIssueStruct(
                        null, "", EditorSetting.INFO_PROBLEM_UNKNOWN_KEY + key, InfoIssueSeverity.IGNORED));

        ArpgElementStruct entries = root.get(schema.getEntriesKey());

        if (entries == null || !entries.isArray()) {
            addFileIssue(EditorSetting.INFO_PROBLEM_NO_ENTRIES + schema.getEntriesKey());
            return;
        }

        ArpgArrayStruct entryArray = entries.getAsArray();

        entryNames.clear();

        for (int i = 0; i < entryArray.size(); i++)
            validateArrayEntry(document, entryArray.get(i), i);
    }

    private void validateArrayEntry(InfoDocumentInstance document, ArpgElementStruct entry, int index) {

        if (!entry.isObject()) {
            addFileIssue(EditorSetting.INFO_PROBLEM_ENTRY_NOT_OBJECT + index);
            return;
        }

        String name = document.getEntryName(entry);

        if (name == null || name.isEmpty()) {
            addFileIssue(EditorSetting.INFO_PROBLEM_ENTRY_UNNAMED + index);
            return;
        }

        validateEntry(name, entry.getAsObject());

        segments.add(schema.getNameField());

        if (!entryNames.add(name))
            addIssue(EditorSetting.INFO_PROBLEM_NAME_IN_FILE);
        else if (schema.isReferencedByName() && infoReferenceBranch.countName(schema.getSchemaName(), name) > 1)
            addIssue(EditorSetting.INFO_PROBLEM_NAME_IN_SCHEMA + schema.getTabName());

        segments.pop();
    }

    private void validateEntry(String name, ArpgObjectStruct arpg) {

        this.entryName = name;
        this.entryArpg = arpg;
        segments.clear();

        validateObject(schema.getRootField(), arpg);
        validateUnique(arpg);
    }

    // Every entry the game cannot start without that this file held on disk, still held by some file
    private void validateRequiredNames(InfoDocumentInstance document) {

        ObjectArrayList<String> requiredNames = schema.getRequiredNames();

        if (requiredNames.isEmpty())
            return;

        ObjectOpenHashSet<String> savedNames = new ObjectOpenHashSet<>();
        infoReferenceBranch.addSavedNames(document, savedNames);

        this.entryName = null;
        segments.clear();

        for (int i = 0; i < requiredNames.size(); i++)
            if (savedNames.contains(requiredNames.get(i))
                    && !infoReferenceBranch.hasName(schema.getSchemaName(), requiredNames.get(i)))
                addFileIssue(EditorSetting.INFO_PROBLEM_REQUIRED_NAME + requiredNames.get(i));
    }

    private void validateUnique(ArpgObjectStruct arpg) {

        ObjectArrayList<String> uniqueKeys = schema.getUniqueKeys();

        for (int i = 0; i < uniqueKeys.size(); i++) {

            ArpgElementStruct value = arpg.get(uniqueKeys.get(i));

            if (value == null || !value.isValue()
                    || infoReferenceBranch.countUniqueValue(schema, uniqueKeys.get(i), value.getAsValue()) <= 1)
                continue;

            segments.add(uniqueKeys.get(i));
            addIssue(EditorSetting.INFO_PROBLEM_UNIQUE);
            segments.pop();
        }
    }

    // Values \\

    private void validateValue(InfoFieldStruct field, ArpgElementStruct value, ArpgObjectStruct container) {

        InfoFieldType type = field.getType();

        if (type == InfoFieldType.RAW)
            return;

        if (!type.accepts(value)) {
            addIssue(EditorSetting.INFO_PROBLEM_TYPE + type.getSchemaName());
            return;
        }

        switch (type) {
            case STRING -> validateString(field, value.getAsString(), container);
            case INT, FLOAT -> validateNumber(field, value.getAsValue(), container);
            case ENUM -> validateEnum(field, value.getAsString());
            case OBJECT -> validateObject(field, value.getAsObject());
            case ARRAY -> validateArray(field, value.getAsArray(), container);
            case MAP -> validateMap(field, value.getAsObject(), container);
            default -> {
            }
        }
    }

    private void validateObject(InfoFieldStruct field, ArpgObjectStruct object) {

        ObjectArrayList<InfoFieldStruct> fields = field.getFields();

        for (int i = 0; i < fields.size(); i++) {

            InfoFieldStruct child = fields.get(i);
            ArpgElementStruct value = object.get(child.getKey());

            segments.add(child.getKey());

            if (value != null && child.getRule().hasConflict() && object.has(child.getRule().getConflictsWith()))
                addIssue(EditorSetting.INFO_PROBLEM_CONFLICT + child.getRule().getConflictsWith());
            else if (value != null)
                validateValue(child, value, object);
            else if (isDemanded(child, object))
                addIssue(EditorSetting.INFO_PROBLEM_MISSING);

            segments.pop();
        }

        for (String key : object.keySet()) {

            if (field.findField(key) != null)
                continue;

            segments.add(key);
            issues.add(new InfoIssueStruct(
                    entryName, joinSegments(), EditorSetting.INFO_PROBLEM_UNKNOWN_FIELD, InfoIssueSeverity.IGNORED));
            segments.pop();
        }
    }

    // A field left out that must be there: required, required by its condition, or naming what its key picks
    private boolean isDemanded(InfoFieldStruct field, ArpgObjectStruct object) {

        InfoRuleStruct rule = field.getRule();

        if (field.isRequired())
            return true;

        if (rule.hasRequiredUnless())
            return !isExcused(field, object);

        if (rule.hasRequiredWhen())
            return ArpgUtility.hasString(object, rule.getRequiredWhenKey())
                    && rule.isRequiredFor(object.get(rule.getRequiredWhenKey()).getAsString());

        return field.hasReference() && field.getReference().isKeyed()
                && resolveTargetSchemaName(field.getReference(), object) != null;
    }

    // True when the sibling that excuses a field is set: present, and true when it is a boolean
    private boolean isExcused(InfoFieldStruct field, ArpgObjectStruct object) {

        ArpgElementStruct excuse = object != null ? object.get(field.getRule().getRequiredUnless()) : null;

        if (excuse == null)
            return false;

        return !excuse.isValue() || !excuse.getAsValue().isBoolean() || excuse.getAsBoolean();
    }

    private void validateCount(InfoFieldStruct field, int count, ArpgObjectStruct container) {

        int minCount = field.getRule().getMinCount();

        if (minCount == 0 && field.getRule().hasRequiredUnless() && !isExcused(field, container))
            minCount = 1;

        if (count < minCount)
            addIssue(EditorSetting.INFO_PROBLEM_MIN_COUNT_PREFIX + minCount
                    + EditorSetting.INFO_PROBLEM_LENGTH_SUFFIX);
        else if (count > field.getRule().getMaxCount())
            addIssue(EditorSetting.INFO_PROBLEM_MAX_COUNT_PREFIX + field.getRule().getMaxCount()
                    + EditorSetting.INFO_PROBLEM_LENGTH_SUFFIX);
    }

    private void validateArray(InfoFieldStruct field, ArpgArrayStruct array, ArpgObjectStruct container) {

        if (field.isFixedLength() && array.size() != field.getLength())
            addIssue(EditorSetting.INFO_PROBLEM_LENGTH_PREFIX + field.getLength()
                    + EditorSetting.INFO_PROBLEM_LENGTH_SUFFIX);

        validateCount(field, array.size(), container);

        InfoFieldStruct element = field.getElement();

        for (int i = 0; i < array.size(); i++) {

            segments.add(Integer.toString(i));
            validateValue(element, array.get(i), null);
            segments.pop();
        }

        if (element.isDistinct())
            validateDistinct(array, null);

        if (element.getType() == InfoFieldType.OBJECT)
            for (int i = 0; i < element.getFields().size(); i++)
                if (element.getFields().get(i).isDistinct())
                    validateDistinct(array, element.getFields().get(i).getKey());
    }

    // Each element's value, or each element's field under a key, repeated by no earlier element
    private void validateDistinct(ArpgArrayStruct array, String key) {

        distinctValues.clear();

        for (int i = 0; i < array.size(); i++) {

            ArpgElementStruct value = key != null && array.get(i).isObject()
                    ? array.get(i).getAsObject().get(key)
                    : key == null ? array.get(i) : null;

            if (value == null || !value.isValue() || distinctValues.add(value.getAsString()))
                continue;

            segments.add(Integer.toString(i));

            if (key != null)
                segments.add(key);

            addIssue(EditorSetting.INFO_PROBLEM_DISTINCT);

            if (key != null)
                segments.pop();

            segments.pop();
        }
    }

    private void validateMap(InfoFieldStruct field, ArpgObjectStruct map, ArpgObjectStruct container) {

        validateCount(field, map.size(), container);

        for (String key : map.keySet()) {

            segments.add(key);

            if (field.hasKeyReference())
                validateSchemaReference(field.getKeyReference(), key, null);

            validateValue(field.getElement(), map.get(key), null);
            segments.pop();
        }
    }

    private void validateNumber(InfoFieldStruct field, ArpgValueStruct value, ArpgObjectStruct container) {

        InfoRuleStruct rule = field.getRule();
        boolean whole = field.getType() == InfoFieldType.INT;

        if (whole && !value.isInteger()) {
            addIssue(EditorSetting.INFO_PROBLEM_WHOLE_NUMBER);
            return;
        }

        if (!rule.isWithinBounds(value.getAsDouble())) {
            addIssue(describeBounds(rule, whole));
            return;
        }

        ArpgElementStruct floor = rule.hasAtLeastField() && container != null
                ? container.get(rule.getAtLeastField())
                : null;

        if (floor != null && floor.isValue() && floor.getAsValue().isNumber()
                && value.getAsDouble() < floor.getAsDouble())
            addIssue(EditorSetting.INFO_PROBLEM_BELOW_FIELD + rule.getAtLeastField());
    }

    private void validateEnum(InfoFieldStruct field, String text) {

        ObjectArrayList<String> values = field.getValues();

        for (int i = 0; i < values.size(); i++)
            if (values.get(i).equalsIgnoreCase(text))
                return;

        addIssue(EditorSetting.INFO_PROBLEM_ENUM + String.join(EditorSetting.INFO_PROBLEM_LIST_SEPARATOR, values));
    }

    // References \\

    private void validateString(InfoFieldStruct field, String text, ArpgObjectStruct container) {

        if (!field.hasReference())
            return;

        InfoReferenceStruct reference = field.getReference();

        if (reference.isAllowed(text))
            return;

        switch (reference.getKind()) {
            case SCHEMA -> validateSchemaReference(reference, text, container);
            case RESOURCE -> validateResourceReference(reference, text);
            case LOCAL -> validateLocalReference(reference, text);
        }
    }

    private void validateSchemaReference(InfoReferenceStruct reference, String text, ArpgObjectStruct container) {

        String targetSchemaName = resolveTargetSchemaName(reference, container);

        if (targetSchemaName == null)
            return;

        String targetTabName = infoReferenceBranch.getTabName(targetSchemaName);

        if (text.isEmpty())
            addIssue(EditorSetting.INFO_PROBLEM_NAMES_NOTHING + targetTabName);
        else if (!infoReferenceBranch.hasName(targetSchemaName, text))
            addIssue(EditorSetting.INFO_PROBLEM_QUOTE + text + EditorSetting.INFO_PROBLEM_NOT_IN + targetTabName);
    }

    private void validateResourceReference(InfoReferenceStruct reference, String text) {

        if (text.isEmpty())
            addIssue(EditorSetting.INFO_PROBLEM_NAMES_NOTHING + reference.getDirectory());
        else if (!infoReferenceBranch.hasResource(reference.getDirectory(), reference.getNaming(), text))
            addIssue(EditorSetting.INFO_PROBLEM_QUOTE + text + EditorSetting.INFO_PROBLEM_NO_FILE
                    + reference.getDirectory());
    }

    private void validateLocalReference(InfoReferenceStruct reference, String text) {

        ObjectArrayList<InfoLocalStepStruct> steps = reference.getLocalSteps();

        localValues.clear();
        collectLocalValues(steps, 0, entryArpg, 0);

        if (!localValues.contains(text))
            addIssue(EditorSetting.INFO_PROBLEM_QUOTE + text + EditorSetting.INFO_PROBLEM_NOT_LOCAL
                    + steps.get(steps.size() - 1).getKey());
    }

    // Every value a local path reaches from its entry, the same-element steps held to the referring value's own
    private void collectLocalValues(
            ObjectArrayList<InfoLocalStepStruct> steps,
            int stepIndex,
            ArpgElementStruct current,
            int segmentIndex) {

        if (!current.isObject())
            return;

        InfoLocalStepStruct step = steps.get(stepIndex);
        ArpgElementStruct value = current.getAsObject().get(step.getKey());

        if (value == null)
            return;

        switch (step.getStep()) {
            case FIELD -> {

                if (stepIndex < steps.size() - 1)
                    collectLocalValues(steps, stepIndex + 1, value, segmentIndex + 1);
                else if (value.isValue() && value.getAsValue().isString())
                    localValues.add(value.getAsString());
            }
            case MAP_KEYS -> {

                if (value.isObject())
                    localValues.addAll(value.getAsObject().keySet());
            }
            case ANY_ELEMENT -> {

                if (value.isArray())
                    for (ArpgElementStruct element : value.getAsArray())
                        collectLocalValues(steps, stepIndex + 1, element, segmentIndex + 2);
            }
            case SAME_ELEMENT -> {

                int index = resolveSameIndex(step, segmentIndex);

                if (value.isArray() && index >= 0 && index < value.getAsArray().size())
                    collectLocalValues(steps, stepIndex + 1, value.getAsArray().get(index), segmentIndex + 2);
            }
        }
    }

    // The index of the element the referring value sits in under a step's key, or none when it sits elsewhere
    private int resolveSameIndex(InfoLocalStepStruct step, int segmentIndex) {

        if (segmentIndex + 1 >= segments.size() || !segments.get(segmentIndex).equals(step.getKey()))
            return EngineSetting.INDEX_NOT_FOUND;

        try {
            return Integer.parseInt(segments.get(segmentIndex + 1));
        } catch (NumberFormatException e) {
            return EngineSetting.INDEX_NOT_FOUND;
        }
    }

    private String resolveTargetSchemaName(InfoReferenceStruct reference, ArpgObjectStruct container) {

        if (!reference.isKeyed())
            return reference.getSchemaName();

        if (container == null || !ArpgUtility.hasString(container, reference.getKeyField()))
            return null;

        return reference.resolveSchemaName(container.get(reference.getKeyField()).getAsString());
    }

    // Links \\

    // Every entry or resource a file's content names, read from any tree of it — live, or as it lies on disk
    ObjectArrayList<InfoLinkStruct> collectLinks(
            InfoSchemaHandle linkSchema,
            String linkDefinitionName,
            ArpgObjectStruct root) {

        ObjectArrayList<InfoLinkStruct> found = new ObjectArrayList<>();

        this.schema = linkSchema;
        this.definitionName = linkDefinitionName;
        this.links = found;

        if (!linkSchema.isArrayLayout()) {
            collectEntryLinks(linkDefinitionName, root);
        } else if (ArpgUtility.hasArray(root, linkSchema.getEntriesKey())) {

            for (ArpgElementStruct entry : root.getAsArray(linkSchema.getEntriesKey())) {

                String name = entry.isObject() && ArpgUtility.hasString(entry.getAsObject(), linkSchema.getNameField())
                        ? entry.getAsObject().get(linkSchema.getNameField()).getAsString()
                        : null;

                if (name != null)
                    collectEntryLinks(name, entry.getAsObject());
            }
        }

        endWalk();
        return found;
    }

    private void collectEntryLinks(String name, ArpgObjectStruct arpg) {

        this.entryName = name;
        this.entryArpg = arpg;
        segments.clear();

        collectObjectLinks(schema.getRootField(), arpg);
    }

    private void collectValueLinks(InfoFieldStruct field, ArpgElementStruct value, ArpgObjectStruct container) {

        if (!field.getType().accepts(value))
            return;

        switch (field.getType()) {
            case STRING -> collectStringLink(field, value.getAsString(), container);
            case OBJECT -> collectObjectLinks(field, value.getAsObject());
            case ARRAY -> {

                ArpgArrayStruct array = value.getAsArray();

                for (int i = 0; i < array.size(); i++) {

                    segments.add(Integer.toString(i));
                    collectValueLinks(field.getElement(), array.get(i), null);
                    segments.pop();
                }
            }
            case MAP -> {

                ArpgObjectStruct map = value.getAsObject();

                for (String key : map.keySet()) {

                    segments.add(key);

                    if (field.hasKeyReference())
                        addLink(field.getKeyReference(), key, null);

                    collectValueLinks(field.getElement(), map.get(key), null);
                    segments.pop();
                }
            }
            default -> {
            }
        }
    }

    private void collectObjectLinks(InfoFieldStruct field, ArpgObjectStruct object) {

        ObjectArrayList<InfoFieldStruct> fields = field.getFields();

        for (int i = 0; i < fields.size(); i++) {

            ArpgElementStruct value = object.get(fields.get(i).getKey());

            if (value == null)
                continue;

            segments.add(fields.get(i).getKey());
            collectValueLinks(fields.get(i), value, object);
            segments.pop();
        }
    }

    private void collectStringLink(InfoFieldStruct field, String text, ArpgObjectStruct container) {

        if (!field.hasReference() || text.isEmpty())
            return;

        addLink(field.getReference(), text, container);
    }

    private void addLink(InfoReferenceStruct reference, String text, ArpgObjectStruct container) {

        if (reference.isAllowed(text) || reference.getKind() == InfoReferenceKind.LOCAL)
            return;

        String targetName = reference.getKind() == InfoReferenceKind.SCHEMA
                ? resolveTargetSchemaName(reference, container)
                : reference.getDirectory();

        if (targetName != null)
            links.add(new InfoLinkStruct(
                    reference.getKind(), targetName, text, schema.getSchemaName(), definitionName, entryName,
                    joinSegments()));
    }

    // Issues \\

    private void addIssue(String message) {
        issues.add(new InfoIssueStruct(entryName, joinSegments(), message, InfoIssueSeverity.ERROR));
    }

    private void addFileIssue(String message) {
        issues.add(new InfoIssueStruct(null, "", message, InfoIssueSeverity.ERROR));
    }

    private String describeBounds(InfoRuleStruct rule, boolean whole) {

        if (rule.hasMin() && rule.hasMax() && !rule.isMinExclusive() && !rule.isMaxExclusive())
            return EditorSetting.INFO_PROBLEM_MUST_BE + EditorSetting.INFO_PROBLEM_BOUND_FROM
                    + formatBound(rule.getMin(), whole) + EditorSetting.INFO_PROBLEM_BOUND_TO
                    + formatBound(rule.getMax(), whole);

        String lower = rule.hasMin()
                ? describeBound(rule.isMinExclusive(), EditorSetting.INFO_PROBLEM_BOUND_ABOVE,
                        EditorSetting.INFO_PROBLEM_BOUND_AT_LEAST, rule.getMin(), whole)
                : null;
        String upper = rule.hasMax()
                ? describeBound(rule.isMaxExclusive(), EditorSetting.INFO_PROBLEM_BOUND_BELOW,
                        EditorSetting.INFO_PROBLEM_BOUND_AT_MOST, rule.getMax(), whole)
                : null;

        if (lower != null && upper != null)
            return EditorSetting.INFO_PROBLEM_MUST_BE + lower + EditorSetting.INFO_PROBLEM_BOUND_AND + upper;

        return EditorSetting.INFO_PROBLEM_MUST_BE + (lower != null ? lower : upper);
    }

    private String describeBound(
            boolean exclusive,
            String exclusiveText,
            String inclusiveText,
            double bound,
            boolean whole) {
        return (exclusive ? exclusiveText : inclusiveText) + formatBound(bound, whole);
    }

    private String formatBound(double bound, boolean whole) {
        return whole ? Long.toString((long) bound) : new ArpgValueStruct(bound).getAsString();
    }

    // Utility \\

    private String joinSegments() {
        return String.join(EditorSetting.INFO_PATH_SEPARATOR, segments);
    }

    private void endWalk() {

        this.schema = null;
        this.definitionName = null;
        this.entryName = null;
        this.entryArpg = null;
        this.issues = null;
        this.links = null;
        segments.clear();
    }
}
