package application.bootstrap.menupipeline.menurendersystem;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.fontrendersystem.FontRenderSystem;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fborendersystem.FboRenderSystem;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.bootstrap.shaderpipeline.sprite.SpriteInstance;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuRenderSystem extends SystemPackage {

    /*
     * Renders all visible menu element trees each frame.
     *
     * resolveActiveState checks click state first, then activeHoverState.
     * activeHoverState is set by ElementHitSystem — whichever state is active
     * (enter, hover, exit) is treated identically here. The render system does
     * not distinguish between the three hover states — it just renders whatever
     * state is currently active.
     *
     * When the active state has a master (root-based overlay), the state root
     * instance is rendered as a full positioned element with its own layout,
     * visuals, and children. When the active state has inline children, those
     * replace the default child list. Both paths apply to all four states equally.
     *
     * resolveSprite picks the correct sprite instance for the active state:
     * hoverEnter, hover, hoverExit, or click.
     *
     * resolveColor picks one color per element for both its sprite tint and
     * its text: the active state's color, then hover_color while the element
     * is hovered or pointed, then the element's own color. Themed colors are
     * resolved against user Settings here every frame, so the interface
     * palette can change live. A fully transparent sprite is never drawn.
     *
     * Label text is fitted to its element: font_size is the size text grows
     * to, and it shrinks until it fits inside the element less its padding.
     * The text's full glyph box — descenders included — is centred
     * vertically, and horizontally unless the label aligns left or right.
     */

    private RenderManager renderManager;
    private FontRenderSystem fontRenderSystem;
    private FboRenderSystem fboRenderSystem;

    private MaskStruct[] maskPool;
    private int maskDepth;

    private MenuInstance currentMenu;
    private WindowInstance currentWindow;
    private FboInstance targetFbo;

    private Vector4 resolvedColor;

    @Override
    protected void create() {
        this.maskPool = new MaskStruct[EngineSetting.MAX_MASK_DEPTH];
        for (int i = 0; i < maskPool.length; i++)
            maskPool[i] = new MaskStruct();
        this.resolvedColor = new Vector4();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.fontRenderSystem = get(FontRenderSystem.class);
        this.fboRenderSystem = get(FboRenderSystem.class);
    }

    // Entry Point \\

    public void renderMenu(MenuInstance instance, FboInstance uiTargetFbo, int layer) {

        if (!instance.isVisible() || uiTargetFbo == null)
            return;

        currentMenu = instance;
        currentWindow = instance.getWindow();
        this.targetFbo = uiTargetFbo;

        float screenW = currentWindow.getWidth();
        float screenH = currentWindow.getHeight();

        if (screenW == 0 || screenH == 0)
            return;

        ObjectArrayList<ElementInstance> elements = instance.getElements();

        for (int i = 0; i < elements.size(); i++)
            renderElement(elements.get(i), 0f, 0f, screenW, screenH);

        fboRenderSystem.pushFbo(uiTargetFbo, layer, currentWindow);
    }

    // Element Rendering \\

    private void renderElement(
            ElementInstance element,
            float parentLeft, float parentTop,
            float parentW, float parentH) {

        ElementStateStruct activeState = resolveActiveState(element);
        ElementType type = element.getElementData().getType();

        if (type == ElementType.TOOLBAR) {
            element.computeToolbarTransform(currentWindow.getWidth(), currentWindow.getHeight());
        } else {
            LayoutStruct stateLayout = activeState != null ? activeState.getLayoutOverride() : null;
            element.computeTransform(parentLeft, parentTop, parentW, parentH, stateLayout);
        }

        if (type == ElementType.CANVAS_AREA) {
            currentMenu.getCanvas().set(
                    (int) element.getComputedLeft(), (int) element.getComputedTop(),
                    (int) element.getComputedW(), (int) element.getComputedH());
        }

        renderElementContent(element, activeState);
    }

    private void renderStackedElement(
            ElementInstance element,
            float left, float top,
            float parentW, float parentH) {

        ElementStateStruct activeState = resolveActiveState(element);
        LayoutStruct stateLayout = activeState != null ? activeState.getLayoutOverride() : null;
        element.computeStackedTransform(left, top, parentW, parentH, stateLayout);
        renderElementContent(element, activeState);
    }

    private void renderElementContent(ElementInstance element, ElementStateStruct activeState) {

        ElementData data = element.getElementData();
        ElementType type = data.getType();

        if (type == ElementType.CANVAS_AREA)
            return;

        MenuColorStruct color = resolveColor(element, activeState);

        SpriteInstance sprite = resolveSprite(element, activeState);
        if (sprite != null)
            pushSpriteRenderCall(element, sprite, color);

        if (element.hasFont())
            pushFontRenderCall(element, activeState, color);

        // Active hover state — root-based overlay takes priority over inline children
        if (activeState != null && !element.isClickExpanded()) {

            ElementInstance activeRoot = resolveActiveHoverRoot(element, activeState);

            if (activeRoot != null) {
                renderElement(activeRoot,
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
                // root renders its own children — skip default and inline
            } else {
                ObjectArrayList<ElementInstance> activeChildren = resolveActiveHoverChildren(element, activeState);

                renderChildren(element, data, activeChildren != null && !activeChildren.isEmpty()
                        ? activeChildren
                        : element.getChildren());
            }

        } else {
            renderChildren(element, data, element.getChildren());
        }

        if (element.isClickExpanded() && element.hasClickStateChildren())
            renderClickStateChildren(element);
    }

    private void renderChildren(
            ElementInstance parent,
            ElementData data,
            ObjectArrayList<ElementInstance> children) {

        if (children.isEmpty())
            return;

        if (data.isMask())
            pushMask(parent);

        ElementType type = data.getType();
        StackDirection stack = type == ElementType.TOOLBAR
                ? StackDirection.HORIZONTAL
                : data.getStackDirection();

        if (stack != StackDirection.NONE)
            renderStacked(parent, children, stack);
        else
            for (int i = 0; i < children.size(); i++)
                renderElement(children.get(i),
                        parent.getComputedLeft(), parent.getComputedTop(),
                        parent.getComputedW(), parent.getComputedH());

        if (data.isMask())
            popMask();
    }

    // Active State Helpers \\

    private ElementInstance resolveActiveHoverRoot(ElementInstance element,
            ElementStateStruct activeState) {
        if (!activeState.hasMaster())
            return null;
        ElementHandle handle = element.getHandle();
        if (activeState == handle.getHoverEnterState())
            return element.getHoverEnterStateRoot();
        if (activeState == handle.getHoverState())
            return element.getHoverStateRoot();
        if (activeState == handle.getHoverExitState())
            return element.getHoverExitStateRoot();
        return null;
    }

    private ObjectArrayList<ElementInstance> resolveActiveHoverChildren(ElementInstance element,
            ElementStateStruct activeState) {
        ElementHandle handle = element.getHandle();
        if (activeState == handle.getHoverEnterState())
            return element.getHoverEnterStateChildren();
        if (activeState == handle.getHoverState())
            return element.getHoverStateChildren();
        if (activeState == handle.getHoverExitState())
            return element.getHoverExitStateChildren();
        return null;
    }

    // State Resolution \\

    private ElementStateStruct resolveActiveState(ElementInstance element) {
        ElementHandle handle = element.getHandle();
        if (element.isClickExpanded() && handle.hasClickState())
            return handle.getClickState();
        if (element.hasActiveHoverState())
            return element.getActiveHoverState();
        return null;
    }

    private SpriteInstance resolveSprite(ElementInstance element, ElementStateStruct activeState) {

        if (activeState != null && activeState.hasSpriteOverride()) {
            ElementHandle handle = element.getHandle();
            SpriteInstance stateSprite = null;
            if (element.isClickExpanded())
                stateSprite = element.getClickSpriteInstance();
            else if (activeState == handle.getHoverEnterState())
                stateSprite = element.getHoverEnterSpriteInstance();
            else if (activeState == handle.getHoverState())
                stateSprite = element.getHoverSpriteInstance();
            else if (activeState == handle.getHoverExitState())
                stateSprite = element.getHoverExitSpriteInstance();
            if (stateSprite != null)
                return stateSprite;
        }

        return element.hasSprite() ? element.getSpriteInstance() : null;
    }

    private MenuColorStruct resolveColor(ElementInstance element, ElementStateStruct activeState) {

        if (activeState != null && activeState.hasColorOverride())
            return activeState.getColorOverride();

        ElementData data = element.getElementData();

        if (data.hasHoverColor() && (element.isHovered() || element.isPointed()))
            return data.getHoverColor();

        return element.getColor();
    }

    private LayoutStruct resolveLayout(ElementInstance element, ElementStateStruct activeState) {
        if (activeState != null && activeState.hasLayoutOverride())
            return activeState.getLayoutOverride();
        if (element.getLayoutOverride() != null)
            return element.getLayoutOverride();
        return element.getElementData().getLayout();
    }

    // Click State Children \\

    private void renderClickStateChildren(ElementInstance element) {

        float left = element.getComputedLeft();
        float parentW = element.getComputedW();
        float parentH = element.getComputedH();
        float cursor = element.getComputedTop();
        float totalH = 0f;

        ObjectArrayList<ElementInstance> children = element.getClickStateChildren();

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);
            LayoutStruct layout = resolveLayout(child, resolveActiveState(child));

            float childH = layout.getSize().getY().resolve(parentH);

            if (layout.hasMinSize())
                childH = Math.max(childH, layout.getMinSize().getY().resolve(parentH));
            if (layout.hasMaxSize())
                childH = Math.min(childH, layout.getMaxSize().getY().resolve(parentH));

            cursor -= childH;
            totalH += childH;
            renderStackedElement(child, left, cursor, parentW, parentH);
        }

        element.setClickStateContentH(totalH);
    }

    // Stacked Rendering \\

    private void renderStacked(
            ElementInstance parent,
            ObjectArrayList<ElementInstance> children,
            StackDirection dir) {

        boolean vertical = dir == StackDirection.VERTICAL;
        float parentW = parent.getComputedW();
        float parentH = parent.getComputedH();
        ElementData pData = parent.getElementData();

        float spacing = pData.getSpacing() != null
                ? pData.getSpacing().resolve(vertical ? parentH : parentW)
                : 0f;

        float cursor = vertical
                ? parent.getComputedTop() + parent.getComputedH() - parent.getScrollY()
                : parent.getComputedLeft() + parent.getScrollX();

        float contentSize = 0f;

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);
            LayoutStruct layout = resolveLayout(child, resolveActiveState(child));

            if (vertical) {

                float childH = layout.getSize().getY().resolve(parentH);

                if (layout.hasMinSize())
                    childH = Math.max(childH, layout.getMinSize().getY().resolve(parentH));
                if (layout.hasMaxSize())
                    childH = Math.min(childH, layout.getMaxSize().getY().resolve(parentH));

                cursor -= childH;
                renderStackedElement(child, parent.getComputedLeft(), cursor, parentW, parentH);
                contentSize += child.getComputedH() + (i < children.size() - 1 ? spacing : 0f);
                cursor -= spacing;

            } else {

                renderStackedElement(child, cursor, parent.getComputedTop(), parentW, parentH);
                float childW = child.getComputedW();
                contentSize += childW + (i < children.size() - 1 ? spacing : 0f);
                cursor += childW + spacing;
            }
        }

        if (vertical)
            parent.setContentH(contentSize);
        else
            parent.setContentW(contentSize);
    }

    // Render Calls \\

    private void pushSpriteRenderCall(ElementInstance element, SpriteInstance sprite, MenuColorStruct color) {

        Vector4 tint = resolveTint(color, EngineSetting.SPRITE_DEFAULT_COLOR);

        if (tint.w <= EngineSetting.COLOR_CHANNEL_MIN)
            return;

        sprite.setColor(tint);
        sprite.getModelInstance().getMaterial().setUniform("u_transform", element.getTransform());
        renderManager.pushRenderCall(sprite.getModelInstance(),
                targetFbo, 0, currentMask(), currentWindow);
    }

    private void pushFontRenderCall(
            ElementInstance element,
            ElementStateStruct activeState,
            MenuColorStruct color) {

        FontInstance font = element.getFontInstance();
        ElementData data = element.getElementData();

        Vector4 tint = resolveTint(color, EngineSetting.FONT_DEFAULT_COLOR);
        font.setColor(tint.x, tint.y, tint.z, tint.w);

        String text = activeState != null && activeState.getTextOverride() != null
                ? activeState.getTextOverride()
                : element.getText();

        if (text != null)
            font.setText(text);

        float rasterPixelSize = font.getHandle().getRasterPixelSize();
        float scale = resolveTextScale(element, font, data, rasterPixelSize);
        font.setFontSize(scale * rasterPixelSize);

        if (!font.hasGlyphs())
            return;

        float x = resolveTextX(element, data.getTextAlign(), font.getTextWidth() * scale);
        float y = element.getComputedTop()
                + (element.getComputedH() - font.getTextHeight() * scale) * 0.5f
                - font.getTextBottom() * scale;

        fontRenderSystem.submit(font, x, y, scale, currentMask(), targetFbo, currentWindow);
    }

    // Color \\

    private Vector4 resolveTint(MenuColorStruct color, Color fallback) {

        if (color != null)
            color.resolve(settings, resolvedColor);
        else
            resolvedColor.set(fallback.r, fallback.g, fallback.b, fallback.a);

        return resolvedColor;
    }

    // Text Fit \\

    private float resolveTextScale(
            ElementInstance element,
            FontInstance font,
            ElementData data,
            float rasterPixelSize) {

        if (rasterPixelSize <= 0f)
            return 1f;

        float targetFontSize = Math.max(1f, data.getFontSize().resolve(element.getComputedH()));
        float scale = targetFontSize / rasterPixelSize;

        float availableW = element.getComputedW() - EngineSetting.FONT_FIT_PADDING_X_PIXELS * 2f;
        float availableH = element.getComputedH() - EngineSetting.FONT_FIT_PADDING_Y_PIXELS * 2f;

        if (font.getTextWidth() > 0f && availableW > 0f)
            scale = Math.min(scale, availableW / font.getTextWidth());

        if (font.getTextHeight() > 0f && availableH > 0f)
            scale = Math.min(scale, availableH / font.getTextHeight());

        return scale;
    }

    private float resolveTextX(ElementInstance element, TextAlign align, float scaledW) {

        float left = element.getComputedLeft();
        float width = element.getComputedW();

        if (align == TextAlign.LEFT)
            return left + EngineSetting.FONT_FIT_PADDING_X_PIXELS;

        if (align == TextAlign.RIGHT)
            return left + width - EngineSetting.FONT_FIT_PADDING_X_PIXELS - scaledW;

        return left + (width - scaledW) * 0.5f;
    }

    // Mask \\

    private void pushMask(ElementInstance element) {

        int x = (int) element.getComputedLeft();
        int y = (int) element.getComputedTop();
        int w = (int) element.getComputedW();
        int h = (int) element.getComputedH();

        if (maskDepth > 0) {
            MaskStruct prev = maskPool[maskDepth - 1];
            int ix = Math.max(x, prev.getX());
            int iy = Math.max(y, prev.getY());
            int ix2 = Math.min(x + w, prev.getX() + prev.getW());
            int iy2 = Math.min(y + h, prev.getY() + prev.getH());
            x = ix;
            y = iy;
            w = Math.max(0, ix2 - ix);
            h = Math.max(0, iy2 - iy);
        }

        maskPool[maskDepth].set(x, y, w, h);
        maskDepth++;
    }

    private void popMask() {
        maskDepth--;
    }

    private MaskStruct currentMask() {
        return maskDepth == 0 ? null : maskPool[maskDepth - 1];
    }

    // Cleanup \\

    public void releaseFontModels(ObjectArrayList<ElementInstance> elements) {

        for (int i = 0; i < elements.size(); i++) {

            ElementInstance el = elements.get(i);

            if (el.hasFont())
                fontRenderSystem.release(el.getFontInstance());

            if (el.hasChildren())
                releaseFontModels(el.getChildren());
            if (el.hasHoverEnterStateChildren())
                releaseFontModels(el.getHoverEnterStateChildren());
            if (el.hasHoverStateChildren())
                releaseFontModels(el.getHoverStateChildren());
            if (el.hasHoverExitStateChildren())
                releaseFontModels(el.getHoverExitStateChildren());
            if (el.hasClickStateChildren())
                releaseFontModels(el.getClickStateChildren());
        }
    }
}