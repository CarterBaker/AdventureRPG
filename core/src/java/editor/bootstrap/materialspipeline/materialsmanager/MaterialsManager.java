package editor.bootstrap.materialspipeline.materialsmanager;

import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import editor.bootstrap.materialspipeline.materials.MaterialDropProvider;
import editor.runtime.EditorSetting;
import engine.root.ContextPackage;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialsManager extends ManagerPackage {

    /*
     * Owns the editor's materials: every texture of every baked pack, pack by
     * pack in name order, and the one chosen, which every window that lays a
     * material reads, as the Brushes tab holds the one brush every canvas
     * strokes with. A material pressed in the Materials tab is chosen and
     * picked up; held, it is carried over the windows, and once let go it is
     * dropped on the window under the pointer when that window takes
     * materials. A release is seen on the first frame no drag is reported,
     * whichever runs first in a frame, the tab's drag or this update.
     */

    // Internal
    private TextureManager textureManager;
    private WindowManager windowManager;

    // Palette
    private ObjectArrayList<String> packNames;
    private Object2ObjectOpenHashMap<String, ObjectArrayList<String>> packName2MaterialNames;
    private ObjectArrayList<String> materialNames;

    // Selection
    private String selectedMaterial;

    // Drag — the material carried, and whether its drag was reported since the last update
    private String draggedMaterial;
    private boolean dragReported;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.packName2MaterialNames = new Object2ObjectOpenHashMap<>();
        this.materialNames = new ObjectArrayList<>();

        // Selection
        this.selectedMaterial = EditorSetting.MATERIALS_DEFAULT_MATERIAL;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.windowManager = get(WindowManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        if (draggedMaterial == null)
            return;

        if (dragReported) {
            dragReported = false;
            return;
        }

        String material = draggedMaterial;
        draggedMaterial = null;
        drop(material);
    }

    // Palette \\

    // The packs and their materials listed once, on first use, after every pack has loaded at bootstrap
    private void resolvePalette() {

        if (packNames != null)
            return;

        this.packNames = textureManager.getPackNames();

        for (int i = 0; i < packNames.size(); i++) {

            ObjectArrayList<String> names = textureManager.getTextureNamesInArray(packNames.get(i));

            packName2MaterialNames.put(packNames.get(i), names);
            materialNames.addAll(names);
        }
    }

    // Selection \\

    public void selectMaterial(String materialName) {

        if (materialName.equals(selectedMaterial) || !hasMaterial(materialName))
            return;

        this.selectedMaterial = materialName;
        revision++;
    }

    // The material chosen stepped through every material, pack by pack
    public void cycleMaterial(int direction) {

        resolvePalette();

        if (materialNames.isEmpty())
            return;

        int index = Math.max(0, materialNames.indexOf(selectedMaterial));

        selectMaterial(materialNames.get(Math.floorMod(index + direction, materialNames.size())));
    }

    // Drag \\

    // Reported every frame the material's row is held, from the press until it is let go
    public void dragMaterial(String materialName) {

        selectMaterial(materialName);
        this.draggedMaterial = materialName;
        this.dragReported = true;
    }

    private void drop(String materialName) {

        WindowInstance window = windowManager.getHoveredWindow();
        ContextPackage context = window != null && window.hasContext() ? window.getContext() : null;

        if (context instanceof MaterialDropProvider provider)
            provider.dropMaterial(materialName);
    }

    // Accessible \\

    public ObjectArrayList<String> getPackNames() {

        resolvePalette();
        return packNames;
    }

    public ObjectArrayList<String> getMaterialNames(String packName) {

        resolvePalette();
        return packName2MaterialNames.get(packName);
    }

    public boolean hasMaterial(String materialName) {
        return textureManager.hasTexture(materialName);
    }

    public String getSelectedMaterial() {
        return selectedMaterial;
    }

    public boolean isDragging() {
        return draggedMaterial != null;
    }

    public int getRevision() {
        return revision;
    }
}
