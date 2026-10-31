package application.runtime.shipwright;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.ElementHitSystem;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
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

public class ShipwrightSystem extends SystemPackage {

    /*
     * The drafting table in this window, the same in the game and the editor.
     * open() lays the table menu over the window, with its leave menu when the
     * table writes to the player's saves; the session it opens is kept, so a
     * table opened again shows the ship left on it. Everything is worked
     * through a cursor on the sheet so the table can be driven without a
     * pointer: the movement bindings step it an octant at a time, repeating
     * while held, and the pointer moves it whenever it passes over the sheet.
     * The jump binding or the left button draws with the tool in hand while
     * held and the sprint binding or the right button erases, a stroke at a
     * time; the activate binding takes up the next tool, the rotate binding
     * turns what is set down next, the cycle bindings step the option of the
     * tool in hand and the level bindings or the wheel step the deck, so
     * every change the menu makes can also be made from the bindings alone, a
     * controller's among them. Undo, redo and save work as everywhere else;
     * the pause binding walks away from a table in the game. While the name is typed the keys type it, Enter keeps
     * it and the pause binding drops it. Whenever the session changes the
     * labels are rewritten, every measure of the ship among them.
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

    // Input — the bindings that opened the table are ignored the frame it opens
    private boolean opening;
    private boolean focused;
    private boolean focusedLastFrame;
    private float pointerX;
    private float pointerY;

    // Cursor — seconds until a held movement binding steps the cursor again
    private float repeatCountdown;
    private boolean stepping;

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
        this.opening = true;
        this.focused = false;
        this.focusedLastFrame = false;
        this.stepping = false;
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

        shipwrightManager.resolveSheetSize(session);
        fitSheet();

        if (opening) {
            this.opening = false;
            this.pointerX = inputManager.getHoverMouseX(window);
            this.pointerY = inputManager.getHoverMouseY(window);
        } else if (session.isNaming())
            updateNaming(window, rawInput);
        else if (updateBindings(window))
            return;
        else
            updateCursor(window, rawInput);

        shipwrightManager.updateSurvey(session, internal.getDeltaTime());
        refreshLabels();
    }

    // The table's bindings, true when the pause binding walked away from a table in the game
    private boolean updateBindings(WindowInstance window) {

        if (isClosable() && inputManager.bindingClicked(KeyBindings.PAUSE, window)) {
            close();
            return true;
        }

        if (inputManager.bindingClicked(KeyBindings.ACTIVATE, window)) {
            shipwrightManager.endStroke(session);
            shipwrightManager.nextTool(session);
        }

        if (inputManager.bindingClicked(KeyBindings.ROTATE_ITEM, window))
            shipwrightManager.turn(session);

        if (inputManager.bindingClicked(KeyBindings.CYCLE_NEXT, window))
            shipwrightManager.stepOption(session, 1);

        if (inputManager.bindingClicked(KeyBindings.CYCLE_PREVIOUS, window))
            shipwrightManager.stepOption(session, -1);

        if (inputManager.bindingClicked(KeyBindings.LEVEL_UP, window))
            shipwrightManager.stepDeck(session, 1);

        if (inputManager.bindingClicked(KeyBindings.LEVEL_DOWN, window))
            shipwrightManager.stepDeck(session, -1);

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

    // Cursor \\

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

    // Moves the cursor by the pointer and the movement bindings, then starts, carries and ends a stroke at it
    private void updateCursor(WindowInstance window, Input rawInput) {

        boolean pointerOver = updatePointer(window, rawInput);

        stepCursor(window);

        boolean drawing = inputManager.bindingHeld(KeyBindings.JUMP, window)
                || pointerOver && rawInput.isMouseDown(Buttons.LEFT);
        boolean erasing = inputManager.bindingHeld(KeyBindings.SPRINT, window)
                || pointerOver && rawInput.isMouseDown(Buttons.RIGHT);

        if (session.isStroking() && !drawing && !erasing) {
            shipwrightManager.endStroke(session);
            return;
        }

        if (!session.isStroking() && focusedLastFrame && (drawing || erasing))
            shipwrightManager.beginStroke(session, erasing && !drawing);

        if (session.isStroking())
            shipwrightManager.strokeAt(session, session.getCursorU(), session.getCursorV());
    }

    // The cursor put on the octant under the pointer once it moves over the sheet, the wheel stepping the deck;
    // true while the pointer lies over the sheet
    private boolean updatePointer(WindowInstance window, Input rawInput) {

        float mouseX = inputManager.getHoverMouseX(window);
        float mouseY = inputManager.getHoverMouseY(window);
        boolean moved = mouseX != pointerX || mouseY != pointerY;
        boolean over = isPointerFree(window) && cellSize > 0f
                && mouseX >= sheetLeft && mouseX < sheetLeft + sheetWidth
                && mouseY >= sheetBottom && mouseY < sheetBottom + sheetHeight;

        this.pointerX = mouseX;
        this.pointerY = mouseY;

        if (!over)
            return false;

        if (moved || rawInput.isMouseDown(Buttons.LEFT) || rawInput.isMouseDown(Buttons.RIGHT))
            session.moveCursor((int) ((mouseX - sheetLeft) / cellSize), (int) ((mouseY - sheetBottom) / cellSize));

        float wheel = rawInput.getScrollY();

        if (wheel != 0f)
            shipwrightManager.stepDeck(session, wheel > 0f ? 1 : -1);

        return true;
    }

    // An octant a press of a movement binding, then again and again while it is held
    private void stepCursor(WindowInstance window) {

        int stepU = (inputManager.bindingHeld(KeyBindings.MOVE_RIGHT, window) ? 1 : 0)
                - (inputManager.bindingHeld(KeyBindings.MOVE_LEFT, window) ? 1 : 0);
        int stepV = (inputManager.bindingHeld(KeyBindings.MOVE_FORWARD, window) ? 1 : 0)
                - (inputManager.bindingHeld(KeyBindings.MOVE_BACK, window) ? 1 : 0);

        if (stepU == 0 && stepV == 0) {
            this.stepping = false;
            return;
        }

        if (stepping) {

            repeatCountdown -= internal.getDeltaTime();

            if (repeatCountdown > 0f)
                return;

            repeatCountdown += EngineSetting.SHIPWRIGHT_CURSOR_REPEAT_INTERVAL;
        } else {
            this.stepping = true;
            this.repeatCountdown = EngineSetting.SHIPWRIGHT_CURSOR_REPEAT_DELAY;
        }

        session.moveCursor(
                Math.max(0, Math.min(session.getSheetWidth() - 1, session.getCursorU() + stepU)),
                Math.max(0, Math.min(session.getSheetHeight() - 1, session.getCursorV() + stepV)));
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
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_DECK, resolveDeck());
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_OPTION, resolveOption());
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

        return text.append(session.getTool().getTitle()).append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(EngineSetting.SHIPWRIGHT_HINTS[session.getTool().ordinal()]).toString();
    }

    // The deck picked, of how many, and whether its floor lies above or below the water
    private String resolveDeck() {

        text.setLength(0);
        ShipLayoutStruct layout = session.getLayout();
        int deck = session.getDeck();

        text.append(EngineSetting.SHIPWRIGHT_TEXT_DECK).append(deck + 1)
                .append(EngineSetting.SHIPWRIGHT_TEXT_OF).append(session.getBlueprint().getDeckCount());

        if (!layout.isAfloat())
            return text.toString();

        boolean under = (deck * EngineSetting.SHIP_DECK_HEIGHT + 1) * EngineSetting.SUB_BLOCK_SIZE
                <= layout.getDraft();

        return text.append(under
                ? EngineSetting.SHIPWRIGHT_TEXT_UNDER_WATER
                : EngineSetting.SHIPWRIGHT_TEXT_ABOVE_WATER).toString();
    }

    // What the tool in hand sets down next and which way it faces
    private String resolveOption() {

        text.setLength(0);
        ShipwrightTool tool = session.getTool();

        return switch (tool) {
            case WALLS -> text.append(session.isHalfWalls()
                    ? EngineSetting.SHIPWRIGHT_TEXT_HALF_WALL : EngineSetting.SHIPWRIGHT_TEXT_FULL_WALL).toString();
            case OPENINGS -> appendFacing(text.append(session.getOpening().getTitle())).toString();
            case STAIRS -> appendFacing(text.append(session.isLadders()
                    ? EngineSetting.SHIPWRIGHT_TEXT_LADDER : EngineSetting.SHIPWRIGHT_TEXT_STAIR)).toString();
            case FURNITURE -> appendFacing(text.append(shipwrightManager.getItemName(session.getItemIndex())))
                    .toString();
            case MASTS -> appendMast(text).toString();
            default -> text.append(EngineSetting.SHIPWRIGHT_TEXT_MIRRORED).toString();
        };
    }

    private StringBuilder appendFacing(StringBuilder out) {
        return out.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(EngineSetting.SHIPWRIGHT_FACINGS[session.getSpin()]);
    }

    // The mast picked as the hull rigs it: its height, the sails it carries and its crow's nest
    private StringBuilder appendMast(StringBuilder out) {

        ShipLayoutStruct layout = session.getLayout();
        int mastIndex = session.getMastIndex();

        if (!session.hasMastPicked() || mastIndex >= layout.getMastCount())
            return out.append(EngineSetting.SHIPWRIGHT_TEXT_NO_MAST);

        out.append(shipwrightManager.getMastName(session, mastIndex))
                .append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(layout.getMastHeight(mastIndex) * EngineSetting.SUB_BLOCK_SIZE)
                .append(EngineSetting.SHIPWRIGHT_TEXT_BLOCKS)
                .append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR)
                .append(layout.getMastSailCount(mastIndex))
                .append(EngineSetting.SHIPWRIGHT_TEXT_SAILS);

        if (layout.hasCrowsNest(mastIndex))
            out.append(EngineSetting.SHIPWRIGHT_TEXT_SEPARATOR).append(EngineSetting.SHIPWRIGHT_TEXT_NEST);

        return out;
    }

    // Survey \\

    private void writeSurvey() {

        ShipSurveyStruct survey = session.getSurvey();
        ShipBlueprintStruct blueprint = session.getBlueprint();

        if (survey.getDraft() <= 0f) {
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_WATER, EngineSetting.SHIPWRIGHT_TEXT_NOT_AFLOAT);
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_BUOYANCY, String.format(EngineSetting.SHIPWRIGHT_FORMAT_WEIGHT,
                    survey.getMass()));
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_STABILITY, EngineSetting.SHIPWRIGHT_STATUS_NONE);
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SPEED, EngineSetting.SHIPWRIGHT_STATUS_NONE);
        } else {
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_WATER, String.format(EngineSetting.SHIPWRIGHT_FORMAT_WATER,
                    survey.getDraft(), survey.getFreeboard()));
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_BUOYANCY, String.format(EngineSetting.SHIPWRIGHT_FORMAT_BUOYANCY,
                    survey.getMass(), survey.getBallast(), survey.getReserve() * 100f));
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_STABILITY, resolveStability(survey));
            setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SPEED, String.format(EngineSetting.SHIPWRIGHT_FORMAT_SPEED,
                    survey.getBreezeSpeed(), survey.getGaleSpeed()));
        }

        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_SIZE, String.format(EngineSetting.SHIPWRIGHT_FORMAT_SIZE,
                survey.getLength(), survey.getBeam(), blueprint.getDeckCount(), survey.getSailCount()));
        setLabel(EngineSetting.SHIPWRIGHT_ENTRY_TRIANGLES, String.format(EngineSetting.SHIPWRIGHT_FORMAT_TRIANGLES,
                survey.getTriangles(), EngineSetting.SHIP_TRIANGLE_LIMIT));
    }

    // Whether the ship rolls over as launched, may be laid over by its canvas, or stands up well, and to how far
    private String resolveStability(ShipSurveyStruct survey) {

        float stable = survey.getStableDegrees();

        if (stable <= 0f)
            return EngineSetting.SHIPWRIGHT_TEXT_CAPSIZES;

        String format = stable < EngineSetting.SHIPWRIGHT_TENDER_DEGREES
                ? EngineSetting.SHIPWRIGHT_FORMAT_TENDER
                : EngineSetting.SHIPWRIGHT_FORMAT_STABLE;

        return String.format(format, stable);
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
