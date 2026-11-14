package editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager;

import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationTreeRowBranch extends BranchPackage {

    /*
     * Lists the rows of the animation tree editor's two panels. The left
     * panel holds the simulation — the character, the movement state, every
     * parameter and an action to play — then every layer of the tree and the
     * nodes of the selected one. The right panel shows the selected layer as
     * the editor's view asks: its blend, its selected node, the node each
     * state and action enters, its bone mask, or its transitions. The base
     * layer shows only what the builder lets a base layer declare. Every
     * value shown is read through AnimationTreePropertyBranch, the same path
     * that edits it.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private AnimationTreePropertyBranch animationTreePropertyBranch;

    // Base \\

    @Override
    protected void get() {
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
        this.animationTreePropertyBranch = get(AnimationTreePropertyBranch.class);
    }

    // List \\

    void buildListRows(TreeDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        addSimulationRows(rows);
        addLayerRows(rows);

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_NODES));

        ObjectArrayList<String> nodeNames = animationTreePropertyBranch.getNodeNames();

        for (int i = 0; i < nodeNames.size(); i++)
            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE, nodeNames.get(i)),
                    nodeNames.get(i),
                    nodeNames.get(i).equals(animationTreeEditorManager.getSelectedNodeName())));
    }

    private void addSimulationRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_SIMULATE));
        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_CHARACTER,
                EditorSetting.ANIMATION_EDITOR_LABEL_CHARACTER);
        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_STATE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_STATE);

        for (AnimationParameter parameter : AnimationParameter.VALUES) {

            String key = CharacterViewUtility.toKey(
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER,
                    parameter.ordinal());

            addNumber(rows, key, ArpgUtility.toEnumName(parameter));
        }

        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_ACTION,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ACTION);
        rows.add(CharacterViewRowStruct.action(
                EditorSetting.ANIMATION_TREE_EDITOR_KEY_PLAY_ACTION,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_PLAY_ACTION));
    }

    private void addLayerRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_LAYERS));

        for (int i = 0; i < animationTreePropertyBranch.getLayerCount(); i++) {

            ArpgObjectStruct layer = animationTreePropertyBranch.getLayerArpg(i);
            String label = ArpgUtility.getString(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NAME, "")
                    + EditorSetting.ANIMATION_EDITOR_KEY_COUNT_OPEN
                    + (i == 0
                            ? EditorSetting.ANIMATION_TREE_EDITOR_LABEL_BASE
                            : ArpgUtility.getString(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MODE,
                                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_OVERRIDE))
                    + EditorSetting.ANIMATION_EDITOR_KEY_COUNT_CLOSE;

            rows.add(CharacterViewRowStruct.item(
                    CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER, i),
                    label,
                    i == animationTreeEditorManager.getSelectedLayerIndex()));
        }
    }

    // Properties \\

    void buildPropertyRows(TreeDocumentInstance document, ObjectArrayList<CharacterViewRowStruct> rows) {

        if (animationTreePropertyBranch.getLayerArpg() == null) {
            rows.add(CharacterViewRowStruct.action(
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_LAYER,
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ADD_LAYER));
            return;
        }

        boolean base = animationTreeEditorManager.getSelectedLayerIndex() == 0;

        switch (animationTreeEditorManager.getView()) {
            case LAYER -> addLayerPropertyRows(rows, base);
            case NODE -> addNodePropertyRows(rows);
            case STATES -> addMapRows(rows, base);
            case MASK -> addMaskRows(rows, base);
            case TRANSITIONS -> addTransitionRows(rows);
        }
    }

    private void addLayerPropertyRows(ObjectArrayList<CharacterViewRowStruct> rows, boolean base) {

        rows.add(CharacterViewRowStruct.header(animationTreeEditorManager.getSelectedLayerName()));
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_LAYER,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ADD_LAYER);
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_RENAME_LAYER,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_RENAME_LAYER);
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_LAYER,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_REMOVE_LAYER);
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_UP,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_LAYER_UP);
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_DOWN,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_LAYER_DOWN);

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_BLEND));

        if (!base) {
            addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_MODE,
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_MODE);
            addNumber(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT,
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_WEIGHT);
        }

        addNumber(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_BLEND,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_BLEND);
        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_DEFAULT,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_DEFAULT);
    }

    private void addNodePropertyRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_NODE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ADD_NODE);

        if (animationTreePropertyBranch.getNodeArpg() == null)
            return;

        rows.add(CharacterViewRowStruct.header(animationTreeEditorManager.getSelectedNodeName()));
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_RENAME_NODE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_RENAME_NODE);
        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_NODE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_REMOVE_NODE);
        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_KIND,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_KIND);

        if (animationTreePropertyBranch.isBlendSpace())
            addBlendSpaceRows(rows);
        else
            addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_CLIP,
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_CLIP);

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_PLAYBACK));
        addNumber(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_RATE);
        addNumber(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_BLEND,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_BLEND);
        addNumber(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_DELAY,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_DELAY);
    }

    private void addBlendSpaceRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_PARAMETER,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_PARAMETER);
        addChoice(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE_SCALE,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_RATE_SCALE);

        for (int i = 0; i < animationTreePropertyBranch.getSpaceCount(); i++) {

            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_SPACE_CLIP + (i + 1)));
            addChoice(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_CLIP, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_CLIP);
            addNumber(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_VALUE);

            if (animationTreePropertyBranch.getSpaceCount() > 1)
                addAction(rows, CharacterViewUtility.toKey(
                        EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_SPACE_CLIP,
                        i),
                        EditorSetting.ANIMATION_TREE_EDITOR_LABEL_REMOVE_SPACE_CLIP);
        }

        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_SPACE_CLIP,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ADD_SPACE_CLIP);
    }

    private void addMapRows(ObjectArrayList<CharacterViewRowStruct> rows, boolean base) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_STATES));

        for (EntityState state : EntityState.VALUES) {

            String stateName = ArpgUtility.toEnumName(state);
            addChoice(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_STATE, stateName),
                    stateName);
        }

        if (base)
            return;

        rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_ACTIONS));

        for (EntityAction action : EntityAction.VALUES) {

            if (action == EntityAction.NONE)
                continue;

            String actionName = ArpgUtility.toEnumName(action);
            addChoice(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_ACTION, actionName),
                    actionName);
        }
    }

    private void addMaskRows(ObjectArrayList<CharacterViewRowStruct> rows, boolean base) {

        RigHandle rigHandle = animationTreePropertyBranch.resolveRig();

        if (base) {
            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_BASE_MASK));
            return;
        }

        if (rigHandle == null)
            return;

        rows.add(CharacterViewRowStruct.header(animationTreePropertyBranch.hasMask()
                ? EditorSetting.ANIMATION_TREE_EDITOR_HEADER_MASK
                : EditorSetting.ANIMATION_TREE_EDITOR_HEADER_NO_MASK));

        for (int i = 0; i < rigHandle.getBoneCount(); i++) {

            String boneName = rigHandle.getBone(i).getName();
            addNumber(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK, boneName),
                    boneName);
        }

        if (animationTreePropertyBranch.hasMask())
            addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_CLEAR_MASK,
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_CLEAR_MASK);
    }

    private void addTransitionRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        addAction(rows, EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_TRANSITION,
                EditorSetting.ANIMATION_TREE_EDITOR_LABEL_ADD_TRANSITION);

        for (int i = 0; i < animationTreePropertyBranch.getTransitionCount(); i++) {

            rows.add(CharacterViewRowStruct.header(EditorSetting.ANIMATION_TREE_EDITOR_HEADER_TRANSITION + (i + 1)));
            addChoice(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_FROM, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_FROM);
            addChoice(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_TO, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_TO);
            addNumber(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_BLEND, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_BLEND);
            addAction(rows, CharacterViewUtility.toKey(EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_TRANSITION, i),
                    EditorSetting.ANIMATION_TREE_EDITOR_LABEL_REMOVE_TRANSITION);
        }
    }

    // Utility \\

    private void addNumber(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.number(key, label, animationTreePropertyBranch.getValueText(key)));
    }

    private void addChoice(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.choice(key, label, animationTreePropertyBranch.getValueText(key)));
    }

    private void addAction(ObjectArrayList<CharacterViewRowStruct> rows, String key, String label) {
        rows.add(CharacterViewRowStruct.action(key, label));
    }
}
