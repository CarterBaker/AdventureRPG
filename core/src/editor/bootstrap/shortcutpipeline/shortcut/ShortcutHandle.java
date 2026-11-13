package editor.bootstrap.shortcutpipeline.shortcut;

import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import engine.input.Input;
import engine.root.ContextPackage;
import engine.root.EngineSetting;
import engine.root.HandlePackage;

public class ShortcutHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded editor shortcut. Registered and
     * owned by ShortcutManager. Delegates all accessors through ShortcutData,
     * which a rebind replaces in place, so every reference keeps up.
     * A shortcut with an action is pressed on the frame its key goes down
     * with exactly its modifiers held, so Ctrl+S never also runs S; one
     * without is held for as long as its key and at least its modifiers are.
     * Its hint shows on the canvases and with the tools its masks name.
     */

    // Internal
    private ShortcutData shortcutData;

    // Constructor \\

    public void constructor(ShortcutData shortcutData) {

        // Internal
        this.shortcutData = shortcutData;
    }

    // Input \\

    public boolean isPressed(Input input, int heldModifiers) {
        return heldModifiers == shortcutData.getModifiers() && input.isKeyClicked(shortcutData.getKey());
    }

    public boolean isHeld(Input input, int heldModifiers) {

        int modifiers = shortcutData.getModifiers();

        return (heldModifiers & modifiers) == modifiers && (!hasKey() || input.isKeyDown(shortcutData.getKey()));
    }

    // Hint \\

    // True when the hint belongs on the bar for a window working on this canvas with this tool, either null for none
    public boolean isHintShown(ToolCanvas canvas, EditorTool tool) {

        if (shortcutData.getHint() == null)
            return false;

        int canvasOrdinal = canvas != null ? canvas.ordinal() : EngineSetting.INDEX_NOT_FOUND;
        int toolOrdinal = tool != null ? tool.ordinal() : EngineSetting.INDEX_NOT_FOUND;

        return matchesMask(shortcutData.getCanvasMask(), canvasOrdinal)
                && matchesMask(shortcutData.getToolMask(), toolOrdinal);
    }

    private boolean matchesMask(int mask, int ordinal) {
        return mask == 0 || ordinal != EngineSetting.INDEX_NOT_FOUND && (mask & 1 << ordinal) != 0;
    }

    // Accessible \\

    public ShortcutData getShortcutData() {
        return shortcutData;
    }

    public String getShortcutName() {
        return shortcutData.getShortcutName();
    }

    public int getShortcutID() {
        return shortcutData.getShortcutID();
    }

    public Class<? extends ContextPackage> getContextClass() {
        return shortcutData.getContextClass();
    }

    public int getKey() {
        return shortcutData.getKey();
    }

    public int getModifiers() {
        return shortcutData.getModifiers();
    }

    public String getActionClass() {
        return shortcutData.getActionClass();
    }

    public String getActionMethod() {
        return shortcutData.getActionMethod();
    }

    public String getActionArg() {
        return shortcutData.getActionArg();
    }

    public boolean hasKey() {
        return shortcutData.getKey() != EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean hasAction() {
        return shortcutData.getActionClass() != null;
    }

    // True for a shortcut that runs only while its window claims its keys
    public boolean isClaimed() {
        return shortcutData.isClaimed();
    }

    public String getHint() {
        return shortcutData.getHint();
    }
}
