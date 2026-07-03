package application.bootstrap.animationpipeline.animationmanager;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class AnimationManager extends ManagerPackage {

    /*
     * Owns the animation clip palette for the engine lifetime. A clip is a
     * self-contained, rig-bound track set — runtime playback position lives
     * on each entity's AnimationStateHandle, never here. Auto-triggers an
     * on-demand load via InternalLoader on a name-based cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, AnimationClipHandle> clipName2ClipHandle;
    private Short2ObjectOpenHashMap<AnimationClipHandle> clipID2ClipHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.clipName2ClipHandle = new Object2ObjectOpenHashMap<>();
        this.clipID2ClipHandle = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addClip(String clipName, AnimationClipHandle handle) {

        short id = RegistryUtility.toShortID(clipName);

        clipName2ClipHandle.put(clipName, handle);
        clipID2ClipHandle.put(id, handle);
    }

    // Accessible \\

    public boolean hasClip(String clipName) {
        return clipName2ClipHandle.containsKey(clipName);
    }

    public AnimationClipHandle getClipHandleFromClipID(short clipID) {
        return clipID2ClipHandle.get(clipID);
    }

    public AnimationClipHandle getClipHandleFromClipName(String clipName) {

        AnimationClipHandle handle = clipName2ClipHandle.get(clipName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(clipName);
            handle = clipName2ClipHandle.get(clipName);
        }

        if (handle == null)
            throwException("Animation clip could not be loaded: \"" + clipName + "\"");

        return handle;
    }
}