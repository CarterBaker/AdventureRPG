package editor.bootstrap.shortcutpipeline.util;

import engine.input.InputCode;
import engine.input.InputNameUtility;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ShortcutUtility extends EngineUtility {

    /*
     * Stateless text every shortcut is read out with: its keys as the editor
     * names them, modifiers first, "Ctrl + Shift + S", appended to a builder
     * its caller keeps, so the Settings page and the hint bar always read a
     * chord the same way and neither allocates to do it.
     */

    // Text \\

    public static void appendChord(StringBuilder textBuilder, int key, int modifiers) {

        int start = textBuilder.length();

        for (ShortcutModifier modifier : ShortcutModifier.VALUES) {

            if ((modifiers & modifier.getBit()) == 0)
                continue;

            if (textBuilder.length() > start)
                textBuilder.append(EngineSetting.INPUT_NAME_COMBO_SEPARATOR);

            textBuilder.append(modifier.getLabel());
        }

        if (key == EngineSetting.INDEX_NOT_FOUND)
            return;

        if (textBuilder.length() > start)
            textBuilder.append(EngineSetting.INPUT_NAME_COMBO_SEPARATOR);

        textBuilder.append(InputNameUtility.getName(InputCode.key(key)));
    }
}
