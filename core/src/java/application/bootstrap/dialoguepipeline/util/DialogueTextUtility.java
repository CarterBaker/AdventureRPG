package application.bootstrap.dialoguepipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class DialogueTextUtility extends EngineUtility {

    /*
     * Finishes generated speech and reads it back. polish() makes the pieces
     * a reply was assembled from read as one sentence, in place: runs of
     * spaces closed up and none left before a mark, a full stop dropped where
     * a sentence had already ended, "a" and "an" matched to
     * the word that follows, every sentence opened with a capital and the
     * last one closed. An action written between asterisks keeps its small
     * letters. hashWords() walks finished text word by word, handing back the
     * stable number of each in lower case, which the wording model pairs.
     */

    // Polish \\

    public static void polish(StringBuilder text) {
        closeSpaces(text);
        matchArticles(text);
        capitalise(text);
        closeSentence(text);
    }

    private static void closeSpaces(StringBuilder text) {

        int write = 0;
        boolean spaced = false;

        for (int read = 0; read < text.length(); read++) {

            char character = text.charAt(read);

            if (Character.isWhitespace(character)) {
                spaced = write > 0;
                continue;
            }

            if (character == EngineSetting.DIALOGUE_SENTENCE_CLOSE && write > 0
                    && isSentenceEnd(text.charAt(write - 1)))
                continue;

            if (spaced && !isMark(character))
                text.setCharAt(write++, ' ');

            spaced = false;
            text.setCharAt(write++, character);
        }

        text.setLength(write);
    }

    // Every "a" or "an" standing alone before a word takes the form that word's first letter calls for
    private static void matchArticles(StringBuilder text) {

        for (int i = 0; i < text.length(); i++) {

            if (Character.toLowerCase(text.charAt(i)) != 'a' || (i > 0 && isWordCharacter(text.charAt(i - 1))))
                continue;

            boolean lengthened = i + 1 < text.length() && Character.toLowerCase(text.charAt(i + 1)) == 'n';
            int end = lengthened ? i + 2 : i + 1;

            if (end + 1 >= text.length() || text.charAt(end) != ' ' || !Character.isLetter(text.charAt(end + 1)))
                continue;

            boolean vowel = EngineSetting.DIALOGUE_VOWELS.indexOf(
                    Character.toLowerCase(text.charAt(end + 1))) != EngineSetting.INDEX_NOT_FOUND;

            if (vowel && !lengthened)
                text.insert(i + 1, 'n');

            if (!vowel && lengthened)
                text.deleteCharAt(i + 1);
        }
    }

    private static void capitalise(StringBuilder text) {

        boolean opening = true;

        for (int i = 0; i < text.length(); i++) {

            char character = text.charAt(i);

            if (isSentenceEnd(character)) {
                opening = true;
                continue;
            }

            if (Character.isWhitespace(character))
                continue;

            if (opening && Character.isLetter(character))
                text.setCharAt(i, Character.toUpperCase(character));

            opening = false;
        }
    }

    private static void closeSentence(StringBuilder text) {

        if (text.length() > 0 && Character.isLetterOrDigit(text.charAt(text.length() - 1)))
            text.append(EngineSetting.DIALOGUE_SENTENCE_CLOSE);
    }

    // A word with its first letter raised, as a name is written
    public static void appendCapitalised(StringBuilder text, String word) {

        if (word.isEmpty())
            return;

        text.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
    }

    // Words \\

    // The end of the word starting at or after an index, or the text's length when no word is left; the word's
    // start is found by findWordStart
    public static int findWordEnd(CharSequence text, int start) {

        int end = start;

        while (end < text.length() && isWordCharacter(text.charAt(end)))
            end++;

        return end;
    }

    public static int findWordStart(CharSequence text, int from) {

        int start = from;

        while (start < text.length() && !isWordCharacter(text.charAt(start)))
            start++;

        return start;
    }

    // The stable number of a word of the text, read in lower case
    public static int hashWord(CharSequence text, int start, int end) {

        int hash = EngineSetting.FNV_OFFSET_BASIS;

        for (int i = start; i < end; i++) {
            hash ^= Character.toLowerCase(text.charAt(i));
            hash *= EngineSetting.FNV_PRIME;
        }

        return hash;
    }

    public static boolean isWordCharacter(char character) {
        return DialogueArpgUtility.isWordCharacter(character);
    }

    private static boolean isMark(char character) {
        return EngineSetting.DIALOGUE_MARKS.indexOf(character) != EngineSetting.INDEX_NOT_FOUND;
    }

    private static boolean isSentenceEnd(char character) {
        return EngineSetting.DIALOGUE_SENTENCE_ENDS.indexOf(character) != EngineSetting.INDEX_NOT_FOUND;
    }
}
