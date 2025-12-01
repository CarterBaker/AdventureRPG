package com.AdventureRPG.Core.Util;

import java.io.File;
import java.io.IOException;

import com.AdventureRPG.Core.Util.Exceptions.FileException;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class FileUtility {

    // Verify integrity of root path
    public static void verifyDirectory(File directory, String message) {

        if (!directory.exists())
            throw new FileException.FileNotFoundException(message);

        if (!directory.isDirectory())
            throw new FileException.InvalidDirectoryException(message);
    }

    // Gets the file name without extension
    public static String getFileName(File file) {

        if (file == null)
            return "";

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == 0)
            return name;

        return name.substring(0, dotIndex);
    }

    // Gets the file name without extension from a path string
    public static String getFileName(String path) {

        if (path == null || path.isEmpty())
            return "";

        return getFileName(new File(path));
    }

    // Gets the file extension
    public static String getExtension(File file) {

        if (file == null)
            return "";

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == name.length() - 1)
            return "";

        return name.substring(dotIndex + 1);
    }

    // Gets the file extension from a path string
    public static String getExtension(String path) {

        if (path == null || path.isEmpty())
            return "";

        return getExtension(new File(path));
    }

    // Checks if a file has the specified extensions
    public static boolean hasExtension(File file, ObjectArraySet<String> extensions) {

        if (extensions == null)
            return false;

        String fileType = getExtension(file);

        for (String extension : extensions)
            if (fileType.equals(extension.toLowerCase()))
                return true;

        return false;
    }

    // Checks if a file has the specified extension
    public static boolean hasExtension(File file, String extension) {

        if (extension == null)
            return false;

        return getExtension(file).equals(extension.toLowerCase());
    }

    // Get file name and addition data stored by file name split by an underscore
    public static String[] splitFileNameByUnderscore(String fileName) {

        if (fileName == null || fileName.isEmpty()) // TODO: Make my own error
            throw new IllegalArgumentException("File name cannot be null or empty");

        int firstUnderscore = fileName.indexOf('_');
        int lastUnderscore = fileName.lastIndexOf('_');

        // If no underscore or more than one underscore
        if (firstUnderscore == -1 || firstUnderscore != lastUnderscore) // TODO: Make my own error
            throw new IllegalArgumentException("File name must contain exactly one underscore");

        // If underscore is at the start or end
        if (firstUnderscore == 0 || firstUnderscore == fileName.length() - 1) // TODO: Make my own error
            throw new IllegalArgumentException("Underscore cannot be at the start or end of the file name");

        return new String[] {
                fileName.substring(0, firstUnderscore),
                fileName.substring(firstUnderscore + 1)
        };
    }

    // Read all lines in a file and return them as strings
    public static ObjectArrayList<String> readAllLines(File file) {
        try {
            return new ObjectArrayList<>(java.nio.file.Files.readAllLines(file.toPath()));
        } catch (IOException e) {
            throw new FileException.FileReadException("Failed reading file: " + file, e);
        }
    }

}