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
     */

    private RenderManager renderManager;
    private FontRenderSystem fontRenderSystem;
    private FboRenderSystem fboRenderSystem;

    private MaskStruct[] maskPool;
    private int maskDepth;

    private MenuInstance currentMenu;
    private WindowInstance currentWindow;
    private FboInstance targetFbo;

    @Override
    protected void create() {
        this.maskPool = new MaskStruct[EngineSetting.MAX_MASK_DEPTH];
        for (int i = 0; i < maskPool.length; i++)
            maskPool[i] = new MaskStruct();
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

        SpriteInstance sprite = resolveSprite(element, activeState);
        if (sprite != null)
            pushSpriteRenderCall(element, sprite);

        if (element.hasFont())
            pushFontRenderCall(element, activeState);

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

    private void pushSpriteRenderCall(ElementInstance element, SpriteInstance sprite) {
        sprite.getModelInstance().getMaterial().setUniform("u_transform", element.getTransform());
        renderManager.pushRenderCall(sprite.getModelInstance(),
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

        TextAlign align = data.getTextAlign();
        float x;
        if (align == TextAlign.LEFT)
            x = element.getComputedLeft();
        else if (align == TextAlign.RIGHT)
            x = element.getComputedLeft() + element.getComputedW() - scaledW;
        else
            x = element.getComputedLeft() + (element.getComputedW() - scaledW) * 0.5f;

        float y = element.getComputedTop() + (element.getComputedH() - scaledH) * 0.5f;

        fontRenderSystem.submit(font, x, y, scale, currentMask(), targetFbo, currentWindow);
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