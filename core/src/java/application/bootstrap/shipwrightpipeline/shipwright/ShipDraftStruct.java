package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipDraftStruct extends StructPackage {

    /*
     * The ship on a drafting table's sheet built for a viewer while it is
     * still being drawn: a compiled layout of its own, apart from the table's,
     * and the meshes every part but the anchor cables is drawn with at rest,
     * made once the sheet has rested and freed before the next are made, so
     * a viewer shows the ship as it stands without anything being saved.
     */

    // Layout
    private final ShipLayoutStruct layout;

    // Meshes
    private final ObjectArrayList<MeshInstance> meshes;

    // Identity
    private String displayName;
    private int deckCount;

    // Constructor \\

    public ShipDraftStruct() {

        // Layout
        this.layout = new ShipLayoutStruct(new ShipOctantGridStruct());

        // Meshes
        this.meshes = new ObjectArrayList<>();

        // Identity
        this.displayName = EngineSetting.SHIP_NAME_NONE;
    }

    // Management \\

    public void setIdentity(String displayName, int deckCount) {
        this.displayName = displayName;
        this.deckCount = deckCount;
    }

    // Accessible \\

    public ShipLayoutStruct getLayout() {
        return layout;
    }

    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDeckCount() {
        return deckCount;
    }
}
