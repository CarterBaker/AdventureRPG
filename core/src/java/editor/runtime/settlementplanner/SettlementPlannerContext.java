package editor.runtime.settlementplanner;

import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.settlementplanner.menu.SettlementPlannerMenuSystem;
import engine.root.ContextPackage;

public class SettlementPlannerContext extends ContextPackage implements PropertiesProvider {

    /*
     * Editor tab for choosing what the World Editor places, the way a brush
     * panel chooses a brush: the catalogue of settlement types and
     * structures, the architectures the chosen settlement can be built in,
     * its layout or a structure's turns, and the placements standing in the
     * pixel last selected, with the edited one's exact coordinates. It is all
     * words; a catalogue row dragged onto a World Editor stands the design
     * where it is dropped. The design, its placements and their live edits
     * live in the editor.
     */

    // Internal
    private SettlementPlannerMenuSystem settlementPlannerMenuSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.settlementPlannerMenuSystem = create(SettlementPlannerMenuSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Properties \\

    // What this tool works on is content selected in the Hierarchy, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
