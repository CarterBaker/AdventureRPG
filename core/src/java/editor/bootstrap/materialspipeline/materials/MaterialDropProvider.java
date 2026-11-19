package editor.bootstrap.materialspipeline.materials;

public interface MaterialDropProvider {

    /*
     * A window a material is dropped on, as a material is dragged from a
     * library onto a mesh in a modelling tool. When a material carried out
     * of the Materials tab is let go over the window, it is handed here
     * once, the pointer still where it was let go; the window lays it on
     * whatever lies under the pointer and says whether anything took it.
     */

    boolean dropMaterial(String materialName);
}
