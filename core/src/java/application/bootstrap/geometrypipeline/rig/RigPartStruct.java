package application.bootstrap.geometrypipeline.rig;

import engine.root.StructPackage;

public class RigPartStruct extends StructPackage {

    /*
     * One part of a body a rig names so its editors can work on it closely,
     * the way a rig's bone collections group its bones: its name, the bones
     * it holds (with every bone below them), the worn part held in it beside
     * the body (eyes in their sockets, teeth in the mouth) and the worn parts
     * shown with it (hair on a head), and whether the face's controls are
     * shaped there. Worn parts are named by their slot, so the rig never
     * depends on what is worn on it.
     */

    // Identity
    private final String name;

    // Bones
    private final int[] bones;

    // Worn — the slot held in the part, empty for none, and the slots shown beside it
    private final String heldSlot;
    private final String[] shownSlots;

    // Face
    private final boolean face;

    // Constructor \\

    public RigPartStruct(String name, int[] bones, String heldSlot, String[] shownSlots, boolean face) {

        // Identity
        this.name = name;

        // Bones
        this.bones = bones;

        // Worn
        this.heldSlot = heldSlot;
        this.shownSlots = shownSlots;

        // Face
        this.face = face;
    }

    // Accessible \\

    public String getName() {
        return name;
    }

    public int[] getBones() {
        return bones;
    }

    public boolean holdsSlot() {
        return !heldSlot.isEmpty();
    }

    public String getHeldSlot() {
        return heldSlot;
    }

    public String[] getShownSlots() {
        return shownSlots;
    }

    public boolean takesFace() {
        return face;
    }
}
