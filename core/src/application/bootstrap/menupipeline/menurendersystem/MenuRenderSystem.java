package application.bootstrap.menupipeline.menurendersystem;

import application.bootstrap.menupipeline.element.ElementData;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.element.ElementType;
import application.bootstrap.menupipeline.font.FontInstance;
import application.bootstrap.menupipeline.fontrendersystem.FontRenderSystem;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.util.LayoutStruct;
import application.bootstrap.menupipeline.util.StackDirection;
import application.bootstrap.menupipeline.util.TextAlign;
import application.bootstrap.renderpipeline.fbo.FboInstance;
import application.bootstrap.renderpipeline.fbo.FboManager;
import application.bootstrap.renderpipeline.fborendermanager.FboRenderManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
import application.bootstrap.renderpipeline.util.MaskStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MenuRenderSystem extends SystemPackage {

    private RenderManager renderManager;
    private FontRenderSystem fontRenderSystem;
    private FboManager fboManager;
    private FboRenderManager fboRenderManager;

    private MaskStruct[] maskPool;
    private int maskDepth;

    private WindowInstance currentWindow;
    private FboInstance uiFbo;

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
        this.fboManager = get(FboManager.class);
        this.fboRenderManager = get(FboRenderManager.class);
    }

    @Override
    protected void awake() {
        this.uiFbo = fboManager.getFbo(EngineSetting.FBO_UI);
    }

    public void renderMenu(MenuInstance instance) {

        if (!instance.isVisible())
            return;

        currentWindow = instance.getWindow();

        float screenW = currentWindow.getWidth();
        float screenH = currentWindow.getHeight();

        if (screenW == 0 || screenH == 0)
            return;

        ObjectArrayList<ElementInstance> elements = instance.getElements();

        for (int i = 0; i < elements.size(); i++)
            renderElement(elements.get(i), 0f, 0f, screenW, screenH);

        fboRenderManager.pushFbo(uiFbo);
    }

    private void renderElement(
            ElementInstance element,
            float parentLeft, float parentTop,
            float parentW, float parentH) {

        if (element.getElementData().getType() == ElementType.TOOLBAR)
            element.computeToolbarTransform(currentWindow.getWidth(), currentWindow.getHeight());
        else
            element.computeTransform(parentLeft, parentTop, parentW, parentH);

        renderElementContent(element);
    }

    private void renderStackedElement(
            ElementInstance element,
            float left, float top,
            float parentW, float parentH) {
        element.computeStackedTransform(left, top, parentW, parentH);
        renderElementContent(element);
    }

    private void renderElementContent(ElementInstance element) {

        ElementData data = element.getElementData();
        ElementType type = data.getType();

        if (element.hasSprite())
            pushSpriteRenderCall(element);

        if (element.hasFont())
            pushFontRenderCall(element);

        if (!element.hasChildren())
            return;

        if (type == ElementType.EXPANDABLE_CONTAINER) {
            if (element.isExpanded())
                renderDropdown(element);
            return;
        }

        if (data.isMask())
            pushMask(element);

        StackDirection stack = type == ElementType.TOOLBAR
                ? StackDirection.HORIZONTAL
                : data.getStackDirection();

        if (stack != StackDirection.NONE)
            renderStacked(element, stack);
        else {
            ObjectArrayList<ElementInstance> children = element.getChildren();
            for (int i = 0; i < children.size(); i++)
                renderElement(
                        children.get(i),
                        element.getComputedLeft(), element.getComputedTop(),
                        element.getComputedW(), element.getComputedH());
        }

        if (data.isMask())
            popMask();
    }

    private void renderDropdown(ElementInstance expandable) {

        float left = expandable.getComputedLeft();
        float parentW = expandable.getComputedW();
        float parentH = expandable.getComputedH();
        float cursor = expandable.getComputedTop();
        float totalH = 0f;

        ObjectArrayList<ElementInstance> children = expandable.getChildren();

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);
            LayoutStruct layout = child.getLayoutOverride() != null
                    ? child.getLayoutOverride()
                    : child.getElementData().getLayout();

            float childH = layout.getSize().getY().resolve(parentH);

            if (layout.hasMinSize())
                childH = Math.max(childH, layout.getMinSize().getY().resolve(parentH));

            if (layout.hasMaxSize())
                childH = Math.min(childH, layout.getMaxSize().getY().resolve(parentH));

            cursor -= childH;
            totalH += childH;
            renderStackedElement(child, left, cursor, parentW, parentH);
        }

        expandable.setContentH(totalH);
    }

    private void renderStacked(ElementInstance parent, StackDirection dir) {

        boolean vertical = dir == StackDirection.VERTICAL;
        float parentW = parent.getComputedW();
        float parentH = parent.getComputedH();

        ElementData parentData = parent.getElementData();

        float spacing = parentData.getSpacing() != null
                ? parentData.getSpacing().resolve(vertical ? parentH : parentW)
                : 0f;

        float cursor = vertical
                ? parent.getComputedTop() + parent.getComputedH() - parent.getScrollY()
                : parent.getComputedLeft() + parent.getScrollX();

        float contentSize = 0f;
        ObjectArrayList<ElementInstance> children = parent.getChildren();

        for (int i = 0; i < children.size(); i++) {

            ElementInstance child = children.get(i);

            if (vertical) {

                LayoutStruct layout = child.getLayoutOverride() != null
                        ? child.getLayoutOverride()
                        : child.getElementData().getLayout();
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

    private void pushSpriteRenderCall(ElementInstance element) {
        element.getSpriteInstance()
                .getModelInstance()
                .getMaterial()
                .setUniform("u_transform", element.getTransform());
        renderManager.pushRenderCall(
                element.getSpriteInstance().getModelInstance(), uiFbo, 0, currentMask(), currentWindow);
    }

    private void pushFontRenderCall(ElementInstance element) {

        FontInstance font = element.getFontInstance();
        ElementData data = element.getElementData();

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

        fontRenderSystem.submit(font, x, y, scale, currentMask(), currentWindow);
    }

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

    public void releaseFontModels(ObjectArrayList<ElementInstance> elements) {

        for (int i = 0; i < elements.size(); i++) {

            ElementInstance el = elements.get(i);

            if (el.hasFont())
                fontRenderSystem.release(el.getFontInstance());

            if (el.hasChildren())
                releaseFontModels(el.getChildren());
        }
    }
}
