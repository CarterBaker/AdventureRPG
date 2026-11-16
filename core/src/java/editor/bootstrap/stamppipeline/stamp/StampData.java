package editor.bootstrap.stamppipeline.stamp;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.stamppipeline.util.StampCategory;
import engine.root.DataPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.mathematics.vectors.Vector3;

public class StampData extends DataPackage {

    /*
     * Immutable definition of one stamp, a body part a character is built
     * from: its name, which is its file's path, and its registry ID; the name
     * it is listed by and its kind; its own rig, the host placeholder first
     * and the stamp's bones under it, with the bones' and layers' entries as
     * the file holds them, to be written into a character's rig when the part
     * joins it; its skinned mesh on that rig, in the stamp's own space; the
     * plug it joins a host by, null for a root part placed alone; the
     * sockets other parts may join it at; and the bounds of its mesh.
     */

    // Identity
    private final String stampName;
    private final int stampID;
    private final String displayName;
    private final StampCategory category;

    // Rig
    private final RigHandle rigHandle;
    private final ArpgArrayStruct bonesArpg;
    private final ArpgArrayStruct partsArpg;

    // Mesh
    private final RiggedMeshStruct mesh;
    private final Vector3 boundsMin;
    private final Vector3 boundsMax;

    // Sockets
    private final StampSocketStruct plug;
    private final StampSocketStruct[] sockets;

    // Constructor \\

    public StampData(
            String stampName,
            int stampID,
            String displayName,
            StampCategory category,
            RigHandle rigHandle,
            ArpgArrayStruct bonesArpg,
            ArpgArrayStruct partsArpg,
            RiggedMeshStruct mesh,
            StampSocketStruct plug,
            StampSocketStruct[] sockets) {

        // Identity
        this.stampName = stampName;
        this.stampID = stampID;
        this.displayName = displayName;
        this.category = category;

        // Rig
        this.rigHandle = rigHandle;
        this.bonesArpg = bonesArpg;
        this.partsArpg = partsArpg;

        // Mesh
        this.mesh = mesh;
        this.boundsMin = new Vector3();
        this.boundsMax = new Vector3();
        resolveBounds();

        // Sockets
        this.plug = plug;
        this.sockets = sockets;
    }

    private void resolveBounds() {

        if (mesh.getVertexCount() == 0)
            return;

        boundsMin.set(mesh.getX(0), mesh.getY(0), mesh.getZ(0));
        boundsMax.set(boundsMin);

        for (int v = 1; v < mesh.getVertexCount(); v++) {
            boundsMin.set(
                    Math.min(boundsMin.x, mesh.getX(v)),
                    Math.min(boundsMin.y, mesh.getY(v)),
                    Math.min(boundsMin.z, mesh.getZ(v)));
            boundsMax.set(
                    Math.max(boundsMax.x, mesh.getX(v)),
                    Math.max(boundsMax.y, mesh.getY(v)),
                    Math.max(boundsMax.z, mesh.getZ(v)));
        }
    }

    // Accessible \\

    public String getStampName() {
        return stampName;
    }

    public int getStampID() {
        return stampID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public StampCategory getCategory() {
        return category;
    }

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public ArpgArrayStruct getBonesArpg() {
        return bonesArpg;
    }

    public ArpgArrayStruct getPartsArpg() {
        return partsArpg;
    }

    public RiggedMeshStruct getMesh() {
        return mesh;
    }

    public Vector3 getBoundsMin() {
        return boundsMin;
    }

    public Vector3 getBoundsMax() {
        return boundsMax;
    }

    public boolean hasPlug() {
        return plug != null;
    }

    public StampSocketStruct getPlug() {
        return plug;
    }

    public int getSocketCount() {
        return sockets.length;
    }

    public StampSocketStruct getSocket(int index) {
        return sockets[index];
    }
}
