package editor.bootstrap.stamppipeline.stampmanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.stamppipeline.stamp.StampData;
import editor.bootstrap.stamppipeline.stamp.StampSocketStruct;
import editor.bootstrap.stamppipeline.util.StampCategory;
import editor.bootstrap.stamppipeline.util.StampRigUtility;
import editor.runtime.EditorSetting;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.mathematics.vectors.Vector3;

class StampBuilder extends BuilderPackage {

    /*
     * Parses one stamp's ARPG object into StampData, from its file at boot
     * or from the Info Panel's live copy while it is edited. "bones" are the
     * part's own, each a rig bone entry; a bone naming no "parent" hangs from
     * the host bone it joins, and every parent must be listed before its
     * child. They are built into a rig of their own under the host
     * placeholder, and "mesh", a rigged mesh in the compact layout, is read
     * against it, weights the part leaves to its host on the placeholder.
     * "parts" are the rig layers it brings. The optional "plug" is the open
     * loop it joins a host by — its "position", the way it faces out of the
     * part ("normal") and its "radius" — and "sockets" the loops other parts
     * may join it at, each named and riding one of its bones. An empty or
     * missing display name falls back to the stamp's name.
     */

    // Internal
    private StampManager stampManager;
    private RigManager rigManager;
    private MeshManager meshManager;

    // Base \\

    @Override
    protected void get() {
        this.stampManager = get(StampManager.class);
        this.rigManager = get(RigManager.class);
        this.meshManager = get(MeshManager.class);
    }

    // Build \\

    StampData build(String stampName, ArpgObjectStruct stampArpg) {

        ArpgArrayStruct bonesArpg = ArpgUtility.validateArray(stampArpg, "bones");
        ArpgArrayStruct partsArpg = ArpgUtility.hasArray(stampArpg, "parts")
                ? stampArpg.getAsArray("parts").deepCopy()
                : new ArpgArrayStruct();
        RigHandle rigHandle = rigManager.buildDetachedRig(
                EditorSetting.STAMP_RIG_PREFIX + stampName, StampRigUtility.toRigArpg(bonesArpg, stampName));
        RiggedMeshStruct mesh = meshManager.parseRiggedMesh(stampArpg, rigHandle, stampName);
        String displayName = ArpgUtility.getString(stampArpg, "display_name", "");

        return new StampData(
                stampName,
                stampManager.registerStampName(stampName),
                displayName.isEmpty() ? stampName : displayName,
                ArpgUtility.getEnum(stampArpg, "category", StampCategory.class, StampCategory.OTHER),
                rigHandle,
                bonesArpg.deepCopy(),
                partsArpg,
                mesh,
                ArpgUtility.hasObject(stampArpg, "plug")
                        ? parseSocket(stampArpg.getAsObject("plug"), rigHandle, false, stampName)
                        : null,
                parseSockets(stampArpg, rigHandle, stampName));
    }

    private StampSocketStruct[] parseSockets(ArpgObjectStruct stampArpg, RigHandle rigHandle, String stampName) {

        if (!ArpgUtility.hasArray(stampArpg, "sockets"))
            return new StampSocketStruct[0];

        ArpgArrayStruct socketsArpg = stampArpg.getAsArray("sockets");
        StampSocketStruct[] sockets = new StampSocketStruct[socketsArpg.size()];

        for (int i = 0; i < sockets.length; i++)
            sockets[i] = parseSocket(socketsArpg.get(i).getAsObject(), rigHandle, true, stampName);

        return sockets;
    }

    // A loop of the part: a socket names itself and the bone it rides, a plug rides the host
    private StampSocketStruct parseSocket(
            ArpgObjectStruct socketArpg,
            RigHandle rigHandle,
            boolean socket,
            String stampName) {

        String socketName = socket ? ArpgUtility.validateString(socketArpg, "name") : EditorSetting.STAMP_HOST_BONE;
        String boneName = socket ? ArpgUtility.validateString(socketArpg, "bone") : EditorSetting.STAMP_HOST_BONE;

        if (!rigHandle.hasBone(boneName))
            throwException("Stamp socket \"" + socketName + "\" rides \"" + boneName
                    + "\", which is not one of its bones. Source: " + stampName);

        Vector3 normal = toVector(ArpgUtility.validateObject(socketArpg, "normal"));

        if (normal.length() <= 0f)
            throwException("Stamp socket \"" + socketName + "\" faces no way: its normal has no length. Source: "
                    + stampName);

        float radius = ArpgUtility.validateFloat(socketArpg, "radius");

        if (radius <= 0f)
            throwException("Stamp socket \"" + socketName + "\" needs a radius above 0. Source: " + stampName);

        return new StampSocketStruct(
                socketName,
                rigHandle.getBoneIndex(boneName),
                toVector(ArpgUtility.validateObject(socketArpg, "position")),
                normal.normalize(),
                radius);
    }

    private Vector3 toVector(ArpgObjectStruct vectorArpg) {
        return new Vector3(
                ArpgUtility.validateFloat(vectorArpg, "x"),
                ArpgUtility.validateFloat(vectorArpg, "y"),
                ArpgUtility.validateFloat(vectorArpg, "z"));
    }
}
