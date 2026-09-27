package application.bootstrap.menupipeline.util;

import engine.root.StructPackage;
import engine.util.arpg.ArpgObjectStruct;

public class DimensionVector2Struct extends StructPackage {

    /*
     * A pair of DimensionValues for a 2D layout field. Used in layout resolution
     * hot paths via getX() and getY().
     */

    // Internal
    private final DimensionValueStruct x;
    private final DimensionValueStruct y;

    // Constructor \\

    public DimensionVector2Struct(DimensionValueStruct x, DimensionValueStruct y) {
        this.x = x;
        this.y = y;
    }

    // Factory \\

    public static DimensionVector2Struct parse(
            ArpgObjectStruct arpg,
            String key,
            String defaultX,
            String defaultY) {

        if (!arpg.has(key))
            return new DimensionVector2Struct(
                    DimensionValueStruct.parse(defaultX),
                    DimensionValueStruct.parse(defaultY));

        ArpgObjectStruct obj = arpg.getAsObject(key);

        DimensionValueStruct x = obj.has("x")
                ? DimensionValueStruct.parse(obj.get("x").getAsString())
                : DimensionValueStruct.parse(defaultX);

        DimensionValueStruct y = obj.has("y")
                ? DimensionValueStruct.parse(obj.get("y").getAsString())
                : DimensionValueStruct.parse(defaultY);

        return new DimensionVector2Struct(x, y);
    }

    // Accessible \\

    public DimensionValueStruct getX() {
        return x;
    }

    public DimensionValueStruct getY() {
        return y;
    }
}