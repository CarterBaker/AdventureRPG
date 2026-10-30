package editor.bootstrap.treeplannerpipeline.treeplannermanager;

import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.treeplannerpipeline.treeplanner.TreePlannerParameter;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

class TreeEditBranch extends BranchPackage {

    /*
     * Reads and steps the fields of a tree species the Tree Planner shows,
     * straight in the species' Info entry. A step is the Info Panel's own
     * kind of edit, made through InfoManager.editEntry(), so the file is
     * marked changed, the Info Panel shows the new value and the species is
     * rebuilt live in every window, saved or not. A number moves by its step
     * within its bounds, the two ends of a range never crossing; a choice
     * cycles round its names. A field the file leaves out reads as its
     * fallback and is written in on its first step.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Read \\

    // A field's value in a species' entry, its fallback while the entry or the field is missing; a choice reads as
    // the index of its name
    float read(String treeName, TreePlannerParameter parameter) {

        InfoEntryStruct entry = findEntry(treeName);

        return entry != null ? read(entry.getArpg(), parameter) : parameter.fallback;
    }

    private float read(ArpgObjectStruct treeArpg, TreePlannerParameter parameter) {

        ArpgElementStruct field = findField(treeArpg, parameter);

        if (field == null || !field.isValue())
            return parameter.fallback;

        if (parameter.choices != null)
            return findChoice(parameter, field.getAsString());

        return field.getAsFloat();
    }

    private ArpgElementStruct findField(ArpgObjectStruct treeArpg, TreePlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(treeArpg, parameter);

        if (group == null || !group.has(parameter.key))
            return null;

        ArpgElementStruct field = group.get(parameter.key);

        if (!parameter.isRangeEnd())
            return field;

        if (!field.isArray() || field.getAsArray().size() <= parameter.end)
            return null;

        return field.getAsArray().get(parameter.end);
    }

    private ArpgObjectStruct findGroup(ArpgObjectStruct treeArpg, TreePlannerParameter parameter) {

        if (parameter.group.isEmpty())
            return treeArpg;

        return ArpgUtility.hasObject(treeArpg, parameter.group) ? treeArpg.getAsObject(parameter.group) : null;
    }

    // The index of a choice's name, its fallback when it names none
    private float findChoice(TreePlannerParameter parameter, String name) {

        Enum<?>[] constants = parameter.choices.getEnumConstants();

        for (int i = 0; i < constants.length; i++)
            if (constants[i].name().equalsIgnoreCase(name))
                return i;

        return parameter.fallback;
    }

    // Step \\

    // A field moved one step up or down in a species' entry — false when the species has no entry to edit
    boolean step(String treeName, TreePlannerParameter parameter, int direction) {

        if (findEntry(treeName) == null)
            return false;

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_TREES, treeName, treeName,
                treeArpg -> write(treeArpg, parameter, resolveStep(treeArpg, parameter, direction)));

        return true;
    }

    // Where one step takes a field: round a choice's names, else within its bounds and short of its range's other end
    private float resolveStep(ArpgObjectStruct treeArpg, TreePlannerParameter parameter, int direction) {

        float value = read(treeArpg, parameter) + parameter.step * direction;

        if (parameter.choices != null)
            return Math.floorMod((int) value, parameter.choices.getEnumConstants().length);

        value = Math.clamp(value, parameter.min, parameter.max);

        if (!parameter.isRangeEnd())
            return value;

        float partner = read(treeArpg, findPartner(parameter));

        return parameter.end == 0 ? Math.min(value, partner) : Math.max(value, partner);
    }

    private TreePlannerParameter findPartner(TreePlannerParameter parameter) {

        TreePlannerParameter[] parameters = TreePlannerParameter.values();

        for (int i = 0; i < parameters.length; i++)
            if (parameters[i] != parameter && parameters[i].isRangeEnd()
                    && parameters[i].group.equals(parameter.group) && parameters[i].key.equals(parameter.key))
                return parameters[i];

        return throwException("Tree planner parameter " + parameter + " has no other end to its range.");
    }

    // Write \\

    private void write(ArpgObjectStruct treeArpg, TreePlannerParameter parameter, float value) {

        ArpgObjectStruct group = resolveGroup(treeArpg, parameter);

        if (!parameter.isRangeEnd()) {
            group.add(parameter.key, toElement(parameter, value));
            return;
        }

        resolveRange(treeArpg, group, parameter).set(parameter.end, toElement(parameter, value));
    }

    // The field's group, written into the entry when it has none
    private ArpgObjectStruct resolveGroup(ArpgObjectStruct treeArpg, TreePlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(treeArpg, parameter);

        if (group != null)
            return group;

        group = new ArpgObjectStruct();
        treeArpg.add(parameter.group, group);

        return group;
    }

    // The range a field is one end of, written whole with both ends as they read when it is missing or short
    private ArpgArrayStruct resolveRange(
            ArpgObjectStruct treeArpg,
            ArpgObjectStruct group,
            TreePlannerParameter parameter) {

        ArpgElementStruct field = group.has(parameter.key) ? group.get(parameter.key) : null;

        if (field != null && field.isArray() && field.getAsArray().size() > parameter.end)
            return field.getAsArray();

        TreePlannerParameter partner = findPartner(parameter);
        TreePlannerParameter low = parameter.end == 0 ? parameter : partner;
        TreePlannerParameter high = parameter.end == 0 ? partner : parameter;
        ArpgArrayStruct range = new ArpgArrayStruct();

        range.add(toElement(low, read(treeArpg, low)));
        range.add(toElement(high, read(treeArpg, high)));
        group.add(parameter.key, range);

        return range;
    }

    // A value as the species file holds it: a choice's name in lower case, a whole number, or a decimal kept to the
    // planner's precision so a step never leaves a long tail of digits
    private ArpgElementStruct toElement(TreePlannerParameter parameter, float value) {

        if (parameter.choices != null)
            return new ArpgValueStruct(toChoiceName(parameter, (int) value));

        if (parameter.whole)
            return new ArpgValueStruct((long) Math.round(value));

        return new ArpgValueStruct(
                Math.round(value * EditorSetting.TREE_PLANNER_VALUE_PRECISION)
                        / (double) EditorSetting.TREE_PLANNER_VALUE_PRECISION);
    }

    // A choice's name as the species file holds it
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String toChoiceName(TreePlannerParameter parameter, int index) {
        return ArpgUtility.toEnumName((Enum) parameter.choices.getEnumConstants()[index]);
    }

    // Utility \\

    private InfoEntryStruct findEntry(String treeName) {

        if (treeName == null || !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_TREES, treeName))
            return null;

        return infoManager.findEntry(EditorSetting.INFO_SCHEMA_TREES, treeName, treeName);
    }
}
