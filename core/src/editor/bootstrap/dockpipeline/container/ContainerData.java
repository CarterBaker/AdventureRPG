package editor.bootstrap.dockpipeline.container;

import engine.root.DataPackage;

public class ContainerData extends DataPackage {

    // Identity
    private final int containerID;

    // Constructor \\

    public ContainerData(int containerID) {
        this.containerID = containerID;
    }

    // Accessible \\

    public int getContainerID() {
        return containerID;
    }
}