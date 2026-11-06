package editor.runtime.properties;

import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewStepTarget;
import editor.runtime.properties.panel.BiomePropertiesSystem;
import editor.runtime.properties.panel.InfoPropertiesSystem;
import editor.runtime.properties.panel.PropertiesSystem;
import editor.runtime.properties.panel.RowPropertiesSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import engine.root.ContextPackage;

public class PropertiesContext extends ContextPackage implements CharacterViewStepTarget {

    /*
     * The editor's one Properties tab, shared by every window: it shows what
     * the window worked in last provides — a character, face or animation
     * window's value rows, each number on a slider and typed into its box,
     * the World Editor's biome palette while it paints, or, for the Hierarchy
     * and every content tool, the entry selected there with every field to
     * edit, the editor's Info Panel. Its toolbar and status line work on the
     * entry selected in the Hierarchy. Every edit goes through the manager
     * that owns it, so every window shows it at once. The shared tool keys
     * work while this tab is focused.
     */

    // Internal
    private PropertiesSystem propertiesSystem;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private RowPropertiesSystem rowPropertiesSystem;
    private InfoPropertiesSystem infoPropertiesSystem;
    private BiomePropertiesSystem biomePropertiesSystem;
    private ToolCanvasSystem toolCanvasSystem;

    // Internal \\

    @Override
    protected void create() {
        this.propertiesSystem = create(PropertiesSystem.class);
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.rowPropertiesSystem = create(RowPropertiesSystem.class);
        this.infoPropertiesSystem = create(InfoPropertiesSystem.class);
        this.biomePropertiesSystem = create(BiomePropertiesSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public void stepRow(String key, int steps) {
        rowPropertiesSystem.stepRow(key, steps);
    }
}
