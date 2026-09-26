package application.bootstrap.menupipeline.util;

import com.google.gson.JsonObject;

import engine.root.StructPackage;

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
            JsonObject json,
            String key,
            String defaultX,
            String defaultY) {

        if (!json.has(key))
            return new DimensionVector2Struct(
                    DimensionValueStruct.parse(defaultX),
                    DimensionValueStruct.parse(defaultY));

        JsonObject obj = json.getAsJsonObject(key);

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