package editor.runtime.worldeditor.layer;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldEditorLayerSystem extends SystemPackage {

    /*
     * Keeps the World Editor window's layer stack: the layer its tools work
     * on, Biomes when it opens, and whether each layer is shown, every layer
     * shown when it opens. The Layers tab lists the stack top first, each
     * layer named by its enum name, picks the layer the tools work on and
     * shows or hides a layer, the revision moving with every change so the
     * tab lays it out again.
     */

    // Layers
    private WorldEditorLayer layer;
    private boolean[] layer2Shown;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.layer = WorldEditorLayer.BIOMES;
        this.layer2Shown = new boolean[WorldEditorLayer.VALUES.length];

        for (int i = 0; i < layer2Shown.length; i++)
            layer2Shown[i] = true;
    }

    // Layers \\

    public void select(String layerKey) {

        WorldEditorLayer selected = ArpgUtility.toEnum(layerKey, WorldEditorLayer.class);

        if (selected == layer)
            return;

        this.layer = selected;
        revision++;
    }

    public void toggleShown(String layerKey) {

        int slot = ArpgUtility.toEnum(layerKey, WorldEditorLayer.class).ordinal();

        layer2Shown[slot] = !layer2Shown[slot];
        revision++;
    }

    // The stack top first, the layer drawn over the others listed above them
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        for (int i = WorldEditorLayer.VALUES.length - 1; i >= 0; i--) {

            WorldEditorLayer listed = WorldEditorLayer.VALUES[i];

            roots.add(new LayerNodeStruct(
                    ArpgUtility.toEnumName(listed),
                    listed.getLabel(),
                    listed == layer,
                    true,
                    layer2Shown[i],
                    false));
        }
    }

    // Accessible \\

    public WorldEditorLayer getLayer() {
        return layer;
    }

    public boolean isShown(WorldEditorLayer shown) {
        return layer2Shown[shown.ordinal()];
    }

    public int getRevision() {
        return revision;
    }
}
