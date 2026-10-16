package application.runtime.shipwright;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipSurveyStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightStorage;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import application.runtime.menueventsmanager.util.NameInputUtility;
import engine.input.Buttons;
import engine.input.Input;
import engine.input.Keys;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ShipwrightSystem extends SystemPackage {

    /*
     * The drafting table in this window, the same in the game and the editor.
     * open() lays the table menu over the window, with its leave menu when the
     * table writes to the player's saves; the session it opens is kept, so a
     * table opened again shows the ship left on it. Each frame the sheet is
     * fitted into its area at whole square octants and the pointer read over
     * it: the left button draws with the tool in hand and the right erases,
     * a stroke at a time, and the wheel steps the layer or level. The rotate
     * binding turns what is set down next, and undo, redo and save work as
     * everywhere else; the pause binding walks away from a table in the game.
     * While the name is typed the keys type it, Enter keeps it and the pause
     * binding drops it. Whenever the session changes the
     * labels are rewritten.
     */

    // Internal
    private ShipwrightManager shipwrightManager;
    private MenuManager menuManager;
    private InputManager inputManager;
    private WindowManager windowManager;
    private ElementHitSystem elementHitSystem;

    // Session
    private ShipwrightSessionInstance session;

    // Menus
    private MenuInstance tableMenu;
    private MenuInstance leaveMenu;

    // Pointer
    private boolean focused;
    private boolean focusedLastFrame;

    // Sheet — the window rectangle the sheet's octants are drawn in, and the side of one octant in pixels
    private float sheetLeft;
    private float sheetBottom;
    private float sheetWidth;
    private float sheetHeight;
    private float cellSize;

    // Labels
    private int shownRevision;
    private StringBuilder text;

    // Base \\

    @Override
    protected void create() {

        // Labels
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.text = new StringBuilder();
    }

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.windowManager = get(WindowManager.class);
        this.elementHitSystem = get(ElementHitSystem.class);
    }

    // Open / Close \\

    public void open(ShipwrightStorage storage) {

        if (isOpen())
            return;

        WindowInstance window = context.getWindow();

        if (session == null || session.getStorage() != storage)
            this.session = shipwrightManager.openSession(storage);

        this.tableMenu = menuManager.openMenu(EngineSetting.SHIPWRIGHT_MENU_TABLE, window);

        if (storage == ShipwrightStorage.SAVES)
            this.leaveMenu = menuManager.openMenu(EngineSetting.SHIPWRIGHT_MENU_LEAVE, window);

        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.focused = false;
        this.focusedLastFrame = false;
    }

    public void close() {

        if (!isOpen())
            return;

        shipwrightManager.endStroke(session);

        if (session.isNaming())
            session.endNaming();

        this.tableMenu = menuManager.closeMenu(tableMenu);

        if (leaveMenu != null)
            this.leaveMenu = menuManager.closeMenu(leaveMenu);
    }

    // Update \\

    @Override
    protected void update() {

        if (!isOpen())
            return;

        WindowInstance window = context.getWindow();
        Input rawInput = inputManager.getRawInput(window);

        this.focusedLastFrame = focused;
        this.focused = windowManager.getFocusedWindow() == window;

        if (session.isNaming())
            updateNaming(window, rawInput);
        else if (updateBindings(window))
            return;

        shipwrightManager.updateSurvey(session, internal.getDeltaTime());
        shipwrightManager.resolveSheetSize(session);
        fitSheet();
        updatePointer(window, rawInput);
        refreshLabels();
    }

    // The table's bindings, true when the pause binding walked away from a table in the game
    private boolean updateBindings(WindowInstance window) {

        if (isClosable() && inputManager.bindingClicked(KeyBindings.PAUSE, window)) {
            close();
            return true;
        }

        if (inputManager.bindingClicked(KeyBindings.ROTATE_ITEM, window))
            shipwrightManager.turn(session);

        if (inputManager.bindingClicked(KeyBindings.UNDO, window))
            shipwrightManager.undo(session);

        if (inputManager.bindingClicked(KeyBindings.REDO, window))
            shipwrightManager.redo(session);

        if (inputManager.bindingClicked(KeyBindings.SAVE, window))
            shipwrightManager.saveShip(session);

        return false;
    }

    // Naming \\

    public void beginNaming() {

        if (isOpen() && !session.isNaming())
            session.beginNaming();
    }

    private void updateNaming(WindowInstance window, Input rawInput) {

        if (inputManager.bindingClicked(KeyBindings.PAUSE, window)) {
            session.endNaming();
            return;
        }

        if (rawInput.isKeyClicked(Keys.ENTER) || rawInput.isKeyClicked(Keys.NUMPAD_ENTER)) {
            shipwrightManager.confirmName(session, session.getNameBuffer().toString().trim());
            return;
        }

        if (NameInputUtility.typeName(rawInput, session.getNameBuffer(), EngineSetting.SHIP_NAME_MAX_LENGTH))
            session.markChanged();
    }

    // Pointer \\

    // The sheet's octants fitted into its area at the largest whole square that fits, centred
    private void fitSheet() {

        ElementInstance area = tableMenu.getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_SHEET);
        int columns = Math.max(1, session.getSheetWidth());
        int rows = Math.max(1, session.getSheetHeight());

        this.cellSize = Math.max(0f, Math.min(area.getComputedW() / columns, area.getComputedH() / rows));
        this.sheetWidth = columns * cellSize;
        this.sheetHeight = rows * cellSize;
        this.sheetLeft = area.getComputedLeft() + (area.getComputedW() - sheetWidth) * 0.5f;
        this.sheetBottom = area.getComputedTop() + (area.getComputedH() - sheetHeight) * 0.5f;
    }

    // Starts, carries and ends strokes with the pointer over the sheet, the wheel stepping the layer
    private void updatePointer(WindowInstance window, Input rawInput) {

        boolean drawing = rawInput.isMouseDown(Buttons.LEFT) || rawInput.isMouseDown(Buttons.RIGHT);

        if (session.isStroking() && !drawing) {
            shipwrightManager.endStroke(session);
            return;
        }

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);
        boolean over = isPointerFree(window) && cellSize > 0f
                && mouseX >= sheetLeft && mouseX < sheetLeft + sheetWidth
                && mouseY >= sheetBottom && mouseY < sheetBottom + sheetHeight;
        int u = over ? (int) ((mouseX - sheetLeft) / cellSize) : EngineSetting.INDEX_NOT_FOUND;
        int v = over ? (int) ((mouseY - sheetBottom) / cellSize) : EngineSetting.INDEX_NOT_FOUND;

        session.setCursor(over, u, v);

        if (!over)
            return;

        float wheel = rawInput.getScrollY();

        if (wheel != 0f)
            shipwrightManager.stepLayer(session, wheel > 0f ? 1 : -1);

        if (!session.isStroking() && focusedLastFrame) {

            if (rawInput.isMouseClicked(Buttons.LEFT))
                shipwrightManager.beginStroke(session, false);
            else if (rawInput.isMouseClicked(Buttons.RIGHT))
                shipwrightManager.beginStroke(session, true);
        }

        if (session.isStroking())
            shipwrightManager.strokeAt(session, u, v);
    }

    // True while this window is hovered and no menu element of its own lies under the pointer
    private boolean isPointerFree(WindowInstance window) {

        if (windowManager.getHoveredWindow() != window)
            return false;

        MenuInstance hoveredMenu = elementHitSystem.getHoveredMenu();

        return hoveredMenu == null || hoveredMenu.getWindow() != window;
    }

    // Labels \\

    private void refreshLabels() {

        if (session.getRevision() == shownRevision)
            return;

        this.shownRevision = session.getRevision();

        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_NAME, resolveName());
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_STATUS, session.getStatus());
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_TOOL, resolveToolHint());
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_LAYER, resolveLayer());
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_ITEM, resolveItem());
        writeSurvey();
    }

    private String resolveName() {

        text.setLength(0);

        if (session.isNaming())
            return text.append(session.getNameBuffer()).append(EngineSetting.SHIPWRIGHT_NAME_CARET).toString();

        String displayName = session.getBlueprint().getDisplayName();

        text.append(displayName.isEmpty() ? EngineSetting.SHIPWRIGHT_NAME_UNNAMED : displayName);

        if (session.isDirty())
            text.append(EngineSetting.SHIPWRIGHT_NAME_DIRTY);

        return text.toString();
    }

    private String resolveToolHint() {

        text.setLength(0);
        text.append(session.getTool().getTitle()).append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR);

        if (session.getTool() == ShipwrightTool.MASTS)
            return appendMast(text).toString();

        return text.append(EngineSetting.SHIPWRIGHT_HINTS[session.getTool().ordinal()]).toString();
    }

    // The mast picked: its height, the sails it carries and its fittings
    private StringBuilder appendMast(StringBuilder out) {

        if (!session.hasMastPicked())
            return out.append(EngineSetting.SHIPWRIGHT_HINT_MAST_NONE);

        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());
        IntArrayList sails = session.getLayout().getMastSailCounts();

        out.append(shipwrightManager.getMastName(session, session.getMastIndex()))
                .append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(mast.getHeight() * EngineSetting.SUB_BLOCK_SIZE)
                .append(EngineSetting.SHIPWRIGHT_TEXT_BLOCKS)
                .append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(session.getMastIndex() < sails.size() ? sails.getInt(session.getMastIndex()) : 0)
                .append(EngineSetting.SHIPWRIGHT_TEXT_SAILS);

        if (mast.hasCrowsNest())
            out.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR).append(EngineSetting.SHIPWRIGHT_TEXT_NEST);

        if (mast.isRigged())
            out.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR).append(EngineSetting.SHIPWRIGHT_TEXT_RIGGED);

        return out;
    }

    private String resolveLayer() {

        text.setLength(0);
        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipwrightTool tool = session.getTool();

        if (tool == ShipwrightTool.HULL || tool == ShipwrightTool.DECKS)
            return text.append(EngineSetting.SHIPWRIGHT_TEXT_LAYER).append(session.getLayer() + 1)
                    .append(EngineSetting.SHIPWRIGHT_TEXT_OF).append(blueprint.getHeight())
                    .append(session.getLayer() == blueprint.getWaterline() ? EngineSetting.SHIPWRIGHT_TEXT_WATERLINE
                            : EngineSetting.SHIPWRIGHT_STATUS_NONE)
                    .toString();

        if (tool == ShipwrightTool.MASTS)
            return text.append(blueprint.getMasts().size()).append(EngineSetting.SHIPWRIGHT_TEXT_MASTS).toString();

        int levels = session.getLayout().getLevels().size();

        if (levels == 0)
            return EngineSetting.SHIPWRIGHT_TEXT_NO_LEVEL;

        return text.append(EngineSetting.SHIPWRIGHT_TEXT_LEVEL)
                .append(Math.min(session.getLevelIndex(), levels - 1) + 1)
                .append(EngineSetting.SHIPWRIGHT_TEXT_OF).append(levels).toString();
    }

    private String resolveItem() {

        text.setLength(0);
        String furnishingName = shipwrightManager.getFurnishingName(session.getFurnishingIndex());

        if (furnishingName != null)
            text.append(furnishingName.substring(furnishingName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1));

        return text.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(EngineSetting.SHIPWRIGHT_FACINGS[session.getSpin()]).toString();
    }

    private void writeSurvey() {

        ShipSurveyStruct survey = session.getSurvey();

        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SPEED, String.format(EngineSetting.SHIPWRIGHT_FORMAT_SPEED,
                survey.getSpeed(), survey.getEntryAngleDegrees()));
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SIZE, String.format(EngineSetting.SHIPWRIGHT_FORMAT_SIZE,
                survey.getLength(), survey.getBeam(), survey.getDraft(), survey.getMass()));
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_TRIANGLES, String.format(EngineSetting.SHIPWRIGHT_FORMAT_TRIANGLES,
                survey.getTriangles(), EngineSetting.SHIP_TRIANGLE_LIMIT));
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SAILS, resolveSails());
    }

    // How many sails each mast carries, from the bow aft
    private String resolveSails() {

        text.setLength(0);
        IntArrayList sails = session.getLayout().getMastSailCounts();

        text.append(EngineSetting.SHIPWRIGHT_TEXT_SAILS_TITLE).append(session.getSurvey().getSailCount());

        for (int i = sails.size() - 1; i >= 0; i--)
            text.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                    .append(shipwrightManager.getMastName(session, i))
                    .append(EngineSetting.SHIPWRIGHT_TEXT_COUNT_SEPARATOR)
                    .append(sails.getInt(i));

        return text.toString();
    }

    private void setLabel(int entryPoint, String value) {

        ElementInstance label = tableMenu.getEntryPoint(entryPoint);

        if (label != null)
            label.setFontText(value);
    }

    // Accessible \\

    public boolean isOpen() {
        return tableMenu != null;
    }

    // True for a table in the game, which the player walks away from; the editor's stays as its tab
    public boolean isClosable() {
        return isOpen() && session.getStorage() == ShipwrightStorage.SAVES;
    }

    public ShipwrightSessionInstance getSession() {
        return session;
    }

    public MenuInstance getTableMenu() {
        return tableMenu;
    }

    public float getSheetLeft() {
        return sheetLeft;
    }

    public float getSheetBottom() {
        return sheetBottom;
    }

    public float getSheetWidth() {
        return sheetWidth;
    }

    public float getSheetHeight() {
        return sheetHeight;
    }
}
