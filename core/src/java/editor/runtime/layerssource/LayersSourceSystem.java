package editor.runtime.layerssource;

import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.layerspipeline.layersmanager.LayersManager;
import engine.root.SystemPackage;

public class LayersSourceSystem extends SystemPackage {

    /*
     * Opens this context's window to LayersManager for as long as it lives,
     * so the Layers tab shows the context's layer stack while it is the
     * window worked in last. Every window that works on layers creates one;
     * nothing else opens or closes a window there.
     */

    // Internal
    private LayersManager layersManager;

    // Base \\

    @Override
    protected void get() {
        this.layersManager = get(LayersManager.class);
    }

    @Override
    protected void awake() {

        if (!(context instanceof LayersProvider))
            throwException("A window that works on layers must list them.");

        layersManager.openWindow(context.getWindow(), (LayersProvider) context);
    }

    @Override
    protected void dispose() {
        layersManager.closeWindow(context.getWindow());
    }
}
