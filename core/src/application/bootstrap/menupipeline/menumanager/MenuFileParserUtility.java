package application.bootstrap.menupipeline.menumanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.bootstrap.menupipeline.element.ElementAnimationStruct;
import application.bootstrap.menupipeline.element.ElementKeyframeStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.MenuEase;
import application.bootstrap.menupipeline.util.ThemeColor;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MenuFileParserUtility extends EngineUtility {

    /*
     * Stateless JSON parsing for MenuBuilder: state blocks, click and drag
     * callbacks, literal or themed colors, and element animation timelines.
     */

    // Callbacks — method only \\

    static String[] parseOnClick(JsonObject json) {
        return parseCallback(json, "on_click");
    }

    static String[] parseOnDrag(JsonObject json) {
        return parseCallback(json, "on_drag");
    }

    private static String[] parseCallback(JsonObject json, String key) {

        if (!json.has(key))
            return null;

        JsonObject obj = json.getAsJsonObject(key);

        return new String[] {
                JsonUtility.validateString(obj, "class"),
                JsonUtility.validateString(obj, "method"),
                JsonUtility.getString(obj, "arg", null)
        };
    }

    // State Block Keys \\

    // Element Type \\

    static ElementType parseElementType(String type, String id) {
        return switch (type.toLowerCase()) {
            case "sprite" -> ElementType.SPRITE;
            case "texture" -> ElementType.TEXTURE;
            case "button" -> ElementType.BUTTON;
            case "label" -> ElementType.LABEL;
            case "container" -> ElementType.CONTAINER;
            case "toolbar" -> ElementType.TOOLBAR;
            case "canvas_area" -> ElementType.CANVAS_AREA;
            default -> {
                throwException("Unknown element type '" + type + "' on element '" + id + "'");
                yield null;
            }
        };
    }

    // Color \\

    static MenuColorStruct parseColor(JsonObject json) {
        return parseColor(json, "color");
    }

    static MenuColorStruct parseHoverColor(JsonObject json) {
        return parseColor(json, "hover_color");
    }

    static MenuColorStruct parseParentHoverColor(JsonObject json) {
        return parseColor(json, "parent_hover_color");
    }

    private static MenuColorStruct parseColor(JsonObject json, String key) {

        if (!json.has(key))
            return null;

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive())
            return new MenuColorStruct(
                    parseThemeColor(el.getAsString(), key),
                    EngineSetting.MENU_THEME_ALPHA_DEFAULT);

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            return new MenuColorStruct(
                    parseThemeColor(JsonUtility.validateString(obj, "theme"), key),
                    JsonUtility.getFloat(obj, "alpha", EngineSetting.MENU_THEME_ALPHA_DEFAULT));
        }

        JsonArray arr = el.getAsJsonArray();

        if (arr.size() != EngineSetting.COLOR_CHANNEL_COUNT)
            throwException("'" + key + "' must be exactly 4 floats [r, g, b, a], a theme name, "
                    + "or { \"theme\": name, \"alpha\": value }");

        return new MenuColorStruct(new Color(
                arr.get(0).getAsFloat(),
                arr.get(1).getAsFloat(),
                arr.get(2).getAsFloat(),
                arr.get(3).getAsFloat()));
    }

    private static ThemeColor parseThemeColor(String name, String key) {

        ThemeColor themeColor = ThemeColor.fromString(name);

        if (themeColor == null)
            throwException("Unknown theme color '" + name + "' in '" + key + "'");

        return themeColor;
    }

    // Animation \\

    static ElementAnimationStruct parseAnimation(JsonObject json) {

        if (!json.has("animation"))
            return null;

        JsonObject animationJson = json.getAsJsonObject("animation");
        JsonArray keyframeArray = JsonUtility.validateArray(animationJson, "keyframes");

        if (keyframeArray.isEmpty())
            throwException("'animation' must define at least one keyframe");

        ObjectArrayList<ElementKeyframeStruct> keyframes = new ObjectArrayList<>(keyframeArray.size());
        float previousTime = 0f;

        for (int i = 0; i < keyframeArray.size(); i++) {

            ElementKeyframeStruct keyframe = parseKeyframe(keyframeArray.get(i).getAsJsonObject());

            if (keyframe.getTime() < previousTime)
                throwException("Animation keyframes must ascend in time — "
                        + keyframe.getTime() + " follows " + previousTime);

            previousTime = keyframe.getTime();
            keyframes.add(keyframe);
        }

        return new ElementAnimationStruct(
                JsonUtility.getFloat(animationJson, "delay", 0f),
                JsonUtility.getBoolean(animationJson, "loop", false),
                JsonUtility.getFloat(animationJson, "repeat_delay", 0f),
                keyframes);
    }

    private static ElementKeyframeStruct parseKeyframe(JsonObject json) {

        JsonObject offset = json.has("offset") ? json.getAsJsonObject("offset") : new JsonObject();
        float scaleX = 1f;
        float scaleY = 1f;

        if (json.has("scale")) {

            JsonElement scale = json.get("scale");

            if (scale.isJsonPrimitive()) {
                scaleX = scale.getAsFloat();
                scaleY = scaleX;
            } else {
                JsonObject scaleJson = scale.getAsJsonObject();
                scaleX = JsonUtility.getFloat(scaleJson, "x", 1f);
                scaleY = JsonUtility.getFloat(scaleJson, "y", 1f);
            }
        }

        return new ElementKeyframeStruct(
                JsonUtility.validateFloat(json, "time"),
                json.has("ease") ? parseEase(json.get("ease").getAsString()) : MenuEase.LINEAR,
                JsonUtility.getFloat(offset, "x", 0f),
                JsonUtility.getFloat(offset, "y", 0f),
                scaleX,
                scaleY,
                JsonUtility.getFloat(json, "rotation", 0f),
                JsonUtility.getFloat(json, "alpha", 1f));
    }

    private static MenuEase parseEase(String name) {

        MenuEase ease = MenuEase.fromString(name);

        if (ease == null)
            throwException("Unknown ease '" + name + "' in animation keyframe");

        return ease;
    }

    // Layout — full parse, absent fields get defaults \\

    static LayoutStruct parseLayout(JsonObject json) {
        return new LayoutStruct(
                parseOriginField(json, "anchor"),
                parseOriginField(json, "pivot"),
                DimensionVector2Struct.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION),
                DimensionVector2Struct.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE),
                json.has("min_size") ? DimensionVector2Struct.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2Struct.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null,
                parseAspect(json));
    }

    // Layout Override — absent fields null, preserved from template \\

    static LayoutStruct parseLayoutOverride(JsonObject json) {

        boolean hasAny = json.has("anchor") || json.has("pivot") || json.has("position")
                || json.has("size") || json.has("min_size") || json.has("max_size")
                || json.has("aspect");

        if (!hasAny)
            return null;

        return new LayoutStruct(
                json.has("anchor") ? parseOriginField(json, "anchor") : null,
                json.has("pivot") ? parseOriginField(json, "pivot") : null,
                json.has("position") ? DimensionVector2Struct.parse(json, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION) : null,
                json.has("size") ? DimensionVector2Struct.parse(json, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE) : null,
                json.has("min_size") ? DimensionVector2Struct.parse(json, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                json.has("max_size") ? DimensionVector2Struct.parse(json, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null,
                parseAspect(json));
    }

    // Aspect — width over height, zero when free \\

    private static float parseAspect(JsonObject json) {

        float aspect = JsonUtility.getFloat(json, "aspect", 0f);

        if (aspect < 0f)
            throwException("'aspect' must be a positive width-over-height ratio, got " + aspect);

        return aspect;
    }

    // Origin Field \\

    static Vector2 parseOriginName(String name) {

        String normalized = name.toLowerCase();
        float x = 0.5f;
        float y = 0.5f;

        if (normalized.contains("left"))
            x = 0f;
        else if (normalized.contains("right"))
            x = 1f;

        if (normalized.contains("top"))
            y = 1f;
        else if (normalized.contains("bottom"))
            y = 0f;

        return new Vector2(x, y);
    }

    static Vector2 parseOriginField(JsonObject json, String key) {

        if (!json.has(key))
            return new Vector2(0.5f, 0.5f);

        JsonElement el = json.get(key);

        if (el.isJsonPrimitive())
            return parseOriginName(el.getAsString());

        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            return new Vector2(
                    JsonUtility.getFloat(obj, "x", 0.5f),
                    JsonUtility.getFloat(obj, "y", 0.5f));
        }

        return new Vector2(0.5f, 0.5f);
    }
}