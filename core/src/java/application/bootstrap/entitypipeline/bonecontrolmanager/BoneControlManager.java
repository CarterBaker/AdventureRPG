package application.bootstrap.entitypipeline.bonecontrolmanager;

import application.bootstrap.entitypipeline.bonecontrol.BoneControlHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BoneControlManager extends ManagerPackage {

    /*
     * Owns the controls of every rig for the engine lifetime, one entry per
     * rig, named after it. Races resolve their rig's controls through here
     * while they load, and the character editor hands a rig's controls in
     * again whenever it changes them, so the races it previews next follow.
     * A rig without a controls file simply has none.
     */

    // Palette
    private Object2IntOpenHashMap<String> rigName2BoneControlID;
    private ObjectArrayList<BoneControlHandle> boneControlID2BoneControlHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.rigName2BoneControlID = RegistryUtility.createNameIndex();
        this.boneControlID2BoneControlHandle = RegistryUtility.createPalette();

        create(BoneControlLoader.class);
    }

    // Management \\

    short registerRigName(String rigName) {
        return (short) RegistryUtility.registerID(
                rigName2BoneControlID,
                boneControlID2BoneControlHandle,
                rigName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addBoneControlHandle(BoneControlHandle handle) {
        boneControlID2BoneControlHandle.set(handle.getBoneControlID(), handle);
    }

    // The controls of a rig as an editor has just changed them, taken up in place, or registered when the rig had none
    public void updateBoneControls(String rigName, ArpgObjectStruct boneControlArpg) {

        ((BoneControlLoader) internalLoader).update(getBoneControlHandle(rigName), boneControlArpg, rigName);
    }

    // Accessible \\

    // The controls of a rig, null for a rig with none
    public BoneControlHandle getBoneControlHandle(String rigName) {
        return RegistryUtility.getHandle(rigName2BoneControlID, boneControlID2BoneControlHandle, rigName);
    }
}
