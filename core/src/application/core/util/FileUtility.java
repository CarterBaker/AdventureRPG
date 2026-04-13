package application.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import application.core.engine.EngineUtility;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

/*
 * Filesystem helpers shared across all bootstrap loading paths. Covers
 * directory validation, filtered file collection at varying depths, extension
 * inspection, path-relative name resolution, and name format conversion.
 */
public class FileUtility extends EngineUtility {

    // Directory Validation \\

    public static void verifyDirectory(File directory, String message) {
        if (!directory.exists() || !directory.isDirectory())
            throwException(message);
    }

    // File Collection \\

    public static List<File> collectFiles(File root, ObjectArraySet<String> extensions) {
        try (var stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> hasExtension(f, extensions))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return throwException("Failed to walk directory: " + root.getAbsolutePath(), e);
        }
    }

    public static List<File> collectFilesShallow(File root, ObjectArraySet<String> extensions) {
        try (var stream = Files.list(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> hasExtension(f, extensions))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return throwException("Failed to list directory: " + root.getAbsolutePath(), e);
        }
    }

    public static List<File> collectSubdirectories(File root) {
        try (var stream = Files.list(root.toPath())) {
            return stream
                    .filter(Files::isDirectory)
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return throwException("Failed to list subdirectories: " + root.getAbsolutePath(), e);
        }
    }

    public static List<File> collectAllSubdirectories(File root) {
        try (var stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> !p.equals(root.toPath()))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return throwException("Failed to walk subdirectories: " + root.getAbsolutePath(), e);
        }
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
            throw new IllegalArgumentException("File name cannot be null or empty");
        int firstUnderscore = fileName.indexOf('_');
        int lastUnderscore = fileName.lastIndexOf('_');
        if (firstUnderscore == -1 || firstUnderscore != lastUnderscore)
            throw new IllegalArgumentException("File name must contain exactly one underscore");
        if (firstUnderscore == 0 || firstUnderscore == fileName.length() - 1)
            throw new IllegalArgumentException("Underscore cannot be at the start or end of the file name");
        return new String[] {
                fileName.substring(0, firstUnderscore),
                fileName.substring(firstUnderscore + 1)
        };
    }

    // Name Format Conversion \\

    /*
     * Converts a slash-delimited resource path into PascalCase.
     * Each segment's first character is uppercased and the segments are joined
     * without a separator.
     *
     * Examples:
     * "blocks/stone" → "BlocksStone"
     * "world/blocks/dirt" → "WorldBlocksDirt"
     * "Blocks" → "Blocks"
     */
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