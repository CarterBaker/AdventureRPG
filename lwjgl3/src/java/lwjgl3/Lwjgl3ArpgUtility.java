package lwjgl3;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

class Lwjgl3ArpgUtility extends EngineUtility {

    /*
     * Commands behind the ARPG data tool. Text files use the JSON-syntax text
     * form and are only ever an authoring copy; every sealed file is written
     * through ArpgUtility and read back before a command reports success, so
     * a file the tool accepts is a file the engine will load. Each command
     * reports every file it touched and returns whether all of them passed.
     */

    // Internal
    private static final ObjectArraySet<String> TEXT_FILE_EXTENSIONS = new ObjectArraySet<>(
            new String[] { EngineSetting.ARPG_TEXT_FILE_EXTENSION });

    // Encode \\

    static boolean encode(File textFile, File arpgFile) {

        try {
            ArpgObjectStruct object = readTextObject(textFile);
            sealVerified(object, arpgFile);
            report("sealed", arpgFile);
            return true;
        } catch (InternalException e) {
            return fail(textFile, e);
        }
    }

    // Decode \\

    static boolean decode(File arpgFile, File textFile) {

        try {
            String text = ArpgUtility.formatText(ArpgUtility.readObject(arpgFile));

            if (textFile == null) {
                System.out.println(text);
                return true;
            }

            writeText(textFile, text);
            report("opened", textFile);
            return true;
        } catch (InternalException e) {
            return fail(arpgFile, e);
        }
    }

    // Convert \\

    static boolean convert(File directory) {

        if (!directory.isDirectory())
            return fail(directory, new InternalException("Not a directory"));

        boolean passed = true;

        for (File textFile : collect(directory, TEXT_FILE_EXTENSIONS)) {

            File arpgFile = toSibling(textFile, EngineSetting.ARPG_FILE_EXTENSION);

            if (!encode(textFile, arpgFile)) {
                passed = false;
                continue;
            }

            if (!textFile.delete())
                passed = fail(textFile, new InternalException("Sealed, but the text file could not be deleted"));
        }

        return passed;
    }

    // Export \\

    static boolean export(File directory) {

        if (!directory.isDirectory())
            return fail(directory, new InternalException("Not a directory"));

        boolean passed = true;

        for (File arpgFile : collect(directory, EngineSetting.ARPG_FILE_EXTENSIONS))
            passed &= decode(arpgFile, toSibling(arpgFile, EngineSetting.ARPG_TEXT_FILE_EXTENSION));

        return passed;
    }

    // Verify \\

    static boolean verify(File target) {

        if (!target.exists())
            return fail(target, new InternalException("No such file or directory"));

        boolean passed = true;
        ObjectArrayList<File> files = target.isDirectory()
                ? collect(target, EngineSetting.ARPG_FILE_EXTENSIONS)
                : ObjectArrayList.of(target);

        for (File arpgFile : files) {

            try {
                ArpgUtility.readObject(arpgFile);
            } catch (InternalException e) {
                passed = fail(arpgFile, e);
            }
        }

        System.out.println((passed ? "verified " : "failures among ") + files.size() + " file(s)");
        return passed;
    }

    // Utility \\

    static File toSibling(File file, String extension) {
        return new File(file.getAbsoluteFile().getParentFile(), FileUtility.getFileName(file) + "." + extension);
    }

    private static ArpgObjectStruct readTextObject(File textFile) {

        ArpgElementStruct root;

        try {
            root = ArpgUtility.parseText(Files.readString(textFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new InternalException("Text file could not be read", e);
        }

        if (!root.isObject())
            throw new InternalException("The root of an ARPG file must be an object");

        return root.getAsObject();
    }

    private static void sealVerified(ArpgObjectStruct object, File arpgFile) {

        if (!ArpgUtility.tryWriteObject(arpgFile, object))
            throw new InternalException("Sealed file could not be written to " + arpgFile.getAbsolutePath());

        if (!ArpgUtility.readObject(arpgFile).equals(object))
            throw new InternalException("Sealed file did not read back identically");
    }

    private static void writeText(File textFile, String text) {

        try {
            Files.writeString(textFile.toPath(), text + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InternalException("Text file could not be written", e);
        }
    }

    private static ObjectArrayList<File> collect(File directory, ObjectArraySet<String> extensions) {

        ObjectArrayList<File> files = FileUtility.collectFiles(directory, extensions);
        files.sort(null);
        return files;
    }

    private static void report(String action, File file) {
        System.out.println(action + " " + file.getPath());
    }

    private static boolean fail(File file, InternalException failure) {
        System.err.println("FAILED " + file.getPath() + ": " + failure.getMessage());
        return false;
    }
}
