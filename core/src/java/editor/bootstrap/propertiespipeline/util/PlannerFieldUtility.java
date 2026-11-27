package editor.bootstrap.propertiespipeline.util;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.arpg.ArpgValueStruct;

public final class PlannerFieldUtility extends EngineUtility {

    /*
     * The one path every planner's Properties rows read, step, set and show
     * the numbers and choices of the content they design through, the Tree
     * Planner's species and the Layout Planner's settlement types alike,
     * straight in the content's Info entry. An edit is the Info Panel's own
     * kind of edit, made through InfoManager.editEntry(), so the file is
     * marked changed, the Info Panel shows the new value and the content is
     * rebuilt live in every window, saved or not. A number moves by whole
     * steps or to a value typed, within its bounds, the two ends of a range
     * never crossing; a choice cycles round its names. A field the file
     * leaves out reads as its fallback and is written in on its first edit.
     */

    // Rows \\

    // A field's row: a choice stepped round its names, or a number slid across its bounds
    public static CharacterViewRowStruct toRow(
            InfoManager infoManager,
            String schemaName,
            String entryName,
            PlannerParameter parameter) {

        String text = format(infoManager, schemaName, entryName, parameter);

        if (parameter.getChoices() != null)
            return CharacterViewRowStruct.choice(parameter.name(), parameter.getLabel(), text);

        return CharacterViewRowStruct.ranged(
                parameter.name(), parameter.getLabel(), text,
                read(infoManager, schemaName, entryName, parameter),
                parameter.getMin(), parameter.getMax(), parameter.getStep());
    }

    // A field's value as the planners show it: a choice by its name, a whole number as one, a decimal to two places
    public static String format(
            InfoManager infoManager,
            String schemaName,
            String entryName,
            PlannerParameter parameter) {

        float value = read(infoManager, schemaName, entryName, parameter);

        if (parameter.getChoices() != null)
            return parameter.getChoices().getEnumConstants()[Math.round(value)].name().toLowerCase();

        if (parameter.isWhole())
            return Integer.toString(Math.round(value));

        return String.format(EditorSetting.PLANNER_VALUE_FORMAT, value);
    }

    // A value typed, null when it is not a finite number
    public static Float parse(String text) {

        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Read \\

    // A field's value in an entry, its fallback while the entry or the field is missing; a choice reads as the
    // index of its name
    public static float read(
            InfoManager infoManager,
            String schemaName,
            String entryName,
            PlannerParameter parameter) {

        InfoEntryStruct entry = findEntry(infoManager, schemaName, entryName);

        return entry != null ? read(entry.getArpg(), parameter) : parameter.getFallback();
    }

    private static float read(ArpgObjectStruct entryArpg, PlannerParameter parameter) {

        ArpgElementStruct field = findField(entryArpg, parameter);

        if (field == null || !field.isValue())
            return parameter.getFallback();

        if (parameter.getChoices() != null)
            return findChoice(parameter, field.getAsString());

        return field.getAsFloat();
    }

    private static ArpgElementStruct findField(ArpgObjectStruct entryArpg, PlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(entryArpg, parameter);

        if (group == null || !group.has(parameter.getKey()))
            return null;

        ArpgElementStruct field = group.get(parameter.getKey());

        if (!parameter.isRangeEnd())
            return field;

        if (!field.isArray() || field.getAsArray().size() <= parameter.getEnd())
            return null;

        return field.getAsArray().get(parameter.getEnd());
    }

    private static ArpgObjectStruct findGroup(ArpgObjectStruct entryArpg, PlannerParameter parameter) {

        if (parameter.getGroup().isEmpty())
            return entryArpg;

        return ArpgUtility.hasObject(entryArpg, parameter.getGroup())
                ? entryArpg.getAsObject(parameter.getGroup())
                : null;
    }

    // The index of a choice's name, its fallback when it names none
    private static float findChoice(PlannerParameter parameter, String name) {

        Enum<?>[] constants = parameter.getChoices().getEnumConstants();

        for (int i = 0; i < constants.length; i++)
            if (constants[i].name().equalsIgnoreCase(name))
                return i;

        return parameter.getFallback();
    }

    // Step \\

    // A field moved a whole number of its steps up or down in an entry — false when there is no entry to edit
    public static boolean step(
            InfoManager infoManager,
            String schemaName,
            String entryName,
            PlannerParameter parameter,
            int steps) {

        if (findEntry(infoManager, schemaName, entryName) == null)
            return false;

        infoManager.editEntry(
                schemaName, entryName, entryName,
                entryArpg -> write(entryArpg, parameter, resolveValue(
                        entryArpg, parameter, read(entryArpg, parameter) + parameter.getStep() * steps)));

        return true;
    }

    // A field set to a value typed — false when there is no entry to edit
    public static boolean set(
            InfoManager infoManager,
            String schemaName,
            String entryName,
            PlannerParameter parameter,
            float value) {

        if (findEntry(infoManager, schemaName, entryName) == null)
            return false;

        infoManager.editEntry(
                schemaName, entryName, entryName,
                entryArpg -> write(entryArpg, parameter, resolveValue(entryArpg, parameter, value)));

        return true;
    }

    // Where a value lands: round a choice's names, else within its bounds and short of its range's other end
    private static float resolveValue(ArpgObjectStruct entryArpg, PlannerParameter parameter, float value) {

        if (parameter.getChoices() != null)
            return Math.floorMod((int) value, parameter.getChoices().getEnumConstants().length);

        value = Math.clamp(value, parameter.getMin(), parameter.getMax());

        if (!parameter.isRangeEnd())
            return value;

        float partner = read(entryArpg, findPartner(parameter));

        return parameter.getEnd() == 0 ? Math.min(value, partner) : Math.max(value, partner);
    }

    private static PlannerParameter findPartner(PlannerParameter parameter) {

        PlannerParameter[] parameters = (PlannerParameter[]) ((Enum<?>) parameter).getDeclaringClass()
                .getEnumConstants();

        for (int i = 0; i < parameters.length; i++)
            if (parameters[i] != parameter && parameters[i].isRangeEnd()
                    && parameters[i].getGroup().equals(parameter.getGroup())
                    && parameters[i].getKey().equals(parameter.getKey()))
                return parameters[i];

        return throwException("Planner parameter " + parameter + " has no other end to its range.");
    }

    // Write \\

    private static void write(ArpgObjectStruct entryArpg, PlannerParameter parameter, float value) {

        ArpgObjectStruct group = resolveGroup(entryArpg, parameter);

        if (!parameter.isRangeEnd()) {
            group.add(parameter.getKey(), toElement(parameter, value));
            return;
        }

        resolveRange(entryArpg, group, parameter).set(parameter.getEnd(), toElement(parameter, value));
    }

    // The field's group, written into the entry when it has none
    private static ArpgObjectStruct resolveGroup(ArpgObjectStruct entryArpg, PlannerParameter parameter) {

        ArpgObjectStruct group = findGroup(entryArpg, parameter);

        if (group != null)
            return group;

        group = new ArpgObjectStruct();
        entryArpg.add(parameter.getGroup(), group);

        return group;
    }

    // The range a field is one end of, written whole with both ends as they read when it is missing or short
    private static ArpgArrayStruct resolveRange(
            ArpgObjectStruct entryArpg,
            ArpgObjectStruct group,
            PlannerParameter parameter) {

        ArpgElementStruct field = group.has(parameter.getKey()) ? group.get(parameter.getKey()) : null;

        if (field != null && field.isArray() && field.getAsArray().size() > parameter.getEnd())
            return field.getAsArray();

        PlannerParameter partner = findPartner(parameter);
        PlannerParameter low = parameter.getEnd() == 0 ? parameter : partner;
        PlannerParameter high = parameter.getEnd() == 0 ? partner : parameter;
        ArpgArrayStruct range = new ArpgArrayStruct();

        range.add(toElement(low, read(entryArpg, low)));
        range.add(toElement(high, read(entryArpg, high)));
        group.add(parameter.getKey(), range);

        return range;
    }

    // A value as the entry's file holds it: a choice's name in lower case, a whole number, or a decimal
    // kept to the planner's precision so a step never leaves a long tail of digits
    private static ArpgElementStruct toElement(PlannerParameter parameter, float value) {

        if (parameter.getChoices() != null)
            return new ArpgValueStruct(toChoiceName(parameter, (int) value));

        if (parameter.isWhole())
            return new ArpgValueStruct((long) Math.round(value));

        return new ArpgValueStruct(
                Math.round(value * EditorSetting.PLANNER_VALUE_PRECISION)
                        / (double) EditorSetting.PLANNER_VALUE_PRECISION);
    }

    // A choice's name as the entry's file holds it
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String toChoiceName(PlannerParameter parameter, int index) {
        return ArpgUtility.toEnumName((Enum) parameter.getChoices().getEnumConstants()[index]);
    }

    // Utility \\

    private static InfoEntryStruct findEntry(InfoManager infoManager, String schemaName, String entryName) {

        if (entryName == null || !infoManager.hasDocument(schemaName, entryName))
            return null;

        return infoManager.findEntry(schemaName, entryName, entryName);
    }
}
