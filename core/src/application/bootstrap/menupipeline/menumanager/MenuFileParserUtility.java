package application.bootstrap.menupipeline.menumanager;

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
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MenuFileParserUtility extends EngineUtility {

    /*
     * Stateless ARPG parsing for MenuBuilder: state blocks, click and drag
     * callbacks, literal or themed colors, and element animation timelines.
     */

    // Callbacks — method only \\

    static String[] parseOnClick(ArpgObjectStruct arpg) {
        return parseCallback(arpg, "on_click");
    }

    static String[] parseOnDrag(ArpgObjectStruct arpg) {
        return parseCallback(arpg, "on_drag");
    }

    private static String[] parseCallback(ArpgObjectStruct arpg, String key) {

        if (!arpg.has(key))
            return null;

        ArpgObjectStruct obj = arpg.getAsObject(key);

        return new String[] {
                ArpgUtility.validateString(obj, "class"),
                ArpgUtility.validateString(obj, "method"),
                ArpgUtility.getString(obj, "arg", null)
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

    static MenuColorStruct parseColor(ArpgObjectStruct arpg) {
        return parseColor(arpg, "color");
    }

    static MenuColorStruct parseHoverColor(ArpgObjectStruct arpg) {
        return parseColor(arpg, "hover_color");
    }

    static MenuColorStruct parseParentHoverColor(ArpgObjectStruct arpg) {
        return parseColor(arpg, "parent_hover_color");
    }

    private static MenuColorStruct parseColor(ArpgObjectStruct arpg, String key) {

        if (!arpg.has(key))
            return null;

        ArpgElementStruct el = arpg.get(key);

        if (el.isValue())
            return new MenuColorStruct(
                    parseThemeColor(el.getAsString(), key),
                    EngineSetting.MENU_THEME_ALPHA_DEFAULT);

        if (el.isObject()) {
            ArpgObjectStruct obj = el.getAsObject();
            return new MenuColorStruct(
                    parseThemeColor(ArpgUtility.validateString(obj, "theme"), key),
                    ArpgUtility.getFloat(obj, "alpha", EngineSetting.MENU_THEME_ALPHA_DEFAULT));
        }

        ArpgArrayStruct arr = el.getAsArray();

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

    static ElementAnimationStruct parseAnimation(ArpgObjectStruct arpg) {

        if (!arpg.has("animation"))
            return null;

        ArpgObjectStruct animationArpg = arpg.getAsObject("animation");
        ArpgArrayStruct keyframeArray = ArpgUtility.validateArray(animationArpg, "keyframes");

        if (keyframeArray.isEmpty())
            throwException("'animation' must define at least one keyframe");

        ObjectArrayList<ElementKeyframeStruct> keyframes = new ObjectArrayList<>(keyframeArray.size());
        float previousTime = 0f;

        for (int i = 0; i < keyframeArray.size(); i++) {

            ElementKeyframeStruct keyframe = parseKeyframe(keyframeArray.get(i).getAsObject());

            if (keyframe.getTime() < previousTime)
                throwException("Animation keyframes must ascend in time — "
                        + keyframe.getTime() + " follows " + previousTime);

            previousTime = keyframe.getTime();
            keyframes.add(keyframe);
        }

        return new ElementAnimationStruct(
                ArpgUtility.getFloat(animationArpg, "delay", 0f),
                ArpgUtility.getBoolean(animationArpg, "loop", false),
                ArpgUtility.getFloat(animationArpg, "repeat_delay", 0f),
                keyframes);
    }

    private static ElementKeyframeStruct parseKeyframe(ArpgObjectStruct arpg) {

        ArpgObjectStruct offset = arpg.has("offset") ? arpg.getAsObject("offset") : new ArpgObjectStruct();
        float scaleX = 1f;
        float scaleY = 1f;

        if (arpg.has("scale")) {

            ArpgElementStruct scale = arpg.get("scale");

            if (scale.isValue()) {
                scaleX = scale.getAsFloat();
                scaleY = scaleX;
            } else {
                ArpgObjectStruct scaleArpg = scale.getAsObject();
                scaleX = ArpgUtility.getFloat(scaleArpg, "x", 1f);
                scaleY = ArpgUtility.getFloat(scaleArpg, "y", 1f);
            }
        }

        return new ElementKeyframeStruct(
                ArpgUtility.validateFloat(arpg, "time"),
                arpg.has("ease") ? parseEase(arpg.get("ease").getAsString()) : MenuEase.LINEAR,
                ArpgUtility.getFloat(offset, "x", 0f),
                ArpgUtility.getFloat(offset, "y", 0f),
                scaleX,
                scaleY,
                ArpgUtility.getFloat(arpg, "rotation", 0f),
                ArpgUtility.getFloat(arpg, "alpha", 1f));
    }

    private static MenuEase parseEase(String name) {

        MenuEase ease = MenuEase.fromString(name);

        if (ease == null)
            throwException("Unknown ease '" + name + "' in animation keyframe");

        return ease;
    }

    // Layout — full parse, absent fields get defaults \\

    static LayoutStruct parseLayout(ArpgObjectStruct arpg) {
        return new LayoutStruct(
                parseOriginField(arpg, "anchor"),
                parseOriginField(arpg, "pivot"),
                DimensionVector2Struct.parse(arpg, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION),
                DimensionVector2Struct.parse(arpg, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE),
                arpg.has("min_size") ? DimensionVector2Struct.parse(arpg, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                arpg.has("max_size") ? DimensionVector2Struct.parse(arpg, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null,
                parseAspect(arpg));
    }

    // Layout Override — absent fields null, preserved from template \\

    static LayoutStruct parseLayoutOverride(ArpgObjectStruct arpg) {

        boolean hasAny = arpg.has("anchor") || arpg.has("pivot") || arpg.has("position")
                || arpg.has("size") || arpg.has("min_size") || arpg.has("max_size")
                || arpg.has("aspect");

        if (!hasAny)
            return null;

        return new LayoutStruct(
                arpg.has("anchor") ? parseOriginField(arpg, "anchor") : null,
                arpg.has("pivot") ? parseOriginField(arpg, "pivot") : null,
                arpg.has("position") ? DimensionVector2Struct.parse(arpg, "position",
                        EngineSetting.ELEMENT_DEFAULT_POSITION,
                        EngineSetting.ELEMENT_DEFAULT_POSITION) : null,
                arpg.has("size") ? DimensionVector2Struct.parse(arpg, "size",
                        EngineSetting.ELEMENT_DEFAULT_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_SIZE) : null,
                arpg.has("min_size") ? DimensionVector2Struct.parse(arpg, "min_size",
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MIN_SIZE) : null,
                arpg.has("max_size") ? DimensionVector2Struct.parse(arpg, "max_size",
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE,
                        EngineSetting.ELEMENT_DEFAULT_MAX_SIZE) : null,
                parseAspect(arpg));
    }

    // Aspect — width over height, zero when free \\

    private static float parseAspect(ArpgObjectStruct arpg) {

        float aspect = ArpgUtility.getFloat(arpg, "aspect", 0f);

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

    static Vector2 parseOriginField(ArpgObjectStruct arpg, String key) {

        if (!arpg.has(key))
            return new Vector2(0.5f, 0.5f);

        ArpgElementStruct el = arpg.get(key);

        if (el.isValue())
            return parseOriginName(el.getAsString());

        if (el.isObject()) {
            ArpgObjectStruct obj = el.getAsObject();
            return new Vector2(
                    ArpgUtility.getFloat(obj, "x", 0.5f),
                    ArpgUtility.getFloat(obj, "y", 0.5f));
        }

        return new Vector2(0.5f, 0.5f);
    }
}