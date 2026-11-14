package application.runtime.menueventsmanager.util;

import engine.input.Input;
import engine.input.Keys;
import engine.root.EngineUtility;

public class NameInputUtility extends EngineUtility {

    /*
     * The one way a name is typed in the game, shared by every name field. Keys
     * only ever produce letters, digits, single inner spaces, hyphens and
     * apostrophes, so every name is also a valid save file name, and every key
     * pressed in a frame is taken, so fast typing never drops a letter.
     * Backspace takes the last character back.
     */

    // Typing \\

    // Every key typed this frame applied to a name, true when the name changed
    public static boolean typeName(Input rawInput, StringBuilder name, int maxLength) {

        boolean changed = false;

        if (rawInput.isKeyClicked(Keys.BACKSPACE) && name.length() > 0) {
            name.deleteCharAt(name.length() - 1);
            changed = true;
        }

        boolean shift = rawInput.isKeyDown(Keys.SHIFT_LEFT) || rawInput.isKeyDown(Keys.SHIFT_RIGHT);

        for (int key = Keys.A; key <= Keys.Z; key++)
            if (rawInput.isKeyClicked(key))
                changed |= append(name, shift ? (char) key : Character.toLowerCase((char) key), maxLength);

        for (int key = Keys.NUM_0; key <= Keys.NUM_9; key++)
            if (rawInput.isKeyClicked(key))
                changed |= append(name, (char) key, maxLength);

        if (rawInput.isKeyClicked(Keys.MINUS))
            changed |= append(name, '-', maxLength);

        if (rawInput.isKeyClicked(Keys.APOSTROPHE))
            changed |= append(name, '\'', maxLength);

        if (rawInput.isKeyClicked(Keys.SPACE) && canAppendSpace(name))
            changed |= append(name, ' ', maxLength);

        return changed;
    }

    private static boolean canAppendSpace(StringBuilder name) {
        return name.length() > 0 && name.charAt(name.length() - 1) != ' ';
    }

    private static boolean append(StringBuilder name, char character, int maxLength) {

        if (name.length() >= maxLength)
            return false;

        name.append(character);

        return true;
    }
}
