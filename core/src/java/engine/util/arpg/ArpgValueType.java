package engine.util.arpg;

enum ArpgValueType {

    /*
     * The kind of scalar an ArpgValueStruct holds. Whole numbers stay INTEGER
     * so they round trip exactly; anything with a fraction or exponent is a
     * DECIMAL; BYTES is a raw block such as baked texture data.
     */

    STRING,
    BOOLEAN,
    INTEGER,
    DECIMAL,
    BYTES
}
