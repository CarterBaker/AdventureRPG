package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.util.DimensionValue;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import engine.root.DataPackage;

public class ElementData extends DataPackage {

    /*
     * Persistent element definition. Holds all immutable visual and layout
     * fields shared across every instance of this element. Owned by ElementHandle,
     * created with new during bootstrap.
     */

    // Identity
    private final String id;
    private final ElementType type;

    // Visuals
    private final String spriteName;
    private final String text;
    private final String fontName;
    private final float fontSize;
    private final float[] color;

    // Layout
    private final LayoutStruct layout;
    private final boolean mask;
    private final StackDirection stackDirection;
    private final DimensionValue spacing;
    private final TextAlign textAlign;

    // Constructor \\

    public ElementData(
            String id,
            ElementType type,
            String spriteName,
            String text,
            String fontName,
            float fontSize,
            float[] color,
            LayoutStruct layout,
            boolean mask,
            StackDirection stackDirection,
            DimensionValue spacing,
            TextAlign textAlign) {

        // Identity
        this.id = id;
        this.type = type;

        // Visuals
        this.spriteName = spriteName;
        this.text = text;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.color = color;

        // Layout
        this.layout = layout;
        this.mask = mask;
        this.stackDirection = stackDirection;
        this.spacing = spacing;
        this.textAlign = textAlign;
    }

    // Accessible \\

    public String getId() {
        return id;
    }

    public ElementType getType() {
        return type;
    }

    public String getSpriteName() {
        return spriteName;
    }

    public String getText() {
        return text;
    }

    public String getFontName() {
        return fontName;
    }

    public float getFontSize() {
        return fontSize;
    }

    public float[] getColor() {
        return color;
    }

    public LayoutStruct getLayout() {
        return layout;
    }

    public boolean isMask() {
        return mask;
    }

    public StackDirection getStackDirection() {
        return stackDirection;
    }

    public DimensionValue getSpacing() {
        return spacing;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    public boolean hasSprite() {
        return spriteName != null;
    }

    public boolean hasText() {
        return text != null;
    }

    public boolean hasFont() {
        return fontName != null;
    }

    public boolean hasColor() {
        return color != null;
    }
}
