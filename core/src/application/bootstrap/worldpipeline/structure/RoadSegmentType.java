package application.bootstrap.worldpipeline.structure;

public enum RoadSegmentType {

    /*
     * How one point along a road path meets the terrain, resolved once when
     * the path is planned. GROUND cuts into or builds a shallow embankment
     * under the road. BRIDGE spans a gap too deep to fill — a valley, a
     * river, open water — with a deck, rails and support pillars. TUNNEL
     * bores through terrain too high above the road to cut away.
     */

    GROUND,
    BRIDGE,
    TUNNEL;

    public static final RoadSegmentType[] VALUES = values();
}
