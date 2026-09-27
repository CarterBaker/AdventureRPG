package engine.util.arpg;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.StructPackage;
import engine.root.UtilityPackage.InternalException;

class ArpgTextUtility extends EngineUtility {

    /*
     * Reads and writes the ARPG text form, which is strict JSON syntax. It
     * exists for authoring and inspection — the converter tool, the editor's
     * raw value field, and diagnostics — while the engine itself only ever
     * reads sealed binary files. Parsing rejects comments, trailing commas,
     * repeated keys and non-finite numbers, and reports the line and column
     * of the first problem. Pretty output indents objects and nested arrays
     * one member per line and keeps arrays of plain values on a single line.
     */

    // Parse \\

    static ArpgElementStruct parse(String text) {

        ParseCursorStruct cursor = new ParseCursorStruct(text);
        cursor.consume(EngineSetting.ARPG_TEXT_BYTE_ORDER_MARK);
        cursor.skipWhitespace();

        ArpgElementStruct root = readElement(cursor);
        cursor.skipWhitespace();

        if (cursor.hasRemaining())
            throw cursor.error("Unexpected text after the root element");

        return root;
    }

    private static ArpgElementStruct readElement(ParseCursorStruct cursor) {

        if (!cursor.hasRemaining())
            throw cursor.error("Expected a value but the text ended");

        char current = cursor.peek();

        return switch (current) {
            case '{' -> readObject(cursor);
            case '[' -> readArray(cursor);
            case '"' -> new ArpgValueStruct(readString(cursor));
            case 't' -> readLiteral(cursor, Boolean.TRUE.toString(), ArpgValueStruct.TRUE);
            case 'f' -> readLiteral(cursor, Boolean.FALSE.toString(), ArpgValueStruct.FALSE);
            case 'n' -> readLiteral(cursor, EngineSetting.ARPG_TEXT_NULL_LITERAL, ArpgNullStruct.INSTANCE);
            default -> {

                if (current == '-' || isDigit(current))
                    yield readNumber(cursor);

                throw cursor.error("Unexpected character '" + current + "'");
            }
        };
    }

    private static ArpgObjectStruct readObject(ParseCursorStruct cursor) {

        ArpgObjectStruct object = new ArpgObjectStruct();
        cursor.expect('{');
        cursor.skipWhitespace();

        if (cursor.consume('}'))
            return object;

        do {

            cursor.skipWhitespace();

            if (!cursor.hasRemaining() || cursor.peek() != '"')
                throw cursor.error("Expected a quoted key");

            String key = readString(cursor);

            if (object.has(key))
                throw cursor.error("Key '" + key + "' appears twice in one object");

            cursor.skipWhitespace();
            cursor.expect(':');
            cursor.skipWhitespace();
            object.add(key, readElement(cursor));
            cursor.skipWhitespace();
        } while (cursor.consume(','));

        cursor.expect('}');
        return object;
    }

    private static ArpgArrayStruct readArray(ParseCursorStruct cursor) {

        ArpgArrayStruct array = new ArpgArrayStruct();
        cursor.expect('[');
        cursor.skipWhitespace();

        if (cursor.consume(']'))
            return array;

        do {
            cursor.skipWhitespace();
            array.add(readElement(cursor));
            cursor.skipWhitespace();
        } while (cursor.consume(','));

        cursor.expect(']');
        return array;
    }

    private static String readString(ParseCursorStruct cursor) {

        cursor.expect('"');
        StringBuilder builder = new StringBuilder();

        while (true) {

            if (!cursor.hasRemaining())
                throw cursor.error("String is missing its closing quote");

            char current = cursor.next();

            if (current == '"')
                return builder.toString();

            if (current < EngineSetting.ARPG_TEXT_ESCAPE_LIMIT)
                throw cursor.error("Strings cannot hold raw control characters; escape them");

            builder.append(current == '\\' ? readEscape(cursor) : current);
        }
    }

    private static char readEscape(ParseCursorStruct cursor) {

        if (!cursor.hasRemaining())
            throw cursor.error("String ends inside an escape");

        char escape = cursor.next();

        return switch (escape) {
            case '"', '\\', '/' -> escape;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape(cursor);
            default -> throw cursor.error("Unknown escape '\\" + escape + "'");
        };
    }

    private static char readUnicodeEscape(ParseCursorStruct cursor) {

        int codeUnit = 0;

        for (int i = 0; i < EngineSetting.ARPG_TEXT_UNICODE_DIGITS; i++) {

            if (!cursor.hasRemaining())
                throw cursor.error("String ends inside a unicode escape");

            int digit = Character.digit(cursor.next(), EngineSetting.ARPG_TEXT_HEX_RADIX);

            if (digit < 0)
                throw cursor.error("Unicode escapes need four hex digits");

            codeUnit = codeUnit * EngineSetting.ARPG_TEXT_HEX_RADIX + digit;
        }

        return (char) codeUnit;
    }

    private static ArpgElementStruct readLiteral(ParseCursorStruct cursor, String literal, ArpgElementStruct value) {

        if (!cursor.consume(literal))
            throw cursor.error("Expected '" + literal + "'");

        return value;
    }

    private static ArpgValueStruct readNumber(ParseCursorStruct cursor) {

        int start = cursor.getPosition();
        boolean integral = true;

        cursor.consume('-');

        if (!cursor.consume('0'))
            readDigits(cursor);

        if (cursor.consume('.')) {
            integral = false;
            readDigits(cursor);
        }

        if (cursor.consume('e') || cursor.consume('E')) {

            integral = false;

            if (!cursor.consume('+'))
                cursor.consume('-');

            readDigits(cursor);
        }

        String literal = cursor.substring(start);

        if (!integral)
            return readDecimal(cursor, literal);

        try {
            return new ArpgValueStruct(Long.parseLong(literal));
        } catch (NumberFormatException e) {
            return readDecimal(cursor, literal);
        }
    }

    private static ArpgValueStruct readDecimal(ParseCursorStruct cursor, String literal) {

        double decimal = Double.parseDouble(literal);

        if (!Double.isFinite(decimal))
            throw cursor.error("Number '" + literal + "' is too large to store");

        return new ArpgValueStruct(decimal);
    }

    private static void readDigits(ParseCursorStruct cursor) {

        if (!cursor.hasRemaining() || !isDigit(cursor.peek()))
            throw cursor.error("Expected a digit");

        while (cursor.hasRemaining() && isDigit(cursor.peek()))
            cursor.next();
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    // Format \\

    static String formatPretty(ArpgElementStruct element) {

        StringBuilder builder = new StringBuilder();
        writePretty(element, 0, builder);
        return builder.toString();
    }

    static String formatCompact(ArpgElementStruct element) {

        StringBuilder builder = new StringBuilder();
        writeCompact(element, builder);
        return builder.toString();
    }

    static String formatDecimal(double value) {
        return Double.toString(value);
    }

    private static void writePretty(ArpgElementStruct element, int depth, StringBuilder builder) {

        if (element.isObject()) {
            writePrettyObject(element.getAsObject(), depth, builder);
            return;
        }

        if (element.isArray()) {
            writePrettyArray(element.getAsArray(), depth, builder);
            return;
        }

        writeScalar(element, builder);
    }

    private static void writePrettyObject(ArpgObjectStruct object, int depth, StringBuilder builder) {

        if (object.isEmpty()) {
            builder.append("{}");
            return;
        }

        builder.append('{');
        boolean first = true;

        for (String key : object.keySet()) {

            builder.append(first ? "\n" : ",\n");
            indent(depth + 1, builder);
            writeString(key, builder);
            builder.append(": ");
            writePretty(object.get(key), depth + 1, builder);
            first = false;
        }

        builder.append('\n');
        indent(depth, builder);
        builder.append('}');
    }

    private static void writePrettyArray(ArpgArrayStruct array, int depth, StringBuilder builder) {

        if (array.isEmpty()) {
            builder.append("[]");
            return;
        }

        if (holdsOnlyScalars(array)) {

            builder.append('[');

            for (int i = 0; i < array.size(); i++) {

                if (i > 0)
                    builder.append(", ");

                writeScalar(array.get(i), builder);
            }

            builder.append(']');
            return;
        }

        builder.append('[');

        for (int i = 0; i < array.size(); i++) {
            builder.append(i == 0 ? "\n" : ",\n");
            indent(depth + 1, builder);
            writePretty(array.get(i), depth + 1, builder);
        }

        builder.append('\n');
        indent(depth, builder);
        builder.append(']');
    }

    private static boolean holdsOnlyScalars(ArpgArrayStruct array) {

        for (int i = 0; i < array.size(); i++)
            if (array.get(i).isObject() || array.get(i).isArray())
                return false;

        return true;
    }

    private static void writeCompact(ArpgElementStruct element, StringBuilder builder) {

        if (element.isObject()) {

            ArpgObjectStruct object = element.getAsObject();
            builder.append('{');
            boolean first = true;

            for (String key : object.keySet()) {

                if (!first)
                    builder.append(',');

                writeString(key, builder);
                builder.append(':');
                writeCompact(object.get(key), builder);
                first = false;
            }

            builder.append('}');
            return;
        }

        if (element.isArray()) {

            ArpgArrayStruct array = element.getAsArray();
            builder.append('[');

            for (int i = 0; i < array.size(); i++) {

                if (i > 0)
                    builder.append(',');

                writeCompact(array.get(i), builder);
            }

            builder.append(']');
            return;
        }

        writeScalar(element, builder);
    }

    private static void writeScalar(ArpgElementStruct element, StringBuilder builder) {

        if (element.isNull()) {
            builder.append(EngineSetting.ARPG_TEXT_NULL_LITERAL);
            return;
        }

        ArpgValueStruct value = element.getAsValue();

        if (value.isString())
            writeString(value.getAsString(), builder);
        else
            builder.append(value.getAsString());
    }

    private static void writeString(String string, StringBuilder builder) {

        builder.append('"');

        for (int i = 0; i < string.length(); i++) {

            char character = string.charAt(i);

            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {

                    if (character < EngineSetting.ARPG_TEXT_ESCAPE_LIMIT)
                        writeUnicodeEscape(character, builder);
                    else
                        builder.append(character);
                }
            }
        }

        builder.append('"');
    }

    private static void writeUnicodeEscape(char character, StringBuilder builder) {

        String hex = Integer.toHexString(character);
        builder.append("\\u");

        for (int i = hex.length(); i < EngineSetting.ARPG_TEXT_UNICODE_DIGITS; i++)
            builder.append('0');

        builder.append(hex);
    }

    private static void indent(int depth, StringBuilder builder) {
        for (int i = 0; i < depth; i++)
            builder.append(EngineSetting.ARPG_TEXT_INDENT);
    }

    private static final class ParseCursorStruct extends StructPackage {

        /*
         * Read position over one text being parsed. Tracks where it is so
         * every error names the line and column that caused it.
         */

        // Text
        private final String text;
        private int position;

        // Constructor \\

        ParseCursorStruct(String text) {
            this.text = text;
            this.position = 0;
        }

        // Read \\

        boolean hasRemaining() {
            return position < text.length();
        }

        char peek() {
            return text.charAt(position);
        }

        char next() {
            return text.charAt(position++);
        }

        boolean consume(char expected) {

            if (!hasRemaining() || text.charAt(position) != expected)
                return false;

            position++;
            return true;
        }

        boolean consume(String expected) {

            if (!text.startsWith(expected, position))
                return false;

            position += expected.length();
            return true;
        }

        void expect(char expected) {
            if (!consume(expected))
                throw error("Expected '" + expected + "'");
        }

        void skipWhitespace() {

            while (hasRemaining()) {

                char current = text.charAt(position);

                if (current != ' ' && current != '\t' && current != '\n' && current != '\r')
                    return;

                position++;
            }
        }

        // Accessible \\

        int getPosition() {
            return position;
        }

        String substring(int start) {
            return text.substring(start, position);
        }

        // Error \\

        InternalException error(String message) {

            int line = 1;
            int column = 1;

            for (int i = 0; i < position && i < text.length(); i++) {

                if (text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else
                    column++;
            }

            return new InternalException("ARPG text is malformed at line " + line + ", column " + column + ": "
                    + message);
        }
    }
}
