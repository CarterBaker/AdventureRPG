package editor.runtime.propertiessource;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import engine.root.SystemPackage;

public class PropertiesSourceSystem extends SystemPackage {

    /*
     * Opens this context's window to PropertiesManager for as long as it
     * lives, so the Properties tab shows what the context provides while it
     * is the window worked in last. Every window that provides properties
     * creates one; nothing else opens or closes a window there.
     */

    // Internal
    private PropertiesManager propertiesManager;

    // Base \\

    @Override
    protected void get() {
        this.propertiesManager = get(PropertiesManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof PropertiesProvider))
            throwException("A window that provides properties must say what it shows.");

        propertiesManager.openWindow(context.getWindow(), (PropertiesProvider) context);
    }

    @Override
    protected void dispose() {
        propertiesManager.closeWindow(context.getWindow());
    }
}
