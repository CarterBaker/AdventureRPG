package application.bootstrap.entitypipeline.animationtreemanager;

import application.bootstrap.entitypipeline.animationtree.AnimationTreeHandle;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class AnimationTreeManager extends ManagerPackage {

    /*
     * Owns the animation tree palette for the engine lifetime. A tree is the
     * JSON graph between an entity's movement state and the clips it plays
     * — which node each state enters, how nodes blend, and which bones each
     * layer touches. Runtime playback lives on each entity's
     * AnimationStateHandle, never here. Auto-triggers an on-demand load via
     * AnimationTreeLoader on a name-based cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, AnimationTreeHandle> treeName2AnimationTreeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.treeName2AnimationTreeHandle = new Object2ObjectOpenHashMap<>();
        create(AnimationTreeLoader.class);
    }

    // Management \\

    void addAnimationTree(AnimationTreeHandle animationTreeHandle) {
        treeName2AnimationTreeHandle.put(animationTreeHandle.getTreeName(), animationTreeHandle);
    }

    // Accessible \\

    public boolean hasAnimationTree(String treeName) {
        return treeName2AnimationTreeHandle.containsKey(treeName);
    }

    public AnimationTreeHandle getAnimationTreeHandleFromTreeName(String treeName) {

        AnimationTreeHandle handle = treeName2AnimationTreeHandle.get(treeName);

        if (handle == null) {
            ((AnimationTreeLoader) internalLoader).request(treeName);
            handle = treeName2AnimationTreeHandle.get(treeName);
        }

        if (handle == null)
            throwException("Animation tree could not be loaded: \"" + treeName + "\"");

        return handle;
    }
}
