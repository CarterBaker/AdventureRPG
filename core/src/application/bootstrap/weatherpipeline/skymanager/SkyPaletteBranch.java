package application.bootstrap.weatherpipeline.skymanager;

import application.bootstrap.weatherpipeline.season.SkyPaletteStruct;
import application.bootstrap.weatherpipeline.season.SkyPhaseStruct;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

class SkyPaletteBranch extends BranchPackage {

    /*
     * Resolves the year's sky palette once per frame by blending the two
     * seasons SeasonManager currently sits between, phase by phase, so the
     * palette eases through the year with no seam at a season boundary.
     * resolvePhase() then places any solar elevation between the palette's
     * night, twilight, golden, and day phases and writes that single eased
     * color set into a caller-owned SkyPhaseStruct.
     */

    // Internal
    private SeasonManager seasonManager;

    // Palette
    private SkyPhaseStruct[] phases;
    private float glowStrength;
    private float beltStrength;

    // Elevation
    private float[] phaseElevations;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.phases = new SkyPhaseStruct[EngineSetting.SKY_PHASE_NAMES.length];

        for (int i = 0; i < phases.length; i++)
            phases[i] = createPhaseBuffer();

        // Elevation
        this.phaseElevations = EngineSetting.SKY_PHASE_ELEVATIONS;
    }

    @Override
    protected void get() {
        this.seasonManager = get(SeasonManager.class);
    }

    // Palette \\

    void resolvePalette() {

        SkyPaletteStruct from = seasonManager.getPreviousSeason().getSkyPalette();
        SkyPaletteStruct to = seasonManager.getNextSeason().getSkyPalette();
        float t = seasonManager.getSeasonBlendFactor();

        for (int i = 0; i < phases.length; i++)
            phases[i].blend(from.getPhase(i), to.getPhase(i), t);

        this.glowStrength = SkyColorUtility.lerp(from.getGlowStrength(), to.getGlowStrength(), t);
        this.beltStrength = SkyColorUtility.lerp(from.getBeltStrength(), to.getBeltStrength(), t);
    }

    // Phase \\

    void resolvePhase(float solarElevation, SkyPhaseStruct target) {

        int last = phases.length - 1;

        if (solarElevation <= phaseElevations[0]) {
            target.blend(phases[0], phases[0], 0f);
            return;
        }

        if (solarElevation >= phaseElevations[last]) {
            target.blend(phases[last], phases[last], 0f);
            return;
        }

        int upper = 1;

        while (solarElevation >= phaseElevations[upper])
            upper++;

        float t = SkyColorUtility.smoothstep(phaseElevations[upper - 1], phaseElevations[upper], solarElevation);

        target.blend(phases[upper - 1], phases[upper], t);
    }

    // Utility \\

    SkyPhaseStruct createPhaseBuffer() {
        return new SkyPhaseStruct(
                new Vector3(),
                new Vector3(),
                new Vector3(),
                new Vector3(),
                new Vector3(),
                new Vector3(),
                new Vector3());
    }

    // Accessible \\

    float getGlowStrength() {
        return glowStrength;
    }

    float getBeltStrength() {
        return beltStrength;
    }
}
