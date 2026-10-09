package application.bootstrap.worldpipeline.settlement;

import application.bootstrap.worldpipeline.layout.LayoutRangeStruct;
import engine.root.StructPackage;

public class SettlementRoleStruct extends StructPackage {

    /*
     * One role a settlement fills on its lots before any other, such as its
     * tavern or smithy, and how many lots of that role it raises.
     */

    // Role
    private final String role;
    private final LayoutRangeStruct count;

    // Constructor \\

    public SettlementRoleStruct(String role, LayoutRangeStruct count) {

        // Role
        this.role = role;
        this.count = count;
    }

    // Accessible \\

    public String getRole() {
        return role;
    }

    public LayoutRangeStruct getCount() {
        return count;
    }
}
