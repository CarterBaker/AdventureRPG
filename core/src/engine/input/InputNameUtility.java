package engine.input;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class InputNameUtility extends EngineUtility {

    /*
     * Readable names for keys, mouse buttons, and bindings, shown wherever a
     * player sees what an action is bound to. Names come from the Keys and
     * Buttons constant names, so every code either class declares is named
     * with no table to keep in step — SHIFT_LEFT reads "Shift Left" and a
     * mouse button reads "Mouse Right".
     */

    // Names
    private static final Int2ObjectOpenHashMap<String> key2Name = collectNames(Keys.class, "");
    private static final Int2ObjectOpenHashMap<String> button2Name = collectNames(
            Buttons.class,
            EngineSetting.INPUT_NAME_MOUSE_PREFIX);

    // Accessible \\

    public static boolean hasName(InputCode inputCode) {
        return resolveName(inputCode) != null;
    }

    public static String getName(InputCode inputCode) {

        String name = resolveName(inputCode);

        return name != null ? name : EngineSetting.INPUT_NAME_UNKNOWN;
    }

    public static String getName(Binding binding) {

        InputCode[] codes = binding.getCodes();
        StringBuilder name = new StringBuilder();

        for (int i = 0; i < codes.length; i++) {

            if (i > 0)
                name.append(EngineSetting.INPUT_NAME_COMBO_SEPARATOR);

            name.append(getName(codes[i]));
        }

        return name.toString();
    }

    // Internal \\

    private static String resolveName(InputCode inputCode) {
        return inputCode.type == BindingType.KEY
                ? key2Name.get(inputCode.code)
                : button2Name.get(inputCode.code);
    }

    private static Int2ObjectOpenHashMap<String> collectNames(Class<?> constantClass, String prefix) {

        Int2ObjectOpenHashMap<String> code2Name = new Int2ObjectOpenHashMap<>();

        for (Field field : constantClass.getFields()) {

            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class)
                continue;

            try {
                code2Name.putIfAbsent(field.getInt(null), prefix + toTitle(field.getName()));
            } catch (IllegalAccessException e) {
                throwException("Failed to read input constant: " + field.getName(), e);
            }
        }

        return code2Name;
    }

    private static String toTitle(String constantName) {

        String[] words = constantName.split(EngineSetting.INPUT_NAME_FIELD_SEPARATOR);
        StringBuilder title = new StringBuilder();

        for (int i = 0; i < words.length; i++) {

            if (i > 0)
                title.append(EngineSetting.INPUT_NAME_WORD_SEPARATOR);

            title.append(words[i].charAt(0)).append(words[i].substring(1).toLowerCase());
        }

        return title.toString();
    }
}
