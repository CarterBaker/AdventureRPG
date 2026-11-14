package editor.bootstrap.layerspipeline.layersmanager;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layerspipeline.layers.LayersProvider;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class LayersManager extends ManagerPackage {

    /*
     * Resolves which window the Layers tab shows: the window
     * ActiveWindowManager names worked in last among those that work on
     * layers, so activating one that works on none, the Layers tab itself
     * among them, keeps the target and a layer can be picked there for the
     * window it belongs to. Every window that works on layers opens itself
     * here for as long as it lives, through openWindow() and closeWindow(),
     * the one pair that owns them, and keeps here the groups turned from how
     * they start, so every Layers tab folds alike. Picking, showing and
     * hiding a layer go to the target window; the revision moves whenever a
     * group is folded or opened.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;

    // Windows
    private Object2ObjectOpenHashMap<WindowInstance, LayersProvider> window2Provider;
    private Object2ObjectOpenHashMap<WindowInstance, ObjectOpenHashSet<String>> window2ToggledKeys;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Windows
        this.window2Provider = new Object2ObjectOpenHashMap<>();
        this.window2ToggledKeys = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
    }

    // Windows \\

    public void openWindow(WindowInstance window, LayersProvider provider) {
        window2Provider.put(window, provider);
        window2ToggledKeys.put(window, new ObjectOpenHashSet<>());
    }

    public void closeWindow(WindowInstance window) {
        window2Provider.remove(window);
        window2ToggledKeys.remove(window);
    }

    // Layers \\

    public void selectLayer(String layerKey) {

        LayersProvider provider = getProvider();

        if (provider != null)
            provider.selectLayer(layerKey);
    }

    public void toggleLayerVisible(String layerKey) {

        LayersProvider provider = getProvider();

        if (provider != null)
            provider.toggleLayerVisible(layerKey);
    }

    // Groups \\

    public void toggleExpanded(String layerKey) {

        WindowInstance targetWindow = getTargetWindow();

        if (targetWindow == null)
            return;

        ObjectOpenHashSet<String> toggledKeys = window2ToggledKeys.get(targetWindow);

        if (!toggledKeys.remove(layerKey))
            toggledKeys.add(layerKey);

        revision++;
    }

    // Whether a group of the target window lists what it holds: as it starts, unless it was turned from that
    public boolean isExpanded(LayerNodeStruct node) {

        WindowInstance targetWindow = getTargetWindow();

        return targetWindow != null
                && node.isExpandedByDefault() != window2ToggledKeys.get(targetWindow).contains(node.getLayerKey());
    }

    // Accessible \\

    // The window worked in last among those that work on layers, null before any is worked in
    private WindowInstance getTargetWindow() {
        return activeWindowManager.findRecent(window2Provider);
    }

    // What the window worked in last shows in the Layers tab, null before any is worked in
    public LayersProvider getProvider() {

        WindowInstance targetWindow = getTargetWindow();

        return targetWindow != null ? window2Provider.get(targetWindow) : null;
    }

    public int getRevision() {
        return revision;
    }
}
