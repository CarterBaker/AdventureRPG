package application.bootstrap.shipwrightpipeline.shipwright;

public enum ShipwrightSheet {

    /*
     * The drawing a drafting sheet shows. A hull plan is one layer of the hull
     * seen from above with the layers either side of it faint beneath; a side
     * elevation is the hull's profile from starboard with every deck and the
     * waterline ruled across it; a level plan is everything standing on one
     * level's floor seen from above; a deck plan is the whole ship from above
     * with its masts. Every plan runs bow to the right and starboard down.
     */

    HULL_PLAN,
    SIDE_ELEVATION,
    LEVEL_PLAN,
    DECK_PLAN;

    public boolean isPlan() {
        return this != SIDE_ELEVATION;
    }
}
