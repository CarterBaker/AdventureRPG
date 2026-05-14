package application.bootstrap.menupipeline.menurendersystem;

import application.bootstrap.menupipeline.canvassystem.CanvasAreaSystem;
import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementHandle;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementStateStruct;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.fontrendersystem.FontRenderSystem;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.LayoutStruct;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuRenderSystem extends SystemPackage {

    private RenderManager renderManager;
    private CanvasAreaSystem canvasAreaSystem;
    private FontRenderSystem fontRenderSystem;
    private FboRenderSystem fboRenderSystem;

    private MaskStruct[] maskPool;
    private int maskDepth;

    private MenuInstance currentMenu;
    private WindowInstance currentWindow;
    private FboInstance targetFbo;

    // Internal \\

    @Override
    protected void create() {
        this.maskPool = new MaskStruct[EngineSetting.MAX_MASK_DEPTH];
        for (int i = 0; i < maskPool.length; i++)
            maskPool[i] = new MaskStruct();
    }

    @Override
    protected void get() {
        this.renderManager = get(RenderManager.class);
        this.canvasAreaSystem = get(CanvasAreaSystem.class);
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
            int cx = (int) element.getComputedLeft();
            int cy = (int) element.getComputedTop();
            int cw = (int) element.getComputedW();
            int ch = (int) element.getComputedH();
            // computedTop is in layout space (Y+ down). Convert to OpenGL space (Y+ up).
            canvasAreaSystem.register(currentMenu, cx, (int) (currentWindow.getHeight() - cy - ch), cw, ch);
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

        SpriteInstance sprite = resolveSprite(element, activeState);
        if (sprite != null)
            pushSpriteRenderCall(element, sprite);

        if (element.hasFont())
            pushFontRenderCall(element, activeState);

        if (activeState != null
                && !element.isClickExpanded()
                && activeState.hasMaster()
                && element.hasHoverStateRoot()) {

            renderElement(
                    element.getHoverStateRoot(),
                    element.getComputedLeft(), element.getComputedTop(),
                    element.getComputedW(), element.getComputedH());

        } else {

            boolean useHoverChildren = activeState != null
                    && !element.isClickExpanded()
                    && element.hasHoverStateChildren();

            ObjectArrayList<ElementInstance> activeChildren = useHoverChildren
                    ? element.getHoverStateChildren()
                    : element.getChildren();

            if (!activeChildren.isEmpty()) {

                if (data.isMask())
                    pushMask(element);

                StackDirection stack = type == ElementType.TOOLBAR
                        ? StackDirection.HORIZONTAL
                        : data.getStackDirection();

                if (stack != StackDirection.NONE)
                    renderStacked(element, activeChildren, stack);
                else {
                    for (int i = 0; i < activeChildren.size(); i++)
                        renderElement(
                                activeChildren.get(i),
                                element.getComputedLeft(), element.getComputedTop(),
                                element.getComputedW(), element.getComputedH());
                }

                if (data.isMask())
                    popMask();
            }
        }

        if (element.isClickExpanded() && element.hasClickStateChildren())
            renderClickStateChildren(element);
    }

    // State Resolution \\

    private ElementStateStruct resolveActiveState(ElementInstance element) {
        ElementHandle handle = element.getHandle();
        if (element.isClickExpanded() && handle.hasClickState())
            return handle.getClickState();
        if (element.isHovered() && handle.hasHoverState())
            return handle.getHoverState();
        return null;
    }

    private SpriteInstance resolveSprite(ElementInstance element, ElementStateStruct activeState) {

        if (activeState != null && activeState.hasSpriteOverride()) {
            SpriteInstance stateSprite = element.isClickExpanded()
                    ? element.getClickSpriteInstance()
                    : element.getHoverSpriteInstance();
            if (stateSprite != null)
                return stateSprite;
        }

        return element.hasSprite() ? element.getSpriteInstance() : null;
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
                    childH = Math.max(childH, layout.getMinSize().getX().resolve(parentH));

                if (layout.hasMaxSize())
                    childH = Math.min(childH, layout.getMaxSize().getX().resolve(parentH));

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

    private void pushSpriteRenderCall(ElementInstance element, SpriteInstance sprite) {
        sprite.getModelInstance()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());
        renderManager.pushRenderCall(
                sprite.getModelInstance(),
                targetFbo, 0, currentMask(), currentWindow);
    }

    private void pushFontRenderCall(ElementInstance element, ElementStateStruct activeState) {

        FontInstance font = element.getFontInstance();
        ElementData data = element.getElementData();

        if (activeState != null && activeState.hasColorOverride()) {
            Color c = activeState.getColorOverride();
            font.setColor(c.r, c.g, c.b, c.a);
        } else if (data.hasColor()) {
            Color c = data.getColor();
            font.setColor(c.r, c.g, c.b, c.a);
        }

        String text = activeState != null && activeState.getTextOverride() != null
                ? activeState.getTextOverride()
                : element.getText();

        if (text != null)
            font.setText(text);

        float targetFontSize = Math.max(1f, data.getFontSize().resolve(element.getComputedH()));
        font.setFontSize(targetFontSize);

        if (!font.hasGlyphs())
            return;

        float rasterPixelSize = font.getHandle().getRasterPixelSize();
        float scale = rasterPixelSize > 0f ? targetFontSize / rasterPixelSize : 1f;
        float scaledW = font.getTextWidth() * scale;
        float scaledH = font.getTextHeight() * scale;

        float screenH = currentWindow.getHeight();

        TextAlign align = data.getTextAlign();
        float x;
        if (align == TextAlign.LEFT)
            x = element.getComputedLeft();
        else if (align == TextAlign.RIGHT)
            x = element.getComputedLeft() + element.getComputedW() - scaledW;
        else
            x = element.getComputedLeft() + (element.getComputedW() - scaledW) * 0.5f;

        float y = (screenH - element.getComputedTop() - element.getComputedH())
                + (element.getComputedH() - scaledH) * 0.5f;

        fontRenderSystem.submit(font, x, y, scale, currentMask(), targetFbo, currentWindow);
    }

    // Mask \\

    private void pushMask(ElementInstance element) {

        int x = (int) element.getComputedLeft();
        int y = (int) element.getComputedTop();
        int w = (int) element.getComputedW();
        int h = (int) element.getComputedH();

        // computedTop is in layout space (Y+ down). Convert to OpenGL space (Y+ up).
        int openglY = (int) (currentWindow.getHeight() - y - h);

        if (maskDepth > 0) {
            MaskStruct prev = maskPool[maskDepth - 1];
            int ix = Math.max(x, prev.getX());
            int iy = Math.max(openglY, prev.getY());
            int ix2 = Math.min(x + w, prev.getX() + prev.getW());
            int iy2 = Math.min(openglY + h, prev.getY() + prev.getH());
            x = ix;
            openglY = iy;
            w = Math.max(0, ix2 - ix);
            h = Math.max(0, iy2 - iy);
        }

        maskPool[maskDepth].set(x, openglY, w, h);
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

            if (el.hasHoverStateChildren())
                releaseFontModels(el.getHoverStateChildren());

            if (el.hasClickStateChildren())
                releaseFontModels(el.getClickStateChildren());
        }
    }
}