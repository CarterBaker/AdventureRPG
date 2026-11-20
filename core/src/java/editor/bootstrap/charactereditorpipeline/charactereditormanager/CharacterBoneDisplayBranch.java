package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import editor.bootstrap.charactereditorpipeline.util.CharacterBoneDisplay;
import editor.bootstrap.charactereditorpipeline.util.CharacterBoneKind;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.util.CharacterViewUtility;
import engine.root.BranchPackage;
import engine.util.mathematics.vectors.Vector4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterBoneDisplayBranch extends BranchPackage {

    /*
     * How every character viewport draws the bones, as Blender's armature
     * display sets it: the style, and the colour each kind of bone takes,
     * one of the named bone colours. One choice for the editor, listed in
     * the Properties tab of every window that draws bones (the Mesh, Weights
     * and Rig tabs, the Animation Editor) and read by each as it draws. The
     * revision moves whenever a choice does.
     */

    // Display
    private CharacterBoneDisplay display;
    private int[] kindColors;
    private Vector4[] colors;

    // Revision
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Display
        this.display = CharacterBoneDisplay.OCTAHEDRAL;
        this.kindColors = new int[CharacterBoneKind.VALUES.length];
        this.colors = new Vector4[CharacterBoneKind.VALUES.length];

        for (int k = 0; k < kindColors.length; k++) {
            this.kindColors[k] = CharacterBoneKind.VALUES[k].getDefaultColor();
            this.colors[k] = new Vector4();
            resolveColor(k);
        }
    }

    // Rows \\

    void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.BONE_DISPLAY_HEADER));
        rows.add(CharacterViewRowStruct.choice(
                EditorSetting.BONE_DISPLAY_KEY_STYLE,
                EditorSetting.BONE_DISPLAY_LABEL_STYLE,
                display.getLabel()));

        for (CharacterBoneKind kind : CharacterBoneKind.VALUES)
            rows.add(CharacterViewRowStruct.choice(
                    kind.getKey(),
                    kind.getLabel(),
                    EditorSetting.BONE_DISPLAY_COLOR_NAMES[kindColors[kind.ordinal()]]));
    }

    boolean isKey(String key) {
        return EditorSetting.BONE_DISPLAY_KEY_STYLE.equals(CharacterViewUtility.getKeyName(key))
                || findKind(key) != null;
    }

    // The style, or a kind's colour, stepped round its choices
    void cycle(String key, int direction) {

        CharacterBoneKind kind = findKind(key);

        if (kind == null)
            this.display = display.step(direction);
        else {
            int k = kind.ordinal();
            kindColors[k] = Math.floorMod(kindColors[k] + direction, EditorSetting.BONE_DISPLAY_COLOR_NAMES.length);
            resolveColor(k);
        }

        revision++;
    }

    String getValueText(String key) {

        CharacterBoneKind kind = findKind(key);

        return kind == null
                ? display.getLabel()
                : EditorSetting.BONE_DISPLAY_COLOR_NAMES[kindColors[kind.ordinal()]];
    }

    private CharacterBoneKind findKind(String key) {

        String keyName = CharacterViewUtility.getKeyName(key);

        for (CharacterBoneKind kind : CharacterBoneKind.VALUES)
            if (kind.getKey().equals(keyName))
                return kind;

        return null;
    }

    private void resolveColor(int kind) {

        float[] rgb = EditorSetting.BONE_DISPLAY_COLORS[kindColors[kind]];

        colors[kind].set(rgb[0], rgb[1], rgb[2], EditorSetting.BONE_DISPLAY_ALPHA);
    }

    // Accessible \\

    CharacterBoneDisplay getDisplay() {
        return display;
    }

    // Each kind's colour, by its ordinal
    Vector4[] getColors() {
        return colors;
    }

    int getRevision() {
        return revision;
    }
}
