package editor.runtime.worldmap.layer;

import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapLayerSystem extends SystemPackage {

    /*
     * Keeps the World Map window's layer stack: the layer picked, Player when
     * it opens, and whether each layer is shown. The Layers tab lists the
     * stack top first, each layer named by its enum name, and its eyes show
     * or hide the day and night, the weather and the character's arrow, the
     * revision moving with every change so the tab lays it out again.
     */

    // Layers
    private WorldMapLayer layer;
    private boolean[] layer2Shown;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Layers
        this.layer = WorldMapLayer.PLAYER;
        this.layer2Shown = new boolean[WorldMapLayer.VALUES.length];

        for (int i = 0; i < layer2Shown.length; i++)
            layer2Shown[i] = WorldMapLayer.VALUES[i].isShownByDefault();
    }

    // Layers \\

    public void select(String layerKey) {

        WorldMapLayer selected = ArpgUtility.toEnum(layerKey, WorldMapLayer.class);

        if (selected == layer)
            return;

        this.layer = selected;
        revision++;
    }

    public void toggleShown(String layerKey) {

        int slot = ArpgUtility.toEnum(layerKey, WorldMapLayer.class).ordinal();

        layer2Shown[slot] = !layer2Shown[slot];
        revision++;
    }

    // The stack top first, the layer drawn over the others listed above them
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        for (int i = WorldMapLayer.VALUES.length - 1; i >= 0; i--) {

            WorldMapLayer listed = WorldMapLayer.VALUES[i];

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

    public boolean isShown(WorldMapLayer shown) {
        return layer2Shown[shown.ordinal()];
    }

    public int getRevision() {
        return revision;
    }
}
