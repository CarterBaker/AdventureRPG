package editor.runtime.label;

import editor.runtime.EditorSetting;
import engine.root.EngineUtility;

public class LabelUtility extends EngineUtility {

    /*
     * Stateless text every editor label shares: a data key read as words,
     * each begun in upper case, so a field, a rig part or any other name
     * written in lower case with underscores reads as a person would write
     * it.
     */

    // Words \\

    // A key read as words, each begun in upper case: "animation_tree" reads "Animation Tree"
    public static String toWords(String key) {

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
}
