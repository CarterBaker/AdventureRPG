package editor.bootstrap.stamppipeline.util;

import editor.runtime.EditorSetting;
import engine.root.EngineUtility;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StampRigUtility extends EngineUtility {

    /*
     * Stateless rig work every stamp shares: a stamp's bones laid out as a rig
     * of their own, the host placeholder first at the origin and every bone
     * naming no parent under it, each bone checked to be listed once and
     * after its parent, so a stamp read from its file and one cut from a
     * character are built on the same rig.
     */

    // The stamp's bones as a rig of their own
    public static ArpgObjectStruct toRigArpg(ArpgArrayStruct bonesArpg, String sourceName) {

        ArpgObjectStruct rigArpg = new ArpgObjectStruct();
        ArpgArrayStruct rigBones = new ArpgArrayStruct();
        ArpgObjectStruct hostArpg = new ArpgObjectStruct();
        ObjectArrayList<String> names = new ObjectArrayList<>();

        ArpgObjectStruct origin = new ArpgObjectStruct();

        origin.addProperty("x", 0f);
        origin.addProperty("y", 0f);
        origin.addProperty("z", 0f);

        hostArpg.addProperty("name", EditorSetting.STAMP_HOST_BONE);
        hostArpg.add("position", origin);
        rigBones.add(hostArpg);
        names.add(EditorSetting.STAMP_HOST_BONE);

        for (int i = 0; i < bonesArpg.size(); i++) {

            ArpgObjectStruct boneArpg = bonesArpg.get(i).getAsObject().deepCopy();
            String boneName = ArpgUtility.validateString(boneArpg, "name");
            String parentName = ArpgUtility.getString(boneArpg, "parent", "");

            if (names.contains(boneName))
                throwException("Stamp bone \"" + boneName + "\" is listed twice or takes the host's name. Source: "
                        + sourceName);

            if (!parentName.isEmpty() && !names.contains(parentName))
                throwException("Stamp bone \"" + boneName + "\" hangs from \"" + parentName
                        + "\", which is not listed before it. Source: " + sourceName);

            boneArpg.addProperty("parent", parentName.isEmpty() ? EditorSetting.STAMP_HOST_BONE : parentName);
            rigBones.add(boneArpg);
            names.add(boneName);
        }

        rigArpg.add("bones", rigBones);
        return rigArpg;
    }
}
