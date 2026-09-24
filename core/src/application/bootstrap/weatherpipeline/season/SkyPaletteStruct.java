package application.bootstrap.weatherpipeline.season;

import engine.root.StructPackage;

public class SkyPaletteStruct extends StructPackage {

    /*
     * One season's sky palette: a SkyPhaseStruct per phase of the day,
     * ordered night to day to match EngineSetting.SKY_PHASE_NAMES and the
     * solar elevations in SKY_PHASE_ELEVATIONS, plus how strongly this
     * season paints its sun-side glow and its anti-solar twilight belt.
     */

    // Phases
    private final SkyPhaseStruct[] phases;

    // Twilight
    private final float glowStrength;
    private final float beltStrength;

    // Constructor \\

    public SkyPaletteStruct(SkyPhaseStruct[] phases, float glowStrength, float beltStrength) {

        // Phases
        this.phases = phases;

        // Twilight
        this.glowStrength = glowStrength;
        this.beltStrength = beltStrength;
    }

    // Accessible \\

    public SkyPhaseStruct getPhase(int index) {
        return phases[index];
    }

    public int getPhaseCount() {
        return phases.length;
    }

    public float getGlowStrength() {
        return glowStrength;
    }

    public float getBeltStrength() {
        return beltStrength;
    }
}
