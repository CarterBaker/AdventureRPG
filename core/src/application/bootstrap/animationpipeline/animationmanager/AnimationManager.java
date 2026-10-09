package application.bootstrap.animationpipeline.animationmanager;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
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
}
