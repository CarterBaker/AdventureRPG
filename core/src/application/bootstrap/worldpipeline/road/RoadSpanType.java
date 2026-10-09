package application.bootstrap.worldpipeline.road;

public enum RoadSpanType {

    /*
     * How a stretch of planned road meets the land under it, decided once
     * when the road is planned so every chunk it crosses lays it alike.
     */

    GROUND, // Laid on the land, cutting into rises and filled over dips
    BRIDGE, // Carried on a deck over water or a deep fall, on pillars down to the ground
    TUNNEL // Bored through high ground, walled and arched in its lining
}
