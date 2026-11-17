package editor.bootstrap.stamppipeline.stampmanager;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.meshmanager.MeshManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import editor.bootstrap.stamppipeline.stamp.StampData;
import editor.bootstrap.stamppipeline.stamp.StampFurStruct;
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
     * "parts" are the rig layers it brings, and "animations" the motion it
     * brings to its host's clips: each clip by name, its "tracks" keyed on
     * the stamp's own bones for either side, and its "left" and "right" for
     * a side that moves its own way. The optional "plug" is the open
     * loop it joins a host by — its "position", the way it faces out of the
     * part ("normal") and its "radius" — and "sockets" the loops other parts
     * may join it at, each named and riding one of its bones. A fur stamp
     * instead names "fur", the patch "radius", the "spacing" between roots
     * and the "jitter" its cards vary by, its mesh the one strand card laid
     * at every root; it never has a plug. An empty or missing display name
     * falls back to the stamp's name.
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

        ArpgArrayStruct bonesArpg = ArpgUtility.validateArray(stampArpg, EditorSetting.STAMP_FIELD_BONES);
        ArpgArrayStruct partsArpg = ArpgUtility.hasArray(stampArpg, EditorSetting.STAMP_FIELD_PARTS)
                ? stampArpg.getAsArray(EditorSetting.STAMP_FIELD_PARTS).deepCopy()
                : new ArpgArrayStruct();
        boolean plugged = ArpgUtility.hasObject(stampArpg, EditorSetting.STAMP_FIELD_PLUG);
        boolean fur = ArpgUtility.hasObject(stampArpg, EditorSetting.STAMP_FIELD_FUR);

        if (plugged && fur)
            throwException("Stamp names both a plug and fur; fur is laid over a body, never joined to it. Source: "
                    + stampName);

        RigHandle rigHandle = rigManager.buildDetachedRig(
                EditorSetting.STAMP_RIG_PREFIX + stampName, StampRigUtility.toRigArpg(bonesArpg, stampName));
        RiggedMeshStruct mesh = meshManager.parseRiggedMesh(stampArpg, rigHandle, stampName);
        String displayName = ArpgUtility.getString(stampArpg, EditorSetting.STAMP_FIELD_DISPLAY_NAME, "");

        return new StampData(
                stampName,
                stampManager.registerStampName(stampName),
                displayName.isEmpty() ? stampName : displayName,
                ArpgUtility.getEnum(stampArpg, EditorSetting.STAMP_FIELD_CATEGORY, StampCategory.class,
                        StampCategory.OTHER),
                rigHandle,
                bonesArpg.deepCopy(),
                partsArpg,
                parseAnimations(stampArpg, rigHandle, stampName),
                mesh,
                plugged ? parsePlug(stampArpg, rigHandle, stampName) : null,
                parseSockets(stampArpg, rigHandle, stampName),
                fur ? parseFur(stampArpg.getAsObject(EditorSetting.STAMP_FIELD_FUR), stampName) : null);
    }

    // The stamp's animations as the file holds them, every bone each names checked to be one of the stamp's own
    private ArpgObjectStruct parseAnimations(ArpgObjectStruct stampArpg, RigHandle rigHandle, String stampName) {

        if (!ArpgUtility.hasObject(stampArpg, EditorSetting.STAMP_FIELD_ANIMATIONS))
            return new ArpgObjectStruct();

        ArpgObjectStruct animationsArpg = stampArpg.getAsObject(EditorSetting.STAMP_FIELD_ANIMATIONS).deepCopy();

        for (String clipName : animationsArpg.keySet()) {

            ArpgObjectStruct animationArpg = animationsArpg.getAsObject(clipName);

            for (int i = 0; i < EditorSetting.STAMP_ANIMATION_GROUPS.length; i++)
                if (ArpgUtility.hasObject(animationArpg, EditorSetting.STAMP_ANIMATION_GROUPS[i]))
                    checkTrackBones(
                            animationArpg.getAsObject(EditorSetting.STAMP_ANIMATION_GROUPS[i]),
                            rigHandle,
                            clipName,
                            stampName);
        }

        return animationsArpg;
    }

    private void checkTrackBones(ArpgObjectStruct tracksArpg, RigHandle rigHandle, String clipName, String stampName) {

        for (String boneName : tracksArpg.keySet())
            if (boneName.equals(EditorSetting.STAMP_HOST_BONE) || !rigHandle.hasBone(boneName))
                throwException("Stamp animation \"" + clipName + "\" keys \"" + boneName
                        + "\", which is not one of its bones. Source: " + stampName);
    }

    private StampSocketStruct parsePlug(ArpgObjectStruct stampArpg, RigHandle rigHandle, String stampName) {
        return parseSocket(stampArpg.getAsObject(EditorSetting.STAMP_FIELD_PLUG), rigHandle, false, stampName);
    }

    private StampSocketStruct[] parseSockets(ArpgObjectStruct stampArpg, RigHandle rigHandle, String stampName) {

        if (!ArpgUtility.hasArray(stampArpg, EditorSetting.STAMP_FIELD_SOCKETS))
            return new StampSocketStruct[0];

        ArpgArrayStruct socketsArpg = stampArpg.getAsArray(EditorSetting.STAMP_FIELD_SOCKETS);
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

        String socketName = socket
                ? ArpgUtility.validateString(socketArpg, EditorSetting.STAMP_FIELD_NAME)
                : EditorSetting.STAMP_HOST_BONE;
        String boneName = socket
                ? ArpgUtility.validateString(socketArpg, EditorSetting.STAMP_FIELD_BONE)
                : EditorSetting.STAMP_HOST_BONE;

        if (!rigHandle.hasBone(boneName))
            throwException("Stamp socket \"" + socketName + "\" rides \"" + boneName
                    + "\", which is not one of its bones. Source: " + stampName);

        Vector3 normal = toVector(ArpgUtility.validateObject(socketArpg, EditorSetting.STAMP_FIELD_NORMAL));

        if (normal.length() <= 0f)
            throwException("Stamp socket \"" + socketName + "\" faces no way: its normal has no length. Source: "
                    + stampName);

        float radius = ArpgUtility.validateFloat(socketArpg, EditorSetting.STAMP_FIELD_RADIUS);

        if (radius <= 0f)
            throwException("Stamp socket \"" + socketName + "\" needs a radius above 0. Source: " + stampName);

        return new StampSocketStruct(
                socketName,
                rigHandle.getBoneIndex(boneName),
                toVector(ArpgUtility.validateObject(socketArpg, EditorSetting.STAMP_FIELD_POSITION)),
                normal.normalize(),
                radius);
    }

    // How a fur stamp lays its card: a patch and a spacing above nothing, a jitter from none to all
    private StampFurStruct parseFur(ArpgObjectStruct furArpg, String stampName) {

        float radius = ArpgUtility.validateFloat(furArpg, EditorSetting.STAMP_FIELD_RADIUS);
        float spacing = ArpgUtility.validateFloat(furArpg, EditorSetting.STAMP_FIELD_SPACING);
        float jitter = ArpgUtility.getFloat(furArpg, EditorSetting.STAMP_FIELD_JITTER,
                EditorSetting.STAMP_FUR_JITTER_DEFAULT);

        if (radius <= 0f || spacing <= 0f)
            throwException("Stamp fur needs a radius and a spacing above 0. Source: " + stampName);

        if (jitter < 0f || jitter > 1f)
            throwException("Stamp fur jitter must lie from 0 to 1. Source: " + stampName);

        return new StampFurStruct(radius, spacing, jitter);
    }

    private Vector3 toVector(ArpgObjectStruct vectorArpg) {
        return new Vector3(
                ArpgUtility.validateFloat(vectorArpg, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[0]),
                ArpgUtility.validateFloat(vectorArpg, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[1]),
                ArpgUtility.validateFloat(vectorArpg, EditorSetting.CHARACTER_VIEW_AXIS_NAMES[2]));
    }
}
