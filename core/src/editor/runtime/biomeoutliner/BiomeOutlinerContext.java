package editor.runtime.biomeoutliner;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.biomeoutliner.menu.BiomeOutlinerMenuSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;

public class BiomeOutlinerContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab listing every biome the world can paint, one row per biome
     * with its swatch and name, the selected one highlighted. Choosing a row
     * chooses the biome every World Editor paints and the Biome Planner
     * centres on, and selects it in the Hierarchy, so the Properties tab
     * shows its fields.
     */

    // Internal
    private BiomeOutlinerMenuSystem biomeOutlinerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.biomeOutlinerMenuSystem = create(BiomeOutlinerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // The biome chosen here is the entry selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
