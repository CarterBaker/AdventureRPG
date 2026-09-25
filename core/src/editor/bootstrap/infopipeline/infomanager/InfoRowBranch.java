package editor.bootstrap.infopipeline.infomanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import editor.bootstrap.infopipeline.inforow.InfoRowStruct;
import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.util.InfoFieldType;
import editor.bootstrap.infopipeline.util.InfoRowKind;
import engine.editor.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoRowBranch extends BranchPackage {

    /*
     * Flattens the selected entry into info panel rows. Every schema field of
     * an object is listed in schema order, an absent optional one as a
     * MISSING row, followed by any keys the schema does not name. Groups list
     * their children beneath them while expanded; array elements are labelled
     * by index and, for objects, by their first summary field.
     */

    // Internal
    private InfoManager infoManager;
    private InfoEditBranch infoEditBranch;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
        this.infoEditBranch = get(InfoEditBranch.class);
    }

    // Rows \\

    void buildRows(JsonObject entryJson, InfoFieldStruct rootField, ObjectArrayList<InfoRowStruct> rows) {
        addObjectRows(entryJson, rootField, "", 0, rows);
    }

    private void addObjectRows(
            JsonObject json,
            InfoFieldStruct objectField,
            String prefix,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        ObjectArrayList<InfoFieldStruct> fields = objectField.getFields();

        for (int i = 0; i < fields.size(); i++)
            addFieldRows(json, objectField, fields.get(i), prefix, depth, rows);

        for (String key : json.keySet())
            if (objectField.findField(key) == null)
                addValueRows(json.get(key), objectField, null, false, joinPath(prefix, key), key, depth, rows);
    }

    private void addFieldRows(
            JsonObject json,
            InfoFieldStruct objectField,
            InfoFieldStruct field,
            String prefix,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        String path = joinPath(prefix, field.getKey());
        JsonElement value = json.get(field.getKey());

        if (value == null) {
            rows.add(new InfoRowStruct(path, field.getKey(), depth, InfoRowKind.MISSING, "", false, false, false));
            return;
        }

        addValueRows(value, objectField, field, false, path, field.getKey(), depth, rows);
    }

    private void addValueRows(
            JsonElement value,
            InfoFieldStruct containerField,
            InfoFieldStruct field,
            boolean inArray,
            String path,
            String label,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        InfoFieldType type = field != null && field.getType().accepts(value) ? field.getType() : InfoFieldType.JSON;
        boolean removable = infoEditBranch.isRemovable(containerField, field, inArray);

        if (!type.isGroup()) {
            rows.add(new InfoRowStruct(
                    path, label, depth, InfoRowKind.VALUE, toPreview(value, type), false, false, removable));
            return;
        }

        boolean expanded = infoManager.isExpanded(path, depth);
        boolean addable = type == InfoFieldType.MAP || (type == InfoFieldType.ARRAY && !field.isFixedLength());

        rows.add(new InfoRowStruct(
                path, label + toGroupSuffix(type, value), depth, InfoRowKind.GROUP, "",
                expanded, addable, removable));

        if (expanded)
            addGroupChildren(value, field, type, path, depth + 1, rows);
    }

    private void addGroupChildren(
            JsonElement value,
            InfoFieldStruct field,
            InfoFieldType type,
            String path,
            int depth,
            ObjectArrayList<InfoRowStruct> rows) {

        if (type == InfoFieldType.OBJECT) {
            addObjectRows(value.getAsJsonObject(), field, path, depth, rows);
            return;
        }

        if (type == InfoFieldType.ARRAY) {

            JsonArray array = value.getAsJsonArray();

            for (int i = 0; i < array.size(); i++)
                addValueRows(
                        array.get(i), field, field.getElement(), true,
                        joinPath(path, Integer.toString(i)), toIndexLabel(i, array.get(i)), depth, rows);

            return;
        }

        JsonObject map = value.getAsJsonObject();

        for (String key : map.keySet())
            addValueRows(map.get(key), field, field.getElement(), false, joinPath(path, key), key, depth, rows);
    }

    // Labels \\

    private String toIndexLabel(int index, JsonElement element) {

        String label = EditorSetting.INFO_INDEX_OPEN + index + EditorSetting.INFO_INDEX_CLOSE;

        if (!element.isJsonObject())
            return label;

        JsonObject object = element.getAsJsonObject();

        for (String summaryField : EditorSetting.INFO_SUMMARY_FIELDS)
            if (object.has(summaryField) && object.get(summaryField).isJsonPrimitive())
                return label + EditorSetting.INFO_SUMMARY_SEPARATOR + object.get(summaryField).getAsString();

        return label;
    }

    private String toGroupSuffix(InfoFieldType type, JsonElement value) {

        return switch (type) {
            case ARRAY -> EditorSetting.INFO_GROUP_ARRAY_OPEN + value.getAsJsonArray().size()
                    + EditorSetting.INFO_GROUP_ARRAY_CLOSE;
            case MAP -> EditorSetting.INFO_GROUP_MAP_OPEN + value.getAsJsonObject().size()
                    + EditorSetting.INFO_GROUP_MAP_CLOSE;
            default -> EditorSetting.INFO_GROUP_OBJECT_SUFFIX;
        };
    }

    private String toPreview(JsonElement value, InfoFieldType type) {

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
