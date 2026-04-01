package program.bootstrap.menupipeline.util;

import com.google.gson.JsonObject;
import program.core.engine.StructPackage;

public class DimensionVector2 extends StructPackage {

    /*
     * A pair of DimensionValues for a 2D layout field. Used in layout resolution
     * hot paths via getX() and getY().
     */

    // Internal
    private final DimensionValue x;
    private final DimensionValue y;

    // Constructor \\

    public DimensionVector2(DimensionValue x, DimensionValue y) {
        this.x = x;
        this.y = y;
    }

    // Factory \\

    public static DimensionVector2 parse(
            JsonObject json,
            String key,
            String defaultX,
            String defaultY) {

        if (!json.has(key))
            return new DimensionVector2(
                    DimensionValue.parse(defaultX),
                    DimensionValue.parse(defaultY));

        JsonObject obj = json.getAsJsonObject(key);

        DimensionValue x = obj.has("x")
                ? DimensionValue.parse(obj.get("x").getAsString())
                : DimensionValue.parse(defaultX);

        DimensionValue y = obj.has("y")
                ? DimensionValue.parse(obj.get("y").getAsString())
                : DimensionValue.parse(defaultY);

        return new DimensionVector2(x, y);
    }

    // Accessible \\

    public DimensionValue getX() {
        return x;
    }

    public DimensionValue getY() {
        return y;
    }
}