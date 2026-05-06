package editor.bootstrap.dockpipeline.container;

import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.dockpipeline.node.NodeInstance;
import engine.root.InstancePackage;

public class ContainerInstance extends InstancePackage {

    /*
     * One per OS window. Owns the root of the dock node tree for that window.
     * Mirrors the relationship between WindowInstance and its content —
     * the container is the layout shell, the nodes are what fills it.
     */

    // Data
    private ContainerData data;

    // Window
    private WindowInstance window;
    private String canvasAreaId;

    // Tree
    private NodeInstance rootNode;

    // Constructor \\

    public void constructor(ContainerData data, WindowInstance window, String canvasAreaId) {
        this.data = data;
        this.window = window;
        this.canvasAreaId = canvasAreaId;
        this.rootNode = null;
    }

    // Mutators \\

    public void setRootNode(NodeInstance rootNode) {
        this.rootNode = rootNode;
    }

    // Queries \\

    public boolean hasRootNode() {
        return rootNode != null;
    }

    public int getWidth() {
        return window.getWidth();
    }

    public int getHeight() {
        return window.getHeight();
    }

    // Accessible \\

    public ContainerData getData() {
        return data;
    }

    public WindowInstance getWindow() {
        return window;
    }

    public String getCanvasAreaId() {
        return canvasAreaId;
    }

    public NodeInstance getRootNode() {
        return rootNode;
    }

    public int getContainerID() {
        return data.getContainerID();
    }
}