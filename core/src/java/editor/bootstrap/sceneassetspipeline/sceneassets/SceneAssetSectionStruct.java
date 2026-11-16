package editor.bootstrap.sceneassetspipeline.sceneassets;

import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SceneAssetSectionStruct extends StructPackage {

    /*
     * One section of the Scene Assets tab: its kind and its rows in the order
     * they are listed, refilled whenever what the section shows changes, and
     * whether it is opened.
     */

    // Section
    private final SceneAssetKind kind;
    private final ObjectArrayList<SceneAssetRowStruct> rows;
    private boolean opened;

    // Constructor \\

    public SceneAssetSectionStruct(SceneAssetKind kind) {
        this.kind = kind;
        this.rows = new ObjectArrayList<>();
        this.opened = true;
    }

    // Management \\

    public void clear() {
        rows.clear();
    }

    public void add(SceneAssetRowStruct row) {
        rows.add(row);
    }

    // The rows in order of what they name, ignoring case
    public void sort() {
        rows.sort((first, second) -> first.getLabel().compareToIgnoreCase(second.getLabel()));
    }

    public void toggle() {
        this.opened = !opened;
    }

    // Accessible \\

    public SceneAssetKind getKind() {
        return kind;
    }

    public int size() {
        return rows.size();
    }

    public SceneAssetRowStruct get(int index) {
        return rows.get(index);
    }

    public boolean isOpened() {
        return opened;
    }
}
