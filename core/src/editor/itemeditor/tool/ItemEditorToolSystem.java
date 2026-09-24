package editor.itemeditor.tool;

import application.bootstrap.geometrypipeline.subvoxel.SubVoxelHitStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.menupipeline.elementhitsystem.ElementHitSystem;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.itemeditorpipeline.itemeditormanager.ItemEditorManager;
import editor.itemeditor.ItemEditorSetting;
import editor.itemeditor.input.ItemEditorInputSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class ItemEditorToolSystem extends SystemPackage {

    /*
     * Casts the pointer into the active item each frame and applies the active
     * tool on click. Toolbar hovers, camera drags, and the click that focuses the
     * window never edit the item.
     */

    // Internal
    private ItemEditorManager itemEditorManager;
    private SubVoxelManager subVoxelManager;
    private ElementHitSystem elementHitSystem;
    private WindowManager windowManager;
    private ItemEditorInputSystem itemEditorInputSystem;

    // Pointer
    private SubVoxelHitStruct hit;
    private Vector3 rayOrigin;
    private Vector3 rayDirection;
    private boolean focusedLastFrame;

    // Base \\

    @Override
    protected void create() {

        // Pointer
        this.hit = new SubVoxelHitStruct();
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.itemEditorManager = get(ItemEditorManager.class);
        this.subVoxelManager = get(SubVoxelManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
        this.windowManager = get(WindowManager.class);
        this.itemEditorInputSystem = get(ItemEditorInputSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();
        boolean focused = windowManager.getFocusedWindow() == window;
        boolean acceptsClicks = focusedLastFrame;
        focusedLastFrame = focused;

        hit.clear();

        if (!focused || !isPointerOverViewport(window) || !itemEditorManager.hasActiveDocument())
            return;

        RawInputHandle rawInput = itemEditorInputSystem.getRawInputHandle();

        if (rawInput.isButtonHeld(ItemEditorSetting.BUTTON_ORBIT))
            return;

        castPointer(window, rawInput);

        if (acceptsClicks && rawInput.isButtonClicked(ItemEditorSetting.BUTTON_APPLY))
            itemEditorManager.applyTool(hit);
    }

    // Pointer \\

    private boolean isPointerOverViewport(WindowInstance window) {

        if (windowManager.getHoveredWindow() != window)
            return false;

        MenuInstance hoveredMenu = elementHitSystem.getHoveredMenu();
        return hoveredMenu == null || hoveredMenu.getWindow() != window;
    }

    private void castPointer(WindowInstance window, RawInputHandle rawInput) {

        float width = window.getWidth();
        float height = window.getHeight();

        if (width <= 0f || height <= 0f)
            return;

        float ndcX = rawInput.getMouseX() / width * 2f - 1f;
        float ndcY = rawInput.getMouseY() / height * 2f - 1f;

        window.getActiveCamera().getPickRay(ndcX, ndcY, rayOrigin, rayDirection);
        subVoxelManager.raycast(itemEditorManager.getActiveDocument().getModel(), rayOrigin, rayDirection, hit);
    }

    // Accessible \\

    public SubVoxelHitStruct getHit() {
        return hit;
    }
}
