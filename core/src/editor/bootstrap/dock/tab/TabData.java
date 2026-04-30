package editor.bootstrap.dock.tab;

import engine.root.ContextPackage;
import engine.root.DataPackage;

public class TabData extends DataPackage {

    // Display
    private final String title;

    // Runtime Type
    private final Class<? extends ContextPackage> contextClass;

    // Constructor \\

    public TabData(String title, Class<? extends ContextPackage> contextClass) {
        this.title = title;
        this.contextClass = contextClass;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public Class<? extends ContextPackage> getContextClass() {
        return contextClass;
    }
}