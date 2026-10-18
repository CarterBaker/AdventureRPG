package editor.commandconsole.dial;

import application.bootstrap.calendarpipeline.calendar.CalendarHandle;
import application.bootstrap.calendarpipeline.clock.ClockHandle;
import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.bootstrap.commandpipeline.commandmanager.CommandManager;
import editor.commandconsole.CommandConsoleSetting;
import editor.dev.time.DevTimeUnit;
import editor.dev.time.DevTimeUtility;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CommandConsoleDialSystem extends SystemPackage {

    /*
     * The round dials the command console's tree shows for the time command,
     * one per unit of the clock. Each dial's needle and value follow the
     * shared clock live. Pressing a dial's face and circling the cursor
     * around it turns it, clockwise forward and counter-clockwise back, and
     * every whole step it passes advances its unit by one at once, carrying
     * into every larger unit as a clock does, so the world changes as the
     * dial turns; its minus and plus buttons step it by one. A cyclic unit's
     * needle goes once around its dial and reads like a clock hand; the year
     * and the age turn a fixed number of steps per turn. The cursor is
     * ignored close to the centre, where its angle is unsteady.
     */

    // Internal
    private InputManager inputManager;
    private MenuManager menuManager;
    private ClockManager clockManager;
    private CommandManager commandManager;

    // Dials
    private ObjectArrayList<CommandConsoleDialStruct> dials;

    // Drag
    private CommandConsoleDialStruct draggedDial;
    private int sentSteps;
    private double dragSteps;
    private float dragAngle;
    private boolean dragAngleTracked;

    // Base \\

    @Override
    protected void create() {

        // Dials
        this.dials = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.inputManager = get(InputManager.class);
        this.menuManager = get(MenuManager.class);
        this.clockManager = get(ClockManager.class);
        this.commandManager = get(CommandManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (draggedDial != null
                && !inputManager.getRawInput(context.getWindow()).isMouseDown(EditorSetting.COMMAND_DRAG_BUTTON))
            this.draggedDial = null;

        for (int i = 0; i < dials.size(); i++)
            refreshDial(dials.get(i));
    }

    private void refreshDial(CommandConsoleDialStruct dial) {

        ClockHandle clock = clockManager.getClockHandle();
        CalendarHandle calendar = clockManager.getCalendarHandle();
        DevTimeUnit unit = dial.getUnit();

        int value = DevTimeUtility.getValue(unit, clock, calendar);
        int stepsPerTurn = resolveStepsPerTurn(unit, clock, calendar);

        if (dial.isShowing(value, stepsPerTurn))
            return;

        int turnSteps = Math.floorMod(value - DevTimeUtility.getMinimum(unit, calendar), stepsPerTurn);

        dial.getNeedleElement().setRotation(-turnSteps * CommandConsoleSetting.DIAL_FULL_TURN_DEGREES / stepsPerTurn);
        dial.getValueElement().setFontText(DevTimeUtility.formatValue(unit, value, calendar));
        dial.setShown(value, stepsPerTurn);
    }

    // Layout \\

    public void clearDials() {
        dials.clear();
        this.draggedDial = null;
    }

    public boolean isAvailable(DevTimeUnit unit) {
        return DevTimeUtility.isAvailable(unit, clockManager.getCalendarHandle());
    }

    public void addDial(ElementInstance row, CommandHandle commandHandle, DevTimeUnit unit) {

        ElementInstance dialElement = menuManager.inject(row, CommandConsoleSetting.MENU_DIAL, null);

        dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_LABEL).setFontText(unit.getTitle());
        dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_DECREASE).setActionArgOverride(unit.getUnitName());
        dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_INCREASE).setActionArgOverride(unit.getUnitName());

        dials.add(new CommandConsoleDialStruct(
                unit,
                commandHandle,
                dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_FACE),
                dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_NEEDLE),
                dialElement.findChildById(CommandConsoleSetting.ELEMENT_DIAL_VALUE)));
    }

    // Turn \\

    // Called every frame the face stays pressed, from the frame it is first pressed
    public void turnDial(ElementInstance faceElement) {

        if (draggedDial == null)
            beginDrag(faceElement);

        if (draggedDial == null || draggedDial.getFaceElement() != faceElement)
            return;

        if (!isOutsideDeadZone(faceElement)) {
            this.dragAngleTracked = false;
            return;
        }

        float angle = resolveCursorAngle(faceElement);

        if (!dragAngleTracked) {
            this.dragAngle = angle;
            this.dragAngleTracked = true;
            return;
        }

        float delta = wrapDegrees(angle - dragAngle);
        int stepsPerTurn = resolveStepsPerTurn(
                draggedDial.getUnit(),
                clockManager.getClockHandle(),
                clockManager.getCalendarHandle());

        this.dragAngle = angle;
        this.dragSteps -= delta / CommandConsoleSetting.DIAL_FULL_TURN_DEGREES * stepsPerTurn;

        int steps = (int) Math.round(dragSteps);

        if (steps == sentSteps)
            return;

        sendSteps(draggedDial, steps - sentSteps);
        this.sentSteps = steps;
    }

    private void beginDrag(ElementInstance faceElement) {

        CommandConsoleDialStruct dial = findDial(faceElement);

        if (dial == null)
            return;

        this.draggedDial = dial;
        this.sentSteps = 0;
        this.dragSteps = 0.0;
        this.dragAngleTracked = false;
    }

    // Step \\

    public void stepDial(String unitName, int step) {

        CommandConsoleDialStruct dial = findDial(DevTimeUnit.fromUnitName(unitName));

        if (dial != null)
            sendSteps(dial, step);
    }

    // Send \\

    private void sendSteps(CommandConsoleDialStruct dial, int steps) {
        commandManager.executeCommand(dial.getCommandHandle().getCommandName()
                + EditorSetting.COMMAND_TOKEN_SEPARATOR + dial.getUnit().getUnitName()
                + EditorSetting.COMMAND_TOKEN_SEPARATOR + steps);
    }

    // Utility \\

    // A cyclic unit goes once around its dial; the year and the age turn a fixed number of steps per turn
    private int resolveStepsPerTurn(DevTimeUnit unit, ClockHandle clock, CalendarHandle calendar) {

        if (!unit.isCyclic())
            return CommandConsoleSetting.DIAL_OPEN_STEPS_PER_TURN;

        return DevTimeUtility.getMaximum(unit, clock, calendar) - DevTimeUtility.getMinimum(unit, calendar) + 1;
    }

    private boolean isOutsideDeadZone(ElementInstance faceElement) {

        WindowInstance window = context.getWindow();
        float dx = inputManager.getHoverMouseX(window) - resolveCenterX(faceElement);
        float dy = inputManager.getHoverMouseY(window) - resolveCenterY(faceElement);
        float deadRadius = Math.min(faceElement.getComputedW(), faceElement.getComputedH()) * 0.5f
                * CommandConsoleSetting.DIAL_DEAD_ZONE;

        return dx * dx + dy * dy > deadRadius * deadRadius;
    }

    // Counter-clockwise from the right, in degrees, as the layout's y runs up
    private float resolveCursorAngle(ElementInstance faceElement) {

        WindowInstance window = context.getWindow();

        return (float) Math.toDegrees(Math.atan2(
                inputManager.getHoverMouseY(window) - resolveCenterY(faceElement),
                inputManager.getHoverMouseX(window) - resolveCenterX(faceElement)));
    }

    private float resolveCenterX(ElementInstance element) {
        return element.getComputedLeft() + element.getComputedW() * 0.5f;
    }

    private float resolveCenterY(ElementInstance element) {
        return element.getComputedTop() + element.getComputedH() * 0.5f;
    }

    // Folds an angle into the half turn either side of zero, so crossing the left edge never reads as a full turn
    private float wrapDegrees(float degrees) {

        float wrapped = degrees % CommandConsoleSetting.DIAL_FULL_TURN_DEGREES;

        if (wrapped >= CommandConsoleSetting.DIAL_HALF_TURN_DEGREES)
            wrapped -= CommandConsoleSetting.DIAL_FULL_TURN_DEGREES;
        else if (wrapped < -CommandConsoleSetting.DIAL_HALF_TURN_DEGREES)
            wrapped += CommandConsoleSetting.DIAL_FULL_TURN_DEGREES;

        return wrapped;
    }

    private CommandConsoleDialStruct findDial(ElementInstance faceElement) {

        for (int i = 0; i < dials.size(); i++)
            if (dials.get(i).getFaceElement() == faceElement)
                return dials.get(i);

        return null;
    }

    private CommandConsoleDialStruct findDial(DevTimeUnit unit) {

        for (int i = 0; i < dials.size(); i++)
            if (dials.get(i).getUnit() == unit)
                return dials.get(i);

        return null;
    }
}
