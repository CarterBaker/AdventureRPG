package editor.commandconsole.dial;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.commandpipeline.command.CommandHandle;
import editor.dev.time.DevTimeUnit;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class CommandConsoleDialStruct extends StructPackage {

    /*
     * One dial in the command console's tree: the time unit it turns, the
     * command it sends, the face it is turned by, the needle and value label
     * it shows, and the value and turn last drawn on it, so a dial is redrawn
     * only when either changes.
     */

    // Dial
    private final DevTimeUnit unit;
    private final CommandHandle commandHandle;

    // Elements
    private final ElementInstance faceElement;
    private final ElementInstance needleElement;
    private final ElementInstance valueElement;

    // Shown
    private int shownValue;
    private int shownStepsPerTurn;

    // Constructor \\

    public CommandConsoleDialStruct(
            DevTimeUnit unit,
            CommandHandle commandHandle,
            ElementInstance faceElement,
            ElementInstance needleElement,
            ElementInstance valueElement) {

        // Dial
        this.unit = unit;
        this.commandHandle = commandHandle;

        // Elements
        this.faceElement = faceElement;
        this.needleElement = needleElement;
        this.valueElement = valueElement;

        // Shown
        this.shownValue = EngineSetting.INDEX_NOT_FOUND;
        this.shownStepsPerTurn = EngineSetting.INDEX_NOT_FOUND;
    }

    // Management \\

    public boolean isShowing(int value, int stepsPerTurn) {
        return shownValue == value && shownStepsPerTurn == stepsPerTurn;
    }

    public void setShown(int value, int stepsPerTurn) {
        this.shownValue = value;
        this.shownStepsPerTurn = stepsPerTurn;
    }

    // Accessible \\

    public DevTimeUnit getUnit() {
        return unit;
    }

    public CommandHandle getCommandHandle() {
        return commandHandle;
    }

    public ElementInstance getFaceElement() {
        return faceElement;
    }

    public ElementInstance getNeedleElement() {
        return needleElement;
    }

    public ElementInstance getValueElement() {
        return valueElement;
    }
}
