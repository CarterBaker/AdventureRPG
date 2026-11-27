package editor.bootstrap.structureeditorpipeline.structureeditor;

import editor.runtime.EditorSetting;

public enum StructureEditorLayer {

    /*
     * The jobs of the Structure Editor's canvas, each a layer in the Layers
     * tab: laying blocks, painting the coverings grown over them and painting
     * the contents whole blocks hold. Each names the field of a block entry
     * it paints and the Info schema the Palette lists its choices from, both
     * empty for the blocks themselves, which come from the block registry.
     */

    BLOCKS(EditorSetting.STRUCTURE_EDITOR_LAYER_BLOCKS, "", ""),
    COVERAGE(EditorSetting.STRUCTURE_EDITOR_LAYER_COVERAGE, "covering", EditorSetting.INFO_SCHEMA_COVERINGS),
    CONTENTS(EditorSetting.STRUCTURE_EDITOR_LAYER_CONTENTS, "content", EditorSetting.INFO_SCHEMA_CONTENTS);

    // Identity
    public final String label;
    public final String field;
    public final String schemaName;

    // Constructor \\

    StructureEditorLayer(String label, String field, String schemaName) {

        // Identity
        this.label = label;
        this.field = field;
        this.schemaName = schemaName;
    }
}
