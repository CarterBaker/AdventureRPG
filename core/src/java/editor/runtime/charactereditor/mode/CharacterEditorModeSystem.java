package editor.runtime.charactereditor.mode;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.charactereditorpipeline.util.CharacterRegionStruct;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.tabpipeline.activewindowmanager.ActiveWindowManager;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CharacterEditorModeSystem extends SystemPackage {

    /*
     * Keeps the mode a character tool tab works in, the one its context names
     * (CharacterModeProvider; the UV Editor works in UV), and the region it
     * works on there: the region picked in the Layers tab
     * for every tab that takes regions, shared by all of them, and the whole
     * character for the rest. The Layers tab lists
     * the regions of the race's rig for a tab that takes them, the Body
     * holding every region nested under it, and the paint layers of the map
     * painted, top first, for the Paint tab. While the window is the active
     * window, and whenever a layer is picked for it, its region and mode are
     * handed to the editor, so the Character Outliner and the Properties tab
     * follow the window worked in last. The window draws its preview in its
     * own mode whatever the editor's is.
     */

    // Internal
    private ActiveWindowManager activeWindowManager;
    private CharacterEditorManager characterEditorManager;

    // Mode
    private CharacterEditorMode mode;

    // Paint Layers — the canvas and its layer revision last listed, so the stack is laid out again when either moves
    private CharacterPaintCanvasStruct listedCanvas;
    private int listedLayerRevision;
    private int paintRevision;

    // Base \\

    @Override
    protected void get() {
        this.activeWindowManager = get(ActiveWindowManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.mode = ((CharacterModeProvider) context).getMode();
    }

    // Update \\

    @Override
    protected void update() {
        if (activeWindowManager.isActive(context.getWindow()))
            adopt();
    }

    // This window's region and mode handed to the editor, the mode only when it moved, so the editor's state holds
    public void adopt() {

        characterEditorManager.setRegion(getRegion());

        if (characterEditorManager.getMode() != mode)
            characterEditorManager.setMode(mode);
    }

    // Layers \\

    // A region by its key, picked for every tab that takes regions, or a paint layer by its number
    public void select(String layerKey) {

        if (mode.paintsMaps())
            characterEditorManager.selectPaintLayer(Integer.parseInt(layerKey));
        else
            characterEditorManager.pickRegion(characterEditorManager.findRegion(layerKey));

        adopt();
    }

    // A paint layer above the base shown or hidden; the regions are always drawn, the one picked standing alone
    public void toggleVisible(String layerKey) {
        if (mode.paintsMaps())
            characterEditorManager.togglePaintLayerVisible(Integer.parseInt(layerKey));
    }

    // The paint layers top first, or the Body and every region within it
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        if (mode.paintsMaps())
            buildPaintLayers(roots);
        else
            roots.add(buildRegion(CharacterRegionStruct.WHOLE));
    }

    // Every layer of the map painted, the base an image that is never hidden, each above it with its eye
    private void buildPaintLayers(ObjectArrayList<LayerNodeStruct> roots) {

        CharacterPaintCanvasStruct canvas = characterEditorManager.getPaintCanvas();

        if (canvas == null)
            return;

        for (int i = canvas.getLayerCount() - 1; i >= 0; i--)
            roots.add(new LayerNodeStruct(
                    Integer.toString(i),
                    canvas.getLayerName(i),
                    i == canvas.getActiveLayer(),
                    i > 0,
                    canvas.isLayerVisible(i),
                    false));
    }

    // A region's group, open by default while it holds regions: the region itself, then the regions listed under it
    private LayerNodeStruct buildRegion(CharacterRegionStruct listed) {

        ObjectArrayList<CharacterRegionStruct> regions = characterEditorManager.getRegions();
        boolean nests = false;

        for (int i = 0; i < regions.size(); i++)
            nests |= regions.get(i).getParent() == listed;

        LayerNodeStruct node = new LayerNodeStruct(listed.getKey(), listed.getLabel(), listed == getRegion(), nests);

        for (int i = 0; i < regions.size(); i++)
            if (regions.get(i).getParent() == listed)
                node.addChild(buildRegion(regions.get(i)));

        return node;
    }

    // Accessible \\

    public CharacterEditorMode getMode() {
        return mode;
    }

    // The region picked for a tab that takes regions, the whole character otherwise
    public CharacterRegionStruct getRegion() {
        return mode.takesRegions() ? characterEditorManager.getPickedRegion() : CharacterRegionStruct.WHOLE;
    }

    // The region the view frames: the region picked for every tab that shares its view, Paint's among them, so tabs
    // handing a view on frame one part; the whole character otherwise
    public CharacterRegionStruct getFramedRegion() {
        return mode.sharesView() ? characterEditorManager.getPickedRegion() : CharacterRegionStruct.WHOLE;
    }

    // Moves whenever the layers listed or the layer picked change
    public int getRevision() {

        if (!mode.paintsMaps())
            return characterEditorManager.getRegionPickRevision();

        CharacterPaintCanvasStruct canvas = characterEditorManager.getPaintCanvas();
        int layerRevision = canvas != null ? canvas.getLayerRevision() : 0;

        if (canvas != listedCanvas || layerRevision != listedLayerRevision) {
            this.listedCanvas = canvas;
            this.listedLayerRevision = layerRevision;
            paintRevision++;
        }

        return paintRevision;
    }
}
