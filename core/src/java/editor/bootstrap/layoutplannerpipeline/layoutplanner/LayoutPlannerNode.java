package editor.bootstrap.layoutplannerpipeline.layoutplanner;

public enum LayoutPlannerNode {

    /*
     * A settlement type's layout as the Layout Planner shows it in the Layers
     * tab: the settlement itself, the root, and under it one node for each
     * stage the planner lays it out in, each owning the fields of the type's
     * file that shape that stage, and whether its eye hides what it lays in
     * the preview.
     */

    SETTLEMENT("Settlement", false), // How often it is chosen, how far it reaches and the road it is linked by
    GROUND("Ground", false), // The heights and slope of the ground it may stand on
    STREETS("Streets", true), // The spokes and branches of its streets, their lengths and how they bend
    LOTS("Lots", true), // How far buildings stand back from the streets and how many there are
    OVERPASSES("Overpasses", false), // How many buildings bridge its streets
    WALL("Wall", true), // The curtain wall's ring and the spacing of its towers
    OUTPOSTS("Outposts", false); // The outposts off its edge and the trails out to them

    public final String displayName;
    public final boolean hideable;

    LayoutPlannerNode(String displayName, boolean hideable) {
        this.displayName = displayName;
        this.hideable = hideable;
    }
}
