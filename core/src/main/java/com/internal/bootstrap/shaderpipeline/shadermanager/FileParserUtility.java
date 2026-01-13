package com.internal.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.internal.core.engine.UtilityPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class FileParserUtility extends UtilityPackage {

    // Shader Parsing \\

    // Read all lines in a file and return them as strings
    static String convertFileToRawText(File file) {

        try {

            // Read entire file as text
            String rawText = Files.readString(file.toPath());

            // Remove /* */ block comments
            rawText = stripBlockComments(rawText);

            // Remove // comments (line-based)
            rawText = stripCommentsFromText(rawText);

            return rawText;
        }

        catch (IOException e) {
            throwException("Failed reading file: " + file, e);
        }

        return null;
    }

    static ObjectArrayList<String> convertRawTextToArray(String rawText) {

        ObjectArrayList<String> result = new ObjectArrayList<>();

        for (String line : rawText.split("\n")) {

            line = line.trim();
            if (!line.isEmpty())
                result.add(line);
        }

        return result;
    }

    static String stripCommentsFromText(String text) {

        ObjectArrayList<String> cleaned = new ObjectArrayList<>();

        for (String line : text.split("\n")) {
            String noComment = stripSingleLineComments(line).trim();
            cleaned.add(noComment);
        }

        return String.join("\n", cleaned);
    }

    private static String stripSingleLineComments(String line) {
        int idx = line.indexOf("//");
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    static String stripBlockComments(String text) {

        StringBuilder result = new StringBuilder();

        int len = text.length();
        int i = 0;

        while (i < len) {

            // Found start of block comment
            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {

                i += 2; // Skip "/*"

                // Skip until "*/" or end of text
                while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) {
                    i++;
                }

                // Skip end "*/" if present
                if (i + 1 < len)
                    i += 2;

                continue; // Keep scanning after the block
            }

            // Normal character
            result.append(text.charAt(i));
            i++;
        }

        return result.toString();
    }

    static boolean lineStartsWith(String line, String token) {

        if (line == null)
            return false;

        return line.trim().startsWith(token);
    }

    static int countCharInString(String str, char target) {

        int count = 0;
        for (char c : str.toCharArray())
            if (c == target)
                count++;

        return count;
    }

    static int extractBufferBinding(String line) {

        try {

            int bindingStart = line.indexOf("binding");
            if (bindingStart == -1)
                return -1; // Changed from 0 to -1

            int equalSign = line.indexOf("=", bindingStart);
            int closeParen = line.indexOf(")", equalSign);

            if (equalSign != -1 && closeParen != -1) {
                String bindingStr = line.substring(equalSign + 1, closeParen).trim();
                return Integer.parseInt(bindingStr);
            }
        }

        catch (Exception e) {
            // Parsing failed, return not found
        }

        return -1;
    }

    static int findLastTypeDelimiter(String declaration) {

        // Find the last space before any special characters (comma, bracket, etc.)
        int lastSpace = -1;
        boolean foundNonSpace = false;

        for (int i = declaration.length() - 1; i >= 0; i--) {

            char c = declaration.charAt(i);

            if (c == ',' || c == '[' || c == ']') {
                foundNonSpace = true;
                continue;
            }

            if (Character.isWhitespace(c) && foundNonSpace) {

                // This is the space between type and variable names
                lastSpace = i;
                break;
            }

            if (!Character.isWhitespace(c))
                foundNonSpace = true;
        }

        return lastSpace;
    }

    static int parseIntOrDefault(String str, int defaultValue) {

        try {
            return Integer.parseInt(str.trim());
        }

        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static String extractPayloadAfterToken(String line, String token) {

        if (line == null || token == null)
            return null;

        // Remove the token from the beginning of the line
        String result = line.replaceFirst("^\\s*" + token + "\\s*", "").trim();

        // Strip wrapping quotes: "value"
        if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1)
            result = result.substring(1, result.length() - 1).trim();

        // Strip angle brackets: <value>
        if (result.startsWith("<") && result.endsWith(">") && result.length() > 1)
            result = result.substring(1, result.length() - 1).trim();

        return result;
    }

}