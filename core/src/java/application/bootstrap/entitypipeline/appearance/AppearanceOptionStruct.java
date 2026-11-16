package application.bootstrap.entitypipeline.appearance;

import engine.root.StructPackage;

public class AppearanceOptionStruct extends StructPackage {

    /*
     * One entry of a creator section: its kind and, for a kind that names a
     * member of a set, the member's ordinal, or, for sliders, the group whose
     * sliders it lists. INDEX_NOT_FOUND and null where the kind names
     * neither.
     */

    // Identity
    private final AppearanceOptionKind kind;
    private final int index;
    private final String groupName;

    // Constructor \\

    public AppearanceOptionStruct(AppearanceOptionKind kind, int index, String groupName) {

        // Identity
        this.kind = kind;
        this.index = index;
        this.groupName = groupName;
    }

    // Accessible \\

    public AppearanceOptionKind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public String getGroupName() {
        return groupName;
    }
}
