package engine.util.arpg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.io.FileUtility;

public class ArpgUtility extends EngineUtility {

    /*
     * The single entry point for every ARPG data file. Reading checks the file
     * through the integrity kernel and decodes its payload into an object tree;
     * writing encodes, seals and swaps the file in through a temporary, so an
     * interrupted write never leaves a broken file behind. Also converts trees
     * to and from the JSON-syntax text form, and validates required and
     * optional fields with clear errors naming the missing or malformed key.
     */

    // Load \\

    public static ArpgObjectStruct readObject(File file) {

        try {
            return decodeObject(Files.readAllBytes(file.toPath()));
        } catch (IOException | RuntimeException e) {
            throw new InternalException("Failed to read ARPG file: " + file.getAbsolutePath() + " ("
                    + describeFailure(e) + ")", e);
        }
    }

    public static ArpgObjectStruct loadObject(File file) {

        try {
            return readObject(file);
        } catch (InternalException e) {
            return throwException(e.getMessage(), e.getCause());
        }
    }

    public static ArpgObjectStruct tryLoadObject(File file) {

        try {
            return readObject(file);
        } catch (InternalException e) {
            return null;
        }
    }

    private static ArpgObjectStruct decodeObject(byte[] file) {

        ArpgElementStruct root = ArpgBinaryUtility.decode(file, ArpgIntegrityUtility.open(file));

        if (!root.isObject())
            throw new InternalException("ARPG file root must be an object, found " + root.describe());

        return root.getAsObject();
    }

    // Write \\

    public static void writeObject(File file, ArpgObjectStruct object) {

        if (!tryWriteObject(file, object))
            throwException("Failed to write ARPG file: " + file.getAbsolutePath());
    }

    public static boolean tryWriteObject(File file, ArpgObjectStruct object) {

        File target = file.getAbsoluteFile();
        File parent = target.getParentFile();
        File temporary = new File(parent, target.getName() + EngineSetting.TEMPORARY_FILE_SUFFIX);

        if (parent != null)
            parent.mkdirs();

        try {
            Files.write(temporary.toPath(), ArpgIntegrityUtility.seal(ArpgBinaryUtility.encode(object)));
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | InternalException e) {
            errorLog("ARPG file could not be written: " + target.getAbsolutePath() + " (" + describeFailure(e) + ")");
            temporary.delete();
            return false;
        }
    }

    // Files \\

    public static File resolveFile(File directory, String name) {
        return new File(directory, toFileName(name));
    }

    public static File resolveCompanionFile(File file) {
        return new File(file.getParentFile(), toFileName(FileUtility.getFileName(file)));
    }

    public static String toFileName(String name) {
        return name + "." + EngineSetting.ARPG_FILE_EXTENSION;
    }

    public static boolean isArpgFile(File file) {
        return file.isFile() && FileUtility.hasExtension(file, EngineSetting.ARPG_FILE_EXTENSION);
    }

    // Text \\

    public static ArpgElementStruct parseText(String text) {
        return ArpgTextUtility.parse(text);
    }

    public static ArpgElementStruct tryParseText(String text) {

        try {
            return ArpgTextUtility.parse(text);
        } catch (InternalException e) {
            return null;
        }
    }

    public static String formatText(ArpgElementStruct element) {
        return ArpgTextUtility.formatPretty(element);
    }

    public static String formatCompactText(ArpgElementStruct element) {
        return ArpgTextUtility.formatCompact(element);
    }

    // Required field accessors — throw if missing \\

    public static String validateString(ArpgObjectStruct object, String key) {
        return requireField(object, key, "").getAsString();
    }

    public static int validateInt(ArpgObjectStruct object, String key) {
        return requireField(object, key, "").getAsInt();
    }

    public static boolean validateBoolean(ArpgObjectStruct object, String key) {
        return requireField(object, key, "").getAsBoolean();
    }

    public static float validateFloat(ArpgObjectStruct object, String key) {
        return requireField(object, key, "").getAsFloat();
    }

    public static ArpgArrayStruct validateArray(ArpgObjectStruct object, String key) {

        ArpgElementStruct element = requireField(object, key, "array ");

        if (!element.isArray())
            return throwException("Field '" + key + "' is not a valid ARPG array");

        return element.getAsArray();
    }

    public static ArpgObjectStruct validateObject(ArpgObjectStruct object, String key) {

        ArpgElementStruct element = requireField(object, key, "object ");

        if (!element.isObject())
            return throwException("Field '" + key + "' is not a valid ARPG object");

        return element.getAsObject();
    }

    public static ArpgArrayStruct validateArray(ArpgObjectStruct object, String key, int requiredSize) {

        ArpgArrayStruct array = validateArray(object, key);

        if (requiredSize > 0 && array.size() != requiredSize)
            return throwException("Array '" + key + "' must have exactly "
                    + requiredSize + " elements, found " + array.size());

        return array;
    }

    private static ArpgElementStruct requireField(ArpgObjectStruct object, String key, String kind) {

        ArpgElementStruct element = object.get(key);

        if (element == null)
            return throwException("Missing required " + kind + "field: '" + key + "'");

        return element;
    }

    // Optional field accessors — return default if missing \\

    public static String getString(ArpgObjectStruct object, String key, String defaultValue) {
        return object.has(key) ? object.get(key).getAsString() : defaultValue;
    }

    public static int getInt(ArpgObjectStruct object, String key, int defaultValue) {
        return object.has(key) ? object.get(key).getAsInt() : defaultValue;
    }

    public static boolean getBoolean(ArpgObjectStruct object, String key, boolean defaultValue) {
        return object.has(key) ? object.get(key).getAsBoolean() : defaultValue;
    }

    public static float getFloat(ArpgObjectStruct object, String key, float defaultValue) {
        return object.has(key) ? object.get(key).getAsFloat() : defaultValue;
    }

    public static <E extends Enum<E>> E getEnum(
            ArpgObjectStruct object,
            String key,
            Class<E> enumClass,
            E defaultValue) {
        return object.has(key) ? toEnum(object.get(key).getAsString(), enumClass) : defaultValue;
    }

    // Enum names — matched ignoring case, so files may spell constants in lower case \\

    public static <E extends Enum<E>> E toEnum(String name, Class<E> enumClass) {

        for (E constant : enumClass.getEnumConstants())
            if (constant.name().equalsIgnoreCase(name))
                return constant;

        return throwException("Unknown " + enumClass.getSimpleName() + " value: '" + name + "'");
    }

    public static <E extends Enum<E>> String toEnumName(E constant) {
        return constant.name().toLowerCase();
    }

    // Type checks — never throw \\

    public static boolean hasString(ArpgObjectStruct object, String key) {
        ArpgElementStruct element = object.get(key);
        return element != null && element.isValue() && element.getAsValue().isString();
    }

    public static boolean hasNumber(ArpgObjectStruct object, String key) {
        ArpgElementStruct element = object.get(key);
        return element != null && element.isValue() && element.getAsValue().isNumber();
    }

    public static boolean hasBoolean(ArpgObjectStruct object, String key) {
        ArpgElementStruct element = object.get(key);
        return element != null && element.isValue() && element.getAsValue().isBoolean();
    }

    public static boolean hasObject(ArpgObjectStruct object, String key) {
        ArpgElementStruct element = object.get(key);
        return element != null && element.isObject();
    }

    public static boolean hasArray(ArpgObjectStruct object, String key) {
        ArpgElementStruct element = object.get(key);
        return element != null && element.isArray();
    }

    // Utility \\

    private static String describeFailure(Exception failure) {
        return failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
    }
}
