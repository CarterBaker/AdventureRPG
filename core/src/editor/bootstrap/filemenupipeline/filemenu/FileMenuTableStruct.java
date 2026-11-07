package editor.bootstrap.filemenupipeline.filemenu;

import editor.bootstrap.filemenupipeline.util.FileMenuAction;
import engine.root.StructPackage;

public class FileMenuTableStruct extends StructPackage {

    /*
     * The File and Edit menu actions one tool takes: the branch class every
     * one of them runs on, and per action the method, and the argument it is
     * handed when it takes one, indexed by FileMenuAction's order. Each tool
     * declares its table once, as a constant naming the methods its own keys
     * already run.
     */

    // Action
    private final String actionClass;

    // Methods
    private final String[] methods;
    private final String[] args;

    // Constructor \\

    public FileMenuTableStruct(String actionClass) {

        // Action
        this.actionClass = actionClass;

        // Methods
        this.methods = new String[FileMenuAction.VALUES.length];
        this.args = new String[FileMenuAction.VALUES.length];
    }

    // Declaration \\

    public FileMenuTableStruct with(FileMenuAction action, String method) {
        return with(action, method, null);
    }

    public FileMenuTableStruct with(FileMenuAction action, String method, String arg) {

        methods[action.ordinal()] = method;
        args[action.ordinal()] = arg;

        return this;
    }

    // Accessible \\

    public boolean takes(FileMenuAction action) {
        return methods[action.ordinal()] != null;
    }

    public String getActionClass() {
        return actionClass;
    }

    public String getMethod(FileMenuAction action) {
        return methods[action.ordinal()];
    }

    public String getArg(FileMenuAction action) {
        return args[action.ordinal()];
    }
}
