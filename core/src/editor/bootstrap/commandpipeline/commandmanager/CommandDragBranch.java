package editor.bootstrap.commandpipeline.commandmanager;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.dev.DevContext;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandDragBranch extends BranchPackage {

    /*
     * Carries a command picked up in a command console — an item tile's give
     * command — across every window until the button is let go, read from the
     * window the press started in, the same one ElementHitSystem latched the
     * drag on. A ghost naming what is carried follows the cursor over
     * whichever OS window it is in. Letting go over a Dev window runs the
     * command in that window alone, with the cursor still where it was
     * dropped; letting go over the element it was picked up from runs it on
     * every Dev window, exactly as a click would; anywhere else drops it.
     */

    // Internal
    private WindowManager windowManager;
    private InputManager inputManager;
    private MenuManager menuManager;
    private CommandManager commandManager;

    // Drag
    private String draggedCommandLine;
    private String draggedLabel;
    private WindowInstance sourceWindow;
    private ElementInstance sourceElement;

    // Ghost
    private MenuInstance ghost;

    // Base \\

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
        this.inputManager = get(InputManager.class);
        this.menuManager = get(MenuManager.class);
        this.commandManager = get(CommandManager.class);
    }

    @Override
    protected void update() {

        if (draggedCommandLine == null)
            return;

        if (!windowManager.getWindows().contains(sourceWindow)) {
            clearState();
            return;
        }

        if (!inputManager.getRawInput(sourceWindow).isMouseDown(EditorSetting.COMMAND_DRAG_BUTTON)) {
            drop();
            return;
        }

        updateGhost();
    }

    // Entry Point \\

    void dragCommand(String commandLine, String label, WindowInstance window, ElementInstance element) {

        if (draggedCommandLine != null)
            return;

        this.draggedCommandLine = commandLine;
        this.draggedLabel = label;
        this.sourceWindow = window;
        this.sourceElement = element;
    }

    // Ghost \\

    private void updateGhost() {

        WindowInstance osWindow = resolveHoveredOsWindow();

        if (ghost != null && (osWindow == null || ghost.getWindow().getGLWindow() != osWindow))
            closeGhost();

        if (osWindow == null)
            return;

        if (ghost == null) {
            ghost = menuManager.openMenuWindow(EditorSetting.MENU_COMMAND_DRAG_GHOST, osWindow);
            ghost.getEntryPoint(EditorSetting.ENTRY_COMMAND_DRAG_GHOST_LABEL).setFontText(draggedLabel);
        }

        ghost.getWindow().place(
                inputManager.getGlobalMouseX(osWindow) + EditorSetting.COMMAND_DRAG_GHOST_OFFSET,
                inputManager.getGlobalMouseY(osWindow) - EditorSetting.COMMAND_DRAG_GHOST_OFFSET
                        - EditorSetting.COMMAND_DRAG_GHOST_H,
                EditorSetting.COMMAND_DRAG_GHOST_W,
                EditorSetting.COMMAND_DRAG_GHOST_H);
    }

    private void closeGhost() {
        menuManager.closeMenuWindow(ghost);
        ghost = null;
    }

    // Drop \\

    private void drop() {

        String commandLine = draggedCommandLine;
        WindowInstance target = resolveHoveredContentWindow();
        boolean overSource = target == sourceWindow && isOverSourceElement();

        clearState();

        if (target != null && target.getContext() instanceof DevContext devContext) {
            commandManager.executeCommand(commandLine, devContext);
            return;
        }

        if (overSource)
            commandManager.executeCommand(commandLine);
    }

    private void clearState() {

        if (ghost != null)
            closeGhost();

        this.draggedCommandLine = null;
        this.draggedLabel = null;
        this.sourceWindow = null;
        this.sourceElement = null;
    }

    // Targets \\

    // Hovered windows run highest first, so the first OS window is the one on top under the cursor
    private WindowInstance resolveHoveredOsWindow() {

        ObjectArrayList<WindowInstance> hoveredWindows = windowManager.getHoveredWindows();

        for (int i = 0; i < hoveredWindows.size(); i++)
            if (hoveredWindows.get(i).hasNativeHandle())
                return hoveredWindows.get(i);

        return null;
    }

    // The topmost hovered window a context runs in — a tab's content above its own chrome
    private WindowInstance resolveHoveredContentWindow() {

        ObjectArrayList<WindowInstance> hoveredWindows = windowManager.getHoveredWindows();

        for (int i = 0; i < hoveredWindows.size(); i++)
            if (hoveredWindows.get(i).hasContext())
                return hoveredWindows.get(i);

        return null;
    }

    private boolean isOverSourceElement() {

        float x = inputManager.getHoverMouseX(sourceWindow);
        float y = inputManager.getHoverMouseY(sourceWindow);

        return x >= sourceElement.getComputedLeft()
                && x <= sourceElement.getComputedLeft() + sourceElement.getComputedW()
                && y >= sourceElement.getComputedTop()
                && y <= sourceElement.getComputedTop() + sourceElement.getComputedH();
    }
}
