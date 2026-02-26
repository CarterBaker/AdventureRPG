package com.internal.bootstrap.menupipeline.element;

import com.google.gson.JsonObject;

public class DimensionVector2 {

    public final DimensionValue x;
    public final DimensionValue y;

    public DimensionVector2(DimensionValue x, DimensionValue y) {
        this.x = x;
        this.y = y;
    }

    public static DimensionVector2 parse(JsonObject json, String key, String defaultX, String defaultY) {
        if (!json.has(key))
            return new DimensionVector2(DimensionValue.parse(defaultX), DimensionValue.parse(defaultY));
        JsonObject obj = json.getAsJsonObject(key);
        DimensionValue x = obj.has("x") ? DimensionValue.parse(obj.get("x").getAsString())
                : DimensionValue.parse(defaultX);
        DimensionValue y = obj.has("y") ? DimensionValue.parse(obj.get("y").getAsString())
                : DimensionValue.parse(defaultY);
        return new DimensionVector2(x, y);
    }
}