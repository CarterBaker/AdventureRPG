package editor.bootstrap.dockpipeline.tabgroup;

import engine.root.DataPackage;

public class TabGroupData extends DataPackage {

    // Identity
    private final int groupID;

    // Constructor \\

    public TabGroupData(int groupID) {
        this.groupID = groupID;
    }

    // Accessible \\

    public int getGroupID() {
        return groupID;
    }
}