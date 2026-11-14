package engine.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class FileUtility extends EngineUtility {

    /*
     * Filesystem helpers shared across all bootstrap loading paths. Covers
     * directory validation, filtered file collection at varying depths,
     * extension inspection, path-relative name resolution, name validation,
     * and name format conversion.
     */

    // Directory Validation \\

    public static void verifyDirectory(File directory, String message) {
        if (!directory.exists() || !directory.isDirectory())
            throwException(message);
    }

    // File Collection \\

    public static ObjectArrayList<File> collectFiles(File root) {
        return collectFiles(root, null);
    }

    public static ObjectArrayList<File> collectFiles(File root, ObjectArraySet<String> extensions) {

        try (Stream<Path> stream = Files.walk(root.toPath())) {
            return collectRegularFiles(stream, extensions);
        } catch (IOException e) {
            return throwException("Failed to walk directory: " + root.getAbsolutePath(), e);
        }
    }

    public static ObjectArrayList<File> collectFilesShallow(File root, ObjectArraySet<String> extensions) {

        try (Stream<Path> stream = Files.list(root.toPath())) {
            return collectRegularFiles(stream, extensions);
        } catch (IOException e) {
            return throwException("Failed to list directory: " + root.getAbsolutePath(), e);
        }
    }

    public static ObjectArrayList<File> collectAllSubdirectories(File root) {

        Path rootPath = root.toPath();
        ObjectArrayList<File> directories = new ObjectArrayList<>();

        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.forEach(path -> {
                if (Files.isDirectory(path) && !path.equals(rootPath))
                    directories.add(path.toFile());
            });
        } catch (IOException e) {
            throwException("Failed to walk subdirectories: " + root.getAbsolutePath(), e);
        }

        return directories;
    }

    private static ObjectArrayList<File> collectRegularFiles(Stream<Path> stream, ObjectArraySet<String> extensions) {

        ObjectArrayList<File> files = new ObjectArrayList<>();

        stream.forEach(path -> {

            if (!Files.isRegularFile(path))
                return;

            File file = path.toFile();

            if (extensions == null || hasExtension(file, extensions))
                files.add(file);
        });

        return files;
    }

    // File Name \\

    public static String getFileName(File file) {
        if (file == null)
            return "";
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex <= 0)
            return name;
        return name.substring(0, dotIndex);
    }

    public static String getFileName(String path) {
        if (path == null || path.isEmpty())
            return "";
        return getFileName(new File(path));
    }

    // Extension \\

    public static String getExtension(File file) {
        if (file == null)
            return "";
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == name.length() - 1)
            return "";
        return name.substring(dotIndex + 1);
    }

    public static String getExtension(String path) {
        if (path == null || path.isEmpty())
            return "";
        return getExtension(new File(path));
    }

    public static boolean hasExtension(File file, ObjectArraySet<String> extensions) {
        if (extensions == null)
            return false;
        String fileType = getExtension(file);
        for (String extension : extensions)
            if (fileType.equals(extension.toLowerCase()))
                return true;
        return false;
    }

    public static boolean hasExtension(File file, String extension) {
        if (extension == null)
            return false;
        return getExtension(file).equals(extension.toLowerCase());
    }

    // Path Resolution \\

    public static String getPathWithFileNameWithoutExtension(File root, File file) {
        if (root == null || file == null)
            return "";
        try {
            String pathStr = resolveRelativePath(root, file);
            int dotIndex = pathStr.lastIndexOf('.');
            return dotIndex > 0 ? pathStr.substring(0, dotIndex) : pathStr;
        } catch (Exception e) {
            return getFileName(file);
        }
    }

    public static String getPathWithFileNameWithExtension(File root, File file) {
        if (root == null || file == null)
            return "";
        try {
            return resolveRelativePath(root, file);
        } catch (Exception e) {
            return file.getName();
        }
    }

    private static String resolveRelativePath(File root, File file) throws Exception {
        Path rootPath = root.toPath().toRealPath();
        Path filePath = file.toPath().toRealPath();
        return rootPath.relativize(filePath).toString().replace('\\', '/');
    }

    // File Name Parsing \\

    public static String[] splitFileNameByUnderscore(String fileName) {
        if (fileName == null || fileName.isEmpty())
            throwException("File name cannot be null or empty");
        int firstUnderscore = fileName.indexOf('_');
        int lastUnderscore = fileName.lastIndexOf('_');
        if (firstUnderscore == -1 || firstUnderscore != lastUnderscore)
            throwException("File name must contain exactly one underscore: " + fileName);
        if (firstUnderscore == 0 || firstUnderscore == fileName.length() - 1)
            throwException("Underscore cannot be at the start or end of the file name: " + fileName);
        return new String[] {
                fileName.substring(0, firstUnderscore),
                fileName.substring(firstUnderscore + 1)
        };
    }

    // Name Validation \\

    public static boolean isValidFileName(String name, int maxLength) {

        if (name == null || name.isEmpty() || name.length() > maxLength)
            return false;

        for (int i = 0; i < name.length(); i++)
            if (!isFileNameCharacter(name.charAt(i)))
                return false;

        return true;
    }

    public static boolean isFileNameCharacter(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || character == '_'
                || character == '-';
    }

    // Name Format Conversion \\

    public static String toPascalCase(String path) {
        if (path == null || path.isEmpty())
            return "";
        String[] segments = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty())
                continue;
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1)
                sb.append(segment.substring(1));
        }
        return sb.toString();
    }
}