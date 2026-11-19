package application.bootstrap.animationpipeline.animationmanager;

import java.io.File;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class AnimationManager extends ManagerPackage {

    /*
     * Owns the animation clip palette for the engine lifetime. A clip is a
     * self-contained, rig-bound track set — runtime playback position lives
     * on each entity's AnimationStateHandle, never here. Clip IDs are
     * assigned in registration order. Auto-triggers an on-demand load via
     * AnimationLoader on a name-based cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> clipName2ClipID;
    private ObjectArrayList<AnimationClipHandle> clipID2ClipHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.clipName2ClipID = RegistryUtility.createNameIndex();
        this.clipID2ClipHandle = RegistryUtility.createPalette();
        create(AnimationLoader.class);
    }

    // Management \\

    void addClip(String clipName, AnimationClipHandle handle) {
        RegistryUtility.registerHandle(
                clipName2ClipID, clipID2ClipHandle, clipName, handle, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // On-Demand \\

    // A clip file written while the game runs, loaded by name when first asked for
    public void addAnimationFile(String name, File file) {
        ((AnimationLoader) internalLoader).registerFile(name, file);
    }

    // Accessible \\

    public boolean hasClip(String clipName) {
        return RegistryUtility.getHandle(clipName2ClipID, clipID2ClipHandle, clipName) != null;
    }

    public short getClipIDFromClipName(String clipName) {

        if (!hasClip(clipName))
            ((AnimationLoader) internalLoader).request(clipName);

        if (!hasClip(clipName))
            throwException("Animation clip could not be loaded: \"" + clipName + "\"");

        return (short) clipName2ClipID.getInt(clipName);
    }

    public AnimationClipHandle getClipHandleFromClipID(short clipID) {
        return RegistryUtility.getHandle(clipID2ClipHandle, clipID);
    }

    public AnimationClipHandle getClipHandleFromClipName(String clipName) {
        return getClipHandleFromClipID(getClipIDFromClipName(clipName));
    }

    // Preview \\

    // Builds a clip from an ARPG tree without registering it, so an editor can play an edit before it is saved
    public AnimationClipHandle buildPreviewClip(String clipName, ArpgObjectStruct clipArpg) {
        return ((AnimationLoader) internalLoader).buildPreview(clipName, clipArpg);
    }

    // A registered clip built again in place from an edited tree, so every state playing it plays the edit at once,
    // the way a rig applies live
    public void updateClip(String clipName, ArpgObjectStruct clipArpg) {

        if (hasClip(clipName))
            getClipHandleFromClipName(clipName).constructor(buildPreviewClip(clipName, clipArpg).getClipData());
    }
}
