package editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager;

import java.util.function.Consumer;

import application.bootstrap.entitypipeline.animationtree.AnimationBlendMode;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationTreeEditBranch extends BranchPackage {

    /*
     * Performs every change the animation tree editor makes to a tree, each
     * through InfoManager so the Info Panel, the preview and every tab see it
     * at once. Edits write only the fields they change and keep every other
     * field and its order, and leave a field at its default out, as authored
     * trees do: an override layer names no mode and a node not scaled by its
     * parameter names no rate scale. Renaming a node carries the new name to
     * every state, action, default and transition that names it; removing one
     * takes those with it. A states, actions or transitions block emptied of
     * its last entry is removed.
     */

    // Internal
    private InfoManager infoManager;

    // Base \\

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    // Tree \\

    private void edit(TreeDocumentInstance document, Consumer<ArpgObjectStruct> editor) {
        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_ANIMATION_TREES,
                document.getTreeName(),
                document.getTreeName(),
                editor);
    }

    private void editLayer(TreeDocumentInstance document, int layerIndex, Consumer<ArpgObjectStruct> editor) {
        edit(document, tree -> editor.accept(getLayers(tree).get(layerIndex).getAsObject()));
    }

    private void editNode(
            TreeDocumentInstance document,
            int layerIndex,
            String nodeName,
            Consumer<ArpgObjectStruct> editor) {

        editLayer(document, layerIndex, layer -> editor.accept(
                layer.getAsObject(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES).getAsObject(nodeName)));
    }

    private ArpgArrayStruct getLayers(ArpgObjectStruct tree) {
        return CharacterViewUtility.requireArray(tree, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_LAYERS);
    }

    // Layers \\

    void addLayer(TreeDocumentInstance document, String layerName) {
        edit(document, tree -> {

            ArpgObjectStruct layer = new ArpgObjectStruct();
            layer.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NAME, layerName);
            layer.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES, new ArpgObjectStruct());
            getLayers(tree).add(layer);
        });
    }

    void removeLayer(TreeDocumentInstance document, int layerIndex) {
        edit(document, tree -> getLayers(tree).remove(layerIndex));
    }

    void swapLayers(TreeDocumentInstance document, int layerIndex, int otherIndex) {
        edit(document, tree -> {

            ArpgArrayStruct layers = getLayers(tree);
            ArpgElementStruct layer = layers.get(layerIndex);

            layers.set(layerIndex, layers.get(otherIndex));
            layers.set(otherIndex, layer);
        });
    }

    void renameLayer(TreeDocumentInstance document, int layerIndex, String layerName) {
        editLayer(document, layerIndex, layer -> layer.addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NAME,
                layerName));
    }

    void setLayerMode(TreeDocumentInstance document, int layerIndex, AnimationBlendMode blendMode) {
        editLayer(document, layerIndex, layer -> {

            if (blendMode == AnimationBlendMode.OVERRIDE)
                layer.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MODE);
            else
                layer.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MODE, ArpgUtility.toEnumName(blendMode));
        });
    }

    void setLayerNumber(TreeDocumentInstance document, int layerIndex, String field, float value) {
        editLayer(document, layerIndex, layer -> layer.addProperty(field, CharacterViewUtility.round(value)));
    }

    void setLayerDefault(TreeDocumentInstance document, int layerIndex, String nodeName) {
        editLayer(document, layerIndex, layer -> putOrRemove(
                layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DEFAULT, nodeName));
    }

    // Nodes \\

    void addNode(TreeDocumentInstance document, int layerIndex, String nodeName, String clipName) {
        editLayer(document, layerIndex, layer -> {

            ArpgObjectStruct nodes = CharacterViewUtility.requireObject(
                    layer,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES);
            ArpgObjectStruct node = new ArpgObjectStruct();

            node.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, clipName);
            nodes.add(nodeName, node);
        });
    }

    // Rebuilds the node map in its order under the new name, and renames every reference to it
    void renameNode(TreeDocumentInstance document, int layerIndex, String nodeName, String renamed) {
        editLayer(document, layerIndex, layer -> {

            ArpgObjectStruct nodes = layer.getAsObject(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES);
            ArpgObjectStruct reordered = new ArpgObjectStruct();

            for (String key : nodes.keySet())
                reordered.add(key.equals(nodeName) ? renamed : key, nodes.get(key));

            layer.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES, reordered);
            replaceReferences(layer, nodeName, renamed);
        });
    }

    void removeNode(TreeDocumentInstance document, int layerIndex, String nodeName) {
        editLayer(document, layerIndex, layer -> {

            layer.getAsObject(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES).remove(nodeName);
            replaceReferences(layer, nodeName, null);
        });
    }

    void setNodeClip(TreeDocumentInstance document, int layerIndex, String nodeName, String clipName) {
        editNode(document, layerIndex, nodeName, node -> node.addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP,
                clipName));
    }

    // A single clip becomes the only entry of a blend space along a parameter, and a blend space its first clip
    void setNodeBlended(
            TreeDocumentInstance document,
            int layerIndex,
            String nodeName,
            boolean blended,
            String parameterName) {

        editNode(document, layerIndex, nodeName, node -> {

            if (blended)
                toBlendSpace(node, parameterName);
            else
                toSingleClip(node);
        });
    }

    private void toBlendSpace(ArpgObjectStruct node, String parameterName) {

        ArpgArrayStruct clips = new ArpgArrayStruct();
        clips.add(toSpaceEntry(ArpgUtility.getString(node, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, ""), 0f));

        node.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP);
        node.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_PARAMETER, parameterName);
        node.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS, clips);
    }

    private void toSingleClip(ArpgObjectStruct node) {

        ArpgArrayStruct clips = node.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS);
        String clipName = clips.isEmpty()
                ? ""
                : ArpgUtility.getString(clips.get(0).getAsObject(), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, "");

        node.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS);
        node.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_PARAMETER);
        node.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE_SCALE);
        node.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, clipName);
    }

    void setNodeParameter(TreeDocumentInstance document, int layerIndex, String nodeName, String parameterName) {
        editNode(document, layerIndex, nodeName, node -> node.addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_PARAMETER,
                parameterName));
    }

    void setNodeNumber(TreeDocumentInstance document, int layerIndex, String nodeName, String field, float value) {
        editNode(document, layerIndex, nodeName, node -> node.addProperty(field, CharacterViewUtility.round(value)));
    }

    void setNodeRateScaled(TreeDocumentInstance document, int layerIndex, String nodeName, boolean rateScaled) {
        editNode(document, layerIndex, nodeName, node -> {

            if (rateScaled)
                node.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE_SCALE, true);
            else
                node.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE_SCALE);
        });
    }

    // Blend Space \\

    void setSpaceClip(TreeDocumentInstance document, int layerIndex, String nodeName, int entry, String clipName) {
        editNode(document, layerIndex, nodeName, node -> getSpaceEntry(node, entry).addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP,
                clipName));
    }

    void setSpaceValue(TreeDocumentInstance document, int layerIndex, String nodeName, int entry, float value) {
        editNode(document, layerIndex, nodeName, node -> getSpaceEntry(node, entry).addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_VALUE,
                CharacterViewUtility.round(value)));
    }

    void addSpaceClip(TreeDocumentInstance document, int layerIndex, String nodeName, String clipName, float value) {
        editNode(document, layerIndex, nodeName, node -> CharacterViewUtility
                .requireArray(node, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS)
                .add(toSpaceEntry(clipName, value)));
    }

    void removeSpaceClip(TreeDocumentInstance document, int layerIndex, String nodeName, int entry) {
        editNode(document, layerIndex, nodeName, node -> node
                .getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS)
                .remove(entry));
    }

    private ArpgObjectStruct getSpaceEntry(ArpgObjectStruct node, int entry) {
        return node.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS).get(entry).getAsObject();
    }

    private ArpgObjectStruct toSpaceEntry(String clipName, float value) {

        ArpgObjectStruct entry = new ArpgObjectStruct();
        entry.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, clipName);
        entry.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_VALUE, CharacterViewUtility.round(value));
        return entry;
    }

    // States \\

    void setMappedNode(TreeDocumentInstance document, int layerIndex, String mapField, String key, String nodeName) {
        editLayer(document, layerIndex, layer -> {

            ArpgObjectStruct map = CharacterViewUtility.requireObject(layer, mapField);
            putOrRemove(map, key, nodeName);

            if (map.isEmpty())
                layer.remove(mapField);
        });
    }

    // Mask \\

    // A layer with no mask touches every bone fully, so the first weight set starts every bone at full
    void setMaskWeight(
            TreeDocumentInstance document,
            int layerIndex,
            String boneName,
            float value,
            ObjectArrayList<String> boneNames) {

        editLayer(document, layerIndex, layer -> {

            if (!ArpgUtility.hasObject(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK)) {

                ArpgObjectStruct mask = new ArpgObjectStruct();

                for (int i = 0; i < boneNames.size(); i++)
                    mask.addProperty(boneNames.get(i), EditorSetting.ANIMATION_TREE_EDITOR_WEIGHT_FULL);

                layer.add(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK, mask);
            }

            layer.getAsObject(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK)
                    .addProperty(boneName, CharacterViewUtility.round(value));
        });
    }

    void clearMask(TreeDocumentInstance document, int layerIndex) {
        editLayer(document, layerIndex, layer -> layer.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK));
    }

    // Transitions \\

    void addTransition(TreeDocumentInstance document, int layerIndex, String fromNode, String toNode, float blend) {
        editLayer(document, layerIndex, layer -> {

            ArpgObjectStruct transition = new ArpgObjectStruct();
            transition.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_FROM, fromNode);
            transition.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TO, toNode);
            transition.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND, CharacterViewUtility.round(blend));

            ArpgArrayStruct transitions = CharacterViewUtility.requireArray(
                    layer,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS);

            transitions.add(transition);
        });
    }

    void setTransitionNode(
            TreeDocumentInstance document,
            int layerIndex,
            int transitionIndex,
            String end,
            String node) {

        editLayer(document, layerIndex, layer -> getTransition(layer, transitionIndex).addProperty(end, node));
    }

    void setTransitionBlend(TreeDocumentInstance document, int layerIndex, int transitionIndex, float blend) {
        editLayer(document, layerIndex, layer -> getTransition(layer, transitionIndex).addProperty(
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND,
                CharacterViewUtility.round(blend)));
    }

    void removeTransition(TreeDocumentInstance document, int layerIndex, int transitionIndex) {
        editLayer(document, layerIndex, layer -> {

            ArpgArrayStruct transitions = layer.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS);
            transitions.remove(transitionIndex);

            if (transitions.isEmpty())
                layer.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS);
        });
    }

    private ArpgObjectStruct getTransition(ArpgObjectStruct layer, int transitionIndex) {
        return layer.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS)
                .get(transitionIndex)
                .getAsObject();
    }

    // References \\

    // Points every state, action, default and transition naming a node at its new name, or drops them for none
    private void replaceReferences(ArpgObjectStruct layer, String nodeName, String renamed) {

        replaceMapReferences(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_STATES, nodeName, renamed);
        replaceMapReferences(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_ACTIONS, nodeName, renamed);

        if (nodeName.equals(ArpgUtility.getString(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DEFAULT, null)))
            putOrRemove(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DEFAULT, renamed);

        if (!ArpgUtility.hasArray(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS))
            return;

        ArpgArrayStruct transitions = layer.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS);

        for (int i = transitions.size() - 1; i >= 0; i--)
            replaceTransitionReferences(transitions, i, nodeName, renamed);

        if (transitions.isEmpty())
            layer.remove(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS);
    }

    private void replaceTransitionReferences(ArpgArrayStruct transitions, int index, String nodeName, String renamed) {

        ArpgObjectStruct transition = transitions.get(index).getAsObject();
        String fromNode = ArpgUtility.getString(transition, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_FROM, "");
        String toNode = ArpgUtility.getString(transition, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TO, "");
        boolean from = nodeName.equals(fromNode);
        boolean to = nodeName.equals(toNode);

        if (!from && !to)
            return;

        if (renamed == null) {
            transitions.remove(index);
            return;
        }

        if (from)
            transition.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_FROM, renamed);

        if (to)
            transition.addProperty(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TO, renamed);
    }

    private void replaceMapReferences(ArpgObjectStruct layer, String mapField, String nodeName, String renamed) {

        if (!ArpgUtility.hasObject(layer, mapField))
            return;

        ArpgObjectStruct map = layer.getAsObject(mapField);
        ObjectArrayList<String> keys = new ObjectArrayList<>(map.keySet());

        for (int i = 0; i < keys.size(); i++)
            if (nodeName.equals(ArpgUtility.getString(map, keys.get(i), "")))
                putOrRemove(map, keys.get(i), renamed);

        if (map.isEmpty())
            layer.remove(mapField);
    }

    private void putOrRemove(ArpgObjectStruct arpg, String key, String value) {

        if (value == null)
            arpg.remove(key);
        else
            arpg.addProperty(key, value);
    }
}
