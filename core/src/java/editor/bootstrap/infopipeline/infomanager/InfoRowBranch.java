package editor.bootstrap.infopipeline.infomanager;

import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.bootstrap.infopipeline.util.InfoRowKind;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

class InfoRowBranch extends BranchPackage {

    /*
     * Flattens the selected entry into info panel rows. Every schema field of
     * an object is listed under its key read as words, its plain values first
     * and its groups after, each in schema order, an absent optional one as a
     * MISSING row, followed by any keys the schema does not name. A number is
     * a NUMBER row, a boolean, an enum or a name of other content a CHOICE
     * row, anything else typed. Groups list their children beneath them while
     * expanded, and one standing in a list or a map may be duplicated; array
     * elements are labelled by index and, for objects, by their first summary
     * field. A row whose
     * path holds an error, or lies above one, is marked as holding it, and a
     * row the game ignores is marked as ignored.
     */

    // Internal
    private InfoManager infoManager;
    private InfoEditBranch infoEditBranch;
    private InfoChoiceBranch infoChoiceBranch;

    // Rows — main thread only
    private ObjectOpenHashSet<String> errorPaths;
    private ObjectOpenHashSet<String> ignoredPaths;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.infoEditBranch = get(InfoEditBranch.class);
        this.infoChoiceBranch = get(InfoChoiceBranch.class);
    }

    // Rows \\

    void buildRows(
            ArpgObjectStruct entryArpg,
            InfoFieldStruct rootField,
            ObjectOpenHashSet<String> errorPaths,
            ObjectOpenHashSet<String> ignoredPaths,
            ObjectArrayList<InfoRowStruct> rows) {

        this.errorPaths = errorPaths;
        this.ignoredPaths = ignoredPaths;
        addObjectRows(entryArpg, rootField, "", 0, rows);
        this.errorPaths = null;
        this.ignoredPaths = null;
    }

    private void addObjectRows(
            ArpgObjectStruct arpg,
            InfoFieldStruct objectField,
            String prefix,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        ObjectArrayList<InfoFieldStruct> fields = objectField.getFields();

        for (int i = 0; i < fields.size(); i++)
            if (!isGroup(fields.get(i), arpg.get(fields.get(i).getKey())))
                addFieldRows(arpg, objectField, fields.get(i), prefix, depth, rows);

        for (int i = 0; i < fields.size(); i++)
            if (isGroup(fields.get(i), arpg.get(fields.get(i).getKey())))
                addFieldRows(arpg, objectField, fields.get(i), prefix, depth, rows);

        for (String key : arpg.keySet())
            if (objectField.findField(key) == null)
                addValueRows(arpg.get(key), objectField, null, false, joinPath(prefix, key), key, depth, rows);
    }

    private void addFieldRows(
            ArpgObjectStruct arpg,
            InfoFieldStruct objectField,
            InfoFieldStruct field,
            String prefix,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        String path = joinPath(prefix, field.getKey());
        ArpgElementStruct value = arpg.get(field.getKey());

        if (value == null) {
            rows.add(new InfoRowStruct(
                    path, toFieldLabel(field.getKey()), depth, InfoRowKind.MISSING, "", Float.NaN,
                    false, false, false, false, errorPaths.contains(path), false));
            return;
        }

        addValueRows(value, objectField, field, false, path, toFieldLabel(field.getKey()), depth, rows);
    }

    private void addValueRows(
            ArpgElementStruct value,
            InfoFieldStruct containerField,
            InfoFieldStruct field,
            boolean inArray,
            String path,
            String label,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        InfoFieldType type = field != null && field.getType().accepts(value) ? field.getType() : InfoFieldType.RAW;
        boolean removable = infoEditBranch.isRemovable(containerField, field, inArray);

        if (!type.isGroup()) {

            InfoRowKind kind = toValueKind(field, type);
            float fraction = kind == InfoRowKind.NUMBER ? infoEditBranch.toFraction(field, value) : Float.NaN;

            String preview = toPreview(value, type);

            rows.add(new InfoRowStruct(
                    path, label, depth, kind,
                    kind == InfoRowKind.CHOICE && preview.isEmpty() ? EditorSetting.INFO_VALUE_NONE : preview,
                    fraction, false, false, removable, false, errorPaths.contains(path), ignoredPaths.contains(path)));
            return;
        }

        boolean expanded = infoManager.isExpanded(path, depth);
        boolean addable = type == InfoFieldType.MAP || (type == InfoFieldType.ARRAY && !field.isFixedLength());
        boolean duplicable = isElement(containerField, inArray)
                && infoEditBranch.isDuplicable(containerField, inArray);

        rows.add(new InfoRowStruct(
                path, label + toGroupSuffix(type, value), depth, InfoRowKind.GROUP, "", Float.NaN,
                expanded, addable, removable, duplicable, errorPaths.contains(path), ignoredPaths.contains(path)));

        if (expanded)
            addGroupChildren(value, field, type, path, depth + 1, rows);
    }

    private void addGroupChildren(
            ArpgElementStruct value,
            InfoFieldStruct field,
            InfoFieldType type,
            String path,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        if (type == InfoFieldType.OBJECT) {
            addObjectRows(value.getAsObject(), field, path, depth, rows);
            return;
        }

        if (type == InfoFieldType.ARRAY) {

            ArpgArrayStruct array = value.getAsArray();

            for (int i = 0; i < array.size(); i++)
                addValueRows(
                        array.get(i), field, field.getElement(), true,
                        joinPath(path, Integer.toString(i)), toIndexLabel(i, array.get(i)), depth, rows);

            return;
        }

        ArpgObjectStruct map = value.getAsObject();

        for (String key : map.keySet())
            addValueRows(map.get(key), field, field.getElement(), false, joinPath(path, key), key, depth, rows);
    }

    // Kinds \\

    private InfoRowKind toValueKind(InfoFieldStruct field, InfoFieldType type) {

        if (type == InfoFieldType.INT || type == InfoFieldType.FLOAT)
            return InfoRowKind.NUMBER;

        return type != InfoFieldType.RAW && infoChoiceBranch.isChoice(field) ? InfoRowKind.CHOICE : InfoRowKind.VALUE;
    }

    // A field listed with the groups: one holding a group, or an absent one that would
    private boolean isGroup(InfoFieldStruct field, ArpgElementStruct value) {
        return field.getType().isGroup() && (value == null || field.getType().accepts(value));
    }

    private boolean isElement(InfoFieldStruct containerField, boolean inArray) {
        return inArray || (containerField != null && containerField.getType() == InfoFieldType.MAP);
    }

    // Labels \\

    // A field's key read as words, each begun in upper case: "animation_tree" reads "Animation Tree"
    private String toFieldLabel(String key) {

        StringBuilder label = new StringBuilder(key.length());
        boolean wordStart = true;

        for (int i = 0; i < key.length(); i++) {

            char character = key.charAt(i);

            if (character == EditorSetting.INFO_KEY_WORD_SEPARATOR) {
                label.append(EditorSetting.INFO_LABEL_WORD_SEPARATOR);
                wordStart = true;
                continue;
            }

            label.append(wordStart ? Character.toUpperCase(character) : character);
            wordStart = false;
        }

        return label.toString();
    }

    private String toIndexLabel(int index, ArpgElementStruct element) {

        String label = EditorSetting.INFO_INDEX_OPEN + index + EditorSetting.INFO_INDEX_CLOSE;

        if (!element.isObject())
            return label;

        ArpgObjectStruct object = element.getAsObject();

        for (String summaryField : EditorSetting.INFO_SUMMARY_FIELDS)
            if (object.has(summaryField) && object.get(summaryField).isValue())
                return label + EditorSetting.INFO_SUMMARY_SEPARATOR + object.get(summaryField).getAsString();

        return label;
    }

    private String toGroupSuffix(InfoFieldType type, ArpgElementStruct value) {

        return switch (type) {
            case ARRAY -> EditorSetting.INFO_GROUP_ARRAY_OPEN + value.getAsArray().size()
                    + EditorSetting.INFO_GROUP_ARRAY_CLOSE;
            case MAP -> EditorSetting.INFO_GROUP_MAP_OPEN + value.getAsObject().size()
                    + EditorSetting.INFO_GROUP_MAP_CLOSE;
            default -> EditorSetting.INFO_GROUP_OBJECT_SUFFIX;
        };
    }

    private String toPreview(ArpgElementStruct value, InfoFieldType type) {

        String text = infoEditBranch.formatValue(value, type);

        if (text.length() <= EditorSetting.INFO_VALUE_PREVIEW_LENGTH)
            return text;

        return text.substring(0, EditorSetting.INFO_VALUE_PREVIEW_LENGTH) + EditorSetting.INFO_VALUE_ELLIPSIS;
    }

    // Utility \\

    private String joinPath(String prefix, String segment) {
        return prefix.isEmpty() ? segment : prefix + EditorSetting.INFO_PATH_SEPARATOR + segment;
    }
}
