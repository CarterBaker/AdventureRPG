package application.bootstrap.weatherpipeline.skymanager;

import engine.root.ManagerPackage;

public class SkyManager extends ManagerPackage {

    /*
     * Owns the sky's live color state. SkyPaletteBranch blends the season
     * palettes the year currently sits between and places them at any solar
     * elevation; SkyColorSystem combines that with each grid's temperature
     * and local weather into the per-grid SkyColorData UBO every frame. The
     * season blend follows the clock's calendar on its own, so a world
     * switch needs nothing from here.
     */

    // Branches
    private SkyPaletteBranch skyPaletteBranch;
    private SkyColorSystem skyColorSystem;

    // Base \\

    @Override
    protected void create() {

        // Branches
        this.skyPaletteBranch = create(SkyPaletteBranch.class);
        this.skyColorSystem = create(SkyColorSystem.class);
    }

    @Override
    protected void awake() {
        skyColorSystem.assignData(skyPaletteBranch);
    }
}
