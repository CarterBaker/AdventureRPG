package editor.bootstrap.animationtreeeditorpipeline.animationtreeeditormanager;

import application.bootstrap.entitypipeline.animationtree.AnimationBlendMode;
import application.bootstrap.entitypipeline.animationtree.AnimationParameter;
import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import editor.bootstrap.animationtreeeditorpipeline.treedocument.TreeDocumentInstance;
import editor.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AnimationTreePropertyBranch extends BranchPackage {

    /*
     * The one place every value in the animation tree editor's panels is
     * read, stepped, typed, cycled or acted on, by the key its row carries.
     * Tree values are read from the live ARPG, defaulting as the builder
     * does, and written through AnimationTreeEditBranch, clamped to the range
     * the builder accepts; simulation values live on the manager and never
     * touch the tree. A node choice steps through the selected layer's nodes,
     * and none where the field may be left out.
     */

    // Internal
    private AnimationTreeEditorManager animationTreeEditorManager;
    private AnimationTreeEditBranch animationTreeEditBranch;

    // Base \\

    @Override
    protected void get() {
        this.animationTreeEditorManager = get(AnimationTreeEditorManager.class);
        this.animationTreeEditBranch = get(AnimationTreeEditBranch.class);
    }

    // Selection \\

    void select(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        if (keyName.equals(EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER))
            animationTreeEditorManager.selectLayer(CharacterViewUtility.getKeyIndex(key));
        else if (keyName.equals(EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE))
            animationTreeEditorManager.selectNode(CharacterViewUtility.getKeyArgument(key));
    }

    // Values \\

    String getValueText(String key) {

        if (isNumberKey(key))
            return hasTarget(key) ? CharacterViewUtility.formatNumber(getNumber(key)) : "";

        return getChoiceText(key);
    }

    boolean isValueValid(String key, String text) {
        return isNumberKey(key) && hasTarget(key) && CharacterViewUtility.isNumber(text);
    }

    void setValue(TreeDocumentInstance document, String key, String text) {
        writeNumber(document, key, CharacterViewUtility.parseNumber(text));
    }

    void step(TreeDocumentInstance document, String key, int direction) {

        if (isNumberKey(key) && hasTarget(key))
            writeNumber(document, key, getNumber(key) + direction * getStep(key));
    }

    // Numbers \\

    private boolean isNumberKey(String key) {
        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_BLEND,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_BLEND,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_DELAY,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_BLEND -> true;
            default -> false;
        };
    }

    // A key names a layer, node, blend space entry, bone or transition that has to exist
    private boolean hasTarget(String key) {

        int index = CharacterViewUtility.getKeyIndex(key);

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER -> index >= 0
                    && index < AnimationParameter.VALUES.length;
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_BLEND,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_DELAY -> getNodeArpg() != null;
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE -> index >= 0 && index < getSpaceCount();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK -> getLayerArpg() != null && resolveRig() != null
                    && resolveRig().hasBone(CharacterViewUtility.getKeyArgument(key));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_BLEND -> index >= 0 && index < getTransitionCount();
            default -> getLayerArpg() != null;
        };
    }

    private float getNumber(String key) {

        int index = CharacterViewUtility.getKeyIndex(key);

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER -> getParameter(index);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT -> getLayerWeight();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_BLEND -> getLayerBlend();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE -> getNodeRate();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_BLEND -> getNodeBlend();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_DELAY -> getNodeDelay();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE -> getSpaceValue(index);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK -> getMaskWeight(
                    CharacterViewUtility.getKeyArgument(key));
            default -> getTransitionBlend(index);
        };
    }

    private void writeNumber(TreeDocumentInstance document, String key, float value) {

        int index = CharacterViewUtility.getKeyIndex(key);

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER -> writeParameter(index, value);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT -> writeLayerNumber(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_WEIGHT,
                    clampWeight(value));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_BLEND -> writeLayerNumber(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND,
                    Math.max(0f, value));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE -> writeNodeNumber(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE,
                    Math.max(EditorSetting.ANIMATION_TREE_EDITOR_RATE_MIN, value));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_BLEND -> writeNodeNumber(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND,
                    Math.max(0f, value));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_DELAY -> writeNodeNumber(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DELAY,
                    Math.max(0f, value));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE -> animationTreeEditBranch.setSpaceValue(
                    document,
                    animationTreeEditorManager.getSelectedLayerIndex(),
                    animationTreeEditorManager.getSelectedNodeName(),
                    index,
                    value);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK -> animationTreeEditBranch.setMaskWeight(
                    document,
                    animationTreeEditorManager.getSelectedLayerIndex(),
                    CharacterViewUtility.getKeyArgument(key),
                    clampWeight(value),
                    getBoneNames());
            default -> animationTreeEditBranch.setTransitionBlend(
                    document,
                    animationTreeEditorManager.getSelectedLayerIndex(),
                    index,
                    Math.max(0f, value));
        }
    }

    private void writeLayerNumber(TreeDocumentInstance document, String field, float value) {
        animationTreeEditBranch.setLayerNumber(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                field,
                value);
    }

    private void writeNodeNumber(TreeDocumentInstance document, String field, float value) {
        animationTreeEditBranch.setNodeNumber(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                field,
                value);
    }

    // The look parameters stay within the angles a body can look; the others take any value
    private void writeParameter(int parameterIndex, float value) {

        AnimationParameter parameter = AnimationParameter.VALUES[parameterIndex];
        float pitchLimit = EditorSetting.ANIMATION_TREE_EDITOR_LOOK_PITCH_LIMIT;
        float yawLimit = EditorSetting.ANIMATION_TREE_EDITOR_LOOK_YAW_LIMIT;
        float written = switch (parameter) {
            case LOOK_PITCH -> CharacterViewUtility.clamp(value, -pitchLimit, pitchLimit);
            case LOOK_YAW -> CharacterViewUtility.clamp(value, -yawLimit, yawLimit);
            default -> value;
        };

        animationTreeEditorManager.setParameter(parameter, CharacterViewUtility.round(written));
    }

    private float getStep(String key) {

        float[] steps = EditorSetting.ANIMATION_TREE_EDITOR_PARAMETER_STEPS;

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PARAMETER -> steps[CharacterViewUtility.getKeyIndex(key)];
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_VALUE -> steps[resolveNodeParameter().ordinal()];
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_WEIGHT,
                    EditorSetting.ANIMATION_TREE_EDITOR_KEY_MASK -> EditorSetting.ANIMATION_TREE_EDITOR_WEIGHT_STEP;
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE -> EditorSetting.ANIMATION_TREE_EDITOR_RATE_STEP;
            default -> EditorSetting.ANIMATION_TREE_EDITOR_BLEND_STEP;
        };
    }

    private float clampWeight(float value) {
        return CharacterViewUtility.clamp(value, 0f, EditorSetting.ANIMATION_TREE_EDITOR_WEIGHT_FULL);
    }

    // Readers \\

    private float getParameter(int parameterIndex) {
        return animationTreeEditorManager.getParameter(AnimationParameter.VALUES[parameterIndex]);
    }

    private float getLayerWeight() {
        return ArpgUtility.getFloat(
                getLayerArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_WEIGHT,
                EngineSetting.DEFAULT_ANIMATION_LAYER_WEIGHT);
    }

    private float getLayerBlend() {
        return ArpgUtility.getFloat(
                getLayerArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND,
                EngineSetting.ANIMATION_BLEND_SECONDS);
    }

    private float getNodeRate() {
        return ArpgUtility.getFloat(
                getNodeArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE,
                EngineSetting.DEFAULT_ANIMATION_NODE_RATE);
    }

    // A node's blend falls back to its layer's, as the builder reads it
    private float getNodeBlend() {
        return ArpgUtility.getFloat(getNodeArpg(), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND, getLayerBlend());
    }

    private float getNodeDelay() {
        return ArpgUtility.getFloat(
                getNodeArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DELAY,
                EngineSetting.DEFAULT_ANIMATION_NODE_DELAY);
    }

    private float getSpaceValue(int entry) {
        return ArpgUtility.getFloat(getSpaceEntry(entry), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_VALUE, 0f);
    }

    private float getTransitionBlend(int transitionIndex) {
        return ArpgUtility.getFloat(
                getTransition(transitionIndex),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_BLEND,
                getLayerBlend());
    }

    // A layer with no mask moves every bone fully; one with a mask moves only the bones it names
    private float getMaskWeight(String boneName) {

        ArpgObjectStruct mask = CharacterViewUtility.findObject(
                getLayerArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK);

        return mask != null
                ? ArpgUtility.getFloat(mask, boneName, 0f)
                : EditorSetting.ANIMATION_TREE_EDITOR_WEIGHT_FULL;
    }

    // Choices \\

    void cycle(TreeDocumentInstance document, String key, int direction) {

        String argument = CharacterViewUtility.getKeyArgument(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_CHARACTER -> animationTreeEditorManager.cycleCharacter(
                    direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_STATE -> cycleMovementState(direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_ACTION -> animationTreeEditorManager.chooseAction(
                    cycleAction(animationTreeEditorManager.getChosenAction(), direction));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_MODE -> toggleLayerMode(document);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_DEFAULT -> cycleLayerDefault(document, direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_KIND -> toggleNodeKind(document);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_CLIP -> cycleNodeClip(document, direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_PARAMETER -> cycleNodeParameter(document, direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE_SCALE -> toggleRateScale(document);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_CLIP -> cycleSpaceClip(document, index, direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_STATE -> cycleMappedNode(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_STATES,
                    argument,
                    direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ACTION -> cycleMappedNode(
                    document,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_ACTIONS,
                    argument,
                    direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_FROM -> cycleTransitionNode(
                    document,
                    index,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_FROM,
                    direction);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_TO -> cycleTransitionNode(
                    document,
                    index,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TO,
                    direction);
            default -> {
            }
        }
    }

    private String getChoiceText(String key) {

        String argument = CharacterViewUtility.getKeyArgument(key);
        int index = CharacterViewUtility.getKeyIndex(key);

        return switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_CHARACTER -> orNone(
                    animationTreeEditorManager.getCharacterName());
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_STATE -> ArpgUtility.toEnumName(
                    animationTreeEditorManager.getMovementState());
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SIMULATED_ACTION -> ArpgUtility.toEnumName(
                    animationTreeEditorManager.getChosenAction());
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_MODE -> ArpgUtility.toEnumName(getLayerMode());
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_DEFAULT -> orNone(
                    getLayerString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DEFAULT));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_KIND -> isBlendSpace()
                    ? EditorSetting.ANIMATION_TREE_EDITOR_KIND_BLEND_SPACE
                    : EditorSetting.ANIMATION_TREE_EDITOR_KIND_SINGLE;
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_CLIP -> orNone(
                    getNodeString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_PARAMETER -> ArpgUtility.toEnumName(
                    resolveNodeParameter());
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_NODE_RATE_SCALE -> isRateScaled()
                    ? EditorSetting.CHARACTER_VIEW_YES
                    : EditorSetting.CHARACTER_VIEW_NO;
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_SPACE_CLIP -> getSpaceClipText(index);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_STATE -> orNone(
                    getMappedNode(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_STATES, argument));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ACTION -> orNone(
                    getMappedNode(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_ACTIONS, argument));
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_FROM -> getTransitionNodeText(
                    index,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_FROM);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_TRANSITION_TO -> getTransitionNodeText(
                    index,
                    EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TO);
            default -> "";
        };
    }

    private String getSpaceClipText(int entry) {

        if (entry < 0 || entry >= getSpaceCount())
            return "";

        return orNone(ArpgUtility.getString(
                getSpaceEntry(entry),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP,
                null));
    }

    private String getTransitionNodeText(int transitionIndex, String end) {

        if (transitionIndex < 0 || transitionIndex >= getTransitionCount())
            return "";

        return orNone(ArpgUtility.getString(getTransition(transitionIndex), end, null));
    }

    private void cycleMovementState(int direction) {

        int current = animationTreeEditorManager.getMovementState().ordinal();
        int next = Math.floorMod(current + direction, EntityState.VALUES.length);

        animationTreeEditorManager.setMovementState(EntityState.VALUES[next]);
    }

    private void toggleLayerMode(TreeDocumentInstance document) {

        AnimationBlendMode blendMode = getLayerMode() == AnimationBlendMode.OVERRIDE
                ? AnimationBlendMode.ADDITIVE
                : AnimationBlendMode.OVERRIDE;

        animationTreeEditBranch.setLayerMode(document, animationTreeEditorManager.getSelectedLayerIndex(), blendMode);
    }

    private void cycleLayerDefault(TreeDocumentInstance document, int direction) {

        String current = getLayerString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_DEFAULT);

        animationTreeEditBranch.setLayerDefault(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                cycleNode(current, direction, true));
    }

    // A single clip becomes a blend space along speed, as most blend spaces are; a blend space keeps its first clip
    private void toggleNodeKind(TreeDocumentInstance document) {
        animationTreeEditBranch.setNodeBlended(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                !isBlendSpace(),
                ArpgUtility.toEnumName(AnimationParameter.SPEED));
    }

    private void cycleNodeClip(TreeDocumentInstance document, int direction) {

        String current = getNodeString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP);

        animationTreeEditBranch.setNodeClip(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                cycleClip(current, direction));
    }

    private void cycleNodeParameter(TreeDocumentInstance document, int direction) {

        int next = Math.floorMod(resolveNodeParameter().ordinal() + direction, AnimationParameter.VALUES.length);

        animationTreeEditBranch.setNodeParameter(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                ArpgUtility.toEnumName(AnimationParameter.VALUES[next]));
    }

    private void toggleRateScale(TreeDocumentInstance document) {
        animationTreeEditBranch.setNodeRateScaled(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                !isRateScaled());
    }

    private void cycleMappedNode(TreeDocumentInstance document, String mapField, String key, int direction) {
        animationTreeEditBranch.setMappedNode(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                mapField,
                key,
                cycleNode(getMappedNode(mapField, key), direction, true));
    }

    // The next node of the selected layer, with none between the last and the first where none is allowed
    private String cycleNode(String current, int direction, boolean allowNone) {

        ObjectArrayList<String> options = new ObjectArrayList<>(getNodeNames());

        if (allowNone)
            options.add(0, null);

        if (options.isEmpty())
            return current;

        int index = options.indexOf(current);
        return options.get(Math.floorMod(index + direction, options.size()));
    }

    private String cycleClip(String current, int direction) {

        ObjectArrayList<String> clipNames = animationTreeEditorManager.getClipNames();

        if (clipNames.isEmpty())
            return current;

        int index = clipNames.indexOf(current);
        return clipNames.get(Math.floorMod(index + direction, clipNames.size()));
    }

    private EntityAction cycleAction(EntityAction current, int direction) {

        int count = EntityAction.VALUES.length - 1;
        int index = Math.floorMod(current.ordinal() - 1 + direction, count);

        return EntityAction.VALUES[index + 1];
    }

    private void cycleSpaceClip(TreeDocumentInstance document, int entry, int direction) {

        if (entry < 0 || entry >= getSpaceCount())
            return;

        String current = ArpgUtility.getString(
                getSpaceEntry(entry),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP,
                null);

        animationTreeEditBranch.setSpaceClip(
                document,
                animationTreeEditorManager.getSelectedLayerIndex(),
                animationTreeEditorManager.getSelectedNodeName(),
                entry,
                cycleClip(current, direction));
    }

    private void cycleTransitionNode(TreeDocumentInstance document, int transitionIndex, String end, int direction) {

        if (transitionIndex < 0 || transitionIndex >= getTransitionCount())
            return;

        String current = ArpgUtility.getString(getTransition(transitionIndex), end, null);
        String next = cycleNode(current, direction, false);

        if (next != null)
            animationTreeEditBranch.setTransitionNode(
                    document, animationTreeEditorManager.getSelectedLayerIndex(), transitionIndex, end, next);
    }

    // Actions \\

    void act(TreeDocumentInstance document, String key) {

        int index = CharacterViewUtility.getKeyIndex(key);
        int layerIndex = animationTreeEditorManager.getSelectedLayerIndex();
        String nodeName = animationTreeEditorManager.getSelectedNodeName();

        switch (CharacterViewUtility.getKeyName(key)) {
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_PLAY_ACTION -> animationTreeEditorManager.playAction();
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_LAYER -> removeLayer(document, layerIndex);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_UP -> moveLayer(document, layerIndex, -1);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_LAYER_DOWN -> moveLayer(document, layerIndex, 1);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_NODE -> {
                if (nodeName != null)
                    animationTreeEditBranch.removeNode(document, layerIndex, nodeName);
            }
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_SPACE_CLIP -> addSpaceClip(document, layerIndex, nodeName);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_SPACE_CLIP -> {
                if (index >= 0 && index < getSpaceCount() && getSpaceCount() > 1)
                    animationTreeEditBranch.removeSpaceClip(document, layerIndex, nodeName, index);
            }
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_CLEAR_MASK -> {
                if (getLayerArpg() != null)
                    animationTreeEditBranch.clearMask(document, layerIndex);
            }
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_ADD_TRANSITION -> addTransition(document, layerIndex);
            case EditorSetting.ANIMATION_TREE_EDITOR_KEY_REMOVE_TRANSITION -> {
                if (index >= 0 && index < getTransitionCount())
                    animationTreeEditBranch.removeTransition(document, layerIndex, index);
            }
            default -> {
            }
        }
    }

    private void removeLayer(TreeDocumentInstance document, int layerIndex) {

        if (getLayerCount() <= 1) {
            animationTreeEditorManager.setStatusMessage(EditorSetting.ANIMATION_TREE_EDITOR_MESSAGE_LAST_LAYER);
            return;
        }

        animationTreeEditBranch.removeLayer(document, layerIndex);
        animationTreeEditorManager.selectLayer(Math.min(layerIndex, getLayerCount() - 1));
    }

    private void moveLayer(TreeDocumentInstance document, int layerIndex, int direction) {

        int otherIndex = layerIndex + direction;

        if (otherIndex < 0 || otherIndex >= getLayerCount())
            return;

        animationTreeEditBranch.swapLayers(document, layerIndex, otherIndex);
        animationTreeEditorManager.selectLayer(otherIndex);
    }

    // A new entry plays the last entry's clip a step further along the parameter
    private void addSpaceClip(TreeDocumentInstance document, int layerIndex, String nodeName) {

        if (!isBlendSpace())
            return;

        int count = getSpaceCount();
        ObjectArrayList<String> clipNames = animationTreeEditorManager.getClipNames();
        String clipName = count > 0
                ? ArpgUtility.getString(getSpaceEntry(count - 1), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIP, "")
                : clipNames.isEmpty() ? "" : clipNames.get(0);
        float value = count > 0
                ? ArpgUtility.getFloat(getSpaceEntry(count - 1), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_VALUE, 0f)
                        + EditorSetting.ANIMATION_TREE_EDITOR_PARAMETER_STEPS[resolveNodeParameter().ordinal()]
                : 0f;

        animationTreeEditBranch.addSpaceClip(document, layerIndex, nodeName, clipName, value);
    }

    // A new transition runs from the selected node to the first other node, at the layer's blend
    private void addTransition(TreeDocumentInstance document, int layerIndex) {

        ObjectArrayList<String> nodeNames = getNodeNames();
        String fromNode = animationTreeEditorManager.getSelectedNodeName();

        if (fromNode == null || nodeNames.size() < 2)
            return;

        String toNode = nodeNames.get(0).equals(fromNode) ? nodeNames.get(1) : nodeNames.get(0);

        animationTreeEditBranch.addTransition(document, layerIndex, fromNode, toNode, getLayerBlend());
    }

    // Tree \\

    ArpgArrayStruct getLayersArpg() {

        ArpgObjectStruct tree = animationTreeEditorManager.getTreeArpg();

        return tree != null && ArpgUtility.hasArray(tree, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_LAYERS)
                ? tree.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_LAYERS)
                : null;
    }

    int getLayerCount() {

        ArpgArrayStruct layers = getLayersArpg();
        return layers != null ? layers.size() : 0;
    }

    ArpgObjectStruct getLayerArpg(int layerIndex) {
        return layerIndex >= 0 && layerIndex < getLayerCount() ? getLayersArpg().get(layerIndex).getAsObject() : null;
    }

    ArpgObjectStruct getLayerArpg() {
        return getLayerArpg(animationTreeEditorManager.getSelectedLayerIndex());
    }

    ObjectArrayList<String> getNodeNames() {

        ObjectArrayList<String> nodeNames = new ObjectArrayList<>();
        ArpgObjectStruct nodes = CharacterViewUtility.findObject(
                getLayerArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES);

        if (nodes != null)
            nodeNames.addAll(nodes.keySet());

        return nodeNames;
    }

    ArpgObjectStruct getNodeArpg() {

        String nodeName = animationTreeEditorManager.getSelectedNodeName();
        ArpgObjectStruct nodes = CharacterViewUtility.findObject(
                getLayerArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_NODES);

        return nodeName != null ? CharacterViewUtility.findObject(nodes, nodeName) : null;
    }

    boolean isBlendSpace() {
        return getNodeArpg() != null && ArpgUtility.hasArray(
                getNodeArpg(),
                EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS);
    }

    int getSpaceCount() {
        return isBlendSpace() ? getNodeArpg().getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS).size() : 0;
    }

    private ArpgObjectStruct getSpaceEntry(int entry) {
        return getNodeArpg().getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_CLIPS).get(entry).getAsObject();
    }

    int getTransitionCount() {

        ArpgObjectStruct layer = getLayerArpg();

        return layer != null && ArpgUtility.hasArray(layer, EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS)
                ? layer.getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS).size()
                : 0;
    }

    private ArpgObjectStruct getTransition(int transitionIndex) {
        return getLayerArpg()
                .getAsArray(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_TRANSITIONS)
                .get(transitionIndex)
                .getAsObject();
    }

    boolean hasMask() {
        return CharacterViewUtility.findObject(getLayerArpg(), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MASK) != null;
    }

    private AnimationBlendMode getLayerMode() {
        return CharacterViewUtility.toEnumOrDefault(
                getLayerString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_MODE),
                AnimationBlendMode.class,
                AnimationBlendMode.OVERRIDE);
    }

    private AnimationParameter resolveNodeParameter() {
        return CharacterViewUtility.toEnumOrDefault(
                getNodeString(EditorSetting.ANIMATION_TREE_EDITOR_FIELD_PARAMETER),
                AnimationParameter.class,
                AnimationParameter.SPEED);
    }

    private boolean isRateScaled() {
        return getNodeArpg() != null
                && ArpgUtility.getBoolean(getNodeArpg(), EditorSetting.ANIMATION_TREE_EDITOR_FIELD_RATE_SCALE, false);
    }

    private String getLayerString(String field) {

        ArpgObjectStruct layer = getLayerArpg();
        return layer != null && ArpgUtility.hasString(layer, field) ? layer.get(field).getAsString() : null;
    }

    private String getNodeString(String field) {

        ArpgObjectStruct node = getNodeArpg();
        return node != null && ArpgUtility.hasString(node, field) ? node.get(field).getAsString() : null;
    }

    private String getMappedNode(String mapField, String key) {

        ArpgObjectStruct map = CharacterViewUtility.findObject(getLayerArpg(), mapField);
        return map != null && ArpgUtility.hasString(map, key) ? map.get(key).getAsString() : null;
    }

    // Rig \\

    RigHandle resolveRig() {

        TreeDocumentInstance document = animationTreeEditorManager.getActiveDocument();
        return document != null ? document.getRigHandle() : null;
    }

    private ObjectArrayList<String> getBoneNames() {

        RigHandle rigHandle = resolveRig();
        ObjectArrayList<String> boneNames = new ObjectArrayList<>(rigHandle.getBoneCount());

        for (int i = 0; i < rigHandle.getBoneCount(); i++)
            boneNames.add(rigHandle.getBone(i).getName());

        return boneNames;
    }

    // Utility \\

    private String orNone(String text) {
        return text != null && !text.isEmpty() ? text : EditorSetting.CHARACTER_VIEW_NONE;
    }
}
