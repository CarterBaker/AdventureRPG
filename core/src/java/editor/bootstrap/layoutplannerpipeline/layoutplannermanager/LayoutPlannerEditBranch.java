package editor.bootstrap.layoutplannerpipeline.layoutplannermanager;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerParameter;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

class LayoutPlannerEditBranch extends BranchPackage {

    /*
     * Reads, steps and sets the numbers and choices of a settlement type the
     * Layout Planner shows, straight in the type's Info entry. An edit is the
     * Info Panel's own kind of edit, made through InfoManager.editEntry(), so
     * the file is marked changed, the Info Panel shows the new value and the
     * type is rebuilt live for every window and its preview planned again,
     * saved or not. A number moves by whole steps or to a value typed, within
     * its bounds, the two ends of a range never crossing; a choice cycles
     * round its names. A field the file leaves out reads as its fallback and
     * is written in on its first edit.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Read \\

    // A field's value in a settlement type's entry, its fallback while the entry or the field is missing; a
    // choice reads as the index of its name
    float read(String settlementName, LayoutPlannerParameter parameter) {

        InfoEntryStruct entry = findEntry(settlementName);

        return entry != null ? read(entry.getArpg(), parameter) : parameter.fallback;
    }

    private float read(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter) {

        ArpgElementStruct field = findField(settlementArpg, parameter);

        if (field == null || !field.isValue())
            return parameter.fallback;

        if (parameter.choices != null)
            return findChoice(parameter, field.getAsString());

        return field.getAsFloat();
    }

    private ArpgElementStruct findField(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(settlementArpg, parameter);

        if (group == null || !group.has(parameter.key))
            return null;

        ArpgElementStruct field = group.get(parameter.key);

        if (!parameter.isRangeEnd())
            return field;

        if (!field.isArray() || field.getAsArray().size() <= parameter.end)
            return null;

        return field.getAsArray().get(parameter.end);
    }

    private ArpgObjectStruct findGroup(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter) {

        if (parameter.group.isEmpty())
            return settlementArpg;

        return ArpgUtility.hasObject(settlementArpg, parameter.group)
                ? settlementArpg.getAsObject(parameter.group)
                : null;
    }

    // The index of a choice's name, its fallback when it names none
    private float findChoice(LayoutPlannerParameter parameter, String name) {

        Enum<?>[] constants = parameter.choices.getEnumConstants();

        for (int i = 0; i < constants.length; i++)
            if (constants[i].name().equalsIgnoreCase(name))
                return i;

        return parameter.fallback;
    }

    // Step \\

    // A field moved a whole number of its steps up or down in a settlement type's entry — false when the type
    // has no entry to edit
    boolean step(String settlementName, LayoutPlannerParameter parameter, int steps) {

        if (findEntry(settlementName) == null)
            return false;

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName,
                settlementArpg -> write(settlementArpg, parameter, resolveValue(
                        settlementArpg, parameter, read(settlementArpg, parameter) + parameter.step * steps)));

        return true;
    }

    // A field set to a value typed — false when the settlement type has no entry to edit
    boolean set(String settlementName, LayoutPlannerParameter parameter, float value) {

        if (findEntry(settlementName) == null)
            return false;

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName,
                settlementArpg -> write(settlementArpg, parameter, resolveValue(settlementArpg, parameter, value)));

        return true;
    }

    // Where a value lands: round a choice's names, else within its bounds and short of its range's other end
    private float resolveValue(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter, float value) {

        if (parameter.choices != null)
            return Math.floorMod((int) value, parameter.choices.getEnumConstants().length);

        value = Math.clamp(value, parameter.min, parameter.max);

        if (!parameter.isRangeEnd())
            return value;

        float partner = read(settlementArpg, findPartner(parameter));

        return parameter.end == 0 ? Math.min(value, partner) : Math.max(value, partner);
    }

    private LayoutPlannerParameter findPartner(LayoutPlannerParameter parameter) {

        LayoutPlannerParameter[] parameters = LayoutPlannerParameter.values();

        for (int i = 0; i < parameters.length; i++)
            if (parameters[i] != parameter && parameters[i].isRangeEnd()
                    && parameters[i].group.equals(parameter.group) && parameters[i].key.equals(parameter.key))
                return parameters[i];

        return throwException("Layout planner parameter " + parameter + " has no other end to its range.");
    }

    // Write \\

    private void write(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter, float value) {

        ArpgObjectStruct group = resolveGroup(settlementArpg, parameter);

        if (!parameter.isRangeEnd()) {
            group.add(parameter.key, toElement(parameter, value));
            return;
        }

        resolveRange(settlementArpg, group, parameter).set(parameter.end, toElement(parameter, value));
    }

    // The field's group, written into the entry when it has none
    private ArpgObjectStruct resolveGroup(ArpgObjectStruct settlementArpg, LayoutPlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(settlementArpg, parameter);

        if (group != null)
            return group;

        group = new ArpgObjectStruct();
        settlementArpg.add(parameter.group, group);

        return group;
    }

    // The range a field is one end of, written whole with both ends as they read when it is missing or short
    private ArpgArrayStruct resolveRange(
            ArpgObjectStruct settlementArpg,
            ArpgObjectStruct group,
            LayoutPlannerParameter parameter) {

        ArpgElementStruct field = group.has(parameter.key) ? group.get(parameter.key) : null;

        if (field != null && field.isArray() && field.getAsArray().size() > parameter.end)
            return field.getAsArray();

        LayoutPlannerParameter partner = findPartner(parameter);
        LayoutPlannerParameter low = parameter.end == 0 ? parameter : partner;
        LayoutPlannerParameter high = parameter.end == 0 ? partner : parameter;
        ArpgArrayStruct range = new ArpgArrayStruct();

        range.add(toElement(low, read(settlementArpg, low)));
        range.add(toElement(high, read(settlementArpg, high)));
        group.add(parameter.key, range);

        return range;
    }

    // A value as the settlement type's file holds it: a choice's name in lower case, a whole number, or a decimal
    // kept to the planner's precision so a step never leaves a long tail of digits
    private ArpgElementStruct toElement(LayoutPlannerParameter parameter, float value) {

        if (parameter.choices != null)
            return new ArpgValueStruct(toChoiceName(parameter, (int) value));

        if (parameter.whole)
            return new ArpgValueStruct((long) Math.round(value));

        return new ArpgValueStruct(
                Math.round(value * EditorSetting.LAYOUT_PLANNER_VALUE_PRECISION)
                        / (double) EditorSetting.LAYOUT_PLANNER_VALUE_PRECISION);
    }

    // A choice's name as the settlement type file holds it
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String toChoiceName(LayoutPlannerParameter parameter, int index) {
        return ArpgUtility.toEnumName((Enum) parameter.choices.getEnumConstants()[index]);
    }

    // Utility \\

    private InfoEntryStruct findEntry(String settlementName) {

        if (settlementName == null || !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName))
            return null;

        return infoManager.findEntry(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName);
    }
}
