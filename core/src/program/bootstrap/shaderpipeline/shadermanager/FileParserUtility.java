package program.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import program.core.engine.UtilityPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Stateless text-parsing helpers for reading and pre-processing GLSL source
 * files. Handles comment stripping, line conversion, and token extraction.
 */
class FileParserUtility extends UtilityPackage {

    // File Reading \\

    static String convertFileToRawText(File file) {

        try {
            String rawText = Files.readString(file.toPath());
            rawText = stripBlockComments(rawText);
            rawText = stripLineComments(rawText);
            return rawText;
        } catch (IOException e) {
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

    // Comment Stripping \\

    private static String stripLineComments(String text) {

        ObjectArrayList<String> cleaned = new ObjectArrayList<>();

        for (String line : text.split("\n")) {
            int idx = line.indexOf("//");
            cleaned.add(idx >= 0 ? line.substring(0, idx).trim() : line);
        }

        return String.join("\n", cleaned);
    }

    static String stripBlockComments(String text) {

        StringBuilder result = new StringBuilder();
        int len = text.length();
        int i = 0;

        while (i < len) {

            if (i + 1 < len && text.charAt(i) == '/' && text.charAt(i + 1) == '*') {

                i += 2;

                while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/'))
                    i++;

                if (i + 1 < len)
                    i += 2;

                continue;
            }

            result.append(text.charAt(i));
            i++;
        }

        return result.toString();
    }

    // Token Parsing \\

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
                return -1;

            int equalSign = line.indexOf("=", bindingStart);
            int closeParen = line.indexOf(")", equalSign);

            if (equalSign != -1 && closeParen != -1) {
                String bindingStr = line.substring(equalSign + 1, closeParen).trim();
                return Integer.parseInt(bindingStr);
            }
        } catch (Exception e) {
            // Best-effort parse — malformed binding returns not-found
        }

        return -1;
    }

    static int findLastTypeDelimiter(String declaration) {

        boolean foundNonSpace = false;

        for (int i = declaration.length() - 1; i >= 0; i--) {

            char c = declaration.charAt(i);

            if (c == ',' || c == '[' || c == ']') {
                foundNonSpace = true;
                continue;
            }

            if (Character.isWhitespace(c) && foundNonSpace)
                return i;

            if (!Character.isWhitespace(c))
                foundNonSpace = true;
        }

        return -1;
    }

    static int parseIntOrDefault(String str, int defaultValue) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static String extractPayloadAfterToken(String line, String token) {

        if (line == null || token == null)
            return null;

        String result = line.replaceFirst("^\\s*" + token + "\\s*", "").trim();

        if (result.startsWith("\"") && result.endsWith("\"") && result.length() > 1)
            result = result.substring(1, result.length() - 1).trim();

        if (result.startsWith("<") && result.endsWith(">") && result.length() > 1)
            result = result.substring(1, result.length() - 1).trim();

        return result;
    }
}