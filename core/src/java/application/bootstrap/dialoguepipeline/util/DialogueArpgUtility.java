package application.bootstrap.dialoguepipeline.util;

import java.util.function.ToIntFunction;

import application.bootstrap.progressionpipeline.util.ProgressionArpgUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class DialogueArpgUtility extends EngineUtility {

    /*
     * Shared parsing for the dialogue data files, so words, phrases, topics
     * and replies all read a condition and a line the same way.
     * parseCondition() reads the optional "when" object of an entry.
     * parseLines() reads a required list of lines, each an object with its
     * "text", an optional "weight" and an optional "when", and cuts every
     * text into segments: {name} calls for a phrase, [tags]
     * for a dictionary word carrying every tag listed, and <name> for a value
     * of the talk. Every name is resolved here, so a line naming an unknown
     * phrase, tag or value fails at load with the entry that wrote it.
     * Bootstrap-only.
     */

    // Condition \\

    public static DialogueConditionStruct parseCondition(
            ArpgObjectStruct ownerArpg,
            DialogueResolverStruct resolver,
            String owner) {

        if (!ArpgUtility.hasObject(ownerArpg, "when"))
            return DialogueConditionStruct.ALWAYS;

        ArpgObjectStruct whenArpg = ownerArpg.getAsObject("when");
        short[] traitIDs = ProgressionArpgUtility.parseNames(whenArpg, "traits", resolver::resolveTrait);
        float affinityMin = ArpgUtility.getFloat(whenArpg, "affinity_min", -EngineSetting.AFFINITY_MAX);
        float affinityMax = ArpgUtility.getFloat(whenArpg, "affinity_max", EngineSetting.AFFINITY_MAX);
        boolean wordless = false;

        if (affinityMin > affinityMax)
            throwException(owner + " asks under \"when\" for an affinity of at least " + affinityMin
                    + " and at most " + affinityMax + " — no being passes both.");

        for (int i = 0; i < traitIDs.length; i++)
            wordless |= resolver.isWordlessTrait(traitIDs[i]);

        return new DialogueConditionStruct(
                traitIDs,
                ProgressionArpgUtility.parseNames(whenArpg, "not_traits", resolver::resolveTrait),
                ProgressionArpgUtility.parseNames(whenArpg, "moods", resolver::resolveMood),
                ProgressionArpgUtility.parseNames(whenArpg, "professions", resolver::resolveProfession),
                ProgressionArpgUtility.parseNames(whenArpg, "relations", resolver::resolveRelation),
                affinityMin,
                affinityMax,
                ArpgUtility.getEnum(whenArpg, "topic", DialogueTopicState.class, DialogueTopicState.ANY),
                wordless);
    }

    // Lines \\

    public static DialogueLineStruct[] parseLines(
            ArpgObjectStruct ownerArpg,
            int ownerSeed,
            DialogueResolverStruct resolver,
            ToIntFunction<String> phraseResolver,
            String owner) {

        ArpgArrayStruct linesArpg = ArpgUtility.validateArray(ownerArpg, "lines");

        if (linesArpg.isEmpty())
            throwException(owner + " needs at least one line under \"lines\".");

        DialogueLineStruct[] lines = new DialogueLineStruct[linesArpg.size()];

        for (int i = 0; i < lines.length; i++)
            lines[i] = parseLine(linesArpg.get(i), ownerSeed, resolver, phraseResolver, owner);

        return lines;
    }

    private static DialogueLineStruct parseLine(
            ArpgElementStruct lineArpg,
            int ownerSeed,
            DialogueResolverStruct resolver,
            ToIntFunction<String> phraseResolver,
            String owner) {

        if (!lineArpg.isObject())
            throwException(owner + " lists a line that is not an object with its \"text\".");

        ArpgObjectStruct objectArpg = lineArpg.getAsObject();
        float weight = ArpgUtility.getFloat(objectArpg, "weight", EngineSetting.DIALOGUE_LINE_WEIGHT_DEFAULT);

        if (weight <= 0f)
            throwException(owner + " gives a line a \"weight\" of " + weight + " — a weight must be above 0.");

        return cutLine(
                ArpgUtility.validateString(objectArpg, "text"), weight, parseCondition(objectArpg, resolver, owner),
                ownerSeed, resolver, phraseResolver, owner);
    }

    // Segments \\

    // The text cut wherever a brace, square bracket or angle bracket opens, each closed by its own pair
    private static DialogueLineStruct cutLine(
            String text,
            float weight,
            DialogueConditionStruct condition,
            int ownerSeed,
            DialogueResolverStruct resolver,
            ToIntFunction<String> phraseResolver,
            String owner) {

        ObjectArrayList<DialogueSegment> kinds = new ObjectArrayList<>();
        ObjectArrayList<String> texts = new ObjectArrayList<>();
        IntArrayList references = new IntArrayList();
        ObjectArrayList<int[]> tagIDs = new ObjectArrayList<>();
        boolean subjectNamed = false;
        boolean topicExpanded = false;
        int start = 0;

        while (start < text.length()) {

            int open = findOpening(text, start);

            if (open > start || open == EngineSetting.INDEX_NOT_FOUND)
                addSegment(kinds, texts, references, tagIDs, DialogueSegment.LITERAL,
                        text.substring(start, open == EngineSetting.INDEX_NOT_FOUND ? text.length() : open),
                        EngineSetting.REGISTRY_RESERVED_ID, null);

            if (open == EngineSetting.INDEX_NOT_FOUND)
                break;

            int close = text.indexOf(toClosing(text.charAt(open)), open);

            if (close == EngineSetting.INDEX_NOT_FOUND)
                throwException(owner + " opens '" + text.charAt(open) + "' without closing it in the line \"" + text
                        + "\".");

            String name = text.substring(open + 1, close).trim();

            switch (text.charAt(open)) {
                case EngineSetting.DIALOGUE_LINE_PHRASE_OPEN -> {

                    boolean topic = name.equals(EngineSetting.DIALOGUE_PHRASE_TOPIC);

                    topicExpanded |= topic;
                    addSegment(kinds, texts, references, tagIDs,
                            topic ? DialogueSegment.TOPIC : DialogueSegment.PHRASE, name,
                            topic ? EngineSetting.REGISTRY_RESERVED_ID : phraseResolver.applyAsInt(name), null);
                }
                case EngineSetting.DIALOGUE_LINE_WORD_OPEN -> addSegment(
                        kinds, texts, references, tagIDs, DialogueSegment.WORD, name,
                        EngineSetting.REGISTRY_RESERVED_ID, parseTags(name, resolver, owner));
                default -> {

                    DialogueValue value = ArpgUtility.toEnum(name, DialogueValue.class);

                    subjectNamed |= value == DialogueValue.SUBJECT;
                    addSegment(kinds, texts, references, tagIDs, DialogueSegment.VALUE, name, value.ordinal(), null);
                }
            }

            start = close + 1;
        }

        return new DialogueLineStruct(
                DialogueFeatureUtility.mix(ownerSeed, DialogueFeatureUtility.hashName(text)),
                weight,
                condition,
                subjectNamed,
                topicExpanded,
                kinds.toArray(new DialogueSegment[0]),
                texts.toArray(new String[0]),
                references.toIntArray(),
                tagIDs.toArray(new int[0][]));
    }

    private static int findOpening(String text, int start) {

        for (int i = start; i < text.length(); i++)
            if (EngineSetting.DIALOGUE_LINE_OPENINGS.indexOf(text.charAt(i)) != EngineSetting.INDEX_NOT_FOUND)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private static char toClosing(char opening) {
        return EngineSetting.DIALOGUE_LINE_CLOSINGS.charAt(EngineSetting.DIALOGUE_LINE_OPENINGS.indexOf(opening));
    }

    private static int[] parseTags(String names, DialogueResolverStruct resolver, String owner) {

        String[] tagNames = names.split(EngineSetting.DIALOGUE_TAG_SEPARATOR);
        int[] tagIDs = new int[tagNames.length];

        if (names.isEmpty())
            throwException(owner + " draws a word in a line without naming a tag.");

        for (int i = 0; i < tagIDs.length; i++)
            tagIDs[i] = resolver.resolveTag(tagNames[i]);

        return tagIDs;
    }

    private static void addSegment(
            ObjectArrayList<DialogueSegment> kinds,
            ObjectArrayList<String> texts,
            IntArrayList references,
            ObjectArrayList<int[]> tagIDs,
            DialogueSegment kind,
            String text,
            int reference,
            int[] segmentTagIDs) {

        kinds.add(kind);
        texts.add(text);
        references.add(reference);
        tagIDs.add(segmentTagIDs);
    }

    // Names \\

    // An optional list of plain strings, empty when the key is missing
    public static String[] parseStrings(ArpgObjectStruct ownerArpg, String key) {

        if (!ArpgUtility.hasArray(ownerArpg, key))
            return new String[0];

        ArpgArrayStruct stringsArpg = ownerArpg.getAsArray(key);
        String[] strings = new String[stringsArpg.size()];

        for (int i = 0; i < strings.length; i++)
            strings[i] = stringsArpg.get(i).getAsString();

        return strings;
    }

    // True for a word as the dictionary keeps it: lower case, with no space
    public static boolean isWordForm(String form) {

        if (form.isEmpty())
            return false;

        for (int i = 0; i < form.length(); i++)
            if (!isWordCharacter(form.charAt(i)) || Character.isUpperCase(form.charAt(i)))
                return false;

        return true;
    }

    // True for a character a word is made of — a letter, a digit, or an apostrophe or hyphen inside one
    public static boolean isWordCharacter(char character) {
        return Character.isLetterOrDigit(character)
                || EngineSetting.DIALOGUE_WORD_JOINERS.indexOf(character) != EngineSetting.INDEX_NOT_FOUND;
    }
}
